name := "SwayDB.examples"

version := "0.1"

scalaVersion := "2.13.1"

resolvers += Opts.resolver.sonatypeSnapshots
resolvers += Opts.resolver.sonatypeReleases
resolvers += Opts.resolver.sonatypeStaging

val swayDBVersion = "0.12-RC3"

libraryDependencies ++=
  Seq(
    "com.typesafe.scala-logging" %% "scala-logging" % "3.9.2",
    "org.scalatest" %% "scalatest" % "3.0.8" % Test,
    "ch.qos.logback" % "logback-classic" % "1.2.3" % Test,
    "io.circe" %% "circe-core" % "0.12.2" % Test,
    "io.circe" %% "circe-generic" % "0.12.2" % Test,
    "io.circe" %% "circe-parser" % "0.12.2" % Test,
    "org.junit.jupiter" % "junit-jupiter-api" % "5.5.2" % Test,
    "org.scala-lang.modules" %% "scala-parallel-collections" % "0.2.0",
    "io.swaydb" %% "swaydb" % swayDBVersion,
    "io.swaydb" %% "monix" % swayDBVersion,
    "io.swaydb" %% "zio" % swayDBVersion
  )
