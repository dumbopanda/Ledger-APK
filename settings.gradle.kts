pluginManagement {
    includeBuild("build-logic")
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "ledger"

include(":app")

include(
    ":core:model",
    ":core:common",
    ":core:database",
    ":core:datastore",
    ":core:network",
    ":core:data",
    ":core:domain",
    ":core:designsystem",
    ":core:ui",
    ":core:navigation",
    ":core:testing",
)

include(
    ":feature:onboarding",
    ":feature:auth",
    ":feature:entry",
    ":feature:history",
    ":feature:accounts",
    ":feature:settings",
)
