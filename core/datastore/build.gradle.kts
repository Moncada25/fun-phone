plugins {
    alias(libs.plugins.funphone.android.library.asProvider())
}

android {
    namespace = "com.bookverse.contacts.core.datastore"
}

dependencies {
    api(libs.androidx.datastore.preferences)

    testImplementation(project(":core:designsystem"))
    testImplementation(libs.junit)
}
