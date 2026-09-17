package com.dumbopanda.ledger.core.datastore

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import com.google.protobuf.InvalidProtocolBufferException
import java.io.InputStream
import java.io.OutputStream

object SyncCursorsSerializer : Serializer<SyncCursors> {
    override val defaultValue: SyncCursors = SyncCursors.getDefaultInstance()

    override suspend fun readFrom(input: InputStream): SyncCursors =
        try {
            SyncCursors.parseFrom(input)
        } catch (e: InvalidProtocolBufferException) {
            throw CorruptionException("Cannot read SyncCursors proto", e)
        }

    override suspend fun writeTo(t: SyncCursors, output: OutputStream) = t.writeTo(output)
}
