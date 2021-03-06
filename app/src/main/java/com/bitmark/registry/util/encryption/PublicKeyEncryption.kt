/**
 * SPDX-License-Identifier: ISC
 * Copyright © 2014-2019 Bitmark. All rights reserved.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */
package com.bitmark.registry.util.encryption

interface PublicKeyEncryption {

    fun encrypt(message: ByteArray, receiverPubKey: ByteArray): ByteArray

    fun decrypt(cipher: ByteArray, senderPubKey: ByteArray): ByteArray
}