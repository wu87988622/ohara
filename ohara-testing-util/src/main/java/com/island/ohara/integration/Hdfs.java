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

package com.island.ohara.integration;

import com.island.ohara.common.util.CommonUtil;
import com.island.ohara.common.util.Releasable;
import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

/** HDFS client Using external HDFS or local FileSystem */
public interface Hdfs extends Releasable {
  String HDFS = "ohara.it.hdfs";
  String HDFS_CLEITN_TIMEOUT = "ohara.it.hdfs.client.timeout";
  String HDFS_CLIENT_RETRIES = "ohara.it.hdfs.client.retries";

  String hdfsURL();

  String tmpDirectory();

  boolean isLocal();

  FileSystem fileSystem();

  static Hdfs of() {
    return of(System.getenv(HDFS));
  }

  static Hdfs of(String hdfs) {
    return Optional.ofNullable(hdfs)
        .map(
            url ->
                (Hdfs)
                    new Hdfs() {
                      private final DateTimeFormatter formatter =
                          DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
                      private final String tempDir = "/it/" + LocalDateTime.now().format(formatter);

                      @Override
                      public void close() {
                        FileSystem fs = fileSystem();
                        try {
                          fs.delete(new Path(tmpDirectory()), true);
                        } catch (Exception e) {
                          throw new RuntimeException(e);
                        }
                      }

                      @Override
                      public String hdfsURL() {
                        return url.toLowerCase();
                      }

                      @Override
                      public String tmpDirectory() {
                        return tempDir;
                      }

                      @Override
                      public boolean isLocal() {
                        return false;
                      }

                      @Override
                      public FileSystem fileSystem() {
                        Configuration config = new Configuration();
                        config.set("fs.defaultFS", hdfsURL());
                        String hdfsClientTimeout = System.getenv(HDFS_CLEITN_TIMEOUT);
                        String hdfsClientRetries = System.getenv(HDFS_CLIENT_RETRIES);
                        if (hdfsClientTimeout != null)
                          config.set("ipc.client.connect.timeout", hdfsClientTimeout);

                        if (hdfsClientRetries != null)
                          config.set(
                              "ipc.client.connect.max.retries.on.timeouts", hdfsClientRetries);

                        try {
                          return FileSystem.get(config);
                        } catch (Exception e) {
                          throw new RuntimeException(e);
                        }
                      }
                    })
        .orElseGet(
            () ->
                new Hdfs() {
                  private final File tempDir = CommonUtil.createTempDir(Hdfs.class.getSimpleName());

                  @Override
                  public void close() {
                    CommonUtil.deleteFiles(tempDir);
                    FileSystem fs = fileSystem();
                    try {
                      fs.delete(new Path(tmpDirectory()), true);
                    } catch (Exception e) {
                      throw new RuntimeException(e);
                    }
                  }

                  @Override
                  public String hdfsURL() {
                    return "file://" + tmpDirectory();
                  }

                  @Override
                  public String tmpDirectory() {
                    return tempDir.getAbsolutePath();
                  }

                  @Override
                  public boolean isLocal() {
                    return true;
                  }

                  @Override
                  public FileSystem fileSystem() {
                    Configuration config = new Configuration();
                    config.set("fs.defaultFS", hdfsURL());
                    try {
                      return FileSystem.get(config);
                    } catch (Exception e) {
                      throw new RuntimeException(e);
                    }
                  }
                });
  }
}
