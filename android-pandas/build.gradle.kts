plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "cn.ac.oac.libs.andas"
    compileSdk = 36

    defaultConfig {
        minSdk = 26
        
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
        
        // CMake配置
        externalNativeBuild {
            cmake {
                cppFlags.addAll(listOf("-std=c++17", "-O3"))
                arguments.addAll(listOf("-DANDROID_STL=c++_shared"))
            }
        }
        
        // NDK配置
        ndk {
            abiFilters.addAll(listOf("arm64-v8a", "x86_64"))
        }
    }
    
    // 版本配置（在 defaultConfig 外部）
    version = "0.0.1.rc3"

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            externalNativeBuild {
                cmake {
                    cppFlags.addAll(listOf("-std=c++17", "-O3"))
                    arguments.addAll(listOf("-DCMAKE_BUILD_TYPE=Release"))
                }
            }
        }
        debug {
            externalNativeBuild {
                cmake {
                    cppFlags.addAll(listOf("-std=c++17", "-g"))
                    arguments.addAll(listOf("-DCMAKE_BUILD_TYPE=Debug"))
                }
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    
    kotlinOptions {
        jvmTarget = "11"
    }
    
    // CMake路径配置
    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }
    
    // 构建功能
    buildFeatures {
        buildConfig = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    testImplementation(libs.junit)
    testImplementation(kotlin("test"))
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
