import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.Sync
import org.gradle.api.tasks.bundling.Zip
import org.gradle.api.tasks.CacheableTask
import groovy.json.JsonOutput
import java.util.Properties
import java.text.SimpleDateFormat
import java.util.Date

val releaseMetadata = Properties().apply {
    rootProject.file("release.properties").inputStream().use(::load)
}
val appVersionCode = releaseMetadata.getProperty("VERSION_CODE").toInt()
val appVersionName: String = releaseMetadata.getProperty("VERSION_NAME")
val appMinSdk = releaseMetadata.getProperty("MIN_SDK").toInt()
val appTargetSdk = releaseMetadata.getProperty("TARGET_SDK").toInt()

data class SigningCredentials(
    val storeFilePath: String?,
    val storePassword: String?,
    val keyAlias: String?,
    val keyPassword: String?,
)

abstract class ValidateReleaseSigningTask : DefaultTask() {
    @get:Input
    abstract val propertyPrefix: Property<String>

    @get:Input
    abstract val storeFilePath: Property<String>

    @get:Input
    abstract val storePasswordPresent: Property<Boolean>

    @get:Input
    abstract val keyAliasPresent: Property<Boolean>

    @get:Input
    abstract val keyPasswordPresent: Property<Boolean>

    @get:Internal
    abstract val projectDirectory: DirectoryProperty

    @TaskAction
    fun validate() {
        val prefix = propertyPrefix.get()
        val path = storeFilePath.orNull?.takeUnless { it.isBlank() }
            ?: throw GradleException(
                "Release signing credentials are missing. Configure ${prefix}_STORE_FILE, ${prefix}_STORE_PASSWORD, ${prefix}_KEY_ALIAS, and ${prefix}_KEY_PASSWORD.",
            )

        val storeFile = projectDirectory.file(path).get().asFile
        if (!storeFile.exists()) {
            throw GradleException(
                "Release keystore not found at ${storeFile.absolutePath}. Update ${prefix}_STORE_FILE or place the keystore in that path.",
            )
        }

        val placeholderKeystores = setOf(
            projectDirectory.file("bookverse-release-key.jks").get().asFile,
            projectDirectory.file("key_prod.jks").get().asFile,
        )
        if (storeFile in placeholderKeystores) {
            throw GradleException(
                "The placeholder keystore ${storeFile.name} is not valid for release artifacts. Point ${prefix}_STORE_FILE to your actual keystore.",
            )
        }

        val missingValue = when {
            !storePasswordPresent.get() -> "${prefix}_STORE_PASSWORD"
            !keyAliasPresent.get() -> "${prefix}_KEY_ALIAS"
            !keyPasswordPresent.get() -> "${prefix}_KEY_PASSWORD"
            else -> null
        }
        if (missingValue != null) {
            throw GradleException(
                "Missing $missingValue. Define it in gradle.properties, local.properties, or as an environment variable before building release artifacts.",
            )
        }
    }
}

@CacheableTask
abstract class GenerateReleaseManifestTask : DefaultTask() {
    @get:Input
    abstract val versionName: Property<String>

    @get:Input
    abstract val versionCode: Property<Int>

    @get:Input
    abstract val minSdk: Property<Int>

    @get:Input
    abstract val targetSdk: Property<Int>

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val releaseProperties: RegularFileProperty

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @TaskAction
    fun generate() {
        val manifest = linkedMapOf(
            "versionName" to versionName.get(),
            "versionCode" to versionCode.get(),
            "minSdk" to minSdk.get(),
            "targetSdk" to targetSdk.get(),
            "distributions" to linkedMapOf(
                "community" to linkedMapOf(
                    "applicationId" to "com.bookverse.contacts.community",
                    "firebase" to false,
                ),
                "play" to linkedMapOf(
                    "applicationId" to "com.bookverse.contacts",
                    "firebase" to false,
                ),
            ),
            "coreCapabilities" to listOf(
                "dialer",
                "contacts",
                "favorites",
                "call_history",
                "limited_mode",
            ),
            "optionalTools" to listOf(
                "notes",
                "expenses",
                "passwords",
                "qr",
                "voice",
                "holidays",
                "paint",
                "games",
                "pomodoro",
                "wellness",
            ),
        )
        val file = outputFile.get().asFile
        file.parentFile.mkdirs()
        file.writeText(JsonOutput.prettyPrint(JsonOutput.toJson(manifest)) + "\n")
    }
}

