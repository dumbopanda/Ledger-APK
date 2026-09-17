plugins {
    alias(libs.plugins.ledger.android.library)
}

android {
    namespace = "com.dumbopanda.ledger.core.testing"
}

dependencies {
    api(project(":core:model"))
    implementation(libs.kotlinx.coroutines.test)
    implementation(libs.junit)
}
