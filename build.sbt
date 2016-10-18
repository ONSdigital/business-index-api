import sbtbuildinfo.BuildInfoPlugin.autoImport._
import sbtassembly.AssemblyPlugin.autoImport._

lazy val root = (project in file(".")).
  enablePlugins(BuildInfoPlugin).
  enablePlugins(PlayScala).
  settings(
    name := """ons-bi-api""",
    scalaVersion := "2.11.7",

    buildInfoPackage := "controllers",

    buildInfoKeys ++= Seq[BuildInfoKey](
      resolvers,
      libraryDependencies,
      BuildInfoKey.action("gitRevision") {
        ("git rev-parse --short HEAD" !!).trim
      }
    ),

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

    resolvers += "splunk" at "http://splunk.artifactoryonline.com/splunk/ext-releases-local",

    libraryDependencies ++= Seq(
      "ch.qos.logback" % "logback-classic" % "1.1.7",
      "com.splunk.logging" % "splunk-library-javalogging" % "1.5.2" excludeAll(
        ExclusionRule("commons-logging", "commons-logging"),
        ExclusionRule("org.apache.logging.log4j", "log4j-core"),
        ExclusionRule("org.apache.logging.log4j", "log4j-api")
        ),
      "com.typesafe.scala-logging" %% "scala-logging" % "3.5.0",
      "com.sksamuel.elastic4s" %% "elastic4s-streams" % "2.3.1",
      "com.sksamuel.elastic4s" %% "elastic4s-jackson" % "2.3.1",
      "org.scalatestplus.play" %% "scalatestplus-play" % "1.5.1" % Test
    )
  )