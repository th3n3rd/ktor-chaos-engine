package io.github.th3n3rd.ktor.client.chaos

import io.ktor.client.engine.*
import io.ktor.client.engine.mock.*
import io.ktor.client.request.*
import io.ktor.http.*

class ReverseProxy(
    private vararg val mappings: Pair<String, suspend (HttpRequestData) -> HttpResponseData>
) : HttpClientEngine by MockEngine ({ request ->
    mappings.firstOrNull { it.first == request.url.host }
        ?.second(request)
        ?: respondError(HttpStatusCode.NotFound, """Reverse proxy: no upstream found for "${request.url.host}"""")
})