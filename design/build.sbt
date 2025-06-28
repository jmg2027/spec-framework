// design/build.sbt
// 이 파일은 RTL 디자인 코드 및 사용자 정의 스펙을 포함하는
// 'design' 프로젝트의 독립적인 SBT 빌드 정의입니다.
// 이 프로젝트는 별도의 Git 리포지토리나 하위 모듈처럼 동작할 수 있습니다.

// ---------- Global Settings for this 'design' project ----------
ThisBuild / scalaVersion := "2.13.12" // 이 디자인 프로젝트의 Scala 버전
ThisBuild / organization := "your.company" // 이 프로젝트의 조직 (프레임워크와 동일하게 유지)
ThisBuild / version      := "0.1.0-SNAPSHOT" // 이 디자인 프로젝트의 버전 (필요에 따라 변경 가능)


// ---------- Project Definition ----------
lazy val design = (project in file("."))
  .enablePlugins(SpecPlugin)
  .settings(
    name := "design",
    libraryDependencies ++= Seq(
      "your.company" %% "spec-core" % "0.1.0-SNAPSHOT",
      "your.company" %% "spec-macros" % "0.1.0-SNAPSHOT"
    ),
    libraryDependencies += "edu.berkeley.cs" %% "chisel3" % "3.5.6",
    Compile / scalacOptions := Seq("-Ymacro-annotations"),
    publish / skip := true
  )
