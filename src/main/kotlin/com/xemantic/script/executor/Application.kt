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
    val compilationTimeMs: Long? = null,
    val executionTimeMs: Long? = null,
    val language: String,
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
                val result = executeTypeScript(request.script)
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

        get("/") {
            call.respondText(
                """
                Script Executor Server (Kotlin vs TypeScript)

                POST /execute - Execute Kotlin script (simple response)
                POST /execute/kotlin - Execute Kotlin script (detailed timing)
                POST /execute/typescript - Execute TypeScript script (detailed timing)

                Body: { "script": "your code here" }

                Kotlin Example:
                curl -X POST http://localhost:8080/execute/kotlin \
                  -H "Content-Type: application/json" \
                  -d '{"script":"println(\"Hello, Kotlin!\"); 42"}'

                TypeScript Example:
                curl -X POST http://localhost:8080/execute/typescript \
                  -H "Content-Type: application/json" \
                  -d '{"script":"const x = 10; const y = 20; x + y"}'
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

fun executeTypeScript(script: String): DetailedScriptResponse {
    var result: Any? = null
    var error: String? = null
    var compilationTime: Long = 0
    var executionTime: Long = 0

    val totalTime = measureTimeMillis {
        try {
            // Create GraalVM JavaScript context
            Context.newBuilder("js")
                .allowAllAccess(true)
                .build().use { context ->

                    // TypeScript compilation phase
                    // For simplicity, we'll use TypeScript as JavaScript (no type checking)
                    // In a real implementation, you'd use the TypeScript compiler

                    // Measure compilation (parsing) time
                    var source: Source? = null
                    compilationTime = measureTimeMillis {
                        source = Source.newBuilder("js", script, "script.ts").build()
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
        compilationTimeMs = compilationTime,
        executionTimeMs = executionTime,
        language = "typescript/javascript",
        error = error
    )
}
