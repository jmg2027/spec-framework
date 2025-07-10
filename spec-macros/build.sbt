// spec-macros/build.sbt

// Project name
name := "spec-macros"

// Depend directly on the specCore project defined in the root build.
dependsOn(LocalProject("specCore"))

// Add Scala reflection for macro support
libraryDependencies += "org.scala-lang" % "scala-reflect" % scalaVersion.value

// Enable macro annotations in the compiler
Compile / scalacOptions += "-Ymacro-annotations"

// Cross-build for Scala 2.13 only (add Scala 3 here if needed in the future)
crossScalaVersions := Seq("2.13.12")
