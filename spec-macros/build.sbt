// spec-macros/build.sbt

name := "spec-macros"

// Depend directly on the specCore project defined in the root build.
dependsOn(LocalProject("specCore"))

// Add Scala reflection for macro support
libraryDependencies += "org.scala-lang" % "scala-reflect" % scalaVersion.value

// Enable macro annotations in the compiler
Compile / scalacOptions += "-Ymacro-annotations"

// Scala 2.13 only: the macros are Scala 2 def-macros + the @LocalSpec
// macro-annotation, and the real consumer — Chisel — publishes for 2.13.
crossScalaVersions := Seq("2.13.12")
