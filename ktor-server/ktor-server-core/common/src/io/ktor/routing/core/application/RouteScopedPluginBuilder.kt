/*
 * Copyright 2014-2024 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.routing.core.application

import io.ktor.routing.core.*
import io.ktor.util.*

/**
 * Utility class to build a [RouteScopedPlugin] instance.
 **/
public abstract class RouteScopedPluginBuilder<PluginConfig : Any>(key: AttributeKey<PluginInstance>) :
    PluginBuilder<PluginConfig>(key) {

    /**
     * A [RouteNode] to which this plugin was installed. Can be `null` if plugin in installed into [Application].
     **/
    public abstract val route: RouteNode?
}
