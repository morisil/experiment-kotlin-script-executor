package com.xemantic.script.executor

import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import org.graalvm.polyglot.Context
import org.graalvm.polyglot.Source
import javax.script.ScriptEngineManager
import kotlin.system.measureTimeMillis

@Serializable
data class ScriptRequest(
    val script: String
)

@Serializable
data class ScriptResponse(
    val result: String?,
    val executionTimeMs: Long,
    val error: String? = null
)

@Serializable
data class DetailedScriptResponse(
    val result: String?,
    val totalTimeMs: Long,
    val typeCheckTimeMs: Long? = null,
    val compilationTimeMs: Long? = null,
    val executionTimeMs: Long? = null,
    val language: String,
    val typeErrors: List<String>? = null,
    val error: String? = null
)

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0") {
        configureRouting()
    }.start(wait = true)
}

fun Application.configureRouting() {
    install(ContentNegotiation) {
        json()
    }

    routing {
        post("/execute") {
            try {
                val request = call.receive<ScriptRequest>()
                val result = executeScript(request.script)
                call.respond(result)
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    ScriptResponse(
                        result = null,
                        executionTimeMs = 0,
                        error = e.message ?: "Unknown error"
                    )
                )
            }
        }

        post("/execute/kotlin") {
            try {
                val request = call.receive<ScriptRequest>()
                val result = executeKotlinDetailed(request.script)
                call.respond(result)
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    DetailedScriptResponse(
                        result = null,
                        totalTimeMs = 0,
                        language = "kotlin",
                        error = e.message ?: "Unknown error"
                    )
                )
            }
        }

        post("/execute/typescript") {
            try {
                val request = call.receive<ScriptRequest>()
                val result = executeTypeScript(request.script, typeCheck = false)
                call.respond(result)
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    DetailedScriptResponse(
                        result = null,
                        totalTimeMs = 0,
                        language = "typescript",
                        error = e.message ?: "Unknown error"
                    )
                )
            }
        }

        post("/execute/typescript-checked") {
            try {
                val request = call.receive<ScriptRequest>()
                val result = executeTypeScript(request.script, typeCheck = true)
                call.respond(result)
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    DetailedScriptResponse(
                        result = null,
                        totalTimeMs = 0,
                        language = "typescript-checked",
                        error = e.message ?: "Unknown error"
                    )
                )
            }
        }

        get("/") {
            call.respondText(
                """
                Script Executor Server (Kotlin vs TypeScript)

                POST /execute - Execute Kotlin script (simple response)
                POST /execute/kotlin - Execute Kotlin script (detailed timing)
                POST /execute/typescript - Execute TypeScript script (no type checking)
                POST /execute/typescript-checked - Execute TypeScript script (with type checking)

                Body: { "script": "your code here" }

                Kotlin Example:
                curl -X POST http://localhost:8080/execute/kotlin \
                  -H "Content-Type: application/json" \
                  -d '{"script":"println(\"Hello, Kotlin!\"); 42"}'

                TypeScript Example (no type checking):
                curl -X POST http://localhost:8080/execute/typescript \
                  -H "Content-Type: application/json" \
                  -d '{"script":"const x = 10; const y = 20; x + y"}'

                TypeScript Example (with type checking):
                curl -X POST http://localhost:8080/execute/typescript-checked \
                  -H "Content-Type: application/json" \
                  -d '{"script":"const x: number = 10; const y: number = 20; x + y"}'
                """.trimIndent(),
                ContentType.Text.Plain
            )
        }
    }
}

fun executeScript(script: String): ScriptResponse {
    val engine = ScriptEngineManager().getEngineByExtension("kts")
        ?: return ScriptResponse(
            result = null,
            executionTimeMs = 0,
            error = "Kotlin script engine not found"
        )

    var result: Any? = null
    var error: String? = null

    val executionTime = measureTimeMillis {
        try {
            result = engine.eval(script)
        } catch (e: Exception) {
            error = e.message ?: "Script execution failed"
        }
    }

    return ScriptResponse(
        result = result?.toString(),
        executionTimeMs = executionTime,
        error = error
    )
}

