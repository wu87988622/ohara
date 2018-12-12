package com.island.ohara.integration;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import org.apache.commons.io.FileUtils;

public final class Integration {

  private Integration() {}

  static int resolvePort(int port) {
    return resolvePorts(Collections.singletonList(port)).get(0);
  }

  static List<Integer> resolvePorts(List<Integer> ports) {
    return ports
        .stream()
        .map(port -> (port <= 0) ? availablePort() : port)
        .collect(Collectors.toList());
  }

  public static int availablePort() {
    try (ServerSocket socket = new ServerSocket(0)) {
      socket.setReuseAddress(true);
      return socket.getLocalPort();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  static File createTempDir(String dirPrefix) {
    int count = 50;
    while (count >= 0) {
      Random random = new Random();
      File file =
          new File(System.getProperty("java.io.tmpdir"), dirPrefix + random.nextInt(100000));
      if (!file.exists()) {
        if (file.mkdirs()) return file;
        else
          throw new RuntimeException("could not create temp directory: " + file.getAbsolutePath());
      }
      count -= 1;
    }
    throw new IllegalStateException("Failed to create tmp folder");
  }

  /**
   * Delete the file or folder
   *
   * @param path path to file or folder
   */
  static void deleteFiles(File path) {
    try {
      FileUtils.forceDelete(path);
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }
}
