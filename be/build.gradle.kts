plugins {
    kotlin("jvm") version "1.9.24" apply false
    kotlin("plugin.spring") version "1.9.24" apply false
    kotlin("plugin.jpa") version "1.9.24" apply false
    id("org.springframework.boot") version "3.3.4" apply false
    id("io.spring.dependency-management") version "1.1.6" apply false
}

allprojects {
    group = "com.auction"
    version = "0.1.0"

    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")

    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        compilerOptions {
            freeCompilerArgs.add("-Xjsr305=strict")
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }

    // Dùng extensions.configure<...>() thay vì DSL "java { ... }" trực tiếp: plugin Kotlin JVM
    // được apply "động" bằng apply(plugin = "...") ở trên (không phải khai báo tĩnh trong khối
    // plugins {}), nên Gradle Kotlin DSL không tự sinh được accessor kiểu "java { }" cho khối
    // subprojects này — gọi thẳng "java { }" sẽ bị nhầm sang PluginDependenciesSpec.java và lỗi
    // biên dịch script ("Expression 'java' cannot be invoked as a function"). configure<T>() tra
    // cứu extension theo type lúc runtime nên hoạt động đúng trong cả 2 cách apply plugin.
    extensions.configure<org.gradle.api.plugins.JavaPluginExtension> {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(17))
        }
    }
}
