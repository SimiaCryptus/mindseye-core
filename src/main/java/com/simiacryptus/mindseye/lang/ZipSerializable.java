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
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.io.*;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public interface ZipSerializable {

  default JsonElement getJson() {
    return getJson(null, SerialPrecision.Double);
  }

  @NotNull
  static HashMap<CharSequence, byte[]> extract(@Nonnull ZipFile zipfile) {
    Enumeration<? extends ZipEntry> entries = zipfile.entries();
    @Nonnull HashMap<CharSequence, byte[]> resources = new HashMap<>();
    while (entries.hasMoreElements()) {
      ZipEntry zipEntry = entries.nextElement();
      CharSequence name = zipEntry.getName();
      try {
        InputStream inputStream = zipfile.getInputStream(zipEntry);
        resources.put(name, IOUtils.readFully(inputStream, (int) zipEntry.getSize()));
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
    return resources;
  }

  JsonElement getJson(Map<CharSequence, byte[]> resources, DataSerializer dataSerializer);

  default void writeZip(@Nonnull File out) {
    writeZip(out, SerialPrecision.Double);
  }

  default void writeZip(@Nonnull File out, SerialPrecision precision) {
    try (@Nonnull ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(out))) {
      writeZip(zipOutputStream, precision, new HashMap<>(), "model.json");
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  default void writeZip(@Nonnull ZipOutputStream out, SerialPrecision precision, HashMap<CharSequence, byte[]> resources, String fileName) {
    try {
      JsonElement json = getJson(resources, precision);
      out.putNextEntry(new ZipEntry(fileName));
      @Nonnull JsonWriter writer = new JsonWriter(new OutputStreamWriter(out));
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
        } catch (java.util.zip.ZipException e) {
          // Ignore duplicate entry
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      });
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

}
