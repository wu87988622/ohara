lazy val akkaHttpVersion = "10.1.1"
lazy val akkaVersion = "2.5.12"

val formatAll   = taskKey[Unit]("Format all the source code which includes src, test, and build files")
val checkFormat = taskKey[Unit]("Check all the source code which includes src, test, and build files")

lazy val manager = (project in file(".")).
        settings(
          inThisBuild(List(
            organization := "com.island",
            scalaVersion := "2.12.6"
          )),
          name := "manager",
          libraryDependencies ++= Seq(
            "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
            "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion,
            "com.typesafe.akka" %% "akka-stream" % akkaVersion,

            "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion % Test,
            "org.scalatest" %% "scalatest" % "3.0.5" % Test,
            "junit" % "junit" % "4.12" % Test
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
          })
