package com.collins.todo.data.Models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ConstructionProject(
    val id: Int? = null,
    @SerialName("user_id")
    val userId: String? = null,
    @SerialName("organization_id")
    val organizationId: String? = null,
    val name: String,
    val location: String,
    @SerialName("total_budget")
    val totalBudget: Double,
    @SerialName("land_cost")
    val landCost: Double,
    @SerialName("target_rental_income")
    val targetRentalIncome: Double,
    @SerialName("current_stage")
    val currentStage: String = "Foundation" // Foundation, Walling, Roofing, Finishing
)

@Serializable
data class MaterialOrder(
    val id: Int? = null,
    @SerialName("user_id")
    val userId: String? = null,
    @SerialName("project_id")
    val projectId: Int,
    @SerialName("material_name")
    val materialName: String,
    val quantity: Double,
    val unit: String,
    @SerialName("unit_price")
    val unitPrice: Double,
    val status: String = "Pending", // Pending, Dispatched, Ongoing, Arrived, Unloading, Delivered
    @SerialName("required_stage")
    val requiredStage: String,
    @SerialName("supplier_name")
    val supplierName: String,
    @SerialName("transporter_id")
    val transporterId: String? = null,
    @SerialName("organization_id")
    val organizationId: String? = null,
    @SerialName("estimated_days")
    val estimatedDays: Int? = null,
    val earnings: Double? = null
)

@Serializable
data class ROISimulation(
    val totalInvestment: Double,
    val monthlyIncome: Double,
    val breakEvenYears: Double,
    val annualYield: Double
)

@Serializable
data class TeamMember(
    val id: Int? = null,
    @SerialName("manager_id")
    val managerId: String? = null,
    val name: String,
    val role: String,
    @SerialName("phone_number")
    val phoneNumber: String,
    val status: String = "Active" // Active, On Leave, Inactive
)

@Serializable
data class Message(
    val id: Int? = null,
    @SerialName("sender_id")
    val senderId: String,
    @SerialName("receiver_id")
    val receiverId: String,
    val content: String,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("is_read")
    val isRead: Boolean = false
)

@Serializable
data class DriverLocation(
    val id: Int? = null,
    @SerialName("driver_id")
    val driverId: String,
    val latitude: Double,
    val longitude: Double,
    @SerialName("updated_at")
    val updatedAt: String? = null
)
