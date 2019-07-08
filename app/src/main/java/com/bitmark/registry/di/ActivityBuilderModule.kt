package com.bitmark.registry.di

import com.bitmark.registry.feature.main.MainActivity
import com.bitmark.registry.feature.main.MainModule
import com.bitmark.registry.feature.register.RegisterActivity
import com.bitmark.registry.feature.register.RegisterModule
import com.bitmark.registry.feature.register.authentication.AuthenticationActivity
import com.bitmark.registry.feature.register.authentication.AuthenticationModule
import com.bitmark.registry.feature.register.recoveryphrase.RecoveryPhraseActivity
import com.bitmark.registry.feature.register.recoveryphrase.RecoveryPhraseModule
import com.bitmark.registry.feature.splash.SplashActivity
import com.bitmark.registry.feature.splash.SplashModule
import dagger.Module
import dagger.android.ContributesAndroidInjector


/**
 * @author Hieu Pham
 * @since 7/1/19
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
@Module
abstract class ActivityBuilderModule {

    @ContributesAndroidInjector(modules = [MainModule::class])
    @ActivityScope
    internal abstract fun bindMainActivity(): MainActivity

    @ContributesAndroidInjector(modules = [SplashModule::class])
    @ActivityScope
    internal abstract fun bindSplashActivity(): SplashActivity

    @ContributesAndroidInjector(modules = [AuthenticationModule::class])
    @ActivityScope
    internal abstract fun bindAuthenticationActivity(): AuthenticationActivity

    @ContributesAndroidInjector(modules = [RegisterModule::class])
    @ActivityScope
    internal abstract fun bindRegisterActivity(): RegisterActivity

    @ContributesAndroidInjector(modules = [RecoveryPhraseModule::class])
    @ActivityScope
    internal abstract fun bindRecoveryPhraseActivity(): RecoveryPhraseActivity
}