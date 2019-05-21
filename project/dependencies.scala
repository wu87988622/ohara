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

import sbt._

object versions {
  val akkaHttpV = "10.1.3"
  val akkaV = "2.5.12"
  val kafkaV = "1.0.2"
  val slf4jV = "1.7.25"
  val hadoopV = "2.7.0"
  val commonsNetV = "3.6"
  val commonsLangV = "3.7"
  val commonsIoV = "2.4"
  val mysqlV = "8.0.12"
  val embeddedsqlV = "4.1.2"
  val ftpServerV = "1.1.1"
  val sshdV = "2.2.0"
  val javassistV = "3.24.1-GA"
  val rocksdbV = "5.7.3"
}

object libs {

  import versions._

  // Hadoop
  lazy val hadoopCommon = "org.apache.hadoop" % "hadoop-common" % hadoopV
  lazy val hadoopHdfs = "org.apache.hadoop" % "hadoop-hdfs" % hadoopV

  // Akka
  lazy val akkaActor = "com.typesafe.akka" %% "akka-actor" % akkaV
  lazy val akkaStream = "com.typesafe.akka" %% "akka-stream" % akkaV
  lazy val akkaHttp = "com.typesafe.akka" %% "akka-http" % akkaHttpV
  lazy val akkaHttpSprayJson = "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpV
  lazy val akkaSlf4j = "com.typesafe.akka" %% "akka-slf4j" % akkaHttpV
  lazy val akkaHttpTestKit = "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpV
  lazy val akkaTestKit = "com.typesafe.akka" %% "akka-testkit" % akkaV
  lazy val akkaStreamTestKit = "com.typesafe.akka" %% "akka-stream-testkit" % akkaV

  // Kafka
  lazy val kafkaCore = "org.apache.kafka" %% "kafka" % kafkaV
  lazy val kafkaClient = "org.apache.kafka" % "kafka-clients" % kafkaV
  lazy val kafkaConnectApi = "org.apache.kafka" % "connect-api" % kafkaV
  lazy val kafkaConnectFile = "org.apache.kafka" % "connect-file" % kafkaV
  lazy val kafkaConnectRuntime = "org.apache.kafka" % "connect-runtime" % kafkaV
  lazy val kafkaConnectJson = "org.apache.kafka" % "connect-json" % kafkaV
  lazy val kafkaStreams = "org.apache.kafka" % "kafka-streams" % kafkaV

  // Apache commons
  lazy val commonsNet = "commons-net" % "commons-net" % commonsNetV
  lazy val commonsLang = "org.apache.commons" % "commons-lang3" % commonsLangV
  lazy val commonsIo = "commons-io" % "commons-io" % commonsIoV

  // Loggin
  lazy val slf4jApi = "org.slf4j" % "slf4j-api" % slf4jV
  lazy val slf4jLog4j = "org.slf4j" % "slf4j-log4j12" % slf4jV
  lazy val scalaLogging = "com.typesafe.scala-logging" %% "scala-logging" % "3.9.0"

  // Testing
  lazy val scalatest = "org.scalatest" %% "scalatest" % "3.0.5"
  lazy val mockito = "org.mockito" % "mockito-all" % "1.10.19"
  lazy val junit = "junit" % "junit" % "4.12"
  lazy val javassist = "org.javassist" % "javassist" % javassistV
  lazy val junitInterface = "com.novocode" % "junit-interface" % "0.11"

  // Others
  lazy val typesafeConfig = "com.typesafe" % "config" % "1.3.3"
  lazy val guava = "com.google.guava" % "guava" % "20.0"
  lazy val rocksdb = "org.rocksdb" % "rocksdbjni" % rocksdbV

  // mini
  lazy val mysql = "mysql" % "mysql-connector-java" % mysqlV
  lazy val embeddedsql = "com.wix" % "wix-embedded-mysql" % embeddedsqlV
  lazy val ftpServer = "org.apache.ftpserver" % "ftpserver-core" % ftpServerV
  lazy val sshd = "org.apache.sshd" % "apache-sshd" % sshdV

  // exclude older libraries from Hadoop
  val hadoopExclusionRules = Seq(
    ExclusionRule("com.sun.jersey", "jersey-core"),
    ExclusionRule("com.sun.jersey", "jersey-json"),
    ExclusionRule("com.sun.jersey", "jersey-servlet"),
    ExclusionRule("com.sun.jersey", "jersey-server"),
    ExclusionRule("com.google.guava", "guava"),
    ExclusionRule("com.jcraft", "jsch")
  )

  val sshdExclusionRules = Seq(
    ExclusionRule("org.apache.sshd", "sshd-mina"),
    ExclusionRule("org.apache.sshd", "sshd-netty")
  )


}