fun Project.resolveSigningCredentials(prefix: String): SigningCredentials {
    val fallbackProperties = Properties().apply {
        listOf(
            "gradle.properties.private",
            "keystore.properties",
            "local.properties"
        ).forEach { fileName ->
            val file = rootProject.file(fileName)
            if (file.exists()) {
                file.inputStream().use(::load)
            }
        }
    }

    fun lookup(key: String): String? =
        System.getenv(key)?.takeUnless { it.isBlank() }
            ?: fallbackProperties.getProperty(key)?.takeUnless { it.isBlank() }
            ?: (findProperty(key) as String?)?.takeUnless { it.isBlank() }

    return SigningCredentials(
        storeFilePath = lookup("${prefix}_STORE_FILE"),
        storePassword = lookup("${prefix}_STORE_PASSWORD"),
        keyAlias = lookup("${prefix}_KEY_ALIAS"),
        keyPassword = lookup("${prefix}_KEY_PASSWORD"),
    )
}

fun SigningCredentials.isComplete(): Boolean =
    storeFilePath != null && storePassword != null && keyAlias != null && keyPassword != null

val releaseSigningCredentials = resolveSigningCredentials("RELEASE")
val communitySigningCredentials = resolveSigningCredentials("COMMUNITY_RELEASE")
val buildDate: String = SimpleDateFormat("ddMMyyyy").format(Date())

plugins {
    alias(libs.plugins.funphone.android.application.compose)
    alias(libs.plugins.androidx.baselineprofile)
    id("kotlin-parcelize")
}

baselineProfile {
    mergeIntoMain = true
    saveInSrc = true
    automaticGenerationDuringBuild = false
    dexLayoutOptimization = true
}

androidComponents {
    onVariants(selector().withFlavor("distribution" to "community").withBuildType("release")) { variant ->
        variant.outputs.forEach { output -> output.versionName.set(appVersionName) }
    }
    finalizeDsl { extension ->
        listOf("nonMinifiedRelease", "benchmarkRelease").forEach { buildTypeName ->
            extension.buildTypes.findByName(buildTypeName)?.signingConfig =
                extension.signingConfigs.getByName("debug")
            extension.sourceSets.findByName(buildTypeName)?.apply {
                kotlin.srcDir("src/benchmark/java")
                assets.srcDir("src/benchmark/assets")
                manifest.srcFile("src/benchmark/AndroidManifest.xml")
            }
        }
    }
}

