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

package com.island.ohara.configurator

import java.io.{File, FileOutputStream}

import com.island.ohara.common.util.CommonUtils

import scala.concurrent.{Await, Future}

import scala.concurrent.duration._
package object file {

  def result[T](f: Future[T]): T = Await.result(f, 20 seconds)

  def generateFile(postfix: String): File = generateFile(postfix, CommonUtils.randomString().getBytes)

  private[this] def generateFile(postFix: String, bytes: Array[Byte]): File = {
    val tempFile = CommonUtils.createTempFile(CommonUtils.randomString(5), postFix)
    val output = new FileOutputStream(tempFile)
    try output.write(bytes)
    finally output.close()
    tempFile.deleteOnExit()
    tempFile
  }

  def generateJarFile(bytes: Array[Byte]): File = generateFile(".jar", bytes)

  def generateJarFile(): File = generateJarFile(CommonUtils.randomString().getBytes)
}
