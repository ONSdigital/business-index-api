name := """ons-bi-api"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.7"

libraryDependencies ++= Seq(
  cache,
  ws,
  "ch.qos.logback" %  "logback-classic" % "1.1.7",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.5.0",
  "com.sksamuel.elastic4s" %% "elastic4s-streams" % "1.7.4",
  "org.scalatestplus.play" %% "scalatestplus-play" % "1.5.1" % Test
)

