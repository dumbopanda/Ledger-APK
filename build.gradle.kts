plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.room) apply false
    alias(libs.plugins.sentry.android.gradle) apply false
}

// Ktor's OkHttp engine pulls okhttp-android 5.x, which gates on compileSdk 37+
// (the same AGP-9 wall core:designsystem's Compose BOM hit at A10). Forced
// back to the last 4.x release project-wide — a per-module force in
// core:network doesn't reach :app or other modules that depend on it, since
// each module resolves its own configurations independently.
allprojects {
    configurations.all {
        resolutionStrategy {
            force("com.squareup.okhttp3:okhttp:4.12.0")
            // Supabase-kt/Ktor ship classes compiled with newer Kotlin metadata
            // (2.4.x) than Hilt 2.58's bundled kotlin-metadata-jvm reader (capped
            // at 2.3.0) can parse during its classpath aggregation step. This
            // artifact versions in lockstep with the Kotlin compiler itself
            // (unlike KSP), so forcing it up is a much smaller change than
            // downgrading Supabase-kt/Ktor to find an older-metadata release.
            force("org.jetbrains.kotlin:kotlin-metadata-jvm:2.4.0")
        }
    }
}
