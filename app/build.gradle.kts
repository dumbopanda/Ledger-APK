import java.util.Properties

plugins {
    alias(libs.plugins.ledger.android.application)
    alias(libs.plugins.ledger.android.compose)
    alias(libs.plugins.ledger.android.hilt)
    alias(libs.plugins.sentry.android.gradle)
}

// §11.2's upload key. Never generated or held by anything but the owner --
// this file just reads WHERE it lives and reads the passwords needed to use
// it, all from keystore.properties (gitignored, machine-local, same pattern
// local.properties already uses for the Supabase URL/key and Google client
// ID). Absent entirely on a machine that hasn't set this up yet -- release
// builds there are simply unsigned, same as every build before this one.
val keystoreProperties = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) file.inputStream().use(::load)
}
val hasSigningConfig = keystoreProperties.containsKey("storeFile")

// C12's crash-reporting DSN. Same footing as the Supabase publishable key
// (core/network/build.gradle.kts) -- safe to embed client-side, kept out of
// source only so prod/local can swap without a code change.
val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use(::load)
}

android {
    namespace = "com.dumbopanda.ledger"

    defaultConfig {
        applicationId = "com.dumbopanda.ledger"
        versionCode = 5
        versionName = "0.3.0"
        buildConfigField("String", "SENTRY_DSN", "\"${localProperties.getProperty("sentry.dsn", "")}\"")
    }

    buildFeatures {
        buildConfig = true
    }

    if (hasSigningConfig) {
        signingConfigs {
            create("release") {
                storeFile = file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
        buildTypes {
            release {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }
}

// mapping.txt upload (readable crash stack traces) needs a Sentry auth
// token -- a real secret, unlike the DSN above, so it lives in its own
// gitignored sentry.properties (same keystore.properties pattern) rather
// than local.properties. Absent entirely on a machine that hasn't set this
// up yet -- release builds there are simply unsymbolicated, not broken.
val hasSentryAuthToken = rootProject.file("sentry.properties").exists()
sentry {
    autoUploadProguardMapping = hasSentryAuthToken
    includeProguardMapping = hasSentryAuthToken
    tracingInstrumentation.enabled = false
    autoInstallation.enabled = false
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:model"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:ui"))
    implementation(project(":core:navigation"))
    implementation(project(":core:data"))
    implementation(project(":core:domain"))

    implementation(project(":feature:onboarding"))
    implementation(project(":feature:auth"))
    implementation(project(":feature:entry"))
    implementation(project(":feature:history"))
    implementation(project(":feature:accounts"))
    implementation(project(":feature:settings"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.hilt.work)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.androidx.biometric)
    implementation(libs.sentry.android)
}
