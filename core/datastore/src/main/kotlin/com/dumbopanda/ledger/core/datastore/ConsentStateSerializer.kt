package com.dumbopanda.ledger.core.datastore

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import com.google.protobuf.InvalidProtocolBufferException
import java.io.InputStream
import java.io.OutputStream

object ConsentStateSerializer : Serializer<ConsentState> {
    override val defaultValue: ConsentState = ConsentState.getDefaultInstance()

    override suspend fun readFrom(input: InputStream): ConsentState =
        try {
            ConsentState.parseFrom(input)
        } catch (e: InvalidProtocolBufferException) {
            throw CorruptionException("Cannot read ConsentState proto", e)
        }

    override suspend fun writeTo(t: ConsentState, output: OutputStream) = t.writeTo(output)
}
