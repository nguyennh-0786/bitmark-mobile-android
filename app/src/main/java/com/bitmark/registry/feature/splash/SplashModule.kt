package com.bitmark.registry.feature.splash

import com.bitmark.registry.data.source.AccountRepository
import com.bitmark.registry.data.source.AppRepository
import com.bitmark.registry.data.source.BitmarkRepository
import com.bitmark.registry.di.ActivityScope
import com.bitmark.registry.feature.DialogController
import com.bitmark.registry.feature.Navigator
import com.bitmark.registry.feature.realtime.WebSocketEventBus
import com.bitmark.registry.util.livedata.RxLiveDataTransformer
import dagger.Module
import dagger.Provides


/**
 * @author Hieu Pham
 * @since 7/1/19
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
@Module
class SplashModule {

    @Provides
    @ActivityScope
    fun provideViewModel(
        activity: SplashActivity,
        accountRepo: AccountRepository,
        appRepo: AppRepository,
        bitmarkRepo: BitmarkRepository,
        rxLiveDataTransformer: RxLiveDataTransformer,
        wsEventBus: WebSocketEventBus
    ): SplashViewModel {
        return SplashViewModel(
            activity.lifecycle,
            accountRepo,
            appRepo,
            bitmarkRepo,
            rxLiveDataTransformer,
            wsEventBus
        )
    }

    @Provides
    @ActivityScope
    fun provideNavigator(activity: SplashActivity) = Navigator(activity)

    @Provides
    @ActivityScope
    fun provideDialogController(
        activity: SplashActivity
    ): DialogController {
        return DialogController(activity)
    }
}