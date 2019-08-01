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

package com.island.ohara.kafka.connector.csv.sink;

import com.island.ohara.kafka.connector.TopicPartition;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

public class FileUtils {
  private static final String DIR_DELIM = "/";
  private static final String FILE_DELIM = "-";
  private static final String ZERO_PAD_OFFSET_FORMAT = "%09d";

  public static String generatePartitionedPath(String topic, String encodedPartition) {
    return topic + DIR_DELIM + encodedPartition;
  }

  public static String fileName(String topicsDir, String directory, String name) {
    return topicsDir + DIR_DELIM + directory + DIR_DELIM + name;
  }

  public static String committedFileName(
      String topicsDir, String directory, TopicPartition tp, long startOffset, String extension) {
    String topic = tp.topicName();
    int partition = tp.partition();
    StringBuffer name = new StringBuffer();
    name.append(topic);
    name.append(FILE_DELIM);
    name.append(partition);
    name.append(FILE_DELIM);
    name.append(String.format(ZERO_PAD_OFFSET_FORMAT, startOffset));
    name.append(extension);
    return fileName(topicsDir, directory, name.toString());
  }

  public static Path temporaryFile(Path file) {
    String ext = getFileExtension(file.toFile().getName());
    String name = UUID.randomUUID().toString() + "_tmp" + ext;
    return Paths.get(file.getParent() + DIR_DELIM + name);
  }

  public static String getFileExtension(String filename) {
    int lastIndexOf = filename.lastIndexOf(".");
    if (lastIndexOf == -1) {
      return ""; // empty extension
    }
    return filename.substring(lastIndexOf);
  }
}
