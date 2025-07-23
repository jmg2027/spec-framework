# SpecPlugin Usage (English)

This document explains how to apply the SpecPlugin to your project based on the current codebase.

## 1. Adding the plugin

Add the following line to `project/plugins.sbt` to load the sbt plugin:

```scala
addSbtPlugin("your.company" % "spec-plugin" % "0.1.0-SNAPSHOT")
```

## 2. Library dependencies

Inside your `build.sbt` project settings add the following libraries:

```scala
libraryDependencies ++= Seq(
  "your.company" %% "spec-core"   % "0.1.0-SNAPSHOT",
  "your.company" %% "spec-macros" % "0.1.0-SNAPSHOT",
  "edu.berkeley.cs" %% "chisel3"  % "3.6.0"            // if needed
)
```

## 3. Enabling the plugin and options

Enable `SpecPlugin` and configure macro options in your project definition:

```scala
lazy val design = (project in file("."))
  .enablePlugins(SpecPlugin)
  .settings(
    Compile / scalacOptions += "-Ymacro-annotations",
    initialize := {
      val _  = initialize.value
      val dir = (Compile / resourceManaged).value / "spec-meta"
      System.setProperty("spec.meta.dir", dir.getAbsolutePath)
    },
    publish / skip := true
  )
```

This sets the `spec.meta.dir` system property so the macros emit metadata to that location.

## 4. Generating the index

After compilation run the following task:

```bash
sbt exportSpecIndex
```

`SpecIndex.json` and `TagIndex.json` will appear under the directory specified by `spec.meta.dir` (default `design/target/`).
