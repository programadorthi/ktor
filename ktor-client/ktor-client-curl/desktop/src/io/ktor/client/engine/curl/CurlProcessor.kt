/*
 * Copyright 2014-2019 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.client.engine.curl

import io.ktor.client.engine.curl.internal.*
import io.ktor.util.*
import kotlinx.atomicfu.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import kotlin.coroutines.*

internal class RequestContainer(
    val requestData: CurlRequestData,
    val completionHandler: CompletableDeferred<CurlSuccess>
)

internal class CurlProcessor(coroutineContext: CoroutineContext) {
    @OptIn(InternalAPI::class)
    private val curlDispatcher: CloseableCoroutineDispatcher =
        Dispatchers.createFixedThreadDispatcher("curl-dispatcher", 1)

    private var curlApi: CurlMultiApiHandler? by atomic(null)
    private val curlScope = CoroutineScope(coroutineContext + curlDispatcher)
    private val requestQueue: Channel<RequestContainer> = Channel(Channel.UNLIMITED)

    init {
        val init = curlScope.launch {
            curlApi = CurlMultiApiHandler()
            runEventLoop()
        }

        runBlocking {
            init.join()
        }
    }

    suspend fun executeRequest(request: CurlRequestData): CurlSuccess {
        val result = CompletableDeferred<CurlSuccess>()
        requestQueue.send(RequestContainer(request, result))
        curlApi?.wakeup()
        return result.await()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun runEventLoop() {
        curlScope.launch {
            val api = curlApi!!
            while (!requestQueue.isClosedForReceive) {
                drainRequestQueue(api)
                api.perform()
            }
        }
    }

    private suspend fun drainRequestQueue(api: CurlMultiApiHandler) {
        val api = curlApi!!

        while (true) {
            val container = if (api.hasHandlers()) {
                requestQueue.tryReceive()
            } else {
                requestQueue.receiveCatching()
            }.getOrNull() ?: break

            val requestHandler = api.scheduleRequest(container.requestData, container.completionHandler)

            val requestCleaner = container.requestData.executionContext.invokeOnCompletion { cause ->
                if (cause == null) return@invokeOnCompletion
                cancelRequest(requestHandler, cause)
            }

            container.completionHandler.invokeOnCompletion {
                requestCleaner.dispose()
            }
        }
    }

    fun close() {
        requestQueue.close()
        curlScope.coroutineContext[Job]!!.invokeOnCompletion {
            curlDispatcher.close()
            curlApi!!.close()
        }
    }

    private fun cancelRequest(easyHandle: EasyHandle, cause: Throwable) {
        curlScope.launch {
            curlApi?.cancelRequest(easyHandle, cause)
        }
    }
}
