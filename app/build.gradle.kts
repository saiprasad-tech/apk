plugins {
    id("com.android.application")
}

android {
    namespace = "com.pixhawk.gcslab"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.pixhawk.gcslab"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"
        ndk { abiFilters += listOf("armeabi-v7a","arm64-v8a","x86_64") }
        externalNativeBuild { cmake { arguments += listOf("-DANDROID_STL=c++_shared") } }
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            isShrinkResources = false
            ndk { debugSymbolLevel = "FULL" }
            packaging { jniLibs.keepDebugSymbols += listOf("**/*.so") }
        }
        debug {
        }
    }
    externalNativeBuild { cmake { path = file("src/main/cpp/CMakeLists.txt"); version = "3.28.0" } }
    bundle { language.enableSplit = false; density.enableSplit = false; abi.enableSplit = false }
}

dependencies {
}
