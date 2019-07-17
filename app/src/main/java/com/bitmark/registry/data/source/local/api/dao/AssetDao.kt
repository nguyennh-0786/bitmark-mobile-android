package com.bitmark.registry.data.source.local.api.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.bitmark.registry.data.model.AssetData
import io.reactivex.Completable
import io.reactivex.Maybe


/**
 * @author Hieu Pham
 * @since 2019-07-10
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
@Dao
abstract class AssetDao {

    @Query("SELECT * FROM Asset WHERE id = :id")
    abstract fun getById(id: String): Maybe<AssetData>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun save(assets: List<AssetData>): Completable

}