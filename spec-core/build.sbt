// spec-core/build.sbt

// Project name
name := "spec-core"

// Pure data model + DSL + checker. The only dependency is upickle (which
// publishes artefacts for Scala 2.12, 2.13 and 3), so this module is macro-free
// and cross-builds to all three. (The macros live in spec-macros.)
// upickle 3.x (matches the Chisel ecosystem, and publishes for 2.12/2.13/3).
libraryDependencies += "com.lihaoyi" %% "upickle" % "3.3.1"

// Cross-build for Scala 2.13, 2.12 and 3.
crossScalaVersions := Seq("2.13.12", "2.12.19", "3.3.4")
