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

package com.island.ohara.agent.k8s

import com.island.ohara.agent.k8s.K8SClient.ImagePullPolicy
import com.island.ohara.agent.k8s.K8SJson.CreatePodContainer
import com.island.ohara.common.rule.SmallTest
import org.junit.Test
import org.scalatest.Matchers
import spray.json._

class TestK8SClient extends SmallTest with Matchers {

  @Test
  def testCreatorEnumator(): Unit = {
    ImagePullPolicy.ALWAYS.toString shouldBe "Always"
    ImagePullPolicy.IFNOTPRESENT.toString shouldBe "IfNotPresent"
    ImagePullPolicy.NEVER.toString shouldBe "Never"
  }

  @Test
  def testCreatePodContainerNonePolicy(): Unit = {
    val json: String =
      CreatePodContainer("podName", "image", Seq(), Seq(), ImagePullPolicy.IFNOTPRESENT, Seq("hello"), Seq("world")).toJson.toString
    json shouldBe "{\"name\":\"podName\",\"image\":\"image\",\"ports\":[],\"command\":[\"hello\"],\"args\":[\"world\"],\"imagePullPolicy\":\"IfNotPresent\",\"env\":[]}"
  }

  @Test
  def testPolicyIsAlways(): Unit = {
    val json: String =
      CreatePodContainer("podName", "image", Seq(), Seq(), ImagePullPolicy.ALWAYS, Seq(), Seq()).toJson.toString
    json shouldBe "{\"name\":\"podName\",\"image\":\"image\",\"ports\":[],\"command\":[],\"args\":[],\"imagePullPolicy\":\"Always\",\"env\":[]}"
  }

  @Test
  def testPolicyIsNever(): Unit = {
    val json: String =
      CreatePodContainer("podName", "image", Seq(), Seq(), ImagePullPolicy.NEVER, Seq("foo"), Seq("bar")).toJson.toString
    json shouldBe "{\"name\":\"podName\",\"image\":\"image\",\"ports\":[],\"command\":[\"foo\"],\"args\":[\"bar\"],\"imagePullPolicy\":\"Never\",\"env\":[]}"
  }

  @Test
  def testPolicyIsIfNotPresent(): Unit = {
    val json: String =
      CreatePodContainer("podName", "image", Seq(), Seq(), ImagePullPolicy.IFNOTPRESENT, Seq(), Seq()).toJson.toString
    json shouldBe "{\"name\":\"podName\",\"image\":\"image\",\"ports\":[],\"command\":[],\"args\":[],\"imagePullPolicy\":\"IfNotPresent\",\"env\":[]}"
  }
}
