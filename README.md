# Script Executor Server (Kotlin vs TypeScript)

A minimal Ktor server that executes both Kotlin and TypeScript scripts, measuring compilation and execution times to compare performance characteristics.

## Features

- **Dual language support**: Execute both Kotlin and TypeScript/JavaScript scripts
- **TypeScript type checking**: Optional type validation with separate timing measurement
- **Detailed timing breakdown**: Separate type-checking, compilation, and execution phases
- **Performance comparison**: Compare Kotlin's full compilation vs TypeScript with/without type checking
- **JSON API** with kotlinx.serialization
- **Error handling** with detailed type error reporting
- **GraalVM JavaScript engine** for optimized TypeScript/JavaScript execution

## API

### 1. Execute Kotlin Script (Simple)

**Endpoint:** `POST /execute`

**Request:**
```json
{
  "script": "println(\"Hello, World!\"); 42"
}
```

**Response:**
```json
{
  "result": "42",
  "executionTimeMs": 245,
  "error": null
}
```

### 2. Execute Kotlin Script (Detailed)

**Endpoint:** `POST /execute/kotlin`

**Request:**
```json
{
  "script": "val x = 10; val y = 20; x + y"
}
```

**Response:**
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

**Note**: Kotlin's JSR-223 engine doesn't separate compilation from execution, so both times are the same.

### 3. Execute TypeScript Script (No Type Checking)

**Endpoint:** `POST /execute/typescript`

**Request:**
```json
{
  "script": "const x = 10; const y = 20; x + y"
}
```

**Response:**
```json
{
  "result": "30",
  "totalTimeMs": 18,
  "typeCheckTimeMs": null,
  "compilationTimeMs": 5,
  "executionTimeMs": 13,
  "language": "typescript/javascript",
  "typeErrors": null,
  "error": null
}
```

**Note**: Without type checking, TypeScript runs as JavaScript - fast parsing with no type validation.

### 4. Execute TypeScript Script (With Type Checking)

**Endpoint:** `POST /execute/typescript-checked`

**Request:**
```json
{
  "script": "const x: number = 10; const y: number = 20; x + y"
}
```

**Response (Success):**
```json
{
  "result": "30",
  "totalTimeMs": 25,
  "typeCheckTimeMs": 5,
  "compilationTimeMs": 8,
  "executionTimeMs": 12,
  "language": "typescript-checked",
  "typeErrors": [],
  "error": null
}
```

**Response (Type Error):**
```json
{
  "result": null,
  "totalTimeMs": 8,
  "typeCheckTimeMs": 7,
  "compilationTimeMs": 0,
  "executionTimeMs": 0,
  "language": "typescript-checked",
  "typeErrors": [
    "Type 'string' is not assignable to type 'number' for variable 'x'"
  ],
  "error": "Type checking failed"
}
```

**Note**: Type checking adds overhead but provides type safety similar to Kotlin. Timing breakdown shows:
- `typeCheckTimeMs`: Time spent validating types
- `compilationTimeMs`: Parsing after type validation
- `executionTimeMs`: Actual script execution

## Building and Running

### Build the project
```bash
./gradlew build
```

### Run the server
```bash
./gradlew run
```

Or build a fat JAR and run it:
```bash
./gradlew buildFatJar
java -jar build/libs/script-executor.jar
```

The server will start on `http://0.0.0.0:8080`

## Performance Testing & Comparison

Three test scripts are provided to measure and compare performance:

### Kotlin-only Performance Tests
```bash
# Start the server
./gradlew run

# In another terminal, run Kotlin performance tests
./test-performance.sh
```

### Kotlin vs TypeScript Comparison
```bash
# Start the server
./gradlew run

# In another terminal, run comparison tests
./test-comparison.sh
```

The comparison script runs equivalent scripts in both languages and shows:
- **Kotlin**: Total compilation + execution time (combined)
- **TypeScript**: Separate compilation (parsing) and execution times

### TypeScript Type Checking Tests
```bash
# Start the server
./gradlew run

# In another terminal, test type checking
./test-typescript-types.sh
```

The type checking test demonstrates:
- **Valid TypeScript**: Types are validated, then script executes
- **Type Errors**: Caught before execution with detailed error messages
- **Performance Impact**: `typeCheckTimeMs` shows type validation overhead
- **Comparison**: With vs without type checking timing differences

### Documentation
- [PERFORMANCE.md](PERFORMANCE.md) - Kotlin performance analysis
- [COMPARISON.md](COMPARISON.md) - Detailed Kotlin vs TypeScript comparison with:
  - Architecture differences
  - Compilation phase breakdowns
  - Expected performance benchmarks
  - Use case recommendations
  - Type checking vs no type checking comparison

## Usage Examples

### Kotlin Examples

