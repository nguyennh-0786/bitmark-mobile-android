package com.bitmark.registry.feature.main.properties.yours

import com.bitmark.registry.data.source.AccountRepository
import com.bitmark.registry.data.source.BitmarkRepository
import com.bitmark.registry.di.FragmentScope
import com.bitmark.registry.feature.DialogController
import com.bitmark.registry.feature.Navigator
import com.bitmark.registry.util.livedata.RxLiveDataTransformer
import dagger.Module
import dagger.Provides


/**
 * @author Hieu Pham
 * @since 2019-07-09
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
@Module
class YourPropertiesModule {

    @Provides
    @FragmentScope
    fun provideViewModel(
        accountRepo: AccountRepository,
        bitmarkRepo: BitmarkRepository,
        rxLiveDataTransformer: RxLiveDataTransformer
    ): YourPropertiesViewModel {
        return YourPropertiesViewModel(
            accountRepo,
            bitmarkRepo,
            rxLiveDataTransformer
        )
    }

    @Provides
    @FragmentScope
    fun provideNavigator(
        fragment: YourPropertiesFragment
    ): Navigator<YourPropertiesFragment> {
        return Navigator(fragment)
    }

    @Provides
    @FragmentScope
    fun provideDialogController(
        fragment: YourPropertiesFragment
    ): DialogController {
        return DialogController(fragment.activity!!)
    }
}