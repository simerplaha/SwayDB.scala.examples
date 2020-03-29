val swayDBVersion = "0.13.4"

//val scala211 = "2.11.12" //for 2.11 see branch scala_2.11
val scala212 = "2.12.11"
val scala213 = "2.13.1"

name := "SwayDB.scala.examples"

version := "0.1"

scalaVersion in ThisBuild := scala213

resolvers += Opts.resolver.sonatypeSnapshots
resolvers += Opts.resolver.sonatypeReleases
resolvers += Opts.resolver.sonatypeStaging

crossScalaVersions := Seq(scala212, scala213)

def scalaParallelCollections(scalaVersion: String) =
  CrossVersion.partialVersion(scalaVersion) match {
    case Some((2, major)) if major >= 13 =>
      Some("org.scala-lang.modules" %% "scala-parallel-collections" % "0.2.0" % Test)

    case _ =>
      None
  }

libraryDependencies ++=
  Seq(
    "com.typesafe.scala-logging" %% "scala-logging" % "3.9.2",
    "org.scalatest" %% "scalatest" % "3.0.8" % Test,
    "ch.qos.logback" % "logback-classic" % "1.2.3" % Test,
    "io.circe" %% "circe-core" % "0.12.0-M3" % Test,
    "io.circe" %% "circe-generic" % "0.12.0-M3" % Test,
    "io.circe" %% "circe-parser" % "0.12.0-M3" % Test,
    "org.junit.jupiter" % "junit-jupiter-api" % "5.5.2" % Test,
    "joda-time" % "joda-time" % "2.10.5" % Test,
    "io.swaydb" %% "swaydb" % swayDBVersion,
    "io.swaydb" %% "java" % swayDBVersion,
    "io.swaydb" %% "monix" % swayDBVersion,
    "io.swaydb" %% "zio" % swayDBVersion
  ) ++ scalaParallelCollections(scalaVersion.value)
