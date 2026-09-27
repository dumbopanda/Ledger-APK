package com.dumbopanda.ledger.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// C14: the columns ProfileSetupScreen reads/writes on `profiles` (0012_profile_details.sql).
// `display_name` already existed (0001); age/location/occupation are new. Separate from
// ProfileConsentUpdateDto since the two are written at different points in the boot flow and
// PATCHing only the fields that actually changed keeps each request's intent legible.

@Serializable
data class ProfileDetailsFetchDto(
    @SerialName("display_name") val displayName: String? = null,
    val age: Int? = null,
    val location: String? = null,
    val occupation: String? = null,
)

@Serializable
data class ProfileDetailsUpdateDto(
    @SerialName("display_name") val displayName: String,
    val age: Int? = null,
    val location: String? = null,
    val occupation: String? = null,
)
