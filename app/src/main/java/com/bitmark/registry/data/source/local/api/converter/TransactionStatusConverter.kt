/**
 * SPDX-License-Identifier: ISC
 * Copyright © 2014-2019 Bitmark. All rights reserved.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */
package com.bitmark.registry.data.source.local.api.converter

import androidx.room.TypeConverter
import com.bitmark.registry.data.model.TransactionData

class TransactionStatusConverter {

    @TypeConverter
    fun toString(status: TransactionData.Status) = status.value

    @TypeConverter
    fun fromString(status: String) = TransactionData.Status.from(status)
}