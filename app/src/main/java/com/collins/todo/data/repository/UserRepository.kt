package com.collins.todo.data.repository

import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.functions.functions

class UserRepository {
    private val supabase = SupabaseClient.client

    /**
     * Deletes the currently authenticated user's account.
     * This calls a Supabase Edge Function to securely handle deletion
     * using the Service Role Key on the server side.
     */
    suspend fun deleteCurrentUserAccount(): Boolean {
        return try {
            val user = supabase.auth.currentUserOrNull() ?: return false
            
            // Call the 'delete-user' edge function
            // We don't necessarily need to pass the UID if the function 
            // validates the user's JWT from the request header.
            supabase.functions.invoke("delete-user")
            
            // Sign out the user locally after successful deletion
            supabase.auth.signOut()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
