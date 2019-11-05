/**
 * SPDX-License-Identifier: ISC
 * Copyright © 2014-2019 Bitmark. All rights reserved.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */
package com.bitmark.registry.data.source.remote.api.middleware

import javax.inject.Inject

class CoreApiInterceptor @Inject constructor() : Interceptor() {

    override fun getTag(): String? = "CoreApi"
}