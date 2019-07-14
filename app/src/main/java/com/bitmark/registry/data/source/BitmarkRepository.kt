package com.bitmark.registry.data.source

import com.bitmark.registry.data.model.AssetData
import com.bitmark.registry.data.model.BitmarkData
import com.bitmark.registry.data.model.mapHead
import com.bitmark.registry.data.source.local.BitmarkDeletedListener
import com.bitmark.registry.data.source.local.BitmarkInsertedListener
import com.bitmark.registry.data.source.local.BitmarkLocalDataSource
import com.bitmark.registry.data.source.local.BitmarkStatusChangedListener
import com.bitmark.registry.data.source.remote.BitmarkRemoteDataSource
import com.bitmark.registry.util.extension.append
import io.reactivex.Maybe
import io.reactivex.Single
import io.reactivex.functions.BiFunction
import io.reactivex.schedulers.Schedulers
import java.io.File


/**
 * @author Hieu Pham
 * @since 7/2/19
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
class BitmarkRepository(
    private val localDataSource: BitmarkLocalDataSource,
    private val remoteDataSource: BitmarkRemoteDataSource
) : AbsRepository() {

    fun setBitmarkDeletedListener(listener: BitmarkDeletedListener?) {
        localDataSource.setBitmarkDeletedListener(listener)
    }

    fun setBitmarkStatusChangedListener(listener: BitmarkStatusChangedListener?) {
        localDataSource.setBitmarkStatusChangedListener(listener)
    }

    fun setBitmarkInsertedListener(listener: BitmarkInsertedListener?) {
        localDataSource.setBitmarkInsertedListener(listener)
    }

    fun syncBitmarks(
        owner: String? = null,
        at: Long = 0,
        to: String = "earlier",
        limit: Int = 100,
        pending: Boolean = false,
        issuer: String? = null,
        refAssetId: String? = null
    ): Single<List<BitmarkData>> {
        return remoteDataSource.listBitmarks(
            owner = owner,
            at = at,
            to = to,
            limit = limit,
            pending = pending,
            issuer = issuer,
            refAssetId = refAssetId,
            loadAsset = true
        ).observeOn(Schedulers.io()).map { p ->
            val bitmarks = p.first.map { b ->
                BitmarkData(
                    b.id,
                    b.assetId,
                    b.blockNumber,
                    b.confirmedAt,
                    b.createdAt,
                    mapHead(b.head),
                    b.headId,
                    b.issuedAt,
                    b.issuer,
                    b.offset,
                    b.owner,
                    BitmarkData.map(b.status)
                )
            }

            val assets = p.second.map { a ->
                AssetData(
                    a.id,
                    a.blockNumber,
                    a.blockOffset,
                    a.createdAt,
                    a.expiredAt,
                    a.fingerprint,
                    a.metadata,
                    a.name,
                    a.offset,
                    a.registrant,
                    AssetData.map(a.status)
                )
            }

            Pair(bitmarks, assets)
        }.flatMap { p ->
            localDataSource.saveAssets(p.second)
                .andThen(localDataSource.saveBitmarks(p.first))
                .andThen(Single.just(p))
        }.map { p ->
            val bitmarks = p.first
            val assets = p.second
            assets.forEach { asset ->
                bitmarks.filter { b -> b.assetId == asset.id }
                    .forEach { b -> b.asset = asset }
            }
            bitmarks
        }
    }

    fun listBitmarks(
        owner: String,
        at: Long,
        limit: Int
    ): Maybe<List<BitmarkData>> {
        return localDataSource.countBitmark().flatMapMaybe { count ->
            if (count == 0L) {
                // database is empty, sync with remote data
                syncBitmarks(owner = owner, limit = limit).toMaybe()
            } else {
                localDataSource.listBitmarksByOffsetLimitDesc(at, limit)
                    .flatMap { bitmarks ->

                        // if database return empty, sync with remote data
                        if (bitmarks.isEmpty()) syncBitmarks(
                            owner = owner,
                            at = at,
                            to = "earlier",
                            limit = limit
                        ).toMaybe()
                        else Maybe.just(bitmarks)
                    }
                    .flatMap(attachAssetFunc())
            }
        }
    }

    fun listStoredPendingBitmarks(): Maybe<List<BitmarkData>> = Maybe.zip(
        localDataSource.listBitmarksByStatus(BitmarkData.Status.TRANSFERRING),
        localDataSource.listBitmarksByStatus(BitmarkData.Status.ISSUING),
        BiFunction<List<BitmarkData>, List<BitmarkData>, List<BitmarkData>> { transferring, issuing ->
            mutableListOf<BitmarkData>().append(transferring, issuing)
        }).flatMap(attachAssetFunc())

    private fun attachAssetFunc(): (List<BitmarkData>) -> Maybe<List<BitmarkData>> =
        { bitmarks ->

            if (bitmarks.isEmpty()) {
                Maybe.just(bitmarks)
            } else {
                // map asset to bitmark
                val assetStreams = mutableListOf<Maybe<AssetData>>()
                for (bitmark in bitmarks) {
                    assetStreams.add(
                        localDataSource.getAssetById(
                            bitmark.assetId
                        )
                    )
                }
                Maybe.merge(assetStreams).doOnNext { asset ->
                    bitmarks.filter { b -> b.assetId == asset.id }
                        .forEach { b -> b.asset = asset }
                }.lastOrError()
                    .flatMapMaybe { Maybe.just(bitmarks) }
            }
        }

    fun maxStoredBitmarkOffset(): Single<Long> =
        localDataSource.maxBitmarkOffset()

    fun minStoredBitmarkOffset(): Single<Long> =
        localDataSource.minBitmarkOffset()

    fun checkAssetFile(
        accountNumber: String,
        assetId: String
    ): Single<Pair<String, File?>> =
        localDataSource.checkAssetFile(accountNumber, assetId)

    fun countStoredBitmark(): Single<Long> = localDataSource.countBitmark()

}