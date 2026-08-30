package io.th3n3rd.ktor.client.chaos

import io.ktor.client.engine.*
import io.ktor.client.request.*
import io.ktor.http.*
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

@OptIn(InternalAPI::class)
suspend fun respondOk(content: String = ""): HttpResponseData =
    HttpResponseData(
        HttpStatusCode.OK,
        GMTDate(),
        headersOf(),
        HttpProtocolVersion.HTTP_1_1,
        ByteReadChannel(content.toByteArray(Charsets.UTF_8)),
        callContext()
    )