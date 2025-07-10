// build.sbt
// SBT root build definition for the hardware spec management framework.
// This file defines the build for the core libraries (spec-core, spec-macros)
// and the SBT plugin (spec-plugin). The design project is now managed as a
// fully independent SBT project and is not included here.

// ---------- Global Settings ----------
ThisBuild / scalaVersion := "2.13.12" // Default Scala version for all modules
ThisBuild / organization := "your.company"
ThisBuild / version      := "0.1.0-SNAPSHOT"


// ---------- Sub-projects Definitions ----------

// spec-core: Core data model and types for the framework
lazy val specCore = (project in file("spec-core"))
  .settings(
    name := "spec-core"
  )

// spec-macros: Macro implementation for @LocalSpec and compile-time emission
lazy val specMacros = (project in file("spec-macros"))
  .dependsOn(specCore)
  .settings(
    name := "spec-macros"
  )

// spec-plugin: SBT plugin for aggregating and emitting spec/tag indices
lazy val specPlugin = (project in file("spec-plugin"))
  .settings(
    name := "spec-plugin"
  )

// Note: The design project is now a fully independent SBT project and is not defined here.

// ---------- Root Project Aggregation ----------
lazy val root = (project in file("."))
  // Only aggregate the core libraries; specPlugin and design are independent
  .aggregate(specCore, specMacros)
  .settings(
    publish / skip := true // Do not publish the root project
  )
