#!/usr/bin/env kotlin

/**
 * EntityMetadata ↔ backend Entity @SFField 정합 검증 (Spec #764).
 *
 * 책임 (v3 — K2 cache 무효화 + BaseEntity 상속 처리):
 *  - common.kts 의 TARGET_SPECS 를 순회
 *  - 각 target 의 entityRelPath 가 가리키는 backend Entity 파일에서 @SFField 정규식 추출
 *  - 해당 target 의 EntityMetadata 의 FieldMapping.sfFieldName 과 set 비교
 *  - 의도된 차이 (EXPECTED_META_ONLY / EXPECTED_ENTITY_ONLY) 는 무시
 *  - 불일치 시 exit 1, 정합 시 exit 0
 *
 * 사용:
 *   kotlin verify-metadata.main.kts
 *
 * extract-csv.sh 가 추출 전에 본 스크립트를 자동 호출하여 사전 검증.
 */

@file:Repository("https://repo.maven.apache.org/maven2/")
@file:DependsOn("com.opencsv:opencsv:5.9")
@file:DependsOn("org.postgresql:postgresql:42.7.4")
@file:Import("common.kts")

import java.io.File

/**
 * 의도된 차이 (불일치로 카운트하지 않음).
 *
 * METADATA 에만 존재:
 *  - "Id": entity 의 @Column("sfid") 자체. @SFField 어노테이션 없음 — 모든 entity 에 공통.
 *  - "Profile.Name" (User): SOQL relationship 필드, entity 는 profile_type 으로 변환.
 *
 * Entity 에만 존재:
 *  - User.IsDeleted: SF User SObject 에 컬럼 없음 (export 불가).
 */
val EXPECTED_META_ONLY: Set<String> = setOf(
    "Organization:Id", "Account:Id", "Product:Id", "Promotion:Id",
    "Group:Id", "Employee:Id", "User:Id", "Notice:Id",
    "AccountCategoryMaster:Id", "AgreementHistory:Id", "AgreementWord:Id", "AlternativeHoliday:Id",
    "Appointment:Id", "AttendanceLog:Id", "AttendInfo:Id",
    "Claim:Id", "DisplayWorkSchedule:Id", "EmployeeInputCriteriaMaster:Id", "ErpOrder:Id",
    "ErpOrderProduct:Id", "HolidayMaster:Id", "InspectionTheme:Id",
    "MonthlyFemaleEmployeeIntegrationSchedule:Id", "MonthlySalesHistory:Id", "NewProduct:Id", "OrderRequest:Id",
    "OrderRequestProduct:Id", "ProductBarcode:Id", "ProfessionalPromotionTeamHistory:Id", "ProfessionalPromotionTeamMaster:Id",
    "PromotionEmployee:Id", "PushMessage:Id", "PushMessageReceiver:Id",
    "TeamMemberSchedule:Id", "UploadFile:Id",
    "User:Profile.Name"
)

val EXPECTED_ENTITY_ONLY: Set<String> = setOf(
    "User:IsDeleted",
    // SF Account 에 정의는 되어 있으나 운영 org 에서 SOQL queryable 이 아님 (export 실패).
    // entity / DB 컬럼 / common.kts METADATA 는 SF 정합 유지, SOQL 만 우회.
    "Account:IsPriorityRecord"
)

val SF_FIELD_REGEX = Regex("""@SFField\("([^"]+)"\)""")

/**
 * Entity 파일에서 @SFField("...") 의 인자를 모두 추출.
 *
 * BaseEntity 를 상속하는 entity 는 BaseEntity 의 @SFField("CreatedDate") +
 * @SFField("LastModifiedDate") 도 함께 보유한 것으로 간주 (상속).
 * BaseEntity 미상속 entity (예: AgreementWord, UploadFile) 는 자체 @SFField 만 카운트.
 */
val BASE_ENTITY_INHERITED_SF_FIELDS: Set<String> = setOf("CreatedDate", "LastModifiedDate")
val BASE_ENTITY_INHERIT_REGEX = Regex("""\)\s*:\s*BaseEntity\b""")

fun extractSfFieldsFromEntity(file: File): Set<String> {
    val text = file.readText()
    val own = SF_FIELD_REGEX.findAll(text).map { it.groupValues[1] }.toSet()
    val inheritsBaseEntity = BASE_ENTITY_INHERIT_REGEX.containsMatchIn(text)
    return if (inheritsBaseEntity) own + BASE_ENTITY_INHERITED_SF_FIELDS else own
}

