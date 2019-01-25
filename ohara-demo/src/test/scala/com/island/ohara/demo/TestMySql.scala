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
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

class TestMySql extends MediumTest with Matchers {

  @Test
  def testTtl(): Unit = {
    val f = Future {
      MySql.start(
        Array(
          MySql.PORT,
          CommonUtil.availablePort().toString,
          MySql.TTL,
          "5"
        ))
    }
    Await.result(f, 30 seconds)
  }

  @Test
  def testArgument(): Unit = {
    val user = methodName() + "-user"
    val password = methodName() + "-pwd"
    val port = CommonUtil.availablePort()
    val dbName = methodName() + "-db"
    val f = Future {
      MySql.start(
        Array(
          MySql.USER,
          user,
          MySql.PASSWORD,
          password,
          MySql.PORT,
          port.toString,
          MySql.DB_NAME,
          dbName,
          MySql.TTL,
          "5"
        ),
        mysql => {
          mysql.user() shouldBe user
          mysql.password() shouldBe password
          mysql.port() shouldBe port
          mysql.databaseName() shouldBe dbName
        }
      )
    }
    Await.result(f, 30 seconds)
  }
}
