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

## 2. Stage2 메서드

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

## 3. 예시

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
