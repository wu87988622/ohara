package com.island.ohara.client.configurator.v0

trait ClusterInfo {
  def name: String
  def imageName: String
  def nodeNames: Seq[String]
}
