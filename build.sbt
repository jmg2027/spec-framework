// ---------- global ----------
ThisBuild / scalaVersion := "2.13.10"
ThisBuild / organization := "jmg2027"
ThisBuild / version      := "0.1.0-SNAPSHOT"
scalacOptions += "-Ymacro-annotations"

// ---------- sub-projects ----------
lazy val specCore = (project in file("spec-core"))
  .settings(
    name := "spec-core",
    libraryDependencies ++= Seq("com.lihaoyi" %% "upickle" % "2.0.0")
  )

lazy val specPlugin = (project in file("spec-plugin"))
  .dependsOn(specCore)
  .settings(
    name := "spec-plugin",
    sbtPlugin := true                         // ← AutoPlugin jar 로 publish
  )

lazy val root = (project in file("."))
  .aggregate(specCore, specPlugin)
  .settings(
    publish / skip := true
  )
