package com.collins.todo.data.repository

import com.collins.todo.data.Models.ConstructionProject
import com.collins.todo.data.Models.MaterialOrder
import com.collins.todo.data.Models.UserProfile
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from

class ConstructionRepository {
    private val supabase = SupabaseClient.client

    // Profile methods
    suspend fun getUserProfile(): UserProfile? {
        val user = supabase.auth.currentUserOrNull() ?: return null
        return supabase.from("profiles").select {
            filter { eq("id", user.id) }
        }.decodeSingleOrNull<UserProfile>()
    }

    suspend fun updateUserProfile(profile: UserProfile): UserProfile? {
        return supabase.from("profiles").upsert(profile) {
            select()
        }.decodeSingleOrNull<UserProfile>()
    }

    suspend fun deleteUserProfile(userId: String) {
        supabase.from("profiles").delete {
            filter { eq("id", userId) }
        }
    }

    // Project methods
    suspend fun getProjects(): List<ConstructionProject> {
        val user = supabase.auth.currentUserOrNull() ?: return emptyList()
        val profile = getUserProfile() ?: return emptyList()
        
        return if (profile.role == "Manager") {
            supabase.from("construction_projects").select {
                filter { eq("user_id", user.id) }
            }.decodeList<ConstructionProject>()
        } else {
            // Transporters see projects within their organization
            supabase.from("construction_projects").select {
                filter { eq("organization_id", profile.organizationId ?: "") }
            }.decodeList<ConstructionProject>()
        }
    }

    suspend fun getProjectById(projectId: Int): ConstructionProject? {
        return supabase.from("construction_projects").select {
            filter { eq("id", projectId) }
        }.decodeSingleOrNull<ConstructionProject>()
    }

    suspend fun createProject(project: ConstructionProject): ConstructionProject? {
        val user = supabase.auth.currentUserOrNull() ?: return null
        val profile = getUserProfile() ?: return null
        return supabase.from("construction_projects").insert(
            project.copy(
                userId = user.id,
                organizationId = profile.organizationId
            )
        ) {
            select()
        }.decodeSingleOrNull<ConstructionProject>()
    }

    suspend fun updateProject(project: ConstructionProject): ConstructionProject? {
        return supabase.from("construction_projects").update(project) {
            filter { eq("id", project.id ?: return null) }
            select()
        }.decodeSingleOrNull<ConstructionProject>()
    }

    suspend fun deleteProject(projectId: Int) {
        supabase.from("construction_projects").delete {
            filter { eq("id", projectId) }
        }
    }

    // Material methods
    suspend fun getOrdersByProject(projectId: Int): List<MaterialOrder> = 
        supabase.from("material_orders").select {
            filter { eq("project_id", projectId) }
        }.decodeList<MaterialOrder>()

    suspend fun createMaterialOrder(order: MaterialOrder): MaterialOrder? = 
        supabase.from("material_orders").insert(order) {
            select()
        }.decodeSingleOrNull<MaterialOrder>()

    suspend fun updateMaterialOrder(order: MaterialOrder): MaterialOrder? =
        supabase.from("material_orders").update(order) {
            filter { eq("id", order.id ?: return null) }
            select()
        }.decodeSingleOrNull<MaterialOrder>()

    suspend fun deleteMaterialOrder(orderId: Int) {
        supabase.from("material_orders").delete {
            filter { eq("id", orderId) }
        }
    }

    suspend fun getTransporterOrders(): List<MaterialOrder> {
        val user = supabase.auth.currentUserOrNull() ?: return emptyList()
        val profile = getUserProfile() ?: return emptyList()
        return supabase.from("material_orders").select {
            filter {
                or {
                    eq("transporter_id", user.id)
                    eq("organization_id", profile.organizationId ?: "")
                }
                neq("status", "Delivered")
                neq("status", "Completed")
            }
        }.decodeList<MaterialOrder>()
    }

    suspend fun getAllDrivers(): List<UserProfile> {
        return try {
            supabase.from("profiles").select {
                filter { eq("role", "Transporter") }
            }.decodeList<UserProfile>()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    // Driver methods
    suspend fun getDriversByOrganization(orgId: String): List<UserProfile> =
        supabase.from("profiles").select {
            filter {
                eq("organization_id", orgId)
                eq("role", "Transporter")
            }
        }.decodeList<UserProfile>()

    // Team methods
    suspend fun getTeam(): List<com.collins.todo.data.Models.TeamMember> {
        val user = supabase.auth.currentUserOrNull() ?: return emptyList()
        return supabase.from("team_members").select {
            filter { eq("manager_id", user.id) }
        }.decodeList<com.collins.todo.data.Models.TeamMember>()
    }

    suspend fun addTeamMember(member: com.collins.todo.data.Models.TeamMember): com.collins.todo.data.Models.TeamMember? {
        val user = supabase.auth.currentUserOrNull() ?: return null
        return supabase.from("team_members").insert(member.copy(managerId = user.id)) {
            select()
        }.decodeSingleOrNull<com.collins.todo.data.Models.TeamMember>()
    }

    // Messaging methods
    suspend fun sendMessage(message: com.collins.todo.data.Models.Message): Boolean {
        return try {
            supabase.from("messages").insert(message)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun getMessagesForUser(): List<com.collins.todo.data.Models.Message> {
        val user = supabase.auth.currentUserOrNull() ?: return emptyList()
        return try {
            supabase.from("messages").select {
                filter {
                    or {
                        eq("sender_id", user.id)
                        eq("receiver_id", user.id)
                    }
                }
            }.decodeList<com.collins.todo.data.Models.Message>()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun getDriverLocation(driverId: String): com.collins.todo.data.Models.DriverLocation? {
        return try {
            supabase.from("driver_locations").select {
                filter { eq("driver_id", driverId) }
                order("updated_at", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                limit(1)
            }.decodeSingleOrNull<com.collins.todo.data.Models.DriverLocation>()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun updateDriverLocation(location: com.collins.todo.data.Models.DriverLocation) {
        try {
            supabase.from("driver_locations").upsert(location)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
