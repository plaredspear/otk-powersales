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
 *   kotlin verify-metadata.main.kts --describe-dir=<sf-object-meta>/prod   # SF describe 대조 추가
 *
 * extract-csv.sh 가 추출 전에 본 스크립트를 자동 호출하여 사전 검증.
 *
 * SF describe 대조 (선택, --describe-dir 지정 시):
 *  - 거울 검증(entity ↔ common.kts)이 못 잡는 사각지대를 SF describe 산출물
 *    (`<dir>/<SObject>.md` — `sf sobject describe` 의 markdown 표, 컬럼: API Name / Type / … / Calc)
 *    와 대조해 보고:
 *      · STALE   — common.kts/entity 가 매핑한 SF field 가 describe 에 없음 (오타 / SF rename·삭제)
 *      · CALC    — calculated(✓) 필드를 매핑 (SOQL 적재 불가)
 *      · MISSING — SF 에만 있는 비-calculated·비-시스템 필드 수 (INFO 카운트)
 *  - STALE/CALC 는 종료 코드 미변경 (describe 산출물 stale 가능성 — 리포트로만 노출).
 *  - describe 산출물 경로는 호출자가 인자로 전달 (소스에 docs 경로 비고정).
 */

@file:Repository("https://repo.maven.apache.org/maven2/")
@file:DependsOn("com.opencsv:opencsv:5.9")
@file:DependsOn("org.postgresql:postgresql:42.7.4")
@file:Import("common.kts")

import java.io.File

// --describe-dir=<path> (선택) — 지정 + 디렉토리 존재 시에만 SF describe 대조 수행.
val describeDir: File? = args.firstOrNull { it.startsWith("--describe-dir=") }
    ?.removePrefix("--describe-dir=")
    ?.let { File(it) }
    ?.takeIf { it.isDirectory }

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
    "MonthlyFemaleEmployeeIntegrationSchedule:Id", "NewProduct:Id", "OrderRequest:Id",
    "OrderRequestProduct:Id", "ProductBarcode:Id", "ProfessionalPromotionTeamHistory:Id", "ProfessionalPromotionTeamMaster:Id",
    "PromotionEmployee:Id", "PromotionProduct:Id", "PushMessage:Id", "PushMessageReceiver:Id",
    "TeamMemberSchedule:Id", "UploadFile:Id", "Suggestion:Id", "SiteActivity:Id",
    "UserRole:Id", "Profile:Id",
    "User:Profile.Name"
)

