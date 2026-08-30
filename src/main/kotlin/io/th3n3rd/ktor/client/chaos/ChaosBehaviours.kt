package io.th3n3rd.ktor.client.chaos

import io.ktor.client.engine.*
import io.ktor.client.engine.mock.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.utils.io.*

fun interface ChaosBehaviour {
    suspend operator fun invoke(
        request: HttpRequestData,
        next: suspend (HttpRequestData) -> HttpResponseData
    ): HttpResponseData
}

object ChaosBehaviours {
    object NoOp {
        operator fun invoke() = ChaosBehaviour { request, next -> next(request) }
    }

    @OptIn(InternalAPI::class)
    object ReturnStatus {
        operator fun invoke(status: HttpStatusCode) = ChaosBehaviour { request, _ ->
            MockRequestHandleScope(callContext()).respond("", status)
        }
    }
}