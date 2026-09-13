package io.github.th3n3rd.ktor.client.chaos

import io.ktor.client.engine.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.http.HttpStatusCode.Companion.OK
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.date.*
import io.ktor.utils.io.*
import io.ktor.utils.io.charsets.*
import io.ktor.utils.io.core.*
import java.util.*

fun anyRequest(): HttpRequestBuilder {
    val builder = HttpRequestBuilder()
    builder.method = HttpMethod.Get
    builder.url.takeFrom("https://example.com/tests/${UUID.randomUUID()}")
    return builder
}

fun returnText(text: String): ChaosBehaviour = { _, _ -> respondOk(text) }

@OptIn(InternalAPI::class)
suspend fun respondOk(content: String = ""): HttpResponseData =
    HttpResponseData(
        OK,
        GMTDate(),
        headersOf(),
        HttpProtocolVersion.HTTP_1_1,
        ByteReadChannel(content.toByteArray(Charsets.UTF_8)),
        callContext()
    )

@OptIn(InternalAPI::class)
suspend fun respond(
    statusCode: HttpStatusCode,
    headers: Headers,
    content: String,
): HttpResponseData =
    HttpResponseData(
        statusCode = statusCode,
        requestTime = GMTDate(),
        headers = headers,
        version = HttpProtocolVersion.HTTP_1_1,
        body = ByteReadChannel(content.toByteArray(Charsets.UTF_8)),
        callContext = callContext()
    )

class UpstreamServer(val staticContent: String) {
    private val server = embeddedServer(CIO, port = 0) {
        routing {
            route("{...}") {
                handle {
                    call.respondText(staticContent, status = OK)
                }
            }
        }
    }.start(wait = false)

    suspend fun url() = "http://localhost:${server.engine.resolvedConnectors().first().port}"
}