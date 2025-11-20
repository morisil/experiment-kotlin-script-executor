# Kotlin Script Executor

A minimal Ktor server that executes Kotlin scripts and returns results with execution time measurement.

## Features

- **Single endpoint** for script execution: `POST /execute`
- **Execution time measurement** in milliseconds
- **JSON API** with kotlinx.serialization
- **Error handling** with detailed error messages

## API

### Execute Script

**Endpoint:** `POST /execute`

**Request:**
```json
{
  "script": "println(\"Hello, World!\"); 42"
}
```

**Response (Success):**
```json
{
  "result": "42",
  "executionTimeMs": 15,
  "error": null
}
```

**Response (Error):**
```json
{
  "result": null,
  "executionTimeMs": 0,
  "error": "Script execution failed: ..."
}
```

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

## Performance Testing

A comprehensive performance test script is provided to measure response times:

```bash
# Start the server
./gradlew run

# In another terminal, run the performance tests
./test-performance.sh
```

See [PERFORMANCE.md](PERFORMANCE.md) for detailed performance analysis, expected response times, and optimization recommendations.

## Usage Examples

### Using curl
```bash
# Simple script
curl -X POST http://localhost:8080/execute \
  -H "Content-Type: application/json" \
  -d '{"script":"println(\"Hello, World!\"); 42"}'

# Math operations
curl -X POST http://localhost:8080/execute \
  -H "Content-Type: application/json" \
  -d '{"script":"val x = 10; val y = 20; x + y"}'

# List operations
curl -X POST http://localhost:8080/execute \
  -H "Content-Type: application/json" \
  -d '{"script":"listOf(1, 2, 3, 4, 5).sum()"}'
```

### Using HTTPie
```bash
http POST http://localhost:8080/execute script="println(\"Hello\"); 42"
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
└── README.md
```

## Technologies

- **Ktor 3.0.3** - Web framework
- **Kotlin 2.2.21** - Language
- **kotlin-scripting-jsr223** - Script execution engine
- **kotlinx.serialization** - JSON serialization
- **Netty** - HTTP server engine
- **Logback** - Logging

## Code Overview

The server is implemented in `src/main/kotlin/com/xemantic/script/executor/Application.kt:1` with:

1. **Data models** for request/response:
   - `ScriptRequest`: Contains the script to execute
   - `ScriptResponse`: Contains result, execution time, and optional error

2. **Script executor function** (`executeScript`):
   - Uses JSR-223 Kotlin script engine
   - Measures execution time using `measureTimeMillis`
   - Returns structured response with result or error

3. **Ktor routing**:
   - `POST /execute` - Main script execution endpoint
   - `GET /` - Help message with usage instructions

## License

Apache License 2.0
