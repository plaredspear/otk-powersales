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
	// Spring Boot 4 에서 Cache autoconfig 가 spring-boot-autoconfigure 에서 분리되어
	// 별도 모듈(spring-boot-cache)로 제공된다. starter-cache 없이 @EnableCaching 만
	// 켜면 CacheManager Bean 자동 등록 / NoOp fallback / cache abstraction 인프라가
	// 모두 비활성이라 "CacheManager bean not found" 로 부팅 실패한다.
	implementation("org.springframework.boot:spring-boot-starter-cache")
	implementation("org.springframework.boot:spring-boot-starter-security")
	implementation("org.springframework.boot:spring-boot-starter-validation")
	implementation("org.springframework.boot:spring-boot-starter-webmvc")
	implementation("org.springframework:spring-aspects")
	implementation("org.aspectj:aspectjweaver")
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

	// ShedLock — 다중 인스턴스 환경에서 스케줄 잡 중복 실행 방지 (Spec #545)
	implementation("net.javacrumbs.shedlock:shedlock-spring:5.16.0")
	implementation("net.javacrumbs.shedlock:shedlock-provider-jdbc-template:5.16.0")

	developmentOnly("org.springframework.boot:spring-boot-devtools")
	// H2 2.4.x 의 column-level CHECK ((col IN (...))) 평가 regression 회피 (#4302/#4311, Spec #573).
	// PR #4311 fix 가 포함된 정식 릴리스가 나오면 Spring Boot 관리 버전으로 복귀한다.
	runtimeOnly("com.h2database:h2:2.3.232")
	runtimeOnly("org.postgresql:postgresql")
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
	testImplementation("org.springframework.boot:spring-boot-starter-data-jpa-test")
	testImplementation("org.springframework.boot:spring-boot-resttestclient")
	// Spring Boot 4 에서 RestTemplateBuilder 가 spring-boot-restclient 모듈로 분리되었고,
	// spring-boot-resttestclient 의 TestRestTemplateTestAutoConfiguration 이 이를 참조한다.
	// 누락 시 NoClassDefFoundError: org/springframework/boot/restclient/RestTemplateBuilder.
	testImplementation("org.springframework.boot:spring-boot-restclient")
	testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
	testImplementation("org.springframework.security:spring-security-test")
//	testImplementation("org.mockito.kotlin:mockito-kotlin:5.2.1")
	// MockK — mockito-kotlin 의 inline reified matcher 가 K2 컴파일러를 폭발시키는
	// 문제를 회피하기 위해 도메인별 점진적 전환 진행 중. 마이그레이션 현황은
	// .claude/guides/mockk-migration-guide.md 참조.
	testImplementation("io.mockk:mockk:1.13.13")
	// springmockk 5.0.x — Spring Framework 7 (Boot 4) 지원. @MockitoBean 대체용
	// @MockkBean 어노테이션 제공. Spring 의 MockitoBean 인프라 위에 MockK 를 얹는다.
	testImplementation("com.ninja-squad:springmockk:5.0.1")
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

tasks.withType<Test> {
	useJUnitPlatform()
	systemProperty("spring.profiles.active", "test")
	systemProperty("user.timezone", "Asia/Seoul")
	maxHeapSize = "3g"
	jvmArgs = listOf("-XX:MaxMetaspaceSize=512m")
	forkEvery = 100
}

tasks.named<Jar>("jar") {
	enabled = false
}

tasks.test {
	// Redis 미연결로 인한 @SpringBootTest 컨텍스트 로드 실패 (기존 이슈)
	exclude("**/OtokiPowerSalesApplicationTests*")

	// OpenAPI spec 생성 테스트는 전용 task로만 실행
	exclude("**/OpenApiSpecGeneratorTest*")

	// (#538-P1) H2 2.4.x 의 column-level CHECK ((col IN (...))) 평가 regression (#4302).
	// H2 dependency 를 2.3.232 로 고정하여 회피했으므로 본 exclude 도 함께 해제 시도한다.
}

tasks.register<Test>("generateOpenApiDocs") {
	group = "documentation"
	description = "OpenAPI spec JSON 파일 생성 (backend/openapi.json + group 별 3종). 외부 DB/Redis/AWS 불필요 — test 프로파일의 H2 in-memory + create-drop 로 컨텍스트가 기동된다. 단 셸 환경에 SPRING_DATASOURCE_* 등이 export 돼 있으면 H2 설정을 override 해서 부팅이 실패할 수 있으니, 자동화에서는 env 를 stripping 해서 호출할 것."
	testClassesDirs = sourceSets.test.get().output.classesDirs
	classpath = sourceSets.test.get().runtimeClasspath
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
