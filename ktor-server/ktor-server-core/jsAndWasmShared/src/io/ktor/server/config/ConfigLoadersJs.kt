/*
 * Copyright 2014-2022 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.server.config

internal actual val CONFIG_PATH: List<String> get() = emptyList()

/**
 * List of all registered [ConfigLoader] implementations.
 */
public actual val configLoaders: List<ConfigLoader>
    get() = emptyList()
