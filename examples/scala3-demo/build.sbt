// examples/scala3-demo/build.sbt
// Proves the framework's macros work on Scala 3. Specs are emitted at run time
// (Scala 3 has no compile-time c.eval), tags at compile time, then SpecCheck
// aggregates — producing the same indices as the Scala 2 path.

ThisBuild / scalaVersion := "3.3.4"
ThisBuild / organization := "consumer3"
ThisBuild / version      := "0.1.0"

lazy val root = (project in file("."))
  .settings(
    name := "scala3-demo",
    libraryDependencies ++= Seq(
      "your.company" %% "spec-core"   % "0.1.0-SNAPSHOT",
      "your.company" %% "spec-macros" % "0.1.0-SNAPSHOT",
    ),
    initialize := {
      val _   = initialize.value
      val dir = (Compile / resourceManaged).value / "spec-meta"
      System.setProperty("spec.meta.dir", dir.getAbsolutePath)
    },
    publish / skip := true,
  )
