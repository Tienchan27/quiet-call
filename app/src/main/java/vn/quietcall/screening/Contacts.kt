package vn.quietcall.screening

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract

object Contacts {
    fun canRead(context: Context) = context.checkSelfPermission(Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
    fun contains(context: Context, number: String): Boolean {
        if (!canRead(context) || number.isBlank()) return false
        val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number))
        // A provider failure must reach the service's fail-open handler, rather than misclassify a contact.
        return context.contentResolver.query(uri, arrayOf(ContactsContract.PhoneLookup._ID), null, null, null)
            ?.use { it.moveToFirst() } ?: error("Không thể truy cập danh bạ")
    }
}
