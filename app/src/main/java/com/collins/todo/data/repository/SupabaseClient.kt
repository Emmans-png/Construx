package com.collins.todo.data.repository

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest

object SupabaseClient {
    val client = createSupabaseClient(
        supabaseUrl = "https://aoxvkohxjfowuctnntam.supabase.co",
        supabaseKey = "sb_publishable_vH_aqHDuNt0_VuG_HFx88w_GtXKoivB"
    ) {
        install(Auth)
        install(Postgrest)
    }
}
