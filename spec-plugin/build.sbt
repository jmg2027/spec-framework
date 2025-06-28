// spec-plugin/build.sbt

// 이 프로젝트가 SBT 플러그임을 명시합니다.
sbtPlugin := true

// (수정됨 - 중요!): 플러그인 프로젝트의 Scala 버전을 명시적으로 설정합니다.
// SBT 런타임은 일반적으로 Scala 2.12.x 기반으로 빌드되므로, 여기에 맞춰야 합니다.
scalaVersion := "2.12.19"

// (수정됨 - 중요!): spec-core에 대한 의존성을 라이브러리 의존성으로 변경합니다.
// `%%`를 사용하여 현재 Scala 버전(2.12.19)에 맞는 spec-core 아티팩트(spec-core_2.12)를 찾습니다.
// spec-core는 이제 crossScalaVersions 설정 덕분에 2.12 버전 아티팩트를 publishLocal 합니다.
libraryDependencies += "your.company" %% "spec-core" % "0.1.0-SNAPSHOT"

// enablePlugins(SbtPlugin)은 여기서 선언합니다.
enablePlugins(SbtPlugin)

// 플러그인 자체에 매크로 어노테이션이 사용되지 않는다면, 이 옵션은 불필요하므로 제거합니다.
// Compile / scalacOptions += "-Ymacro-annotations"
