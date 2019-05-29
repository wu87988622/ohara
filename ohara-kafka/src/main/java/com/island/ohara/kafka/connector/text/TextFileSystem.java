/*
 * Copyright 2019 is-land
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.island.ohara.kafka.connector.text;

import java.io.Closeable;
import java.io.InputStreamReader;
import java.util.Collection;

/**
 * The close method will be invoked by the {@link TextSourceTask#_stop()} to release resources that
 * the object is holding (such as open ftpClient).
 */
public interface TextFileSystem extends Closeable {
  /**
   * list the files from input folder. NOTED: the returned value is full path.
   *
   * @return files from input folder
   */
  Collection<String> listInputFiles();

  /**
   * create a inputStreamReader for input file. NOTE: this resource will be automatically closed by
   * the {@link TextSourceTask#_poll()}
   *
   * @param path a full path form input file
   * @return a inputStreamReader from input file
   */
  InputStreamReader createReader(String path);

  void handleErrorFile(String path);

  void handleCompletedFile(String path);
}
