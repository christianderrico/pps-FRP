name := "pps-FRP"

version := "0.1"

scalaVersion := "2.13.7"

val AkkaVersion = "2.6.17"
val MonixVersion =  "3.4.0"
val ScalaTestVersion = "3.1.0"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-stream" % AkkaVersion,
  "io.monix" %% "monix" % MonixVersion,
  "org.scalatest" %% "scalatest" % ScalaTestVersion % Test,
  "com.typesafe.akka" %% "akka-testkit" % AkkaVersion % Test,
  "com.typesafe.akka" %% "akka-actor-testkit-typed" % AkkaVersion % Test,
  "com.typesafe.akka" %% "akka-stream-testkit" % AkkaVersion % Test
)

scalacOptions ++= Seq(
  "-feature",
  "-language:implicitConversions",
  "-language:postfixOps"
)