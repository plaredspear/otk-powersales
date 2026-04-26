plugins {
	kotlin("jvm") version "2.2.21"
	kotlin("plugin.spring") version "2.2.21"
	id("org.springframework.boot") version "4.0.5"
	id("io.spring.dependency-management") version "1.1.7"
}

group = "com.otoki.powersales"
version = "0.0.1-SNAPSHOT"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(24)
	}
}

repositories {
	mavenCentral()
}

extra["springCloudAwsVersion"] = "4.0.0"

dependencyManagement {
	imports {
		mavenBom("io.awspring.cloud:spring-cloud-aws-dependencies:${property("springCloudAwsVersion")}")
	}
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-webmvc")
	implementation("org.springframework.boot:spring-boot-starter-actuator")
	implementation("org.springframework.boot:spring-boot-starter-data-redis")
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("io.awspring.cloud:spring-cloud-aws-starter-secrets-manager")
	implementation("io.awspring.cloud:spring-cloud-aws-starter-s3")

	// OpenAPI (Swagger UI) — Boot 4 GA 릴리즈 (3.0.3, parent: spring-boot-starter-parent:4.0.5)
	implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:3.0.3")

	runtimeOnly("org.postgresql:postgresql")
	implementation("org.jetbrains.kotlin:kotlin-reflect")
	implementation("tools.jackson.module:jackson-module-kotlin")
	testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
	testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
	compilerOptions {
		freeCompilerArgs.addAll("-Xjsr305=strict", "-Xannotation-default-target=param-property")
	}
}

tasks.withType<Test> {
	useJUnitPlatform()
	systemProperty("spring.profiles.active", "test")
}

tasks.named<Jar>("jar") {
	enabled = false
}

tasks.test {
	// OpenAPI spec 생성 테스트는 전용 task로만 실행
	exclude("**/OpenApiSpecGeneratorTest*")
}

tasks.register<Test>("generateOpenApiDocs") {
	group = "documentation"
	description = "OpenAPI spec JSON 파일 생성 (backend/openapi.json). 실 DB(local Postgres 또는 dev RDS) 가 활성 상태여야 컨텍스트가 기동된다."
	filter {
		includeTestsMatching("com.otoki.powersales.OpenApiSpecGeneratorTest")
	}
}
