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

package com.simiacryptus.mindseye.lang;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.stream.JsonWriter;
import com.simiacryptus.util.Util;
import org.apache.commons.io.IOUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.*;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 * The interface Zip serializable.
 */
public interface ZipSerializable {

  /**
   * Gets json.
   *
   * @return the json
   */
  @Nullable
  default JsonElement getJson() {
    return getJson(null, SerialPrecision.Double);
  }

  /**
   * Extract hash map.
   *
   * @param zipfile the zipfile
   * @return the hash map
   */
  @Nonnull
  static HashMap<CharSequence, byte[]> extract(@Nonnull ZipFile zipfile) {
    Enumeration<? extends ZipEntry> entries = zipfile.entries();
    @Nonnull
    HashMap<CharSequence, byte[]> resources = new HashMap<>();
    while (entries.hasMoreElements()) {
      ZipEntry zipEntry = entries.nextElement();
      CharSequence name = zipEntry.getName();
      try {
        InputStream inputStream = zipfile.getInputStream(zipEntry);
        resources.put(name, IOUtils.readFully(inputStream, (int) zipEntry.getSize()));
      } catch (IOException e) {
        throw Util.throwException(e);
      }
    }
    return resources;
  }

  /**
   * Gets json.
   *
   * @param resources      the resources
   * @param dataSerializer the data serializer
   * @return the json
   */
  @Nullable
  JsonElement getJson(Map<CharSequence, byte[]> resources, DataSerializer dataSerializer);

  /**
   * Write zip.
   *
   * @param out the out
   */
  default void writeZip(@Nonnull File out) {
    writeZip(out, SerialPrecision.Double);
  }

  /**
   * Write zip.
   *
   * @param out       the out
   * @param precision the precision
   */
  default void writeZip(@Nonnull File out, SerialPrecision precision) {
    try (@Nonnull
         ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(out))) {
      writeZip(zipOutputStream, precision, new HashMap<>(), "model.json");
    } catch (IOException e) {
      throw Util.throwException(e);
    }
  }

  /**
   * Write zip.
   *
   * @param out       the out
   * @param precision the precision
   * @param resources the resources
   * @param fileName  the file name
   */
  default void writeZip(@Nonnull ZipOutputStream out, SerialPrecision precision,
                        @Nonnull HashMap<CharSequence, byte[]> resources, @Nonnull String fileName) {
    try {
      JsonElement json = getJson(resources, precision);
      out.putNextEntry(new ZipEntry(fileName));
      @Nonnull
      JsonWriter writer = new JsonWriter(new OutputStreamWriter(out));
      writer.setIndent("  ");
      writer.setHtmlSafe(true);
      writer.setSerializeNulls(false);
      new GsonBuilder().setPrettyPrinting().create().toJson(json, writer);
      writer.flush();
      out.closeEntry();
      resources.forEach((name, data) -> {
        try {
          out.putNextEntry(new ZipEntry(String.valueOf(name)));
          IOUtils.write(data, out);
          out.flush();
          out.closeEntry();
        } catch (ZipException e) {
          // Ignore duplicate entry
        } catch (IOException e) {
          throw Util.throwException(e);
        }
      });
    } catch (IOException e) {
      throw Util.throwException(e);
    }
  }

}
