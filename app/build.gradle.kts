plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
val keystorePath: String = System.getenv("KEYSTORE_FILE").orEmpty()
val hasReleaseKey: Boolean = keystorePath.isNotEmpty() && file(keystorePath).exists() &&
    !System.getenv("KEYSTORE_PASSWORD").isNullOrEmpty() &&
    !System.getenv("KEY_ALIAS").isNullOrEmpty() &&
    !System.getenv("KEY_PASSWORD").isNullOrEmpty()

android {
    namespace = "app.iptv.tv"
    compileSdk = 35
    defaultConfig {
        // Отдельный идентификатор: RuStore-версия может стоять на устройстве рядом с версией из GitHub,
        // и у каждой свои настройки. После первой публикации этот идентификатор менять нельзя.
        applicationId = "app.iptv.tv.rustore"
        minSdk = 21
        targetSdk = 35
        // Для RuStore номер сборки фиксированный и растёт вручную перед каждой публикацией
        // (28 — отклонена из-за REQUEST_INSTALL_PACKAGES, 29 — без разрешений на установку и на запись)
        versionCode = 33
        versionName = "3.2.2"
        // Версия для RuStore: автообновление не используется, приложение APK не скачивает и не устанавливает
        buildConfigField("boolean", "UPDATES_ENABLED", "false")
    }
    buildFeatures { buildConfig = true }
    // Подпись релиза: ключ и пароли берутся из переменных окружения (в GitHub Actions — из Secrets).
    // Без них (локальная сборка) APK подписывается обычным отладочным ключом Android.
    signingConfigs {
        if (hasReleaseKey) {
            create("release") {
                storeFile = file(keystorePath)
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS")
                keyPassword = System.getenv("KEY_PASSWORD")
            }
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName(if (hasReleaseKey) "release" else "debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    lint {
        abortOnError = false
        // Системные проверки единственного окна-экрана не относятся к приложению для ТВ (собственный интерфейс, один Activity)
        disable += listOf("SetTextI18n", "UnsafeOptInUsageError")
    }
}
dependencies {
    // Media3 1.9+ требует minSdk 23 (Android 6): на 1.8.x остаются поддерживаемыми приставки с Android 5 (minSdk 21)
    implementation("androidx.core:core:1.15.0")
    implementation("androidx.media3:media3-exoplayer:1.8.0")
    implementation("androidx.media3:media3-exoplayer-hls:1.8.0")
    implementation("androidx.media3:media3-exoplayer-rtsp:1.8.0")
    implementation("androidx.media3:media3-ui:1.8.0")
    testImplementation("junit:junit:4.13.2")
    // kxml2 — тот же разборщик XML, что android.util.Xml подставляет на устройстве: разбор XMLTV проверяется в обычном JVM-тесте
    testImplementation("net.sf.kxml:kxml2:2.3.0")
}
