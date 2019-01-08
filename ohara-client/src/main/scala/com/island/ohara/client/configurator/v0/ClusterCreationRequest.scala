package com.island.ohara.client.configurator.v0

trait ClusterCreationRequest {
  def name: String
  def nodeNames: Seq[String]
}
