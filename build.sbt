// build.sbt
// 이 파일은 프레임워크의 핵심 라이브러리(spec-core, spec-macros)와
// SBT 플러그인(spec-plugin)을 빌드하는 루트 정의를 담당합니다.
// design 프로젝트는 이제 완전히 독립적인 별도의 SBT 프로젝트로 관리됩니다.

// ---------- Global Settings ----------
ThisBuild / scalaVersion := "2.13.12" // 기본 Scala 버전
ThisBuild / organization := "your.company"
ThisBuild / version      := "0.1.0-SNAPSHOT"


// ---------- Sub-projects Definitions ----------

// spec-core: 프레임워크의 핵심 데이터 모델
lazy val specCore = (project in file("spec-core"))
  .settings(
    name := "spec-core"
  )

// spec-macros: @LocalSpec 매크로 구현
lazy val specMacros = (project in file("spec-macros"))
  .settings(
    name := "spec-macros"
  )

// spec-plugin: SBT 빌드 태스크 정의 (SBT 플러그인)
lazy val specPlugin = (project in file("spec-plugin"))
  .settings(
    name := "spec-plugin"
  )

// (수정됨): design 프로젝트는 이제 이 루트 build.sbt에서 정의되지 않습니다.
// design은 이제 독립적인 SBT 프로젝트입니다.

// ---------- Root Project Aggregation ----------
lazy val root = (project in file("."))
  // (수정됨): design 프로젝트를 aggregate 대상에서 제외합니다.
  // specPlugin도 aggregate 대상에서 제외합니다. 이들은 이제 독립적인 빌드 단위로 관리됩니다.
  .aggregate(specCore, specMacros) // 루트는 이제 핵심 라이브러리만 집계합니다.
  .settings(
    publish / skip := true // 루트 프로젝트는 배포하지 않음
  )
