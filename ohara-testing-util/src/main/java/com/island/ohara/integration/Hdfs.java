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
                      private DateTimeFormatter formatter =
                          DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
                      private String tempDir = "/it/" + LocalDateTime.now().format(formatter);

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
                  private File tempDir = CommonUtil.createTempDir(Hdfs.class.getSimpleName());

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
