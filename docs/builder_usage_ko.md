# Spec Builder 사용법 (한국어)

이 문서는 `framework.spec.Spec` DSL을 이용해 하드웨어 스펙을 작성하는 방법을 설명합니다. 현재 코드베이스의 `Spec.scala` 구현에 맞춰 설명됩니다.

## 1. 기본 구조

스펙은 카테고리를 선택한 뒤 `desc` 로 설명을 적고, 필요한 옵션을 이어서 호출한 후 `build()` 로 마무리합니다.

```scala
import framework.macros.SpecEmit.spec
import framework.spec.Spec._

val mySpec = spec {
  CONTRACT("EXAMPLE_ID").desc("예시 스펙")
    .status("DRAFT")
    .entry("Author", "HW Team")
    .build()
}
```

`spec { ... }` 블록 안에서 `build()` 가 호출되면 컴파일 시 메타 파일이 생성되고 런타임 레지스트리에 등록됩니다.

## 2. 카테고리

`desc` 를 호출하기 전에 어떤 종류의 스펙인지에 맞는 카테고리 생성 메서드를 골라 사용합니다.

| 카테고리 | 사용법 | 설명 |
|---------|-------|------|
| `CONTRACT` | `CONTRACT(id)` | 상위 요구 사항이나 계약을 정의합니다. |
| `FUNCTION` | `FUNCTION(id)` | 기능이나 알고리즘을 설명합니다. |
| `PROPERTY` | `PROPERTY(id)` | 속성이나 검증 항목을 명시합니다. |
| `COVERAGE` | `COVERAGE(id)` | 커버리지 요구 사항을 정의합니다. |
| `INTERFACE` | `INTERFACE(id)` | 하드웨어 인터페이스를 기술합니다. |
| `PARAMETER` | `PARAMETER(id)` | 파라미터나 설정 값을 설명합니다. |
| `CAPABILITY` | `CAPABILITY(id)` | 지원하는 기능을 정의합니다. |
| `BUNDLE` | `BUNDLE(id)` | 인터페이스에서 참조하는 재사용 가능한 데이터 구조입니다. |
| `RAW` | `RAW(id, prefix)` | 접두사를 가진 사용자 정의 카테고리입니다. |

예시:

```scala
val contractSpec  = spec { CONTRACT("CORE_REQ").desc("코어 요구 사항").build() }
val functionSpec  = spec { FUNCTION("ADD_FN").desc("덧셈 기능").build() }
val bundleSpec    = spec { BUNDLE("REQ_BUNDLE").desc("요청 필드").build() }
val interfaceSpec = spec {
  INTERFACE("BUS_IF").desc("버스 인터페이스")
    .has(bundleSpec)
    .build()
}
```

## 3. Stage2 메서드

`desc` 를 호출한 이후에는 다음과 같은 메서드들을 사용할 수 있습니다.

| 메서드 | 설명 |
|-------|------|
| `status(String)` | 스펙의 상태 값을 설정합니다. |
| `is(spec* )` | 다른 스펙 ID 혹은 스펙 객체를 참조합니다. |
| `has(spec* )` | 하위 스펙 ID 혹은 스펙 객체를 나타냅니다. |
| `uses(spec* )` | CONTRACT 카테고리 간의 의존 관계를 표현합니다. |
| `entry(key, value)` | 키-값 형태의 항목을 추가합니다. `value` 를 생략하면 계층형 리스트 항목으로 취급합니다. |
| `table(tableType, content)` | Markdown, CSV 등 문자열로 테이블을 추가합니다. |
| `markdownTable(headers, rows)` | 헤더와 행 리스트를 이용해 Markdown 테이블을 만듭니다. |
| `draw(drawType, content)` | mermaid, svg, ascii 등의 그림을 삽입합니다. |
| `code(language, content)` | 코드 블록을 삽입합니다. 기본 언어는 `text` 입니다. |
| `note(text)` | 메모 혹은 추가 설명을 남깁니다. |
| `build(scalaDeclarationPath)` | 스펙을 완성하여 레지스트리에 등록합니다. |

계층형 리스트를 표현할 때는 `entry` 의 첫 번째 인자에 들여쓰기를 포함한 문자열을 사용합니다.

```scala
val spec = FUNCTION("PIPELINE").desc("파이프라인 동작")
  .entry("- 단계")
  .entry("  - IF")
  .entry("  - ID")
  .entry("  - EX")
  .build()
```

## 4. 예시

```scala
val example = spec {
  INTERFACE("BUS").desc("버스 인터페이스")
    .is("DMA_CONTROLLER")
    .entry("addr", "주소 입력")
    .table("csv", "Signal,Width\naddr,32")
    .draw("mermaid", "graph TD; A-->B")
    .code("verilog", "module bus(...);")
    .note("추가 설명")
    .build()
}
```

이렇게 작성된 스펙은 `SpecRegistry` 에 등록되고 `exportSpecIndex` 실행 시 JSON 파일로 저장됩니다.
