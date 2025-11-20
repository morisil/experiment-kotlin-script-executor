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

        get("/") {
            call.respondText(
                """
                Kotlin Script Executor Server

                POST /execute
                Body: { "script": "your kotlin code here" }

                Example:
                curl -X POST http://localhost:8080/execute \
                  -H "Content-Type: application/json" \
                  -d '{"script":"println(\"Hello, World!\"); 42"}'
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
