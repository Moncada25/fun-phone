plugins {
    alias(libs.plugins.funphone.android.library.asProvider())
}

android {
    namespace = "com.bookverse.contacts.core.security"
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:datastore"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.google.crypto.tink.android)
    implementation(libs.androidx.security.crypto) {
        exclude(group = "com.google.crypto.tink", module = "tink-android")
        exclude(group = "com.google.crypto.tink", module = "tink")
    }

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
