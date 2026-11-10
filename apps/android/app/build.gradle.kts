plugins {
  id("com.android.application")
  id("org.jetbrains.kotlin.android")
  id("org.jetbrains.kotlin.plugin.compose")
  id("com.google.devtools.ksp")
}

android {
  namespace = "zw.zimlms"
  compileSdk = 35

  defaultConfig {
    applicationId = "zw.zimlms"
    minSdk = 26
    targetSdk = 35
    versionCode = 11
    versionName = "0.11.0-expanded-curriculum-preview"
    manifestPlaceholders["applicationLabel"] = "ZimSchool LMS"
    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

    // The key is publishable; never compile any service-role or secret key into an APK.
    val supabaseUrl = System.getenv("ZIMLMS_SUPABASE_URL") ?: ""
    val publishableKey = System.getenv("ZIMLMS_SUPABASE_PUBLISHABLE_KEY") ?: ""
    buildConfigField("String", "SUPABASE_URL", "\"${supabaseUrl}\"")
    buildConfigField("String", "SUPABASE_PUBLISHABLE_KEY", "\"${publishableKey}\"")
  }

  // Embed the very same original curriculum JSON used by the web LMS.
  // Relative to apps/android/app: repository_root/packages/curriculum.
  sourceSets {
    getByName("main").assets.srcDir("../../../packages/curriculum")
  }

  buildFeatures { compose = true; buildConfig = true }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }
  kotlinOptions { jvmTarget = "17" }
  buildTypes {
    debug {
      // Side-by-side test install protects earlier offline queues when CI's
      // ephemeral debug signing certificate differs between workflow runs.
      applicationIdSuffix = ".preview.curriculum11"
      versionNameSuffix = "-test"
      manifestPlaceholders["applicationLabel"] = "ZimSchool Curriculum 63 Preview"
    }
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
  implementation("androidx.room:room-runtime:2.8.5")
  implementation("androidx.room:room-ktx:2.8.5")
  ksp("androidx.room:room-compiler:2.8.5")
  implementation("androidx.work:work-runtime-ktx:2.10.2")
  testImplementation("junit:junit:4.13.2")
  testImplementation("org.json:json:20240303")
  androidTestImplementation("androidx.test:runner:1.6.2")
  androidTestImplementation("androidx.test.ext:junit:1.2.1")
  androidTestImplementation("androidx.test:core:1.6.1")
  androidTestImplementation(platform("androidx.compose:compose-bom:2024.09.00"))
  androidTestImplementation("androidx.compose.ui:ui-test-junit4")
  debugImplementation("androidx.compose.ui:ui-test-manifest")
}

ksp {
  arg("room.schemaLocation", "$projectDir/schemas")
}
