// ---------- global ----------
ThisBuild / scalaVersion := "2.13.12"
ThisBuild / organization := "jmg2027"
ThisBuild / version      := "0.1.0-SNAPSHOT"


// ---------- sub-projects ----------
lazy val specCore = (project in file("spec-core"))
  .settings(
    name := "spec-core",
//    libraryDependencies += "com.lihaoyi" %% "upickle" % "2.0.0",
    libraryDependencies += "com.lihaoyi" %% "upickle" % "3.3.0",
   libraryDependencies += "org.scala-lang" % "scala-reflect" % scalaVersion.value,
//    addCompilerPlugin ("org.scalamacros" % "paradise_2.13.0-M3" % "2.1.0"),
    Compile / scalacOptions += "-Ymacro-annotations",
//    resolvers += Resolver.sonatypeRepo("releases")
//    addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full)
)

lazy val specPlugin = (project in file("spec-plugin"))
  .dependsOn(specCore)
  .settings(
    name := "spec-plugin",
    sbtPlugin := true,                         // ← publish with AutoPlugin jar

    Compile / scalacOptions += "-Ymacro-annotations",
  )

lazy val root = (project in file("."))
  .aggregate(specCore, specPlugin)
  .settings(
    publish / skip := true
  )
