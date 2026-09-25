import java.util.Properties

plugins {
    alias(libs.plugins.ledger.android.application)
    alias(libs.plugins.ledger.android.compose)
    alias(libs.plugins.ledger.android.hilt)
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

android {
    namespace = "com.dumbopanda.ledger"

    defaultConfig {
        applicationId = "com.dumbopanda.ledger"
        versionCode = 1
        versionName = "0.1.0"
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

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:model"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:ui"))
    implementation(project(":core:navigation"))
    implementation(project(":core:data"))

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
}
