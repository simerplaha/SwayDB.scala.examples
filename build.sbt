name := "SwayDB.examples"

version := "0.1"

scalaVersion := "2.12.10"

resolvers += Opts.resolver.sonatypeSnapshots
resolvers += Opts.resolver.sonatypeReleases
resolvers += Opts.resolver.sonatypeStaging

val swayDBVersion = "0.10.2"

libraryDependencies ++=
  Seq(
    "com.typesafe.scala-logging" %% "scala-logging" % "3.7.2",
    "org.scalatest" %% "scalatest" % "3.0.4" % Test,
    "org.scalamock" %% "scalamock-scalatest-support" % "3.6.0" % Test,
    "ch.qos.logback" % "logback-classic" % "1.2.3" % Test,
    "io.circe" %% "circe-core" % "0.10.0" % Test,
    "io.circe" %% "circe-generic" % "0.10.0" % Test,
    "io.circe" %% "circe-parser" % "0.10.0" % Test,
    "io.swaydb" %% "swaydb" % swayDBVersion,
    "io.swaydb" %% "monix" % swayDBVersion
  )
