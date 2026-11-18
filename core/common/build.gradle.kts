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
    implementation(libs.androidx.work.runtime.ktx)

    testImplementation(libs.junit)
}
