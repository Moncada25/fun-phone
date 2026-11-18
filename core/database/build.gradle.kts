plugins {
    alias(libs.plugins.funphone.android.library.asProvider())
    alias(libs.plugins.funphone.android.room)
    alias(libs.plugins.kotlin.compose)
    id("kotlin-parcelize")
}

android {
    namespace = "com.bookverse.contacts.core.database"
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.runtime)
    api(libs.contacts.android)
    implementation(libs.google.gson)

    testImplementation(libs.junit)
}
