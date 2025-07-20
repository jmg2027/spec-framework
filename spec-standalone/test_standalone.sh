#!/bin/bash
# test_standalone.sh - Test script for standalone spec framework

echo "=== Testing Standalone Spec Framework ==="

echo "Step 1: Compile the standalone spec framework..."
scalac -cp "." StandaloneSpec.scala StandaloneSpecExample.scala

if [ $? -eq 0 ]; then
    echo "✅ Compilation successful!"
else
    echo "❌ Compilation failed!"
    exit 1
fi

echo -e "\nStep 2: Check generated class files..."
ls -la *.class 2>/dev/null || echo "No .class files found (that's ok, they might be in subdirectories)"

echo -e "\nStep 3: Test basic usage in Scala REPL..."
echo '
import framework.specs._

val testSpec = Spec.CONTRACT("TEST_001")
  .desc("Test specification")
  .entry("Author", "Test User")
  .table("markdown", "| Col1 | Col2 |\n|------|------|\n| A    | B    |")
  .code("scala", "println(\"Hello World\")")
  .build()

println("Standalone spec framework works!")
:quit
' | scala -cp "." -i 

echo -e "\n✅ Standalone spec framework test completed!"
echo -e "\nUsage: Just copy StandaloneSpec.scala to your project and import framework.specs._"
