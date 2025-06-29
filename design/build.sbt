// design/build.sbt
// SBT build definition for a hardware design project using the spec-framework.
// This file demonstrates how to consume the spec-plugin and core libraries in a user project.
// Use this as a reference for integrating the framework as an SBT plugin in other projects.

// ------------------------------------------------------------------------------
// 1. Global project settings
// ------------------------------------------------------------------------------
ThisBuild / scalaVersion := "2.13.12"         // Set the Scala version for the project
ThisBuild / organization := "your.company"    // Set your organization
ThisBuild / version      := "0.1.0-SNAPSHOT"  // Project version

// ------------------------------------------------------------------------------
// 2. Project definition: enable the SpecPlugin and configure dependencies
// ------------------------------------------------------------------------------
lazy val design = (project in file("."))
  // Enable the spec-plugin (make sure it is published locally or available as a plugin dependency)
  .enablePlugins(SpecPlugin)
  .settings(
    name := "design", // Project name

    // --------------------------------------------------------------------------
    // 2.1. Add dependencies on the core framework modules and Chisel
    // --------------------------------------------------------------------------
    libraryDependencies ++= Seq(
      "your.company" %% "spec-core"   % "0.1.0-SNAPSHOT",   // Core types and registry
      "your.company" %% "spec-macros" % "0.1.0-SNAPSHOT",   // Macro annotation for compile-time emission
      "edu.berkeley.cs" %% "chisel3"  % "3.5.6"             // Chisel HDL (optional, for hardware design)
    ),

    // --------------------------------------------------------------------------
    // 2.2. Enable macro annotations (required for @LocalSpec and similar macros)
    // --------------------------------------------------------------------------
    Compile / scalacOptions += "-Ymacro-annotations",

    // --------------------------------------------------------------------------
    // 2.3. Set up the spec-meta directory for macro output and runtime access
    // --------------------------------------------------------------------------
    // Inject the spec-meta directory as a system property (once per project load)
    initialize := {
      val _  = initialize.value // Keep existing initialization
      val dir = (Compile / resourceManaged).value / "spec-meta"
      System.setProperty("spec.meta.dir", dir.getAbsolutePath)
    },

    // Ensure the spec-meta directory is included in the classpath/resources
    Compile / resourceGenerators += Def.task {
      (Compile / resourceManaged).value / "spec-meta"
      Seq.empty[File]
    }.taskValue,

    // Do not publish this project by default
    publish / skip := true
  )

// ------------------------------------------------------------------------------
// Reference: How to use the spec-framework as a plugin in your own project
// ------------------------------------------------------------------------------
// 1. Add the published spec-plugin to your project's plugins.sbt:
//    addSbtPlugin("your.company" % "spec-plugin" % "0.1.0-SNAPSHOT")
// 2. Add the core and macro libraries to your build.sbt as shown above.
// 3. Enable the plugin in your project definition:
//    .enablePlugins(SpecPlugin)
// 4. Ensure macro annotations are enabled and the spec-meta directory is set up.
// 5. Use the macro-based DSL and SBT tasks as described in the framework documentation.
