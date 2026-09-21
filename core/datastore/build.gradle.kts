import com.google.protobuf.gradle.id

plugins {
    alias(libs.plugins.ledger.android.library)
    alias(libs.plugins.ledger.android.hilt)
    alias(libs.plugins.protobuf)
}

android {
    namespace = "com.dumbopanda.ledger.core.datastore"
}

protobuf {
    protoc {
        artifact = "${libs.protobuf.protoc.get()}"
    }
    generateProtoTasks {
        all().forEach { task ->
            task.builtins {
                create("java") {
                    option("lite")
                }
                create("kotlin") {
                    option("lite")
                }
            }
        }
    }
}

dependencies {
    implementation(project(":core:model"))

    // api, not implementation: LedgerDataStoreFactory exposes DataStore<SyncCursors>
    // / DataStore<UserPrefs> to callers (core:data's Puller), so they need these
    // types -- and SyncCursor/UserPrefs' own protobuf-lite supertypes -- on their
    // own compile classpath, not just core:datastore's.
    api(libs.androidx.datastore.core)
    api(libs.protobuf.kotlin.lite)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.security.crypto)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.ext.junit)
}
