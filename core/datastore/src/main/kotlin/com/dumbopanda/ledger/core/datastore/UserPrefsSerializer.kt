package com.dumbopanda.ledger.core.datastore

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import com.google.protobuf.InvalidProtocolBufferException
import java.io.InputStream
import java.io.OutputStream

object UserPrefsSerializer : Serializer<UserPrefs> {
    override val defaultValue: UserPrefs =
        UserPrefs.getDefaultInstance().toBuilder().setSyncMode(SyncMode.SYNC_MODE_SMART).build()

    override suspend fun readFrom(input: InputStream): UserPrefs =
        try {
            UserPrefs.parseFrom(input)
        } catch (e: InvalidProtocolBufferException) {
            throw CorruptionException("Cannot read UserPrefs proto", e)
        }

    override suspend fun writeTo(t: UserPrefs, output: OutputStream) = t.writeTo(output)
}
