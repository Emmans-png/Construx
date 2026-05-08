package com.collins.todo.data.repository

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.functions.Functions
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.storage.Storage

object SupabaseClient {
    val client = createSupabaseClient(
        supabaseUrl = "https://aoxvkohxjfowuctnntam.supabase.co",
        supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImFveHZrb2h4amZvd3VjdG5udGFtIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzcwMjQ0NDAsImV4cCI6MjA5MjYwMDQ0MH0.gJTGtmW4NoZdE4LBlph55Aa64G5XXsaRp-EU072W5xg",
    ) {
        install(Auth)
        install(Functions)
        install(Postgrest)
        install(Realtime)
        install(Storage)
    }
}
