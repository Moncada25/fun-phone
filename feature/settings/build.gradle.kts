plugins {
    alias(libs.plugins.funphone.android.library.compose)
}

android {
    namespace = "com.bookverse.contacts.feature.settings"
}

dependencies {
    implementation(project(":core:ui"))
    implementation(project(":feature:contacts"))
    implementation(project(":feature:expenses"))
    implementation(project(":feature:games"))
    implementation(project(":feature:notes"))
    implementation(project(":feature:qr"))
    implementation(project(":feature:tools"))
    implementation(project(":feature:vault"))
    implementation(project(":feature:widgets"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.appcompat)
    implementation(libs.icons.lucide.android)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.haze)
    implementation(libs.coil.compose)
    implementation(libs.google.gson)
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.konfetti.compose)
    implementation(libs.google.crypto.tink.android)
    implementation(libs.androidx.biometric)

    androidTestImplementation(libs.androidx.espresso.core)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}

android {
    defaultConfig {
        missingDimensionStrategy("distribution", "community", "play")
    }
}
