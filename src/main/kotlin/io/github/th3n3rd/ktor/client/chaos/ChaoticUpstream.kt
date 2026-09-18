package io.github.th3n3rd.ktor.client.chaos

import io.github.th3n3rd.ktor.client.chaos.ChaoticUpstream.Handler
import io.github.th3n3rd.ktor.client.chaos.Routing.Route.Companion.RoutingAttributes
import io.ktor.client.engine.mock.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.http.HttpMethod.Companion.Delete
import io.ktor.http.HttpMethod.Companion.Get
import io.ktor.http.HttpMethod.Companion.Head
import io.ktor.http.HttpMethod.Companion.Options
import io.ktor.http.HttpMethod.Companion.Post
import io.ktor.http.HttpMethod.Companion.Put
import io.ktor.http.HttpStatusCode.Companion.NotFound
import io.ktor.util.*
import io.ktor.utils.io.*
import java.util.*

abstract class ChaoticUpstream(
    val url: Url = Url("https://upstream-${UUID.randomUUID()}.internal")
) {
    private val engine = ChaosEngine(MockEngine(routes()))

    @OptIn(InternalAPI::class)
    suspend operator fun invoke(request: HttpRequestData): HttpResponseData = engine.execute(request)

    fun misbehave(behaviour: ChaosBehaviour) = engine.misbehave(behaviour)

    fun misbehave(stage: ChaosStage) = engine.misbehave(stage)

    abstract fun routes(): Handler

    typealias Handler = suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData
}

fun ChaoticUpstream.routing(block: Routing.() -> Unit): Handler {
    return Routing().apply(block).build()
}

class Routing {
    private val routes = mutableListOf<Route>()
    private var orElse: Handler = { respondError(NotFound) }

    fun get(path: String, handler: Handler) {
        routes += Route(Get, path, handler)
    }

    fun post(path: String, handler: Handler) {
        routes += Route(Post, path, handler)
    }

    fun put(path: String, handler: Handler) {
        routes += Route(Put, path, handler)
    }

    fun delete(path: String, handler: Handler) {
        routes += Route(Delete, path, handler)
    }

    fun head(path: String, handler: Handler) {
        routes += Route(Head, path, handler)
    }

    fun options(path: String, handler: Handler) {
        routes += Route(Options, path, handler)
    }

    fun orElse(handler: Handler) {
        orElse = handler
    }

    fun build(): Handler = { request ->
        routes
            .firstOrNull { it.matches(request) }
            ?.handler(this, request)
            ?: orElse(this, request)
    }

    data class Route(
        val method: HttpMethod,
        val path: String,
        val handler: Handler
    ) {
        private val parameterNames = TemplatedPathPattern
            .findAll(path)
            .map { it.groupValues[1] }
            .toList()

        private val pathPattern = TemplatedPathPattern
            .replace(path, "([^/]+)")
            .let { Regex("^$it$") }

        fun matches(request: HttpRequestData): Boolean {
            if (request.method != method) {
                return false
            }

            val match = pathPattern.matchEntire(request.url.encodedPath)
                ?: return false

            request.attributes.put(
                RoutingAttributes,
                parameterNames.zip(match.groupValues.drop(1)).toMap()
            )

            return true
        }

        companion object {
            private val TemplatedPathPattern = Regex("""\{([^}]+)\}""")
            val RoutingAttributes = AttributeKey<Map<String, String>>("RoutingAttributes")
        }
    }
}

val HttpRequestData.parameters get() = attributes[RoutingAttributes]