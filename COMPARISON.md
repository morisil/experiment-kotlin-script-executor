# Kotlin vs TypeScript Script Execution Comparison

## Overview

This document compares the performance characteristics of executing Kotlin scripts vs TypeScript/JavaScript scripts in a JVM environment, with detailed timing of compilation and execution phases.

## Architecture Differences

### Kotlin Script Execution (JSR-223)

**Technology**: Kotlin Scripting JSR-223 engine

**Process**:
1. Script received as string
2. JSR-223 engine invoked via `ScriptEngineManager`
3. **Compilation + Execution** happen together (not separated by JSR-223 API)
4. Result returned

**Timing Measured**:
- `totalTimeMs`: Complete compilation + execution time
- `executionTimeMs`: Same as total (no separation available)
- `compilationTimeMs`: null (not available separately)

**Key Characteristics**:
- Full Kotlin compilation to JVM bytecode
- Type-safe compilation with full Kotlin semantics
- Slower compilation due to full type checking and inference
- Execution performance benefits from JVM JIT optimization
- Each script is compiled independently (no caching in this implementation)

### TypeScript/JavaScript Execution (GraalVM)

**Technology**: GraalVM Polyglot JavaScript engine

**Process**:
1. Script received as string
2. **Compilation Phase**: Source parsing and AST generation via `Source.newBuilder()`
3. **Execution Phase**: AST interpretation/compilation via `context.eval()`
4. Result returned

**Timing Measured**:
- `totalTimeMs`: Complete process time
- `compilationTimeMs`: Source parsing and preparation time
- `executionTimeMs`: Actual script execution time

**Key Characteristics**:
- JavaScript interpretation (TypeScript types ignored at runtime)
- Fast parsing with minimal overhead
- GraalVM's optimizing compiler for hot code paths
- Lighter-weight than full Kotlin compilation
- Dynamic typing (no type checking)

## Performance Expectations

### Simple Expressions

**Example**: `42`

| Language | Compilation | Execution | Total | Notes |
|----------|-------------|-----------|-------|-------|
| Kotlin | N/A | 150-250ms | 150-250ms | Includes script engine setup |
| TypeScript | 1-5ms | 5-15ms | 10-25ms | Fast parsing, minimal execution |

**Analysis**: TypeScript is significantly faster for simple expressions due to lighter compilation.

### Variable Operations

**Kotlin**: `val x = 10; val y = 20; x + y`
**TypeScript**: `const x = 10; const y = 20; x + y`

| Language | Compilation | Execution | Total | Notes |
|----------|-------------|-----------|-------|-------|
| Kotlin | N/A | 200-300ms | 200-300ms | Type inference overhead |
| TypeScript | 2-8ms | 10-20ms | 15-35ms | No type checking at runtime |

**Analysis**: Kotlin's type inference adds compilation overhead, while TypeScript treats variables dynamically.

### Recursive Algorithms (Fibonacci)

**Kotlin**: `fun fib(n: Int): Int = if (n <= 1) n else fib(n-1) + fib(n-2); fib(20)`
**TypeScript**: `function fib(n) { return n <= 1 ? n : fib(n-1) + fib(n-2); } fib(20)`

| Language | Compilation | Execution | Total | Notes |
|----------|-------------|-----------|-------|-------|
| Kotlin | N/A | 300-500ms | 300-500ms | Compilation + ~200ms computation |
| TypeScript | 3-10ms | 180-250ms | 190-270ms | Fast compile, GraalVM optimized execution |

**Analysis**: Compilation overhead is amortized by execution time. GraalVM's JIT optimization makes execution competitive.

### Collection Processing

**Kotlin**: `(1..100).filter { it % 2 == 0 }.map { it * 2 }.sum()`
**TypeScript**: `Array.from({length: 100}, (_, i) => i + 1).filter(x => x % 2 === 0).map(x => x * 2).reduce((a, b) => a + b, 0)`

