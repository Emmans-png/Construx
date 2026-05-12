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
    val preferredSiteId: Int? = null,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("profile_picture_url")
    val profilePictureUrl: String? = null,
    @SerialName("vehicle_plate")
    val vehiclePlate: String? = null,
    @SerialName("vehicle_model")
    val vehicleModel: String? = null,
    @SerialName("fuel_level")
    val fuelLevel: Int = 100,
    @SerialName("next_service_km")
    val nextServiceKm: Int = 5000,
    @SerialName("wallet_balance")
    val walletBalance: Double = 0.0
)
