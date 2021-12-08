name := "pps-FRP"

version := "0.1"

scalaVersion := "2.13.7"

val AkkaVersion = "2.6.17"
val MonixVersion =  "3.4.0"
val AkkaHttpVersion = "10.2.7"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-stream" % AkkaVersion,
  "io.monix" %% "monix" % MonixVersion,
  "org.scalatest" %% "scalatest" % "3.1.0" % Test,
  "com.typesafe.akka" %% "akka-http" % AkkaHttpVersion
)