lazy val akkaHttpVersion = "10.1.1"
lazy val akkaVersion    = "2.5.12"

lazy val manager = (project in file(".")).
        settings(
          inThisBuild(List(
            organization    := "com.island",
            scalaVersion    := "2.12.4"
          )),
          name := "manager",
          libraryDependencies ++= Seq(
            "com.typesafe.akka" %% "akka-http"            % akkaHttpVersion,
            "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion,
            "com.typesafe.akka" %% "akka-stream"          % akkaVersion,

            "com.typesafe.akka" %% "akka-http-testkit"    % akkaHttpVersion % Test,
            "org.scalatest"     %% "scalatest"            % "3.0.5"         % Test,
            "junit"     % "junit"            % "4.12"         % Test
          )
        )
