/*
 * Copyright 2014-2021 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.server.webjars

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.routing.core.*
import io.ktor.routing.core.application.*
import io.ktor.server.http.content.*
import io.ktor.server.plugins.cachingheaders.*
import io.ktor.server.plugins.conditionalheaders.*
import io.ktor.server.response.*
import io.ktor.server.testing.*
import io.ktor.util.date.*
import kotlin.test.*
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.seconds

class WebjarsTest {

    @Test
    fun resourceNotFound() {
        testApplication {
            install(Webjars)
            client.get("/webjars/foo.js").let { response ->
                // Should be handled by some other routing
                assertEquals(HttpStatusCode.NotFound, response.status)
            }
        }
    }

    @Test
    fun pathLike() {
        testApplication {
            install(Webjars)
            routing {
                get("/webjars-something/jquery") {
                    call.respondText { "Something Else" }
                }
            }
            client.get("/webjars-something/jquery").let { response ->
                assertEquals(HttpStatusCode.OK, response.status)
                assertEquals("Something Else", response.bodyAsText())
            }
        }
    }

    @Test
    fun nestedPath() {
        testApplication {
            install(Webjars) {
                path = "/assets/webjars"
            }
            client.get("/assets/webjars/jquery/jquery.js").let { response ->
                assertEquals(HttpStatusCode.OK, response.status)
                assertEquals("application/javascript", response.headers["Content-Type"])
            }
        }
    }

    @Test
    fun rootPath() {
        testApplication {
            install(Webjars) {
                path = "/"
            }
            client.get("/jquery/jquery.js").let { response ->
                assertEquals(HttpStatusCode.OK, response.status)
                assertEquals("application/javascript", response.headers["Content-Type"])
            }
        }
    }

    @Test
    fun rootPath2() {
        testApplication {
            install(Webjars) {
                path = "/"
            }
            routing {
                get("/") { call.respondText("Hello, World") }
            }
            client.get("/").let { response ->
                assertEquals(HttpStatusCode.OK, response.status)
                assertEquals("Hello, World", response.bodyAsText())
            }
            client.get("/jquery/jquery.js").let { response ->
                assertEquals(HttpStatusCode.OK, response.status)
                assertEquals("application/javascript", response.headers["Content-Type"])
            }
        }
    }

    @Test
    fun versionAgnostic() {
        testApplication {
            install(Webjars)

            client.get("/webjars/jquery/jquery.js").let { response ->
                assertEquals(HttpStatusCode.OK, response.status)
                assertEquals("application/javascript", response.headers["Content-Type"])
            }
        }
    }

    @Test
    fun withGetParameters() {
        testApplication {
            install(Webjars)

            client.get("/webjars/jquery/jquery.js?param1=value1").let { response ->
                assertEquals(HttpStatusCode.OK, response.status)
                assertEquals("application/javascript", response.headers["Content-Type"])
            }
        }
    }

    @Test
    fun withSpecificVersion() {
        testApplication {
            install(Webjars)

            client.get("/webjars/jquery/3.6.4/jquery.js").let { response ->
                assertEquals(HttpStatusCode.OK, response.status)
                assertEquals("application/javascript", response.headers["Content-Type"])
            }
        }
    }

    @Test
    fun isStaticContentReturnsTrue() {
        var isStatic = false
        var location = ""
        testApplication {
            install(Webjars)
            install(
                createApplicationPlugin("TestResponseMeta") {
                    onCallRespond { call ->
                        isStatic = call.isStaticContent()
                        location = call.attributes[StaticFileLocationProperty]
                    }
                }
            )

            client.get("/webjars/jquery/jquery.js")

            assertTrue(isStatic, "Should be static file")
            assertEquals(location, "jquery/jquery.js")
        }
    }

    @Test
    fun withConditionalAndCachingHeaders() {
        testApplication {
            install(Webjars)
            install(ConditionalHeaders)
            install(CachingHeaders)
            client.get("/webjars/jquery/3.6.4/jquery.js").let { response ->
                assertEquals(HttpStatusCode.OK, response.status)
                assertEquals("application/javascript", response.headers["Content-Type"])
                assertNotNull(response.headers["Last-Modified"])
                assertEquals("\"3.6.4\"", response.headers["Etag"])
                assertEquals("max-age=${90.days.inWholeSeconds}", response.headers["Cache-Control"])
            }
        }
    }

    @Test
    fun withConditionalAndCachingHeadersCustom() {
        testApplication {
            val date = GMTDate()
            install(Webjars) {
                maxAge { 5.seconds }
                lastModified { date }
                etag { "test" }
            }
            install(ConditionalHeaders)
            install(CachingHeaders)
            client.get("/webjars/jquery/3.6.4/jquery.js").let { response ->
                assertEquals(HttpStatusCode.OK, response.status)
                assertEquals("application/javascript", response.headers["Content-Type"])
                assertEquals(date.toHttpDate(), response.headers["Last-Modified"])
                assertEquals("\"test\"", response.headers["Etag"])
                assertEquals("max-age=5", response.headers["Cache-Control"])
            }
        }
    }

    @Test
    fun callHandledBeforeWebjars() {
        val alwaysRespondHello = object : Hook<Unit> {
            override fun install(pipeline: ApplicationCallPipeline, handler: Unit) {
                pipeline.intercept(ApplicationCallPipeline.Setup) {
                    call.respond("Hello")
                }
            }
        }
        val pluginBeforeWebjars = createApplicationPlugin("PluginBeforeWebjars") {
            on(alwaysRespondHello, Unit)
        }

        testApplication {
            install(pluginBeforeWebjars)
            install(Webjars)

            val response = client.get("/webjars/jquery/3.3.1/jquery.js")
            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals("Hello", response.bodyAsText())
            assertNotEquals("application/javascript", response.headers["Content-Type"])
        }
    }
}
