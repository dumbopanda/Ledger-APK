plugins {
    alias(libs.plugins.ledger.android.library)
    alias(libs.plugins.ledger.android.room)
    alias(libs.plugins.ledger.android.hilt)
}

android {
    namespace = "com.dumbopanda.ledger.core.database"

    // MigrationTestHelper reads prior-version schemas from the test assets --
    // which Robolectric only merges in for local unit tests when this is on.
    sourceSets.getByName("test") {
        assets.srcDirs("$projectDir/schemas")
    }
    testOptions.unitTests.isIncludeAndroidResources = true
}

dependencies {
    implementation(project(":core:model"))
    implementation(libs.androidx.room.paging)
    implementation(libs.androidx.paging.common)

    testImplementation(libs.junit)
    testImplementation(libs.androidx.paging.testing)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.ext.junit)
    testImplementation(libs.androidx.room.testing)
}
