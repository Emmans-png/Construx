package com.collins.todo.data.repository

import com.collins.todo.data.Models.InvestmentPlan
import com.collins.todo.data.Models.Order
import com.collins.todo.data.Models.Product
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from

class BusinessRepository {
    private val supabase = SupabaseClient.client

    // Product methods
    suspend fun getProducts(): List<Product> = 
        supabase.from("products").select().decodeList<Product>()

    // Order methods
    suspend fun getOrders(): List<Order> {
        val user = supabase.auth.currentUserOrNull() ?: return emptyList()
        return supabase.from("orders").select {
            filter { eq("user_id", user.id) }
        }.decodeList<Order>()
    }

    suspend fun createOrder(order: Order): Order? {
        val user = supabase.auth.currentUserOrNull() ?: return null
        return supabase.from("orders").insert(order.copy(userId = user.id)) {
            select()
        }.decodeSingleOrNull<Order>()
    }

    // Investment methods
    suspend fun getInvestmentPlans(): List<InvestmentPlan> {
        val user = supabase.auth.currentUserOrNull() ?: return emptyList()
        return supabase.from("investment_plans").select {
            filter { eq("user_id", user.id) }
        }.decodeList<InvestmentPlan>()
    }

    suspend fun createInvestmentPlan(plan: InvestmentPlan): InvestmentPlan? {
        val user = supabase.auth.currentUserOrNull() ?: return null
        return supabase.from("investment_plans").insert(plan.copy(userId = user.id)) {
            select()
        }.decodeSingleOrNull<InvestmentPlan>()
    }
}
