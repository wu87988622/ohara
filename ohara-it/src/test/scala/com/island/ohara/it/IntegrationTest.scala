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

package com.island.ohara.it

import java.time.Duration
import java.util.concurrent.TimeUnit

import com.island.ohara.client.configurator.v0.ClusterInfo
import com.island.ohara.common.rule.OharaTest
import com.island.ohara.common.util.CommonUtils
import org.junit.Rule
import org.junit.rules.Timeout

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

class IntegrationTest extends OharaTest {
  @Rule def globalTimeout: Timeout = new Timeout(2, TimeUnit.MINUTES)

  protected def result[T](f: Future[T]): T = IntegrationTest.result(f)

  protected def await(f: () => Boolean): Unit = IntegrationTest.await(f)

  /**
    * the creation of cluster is async so you need to wait the cluster to build.
    * @param f clusters
    * @param name cluster name
    */
  protected def assertCluster(f: () => Seq[ClusterInfo], name: String): Unit = assertClusters(f, Seq(name))
  protected def assertClusters(f: () => Seq[ClusterInfo], names: Seq[String]): Unit = await { () =>
    val clusters = f()
    names.forall(name => clusters.map(_.name).contains(name))
  }
  protected def assertNoCluster(f: () => Seq[ClusterInfo], name: String): Unit = assertNoClusters(f, Seq(name))

  protected def assertNoClusters(f: () => Seq[ClusterInfo], names: Seq[String]): Unit = await { () =>
    val clusters = f()
    names.forall(name => !clusters.map(_.name).contains(name))
  }
}

object IntegrationTest {
  def result[T](f: Future[T]): T = Await.result(f, 300 seconds)

  def await(f: () => Boolean): Unit = CommonUtils.await(() => f(), Duration.ofSeconds(300))
}
