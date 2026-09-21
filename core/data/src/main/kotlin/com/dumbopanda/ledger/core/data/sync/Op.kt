package com.dumbopanda.ledger.core.data.sync

// A user intention not yet sent to the server (§5.1). `payloadJson` always
// carries the full current row shape, deleted_at included -- push_entries
// treats every op as an upsert (§4.6), so even a Delete must send accurate
// values for every other column or it would clobber them server-side on
// conflict.
sealed interface Op {
    val entityType: String // "entry" | "account"
    val entityClientUid: String
    val payloadJson: String

    data class Insert(
        override val entityType: String,
        override val entityClientUid: String,
        override val payloadJson: String,
    ) : Op

    data class Update(
        override val entityType: String,
        override val entityClientUid: String,
        override val payloadJson: String,
    ) : Op

    data class Delete(
        override val entityType: String,
        override val entityClientUid: String,
        override val payloadJson: String,
    ) : Op
}

internal fun Op.typeName(): String = when (this) {
    is Op.Insert -> "insert"
    is Op.Update -> "update"
    is Op.Delete -> "delete"
}
