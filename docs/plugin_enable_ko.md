# SpecPlugin 사용법 (한국어)

이 문서는 현재 코드베이스에 맞춰 SpecPlugin을 프로젝트에 적용하는 방법을 설명합니다.

## 1. 플러그인 추가

`project/plugins.sbt` 파일에 다음 라인을 추가해 sbt 플러그인을 불러옵니다.

```scala
addSbtPlugin("your.company" % "spec-plugin" % "0.1.0-SNAPSHOT")
```

## 2. 라이브러리 의존성 설정

`build.sbt`의 프로젝트 설정 안에서 다음 라이브러리들을 추가합니다.

```scala
libraryDependencies ++= Seq(
  "your.company" %% "spec-core"   % "0.1.0-SNAPSHOT",
  "your.company" %% "spec-macros" % "0.1.0-SNAPSHOT",
  "edu.berkeley.cs" %% "chisel3"  % "3.6.0"            // 필요한 경우
)
```

## 3. 플러그인 활성화 및 옵션

프로젝트 정의에 `SpecPlugin`을 활성화하고 매크로 사용을 위해 컴파일 옵션을 지정합니다.

```scala
lazy val design = (project in file("."))
  .enablePlugins(SpecPlugin)
  .settings(
    Compile / scalacOptions += "-Ymacro-annotations",
    initialize := {
      val _  = initialize.value
      val dir = (Compile / resourceManaged).value / "spec-meta"
      System.setProperty("spec.meta.dir", dir.getAbsolutePath)
    },
    publish / skip := true
  )
```

위 설정은 `spec.meta.dir` 시스템 프로퍼티를 지정하여 매크로가 메타 데이터를 해당 위치에 출력하도록 합니다.

## 4. 인덱스 생성

컴파일 후 `exportSpecIndex` 태스크를 실행하면 `SpecIndex.json`과 `TagIndex.json`이 생성됩니다.

```bash
sbt exportSpecIndex
```

출력 경로는 위에서 설정한 `spec.meta.dir`에 따라 결정되며 기본값은 `design/target/` 입니다.
