/*
* Copyright 2014-2021 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
*/

package io.ktor.server.engine

import io.ktor.routing.core.application.*
import io.ktor.server.config.*
import io.ktor.util.logging.*
import io.ktor.utils.io.*

/**
 * Engine environment configuration builder
 */
@KtorDsl
public expect class ApplicationEnvironmentBuilder() {

    /**
     * Application logger
     */
    public var log: Logger

    /**
     * Application config
     */
    public var config: ApplicationConfig

    /**
     * Build an application engine environment
     */
    public fun build(): ApplicationEnvironment
}

public fun applicationEnvironment(
    block: ApplicationEnvironmentBuilder.() -> Unit = {}
): ApplicationEnvironment {
    return ApplicationEnvironmentBuilder().apply(block).build()
}
