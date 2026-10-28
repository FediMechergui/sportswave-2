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
        versionCode = 16
        versionName = "1.0.15"
        testInstrumentationRunner = "com.sportswave.app.tests.CatalogInstrumentation"
    }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
}
dependencies {
    implementation("org.jsoup:jsoup:1.18.3")
    implementation("androidx.webkit:webkit:1.17.1")
    testImplementation("org.json:json:20240303")
    implementation("com.caverock:androidsvg-aar:1.4")
    implementation("androidx.media3:media3-exoplayer:1.9.0")
    implementation("androidx.media3:media3-exoplayer-hls:1.9.0")
    implementation("androidx.media3:media3-exoplayer-dash:1.9.0")
    implementation("androidx.media3:media3-ui:1.9.0")
    testImplementation("junit:junit:4.13.2")
}