/**
 * 본 script 의 cwd 기준 repo root 추정 — scripts/sf-data-migration 의 두 단계 위.
 */
fun resolveRepoRoot(): File {
    // user.dir 은 사용자가 어디서 실행했냐에 따라 다름. script 파일 위치 기준이 더 안정적이지만
    // .main.kts 는 __FILE__ 같은 게 없으므로 cwd 만 활용 + 검증.
    val cwd = File(System.getProperty("user.dir"))
    val candidates = listOf(
        cwd,                                    // repo root 에서 실행
        cwd.parentFile?.parentFile,             // scripts/sf-data-migration 에서 실행
        cwd.parentFile                          // scripts 에서 실행
    ).filterNotNull()
    for (c in candidates) {
        if (File(c, "backend/src/main/kotlin/com/otoki/powersales").exists()) return c
    }
    error("repo root 탐색 실패 — cwd=${cwd.absolutePath}")
}

// =============================================================================
// Main
// =============================================================================

val repoRoot = resolveRepoRoot()
val backendBase = File(repoRoot, "backend/src/main/kotlin/com/otoki/powersales")

println("=".repeat(60))
println("EntityMetadata ↔ @SFField 정합 검증")
println("=".repeat(60))
println("repo root : ${repoRoot.absolutePath}")
println()

var mismatchCount = 0
// K2 cross-file lambda + 컬렉션 wrapper 버그 회피 — sortTargetsByDependency 우회.
// TARGET_DEPENDENCY_ORDER 를 그대로 순회 (의존 순서 보존).
val sortedTargets: List<String> = TARGET_DEPENDENCY_ORDER

for (target in sortedTargets) {
    val spec = TARGET_SPECS[target] ?: error("Unknown target: $target")
    val entityRel = spec.entityRelPath
    if (entityRel == null) {
        println("[$target] ⏭  verify 면제 (entityRelPath=null)")
        continue
    }
    val entityFile = File(backendBase, "$entityRel.kt")
    if (!entityFile.exists()) {
        println("[$target] ❌ Entity 파일 없음: ${entityFile.absolutePath}")
        mismatchCount++
        continue
    }

    // K2 cross-file lambda + cross-file data class 참조 버그 회피 — for loop 으로 직접 수집.
    val meta = spec.meta
    val metaFields: MutableSet<String> = mutableSetOf()
    when (meta) {
        is EntityMetadata -> for (f in meta.fields) metaFields.add(f.sfFieldName)
        is PermissionStagingMetadata -> for (f in meta.fields) metaFields.add(f.sfFieldName)
        else -> {}
    }
    val entityFields = extractSfFieldsFromEntity(entityFile)

    val onlyEntity = entityFields - metaFields
    val onlyMeta = metaFields - entityFields

    val unexpectedEntity = onlyEntity.filterNot { "$target:$it" in EXPECTED_ENTITY_ONLY }
    val unexpectedMeta = onlyMeta.filterNot { "$target:$it" in EXPECTED_META_ONLY }

    if (unexpectedEntity.isEmpty() && unexpectedMeta.isEmpty()) {
        println("[$target] ✅ 정합 (entity=${entityFields.size} / metadata=${metaFields.size})")
    } else {
        mismatchCount++
        println("[$target] ❌ 불일치 (entity=${entityFields.size} / metadata=${metaFields.size})")
        if (unexpectedEntity.isNotEmpty()) {
            println("  Entity 에만 존재 — METADATA 누락:")
            for (f in unexpectedEntity.sorted()) println("    - $f")
        }
        if (unexpectedMeta.isNotEmpty()) {
            println("  METADATA 에만 존재 — Entity 에 없음 (오타 / 잘못된 SF 컬럼):")
            for (f in unexpectedMeta.sorted()) println("    + $f")
        }
    }
}

println()
println("=".repeat(60))
val total = sortedTargets.count { TARGET_SPECS[it]?.entityRelPath != null }
if (mismatchCount == 0) {
    println("✅ 전체 $total 개 target 정합")
} else {
    println("❌ $mismatchCount / $total 개 target 불일치")
    println()
    println("다음 작업:")
    println("  - 'Entity 에만 존재' → common.kts METADATA 에 FieldMapping 추가 + extract-csv.sh SOQL 추가")
    println("  - 'METADATA 에만 존재' → common.kts 의 FieldMapping 제거 (오타) 또는 EXPECTED_META_ONLY 등록 (의도된 예외)")
    System.exit(1)
}
