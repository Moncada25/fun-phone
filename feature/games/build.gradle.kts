plugins {
    alias(libs.plugins.funphone.android.library.compose)
}

android {
    namespace = "com.bookverse.contacts.feature.games"
}

dependencies {
    implementation(project(":core:ui"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.icons.lucide.android)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.konfetti.compose)
    implementation(libs.haze)
    implementation(libs.androidx.activity.compose)
    implementation(libs.yalantis.ucrop)
    implementation(libs.androidx.appcompat)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