```bash
# Simple expression
curl -X POST http://localhost:8080/execute/kotlin \
  -H "Content-Type: application/json" \
  -d '{"script":"42"}'

# Variables and math
curl -X POST http://localhost:8080/execute/kotlin \
  -H "Content-Type: application/json" \
  -d '{"script":"val x = 10; val y = 20; x + y"}'

# List operations
curl -X POST http://localhost:8080/execute/kotlin \
  -H "Content-Type: application/json" \
  -d '{"script":"listOf(1, 2, 3, 4, 5).sum()"}'

# Fibonacci
curl -X POST http://localhost:8080/execute/kotlin \
  -H "Content-Type: application/json" \
  -d '{"script":"fun fib(n: Int): Int = if (n <= 1) n else fib(n-1) + fib(n-2); fib(20)"}'
```

### TypeScript Examples

```bash
# Simple expression
curl -X POST http://localhost:8080/execute/typescript \
  -H "Content-Type: application/json" \
  -d '{"script":"42"}'

# Variables and math
curl -X POST http://localhost:8080/execute/typescript \
  -H "Content-Type: application/json" \
  -d '{"script":"const x = 10; const y = 20; x + y"}'

# Array operations
curl -X POST http://localhost:8080/execute/typescript \
  -H "Content-Type: application/json" \
  -d '{"script":"[1, 2, 3, 4, 5].reduce((a, b) => a + b, 0)"}'

# Fibonacci
curl -X POST http://localhost:8080/execute/typescript \
  -H "Content-Type: application/json" \
  -d '{"script":"function fib(n) { return n <= 1 ? n : fib(n-1) + fib(n-2); } fib(20)"}'
```

### TypeScript with Type Checking Examples

```bash
# Valid types - executes successfully
curl -X POST http://localhost:8080/execute/typescript-checked \
  -H "Content-Type: application/json" \
  -d '{"script":"const x: number = 10; const y: number = 20; x + y"}'

# Type error - caught before execution
curl -X POST http://localhost:8080/execute/typescript-checked \
  -H "Content-Type: application/json" \
  -d '{"script":"const name: string = 123"}'

# Typed function
curl -X POST http://localhost:8080/execute/typescript-checked \
  -H "Content-Type: application/json" \
  -d '{"script":"function add(a: number, b: number): number { return a + b; } add(5, 10)"}'
```

## Project Structure

```
.
├── build.gradle.kts              # Build configuration
├── gradle/
│   └── libs.versions.toml        # Dependency versions
├── src/
│   └── main/
│       ├── kotlin/
│       │   └── com/xemantic/script/executor/
│       │       └── Application.kt    # Main server code
│       └── resources/
│           └── logback.xml          # Logging configuration
├── test-performance.sh           # Kotlin performance tests
├── test-comparison.sh            # Kotlin vs TypeScript comparison tests
├── PERFORMANCE.md                # Kotlin performance analysis
├── COMPARISON.md                 # Detailed language comparison
└── README.md
```

## Technologies

- **Ktor 3.0.3** - Web framework
- **Kotlin 2.2.21** - Language
- **kotlin-scripting-jsr223** - Kotlin script execution engine (JSR-223)
- **GraalVM Polyglot 24.1.1** - JavaScript/TypeScript execution engine
- **kotlinx.serialization** - JSON serialization
- **Netty** - HTTP server engine
- **Logback** - Logging

## Code Overview

The server is implemented in `src/main/kotlin/com/xemantic/script/executor/Application.kt:1` with:

1. **Data models** for request/response:
   - `ScriptRequest`: Contains the script to execute
   - `ScriptResponse`: Simple response with result and execution time
   - `DetailedScriptResponse`: Extended response with compilation/execution breakdown

2. **Script executor functions**:
   - `executeScript()`: Original Kotlin executor (simple response)
   - `executeKotlinDetailed()`: Kotlin executor with detailed timing
   - `executeTypeScript()`: TypeScript/JavaScript executor with GraalVM
     - Separates compilation (parsing) from execution
     - Measures each phase independently

3. **Ktor routing**:
   - `POST /execute` - Kotlin script execution (simple response)
   - `POST /execute/kotlin` - Kotlin script execution (detailed timing)
   - `POST /execute/typescript` - TypeScript script execution (detailed timing)
   - `GET /` - Help message with usage instructions

## Key Implementation Details

### Kotlin Execution (JSR-223)
```kotlin
fun executeKotlinDetailed(script: String): DetailedScriptResponse {
    val engine = ScriptEngineManager().getEngineByExtension("kts")
    val totalTime = measureTimeMillis {
        result = engine.eval(script)  // Compilation + Execution combined
    }
    // JSR-223 doesn't separate compilation and execution
}
```

### TypeScript Execution (GraalVM)
```kotlin
fun executeTypeScript(script: String): DetailedScriptResponse {
    Context.newBuilder("js").build().use { context ->
        // Measure compilation (parsing) separately
        compilationTime = measureTimeMillis {
            source = Source.newBuilder("js", script, "script.ts").build()
        }

        // Measure execution separately
        executionTime = measureTimeMillis {
            result = context.eval(source)
        }
    }
}
```

The key difference is that **GraalVM allows measuring compilation and execution separately**, while **JSR-223 Kotlin scripting combines both phases**.

## License

Apache License 2.0
