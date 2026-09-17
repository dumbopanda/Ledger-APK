plugins {
    alias(libs.plugins.ledger.android.library)
    alias(libs.plugins.ledger.android.compose)
}

android {
    namespace = "com.dumbopanda.ledger.core.designsystem"
}

dependencies {
    implementation(libs.androidx.compose.ui.text.google.fonts)
    implementation(libs.androidx.compose.material.icons.core)
}
