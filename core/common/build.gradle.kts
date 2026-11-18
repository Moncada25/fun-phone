plugins {
    alias(libs.plugins.funphone.android.library.asProvider())
}

android {
    namespace = "com.bookverse.contacts.core.common"
    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.google.gson)

    testImplementation(libs.junit)
}
