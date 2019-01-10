
name := "ohara"

version := "0.0.1"

organizationName := "is-land Systems Inc."

organizationHomepage := Some(url("https://github.com/is-land"))

organization := "com.island"

cancelable in Global := true

val cpus = sys.runtime.availableProcessors

scalacOptions ++= Seq(
  // Scala Compiler Options
  // https://github.com/scala/scala/blob/2.12.x/src/compiler/scala/tools/nsc/settings/StandardScalaSettings.scala
  // https://github.com/scala/scala/blob/2.12.x/src/compiler/scala/tools/nsc/settings/ScalaSettings.scala
  "-deprecation",
  "-unchecked",
  "-encoding", "utf8",
  "-Xlog-reflective-calls",
  "-feature",
  "-language:postfixOps",
  "-language:implicitConversions",
  "-language:existentials",
  "-Xlint:by-name-right-associative",
  "-Xlint:delayedinit-select",
  "-Xlint:doc-detached",
  "-Xlint:missing-interpolator",
  "-Xlint:nullary-override",
  "-Xlint:nullary-unit",
  "-Xlint:option-implicit",
  "-Xlint:package-object-classes",
  "-Xlint:poly-implicit-overload",
  "-Xlint:private-shadow",
  "-Xlint:stars-align",
  "-Xlint:type-parameter-shadow",
  "-Xlint:unsound-match",
  "-target:jvm-1.8",
  "-explaintypes",
  "-feature",
  "-unchecked",
  "-Xlint",
  "-Ywarn-dead-code",
  "-Ywarn-unused:implicits",
  "-Ywarn-unused:imports",
  "-Ywarn-unused:locals",
  "-Ywarn-unused:params",
  "-Ywarn-unused:patvars",
  "-Ywarn-unused:privates",
  "-Ywarn-value-discard",
  "-Ybackend-parallelism", cpus.toString
)


val formatAll = taskKey[Unit]("Format all the source code which includes src, test, and build files")
val checkFormat = taskKey[Unit]("Check all the source code which includes src, test, and build files")

lazy val commonSettings = Seq(
  scalaVersion := "2.11.12",
  resolvers += Resolver.bintrayRepo("cakesolutions", "maven"),
  resolvers += "Cloudera" at "https://repository.cloudera.com/artifactory/cloudera-repos/",
  fork in Test := true,
  fork in run := true,
  run / connectInput := true,
  javaOptions ++= Seq("-Xms256m", "-Xmx4g", "-XX:MaxMetaspaceSize=512m"),
  libraryDependencies ++= Seq(
    libs.junit % Test,
    libs.mockito % Test,
    libs.scalatest % Test
  ),
  scalafmtOnCompile := true,
  formatAll := {
    (scalafmt in Compile).value
    (scalafmt in Test).value
    (scalafmtSbt in Compile).value
  },
  checkFormat := {
    (scalafmtCheck in Compile).value
    (scalafmtCheck in Test).value
    (scalafmtSbtCheck in Compile).value
  },
  scalafmtOnCompile := true
)

concurrentRestrictions in Global := Seq(Tags.limitAll(1))

lazy val `common` = (project in file("ohara-common"))
        .settings(
          commonSettings,
          libraryDependencies ++= Seq(
            libs.commonsNet,
            libs.commonsLang,
            libs.commonsIo,
            libs.scalaLogging,
            libs.slf4jApi,
            libs.slf4jLog4j,
            libs.akkaHttp,
            libs.akkaStream,
            libs.akkaActor,
            libs.akkaHttpSprayJson
          )
        )


lazy val `testing-util` = (project in file("ohara-testing-util"))
        .dependsOn(
          `common` % "compile->compile; test->test"
        )
        .settings(
          commonSettings,
          libraryDependencies ++= Seq(
            libs.kafkaCore,
            libs.kafkaConnectJson,
            libs.kafkaConnectRuntime,
            libs.hadoopCommon excludeAll(libs.hadoopExclusionRules:_*),
            libs.hadoopHdfs excludeAll(libs.hadoopExclusionRules:_*),
            libs.mysql,
            libs.embeddedsql,
            libs.ftpServer
          ),
          excludeDependencies ++= libs.hadoopExclusionRules
        )


lazy val `kafka` = (project in file("ohara-kafka"))
        .dependsOn(
          `common` % "compile->compile; test->test",
          `testing-util` % "test->test"
        )
        .settings(
          commonSettings,
          libraryDependencies ++= Seq(
            libs.kafkaClient,
            libs.kafkaConnectFile,
            libs.kafkaConnectRuntime,
            libs.zookeeper,
            libs.scalaLogging,
            libs.slf4jApi,
            libs.slf4jLog4j,
            libs.akkaHttpSprayJson % Test
          )
        )


lazy val `hdfs-connector` = (project in file("ohara-hdfs-connector"))
        .dependsOn(
          `kafka` % "compile->compile",
          `common` % "test->test",
          `testing-util` % "test->test"
        )
        .settings(
          commonSettings,
          libraryDependencies ++= Seq(
            libs.hadoopCommon excludeAll(libs.hadoopExclusionRules:_*),
            libs.hadoopHdfs excludeAll(libs.hadoopExclusionRules:_*)
          )
        )

lazy val `jdbc-connector` = (project in file("ohara-jdbc-connector"))
        .dependsOn(
          `kafka` % "compile->compile",
          `common` % "test->test",
          `testing-util` % "test->test"
        )
        .settings(
          commonSettings,
          libraryDependencies ++= Seq(
          )
        )

lazy val `ftp-connector` = (project in file("ohara-ftp-connector"))
        .dependsOn(
          `kafka` % "compile->compile",
          `common` % "test->test",
          `testing-util` % "test->test"
        )
        .settings(
          commonSettings,
          libraryDependencies ++= Seq(
          )
        )

lazy val `configurator` = (project in file("ohara-configurator"))
        .dependsOn(
          `common` % "compile->compile; test->test",
          `kafka` % "compile->compile",
          `testing-util` % "test->test"
        )
        .settings(
          commonSettings,
          libraryDependencies ++= Seq(
            libs.hadoopCommon excludeAll(libs.hadoopExclusionRules:_*),
            libs.hadoopHdfs excludeAll(libs.hadoopExclusionRules:_*),
            libs.kafkaClient,
            libs.kafkaConnectFile,
            libs.kafkaConnectRuntime,

            libs.zookeeper,
            libs.scalaLogging,
            libs.slf4jApi,
            libs.slf4jLog4j,
            libs.akkaHttpSprayJson
          )
        )


lazy val `shabondi` = (project in file("ohara-shabondi"))
        .dependsOn(
        )
        .settings(
          commonSettings,
          libraryDependencies ++= Seq(
            libs.akkaHttp,
            libs.akkaStream,
            libs.akkaHttpSprayJson,

            libs.akkaHttpTestKit % Test
          )
        )

lazy val `streams` = (project in file("ohara-streams"))
        .dependsOn(
          `common` % "compile->compile; test->test",
          `kafka` % "compile->compile",
          `testing-util` % "test->test"
        )
        .settings(
          commonSettings,
          libraryDependencies ++= Seq(
            libs.scalaLogging,
            libs.slf4jApi,
            libs.slf4jLog4j,
            libs.akkaHttpSprayJson,
            libs.kafkaStreams,

            libs.hadoopCommon % Test excludeAll(libs.hadoopExclusionRules:_*),
            libs.hadoopHdfs %Test excludeAll(libs.hadoopExclusionRules:_*),
          )
        )
