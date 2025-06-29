// spec-plugin/build.sbt
// SBT build definition for the spec-plugin module.
// This file configures the SBT plugin project, Scala version, and dependencies.

sbtPlugin := true
enablePlugins(SbtPlugin)

// Restrict plugin to a single Scala version (required for sbt 1.x plugins)
scalaVersion       := "2.12.19"
crossScalaVersions := Seq("2.12.19")

// Add dependency on the core module (must be published locally for 2.12)
libraryDependencies += "your.company" %% "spec-core" % "0.1.0-SNAPSHOT"

// Additional scalac options (uncomment if needed)
// scalacOptions += "-deprecation"
