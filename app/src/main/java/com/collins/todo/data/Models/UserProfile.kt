package com.collins.todo.data.Models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserProfile(
    val id: String,
    val username: String,
    val email: String,
    @SerialName("organization_name")
    val organizationName: String? = null,
    @SerialName("organization_id")
    val organizationId: String? = null,
    @SerialName("industry_type")
    val industryType: String? = null,
    val location: String? = null,
    val role: String,
    @SerialName("phone_number")
    val phoneNumber: String? = null,
    @SerialName("preferred_site_id")
    val preferredSiteId: Int? = null
)
