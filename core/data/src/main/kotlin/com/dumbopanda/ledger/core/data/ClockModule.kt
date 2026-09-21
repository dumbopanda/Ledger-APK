package com.dumbopanda.ledger.core.data

import com.dumbopanda.ledger.core.common.LedgerClock
import com.dumbopanda.ledger.core.common.SystemLedgerClock
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ClockModule {
    @Provides
    @Singleton
    fun provideLedgerClock(): LedgerClock = SystemLedgerClock
}
