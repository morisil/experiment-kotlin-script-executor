#!/bin/bash
# Test TypeScript type checking functionality

BASE_URL="http://localhost:8080"

echo "=== TypeScript Type Checking Tests ==="
echo ""

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
echo "Test 1: Valid TypeScript with type annotations"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "Script: const x: number = 10; const y: number = 20; x + y"
echo ""

response=$(curl -s -X POST "$BASE_URL/execute/typescript-checked" \
    -H "Content-Type: application/json" \
    -d '{"script":"const x: number = 10; const y: number = 20; x + y"}')

echo "Response: $response"
echo ""

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "Test 2: Type Error - number assigned to string"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo 'Script: const name: string = 123'
echo ""

response=$(curl -s -X POST "$BASE_URL/execute/typescript-checked" \
    -H "Content-Type: application/json" \
    -d '{"script":"const name: string = 123"}')

echo "Response: $response"
echo ""

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "Test 3: Type Error - string assigned to number"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo 'Script: const count: number = "hello"'
echo ""

response=$(curl -s -X POST "$BASE_URL/execute/typescript-checked" \
    -H "Content-Type: application/json" \
    -d '{"script":"const count: number = \"hello\""}')

echo "Response: $response"
echo ""

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "Test 4: Valid function with type annotations"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo 'Script: function add(a: number, b: number): number { return a + b; } add(5, 10)'
echo ""

response=$(curl -s -X POST "$BASE_URL/execute/typescript-checked" \
    -H "Content-Type: application/json" \
    -d '{"script":"function add(a: number, b: number): number { return a + b; } add(5, 10)"}')

echo "Response: $response"
echo ""

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "Test 5: Comparison - Without type checking (fast)"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo 'Script: const x = 10; const y = 20; x + y'
echo ""

response=$(curl -s -X POST "$BASE_URL/execute/typescript" \
    -H "Content-Type: application/json" \
    -d '{"script":"const x = 10; const y = 20; x + y"}')

echo "Response: $response"
echo ""

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "Test 6: Comparison - With type checking (adds overhead)"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo 'Script: const x: number = 10; const y: number = 20; x + y'
echo ""

response=$(curl -s -X POST "$BASE_URL/execute/typescript-checked" \
    -H "Content-Type: application/json" \
    -d '{"script":"const x: number = 10; const y: number = 20; x + y"}')

echo "Response: $response"
echo ""

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "Test 7: Multiple type errors"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo 'Script: const a: number = "string"; const b: string = 456; const c: boolean = "not bool"'
echo ""

response=$(curl -s -X POST "$BASE_URL/execute/typescript-checked" \
    -H "Content-Type: application/json" \
    -d '{"script":"const a: number = \"string\"; const b: string = 456; const c: boolean = \"not bool\""}')

echo "Response: $response"
echo ""

echo "=== Type Checking Tests Complete ==="
echo ""
echo "Key Observations:"
echo "- Type checking adds overhead (typeCheckTimeMs in response)"
echo "- Type errors are caught before execution"
echo "- Without type checking, scripts run faster but no type safety"
echo "- With type checking, you get compile-time type validation like Kotlin"
