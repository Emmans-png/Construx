package com.collins.todo.data.Models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Product(
    val id: Int? = null,
    val name: String,
    val description: String,
    val price: Double,
    val stock: Int,
    @SerialName("image_url")
    val imageUrl: String? = null
)

@Serializable
data class Order(
    val id: Int? = null,
    @SerialName("user_id")
    val userId: String? = null,
    @SerialName("customer_name")
    val customerName: String,
    val status: String = "Pending", // Pending, Processing, Shipped, Completed
    @SerialName("total_amount")
    val totalAmount: Double,
    @SerialName("created_at")
    val createdAt: Long? = null
)

@Serializable
data class InvestmentPlan(
    val id: Int? = null,
    @SerialName("user_id")
    val userId: String? = null,
    val name: String,
    @SerialName("initial_cost")
    val initialCost: Double,
    @SerialName("expected_monthly_return")
    val expectedMonthlyReturn: Double,
    @SerialName("duration_months")
    val durationMonths: Int,
    @SerialName("risk_level")
    val riskLevel: String // Low, Medium, High
)

@Serializable
data class Profile(
    val id: String,
    val username: String? = null,
    val email: String? = null,
    @SerialName("organization_name")
    val organizationName: String? = null,
    @SerialName("organization_id")
    val organizationId: String? = null,
    @SerialName("industry_type")
    val industryType: String? = "Shelter/Construction",
    val location: String? = null,
    val role: String? = null, // "Manager" or "Transporter"
    @SerialName("phone_number")
    val phoneNumber: String? = null,
    @SerialName("created_at")
    val createdAt: String? = null
)
