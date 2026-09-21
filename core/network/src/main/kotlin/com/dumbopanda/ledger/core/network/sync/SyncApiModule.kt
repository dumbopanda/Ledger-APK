package com.dumbopanda.ledger.core.network.sync

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SyncApiModule {
    @Binds
    @Singleton
    abstract fun bindSyncApi(impl: SupabaseSyncApi): SyncApi
}
