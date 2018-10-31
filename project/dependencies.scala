import sbt._

object versions {
  lazy val akkaHttpV = "10.1.3"
  lazy val akkaV = "2.5.12"
  lazy val kafkaV = "1.0.1"
  lazy val slf4jV = "1.7.25"
  lazy val hadoopV = "2.7.0"
  lazy val commonsNetV = "3.6"
  lazy val commonsLangV = "3.7"
  lazy val zookeeperV = "3.4.10"
  lazy val mysqlV = "8.0.12"
  lazy val embeddedsqlV = "4.1.2"
  lazy val ftpServerV = "1.1.1"
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

  // Loggin
  lazy val slf4jApi = "org.slf4j" % "slf4j-api" % slf4jV
  lazy val slf4jLog4j = "org.slf4j" % "slf4j-log4j12" % slf4jV
  lazy val scalaLogging = "com.typesafe.scala-logging" %% "scala-logging" % "3.9.0"

  // Testing
  lazy val scalatest = "org.scalatest" %% "scalatest" % "3.0.5"
  lazy val mockito = "org.mockito" % "mockito-all" % "1.10.19"
  lazy val junit = "junit" % "junit" % "4.12"

  // Others
  lazy val typesafeConfig = "com.typesafe" % "config" % "1.3.3"

  // mini
  lazy val zookeeper = "org.apache.zookeeper" % "zookeeper" % versions.zookeeperV
  lazy val mysql =  "mysql" % "mysql-connector-java" % mysqlV
  lazy val embeddedsql = "com.wix" % "wix-embedded-mysql" % embeddedsqlV
  lazy val ftpServer = "org.apache.ftpserver" % "ftpserver-core" % ftpServerV

  val hadoopExclusionRules = Seq(
    ExclusionRule("com.sun.jersey", "jersey-core"),
    ExclusionRule("com.sun.jersey", "jersey-json"),
    ExclusionRule("com.sun.jersey", "jersey-servlet"),
    ExclusionRule("com.sun.jersey", "jersey-server")
  )


}
