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

package oharastream.ohara.it.collie

import oharastream.ohara.client.configurator.v0.{ContainerApi, NodeApi, ZookeeperApi}
import oharastream.ohara.it.category.CollieGroup
import oharastream.ohara.it.{ContainerPlatform, WithRemoteConfigurator}
import org.junit.Test
import org.junit.experimental.categories.Category
import org.scalatest.matchers.should.Matchers._

import scala.concurrent.ExecutionContext.Implicits.global

@Category(Array(classOf[CollieGroup]))
class TestGetNodeWithRunningCluster(platform: ContainerPlatform)
    extends WithRemoteConfigurator(platform: ContainerPlatform) {
  @Test
  def test(): Unit = {
    val cluster = result(
      ZookeeperApi.access
        .hostname(configuratorHostname)
        .port(configuratorPort)
        .request
        .key(serviceKeyHolder.generateClusterKey())
        .nodeNames(platform.nodeNames)
        .create()
        .flatMap(
          info =>
            ZookeeperApi.access
              .hostname(configuratorHostname)
              .port(configuratorPort)
              .start(info.key)
              .flatMap(_ => ZookeeperApi.access.hostname(configuratorHostname).port(configuratorPort).get(info.key))
        )
    )
    assertCluster(
      () => result(ZookeeperApi.access.hostname(configuratorHostname).port(configuratorPort).list()),
      () =>
        result(
          ContainerApi.access
            .hostname(configuratorHostname)
            .port(configuratorPort)
            .get(cluster.key)
            .map(_.flatMap(_.containers))
        ),
      cluster.key
    )
    result(NodeApi.access.hostname(configuratorHostname).port(configuratorPort).list()).isEmpty shouldBe false
    result(NodeApi.access.hostname(configuratorHostname).port(configuratorPort).list()).foreach { node =>
      node.services.isEmpty shouldBe false
      withClue(s"${node.services}")(node.services.map(_.clusterKeys.size).sum > 0 shouldBe true)
    }
  }
}
