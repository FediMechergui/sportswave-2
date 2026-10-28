plugins { id("com.android.application"); id("org.jetbrains.kotlin.android") }
android {
    namespace = "com.sportswave.app"
    compileSdk = 36
    signingConfigs {
        create("release") {
            val keyPath = System.getenv("SPORTSWAVE_KEYSTORE")
            if (!keyPath.isNullOrBlank()) {
                storeFile = file(keyPath)
                storePassword = System.getenv("SPORTSWAVE_STORE_PASSWORD")
                keyAlias = "sportswave"
                keyPassword = System.getenv("SPORTSWAVE_KEY_PASSWORD")
            }
        }
    }
    buildTypes {
        getByName("release") { signingConfig = signingConfigs.getByName("release") }
    }
    defaultConfig {
        applicationId = "com.sportswave.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 15
        versionName = "1.0.14"
        testInstrumentationRunner = "com.sportswave.app.tests.CatalogInstrumentation"
    }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
}
dependencies {
    implementation("com.caverock:androidsvg-aar:1.4")
    implementation("androidx.media3:media3-exoplayer:1.9.0")
    implementation("androidx.media3:media3-exoplayer-hls:1.9.0")
    implementation("androidx.media3:media3-exoplayer-dash:1.9.0")
    implementation("androidx.media3:media3-ui:1.9.0")
    testImplementation("junit:junit:4.13.2")
}
