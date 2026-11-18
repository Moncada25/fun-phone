import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.CompileOptions
import com.android.build.api.dsl.LibraryExtension
import com.android.build.api.dsl.Lint
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension

internal const val FUN_PHONE_COMPILE_SDK = 37
internal const val FUN_PHONE_MIN_SDK = 29
internal const val FUN_PHONE_TEST_RUNNER = "androidx.test.runner.AndroidJUnitRunner"

internal val Project.libs: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

internal fun Project.configureAndroidLibrary(extension: LibraryExtension) {
    extension.apply {
        compileSdk = FUN_PHONE_COMPILE_SDK
        defaultConfig {
            minSdk = FUN_PHONE_MIN_SDK
            testInstrumentationRunner = FUN_PHONE_TEST_RUNNER
        }
        compileOptions { configureJava() }
        testOptions {
            unitTests.isReturnDefaultValues = true
        }
        lint { configureFunPhoneLint() }
    }
    configureKotlinJvm()
}

internal fun Project.configureAndroidApplication(extension: ApplicationExtension) {
    extension.apply {
        compileSdk = FUN_PHONE_COMPILE_SDK
        defaultConfig {
            minSdk = FUN_PHONE_MIN_SDK
            testInstrumentationRunner = FUN_PHONE_TEST_RUNNER
        }
        compileOptions { configureJava() }
        testOptions {
            unitTests.isReturnDefaultValues = true
        }
        lint { configureFunPhoneLint() }
    }
    configureKotlinJvm()
}

internal fun Project.addComposeDependencies(bomConfiguration: String) {
    dependencies {
        val bom = platform(libs.findLibrary("androidx-compose-bom").get())
        add(bomConfiguration, bom)
        add("androidTestImplementation", bom)
        add("implementation", libs.findLibrary("androidx-ui-tooling-preview").get())
        add("debugImplementation", libs.findLibrary("androidx-ui-tooling").get())
        add("debugImplementation", libs.findLibrary("androidx-ui-test-manifest").get())
        add("androidTestImplementation", libs.findLibrary("androidx-ui-test-junit4").get())
    }
}

private fun CompileOptions.configureJava() {
    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
}

private fun Lint.configureFunPhoneLint() {
    abortOnError = true
    disable += "AndroidGradlePluginVersion"
    disable += "GradleDependency"
    disable += "NewerVersionAvailable"
    disable += "ObsoleteSdkInt"
}

private fun Project.configureKotlinJvm() {
    extensions.configure<KotlinAndroidProjectExtension> {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_25)
        }
    }
}
