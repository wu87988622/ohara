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

import java.text.SimpleDateFormat
import java.util.{Date, Properties}

import sbt._

object VersionUtilsGenerator {

  def generate(project: SettingKey[ResolvedProject], javaDir: SettingKey[File], log: SettingKey[Logger]) = Def.task[Seq[File]] {
    val outputFile = new File(javaDir.value, "com/island/ohara/common/util/VersionUtils.java")
    val version = readProperty(project.value.base / ".." / "gradle.properties", "version")
    val content = fileContent(version, user(), revision(), datetime())
    if (!outputFile.exists || IO.read(outputFile) != content) {
      IO.write(outputFile, content)
      log.value.info(s"File generated: $outputFile")
    }
    Seq(outputFile)
  }

  import sys.process._

  private def user() = "git config user.name".!!.split("\n").head

  private def revision() = "git log -1 --pretty=format:%H".!!.split("\n").head

  private def datetime() = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())

  private def readProperty(file: File, key: String): String = {
    val props = new Properties()
    sbt.IO.load(props, file)
    props.getProperty(key)
  }

  private def fileContent(version: String, user: String, revision: String, date: String): String = {
    s"""|package com.island.ohara.common.util;
       |// DON'T touch this file!!! It is generated dynamically. see project/VersionUtilsGenerator.scala
       |public final class VersionUtils {
       |  public static final String VERSION = "$version";
       |  public static final String USER = "$user";
       |  public static final String REVISION = "$revision";
       |  public static final String DATE = "$date";
       |
       |  public static void main(String[] args) {
       |    System.out.println("ohara " + VERSION);
       |    System.out.println("revision=" + REVISION);
       |    System.out.println("Compiled by " + USER + " on " + DATE);
       |  }
       |
       |  private VersionUtils() {}
       |}
      """.stripMargin
  }

}
