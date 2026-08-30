package io.th3n3rd.ktor.client.chaos

import io.ktor.client.engine.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.util.date.*
import io.ktor.utils.io.*
import io.ktor.utils.io.charsets.*
import io.ktor.utils.io.core.*
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.time.delay
import java.time.Duration

fun interface ChaosBehaviour {
    suspend operator fun invoke(
        request: HttpRequestData,
        next: suspend (HttpRequestData) -> HttpResponseData
    ): HttpResponseData
}

object ChaosBehaviours {
    val None = ChaosBehaviour { request, next -> next(request) }

    @OptIn(InternalAPI::class)
    object ReturnStatus {
        operator fun invoke(status: HttpStatusCode) = ChaosBehaviour { _, _ ->
            HttpResponseData(
                status,
                GMTDate(),
                headersOf(),
                HttpProtocolVersion.HTTP_1_1,
                ByteReadChannel("".toByteArray(Charsets.UTF_8)),
                callContext()
            )
        }
    }

    object StripBody {
        operator fun invoke() = ChaosBehaviour { request, next ->
            val response = next(request)
            HttpResponseData(
                statusCode = response.statusCode,
                requestTime = response.requestTime,
                headers = response.headers,
                version = response.version,
                body = ByteReadChannel.Empty,
                callContext = response.callContext
            )
        }
    }

    object ThrowException {
        operator fun invoke(e: Throwable = RuntimeException("something went wrong!")) = ChaosBehaviour { _, _ ->
            throw e
        }
    }

    object Latency {
        operator fun invoke(duration: Duration) = ChaosBehaviour { request, next ->
            delay(duration)
            next(request)
        }
    }

    object SuspendForever {
        operator fun invoke() = ChaosBehaviour { _, _ ->
            awaitCancellation()
        }
    }

    object StreamBodyForever {
        operator fun invoke() = ChaosBehaviour { request, next ->
            val response = next(request)
            HttpResponseData(
                statusCode = response.statusCode,
                requestTime = response.requestTime,
                headers = response.headers,
                version = response.version,
                body = ByteChannel(),
                callContext = response.callContext,
            )
        }
    }
}