// spec-macros/build.sbt

// 프로젝트 이름 설정
name := "spec-macros"

// (수정됨 - 중요!): specCore 프로젝트에 대한 의존성을 'dependsOn(specCore)' 대신 라이브러리 의존성으로 변경합니다.
// 이렇게 하면 spec-macros는 specCore가 로컬에 publishLocal된 아티팩트를 참조하게 됩니다.
// '%%'는 현재 Scala 버전(2.13.12)에 맞는 spec-core 아티팩트(spec-core_2.13)를 찾도록 합니다.
libraryDependencies += "your.company" %% "spec-core" % "0.1.0-SNAPSHOT"

// 라이브러리 의존성 추가
libraryDependencies += "org.scala-lang" % "scala-reflect" % scalaVersion.value

// 매크로 어노테이션을 활성화하는 컴파일러 옵션
Compile / scalacOptions += "-Ymacro-annotations"

// spec-macros도 크로스 컴파일 버전이 명시되어야 합니다.
// 현재는 2.13만 사용하므로 2.13만 명시. 향후 Scala 3 추가 시 `Seq("2.13.12", "3.x.x")`로 변경.
crossScalaVersions := Seq("2.13.12")
