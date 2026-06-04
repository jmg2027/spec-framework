// spec-core/build.sbt

// Project name
name := "spec-core"

// Pure data model + DSL + checker. The only dependency is upickle (which
// publishes artefacts for Scala 2.12, 2.13 and 3), so this module is macro-free
// and cross-builds to all three. (The macros live in spec-macros.)
libraryDependencies += "com.lihaoyi" %% "upickle" % "2.0.0"

// Cross-build for Scala 2.13, 2.12 and 3.
crossScalaVersions := Seq("2.13.12", "2.12.19", "3.3.4")
