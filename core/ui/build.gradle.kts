plugins {
    alias(libs.plugins.ledger.android.library)
    alias(libs.plugins.ledger.android.compose)
}

android {
    namespace = "com.dumbopanda.ledger.core.ui"
}

dependencies {
    api(project(":core:designsystem"))
    implementation(project(":core:model"))
}
