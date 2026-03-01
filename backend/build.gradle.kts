plugins {
	kotlin("jvm") version "1.9.25"
	kotlin("plugin.spring") version "1.9.25"
	kotlin("kapt") version "1.9.25"
	id("org.springframework.boot") version "3.5.0"
	id("io.spring.dependency-management") version "1.1.7"
	kotlin("plugin.jpa") version "1.9.25"
}

group = "com.otoki"
version = "0.0.1-SNAPSHOT"
description = "Demo project for Spring Boot"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(17)
	}
}

repositories {
	mavenCentral()
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("org.flywaydb:flyway-core")
	implementation("org.flywaydb:flyway-database-postgresql")
	implementation("org.springframework.boot:spring-boot-starter-data-redis")
	implementation("org.springframework.boot:spring-boot-starter-security")
	implementation("org.springframework.boot:spring-boot-starter-validation")
	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("org.springframework.boot:spring-boot-starter-thymeleaf")
	implementation("org.thymeleaf.extras:thymeleaf-extras-springsecurity6")
	implementation("nz.net.ultraq.thymeleaf:thymeleaf-layout-dialect")
	implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
	implementation("org.jetbrains.kotlin:kotlin-reflect")

	// OpenAPI (Swagger UI)
	implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.4")

	// QueryDSL
	implementation("com.querydsl:querydsl-jpa:5.1.0:jakarta")
	kapt("com.querydsl:querydsl-apt:5.1.0:jakarta")

	// JWT
	implementation("io.jsonwebtoken:jjwt-api:0.12.6")
	runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.6")
	runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.6")

	developmentOnly("org.springframework.boot:spring-boot-devtools")
	runtimeOnly("com.h2database:h2")
	runtimeOnly("org.postgresql:postgresql")
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
	testImplementation("org.springframework.security:spring-security-test")
	testImplementation("org.mockito.kotlin:mockito-kotlin:5.2.1")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
	compilerOptions {
		freeCompilerArgs.addAll("-Xjsr305=strict")
	}
}

kapt {
	correctErrorTypes = true
}

allOpen {
	annotation("jakarta.persistence.Entity")
	annotation("jakarta.persistence.MappedSuperclass")
	annotation("jakarta.persistence.Embeddable")
}

tasks.matching { it.name == "kaptTestKotlin" }.configureEach {
	enabled = false
}

// 주석 처리된 Entity를 참조하는 테스트 파일 컴파일 제외 (Phase 2 Entity 활성화 후 복구)
sourceSets {
	test {
		kotlin {
			exclude(
				// "**/AttendanceControllerTest.kt", // re-enabled: test rewritten for V1 schema
				"**/ClaimControllerTest.kt",
				"**/ClientOrderControllerTest.kt",
				"**/EventControllerTest.kt",
				"**/InspectionControllerTest.kt",
				// "**/NoticeControllerTest.kt", // re-enabled: test rewritten for legacy table
				"**/OrderControllerTest.kt",
				"**/OrderQueryControllerTest.kt",
				"**/DailySalesCreateRequestTest.kt",
				"**/DailySalesCreateResponseTest.kt",
				"**/DailySalesTest.kt",
				"**/DailySalesExceptionsTest.kt",
				"**/AttendanceRepositoryTest.kt",
				"**/ClaimRepositoryTest.kt",
				"**/DailySalesRepositoryTest.kt",
				"**/EducationPostRepositoryTest.kt",
				"**/ExpiryProductRepositoryTest.kt",
				"**/InspectionRepositoryTest.kt",
				"**/NoticePostRepositoryTest.kt",
				"**/OrderRepositoryTest.kt",
				"**/SuggestionPhotoRepositoryTest.kt",
				// "**/AttendanceServiceTest.kt", // re-enabled: test rewritten for V1 schema
				"**/ClaimServiceTest.kt",
				"**/ClientOrderServiceTest.kt",
				"**/DailySalesServiceTest.kt",
				"**/EducationServiceTest.kt",
				"**/EventServiceTest.kt",
				// "**/HomeServiceTest.kt", // re-enabled: test rewritten for V1 schema
				"**/InspectionServiceTest.kt",
				"**/MyScheduleServiceTest.kt",
				// "**/NoticeServiceTest.kt", // re-enabled: test rewritten for legacy table
				"**/OrderQueryServiceTest.kt",
				"**/OrderServiceTest.kt",
				"**/OrderSubmitServiceTest.kt",
				"**/SuggestionServiceTest.kt"
			)
		}
	}
}

tasks.withType<Test> {
	useJUnitPlatform()
}

tasks.test {
	// Redis 미연결로 인한 @SpringBootTest 컨텍스트 로드 실패 (기존 이슈)
	exclude("**/OtokiInternalApplicationTests*")
	exclude("**/HealthControllerTest*")

	// OpenAPI spec 생성 테스트는 전용 task로만 실행
	exclude("**/OpenApiSpecGeneratorTest*")
}

tasks.register<Test>("generateOpenApiDocs") {
	group = "documentation"
	description = "OpenAPI spec JSON 파일 생성 (backend/openapi.json)"
	filter {
		includeTestsMatching("com.otoki.internal.OpenApiSpecGeneratorTest")
	}
}
