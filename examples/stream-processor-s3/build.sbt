// Scala 3 counterpart of ../stream-processor. Chisel has no Scala 3 release, so
// the design runs on a small Chisel-shaped shim — but the SPEC graph is the
// *same source*, shared from the Chisel project, demonstrating the framework on
// Scala 3 with the identical architecture and IDs.

ThisBuild / scalaVersion := "3.3.4"
ThisBuild / organization := "streamproc"
ThisBuild / version      := "0.1.0"

lazy val root = (project in file("."))
  .settings(
    name := "stream-processor-s3",
    libraryDependencies ++= Seq(
      "your.company" %% "spec-core"   % "0.1.0-SNAPSHOT",
      "your.company" %% "spec-macros" % "0.1.0-SNAPSHOT",
    ),
    // Reuse the spec graph verbatim from the Chisel project.
    Compile / unmanagedSourceDirectories +=
      baseDirectory.value.getParentFile / "stream-processor" / "src" / "main" / "scala" / "streamproc" / "specs",
    initialize := {
      val _   = initialize.value
      val dir = (Compile / resourceManaged).value / "spec-meta"
      System.setProperty("spec.meta.dir", dir.getAbsolutePath)
    },
    publish / skip := true,
  )
