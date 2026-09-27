package com.dumbopanda.ledger.core.network.auth

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class EmailOtpApiModule {
    @Binds
    @Singleton
    abstract fun bindEmailOtpApi(impl: SupabaseEmailOtpApi): EmailOtpApi
}
