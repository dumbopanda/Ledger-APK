import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("com.android.application")
                apply("org.jetbrains.kotlin.android")
            }

            extensions.configure<ApplicationExtension> {
                configureKotlinAndroid(this)
                defaultConfig.targetSdk = 36

                buildTypes {
                    // §11.2: "R8 full mode on; mapping.txt uploaded to Sentry on every
                    // release or crash reports are unreadable." Full mode is AGP's own
                    // default once minification is on (no gradle.properties override in
                    // this project turns it off) -- shrinkResources rides along with it
                    // since an unshrunk resource table is the other half of "the APK
                    // still ships everything debug builds do."
                    release {
                        isMinifyEnabled = true
                        isShrinkResources = true
                        proguardFiles(
                            getDefaultProguardFile("proguard-android-optimize.txt"),
                            "proguard-rules.pro",
                        )
                    }
                }
            }
        }
    }
}
