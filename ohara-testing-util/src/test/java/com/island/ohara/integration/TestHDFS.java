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

import com.island.ohara.common.rule.MediumTest;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Map;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestHDFS extends MediumTest {
  // fake url
  private final String hdfsURI = "hdfs://10.1.0.1";

  private static Map<String, String> envMap;

  @BeforeClass
  @SuppressWarnings("unchecked")
  public static void before() {
    try {
      Field field = System.getenv().getClass().getDeclaredField("m");
      field.setAccessible(true);
      envMap = (Map<String, String>) field.get(System.getenv());
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * remote url is fake , cuz minicluster can't use in OHARA porject All remote method will throw
   * exception
   */
  @Test(expected = IOException.class)
  public void testHDFSRemote() throws IOException {
    envMap.put("ohara.it.hdfs", hdfsURI);
    envMap.put("ohara.it.hdfs.client.timeout", "1000");
    envMap.put("ohara.it.hdfs.client.retries", "2");

    Hdfs hdfs = OharaTestUtil.localHDFS().hdfs();
    assertFalse(hdfs.isLocal());
    assertTrue(hdfs.tmpDirectory().startsWith("/it"));
    hdfs.fileSystem().listFiles(new Path("/"), false);
  }

  @Test(expected = IOException.class)
  public void testFileSystemRemoteException() throws IOException {
    envMap.put("ohara.it.hdfs", hdfsURI);
    Hdfs hdfs = OharaTestUtil.localHDFS().hdfs();
    Configuration config = new Configuration();
    config.set("fs.defaultFS", hdfs.hdfsURL());
    FileSystem.get(config).listFiles(new Path("/"), false);
  }

  @Test
  public void testHDFSLocal() throws IOException {
    envMap.remove("ohara.it.hdfs", hdfsURI);
    Hdfs hdfs = OharaTestUtil.localHDFS().hdfs();
    assertTrue(hdfs.isLocal());
    assertFalse(hdfs.tmpDirectory().startsWith("/it"));

    Configuration config = new Configuration();
    config.set("fs.defaultFS", hdfs.hdfsURL());
    FileSystem.get(config).listFiles(new Path("/"), false);

    hdfs.fileSystem().listFiles(new Path("/"), false);
    assertTrue(hdfs.fileSystem().getHomeDirectory().toString().startsWith("file:"));
  }
}
