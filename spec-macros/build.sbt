// spec-macros/build.sbt

// Project name
name := "spec-macros"

// Depend on spec-core via published local artifact (not project dependency)
// '%%' ensures the correct Scala versioned artifact is used
libraryDependencies += "your.company" %% "spec-core" % "0.1.0-SNAPSHOT"

// Add Scala reflection for macro support
libraryDependencies += "org.scala-lang" % "scala-reflect" % scalaVersion.value

// Enable macro annotations in the compiler
Compile / scalacOptions += "-Ymacro-annotations"

// Cross-build for Scala 2.13 only (add Scala 3 here if needed in the future)
crossScalaVersions := Seq("2.13.12")