| Language | Compilation | Execution | Total | Notes |
|----------|-------------|-----------|-------|-------|
| Kotlin | N/A | 250-400ms | 250-400ms | Functional operations overhead |
| TypeScript | 5-15ms | 80-150ms | 90-170ms | Native array methods optimized |

**Analysis**: TypeScript's native array methods are highly optimized in GraalVM.

### Loop Performance

**Kotlin**: `var sum = 0; for (i in 1..1000) { sum += i }; sum`
**TypeScript**: `let sum = 0; for (let i = 1; i <= 1000; i++) { sum += i; } sum`

| Language | Compilation | Execution | Total | Notes |
|----------|-------------|-----------|-------|-------|
| Kotlin | N/A | 200-350ms | 200-350ms | Range iteration |
| TypeScript | 3-8ms | 50-100ms | 60-115ms | Simple loop, highly optimized |

**Analysis**: Simple loops favor TypeScript due to faster compilation and GraalVM optimization.

## Compilation Phase Deep Dive

### Kotlin Compilation

**What happens during Kotlin script compilation:**

1. **Parsing**: Source code → AST
2. **Type Resolution**: Infer types for `val`, `var`, function returns
3. **Type Checking**: Verify type safety across all operations
4. **Semantic Analysis**: Check for undefined symbols, resolve imports
5. **Bytecode Generation**: Generate JVM bytecode
6. **Class Loading**: Load compiled classes into JVM

**Time Distribution** (estimated):
- Parsing: ~15%
- Type checking: ~40%
- Semantic analysis: ~20%
- Bytecode generation: ~20%
- Loading: ~5%

**Why it's slower:**
- Full static type analysis
- Kotlin language features (extension functions, null safety, etc.)
- JVM bytecode generation overhead
- No pre-compiled standard library for scripts

### TypeScript/JavaScript Compilation (GraalVM)

**What happens during Source.newBuilder():**

1. **Parsing**: Source code → AST
2. **Basic Validation**: Syntax errors only
3. **Source Preparation**: Create Source object

**What happens during context.eval():**

1. **AST Interpretation**: Initial execution via interpreter
2. **Profiling**: Collect runtime type information
3. **JIT Compilation**: Hot code paths compiled to machine code (Graal compiler)
4. **Optimization**: Inline caching, type specialization

**Time Distribution** (estimated):
- Parsing: ~50-70% of compilation time
- Validation: ~20-30%
- Source prep: ~10-20%
- Execution varies based on script complexity and JIT warmup

**Why it's faster initially:**
- No type checking (dynamic typing)
- Simpler language semantics
- Lighter AST representation
- Optimized for quick startup

## Key Differences Summary

### Kotlin Advantages
✅ **Type Safety**: Compile-time type checking catches errors early
✅ **Language Features**: Full Kotlin language available (coroutines, extensions, etc.)
✅ **IDE Support**: Better tooling and auto-completion for development
✅ **JVM Integration**: Native JVM types and libraries

### Kotlin Disadvantages
❌ **Slower Compilation**: Full type checking and inference
❌ **Higher Overhead**: Bytecode generation for every script
❌ **No Timing Separation**: JSR-223 doesn't expose compilation vs execution

### TypeScript/JavaScript Advantages
✅ **Fast Startup**: Minimal compilation overhead
✅ **Timing Visibility**: Clear separation of parsing and execution
✅ **GraalVM Optimization**: State-of-the-art JavaScript JIT compiler
✅ **Lower Resource Usage**: Lighter memory footprint initially

### TypeScript/JavaScript Disadvantages
❌ **No Type Checking**: Types are stripped, no runtime validation
❌ **Dynamic Typing**: Errors only caught at runtime
❌ **Less JVM Integration**: JavaScript semantics vs JVM semantics

## Use Case Recommendations

### Choose Kotlin Script Execution When:
- Type safety is critical
- Scripts use complex Kotlin language features
- Integrating with existing Kotlin codebase
- Scripts are long-running (amortize compilation cost)
- Need full Kotlin standard library

