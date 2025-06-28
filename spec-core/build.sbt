// spec-core/build.sbt

// 프로젝트 이름 설정
name := "spec-core"

// 라이브러리 의존성 추가
libraryDependencies ++= Seq(
  "com.lihaoyi" %% "upickle" % "2.0.0", // JSON 직렬화를 위함
  "org.scala-lang" % "scala-reflect" % scalaVersion.value // 매크로를 위함 (spec-macros에서 의존)
)

// (수정됨 - 중요!): 이 프로젝트 자체를 Scala 2.13.12와 2.12.19 두 가지 버전으로 크로스 컴파일하도록 명시합니다.
// 이렇게 하면 spec-plugin (Scala 2.12)이 spec-core의 올바른 버전을 찾을 수 있습니다.
crossScalaVersions := Seq("2.13.12", "2.12.19")

// Scala 2.13에서만 -Ymacro-annotations 컴파일러 옵션을 추가합니다.
// `=` 연산자를 사용하여 상속된 옵션을 완전히 덮어씁니다.
Compile / compile / scalacOptions := {
  CrossVersion.partialVersion(scalaVersion.value) match {
    case Some((2, 13)) => Seq("-Ymacro-annotations")
    case _ => Seq() // Scala 2.12에서는 옵션을 추가하지 않음
  }
}

// Scaladoc(문서화) 시에도 -Ymacro-annotations 옵션이 Scala 2.13에서만 추가되도록 합니다.
// 이를 통해 Scala 2.12에서 문서 생성 시 발생하는 오류를 방지합니다.
Compile / doc / scalacOptions := {
  CrossVersion.partialVersion(scalaVersion.value) match {
    case Some((2, 13)) => Seq("-Ymacro-annotations")
    case _ => Seq() // Scala 2.12에서는 옵션을 추가하지 않음
  }
}
