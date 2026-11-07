plugins {
  id("com.android.application")
  id("org.jetbrains.kotlin.android")
  id("org.jetbrains.kotlin.plugin.compose")
}

android {
  namespace = "zw.zimlms"
  compileSdk = 35

  defaultConfig {
    applicationId = "zw.zimlms"
    minSdk = 26
    targetSdk = 35
    versionCode = 1
    versionName = "0.1.0-p0"
    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

    // The key is publishable; never compile any service-role or secret key into an APK.
    val supabaseUrl = System.getenv("ZIMLMS_SUPABASE_URL") ?: ""
    val publishableKey = System.getenv("ZIMLMS_SUPABASE_PUBLISHABLE_KEY") ?: ""
    buildConfigField("String", "SUPABASE_URL", "\"${supabaseUrl}\"")
    buildConfigField("String", "SUPABASE_PUBLISHABLE_KEY", "\"${publishableKey}\"")
  }

  buildFeatures { compose = true; buildConfig = true }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }
  kotlinOptions { jvmTarget = "17" }
  buildTypes {
    release {
      isMinifyEnabled = true
      isShrinkResources = true
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
    }
  }
  bundle { language { enableSplit = false } }
}

dependencies {
  implementation(platform("androidx.compose:compose-bom:2024.09.00"))
  implementation("androidx.compose.material3:material3")
  implementation("androidx.activity:activity-compose:1.9.2")
  implementation("androidx.core:core-ktx:1.13.1")
  implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.5")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
  implementation("com.squareup.okhttp3:okhttp:4.12.0")
  testImplementation("junit:junit:4.13.2")
}
