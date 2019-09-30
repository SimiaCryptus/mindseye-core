/*
 * Copyright (c) 2019 by Andrew Charneski.
 *
 * The author licenses this file to you under the
 * Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance
 * with the License.  You may obtain a copy
 * of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.simiacryptus.mindseye.util;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.simiacryptus.mindseye.lang.Tensor;
import com.simiacryptus.util.data.DoubleStatistics;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class ImageUtil {
  private static final Logger logger = LoggerFactory.getLogger(ImageUtil.class);
  public static ScheduledExecutorService scheduledThreadPool = Executors.newScheduledThreadPool(1, new ThreadFactoryBuilder().setDaemon(true).build());

  public static Stream<BufferedImage> renderToImages(@Nonnull final Tensor tensor, final boolean normalize) {
    final DoubleStatistics[] statistics = IntStream.range(0, tensor.getDimensions()[2]).mapToObj(band -> {
      return new DoubleStatistics().accept(tensor.coordStream(false)
          .filter(x -> x.getCoords()[2] == band)
          .mapToDouble(c -> tensor.get(c)).toArray());
    }).toArray(i -> new DoubleStatistics[i]);
    @Nonnull final BiFunction<Double, DoubleStatistics, Double> transform = (value, stats) -> {
      final double width = Math.sqrt(2) * stats.getStandardDeviation();
      final double centered = value - stats.getAverage();
      final double distance = Math.abs(value - stats.getAverage());
      final double positiveMax = stats.getMax() - stats.getAverage();
      final double negativeMax = stats.getAverage() - stats.getMin();
      final double unitValue;
      if (value < centered) {
        if (distance > width) {
          unitValue = 0.25 - 0.25 * ((distance - width) / (negativeMax - width));
        } else {
          unitValue = 0.5 - 0.25 * (distance / width);
        }
      } else {
        if (distance > width) {
          unitValue = 0.75 + 0.25 * ((distance - width) / (positiveMax - width));
        } else {
          unitValue = 0.5 + 0.25 * (distance / width);
        }
      }
      return 0xFF * unitValue;
    };
    tensor.coordStream(true).collect(Collectors.groupingBy(x -> x.getCoords()[2], Collectors.toList()));
    @Nullable final Tensor normal = tensor.mapCoords((c) -> transform.apply(tensor.get(c), statistics[c.getCoords()[2]]))
        .map(v -> Math.min(0xFF, Math.max(0, v)));
    return (normalize ? normal : tensor).toImages().stream();
  }

  @Nonnull
  public static BufferedImage resize(@Nonnull final BufferedImage source, final int size) {
    return resize(source, size, false);
  }

  @Nonnull
  public static BufferedImage resize(@Nonnull final BufferedImage source, final int size, boolean preserveAspect) {
    if (size <= 0) return source;
    double zoom = (double) size / source.getWidth();
    int steps = (int) Math.ceil(Math.abs(Math.log(zoom)) / Math.log(1.5));
    BufferedImage img = source;
    for (int i = 1; i <= steps; i++) {
      double pos = ((double) i / steps);
      double z = Math.pow(zoom, pos);
      int targetWidth = (int) (source.getWidth() * z);
      int targetHeight = (int) ((source.getWidth() == source.getHeight()) ? targetWidth : ((preserveAspect ? source.getHeight() : source.getWidth()) * z));
      img = resize(img, targetWidth, targetHeight);
    }
    return img;
  }

  public static BufferedImage resizePx(@Nonnull final BufferedImage source, final long size) {
    if (size < 0) return source;
    double scale = Math.sqrt(size / ((double) source.getHeight() * source.getWidth()));
    int width = (int) (scale * source.getWidth());
    int height = (int) (scale * source.getHeight());
    return resize(source, width, height);
  }

  @Nonnull
  public static BufferedImage resize(BufferedImage source, int width, int height) {
    @Nonnull final BufferedImage image = new BufferedImage(width, height, source.getType());
    @Nonnull final Graphics2D graphics = (Graphics2D) image.getGraphics();
    HashMap<Object, Object> hints = new HashMap<>();
    hints.put(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
    hints.put(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
    hints.put(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
    hints.put(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
    graphics.setRenderingHints(hints);
    graphics.drawImage(source, 0, 0, width, height, null);
    return image;
  }

  public static void monitorImage(final Tensor input, final boolean exitOnClose, final boolean normalize) {
    monitorImage(input, exitOnClose, 30, normalize);
  }

  public static void monitorImage(final Tensor input, final boolean exitOnClose, final int period, final boolean normalize) {
    if (GraphicsEnvironment.isHeadless() || !Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.BROWSE))
      return;
    JLabel label = new JLabel(new ImageIcon(input.toImage()));
    final AtomicReference<JDialog> dialog = new AtomicReference<JDialog>();
    WeakReference<JLabel> labelWeakReference = new WeakReference<>(label);
    ScheduledFuture<?> updater = scheduledThreadPool.scheduleAtFixedRate(() -> {
      try {
        JLabel jLabel = labelWeakReference.get();
        if (null != jLabel && !input.isFinalized()) {
          BufferedImage image = (normalize ? normalizeBands(input) : input).toImage();
          int width = jLabel.getWidth();
          if (width > 0) resize(image, width, jLabel.getHeight());
          jLabel.setIcon(new ImageIcon(image));
          return;
        }
      } catch (Throwable e) {
        logger.warn("Error updating png", e);
      }
      JDialog jDialog = dialog.get();
      jDialog.setVisible(false);
      jDialog.dispose();
    }, 0, period, TimeUnit.SECONDS);
    new Thread(() -> {
      Window window = JOptionPane.getRootFrame();
      String title = "Image: " + Arrays.toString(input.getDimensions());
      if (window instanceof Frame) {
        dialog.set(new JDialog((Frame) window, title, true));
      } else {
        dialog.set(new JDialog((Dialog) window, title, true));
      }
      dialog.get().setResizable(false);
      dialog.get().setSize(input.getDimensions()[0], input.getDimensions()[1]);
      JMenuBar menu = new JMenuBar();
      JMenu fileMenu = new JMenu("File");
      JMenuItem saveAction = new JMenuItem("Save");
      fileMenu.add(saveAction);
      saveAction.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(final ActionEvent e) {
          JFileChooser fileChooser = new JFileChooser();
          fileChooser.setAcceptAllFileFilterUsed(false);
          fileChooser.addChoosableFileFilter(new FileFilter() {
            @Override
            public boolean accept(final File f) {
              return f.getName().toUpperCase().endsWith(".PNG");
            }

            @Override
            public String getDescription() {
              return "*.png";
            }
          });

          int result = fileChooser.showSaveDialog(dialog.get());
          if (JFileChooser.APPROVE_OPTION == result) {
            try {
              File selectedFile = fileChooser.getSelectedFile();
              if (!selectedFile.getName().toUpperCase().endsWith(".PNG"))
                selectedFile = new File(selectedFile.getParent(), selectedFile.getName() + ".png");
              BufferedImage image = (normalize ? normalizeBands(input) : input).toImage();
              if (!ImageIO.write(image, "PNG", selectedFile)) throw new IllegalArgumentException();
            } catch (IOException e1) {
              throw new RuntimeException(e1);
            }
          }
        }
      });
      menu.add(fileMenu);
      dialog.get().setJMenuBar(menu);

      Container contentPane = dialog.get().getContentPane();
      contentPane.setLayout(new BorderLayout());
      contentPane.add(label, BorderLayout.CENTER);
      //contentPane.add(dialog, BorderLayout.CENTER);
      if (JDialog.isDefaultLookAndFeelDecorated()) {
        boolean supportsWindowDecorations = UIManager.getLookAndFeel().getSupportsWindowDecorations();
        if (supportsWindowDecorations) {
          dialog.get().setUndecorated(true);
          SwingUtilities.getRootPane(dialog.get()).setWindowDecorationStyle(JRootPane.PLAIN_DIALOG);
        }
      }
      dialog.get().pack();
      dialog.get().setLocationRelativeTo(null);
      dialog.get().addComponentListener(new ComponentAdapter() {
        @Override
        public void componentResized(final ComponentEvent e) {
          //dialog.pack();
          super.componentResized(e);
          BufferedImage image = input.toImage();
          int width = e.getComponent().getWidth();
          if (width > 0) resize(image, width, e.getComponent().getHeight());
          label.setIcon(new ImageIcon(image));
          dialog.get().pack();
        }
      });
      dialog.get().addWindowListener(new WindowAdapter() {
        private boolean gotFocus = false;

        public void windowClosed(WindowEvent e) {
          dialog.get().getContentPane().removeAll();
          updater.cancel(false);
          if (exitOnClose) {
            logger.warn("Exiting test", new RuntimeException("Stack Trace"));
            System.exit(0);
          }
        }

        public void windowGainedFocus(WindowEvent we) {
          // Once window gets focus, set initial focus
          if (!gotFocus) {
            gotFocus = true;
          }
        }

      });
      dialog.get().setVisible(true);
      dialog.get().dispose();
    }).start();
  }

  public static Tensor normalizeBands(final Tensor image) {
    return normalizeBands(image, 255);
  }

  public static Tensor normalizeBands(final Tensor image, final int max) {
    DoubleStatistics[] statistics = IntStream.range(0, image.getDimensions()[2]).mapToObj(i -> new DoubleStatistics()).toArray(i -> new DoubleStatistics[i]);
    image.coordStream(false).forEach(c -> {
      double value = image.get(c);
      statistics[c.getCoords()[2]].accept(value);
    });
    return image.mapCoords(c -> {
      double value = image.get(c);
      DoubleStatistics statistic = statistics[c.getCoords()[2]];
      return max * (value - statistic.getMin()) / (statistic.getMax() - statistic.getMin());
    });
  }

  public static BufferedImage load(@Nonnull final Supplier<BufferedImage> image, final int imageSize) {
    return imageSize <= 0 ? image.get() : resize(image.get(), imageSize, true);
  }

  @Nonnull
  public static BufferedImage load(@Nonnull final Supplier<BufferedImage> image, final int width, final int height) {
    return width <= 0 ? image.get() : resize(image.get(), width, height);
  }

  @NotNull
  public static Tensor getTensor(@Nonnull CharSequence file) {
    String fileStr = file.toString();
    if (fileStr.startsWith("http")) {
      try {
        BufferedImage read = ImageIO.read(new URL(fileStr));
        if (null == read) throw new IllegalArgumentException("Error reading " + file);
        return Tensor.fromRGB(read);
      } catch (Throwable e) {
        throw new RuntimeException("Error reading " + file, e);
      }
    }
    if (fileStr.startsWith("file:///")) {
      try {
        BufferedImage read = ImageIO.read(new File(fileStr.substring(8)));
        if (null == read) throw new IllegalArgumentException("Error reading " + file);
        return Tensor.fromRGB(read);
      } catch (Throwable e) {
        throw new RuntimeException("Error reading " + file, e);
      }
    }
    throw new IllegalArgumentException(file.toString());
  }
}