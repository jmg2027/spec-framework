// spec-plugin/build.sbt  ───────────────────────────────────────────────

// 1) 플러그인임을 선언
sbtPlugin := true
enablePlugins(SbtPlugin)

// 2) 플러그인 *단일* Scala 버전 고정  ➜ sbt 1.x = 2.12
scalaVersion       := "2.12.19"
crossScalaVersions := Seq("2.12.19")   // 다른 버전 빌드 금지

// 3) core 모듈 의존성 (2.12 아티팩트가 publishLocal 되어 있어야 함)
libraryDependencies += "your.company" %% "spec-core" % "0.1.0-SNAPSHOT"

// 4) 추가적인 scalac 옵션은 불필요하면 제거
// scalacOptions += "-deprecation"
