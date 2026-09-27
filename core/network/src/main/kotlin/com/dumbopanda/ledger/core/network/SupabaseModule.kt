package com.dumbopanda.ledger.core.network

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.functions.Functions
import io.github.jan.supabase.postgrest.Postgrest
import javax.inject.Singleton

// D3: direct from client, RLS-enforced. No API server, no service-role key
// on device — this is a plain postgrest/auth/functions client against the
// anon (publishable) key. Functions is installed for the email-otp Edge
// Function (C13) -- it's a public, verify_jwt=false endpoint (no session
// exists yet when someone is trying to sign in), so this client calls it
// the same signed-out way it calls everything else.
@Module
@InstallIn(SingletonComponent::class)
object SupabaseModule {
    @Provides
    @Singleton
    fun provideSupabaseClient(): SupabaseClient =
        createSupabaseClient(
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseKey = BuildConfig.SUPABASE_PUBLISHABLE_KEY,
        ) {
            install(Postgrest)
            install(Auth)
            install(Functions)
        }
}
