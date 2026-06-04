// spec-macros/build.sbt

name := "spec-macros"

// Depend directly on the specCore project defined in the root build.
dependsOn(LocalProject("specCore"))

// Cross-build for Scala 2.13 and Scala 3. (2.12 could be added for 2.12 macro
// consumers, but the real consumers — Chisel — are 2.13/3.)
crossScalaVersions := Seq("2.13.12", "3.3.4")

// Version-specific sources: the Scala 2 macros (def macros + the @LocalSpec
// macro-annotation) live under scala-2, the Scala 3 macros (inline + quotes)
// under scala-3.
Compile / unmanagedSourceDirectories += {
  val base = (Compile / sourceDirectory).value
  CrossVersion.partialVersion(scalaVersion.value) match {
    case Some((2, _)) => base / "scala-2"
    case _            => base / "scala-3"
  }
}

// scala-reflect and -Ymacro-annotations apply to Scala 2 only.
libraryDependencies ++= {
  CrossVersion.partialVersion(scalaVersion.value) match {
    case Some((2, _)) => Seq("org.scala-lang" % "scala-reflect" % scalaVersion.value)
    case _            => Seq.empty
  }
}
Compile / scalacOptions ++= {
  CrossVersion.partialVersion(scalaVersion.value) match {
    case Some((2, _)) => Seq("-Ymacro-annotations")
    case _            => Seq.empty
  }
}
