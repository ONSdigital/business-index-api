import sbtbuildinfo.BuildInfoPlugin.autoImport._
import sbtassembly.AssemblyPlugin.autoImport._

scalacOptions in ThisBuild ++= Seq(
  "-target:jvm-1.8",
  "-encoding", "UTF-8",
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

lazy val root = (project in file(".")).
  enablePlugins(BuildInfoPlugin).
  enablePlugins(PlayScala).
  settings(
    name := """ons-bi-api""",
    scalaVersion := "2.11.8",

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
    mainClass in assembly := Some("play.core.server.NettyServer"),

    resolvers += "splunk" at "http://splunk.artifactoryonline.com/splunk/ext-releases-local",

    libraryDependencies ++= Seq(
      filters,
      "org.webjars" %% "webjars-play" % "2.5.0-3",
      "org.webjars.bower" % "angular" % "1.5.8",
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
      "com.sksamuel.elastic4s" %% "elastic4s-streams" % "2.4.0",
      "com.sksamuel.elastic4s" %% "elastic4s-jackson" % "2.4.0",
      "org.scalatestplus.play" %% "scalatestplus-play" % "2.0.0-M1" % Test
    )
  )