### Choose TypeScript/JavaScript Execution When:
- Fast startup time is critical
- Scripts are short-lived
- Need to measure compilation vs execution separately
- Dynamic typing is acceptable
- Leveraging existing JavaScript/TypeScript code
- Need optimal performance for mathematical/array operations

## Performance Optimization Tips

### For Kotlin:
1. **Cache Script Engine**: Reuse `ScriptEngine` instance
   ```kotlin
   companion object {
       private val scriptEngine = ScriptEngineManager().getEngineByExtension("kts")
   }
   ```

2. **Pre-compile Scripts**: Use `Compilable.compile()` if script is reused
   ```kotlin
   val compiledScript = (engine as Compilable).compile(script)
   compiledScript.eval() // Faster on subsequent runs
   ```

3. **Reduce Type Complexity**: Simpler type hierarchies compile faster

### For TypeScript:
1. **Reuse Context**: Create one context and reuse it
   ```kotlin
   companion object {
       private val jsContext = Context.newBuilder("js").build()
   }
   ```

2. **Warm-up JIT**: Run scripts multiple times to trigger optimization

3. **Use Native Functions**: Leverage built-in JavaScript functions

## Benchmark Results (Expected)

### Cold Start (First Execution)

| Test | Kotlin Total | TS Compilation | TS Execution | TS Total |
|------|--------------|----------------|--------------|----------|
| Simple Expression | 200ms | 3ms | 8ms | 11ms |
| Variables + Math | 250ms | 5ms | 12ms | 17ms |
| Fibonacci(20) | 400ms | 8ms | 220ms | 228ms |
| Array Processing | 350ms | 10ms | 120ms | 130ms |
| Loop (1-1000) | 280ms | 5ms | 75ms | 80ms |

### Warm (Subsequent Executions)

| Test | Kotlin Total | TS Compilation | TS Execution | TS Total |
|------|--------------|----------------|--------------|----------|
| Simple Expression | 50ms | 2ms | 5ms | 7ms |
| Variables + Math | 80ms | 3ms | 8ms | 11ms |
| Fibonacci(20) | 250ms | 5ms | 150ms | 155ms |
| Array Processing | 180ms | 6ms | 60ms | 66ms |
| Loop (1-1000) | 120ms | 3ms | 35ms | 38ms |

**Note**: These are estimated values. Actual performance depends on:
- Hardware (CPU, memory)
- JVM version and settings
- GraalVM version
- System load
- Script complexity

## Testing Methodology

### Running Comparison Tests

```bash
# Start the server
./gradlew run

# In another terminal, run comparison tests
./test-comparison.sh
```

### Manual Testing

**Kotlin:**
```bash
curl -X POST http://localhost:8080/execute/kotlin \
  -H "Content-Type: application/json" \
  -d '{"script":"val x = 10; val y = 20; x + y"}'
```

**TypeScript:**
```bash
curl -X POST http://localhost:8080/execute/typescript \
  -H "Content-Type: application/json" \
  -d '{"script":"const x = 10; const y = 20; x + y"}'
```

### Response Format

**Kotlin Response:**
```json
{
  "result": "30",
  "totalTimeMs": 245,
  "compilationTimeMs": null,
  "executionTimeMs": 245,
  "language": "kotlin",
  "error": null
}
```

**TypeScript Response:**
```json
{
  "result": "30",
  "totalTimeMs": 18,
  "compilationTimeMs": 5,
  "executionTimeMs": 13,
  "language": "typescript/javascript",
  "error": null
}
```

## Conclusion

Both Kotlin and TypeScript script execution have their place:

- **Kotlin excels** when type safety, language features, and JVM integration are priorities
- **TypeScript excels** when fast startup, visibility into compilation phases, and dynamic typing are acceptable

The choice depends on your specific use case, performance requirements, and whether you value type safety over raw speed.

For this implementation:
- Kotlin uses JSR-223 (standard but less visibility)
- TypeScript uses GraalVM Polyglot (modern, more control)

The **compilation time difference** is the most significant factor for short-lived scripts. For long-running computations, both perform similarly after JIT warmup.
