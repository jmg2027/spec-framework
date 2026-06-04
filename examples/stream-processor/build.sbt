// stream-processor — a configurable AXI-Stream packet-processing pipeline in
// Chisel 7, with its spec graph mirroring the design (top/modules/bundles/params).
// Cross-builds to Scala 2.13 and Scala 3 (Chisel 7 and the spec framework both do).

ThisBuild / organization := "streamproc"
ThisBuild / version      := "0.1.0"
// Chisel publishes only for Scala 2.13 (there is no `chisel_3` artifact), and
// Chisel 7 pulls scala-library 2.13.16 (SIP-51), so this real-Chisel build is
// Scala 2.13.16. The Scala 3 counterpart of the same design lives next door in
// `../stream-processor-s3` (Chisel-shaped shim), since the spec framework — not
// Chisel — is what we are demonstrating on Scala 3.
ThisBuild / scalaVersion := "2.13.16"

lazy val root = (project in file("."))
  .settings(
    name := "stream-processor",
    crossScalaVersions := Seq("2.13.16"),
    libraryDependencies ++= Seq(
      "your.company"      %% "spec-core"   % "0.1.0-SNAPSHOT",
      "your.company"      %% "spec-macros" % "0.1.0-SNAPSHOT",
      "org.chipsalliance" %% "chisel"      % "7.0.0",
    ),
    // Chisel's compiler plugin (auto-naming/source locators) is Scala 2 only;
    // on Scala 3 Chisel uses the language's own metaprogramming.
    libraryDependencies ++= (CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, _)) => Seq(compilerPlugin("org.chipsalliance" % "chisel-plugin" % "7.0.0" cross CrossVersion.full))
      case _            => Seq.empty
    }),
    // Point spec emission at the directory aggregated by SpecCheck.
    initialize := {
      val _   = initialize.value
      val dir = (Compile / resourceManaged).value / "spec-meta"
      System.setProperty("spec.meta.dir", dir.getAbsolutePath)
    },
    publish / skip := true,
  )
