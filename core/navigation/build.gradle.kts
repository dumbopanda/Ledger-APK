plugins {
    alias(libs.plugins.ledger.android.library)
    alias(libs.plugins.ledger.android.compose)
}

android {
    namespace = "com.dumbopanda.ledger.core.navigation"
}

dependencies {
    implementation(libs.androidx.navigation.compose)
}
