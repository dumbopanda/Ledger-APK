plugins {
    alias(libs.plugins.ledger.android.library)
    alias(libs.plugins.ledger.android.room)
    alias(libs.plugins.ledger.android.hilt)
}

android {
    namespace = "com.dumbopanda.ledger.core.database"
}

dependencies {
    implementation(project(":core:model"))
}
