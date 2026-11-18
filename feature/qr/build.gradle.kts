plugins {
    alias(libs.plugins.funphone.android.library.compose)
}

android {
    namespace = "com.bookverse.contacts.feature.qr"

    flavorDimensions += "distribution"
    productFlavors {
        create("community") {
            dimension = "distribution"
        }
        create("play") {
            dimension = "distribution"
        }
    }
}

dependencies {
    implementation(project(":core:ui"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.icons.lucide.android)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.haze)
    implementation(libs.google.zxing.core)
    implementation(libs.google.gson)
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.compose)
    implementation(libs.androidx.camera.view)
    implementation(libs.google.guava)
    add("playImplementation", libs.google.mlkit.barcode.scanning)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