android {
    namespace = "com.bookverse.contacts"

    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        applicationId = "com.bookverse.contacts"
        minSdk = appMinSdk
        targetSdk = appTargetSdk
        versionCode = appVersionCode
        versionName = appVersionName
    }

    signingConfigs {
        create("release") {
            val storeFilePath = releaseSigningCredentials.storeFilePath
            val storePass = releaseSigningCredentials.storePassword
            val alias = releaseSigningCredentials.keyAlias
            val keyPass = releaseSigningCredentials.keyPassword

            if (storeFilePath != null && storePass != null && alias != null && keyPass != null) {
                val resolvedStoreFile = rootProject.file(storeFilePath)
                if (resolvedStoreFile.exists()) {
                    storeFile = resolvedStoreFile
                    storePassword = storePass
                    keyAlias = alias
                    keyPassword = keyPass
                }
            }
        }
        create("community") {
            if (communitySigningCredentials.isComplete()) {
                storeFile = rootProject.file(communitySigningCredentials.storeFilePath!!)
                storePassword = communitySigningCredentials.storePassword
                keyAlias = communitySigningCredentials.keyAlias
                keyPassword = communitySigningCredentials.keyPassword
            }
        }
    }

    flavorDimensions += "distribution"
    productFlavors {
        create("community") {
            dimension = "distribution"
            applicationIdSuffix = ".community"
            versionNameSuffix = "-community"
            buildConfigField("String", "DISTRIBUTION", "\"community\"")
            buildConfigField("boolean", "GITHUB_SPONSORS_ENABLED", "false")
            if (communitySigningCredentials.isComplete()) {
                signingConfig = signingConfigs.getByName("community")
            }
        }
        create("play") {
            dimension = "distribution"
            buildConfigField("String", "DISTRIBUTION", "\"play\"")
            buildConfigField("boolean", "GITHUB_SPONSORS_ENABLED", "false")
            signingConfig = signingConfigs.getByName("release")
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = ".$buildDate Dev"
            isMinifyEnabled = false
            buildConfigField("boolean", "DEBUG", "true")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        release {
            versionNameSuffix = ".$buildDate Prod"
            isMinifyEnabled = true
            isShrinkResources = true
            buildConfigField("boolean", "DEBUG", "false")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            ndk {
                debugSymbolLevel = "FULL"
            }
        }
        create("benchmark") {
            initWith(getByName("release"))
            applicationIdSuffix = ".benchmark"
            versionNameSuffix = ".$buildDate Benchmark"
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += listOf("release")
            isDebuggable = false
        }
    }

    dependenciesInfo {
        includeInApk = false
        includeInBundle = true
    }

    lint {
        checkDependencies = true
    }
}

val guardedReleaseTasks = setOf(
    "assemblePlayRelease",
    "bundlePlayRelease",
    "preparePlayStoreArtifacts",
    "packagePlayReleaseNativeSymbols",
)

val validatePlayReleaseSigning by tasks.registering(ValidateReleaseSigningTask::class) {
    group = "distribution"
    description = "Validates the credentials and keystore used for Play release artifacts."
    propertyPrefix.set("RELEASE")
    storeFilePath.set(releaseSigningCredentials.storeFilePath.orEmpty())
    storePasswordPresent.set(!releaseSigningCredentials.storePassword.isNullOrBlank())
    keyAliasPresent.set(!releaseSigningCredentials.keyAlias.isNullOrBlank())
    keyPasswordPresent.set(!releaseSigningCredentials.keyPassword.isNullOrBlank())
    projectDirectory.set(layout.projectDirectory)
}

tasks.matching { it.name in guardedReleaseTasks }.configureEach {
    dependsOn(validatePlayReleaseSigning)
}

val validateCommunityReleaseSigning by tasks.registering(ValidateReleaseSigningTask::class) {
    group = "distribution"
    description = "Validates the credentials and keystore used for signed community release APKs."
    propertyPrefix.set("COMMUNITY_RELEASE")
    storeFilePath.set(communitySigningCredentials.storeFilePath.orEmpty())
    storePasswordPresent.set(!communitySigningCredentials.storePassword.isNullOrBlank())
    keyAliasPresent.set(!communitySigningCredentials.keyAlias.isNullOrBlank())
    keyPasswordPresent.set(!communitySigningCredentials.keyPassword.isNullOrBlank())
    projectDirectory.set(layout.projectDirectory)
}

configurations.all {
    resolutionStrategy {
        force("com.google.crypto.tink:tink-android:1.23.0")
        eachDependency {
            if (requested.group == "com.google.crypto.tink" && requested.name == "tink") {
                useTarget("com.google.crypto.tink:tink-android:1.23.0")
            }
        }
    }
}

configurations.named("implementation") {
    exclude(group = "com.intellij", module = "annotations")
}

