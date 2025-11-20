# Performance Analysis

## Overview

The Kotlin Script Executor server measures execution time for each script using Kotlin's `measureTimeMillis` function, which provides millisecond-precision timing of the script evaluation.

## Response Time Components

Total response time consists of:

1. **Network Latency**: Time for request/response transmission
2. **JSON Serialization/Deserialization**: Parsing request and building response
3. **Script Engine Initialization**: First-time initialization of JSR-223 Kotlin script engine
4. **Script Compilation**: Compiling the Kotlin script
5. **Script Execution**: Actual script execution time (this is what we measure)

## Measured Execution Time

The `executionTimeMs` field in the response represents **only** the script compilation and execution time (components 4 & 5), measured using:

```kotlin
val executionTime = measureTimeMillis {
    try {
        result = engine.eval(script)
    } catch (e: Exception) {
        error = e.message ?: "Script execution failed"
    }
}
```

## Expected Performance Characteristics

### Simple Expressions
```json
Request: {"script":"42"}
Expected executionTimeMs: 50-200ms (first run), 10-50ms (subsequent runs)
```

**Analysis**: Simple expressions have minimal execution time but include script compilation overhead.

### Mathematical Operations
```json
Request: {"script":"val x = 10; val y = 20; x + y"}
Expected executionTimeMs: 50-250ms (first run), 20-80ms (subsequent runs)
```

**Analysis**: Variable declarations add minimal overhead.

### List Operations
```json
Request: {"script":"listOf(1, 2, 3, 4, 5).sum()"}
Expected executionTimeMs: 100-300ms (first run), 30-100ms (subsequent runs)
```

**Analysis**: Collection operations are generally fast but depend on collection size.

### Complex Calculations (Recursive Fibonacci)
```json
Request: {"script":"fun fib(n: Int): Int = if (n <= 1) n else fib(n-1) + fib(n-2); fib(20)"}
Expected executionTimeMs: 150-400ms (includes compilation + exponential recursion)
```

**Analysis**: Recursive algorithms show actual computation time. fib(20) performs ~21,891 function calls.

### Iterative Operations
```json
Request: {"script":"(1..100).filter { it % 2 == 0 }.map { it * 2 }.sum()"}
Expected executionTimeMs: 100-350ms
```

**Analysis**: Functional operations on ranges are generally efficient.

### String Processing
```json
Request: {"script":"\"Hello World\".split(\" \").joinToString(\"-\").lowercase()"}
Expected executionTimeMs: 80-250ms
```

**Analysis**: String operations are fast with modern JVM optimization.

### Error Handling
```json
Request: {"script":"throw Exception(\"Test error\")"}
Expected executionTimeMs: 50-150ms
Response: {"result":null,"executionTimeMs":..,"error":"Test error"}
```

**Analysis**: Exception handling adds minimal overhead as it's caught immediately.

## Performance Optimization Notes

### Script Engine Caching
Currently, a new script engine is created for each request:
```kotlin
val engine = ScriptEngineManager().getEngineByExtension("kts")
```

**Optimization Opportunity**: Cache the script engine instance to reduce initialization overhead:
```kotlin
companion object {
    private val scriptEngine = ScriptEngineManager().getEngineByExtension("kts")
}
```

### Warm-up Effect
The JVM JIT compiler optimizes frequently executed code paths. First requests are typically slower than subsequent ones due to:
- Class loading
- JIT compilation
- Script engine initialization

### Concurrency Considerations
The JSR-223 script engine is not thread-safe. For production use with concurrent requests, consider:
1. Using a pool of script engines
2. Synchronizing access to a single engine
3. Using Kotlin's native scripting API with better concurrency support

## Benchmarking

To benchmark the server:

1. Start the server:
   ```bash
   ./gradlew run
   ```

2. Run the performance test script:
   ```bash
   ./test-performance.sh
   ```

3. Or use `curl` with timing:
   ```bash
   curl -w "\nTotal time: %{time_total}s\n" -X POST http://localhost:8080/execute \
     -H "Content-Type: application/json" \
     -d '{"script":"42"}'
   ```

## Sample Performance Results

Based on typical JVM performance on modern hardware:

| Script Type | First Run | Subsequent Runs | Notes |
|-------------|-----------|-----------------|-------|
| Simple expression (`42`) | 150-250ms | 20-50ms | Includes engine warmup |
| Math operation | 200-300ms | 30-80ms | Variable handling |
| List operations | 250-400ms | 50-150ms | Collection overhead |
| Recursive fib(20) | 300-500ms | 150-400ms | Actual computation time |
| String processing | 150-300ms | 40-100ms | String allocation |
| Large iteration (1..10000) | 400-700ms | 200-500ms | Linear with size |

**Note**: Times include both compilation and execution. Actual measurements may vary based on:
- Hardware (CPU speed, available memory)
- JVM version and settings
- System load
- Script complexity

## Response Time vs Execution Time

**Important Distinction**:
- **executionTimeMs**: Time to compile and execute the script (what we measure)
- **Total Response Time**: Includes network latency, JSON processing, and executionTimeMs

For local requests, total response time is typically: `executionTimeMs + 5-20ms`

For network requests, add round-trip network latency.

## Monitoring Recommendations

For production deployments, monitor:
1. **executionTimeMs** - Track script execution time trends
2. **Total request duration** - Include network and processing time
3. **Error rate** - Failed script executions
4. **Memory usage** - Script engines can be memory-intensive
5. **CPU usage** - Complex scripts may spike CPU

## Testing Methodology

The performance test script (`test-performance.sh`) measures:
- HTTP response time (via `curl --write-out '%{time_total}'`)
- Extracted execution time from JSON response
- Success/failure rate
- Error handling behavior

Run it after starting the server to validate performance characteristics.
