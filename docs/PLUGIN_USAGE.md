# SpecPlugin 사용법 (cross-file/cross-package/cross-module @LocalSpec 지원)

이 문서는 SpecPlugin 기반 하드웨어 스펙 관리/검증 프레임워크를 **가장 편하게 사용하는 방법**을 안내합니다.

---

## 1. 프로젝트 구조 예시 (단일 모듈, 내부 폴더 분리)

```
design/
  build.sbt
  src/
    main/
      scala/
        your_project/
          specs/
            MyExampleSpecs.scala   // ← 모든 HardwareSpecification val/object
          design/
            MyExampleDesign.scala // ← 실제 하드웨어/테스트 코드
```

---

## 2. build.sbt 최소 설정

```scala
// design/build.sbt
lazy val design = (project in file("."))
  .enablePlugins(SpecPlugin)
  .settings(
    name := "design"
    // 별도 javaOptions, scalacOptions, resourceManaged 세팅 필요 없음
  )
```
- **별도의 의존성, 옵션, 디렉토리 세팅 필요 없음**
- cross-file/cross-package 참조는 import만 하면 됨

---

## 3. cross-file/cross-package @LocalSpec 사용 예시

```scala
// src/main/scala/your_project/specs/MyExampleSpecs.scala
package your_project.specs
import framework.spec._
object MyExampleSpecs {
  val QueueSpec = Spec.CONTRACT("QUEUE", "desc").entry("k", "v").build()
}

// src/main/scala/your_project/design/MyExampleDesign.scala
package your_project.design
import chisel3._
import framework.macros.LocalSpec
import your_project.specs.MyExampleSpecs._

@LocalSpec(QueueSpec)
class MyModule extends Module { ... }
```

---

## 4. cross-module(멀티 모듈) 환경 예시

```scala
// build.sbt
lazy val specs = (project in file("specs")).settings(name := "specs")
lazy val design = (project in file("design")).enablePlugins(SpecPlugin).dependsOn(specs)
```
- specs 모듈에 모든 HardwareSpecification 정의, design 모듈에서 import

---

## 5. 빌드/실행/테스트

- `sbt compile` : .spec/.tag 파일 자동 생성
- `sbt run`/`sbt test` : cross-file/cross-module @LocalSpec 정상 동작
- 별도 옵션/디렉토리 세팅 필요 없음

---

## 6. 주의사항/팁

- cross-module 환경에서는 반드시 `dependsOn(specs)`로 빌드 순서 보장
- `.spec` 파일이 먼저 생성되어야 cross-file/cross-module @LocalSpec이 정상 동작
- 플러그인만 enable하면 모든 환경 자동 세팅

---

## 7. 요약

- **가장 쉬운 방법:**
  - specs를 내부 폴더로만 분리, 플러그인만 enable
  - cross-file/cross-package @LocalSpec 완벽 지원
- **대규모/멀티팀:**
  - specs를 별도 모듈로 분리, design에서 dependsOn

---

자세한 내용은 공식 문서/예제 프로젝트를 참고하세요.
