import play.sbt.PlayScala
import sbtassembly.AssemblyPlugin.autoImport._
import sbtbuildinfo.BuildInfoPlugin.autoImport._

lazy val Versions = new {
  val util = "0.27.8"
  val elastic4s = "2.3.1"
  val spark = "1.6.0"
  val elasticSearchSpark = "2.4.0"
}

lazy val commonSettings = Seq(
  scalaVersion := "2.11.8",
  resolvers ++= Seq(
    Resolver.bintrayRepo("outworkers", "oss-releases"),
    "splunk" at "http://splunk.artifactoryonline.com/splunk/ext-releases-local"
  ),
  scalacOptions in ThisBuild ++= Seq(
    "-language:experimental.macros",
    "-target:jvm-1.8",
    "-encoding", "UTF-8",
    "-language:reflectiveCalls",
    "-language:experimental.macros",
    "-language:implicitConversions",
    "-language:higherKinds",
    "-language:postfixOps",
    "-deprecation", // warning and location for usages of deprecated APIs
    "-feature", // warning and location for usages of features that should be imported explicitly
    "-unchecked", // additional warnings where generated code depends on assumptions
    "-Xlint", // recommended additional warnings
    "-Xcheckinit", // runtime error when a val is not initialized due to trait hierarchies (instead of NPE somewhere else)
    "-Ywarn-adapted-args", // Warn if an argument list is modified to match the receiver
    //"-Yno-adapted-args", // Do not adapt an argument list (either by inserting () or creating a tuple) to match the receiver
    "-Ywarn-value-discard", // Warn when non-Unit expression results are unused
    "-Ywarn-inaccessible", // Warn about inaccessible types in method signatures
    "-Ywarn-dead-code", // Warn when dead code is identified
    "-Ywarn-unused", // Warn when local and private vals, vars, defs, and types are unused
    "-Ywarn-unused-import", //  Warn when imports are unused (don't want IntelliJ to do it automatically)
    "-Ywarn-numeric-widen" // Warn when numerics are widened
  )
)

/**
  * The multi-module separation is necessary because the parsers module uses macros.
  * In order to use macros, they cannot be part of the same compilation unit that defines them,
  * meaning you cannot define and use a macro in the same module.
  *
  * This is why we separate parsers in their own entity, to make sure they are not compiled together
  * with any module that attempts to use them.
  */
lazy val businessIndex = (project in file("."))
  .settings(commonSettings: _*)
  .settings(
    name := "ons-bi",
    moduleName := "ons-bi"
  ).aggregate(api)

lazy val api = (project in file("api"))
  .enablePlugins(BuildInfoPlugin, PlayScala)
  .settings(commonSettings: _*)
  .settings(
    name := "ons-bi-api",
    scalaVersion := "2.11.8",
    buildInfoPackage := "controllers",
    resolvers ++= Seq(
      "Hadoop Releases" at "https://repository.cloudera.com/content/repositories/releases/"
    ),
    javaOptions in Test += "-Denvironment=local",
    fork in run := true,
    buildInfoKeys ++= Seq[BuildInfoKey](
      resolvers,
      libraryDependencies,
      BuildInfoKey.action("gitRevision") {
        ("git rev-parse --short HEAD" !!).trim
      }
    ),
    routesGenerator := InjectedRoutesGenerator,
    buildInfoOptions += BuildInfoOption.BuildTime,
    buildInfoOptions += BuildInfoOption.ToJson,

    // no javadoc for BuildInfo.scala
    sources in(Compile, doc) <<= sources in(Compile, doc) map {
      _.filterNot(_.getName endsWith ".scala")
    },

    assemblyJarName in assembly := "ons-bi-api.jar",
    assemblyMergeStrategy in assembly := {
      case PathList("META-INF", "io.netty.versions.properties", xs@_ *) => MergeStrategy.last
      case PathList("org", "joda", "time", "base", "BaseDateTime.class") => MergeStrategy.first // ES shades Joda
      case x =>
        val oldStrategy = (assemblyMergeStrategy in assembly).value
        oldStrategy(x)
    },
    mainClass in assembly := Some("play.core.server.ProdServerStart"),
    fullClasspath in assembly += Attributed.blank(PlayKeys.playPackageAssets.value),
    libraryDependencies ++= Seq(
      filters,
      "org.webjars" %% "webjars-play" % "2.5.0-3",
      "org.webjars.bower" % "angular" % "1.5.9",
      "org.webjars.bower" % "dali" % "1.3.2",
      "org.webjars.bower" % "angular-toggle-switch" % "1.3.0",
      "org.webjars.bower" % "angular-bootstrap" % "1.1.0",
      "org.webjars.bower" % "angular-ui-router" % "0.2.15",
      "com.typesafe.scala-logging" %% "scala-logging" % "3.5.0",
      "ch.qos.logback" % "logback-classic" % "1.1.7",
      "com.splunk.logging" % "splunk-library-javalogging" % "1.5.2" excludeAll(
        ExclusionRule("commons-logging", "commons-logging"),
        ExclusionRule("org.apache.logging.log4j", "log4j-core"),
        ExclusionRule("org.apache.logging.log4j", "log4j-api")
      ),
      "nl.grons" %% "metrics-scala" % "3.5.5",
      "com.sksamuel.elastic4s" %% "elastic4s-streams" % Versions.elastic4s,
      "com.sksamuel.elastic4s" %% "elastic4s-jackson" % Versions.elastic4s,
      "com.outworkers" %% "util-parsers-cats" % Versions.util,
      "com.outworkers" %% "util-play" % Versions.util,
      "com.outworkers" %% "util-testing" % Versions.util % Test,
      "org.scalatestplus.play" %% "scalatestplus-play" % "2.0.0-M1" % Test,
      "com.google.guava" % "guava" % "18.0",
      "org.apache.hadoop" % "hadoop-common" % "2.6.0",
      "org.apache.hadoop" % "hadoop-mapred" % "0.22.0",
      "org.apache.hbase" % "hbase-common" % "1.3.0",
      "org.apache.hbase" % "hbase-client" % "1.3.0"
    ),

    dependencyOverrides += "com.google.guava" % "guava" % "18.0"
  )
