# Script Executor Server (Kotlin vs TypeScript)

A minimal Ktor server that executes both Kotlin and TypeScript scripts, measuring compilation and execution times to compare performance characteristics.

## Features

- **Dual language support**: Execute both Kotlin and TypeScript/JavaScript scripts
- **Detailed timing breakdown**: Separate compilation and execution time measurement for TypeScript
- **Performance comparison**: Compare Kotlin's full compilation vs TypeScript's lighter parsing
- **JSON API** with kotlinx.serialization
- **Error handling** with detailed error messages
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

### 3. Execute TypeScript Script

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
  "compilationTimeMs": 5,
  "executionTimeMs": 13,
  "language": "typescript/javascript",
  "error": null
}
```

**Note**: TypeScript shows separate compilation (parsing) and execution times, highlighting the lightweight compilation phase.

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

Two test scripts are provided to measure and compare performance:

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

### Documentation
- [PERFORMANCE.md](PERFORMANCE.md) - Kotlin performance analysis
- [COMPARISON.md](COMPARISON.md) - Detailed Kotlin vs TypeScript comparison with:
  - Architecture differences
  - Compilation phase breakdowns
  - Expected performance benchmarks
  - Use case recommendations

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
