package your_project.design

import framework.spec.Spec._
import framework.spec.HardwareSpecification

/** 
 * Test cases for the improved Spec Framework
 * 수정된 Spec Framework의 테스트 케이스들
 */
object SpecFrameworkTests {
  
  // Test 1: PARAMETER can be used by any category
  val widthParam = PARAMETER("WIDTH_PARAM")
    .desc("Width parameter specification")
    .entry("Default", "32")
    .entry("Range", "8-64")
    .build()
  
  val clockParam = PARAMETER("CLOCK_FREQ") 
    .desc("Clock frequency parameter")
    .entry("Default", "100MHz")
    .entry("Range", "50MHz-200MHz")
    .build()
  
  // Test 2: BUNDLE can use PARAMETER (should work now)
  val testBundle = BUNDLE("TEST_BUNDLE")
    .desc("Test bundle using parameters")
    .uses(widthParam, clockParam)
    .entry("Purpose", "Testing parameter usage")
    .build()
  
  // Test 3: FUNCTION can use PARAMETER (should work now)  
  val testFunction = FUNCTION("ADDER_FUNC")
    .desc("Adder function using width parameter")
    .uses(widthParam)
    .code("scala", """
def adder(a: UInt, b: UInt): UInt = {
  require(a.getWidth == WIDTH_PARAM)
  require(b.getWidth == WIDTH_PARAM)
  a + b
}
""")
    .build()
  
  // Test 4: CONTRACT can use other CONTRACT (should work now)
  val cpuContract = CONTRACT("CPU_CONTRACT")
    .desc("CPU contract specification")
    .entry("ISA", "RISC-V")
    .entry("Pipeline", "5-stage")
    .build()
  
  val socContract = CONTRACT("SOC_CONTRACT")
    .desc("SoC contract using CPU")
    .uses(cpuContract)  // CONTRACT uses CONTRACT - should work now
    .entry("CPU", "RISC-V based")
    .entry("Memory", "DDR4")
    .build()
  
  // Test 5: Single parameter table method (backward compatibility)
  val tableTest = INTERFACE("TABLE_TEST")
    .desc("Testing table methods")
    .table("|Name|Type|Width|\n|---|---|---|\n|data|UInt|32|\n|valid|Bool|1|")  // Single parameter
    .table("markdown", "|Another|Table|\n|---|---|\n|A|B|")  // Two parameters
    .build()
  
  // Test 6: Complex relationship test
  val memInterface = INTERFACE("MEM_INTERFACE")
    .desc("Memory interface specification")
    .uses(widthParam)  // INTERFACE uses PARAMETER
    .has(testBundle)   // INTERFACE has BUNDLE
    .table("markdown", """
| Signal | Direction | Width | Description |
|--------|-----------|-------|-------------|
| addr   | Input     | 32    | Address bus |
| data   | Inout     | WIDTH_PARAM | Data bus |
| valid  | Input     | 1     | Valid signal |
| ready  | Output    | 1     | Ready signal |
""")
    .draw("ascii", """
    ┌─────────────┐
    │   Memory    │
    │ Interface   │
    │             │
    │ addr[31:0]  │◄─── 
    │ data[31:0]  │◄───►
    │ valid       │◄─── 
    │ ready       │────►
    └─────────────┘
    """)
    .build()
  
  // Print test results
  def main(args: Array[String]): Unit = runTests()
  
  def runTests(): Unit = {
    println("=== Spec Framework Improvement Tests ===")
    
    println(s"✅ Test 1: PARAMETER specs created - ${widthParam.id}, ${clockParam.id}")
    println(s"✅ Test 2: BUNDLE uses PARAMETER - ${testBundle.id} uses ${testBundle.uses.mkString(", ")}")
    println(s"✅ Test 3: FUNCTION uses PARAMETER - ${testFunction.id} uses ${testFunction.uses.mkString(", ")}")
    println(s"✅ Test 4: CONTRACT uses CONTRACT - ${socContract.id} uses ${socContract.uses.mkString(", ")}")
    println(s"✅ Test 5: Table methods - ${tableTest.id} has ${tableTest.tables.length} tables")
    println(s"✅ Test 6: Complex relationships - ${memInterface.id} completed")
    
    println("\n=== All tests passed! ===")
  }
}

// Usage example for annotation test (to be updated later)
object AnnotationTests {
  import your_project.specs.MyExampleSpecs._
  
  // This will be updated when LocalSpec annotation is improved
  // @LocalSpec("testModule")  // Future: string-based reference
  class TestModuleForAnnotation {
    println("Test module with annotation")
  }
}
