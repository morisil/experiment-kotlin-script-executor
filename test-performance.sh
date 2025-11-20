#!/bin/bash
# Performance test script for Kotlin Script Executor
# This script tests the /execute endpoint with various scripts and measures response times

BASE_URL="http://localhost:8080"

echo "=== Kotlin Script Executor Performance Tests ==="
echo ""

# Function to test script execution and measure time
test_script() {
    local name="$1"
    local script="$2"

    echo "Testing: $name"

    # Measure total request time including network latency
    local start=$(date +%s%3N)
    local response=$(curl -s -w "\n%{http_code}\n%{time_total}" -X POST "$BASE_URL/execute" \
        -H "Content-Type: application/json" \
        -d "{\"script\":\"$script\"}")
    local end=$(date +%s%3N)

    local http_code=$(echo "$response" | tail -n 2 | head -n 1)
    local curl_time=$(echo "$response" | tail -n 1)
    local body=$(echo "$response" | head -n -2)

    echo "  HTTP Status: $http_code"
    echo "  Total Request Time: ${curl_time}s"
    echo "  Response: $body"

    # Extract execution time from response
    local exec_time=$(echo "$body" | grep -o '"executionTimeMs":[0-9]*' | cut -d':' -f2)
    if [ -n "$exec_time" ]; then
        echo "  Script Execution Time: ${exec_time}ms"
    fi

    echo ""
}

# Wait for server to be ready
echo "Waiting for server to start..."
for i in {1..30}; do
    if curl -s "$BASE_URL/" > /dev/null 2>&1; then
        echo "Server is ready!"
        echo ""
        break
    fi
    sleep 1
done

# Test 1: Simple expression
test_script "Simple Expression" "42"

# Test 2: Simple calculation
test_script "Simple Math" "10 + 20 + 30"

# Test 3: String operation
test_script "String Operation" "println(\\\"Hello, World!\\\"); \\\"result\\\""

# Test 4: List operations
test_script "List Sum" "listOf(1, 2, 3, 4, 5).sum()"

# Test 5: More complex calculation
test_script "Factorial" "fun factorial(n: Int): Int = if (n <= 1) 1 else n * factorial(n - 1); factorial(10)"

# Test 6: Fibonacci
test_script "Fibonacci" "fun fib(n: Int): Int = if (n <= 1) n else fib(n-1) + fib(n-2); fib(20)"

# Test 7: List processing
test_script "List Processing" "(1..100).filter { it % 2 == 0 }.map { it * 2 }.sum()"

# Test 8: String manipulation
test_script "String Manipulation" "\\\"Hello World\\\".split(\\\" \\\").joinToString(\\\"-\\\").lowercase()"

# Test 9: Error case
test_script "Error Case" "throw Exception(\\\"Test error\\\")"

# Test 10: Multiple operations
test_script "Multiple Operations" "val x = 10; val y = 20; val z = x + y; println(\\\"Sum: \$z\\\"); z * 2"

echo "=== Performance Test Complete ==="
