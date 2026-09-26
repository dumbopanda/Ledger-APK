package com.dumbopanda.ledger.core.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

/**
 * No-op if nothing on the device can handle it (e.g. no browser/mail app) --
 * a toast beats a crash for a Settings/About row.
 */
fun Context.openUrl(url: String) {
    runCatching {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }.onFailure {
        if (it is ActivityNotFoundException) Toast.makeText(this, "Couldn't open $url", Toast.LENGTH_SHORT).show() else throw it
    }
}

fun Context.openEmail(address: String) {
    runCatching {
        startActivity(
            Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$address")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }.onFailure {
        if (it is ActivityNotFoundException) Toast.makeText(this, "Couldn't open a mail app", Toast.LENGTH_SHORT).show() else throw it
    }
}
