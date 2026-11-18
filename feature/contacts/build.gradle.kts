plugins {
    alias(libs.plugins.funphone.android.library.compose)
    id("kotlin-parcelize")
}

android {
    namespace = "com.bookverse.contacts.feature.contacts"
}

dependencies {
    implementation(project(":core:ui"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.appcompat)
    implementation(libs.icons.lucide.android)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.haze)
    implementation(libs.coil.compose)
    implementation(libs.google.gson)
    implementation(libs.google.zxing.core)
    implementation(libs.yalantis.ucrop)
    implementation(libs.androidx.work.runtime.ktx)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
