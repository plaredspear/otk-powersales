plugins {
	kotlin("jvm") version "2.2.21"
	kotlin("plugin.spring") version "2.2.21"
	id("com.google.devtools.ksp") version "2.2.21-2.0.5"
	id("org.springframework.boot") version "4.0.5"
	id("io.spring.dependency-management") version "1.1.7"
	kotlin("plugin.jpa") version "2.2.21"
}

group = "com.otoki.powersales"
version = "0.0.1-SNAPSHOT"
description = "Demo project for Spring Boot"

extra["springCloudAwsVersion"] = "4.0.0"

springBoot {
	mainClass.set("com.otoki.powersales.OtokiPowerSalesApplicationKt")
}

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(24)
	}
	sourceCompatibility = JavaVersion.VERSION_24
	targetCompatibility = JavaVersion.VERSION_24
}

repositories {
	mavenCentral()
}

dependencyManagement {
	imports {
		mavenBom("io.awspring.cloud:spring-cloud-aws-dependencies:${property("springCloudAwsVersion")}")
	}
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-actuator")
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	// Spring Boot 4 에서 Flyway autoconfig 가 spring-boot-autoconfigure 에서
	// 분리되어 별도 모듈(spring-boot-flyway)로 제공된다. 누락 시 자동 마이그레이션이
	// 동작하지 않고 Hibernate 가 곧바로 schema validation 으로 진행해 실패한다.
	implementation("org.springframework.boot:spring-boot-flyway")
	implementation("org.flywaydb:flyway-core")
	implementation("org.flywaydb:flyway-database-postgresql")
	implementation("org.springframework.boot:spring-boot-starter-data-redis")
	implementation("org.springframework.boot:spring-boot-starter-security")
	implementation("org.springframework.boot:spring-boot-starter-validation")
	implementation("org.springframework.boot:spring-boot-starter-webmvc")
	implementation("tools.jackson.module:jackson-module-kotlin")
	implementation("org.jetbrains.kotlin:kotlin-reflect")

	// Spring Cloud AWS (Secrets Manager + S3)
	implementation("io.awspring.cloud:spring-cloud-aws-starter-secrets-manager")
	implementation("io.awspring.cloud:spring-cloud-aws-starter-s3")

	// OpenAPI (Swagger UI) — Boot 4 GA 릴리즈 (3.0.3, parent: spring-boot-starter-parent:4.0.5)
	implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:3.0.3")

	// QueryDSL (OpenFeign fork + KSP)
	implementation("io.github.openfeign.querydsl:querydsl-jpa:7.1")
	ksp("io.github.openfeign.querydsl:querydsl-ksp-codegen:7.1")

	// Apache POI (Excel)
	implementation("org.apache.poi:poi-ooxml:5.3.0")

	// JWT
	implementation("io.jsonwebtoken:jjwt-api:0.12.6")
	runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.6")
	runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.6")

	developmentOnly("org.springframework.boot:spring-boot-devtools")
	runtimeOnly("com.h2database:h2")
	runtimeOnly("org.postgresql:postgresql")
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
	testImplementation("org.springframework.boot:spring-boot-starter-data-jpa-test")
	testImplementation("org.springframework.boot:spring-boot-resttestclient")
	testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
	testImplementation("org.springframework.security:spring-security-test")
	testImplementation("org.mockito.kotlin:mockito-kotlin:5.2.1")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
	compilerOptions {
		freeCompilerArgs.addAll("-Xjsr305=strict", "-Xannotation-default-target=param-property")
	}
}

allOpen {
	annotation("jakarta.persistence.Entity")
	annotation("jakarta.persistence.MappedSuperclass")
	annotation("jakarta.persistence.Embeddable")
}

// 주석 처리된 Entity를 참조하는 테스트 파일 컴파일 제외 (Phase 2 Entity 활성화 후 복구)
sourceSets {
	test {
		kotlin {
			exclude(
				// Spring Boot 4 + H2 240 + Hibernate 7 조합에서 @DataJpaTest 컨텍스트가 조기 종료되는 이슈로 일시 제외 (#538-P1).
				// "The database has been closed" 발생. 후속 스펙에서 재활성화 예정.
				"**/NoticeRepositoryTest.kt",
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
				// "**/EducationServiceTest.kt", // re-enabled: test rewritten for admin CRUD
				"**/EventServiceTest.kt",
				// "**/HomeServiceTest.kt", // re-enabled: test rewritten for V1 schema
				"**/InspectionServiceTest.kt",
				// "**/MyScheduleServiceTest.kt", // re-enabled: test rewritten for substitute holiday
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
	systemProperty("spring.profiles.active", "test")
}

tasks.named<Jar>("jar") {
	enabled = false
}

tasks.test {
	// Redis 미연결로 인한 @SpringBootTest 컨텍스트 로드 실패 (기존 이슈)
	exclude("**/OtokiPowerSalesApplicationTests*")

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

tasks.register<JavaExec>("migrateHeroku") {
	group = "migration"
	description = "Heroku DB → Dev DB 데이터 마이그레이션 (예: ./gradlew migrateHeroku --args='account')"
	mainClass.set("com.otoki.powersales.common.migration.HerokuMigrationTool")
	classpath = sourceSets.main.get().runtimeClasspath
}
