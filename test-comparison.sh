#!/bin/bash
# Comparison test script for Kotlin vs TypeScript execution
# This script tests both languages with equivalent scripts and compares performance

BASE_URL="http://localhost:8080"

echo "=== Kotlin vs TypeScript Performance Comparison ==="
echo ""

# Function to test Kotlin script
test_kotlin() {
    local name="$1"
    local script="$2"

    echo "Testing Kotlin: $name"

    local response=$(curl -s -X POST "$BASE_URL/execute/kotlin" \
        -H "Content-Type: application/json" \
        -d "{\"script\":\"$script\"}")

    echo "  Response: $response"

    local total=$(echo "$response" | grep -o '"totalTimeMs":[0-9]*' | cut -d':' -f2)
    local exec=$(echo "$response" | grep -o '"executionTimeMs":[0-9]*' | cut -d':' -f2)

    echo "  Total Time: ${total}ms"
    echo "  Execution Time: ${exec}ms"
    echo ""
}

# Function to test TypeScript script
test_typescript() {
    local name="$1"
    local script="$2"

    echo "Testing TypeScript: $name"

    local response=$(curl -s -X POST "$BASE_URL/execute/typescript" \
        -H "Content-Type: application/json" \
        -d "{\"script\":\"$script\"}")

    echo "  Response: $response"

    local total=$(echo "$response" | grep -o '"totalTimeMs":[0-9]*' | cut -d':' -f2)
    local compile=$(echo "$response" | grep -o '"compilationTimeMs":[0-9]*' | cut -d':' -f2)
    local exec=$(echo "$response" | grep -o '"executionTimeMs":[0-9]*' | cut -d':' -f2)

    echo "  Total Time: ${total}ms"
    echo "  Compilation Time: ${compile}ms"
    echo "  Execution Time: ${exec}ms"
    echo ""
}

# Wait for server
echo "Waiting for server to start..."
for i in {1..30}; do
    if curl -s "$BASE_URL/" > /dev/null 2>&1; then
        echo "Server is ready!"
        echo ""
        break
    fi
    sleep 1
done

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "Test 1: Simple Expression"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
test_kotlin "Simple Expression" "42"
test_typescript "Simple Expression" "42"

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "Test 2: Variable Declaration and Math"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
test_kotlin "Variables + Math" "val x = 10; val y = 20; x + y"
test_typescript "Variables + Math" "const x = 10; const y = 20; x + y"

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "Test 3: Array/List Sum"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
test_kotlin "List Sum" "listOf(1, 2, 3, 4, 5).sum()"
test_typescript "Array Sum" "[1, 2, 3, 4, 5].reduce((a, b) => a + b, 0)"

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "Test 4: Fibonacci (Recursive)"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
test_kotlin "Fibonacci(20)" "fun fib(n: Int): Int = if (n <= 1) n else fib(n-1) + fib(n-2); fib(20)"
test_typescript "Fibonacci(20)" "function fib(n) { return n <= 1 ? n : fib(n-1) + fib(n-2); } fib(20)"

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "Test 5: Factorial"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
test_kotlin "Factorial(10)" "fun factorial(n: Int): Int = if (n <= 1) 1 else n * factorial(n - 1); factorial(10)"
test_typescript "Factorial(10)" "function factorial(n) { return n <= 1 ? 1 : n * factorial(n - 1); } factorial(10)"

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "Test 6: Array Processing (Filter + Map)"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
test_kotlin "Array Processing" "(1..100).filter { it % 2 == 0 }.map { it * 2 }.sum()"
test_typescript "Array Processing" "Array.from({length: 100}, (_, i) => i + 1).filter(x => x % 2 === 0).map(x => x * 2).reduce((a, b) => a + b, 0)"

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "Test 7: String Manipulation"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
test_kotlin "String Processing" "\\\"Hello World\\\".split(\\\" \\\").joinToString(\\\"-\\\").lowercase()"
test_typescript "String Processing" "\\\"Hello World\\\".split(\\\" \\\").join(\\\"-\\\").toLowerCase()"

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "Test 8: Object/Map Creation"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
test_kotlin "Map Creation" "mapOf(\\\"a\\\" to 1, \\\"b\\\" to 2, \\\"c\\\" to 3).values.sum()"
test_typescript "Object Creation" "const obj = {a: 1, b: 2, c: 3}; Object.values(obj).reduce((a, b) => a + b, 0)"

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "Test 9: Loop Performance"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
test_kotlin "Loop (1-1000)" "var sum = 0; for (i in 1..1000) { sum += i }; sum"
test_typescript "Loop (1-1000)" "let sum = 0; for (let i = 1; i <= 1000; i++) { sum += i; } sum"

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "Test 10: Higher-Order Functions"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
test_kotlin "Higher-Order" "listOf(1, 2, 3, 4, 5).fold(0) { acc, n -> acc + n * n }"
test_typescript "Higher-Order" "[1, 2, 3, 4, 5].reduce((acc, n) => acc + n * n, 0)"

echo ""
echo "=== Performance Comparison Complete ==="
echo ""
echo "Key Observations:"
echo "- Kotlin: Total time includes compilation + execution (not separated by JSR-223)"
echo "- TypeScript: Shows separate compilation (parsing) and execution times"
echo "- First runs may be slower due to JVM warmup"
echo "- Compilation time matters for dynamic script execution"
echo "- GraalVM JS is highly optimized for JavaScript/TypeScript"
