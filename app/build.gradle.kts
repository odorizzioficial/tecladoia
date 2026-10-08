import java.io.File
import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

/*
 * Dados da assinatura ficam em keystore.properties, na raiz do projeto, que o
 * .gitignore nao versiona. Sem esse arquivo o projeto continua compilando: so
 * o release sai sem assinatura propria.
 */
val keystoreProperties = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}
// So vale se o .jks realmente existir no caminho indicado: assim o modelo com
// valores de exemplo nao quebra o build de quem ainda nao configurou a chave.
val hasSigningConfig = keystoreProperties.getProperty("storeFile")
    ?.let { File(it).exists() } == true

// Nome dos arquivos gerados: TecladoIA-debug.apk / TecladoIA-release.apk
base {
    archivesName.set("TecladoIA")
}

android {
    namespace = "com.odorizzioficial.tecladoia"
    // 36: o motor offline e o llama.cpp exigem compilar contra o Android 16.
    // O app continua mirando o 35 (targetSdk) e instalando a partir do 26.
    compileSdk = 36

    defaultConfig {
        applicationId = "com.odorizzioficial.tecladoia"
        minSdk = 26
        targetSdk = 35
        versionCode = 17
        versionName = "1.4.2"
        vectorDrawables.useSupportLibrary = true
    }

    signingConfigs {
        create("release") {
            if (hasSigningConfig) {
                storeFile = file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            // Sem keystore.properties, cai no comportamento padrao do Gradle.
            if (hasSigningConfig) {
                signingConfig = signingConfigs.getByName("release")
            }
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Os motores de IA offline trazem codigo nativo por arquitetura. No
            // release ficam so as de celular (64 e 32 bits); o debug mantem todas
            // para o emulador de PC (x86_64) continuar instalando.
            ndk {
                abiFilters += listOf("arm64-v8a", "armeabi-v7a")
            }
        }
        debug {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    packaging {
        // Bibliotecas nativas compactadas dentro do APK: o download fica bem menor
        // (elas sao extraidas na instalacao).
        jniLibs {
            useLegacyPackaging = true
        }
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

// kotlinOptions {} foi removido no Kotlin 2.2+: o alvo da JVM agora fica aqui.
kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.savedstate.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.datastore.preferences)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    debugImplementation(libs.androidx.ui.tooling)

    implementation(libs.okhttp)
    implementation(libs.kotlinx.serialization.json)

    // Motor de IA que roda no aparelho (modo IA offline), da Google.
    implementation(libs.litertlm.android)

    // Motor llama.cpp (arquivos .gguf) no aparelho, MIT.
    implementation(libs.llamatik)
}
