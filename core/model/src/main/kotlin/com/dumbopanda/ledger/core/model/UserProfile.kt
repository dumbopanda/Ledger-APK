package com.dumbopanda.ledger.core.model

import java.time.Instant

data class UserProfile(
    val userId: String,
    val displayName: String?,
    val plan: String,
    val entitlementExpiresAt: Instant?,
    val country: String?,
    val currency: String,
    val entryCount: Int,
    val onboardedAt: Instant?,
    val consentVersion: Int?,
    val consentAt: Instant?,
) {
    init {
        require(displayName == null || displayName.length <= 80)
    }

    val hasConsented get() = consentVersion != null && consentAt != null
}
