/*
 * Copyright (c) 2023-2024 Oleg Yukhnevich. Use of this source code is governed by the Apache 2.0 license.
 */

package dev.whyoleg.cryptography.testtool.client

import dev.whyoleg.cryptography.testtool.api.*

public expect val TesttoolClient: TesttoolApi

internal expect fun hostOverride(): String?
