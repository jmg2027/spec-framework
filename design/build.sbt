// design/build.sbt
ThisBuild / scalaVersion := "2.13.12"
ThisBuild / organization := "your.company"
ThisBuild / version      := "0.1.0-SNAPSHOT"

lazy val design = (project in file("."))
  .enablePlugins(SpecPlugin)
  .settings(
    name := "design",

    /* ---------- 의존성 ---------- */
    libraryDependencies ++= Seq(
      "your.company" %% "spec-core"   % "0.1.0-SNAPSHOT",
      "your.company" %% "spec-macros" % "0.1.0-SNAPSHOT",
      "edu.berkeley.cs" %% "chisel3"  % "3.5.6"
    ),

    /* ---------- 매크로 활성화 ---------- */
    Compile / scalacOptions += "-Ymacro-annotations",

    /* ---------- ① System.property 주입 (프로젝트 로드 시 단 한 번) ---------- */
    initialize := {
      val _  = initialize.value                          // 기존 초기화 유지
      val dir = (Compile / resourceManaged).value / "spec-meta"
      System.setProperty("spec.meta.dir", dir.getAbsolutePath)
    },

    /* ---------- ② spec‑meta 디렉터리를 class‑path에 포함 ---------- */
    Compile / resourceGenerators += Def.task {
      (Compile / resourceManaged).value / "spec-meta"
      Seq.empty[File]
    }.taskValue,

    publish / skip := true
  )
