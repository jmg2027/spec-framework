// spec-core/build.sbt

// Project name
name := "spec-core"

// Pure data model + DSL + checker. The only dependency is upickle, so this
// module is macro-free and cross-builds to 2.13 and 2.12 (the 2.12 build is
// what the sbt plugin consumes). The macros live in spec-macros.
// upickle 3.x (matches the Chisel ecosystem).
libraryDependencies += "com.lihaoyi" %% "upickle" % "3.3.1"

// Cross-build for Scala 2.13 and 2.12.
crossScalaVersions := Seq("2.13.12", "2.12.19")
