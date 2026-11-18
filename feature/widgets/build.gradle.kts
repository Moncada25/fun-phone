plugins {
    alias(libs.plugins.funphone.android.library.compose)
}

android {
    namespace = "com.bookverse.contacts.feature.widgets"
}

dependencies {
    implementation(project(":core:ui"))
    implementation(project(":feature:contacts"))
    implementation(project(":feature:expenses"))
    implementation(project(":feature:notes"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.glance.material3)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
