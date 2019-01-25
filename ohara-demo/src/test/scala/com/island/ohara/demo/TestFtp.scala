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

package com.island.ohara.demo

import com.island.ohara.common.rule.MediumTest
import com.island.ohara.common.util.CommonUtil
import org.junit.Test
import org.scalatest.Matchers

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
class TestFtp extends MediumTest with Matchers {

  @Test
  def testTtl(): Unit = {
    val f = Future {
      Ftp.start(
        Array(
          Ftp.CONTROL_PORT,
          CommonUtil.availablePort().toString,
          Ftp.DATA_PORTS,
          (0 to 3).map(_ => CommonUtil.availablePort()).mkString(","),
          Ftp.TTL,
          "5"
        ))
    }
    Await.result(f, 10 seconds)
  }

  @Test
  def testArgument(): Unit = {
    val user = methodName() + "-user"
    val password = methodName() + "-pwd"
    val controlPort = CommonUtil.availablePort()
    val dataPorts: Array[Int] = (0 to 3).map(_ => CommonUtil.availablePort()).toArray
    val f = Future {
      Ftp.start(
        Array(
          Ftp.USER,
          user,
          Ftp.PASSWORD,
          password,
          Ftp.CONTROL_PORT,
          controlPort.toString,
          Ftp.DATA_PORTS,
          dataPorts.mkString(","),
          Ftp.TTL,
          "5"
        ),
        ftp => {
          import scala.collection.JavaConverters._
          ftp.user() shouldBe user
          ftp.password() shouldBe password
          ftp.port() shouldBe controlPort
          ftp.dataPort().asScala shouldBe dataPorts
        }
      )
    }
    Await.result(f, 10 seconds)
  }
}
