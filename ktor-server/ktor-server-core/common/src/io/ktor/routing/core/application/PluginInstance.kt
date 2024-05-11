/*
 * Copyright 2014-2024 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.routing.core.application

/**
 * An instance of the plugin installed to your application.
 * */
public class PluginInstance internal constructor(internal val builder: PluginBuilder<*>)
