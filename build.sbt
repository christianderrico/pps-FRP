name := "pps-FRP"

version := "0.1"

scalaVersion := "2.13.7"

val AkkaVersion = "2.6.17"

val CatsEffectVersion = "3.2.9"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-stream" % AkkaVersion,
  "org.typelevel" %% "cats-effect" % CatsEffectVersion
)