fun executeKotlinDetailed(script: String): DetailedScriptResponse {
    val engine = ScriptEngineManager().getEngineByExtension("kts")
        ?: return DetailedScriptResponse(
            result = null,
            totalTimeMs = 0,
            language = "kotlin",
            error = "Kotlin script engine not found"
        )

    var result: Any? = null
    var error: String? = null

    val totalTime = measureTimeMillis {
        try {
            result = engine.eval(script)
        } catch (e: Exception) {
            error = e.message ?: "Script execution failed"
        }
    }

    return DetailedScriptResponse(
        result = result?.toString(),
        totalTimeMs = totalTime,
        compilationTimeMs = null, // Kotlin JSR-223 doesn't separate compilation
        executionTimeMs = totalTime,
        language = "kotlin",
        error = error
    )
}

fun executeTypeScript(script: String, typeCheck: Boolean = false): DetailedScriptResponse {
    var result: Any? = null
    var error: String? = null
    var typeCheckTime: Long? = null
    var compilationTime: Long = 0
    var executionTime: Long = 0
    var typeErrors: List<String>? = null

    val totalTime = measureTimeMillis {
        try {
            // Create GraalVM JavaScript context
            Context.newBuilder("js")
                .allowAllAccess(true)
                .build().use { context ->

                    // Type checking phase (if enabled)
                    if (typeCheck) {
                        typeCheckTime = measureTimeMillis {
                            typeErrors = performTypeCheck(script, context)
                        }

                        // If there are type errors, don't execute
                        if (typeErrors?.isNotEmpty() == true) {
                            error = "Type checking failed"
                            return@use
                        }
                    }

                    // Strip TypeScript type annotations for execution
                    val jsScript = stripTypeAnnotations(script)

                    // Measure compilation (parsing) time
                    var source: Source? = null
                    compilationTime = measureTimeMillis {
                        source = Source.newBuilder("js", jsScript, "script.ts").build()
                    }

                    // Measure execution time
                    executionTime = measureTimeMillis {
                        val value = context.eval(source)
                        result = if (value.isNull) null else value.toString()
                    }
                }
        } catch (e: Exception) {
            error = e.message ?: "TypeScript execution failed"
        }
    }

    return DetailedScriptResponse(
        result = result,
        totalTimeMs = totalTime,
        typeCheckTimeMs = typeCheckTime,
        compilationTimeMs = compilationTime,
        executionTimeMs = executionTime,
        language = if (typeCheck) "typescript-checked" else "typescript/javascript",
        typeErrors = typeErrors,
        error = error
    )
}

fun performTypeCheck(script: String, context: Context): List<String> {
    val errors = mutableListOf<String>()

    try {
        // Load the type checker JavaScript
        val checkerScript = object {}.javaClass.getResourceAsStream("/typescript-checker.js")
            ?.bufferedReader()
            ?.use { it.readText() }
            ?: return listOf("Type checker not found")

        // Evaluate the checker function
        context.eval("js", checkerScript)

        // Call the type checker
        val checkResult = context.eval("js", "typeCheckScript(${escapeForJs(script)})")

        // Extract errors
        if (checkResult.hasMember("errors")) {
            val errorsArray = checkResult.getMember("errors")
            if (errorsArray.hasArrayElements()) {
                for (i in 0 until errorsArray.arraySize) {
                    errors.add(errorsArray.getArrayElement(i).asString())
                }
            }
        }
    } catch (e: Exception) {
        errors.add("Type checking error: ${e.message}")
    }

    return errors
}

fun stripTypeAnnotations(script: String): String {
    // Simple regex-based type annotation stripping
    var cleaned = script

    // Remove type annotations like : type
    cleaned = cleaned.replace(Regex(""":\s*\w+(\[\])?(<[^>]+>)?(\s*\||,|\)|\s*=|\s*;)"""), "$3")

    // Remove interface declarations
    cleaned = cleaned.replace(Regex("""interface\s+\w+\s*\{[^}]*\}"""), "")

    // Remove type declarations
    cleaned = cleaned.replace(Regex("""type\s+\w+\s*=\s*[^;]+;"""), "")

    // Remove as type assertions
    cleaned = cleaned.replace(Regex("""\s+as\s+\w+"""), "")

    // Remove <type> assertions
    cleaned = cleaned.replace(Regex("""<\w+>"""), "")

    return cleaned
}

fun escapeForJs(script: String): String {
    return "`" + script.replace("`", "\\`").replace("$", "\\$") + "`"
}
