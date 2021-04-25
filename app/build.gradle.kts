import org.jetbrains.kotlin.config.KotlinCompilerVersion

plugins {
    id("com.android.application")
    kotlin("android")
    id("kotlin-android-extensions")
}

android {
    buildFeatures {
        dataBinding = true
        viewBinding = true
    }

    buildToolsVersion("29.0.3")

    compileSdkVersion(29)
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }

    defaultConfig {
        applicationId = "com.androidvip.hebf"
        minSdkVersion(16)
        targetSdkVersion(29)
        versionCode = 183
        versionName = "2.5.1"
        vectorDrawables.useSupportLibrary = true
        resConfigs("en", "ar", "de", "es", "fr", "hi", "in", "it", "pt", "ru", "tr", "zh")
        vectorDrawables.useSupportLibrary = true
        multiDexEnabled = true
    }

    buildTypes {
        getByName("release") {
            isDebuggable = false
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
                "proguard-kt.pro"
            )
        }

        getByName("debug") {
            isDebuggable = true
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
                "proguard-kt.pro"
            )
        }
    }

    packagingOptions {
        exclude("/META-INF/**")
        exclude("/androidsupportmultidexversion.txt")
        exclude("/kotlin/**")
        exclude("/kotlinx/**")
        exclude("/okhttp3/**")
        exclude("/*.txt")
        exclude("/*.bin")
    }
}

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
    implementation(kotlin("stdlib-jdk8", KotlinCompilerVersion.VERSION))

    implementation("androidx.appcompat:appcompat:1.2.0")
    implementation("androidx.core:core-ktx:1.3.2")
    implementation("androidx.constraintlayout:constraintlayout:2.0.4")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.3.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.3.1")
    implementation("androidx.multidex:multidex:2.0.1")
    implementation("androidx.navigation:navigation-fragment-ktx:2.3.5")
    implementation("androidx.navigation:navigation-ui-ktx:2.3.5")
    implementation("androidx.preference:preference-ktx:1.1.1")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    implementation("androidx.vectordrawable:vectordrawable:1.1.0")
    implementation("androidx.work:work-runtime-ktx:2.5.0")

    implementation("com.getkeepsafe.taptargetview:taptargetview:1.11.0")
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")
    implementation("com.github.topjohnwu.libsu:core:3.0.2")

    implementation("com.google.android.material:material:1.3.0-alpha03")
    implementation("com.google.code.gson:gson:2.8.6")
    implementation("de.hdodenhof:circleimageview:3.1.0")
    implementation("io.insert-koin:koin-android:3.0.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.4.1")
}

repositories {
    mavenCentral()
}