configurations.matching { it.name.contains("AndroidTest", ignoreCase = true) }.configureEach {
    resolutionStrategy.force(
        "org.jetbrains.kotlinx:kotlinx-serialization-core:1.8.1",
        "org.jetbrains.kotlinx:kotlinx-serialization-core-jvm:1.8.1",
        "org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.1",
        "org.jetbrains.kotlinx:kotlinx-serialization-json-jvm:1.8.1",
    )
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:database"))
    implementation(project(":core:datastore"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:security"))
    implementation(project(":core:ui"))
    implementation(project(":feature:contacts"))
    implementation(project(":feature:dialer"))
    implementation(project(":feature:expenses"))
    implementation(project(":feature:games"))
    implementation(project(":feature:notes"))
    implementation(project(":feature:qr"))
    implementation(project(":feature:settings"))
    implementation(project(":feature:tools"))
    implementation(project(":feature:vault"))
    implementation(project(":feature:widgets"))

    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.haze)
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.coil.compose)
    implementation(libs.androidx.material.icons.extended)

    implementation(libs.google.zxing.core)
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.compose)
    implementation(libs.androidx.camera.view)
    add("playImplementation", libs.google.mlkit.barcode.scanning)
    implementation(libs.google.guava)
    implementation(libs.google.gson)
    add("playImplementation", libs.google.play.review)
    add("playImplementation", libs.google.play.review.ktx)
    implementation(libs.coil.svg)
    implementation(libs.icons.lucide.android)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.richeditor.compose)
    implementation(libs.contacts.android)
    implementation(libs.yalantis.ucrop)
    implementation(libs.androidx.transition)
    implementation(libs.konfetti.compose)

    implementation(libs.androidx.compose.foundation)

    implementation(libs.google.crypto.tink.android)
    implementation(libs.androidx.security.crypto) {
        exclude(group = "com.google.crypto.tink", module = "tink-android")
        exclude(group = "com.google.crypto.tink", module = "tink")
    }
    implementation(libs.androidx.biometric)
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.glance.material3)
    implementation(libs.androidx.profileinstaller)
    baselineProfile(project(":baselineprofile"))

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.room.testing)
    androidTestImplementation(libs.androidx.uiautomator)
}

tasks.register("getApk") {
    group = "build"
    description = "Assembles the Play release APK for easy access from the IDE."
    dependsOn("assemblePlayRelease")
}

tasks.register<GenerateReleaseManifestTask>("generateReleaseManifest") {
    group = "distribution"
    description =
        "Generates version and capability metadata for documentation and the landing site."
    versionName.set(appVersionName)
    versionCode.set(appVersionCode)
    minSdk.set(appMinSdk)
    targetSdk.set(appTargetSdk)
    releaseProperties.set(rootProject.layout.projectDirectory.file("release.properties"))
    outputFile.set(layout.buildDirectory.file("reports/release/release-manifest.json"))
}

val packagePlayReleaseNativeSymbols by tasks.registering(Zip::class) {
    group = "distribution"
    description =
        "Packages Play release native libraries as a symbol archive for Play Console uploads."
    val variant = "playRelease"
    dependsOn("mergePlayReleaseNativeLibs")
    from(layout.buildDirectory.dir("intermediates/merged_native_libs/$variant/mergePlayReleaseNativeLibs/out/lib"))
    include("**/*.so")
    destinationDirectory.set(layout.buildDirectory.dir("outputs/native-debug-symbols/$variant"))
    archiveFileName.set("native-debug-symbols.zip")
}

val preparePlayStoreArtifacts by tasks.registering(Sync::class) {
    group = "distribution"
    description = "Collects the release App Bundle, mapping file, and native debug symbols for Play Console uploads."
    dependsOn("bundlePlayRelease", packagePlayReleaseNativeSymbols)
    into(layout.buildDirectory.dir("playStoreArtifacts/playRelease"))
    from(layout.buildDirectory.dir("outputs/bundle/playRelease")) {
        include("*.aab")
    }
    from(layout.buildDirectory.dir("outputs/mapping/playRelease")) {
        include("mapping.txt")
    }
    from(packagePlayReleaseNativeSymbols)
}
