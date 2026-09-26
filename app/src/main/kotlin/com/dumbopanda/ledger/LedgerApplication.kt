package com.dumbopanda.ledger

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import io.sentry.android.core.SentryAndroid
import javax.inject.Inject

@HiltAndroidApp
class LedgerApplication : Application(), Configuration.Provider {
    @Inject lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

    override fun onCreate() {
        super.onCreate()
        // §11.5: crash reports carry the class of error and the screen, never
        // the user's data. sendDefaultPii off means the SDK never attaches
        // device user/IP on its own; beforeSend is a last-resort net in case
        // anything upstream ever accidentally passed an amount/name/note
        // through as an event message or extra.
        SentryAndroid.init(this) { options ->
            options.dsn = BuildConfig.SENTRY_DSN
            options.isSendDefaultPii = false
            options.tracesSampleRate = 0.0
            options.beforeSend = io.sentry.SentryOptions.BeforeSendCallback { event, _ ->
                event.removeExtra("amount")
                event.removeExtra("expenseName")
                event.removeExtra("moreInfo")
                event.request = null
                event
            }
        }
    }
}
