package com.dumbopanda.ledger.core.database.converter

import androidx.room.TypeConverter
import com.dumbopanda.ledger.core.model.TxnDirection
import com.dumbopanda.ledger.core.model.TxnFrequency

// String values match the server's Postgres enum labels exactly (0003_entries.sql)
// so a raw row can move between local storage and the wire without a translation
// table anywhere else in the app.
class EnumConverters {
    @TypeConverter
    fun fromDirection(value: TxnDirection): String = when (value) {
        TxnDirection.Debit -> "debit"
        TxnDirection.Credit -> "credit"
    }

    @TypeConverter
    fun toDirection(value: String): TxnDirection = when (value) {
        "debit" -> TxnDirection.Debit
        "credit" -> TxnDirection.Credit
        else -> error("Unknown txn_direction: $value")
    }

    @TypeConverter
    fun fromFrequency(value: TxnFrequency): String = when (value) {
        TxnFrequency.OneOff -> "one_off"
        TxnFrequency.Monthly -> "monthly"
    }

    @TypeConverter
    fun toFrequency(value: String): TxnFrequency = when (value) {
        "one_off" -> TxnFrequency.OneOff
        "monthly" -> TxnFrequency.Monthly
        else -> error("Unknown txn_frequency: $value")
    }
}
