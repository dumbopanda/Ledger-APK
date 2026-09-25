package com.dumbopanda.ledger.core.data.repository

import android.util.Log
import com.dumbopanda.ledger.core.network.dto.AppConfigRowDto
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * §7.1's forced-update row / `public.app_config` (0001_extensions_and_profiles.sql), read
 * directly -- no offline cache, no op log, this is a live "is this build still allowed" check,
 * and a stale cached answer is worse than no answer. [fetchMinSupportedBuild] returns null on
 * any failure (offline, server error, unexpected shape): [BootRepository] treats null the same
 * as "don't know," which means don't block -- the same "no wall on a plane" reasoning §7.1's
 * offline row is built on applies here too, and a version gate must never be the thing that
 * turns a network hiccup into every user's app refusing to open.
 */
@Singleton
class AppConfigRepository @Inject constructor(
    private val supabase: SupabaseClient,
) {
    suspend fun fetchMinSupportedBuild(): Int? = runCatching {
        supabase.postgrest.from("app_config")
            .select {
                filter { eq("key", "min_supported_build") }
            }
            .decodeSingle<AppConfigRowDto>()
            .value
    }.onFailure { e -> Log.w(TAG, "fetchMinSupportedBuild failed, treating as not required", e) }
        .getOrNull()

    private companion object {
        const val TAG = "AppConfigRepository"
    }
}
