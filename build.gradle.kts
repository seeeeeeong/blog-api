import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.jvm.tasks.Jar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.springframework.boot.gradle.tasks.bundling.BootJar

plugins {
    kotlin("jvm") version "2.3.0"
    kotlin("plugin.spring") version "2.3.0"
    kotlin("plugin.jpa") version "2.3.0"
    id("org.springframework.boot") version "3.5.9"
    id("io.spring.dependency-management") version "1.1.7"
    id("co.uzzu.dotenv.gradle") version "2.0.0"
}

group = "com.blog"
version = "0.0.1-SNAPSHOT"

repositories {
    mavenCentral()
}

extensions.configure<JavaPluginExtension> {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

allOpen {
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.MappedSuperclass")
    annotation("jakarta.persistence.Embeddable")
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.apache.commons:commons-pool2")
    implementation("org.springframework.boot:spring-boot-starter-aop")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.flywaydb:flyway-core")
    implementation("com.github.ben-manes.caffeine:caffeine")
    developmentOnly("org.springframework.boot:spring-boot-devtools")

    runtimeOnly("org.flywaydb:flyway-database-postgresql")
    runtimeOnly("org.postgresql:postgresql")
    runtimeOnly("io.micrometer:micrometer-registry-prometheus")

    // Resilience4j
    implementation("io.github.resilience4j:resilience4j-spring-boot3:2.2.0")

    // JWT
    implementation("io.jsonwebtoken:jjwt-api:0.12.3")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.3")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.3")

    // AWS S3
    implementation(platform("software.amazon.awssdk:bom:2.25.16"))
    implementation("software.amazon.awssdk:s3")

    // Markdown + Sanitization
    implementation("org.commonmark:commonmark:0.22.0")
    implementation("org.commonmark:commonmark-ext-gfm-tables:0.22.0")
    implementation("org.jsoup:jsoup:1.17.2")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("org.springframework.security:spring-security-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

tasks.named<BootJar>("bootJar") {
    archiveBaseName.set("blog-api")
}

tasks.named<Jar>("jar") {
    enabled = false
}
