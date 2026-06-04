// examples/plugin-consumer/build.sbt
// A standalone consumer build that uses the published spec-plugin end to end.
// Run:  sbt exportSpecIndex   (emits SpecIndex.json / TagIndex.json / properties.sva)

ThisBuild / scalaVersion := "2.13.12"
ThisBuild / organization := "consumer"
ThisBuild / version      := "0.1.0"

lazy val root = (project in file("."))
  .enablePlugins(SpecPlugin)
  .settings(
    name := "plugin-consumer",
    libraryDependencies ++= Seq(
      "your.company" %% "spec-core"   % "0.1.0-SNAPSHOT",
      "your.company" %% "spec-macros" % "0.1.0-SNAPSHOT",
    ),
    Compile / scalacOptions += "-Ymacro-annotations",
    // Point the macro emission at the directory the plugin aggregates.
    initialize := {
      val _   = initialize.value
      val dir = (Compile / resourceManaged).value / "spec-meta"
      System.setProperty("spec.meta.dir", dir.getAbsolutePath)
    },
    publish / skip := true,
  )
