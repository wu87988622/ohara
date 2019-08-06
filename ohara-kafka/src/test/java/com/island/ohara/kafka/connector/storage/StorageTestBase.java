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

package com.island.ohara.kafka.connector.storage;

import com.google.common.collect.Iterators;
import com.island.ohara.common.annotations.IgnoreNamingRule;
import com.island.ohara.common.exception.OharaException;
import com.island.ohara.common.exception.OharaFileAlreadyExistsException;
import com.island.ohara.common.util.CommonUtils;
import com.island.ohara.common.util.Releasable;
import com.island.ohara.testing.WithTestUtils;
import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.stream.Collectors;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

@IgnoreNamingRule
public abstract class StorageTestBase extends WithTestUtils {
  protected static final Path ROOT_FOLDER_DEFAULT = Paths.get("/root");

  private Storage storage;
  private String rootFolder;

  protected abstract Storage createStorage();

  protected abstract Path getRootFolder();

  @Before
  public void setup() {
    storage = createStorage();
    rootFolder = getRootFolder().toString();
    if (!storage.exists(rootFolder)) {
      storage.mkdirs(rootFolder);
    }
  }

  @After
  public void cleanup() {
    Releasable.close(storage);
  }

  @Test
  public void testNormal() throws IOException {
    Assert.assertEquals(0, size(storage.list(rootFolder)));

    // create a folder
    String folder = randomFolder();
    Assert.assertFalse(storage.exists(folder));
    storage.mkdirs(folder);
    Assert.assertTrue(storage.exists(folder));
    Assert.assertEquals(1, size(storage.list(rootFolder)));

    // create a file
    String file = randomFile();
    storage.create(file).close();
    Assert.assertEquals(2, size(storage.list(rootFolder)));

    // delete a file
    storage.delete(file);
    Assert.assertEquals(1, size(storage.list(rootFolder)));
  }

  @Test
  public void testList() {
    Assert.assertEquals(0, size(storage.list(rootFolder)));
  }

  @Test(expected = OharaException.class)
  public void testListWithNonExistedPath() {
    storage.list(randomFolder());
  }

  @Test
  public void testCreate() throws IOException {
    String file = randomFile();
    String text = randomText();

    OutputStream outputStream = storage.create(file);
    outputStream.write(text.getBytes());
    outputStream.close();
    Assert.assertEquals(text, readLine(file));
  }

  @Test(expected = OharaFileAlreadyExistsException.class)
  public void testCreateWithExistedPath() {
    String file = randomFile();
    OutputStream outputStream = storage.create(file);
    Releasable.close(outputStream);

    // create file with the same path
    storage.create(file);
  }

  @Test
  public void testAppend() throws IOException {
    String file = randomFile();
    String text = randomText();

    // create file
    OutputStream stream = storage.create(file);
    stream.write(text.getBytes());
    stream.close();
    Assert.assertEquals(text, readLine(file));

    // append file
    OutputStream stream2 = storage.append(file);
    stream2.write(text.getBytes());
    stream2.close();
    Assert.assertEquals(text + text, readLine(file));
  }

  @Test(expected = OharaException.class)
  public void testAppendWithNonExistedPath() {
    storage.append(randomFile());
  }

  @Test
  public void testDelete() throws IOException {
    String file = randomFile();
    storage.create(file).close();
    Assert.assertEquals(1, size(storage.list(rootFolder)));

    // delete file
    storage.delete(file);
    Assert.assertEquals(0, size(storage.list(rootFolder)));

    // delete file again... nothing happen
    storage.delete(file);
  }

  @Test
  public void testMove() throws IOException {
    String file = randomFile();
    String file2 = randomFile();
    storage.create(file).close();

    Assert.assertTrue(storage.exists(file));
    Assert.assertFalse(storage.exists(file2));
    storage.move(file, file2);
    Assert.assertFalse(storage.exists(file));
    Assert.assertTrue(storage.exists(file2));
  }

  protected String randomFolder() {
    return CommonUtils.path(rootFolder, CommonUtils.randomString(10), CommonUtils.randomString(10));
  }

  protected String randomFile() {
    return CommonUtils.path(rootFolder, CommonUtils.randomString(10) + ".txt");
  }

  protected String randomText() {
    return CommonUtils.randomString(10);
  }

  protected int size(Iterator<String> it) {
    return Iterators.size(it);
  }

  protected String readLine(String path) {
    InputStream inputStream = storage.open(path);
    String result =
        new BufferedReader(new InputStreamReader(inputStream))
            .lines()
            .collect(Collectors.joining(System.lineSeparator()));
    Releasable.close(inputStream);
    return result;
  }

  protected Storage getStorage() {
    return storage;
  }
}
