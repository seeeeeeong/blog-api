import org.gradle.jvm.tasks.Jar

plugins {
    id("org.springframework.boot")
}

dependencies {
    implementation(project(":core:core-enum"))
    implementation(project(":storage:db-core"))

    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework.boot:spring-boot-starter-aop")
    developmentOnly("org.springframework.boot:spring-boot-devtools")

    // JWT
    implementation("io.jsonwebtoken:jjwt-api:0.12.3")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.3")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.3")

    // Cloudinary
    implementation("com.cloudinary:cloudinary-http44:1.36.0")

    // Markdown + Sanitization
    implementation("org.commonmark:commonmark:0.22.0")
    implementation("org.commonmark:commonmark-ext-gfm-tables:0.22.0")
    implementation("org.jsoup:jsoup:1.17.2")

    // Springdoc OpenAPI (Swagger)
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.7.0")

    // Spring-AI for MCP
    implementation("org.springframework.ai:spring-ai-core:1.0.0-M5")
    implementation("org.springframework.ai:spring-ai-openai:1.0.0-M5")

    testImplementation("org.springframework.security:spring-security-test")
}

tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    enabled = true
    archiveBaseName.set("blog-api")
}

tasks.named<Jar>("jar") {
    enabled = false
}
