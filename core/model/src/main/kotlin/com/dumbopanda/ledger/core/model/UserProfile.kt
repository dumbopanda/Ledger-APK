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
    // C14: collected once, right after first sign-in, regardless of sign-in method -- see
    // ProfileSetupScreen. All three are optional even once collection has happened; only
    // displayName is ever required, since it's the one field personalization can't work
    // without at all.
    val age: Int? = null,
    val location: String? = null,
    val occupation: String? = null,
) {
    init {
        require(displayName == null || displayName.length <= 80)
        require(location == null || location.length <= 80)
        require(age == null || age in 13..120)
        require(occupation == null || occupation in ProfileOccupations.OPTIONS)
    }

    val hasConsented get() = consentVersion != null && consentAt != null

    // Whether ProfileSetupScreen has already run for this account -- displayName is the one
    // field it always sets (Google-prefilled or typed), so its presence is the marker.
    val hasProfileDetails get() = displayName != null
}

// C14: "15 preset values... that is enough" -- a fixed list, no free-text "Other" elaboration.
object ProfileOccupations {
    val OPTIONS = listOf(
        "Student", "Software / IT", "Business owner", "Doctor / Healthcare", "Teacher / Educator",
        "Engineer", "Finance / Banking", "Government employee", "Homemaker", "Retired",
        "Freelancer", "Sales / Marketing", "Artist / Creative", "Farmer", "Other",
    )
}