val EXPECTED_ENTITY_ONLY: Set<String> = setOf(
    "User:IsDeleted",
    // SF describe 에는 정의되어 있으나 운영 org 에서 SOQL 거부 (export 실패).
    // entity / DB 컬럼 / common.kts METADATA 는 SF 정합 유지, SOQL 만 우회.
    "Account:IsPriorityRecord",
    "Group:Description"
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

// =============================================================================
// SF describe 대조 (--describe-dir 지정 시)
// =============================================================================

// 의도적 제외 시스템 필드 (Group A) — entity/common.kts 미매핑이 정상 → MISSING 집계 제외.
val SF_SYSTEM_FIELDS: Set<String> = setOf(
    "SystemModstamp", "LastActivityDate", "LastViewedDate", "LastReferencedDate",
)

// describe md 표의 데이터 행: `| # | API Name | Type | Length | Prec | Nullable | Unique | Calc | Label |`
// `|` split → [2]=API Name, [8]=Calc (✓ = calculated). 헤더/구분선 행은 [1] 이 숫자가 아니라 제외.
val sfMdRowRegex = Regex("""^\|\s*\d+\s*\|""")

if (describeDir != null) {
    println()
    println("=".repeat(60))
    println("SF describe 대조 (--describe-dir=${describeDir.path})")
    println("=".repeat(60))

    var sfDumpFound = 0
    var sfDumpMissing = 0
    var sfWarnCount = 0
    var totalMissing = 0

    for (target in sortedTargets) {
        val spec = TARGET_SPECS[target] ?: continue
        val meta = spec.meta
        val sObjectName: String = when (meta) {
            is EntityMetadata -> meta.sObjectName
            else -> continue // PermissionStagingMetadata 등 XML/staging 출처 — describe 대조 대상 아님
        }

        val dumpFile = File(describeDir, "$sObjectName.md")
        if (!dumpFile.exists()) {
            sfDumpMissing++
            continue
        }

        // describe md 표 파싱 — field name set + calculated(✓) name set.
        val sfAllFields: MutableSet<String> = mutableSetOf()
        val sfCalculated: MutableSet<String> = mutableSetOf()
        for (line in dumpFile.readLines()) {
            if (!sfMdRowRegex.containsMatchIn(line)) continue
            val cols = line.split("|")
            if (cols.size < 9) continue
            val apiName = cols[2].trim()
            if (apiName.isEmpty()) continue
            sfAllFields.add(apiName)
            if (cols[8].trim() == "✓") sfCalculated.add(apiName)
        }
        if (sfAllFields.isEmpty()) {
            sfDumpMissing++
            continue
        }
        sfDumpFound++

        // 매핑 집합 = entity @SFField ∪ common.kts METADATA.
        val entityRel = spec.entityRelPath
        val entityFile = entityRel?.let { File(backendBase, "$it.kt") }
        val entityFields = if (entityFile != null && entityFile.exists()) {
            extractSfFieldsFromEntity(entityFile)
        } else {
            emptySet()
        }
        val metaFields: MutableSet<String> = mutableSetOf()
        when (meta) {
            is EntityMetadata -> for (f in meta.fields) metaFields.add(f.sfFieldName)
            else -> {}
        }
        val mapped = entityFields + metaFields
        // 관계 traversal (예: "Profile.Name") 은 describe 에 평면 필드로 없으므로 STALE 판정 제외.
        val mappedFlat: MutableSet<String> = mutableSetOf()
        for (f in mapped) if (!f.contains(".")) mappedFlat.add(f)

        val stale: MutableList<String> = mutableListOf()
        for (f in mappedFlat) if (f !in sfAllFields) stale.add(f)
        val calculatedMapped: MutableList<String> = mutableListOf()
        for (f in mapped) if (f in sfCalculated) calculatedMapped.add(f)
        val missing: MutableList<String> = mutableListOf()
        for (f in sfAllFields) {
            if (f !in mapped && f !in sfCalculated && f !in SF_SYSTEM_FIELDS) missing.add(f)
        }
        totalMissing += missing.size

        val hasWarn = stale.isNotEmpty() || calculatedMapped.isNotEmpty()
        if (hasWarn) {
            sfWarnCount++
            println()
            println("⚠ $target ($sObjectName)")
            if (stale.isNotEmpty()) {
                println("  STALE — 매핑했으나 SF describe 에 없음: ${stale.sorted()}")
            }
            if (calculatedMapped.isNotEmpty()) {
                println("  CALC  — calculated 필드를 매핑 (SOQL 적재 불가): ${calculatedMapped.sorted()}")
            }
        }
        if (missing.isNotEmpty()) {
            val sample = missing.sorted().take(8)
            val more = if (missing.size > sample.size) " …" else ""
            println("  INFO  — $target: SF 에만 있는 비매핑 필드 ${missing.size}개 (예: $sample$more)")
        }
    }

    println()
    println("-".repeat(60))
    println(
        "SF describe 대조 요약: dump $sfDumpFound 종 검사 / $sfDumpMissing 종 dump 없음 / " +
            "WARNING $sfWarnCount 종 / 비매핑 SF 필드 총 $totalMissing 개",
    )
    if (sfWarnCount > 0) {
        println("  ⚠ STALE/CALC 는 종료 코드 미변경 (describe dump stale 가능성). 수기 확인 권장.")
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
