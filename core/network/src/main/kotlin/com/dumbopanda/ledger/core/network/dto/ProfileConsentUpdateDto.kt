package com.dumbopanda.ledger.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// §7.2 pane 3 / §11.4: the PATCH body for `profiles.consent_version` / `consent_at` /
// `onboarded_at` (0001_extensions_and_profiles.sql). Only these three columns -- `plan`,
// `entitlement_expires_at` and `entry_count` are guarded server-side by
// protect_profile_columns() (0005_rls.sql) regardless of what a client sends, so there is no
// need to round-trip them here just to leave them unchanged.
@Serializable
data class ProfileConsentUpdateDto(
    @SerialName("consent_version") val consentVersion: Int,
    @SerialName("consent_at") val consentAt: String,
    @SerialName("onboarded_at") val onboardedAt: String,
)
