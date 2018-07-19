resolvers += Resolver.typesafeRepo("releases")

// Default sbt
addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.5.14")

addSbtPlugin("com.typesafe.sbt" % "sbt-git" % "0.9.3")

addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.7.0")

addSbtPlugin("com.typesafe.sbt" % "sbt-gzip" % "1.0.0")

addSbtPlugin("com.dwijnand" % "sbt-dynver" % "1.1.1")

addSbtPlugin("com.timushev.sbt" % "sbt-updates" % "0.3.0")

// Build

addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.14.5")

addSbtPlugin("net.virtual-void" % "sbt-dependency-graph" % "0.8.2")

// CI

addSbtPlugin("com.github.gseitz" % "sbt-release" % "1.0.5")

// revision

addSbtPlugin("org.scoverage" % "sbt-scoverage" % "1.5.0")

addSbtPlugin("org.scoverage" % "sbt-coveralls" % "1.1.0")

addSbtPlugin("com.codacy" % "sbt-codacy-coverage" % "1.3.8")


// style

addSbtPlugin("org.scalariform" % "sbt-scalariform" % "1.6.0")


// test

addSbtPlugin("com.sksamuel.scapegoat" %% "sbt-scapegoat" % "1.0.4")

addSbtPlugin("org.scalastyle" % "scalastyle-sbt-plugin" % "0.8.0")

// https://github.com/sbt/sbt/issues/1931
libraryDependencies += "org.slf4j" % "slf4j-nop" % "1.7.21"