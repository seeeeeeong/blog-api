plugins {
	kotlin("jvm") version "1.9.25"
	kotlin("plugin.spring") version "1.9.25"
	id("org.springframework.boot") version "3.5.7"
	id("io.spring.dependency-management") version "1.1.7"
	id("co.uzzu.dotenv.gradle") version "2.0.0"
	kotlin("plugin.jpa") version "1.9.25"
}

group = "com.blog"
version = "0.0.1-SNAPSHOT"
description = "Demo project for Spring Boot"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(17)
	}
}

repositories {
	mavenCentral()
	maven { url = uri("https://repo.spring.io/milestone") }
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("org.springframework.boot:spring-boot-starter-security")
	implementation("org.springframework.boot:spring-boot-starter-validation")
	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
	implementation("org.jetbrains.kotlin:kotlin-reflect")
	developmentOnly("org.springframework.boot:spring-boot-devtools")

	// JWT
	implementation("io.jsonwebtoken:jjwt-api:0.12.3")
	runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.3")
	runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.3")

	// Redis
	implementation("org.springframework.boot:spring-boot-starter-data-redis")

	// AOP
	implementation("org.springframework.boot:spring-boot-starter-aop")

	// Kotlin
	implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
	implementation("org.jetbrains.kotlin:kotlin-reflect")

	// Cloudinary
	implementation("com.cloudinary:cloudinary-http44:1.36.0")

	// Markdown
	implementation("org.commonmark:commonmark:0.22.0")
	implementation("org.commonmark:commonmark-ext-gfm-tables:0.22.0")

	// HTML Sanitization
	implementation("org.jsoup:jsoup:1.17.2")

	// Springdoc OpenAPI (Swagger)
	implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.7.0")

	// Spring-AI for MCP
	implementation("org.springframework.ai:spring-ai-core:1.0.0-M5")
	implementation("org.springframework.ai:spring-ai-openai:1.0.0-M5")

	// PostgreSQL & pgvector
	runtimeOnly("org.postgresql:postgresql")
	implementation("com.pgvector:pgvector:0.1.4")
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
	testImplementation("org.springframework.security:spring-security-test")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
	compilerOptions {
		freeCompilerArgs.addAll("-Xjsr305=strict")
	}
}

allOpen {
	annotation("jakarta.persistence.Entity")
	annotation("jakarta.persistence.MappedSuperclass")
	annotation("jakarta.persistence.Embeddable")
}

tasks.withType<Test> {
	useJUnitPlatform()
}
