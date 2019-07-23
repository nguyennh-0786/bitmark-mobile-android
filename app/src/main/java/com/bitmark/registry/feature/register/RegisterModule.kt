package com.bitmark.registry.feature.register

import com.bitmark.registry.di.FragmentScope
import com.bitmark.registry.feature.Navigator
import dagger.Module
import dagger.Provides


/**
 * @author Hieu Pham
 * @since 7/4/19
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
@Module
class RegisterModule {

    @Provides
    @FragmentScope
    fun provideNavigator(
        fragment: RegisterFragment
    ): Navigator<RegisterFragment> {
        return Navigator(fragment)
    }

}