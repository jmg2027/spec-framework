// StandaloneSpecExample.scala
// 의존성 없는 단일 파일 spec 프레임워크 예제

package your_project.design

// === 단독 실행 가능한 Spec 프레임워크 (복사해서 사용) ===

case class SpecBuilder(id: String, desc: String = "") {
  def desc(d: String): SpecBuilder = this.copy(desc = d)
  def is(other: Any*): SpecBuilder = this
  def has(other: Any*): SpecBuilder = this
  def uses(other: Any*): SpecBuilder = this
  def status(s: String): SpecBuilder = this
  def entry(key: String, value: String = ""): SpecBuilder = this
  def table(tableType: String, content: String): SpecBuilder = this
  def draw(drawType: String, content: String): SpecBuilder = this
  def code(language: String, content: String): SpecBuilder = this
  def code(content: String): SpecBuilder = this
  def note(n: String): SpecBuilder = this
  def build(): Unit = {
    println(s"[Spec] Built spec '$id': $desc")
  }
}

object SimpleSpec {
  def CONTRACT(id: String): SpecBuilder = SpecBuilder(id)
  def FUNCTION(id: String): SpecBuilder = SpecBuilder(id)
  def INTERFACE(id: String): SpecBuilder = SpecBuilder(id)
  def PARAMETER(id: String): SpecBuilder = SpecBuilder(id)
}

class LocalSpec(spec: Any) extends scala.annotation.StaticAnnotation

// === 사용 예제 ===

object MySpecs {
  
  val cpuSpec = SimpleSpec.CONTRACT("CPU_001")
    .desc("CPU core specification")
    .status("DRAFT")
    .entry("Author", "CPU Team")
    .entry("Version", "1.0")
    .table("markdown", """
| Register | Width | Purpose |
|----------|-------|---------|
| PC       | 32    | Program Counter |
| SP       | 32    | Stack Pointer |
| ACC      | 32    | Accumulator |
""")
    .draw("mermaid", """
graph TD
  A[Fetch] --> B[Decode]
  B --> C[Execute]
  C --> D[Writeback]
  D --> A
""")
    .code("scala", """
class CPU extends Module {
  val io = IO(new Bundle {
    val instr = Input(UInt(32.W))
    val data_out = Output(UInt(32.W))
  })
  
  val pc = RegInit(0.U(32.W))
  val acc = RegInit(0.U(32.W))
  
  // CPU implementation...
}
""")
    .note("This CPU implements a simple RISC architecture")
    .build()

  val memorySpec = SimpleSpec.INTERFACE("MEM_001")
    .desc("Memory interface specification")
    .entry("- Main requirements")
    .entry("  - Read/write operations")
    .entry("  - 32-bit addressing")
    .entry("  - Cache coherency")
    .entry("- Performance")
    .entry("  - Single cycle access")
    .entry("  - Pipeline support")
    .draw("ascii", """
    ┌─────────┐    ┌─────────┐
    │   CPU   │───▶│ Memory  │
    │         │◀───│         │
    └─────────┘    └─────────┘
    """)
    .build()
}

// 어노테이션 예제 (아무것도 하지 않지만 컴파일됨)
@LocalSpec(MySpecs.cpuSpec)
class MyCPU {
  println("CPU implementation here")
}

@LocalSpec(MySpecs.memorySpec)  
class MyMemory {
  println("Memory implementation here")
}

// 테스트 실행
object StandaloneSpecExample extends App {
  println("=== 단독 실행 Spec 프레임워크 예제 ===")
  
  // 스펙들 빌드 (이미 MySpecs에서 호출됨)
  println("모든 스펙이 성공적으로 빌드되었습니다!")
  
  // 모듈들 테스트
  new MyCPU()
  new MyMemory()
  
  println("=== 완료! ===")
}
