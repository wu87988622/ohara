package com.island.ohara.integration;

import com.island.ohara.common.util.CommonUtil;
import com.island.ohara.common.util.Releasable;
import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

/** HDFS client Using external HDFS or local FileSystem */
public interface Hdfs extends Releasable {
  String HDFS = "ohara.it.hdfs";

  String hdfsURL();

  String tmpDirectory();

  boolean isLocal();

  FileSystem fileSystem();

  static Hdfs of() {
    return of(System.getenv(HDFS));
  }

  static Hdfs of(String hdfs) {
    final File tmpFile;
    final String _hdfsUrl;
    final String _tmpDirectory;
    final Boolean _isLocal;

    if (hdfs != null) {
      tmpFile = null;
      hdfs = hdfs.toLowerCase();
      LocalDateTime now = LocalDateTime.now();
      DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
      String timeString = now.format(formatter);
      _tmpDirectory = "/it/" + timeString;
      _isLocal = false;
      _hdfsUrl = hdfs;
    } else {
      File file = CommonUtil.createTempDir(Hdfs.class.getSimpleName());
      tmpFile = file;
      _tmpDirectory = file.getAbsolutePath();
      _isLocal = true;
      _hdfsUrl = "file://" + _tmpDirectory;
    }

    return new Hdfs() {

      @Override
      public void close() {
        // delete localfile
        if (tmpFile != null) {
          CommonUtil.deleteFiles(tmpFile);
        }
        FileSystem fs = fileSystem();
        try {
          fs.delete(new Path(tmpDirectory()), true);
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
      }

      @Override
      public String hdfsURL() {
        return _hdfsUrl;
      }

      @Override
      public String tmpDirectory() {
        return _tmpDirectory;
      }

      @Override
      public boolean isLocal() {
        return _isLocal;
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
    };
  }
}
