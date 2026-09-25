package com.dumbopanda.ledger.core.network.dto

import kotlinx.serialization.Serializable

// §7.1's forced-update row / 0001_extensions_and_profiles.sql's public.app_config: `key text
// primary key, value jsonb`. Every row this app reads happens to hold a plain JSON integer
// (min_supported_build, max_entries_per_user, sync_page_size), so `value` decodes directly as
// Int rather than modelling jsonb's full generality for a table nothing writes from the client.
@Serializable
data class AppConfigRowDto(
    val key: String,
    val value: Int,
)
