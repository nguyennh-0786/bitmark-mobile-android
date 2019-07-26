package com.bitmark.registry.data.source.local.api.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy.REPLACE
import androidx.room.Query
import com.bitmark.registry.data.model.TransactionData
import io.reactivex.Completable
import io.reactivex.Single


/**
 * @author Hieu Pham
 * @since 2019-07-10
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
@Dao
abstract class TransactionDao {

    @Insert(onConflict = REPLACE)
    abstract fun save(txs: List<TransactionData>): Completable

    @Query("SELECT * FROM `Transaction` WHERE bitmark_id = :bitmarkId AND status IN (:status) ORDER BY `offset` DESC LIMIT :limit")
    abstract fun listByBitmarkIdStatusLimitDesc(
        bitmarkId: String,
        status: Array<TransactionData.Status>,
        limit: Int
    ): Single<List<TransactionData>>

    @Query("DELETE FROM `Transaction` WHERE bitmark_id = :bitmarkId ")
    abstract fun deleteTxsByBitmarkId(bitmarkId: String): Completable

    @Query("DELETE FROM `Transaction` WHERE bitmark_id IN (:bitmarkIds) ")
    abstract fun deleteTxsByBitmarkIds(bitmarkIds: List<String>): Completable

    @Query("SELECT * FROM `Transaction` WHERE (owner = :owner OR previous_owner = :previousOwner) AND status IN (:status) AND `offset` <= :offset ORDER BY `offset` DESC LIMIT :limit")
    abstract fun listTxsByOwnerOffsetStatusLimitDesc(
        owner: String,
        previousOwner: String,
        offset: Long,
        status: Array<TransactionData.Status>,
        limit: Int
    ): Single<List<TransactionData>>

    @Query("SELECT MAX(`offset`) FROM `Transaction` WHERE owner = :who OR previous_owner = :who")
    abstract fun maxRelevantOffset(who: String): Single<Long>

    @Query("SELECT * FROM `Transaction` WHERE owner = :owner AND status == :status ORDER BY `offset` DESC")
    abstract fun listByOwnerStatusDesc(
        owner: String,
        status: TransactionData.Status
    ): Single<List<TransactionData>>

    @Query("DELETE FROM `Transaction`")
    abstract fun delete(): Completable

}