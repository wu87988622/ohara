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

package com.island.ohara.testing.service;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.island.ohara.testing.WithTestUtils;
import java.io.IOException;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.junit.Test;

public class TestHDFS extends WithTestUtils {

  @Test
  public void testHDFSLocal() throws IOException {
    Hdfs hdfs = testUtil().hdfs();
    assertTrue(hdfs.isLocal());
    assertFalse(hdfs.tmpDirectory().startsWith("/it"));

    Configuration config = new Configuration();
    config.set("fs.defaultFS", hdfs.hdfsURL());
    FileSystem.get(config).listFiles(new Path("/"), false);

    hdfs.fileSystem().listFiles(new Path("/"), false);
    assertTrue(hdfs.fileSystem().getHomeDirectory().toString().startsWith("file:"));
  }
}
