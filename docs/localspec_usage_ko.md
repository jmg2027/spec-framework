# LocalSpec 매크로 사용법 (한국어)

`@LocalSpec` 매크로는 하드웨어 모듈이나 값, 메서드가 어떤 스펙을 구현하는지 표시하기 위해 사용됩니다. 컴파일 단계에서 태그 정보를 메타 파일로 기록하며, 이후 `exportSpecIndex` 실행 시 JSON 인덱스에 포함됩니다.

## 1. 준비

- `SpecPlugin`을 활성화하고 `-Ymacro-annotations` 옵션을 설정해야 합니다. 자세한 설정 방법은 [plugin_enable_ko.md](plugin_enable_ko.md)를 참고하십시오.
- 스펙 정의는 보통 `spec { ... }` 블록에서 작성하고 `val` 로 저장합니다.

```scala
import framework.macros.SpecEmit.spec
import framework.spec.Spec._

object MySpecs {
  val QueueSpec = spec {
    CONTRACT("QUEUE_CONTRACT").desc("큐 모듈 규격")
      .build()
  }
}
```

## 2. 모듈에 태그 달기

스펙 객체나 스펙 ID 문자열을 인자로 전달하여 모듈, 값, 메서드 등에 어노테이션을 붙입니다.

```scala
import chisel3._
import framework.macros.LocalSpec
import MySpecs._

@LocalSpec(QueueSpec)
class QueueModule extends Module {
  val io = IO(new Bundle {
    val enq = Flipped(Decoupled(UInt(32.W)))
    val deq = Decoupled(UInt(32.W))
  })
  // 구현부
}
```

어노테이션을 직접 붙일 수 없는 표현식에는 더미 `val`을 선언하여 태그를 남길 수 있습니다.

```scala
@LocalSpec(QueueSpec)
val tagForWhen = ()
when(io.enq.valid) {
  // ...
}
```

## 3. 실행 및 결과

어노테이션이 달린 코드가 실행되면 `SpecRegistry` 에 태그 정보가 기록됩니다. 이후 sbt에서 다음 명령을 실행하여 JSON 파일을 생성합니다.

```bash
sbt exportSpecIndex
```

`design/target/`(또는 설정한 경로)에 `SpecIndex.json`과 `TagIndex.json`이 생성되며, 각 태그의 소스 위치가 포함됩니다.
