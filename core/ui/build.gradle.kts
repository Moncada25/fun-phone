plugins {
    alias(libs.plugins.funphone.android.library.compose)
}

android {
    namespace = "com.bookverse.contacts.core.ui"
}

dependencies {
    api(project(":core:designsystem"))
}
