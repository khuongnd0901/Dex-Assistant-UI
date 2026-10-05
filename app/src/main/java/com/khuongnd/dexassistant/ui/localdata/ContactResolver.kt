package com.khuongnd.dexassistant.ui.localdata

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.ContactsContract
import android.provider.ContactsContract.CommonDataKinds.Phone

data class ContactCandidate(val rowId: Long, val contactId: Long, val lookupKey: String,
    val name: String, val number: String)

object ContactIdentity {
    private val permitted = Regex("^\\+?[0-9 ()-]+$")
    private val phone = Regex("^\\+?[0-9]{3,15}$")
    fun normalizedNumber(raw: String): String? {
        if (!permitted.matches(raw)) return null
        return raw.replace(" ", "").replace("(", "").replace(")", "").replace("-", "").takeIf(phone::matches)
    }
    fun unchanged(before: ContactCandidate, current: ContactCandidate?): Boolean = current != null &&
        before.rowId == current.rowId && before.contactId == current.contactId &&
        before.lookupKey == current.lookupKey && before.name == current.name && before.number == current.number
}

class ContactResolver(private val context: Context, private val aliases: LocalAliases = LocalAliases(context)) {
    fun hasPermission(): Boolean = context.checkSelfPermission(Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
    fun resolveExact(query: String, cancellation: android.os.CancellationSignal? = null): List<ContactCandidate> {
        if (!hasPermission()) return emptyList()
        val target = AliasRules.key(aliases.contact(query))
        // Match full normalized names. No fuzzy fallback and no contact data sent remotely.
        return read(null, null, cancellation, target)
            .distinctBy { Triple(it.contactId, it.name, it.number) }.toList()
    }
    fun revalidate(candidate: ContactCandidate): Boolean = hasPermission() &&
        ContactIdentity.unchanged(candidate, read("${ContactsContract.CommonDataKinds.Phone._ID} = ?", arrayOf(candidate.rowId.toString())).singleOrNull())
    private fun read(selection: String?, args: Array<String>?, cancellation: android.os.CancellationSignal? = null, target: String? = null): List<ContactCandidate> {
        val output = mutableListOf<ContactCandidate>()
        context.contentResolver.query(Phone.CONTENT_URI,
            arrayOf(Phone._ID, Phone.CONTACT_ID, Phone.LOOKUP_KEY, Phone.DISPLAY_NAME, Phone.NUMBER), selection, args,
            "${Phone.DISPLAY_NAME} ASC, ${Phone._ID} ASC", cancellation)?.use { cursor ->
            while (cursor.moveToNext()) {
                cancellation?.throwIfCanceled()
                val name = cursor.getString(3).orEmpty()
                if (target != null && AliasRules.key(name) != target) continue
                val number = ContactIdentity.normalizedNumber(cursor.getString(4).orEmpty()) ?: continue
                output += ContactCandidate(cursor.getLong(0), cursor.getLong(1), cursor.getString(2).orEmpty(),
                    name, number)
            }
        }
        return output.toList()
    }
}
