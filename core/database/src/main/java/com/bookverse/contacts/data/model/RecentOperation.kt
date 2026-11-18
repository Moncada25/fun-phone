package com.bookverse.contacts.data.model

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey

@Entity(tableName = "recent_operations")
data class RecentOperation(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @Embedded(prefix = "contact_") val contact: Contact,
    val date: Long,
    val type: RecentType,
    val target: String,
    val time: Long? = null,
    @Ignore val source: RecentOperationSource = RecentOperationSource.LOCAL,
) {
    constructor(
        id: Long,
        contact: Contact,
        date: Long,
        type: RecentType,
        target: String,
        time: Long?,
    ) : this(id, contact, date, type, target, time, RecentOperationSource.LOCAL)
}

enum class RecentOperationSource(val keyPrefix: String) {
    LOCAL("local"),
    CALL_LOG("call_log"),
}

val RecentOperation.historyKey: String
    get() = "${source.keyPrefix}:$id"

data class RecentOperationIdsBySource(
    val localIds: List<Long>,
    val callLogIds: List<Long>,
)

fun Collection<RecentOperation>.idsBySource(): RecentOperationIdsBySource {
    val (callLogOps, localOps) = partition { it.source == RecentOperationSource.CALL_LOG }
    return RecentOperationIdsBySource(
        localIds = localOps.map { it.id }.distinct(),
        callLogIds = callLogOps.map { it.id }.distinct(),
    )
}

enum class RecentType {
    WHATSAPP_OPEN,
    CALL_RECEIVED,
    CALL_SENT,
    CALL_MISSED,
    CALL_REJECTED,
    CALL_BLOCKED,
    CALL_UNKNOWN
}
