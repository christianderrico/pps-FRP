name := "pps-FRP"

version := "0.1"

scalaVersion := "2.13.7"

val AkkaVersion = "2.6.17"

val MonixVersion =  "3.4.0"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-stream" % AkkaVersion,
  "io.monix" %% "monix" % MonixVersion
)