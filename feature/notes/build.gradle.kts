plugins {
    alias(libs.plugins.funphone.android.library.compose)
}

android {
    namespace = "com.bookverse.contacts.feature.notes"
}

dependencies {
    implementation(project(":core:ui"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.icons.lucide.android)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.richeditor.compose)
    implementation(libs.coil.compose)
    implementation(libs.haze)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.work.runtime.ktx)
}
