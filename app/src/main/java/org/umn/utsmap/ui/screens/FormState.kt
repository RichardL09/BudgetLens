package org.umn.utsmap.ui.screens

import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import org.json.JSONArray
import org.json.JSONObject
import org.umn.utsmap.ui.finance.*

internal val StringMapSaver = Saver<Map<String, String>, String>(
    save = { JSONObject(it).toString() },
    restore = { value -> JSONObject(value).let { json -> json.keys().asSequence().associateWith { json.getString(it) } } }
)
internal val AllocationSaver = Saver<List<Map<String, String>>, String>(
    save = { rows -> JSONArray(rows.map { JSONObject(it) }).toString() },
    restore = { value -> JSONArray(value).let { rows -> List(rows.length()) { index ->
        rows.getJSONObject(index).let { json -> json.keys().asSequence().associateWith { json.getString(it) } }
    } } }
)
internal val MemberListSaver = Saver<SnapshotStateList<BillMember>, String>(
    save = { people -> JSONArray(people.map { person -> JSONObject().apply {
        put("id", person.id); put("name", person.name); put("phone", person.phone)
        put("self", person.self); put("paid", person.paid); put("status", person.status.name)
    } }).toString() },
    restore = { value -> JSONArray(value).let { people -> List(people.length()) { index ->
        people.getJSONObject(index).let { person -> BillMember(person.getString("id"), person.getString("name"),
            person.getString("phone"), person.getBoolean("self"), person.getBoolean("paid"), PaymentStatus.valueOf(person.getString("status"))) }
    }.toMutableStateList() } }
)
/** Only a completed manual receipt is kept here; unfinished fields have their own saveable state. */
internal val ReceiptDraftSaver = Saver<ReceiptScanViewModel, String>(
    save = { draft -> JSONObject().apply {
        put("title", draft.merchant); put("date", draft.date); put("tax", draft.taxPercent)
        put("category", draft.category); put("note", draft.note)
        put("items", JSONArray(draft.items.map { item -> JSONObject().apply {
            put("name", item.name); put("quantity", item.quantity); put("price", item.unitPrice)
        } }))
    }.toString() },
    restore = { value -> JSONObject(value).let { json -> ReceiptScanViewModel().apply {
        beginManual()
        val rows = json.getJSONArray("items")
        updateReceipt(json.getString("title"), json.getString("date"), List(rows.length()) { index ->
            rows.getJSONObject(index).let { ReceiptItem(it.getString("name"), it.getInt("quantity"), it.getLong("price")) }
        })
        taxPercent = json.getInt("tax"); category = json.getString("category"); note = json.getString("note")
    } } }
)

internal fun numberError(value: String, minimum: Long, maximum: Long, label: String): String? = when {
    value.isBlank() -> "$label wajib diisi."
    value.any { it !in '0'..'9' } -> "$label hanya boleh berisi angka."
    value.toLongOrNull() == null || value.toLong() !in minimum..maximum -> "$label harus $minimum–$maximum."
    else -> null
}
internal fun itemNameError(value: String): String? = when {
    value.isBlank() -> "Nama item wajib diisi."
    value.trim().length > 80 -> "Nama item maksimal 80 karakter."
    else -> null
}
