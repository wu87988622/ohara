package com.island.ohara.integration;

import java.io.File;
import java.net.ServerSocket;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public final class Integration {
  private Integration() {}

  public static int freePort() {
    return resolvePort(0);
  }

  public static int resolvePort(int port) {
    return resolvePorts(Collections.singletonList(port)).get(0);
  }

  public static List<Integer> resolvePorts(List<Integer> ports) {
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

  public static File createTempDir(String dirPrefix) {
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
   * @return <code>true</code> if and only if the file or directory is successfully deleted; <code>
   *     false</code> otherwise
   */
  public static void deleteFiles(File path) {
    if (!deleteFile(path))
      throw new IllegalStateException("Fail to delete " + path.getAbsolutePath());
  }

  private static boolean deleteFile(File path) {
    if (!path.exists())
      throw new RuntimeException(path.getAbsolutePath().toString() + " not found");
    boolean ret = true;
    if (path.isDirectory()) {
      for (File f : path.listFiles()) {
        ret = ret && deleteFile(f);
      }
    }
    return ret && path.delete();
  }
}
