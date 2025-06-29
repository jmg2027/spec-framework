// spec-core/build.sbt

// Project name
name := "spec-core"

// Library dependencies for core functionality and macro support
libraryDependencies ++= Seq(
  "com.lihaoyi" %% "upickle" % "2.0.0", // For JSON serialization
  "org.scala-lang" % "scala-reflect" % scalaVersion.value // For macros (used by spec-macros)
)

// Cross-build for both Scala 2.13.12 and 2.12.19 to support all modules
crossScalaVersions := Seq("2.13.12", "2.12.19")

// Add -Ymacro-annotations only for Scala 2.13 during compilation
Compile / compile / scalacOptions := {
  CrossVersion.partialVersion(scalaVersion.value) match {
    case Some((2, 13)) => Seq("-Ymacro-annotations")
    case _ => Seq() // No extra options for Scala 2.12
  }
}

// Add -Ymacro-annotations for Scaladoc only in Scala 2.13 to avoid doc errors in 2.12
Compile / doc / scalacOptions := {
  CrossVersion.partialVersion(scalaVersion.value) match {
    case Some((2, 13)) => Seq("-Ymacro-annotations")
    case _ => Seq()
  }
}
