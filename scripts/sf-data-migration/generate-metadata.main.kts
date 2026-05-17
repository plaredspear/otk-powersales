#!/usr/bin/env kotlin

/**
 * @SFObject Entity → EntityMetadata + SOQL 자동 생성기 (Spec #764).
 *
 * 책임:
 *  - backend Entity 파일을 파싱하여 @SFObject + @SFField + @Column + Kotlin type 추출
 *  - common.kts 에 추가할 EntityMetadata Kotlin 코드 + extract-csv.sh 에 추가할 SOQL + TARGET_SPECS 항목 생성
 *  - stdout 으로 출력 — 사용자가 검토 후 수동 복사
 *
 * 사용:
 *   kotlin generate-metadata.main.kts <entity-relative-path>
 *
 * 예:
 *   kotlin generate-metadata.main.kts claim/entity/Claim
 *   kotlin generate-metadata.main.kts schedule/entity/Appointment
 *
 * 출력: 3 섹션
 *   1. common.kts 추가 코드 (val XXX_METADATA = EntityMetadata(...))
 *   2. extract-csv.sh 추가 코드 (XXX_SOQL=$(cat <<'EOF' ... EOF))
 *   3. TARGET_SPECS / SUPPORTED_TARGETS / TARGET_DEPENDENCY_ORDER 추가 라인
 */

import java.io.File

if (args.isEmpty()) {
    System.err.println("Usage: kotlin generate-metadata.main.kts <entity-rel-path>")
    System.err.println("  예: kotlin generate-metadata.main.kts claim/entity/Claim")
    System.exit(1)
}

val entityRelPath = args[0]
val scriptDir = File(System.getProperty("user.dir"))

/**
 * cwd 기준 repo root 탐색 — backend/src/main/kotlin/com/otoki/powersales 존재 확인.
 */
fun resolveRepoRoot(): File {
    val cwd = scriptDir
    val candidates = listOf(cwd, cwd.parentFile, cwd.parentFile?.parentFile).filterNotNull()
    for (c in candidates) {
        if (File(c, "backend/src/main/kotlin/com/otoki/powersales").exists()) return c
    }
    error("repo root 탐색 실패 — cwd=${cwd.absolutePath}")
}

val repoRoot = resolveRepoRoot()
val backendBase = File(repoRoot, "backend/src/main/kotlin/com/otoki/powersales")
val entityFile = File(backendBase, "$entityRelPath.kt")
if (!entityFile.exists()) {
    System.err.println("Entity 파일 없음: ${entityFile.absolutePath}")
    System.exit(1)
}

// =============================================================================
// Entity 파싱
// =============================================================================

data class EntityField(
    val sfFieldName: String,
    val dbColumnName: String,
    val kotlinType: String,
    val nullable: Boolean,
    val length: Int?
)

data class EntityInfo(
    val sObjectName: String,
    val tableName: String,
    val targetName: String,
    val pkColumn: String,
    val conflictKey: String,
    val fields: List<EntityField>
)

val SOBJECT_REGEX = Regex("""@SFObject\("([^"]+)"\)""")
// @Table 의 첫 번째 name 만 추출 — indexes block 의 name 은 무시.
// @Table( + 줄바꿈 가능 + name = "value" 의 가장 가까운 위치.
val TABLE_REGEX = Regex("""@Table\s*\(\s*(?:[^)"]|"[^"]*")*?name\s*=\s*"([^"]+)"""", RegexOption.DOT_MATCHES_ALL)

val ID_COLUMN_REGEX = Regex("""@Id""")
val COLUMN_NAME_REGEX = Regex("""@Column\([^)]*name\s*=\s*"([^"]+)"[^)]*\)""")
val COLUMN_LENGTH_REGEX = Regex("""length\s*=\s*(\d+)""")
val COLUMN_NULLABLE_FALSE_REGEX = Regex("""nullable\s*=\s*false""")
val VAR_TYPE_REGEX = Regex("""\b(?:var|val)\s+\w+\s*:\s*([\w.<>?]+)""")
val SFFIELD_REGEX = Regex("""@SFField\("([^"]+)"\)""")

fun parseEntity(file: File, entityRelPath: String): EntityInfo {
    val text = file.readText()
    val lines = text.lines()

    val sObjectName = SOBJECT_REGEX.find(text)?.groupValues?.get(1)
        ?: error("@SFObject 어노테이션 없음: ${file.absolutePath}")

    // @Table(name = "...") — 큰따옴표 안 escaped (\"user\") 케이스 처리
    val tableMatch = TABLE_REGEX.find(text)
    val rawTable = tableMatch?.groupValues?.get(1) ?: error("@Table 매칭 실패")
    // \"user\" -> user, "user" -> user
    val tableName = rawTable.removePrefix("\\\"").removeSuffix("\\\"").removePrefix("\"").removeSuffix("\"")

    // target 이름 = file basename
    val targetName = file.nameWithoutExtension

    // PK 컬럼 — @Id 뒤의 @Column name
    var pkColumn = "${tableName}_id"
    for ((i, line) in lines.withIndex()) {
        if (ID_COLUMN_REGEX.containsMatchIn(line)) {
            for (j in i..minOf(i + 4, lines.size - 1)) {
                COLUMN_NAME_REGEX.find(lines[j])?.let {
                    pkColumn = it.groupValues[1]
                    return@let
                }
                if (lines[j].contains("val id") || lines[j].contains("var id")) break
            }
            break
        }
    }

    // @SFField 블록 파싱 — 같은 필드의 @Column / var/val Type 추출
    // 종료 조건: 다음 @SFField 등장 또는 빈 줄 2회 연속 또는 } 만나기
    val fields = mutableListOf<EntityField>()
    var i = 0
    while (i < lines.size) {
        val sfMatch = SFFIELD_REGEX.find(lines[i])
        if (sfMatch == null) { i++; continue }
        val sfFieldName = sfMatch.groupValues[1]

        var dbName: String? = null
        var nullable = true
        var length: Int? = null
        var kotlinType: String? = null

        var j = i + 1
        while (j < lines.size && j <= i + 12) {
            val l = lines[j]
            // 다음 @SFField 등장 시 중단
            if (SFFIELD_REGEX.containsMatchIn(l)) break

            // @Column 매칭 (아직 못 잡았으면)
            if (dbName == null) {
                COLUMN_NAME_REGEX.find(l)?.let { m ->
                    dbName = m.groupValues[1]
                    if (COLUMN_NULLABLE_FALSE_REGEX.containsMatchIn(l)) nullable = false
                    COLUMN_LENGTH_REGEX.find(l)?.let { length = it.groupValues[1].toInt() }
                }
            }
            // var/val type 매칭 (아직 못 잡았으면)
            if (kotlinType == null) {
                VAR_TYPE_REGEX.find(l)?.let { m ->
                    kotlinType = m.groupValues[1]
                }
            }
            // 둘 다 잡았으면 종료
            if (dbName != null && kotlinType != null) break
            j++
        }

        if (dbName != null) {
            fields.add(EntityField(sfFieldName, dbName!!, kotlinType ?: "String", nullable, length))
        }
        i++
    }

    // sfid 가 entity 에 있으면 첫 필드로 Id 추가
    val hasSfid = text.contains(Regex("""@Column\([^)]*name\s*=\s*"sfid""""))
    val allFields = if (hasSfid) {
        listOf(EntityField("Id", "sfid", "String", false, 18)) + fields
    } else fields

    return EntityInfo(
        sObjectName = sObjectName,
        tableName = tableName,
        targetName = targetName,
        pkColumn = pkColumn,
        conflictKey = "sfid",     // 기본 — 추후 수동 조정
        fields = allFields
    )
}

/**
 * Kotlin type → isString 결정.
 *  - boolean/숫자/날짜 → isString=false
 *  - 그 외 (String/enum/reference/textarea) → isString=true (default)
 */
fun isStringType(type: String): Boolean {
    val base = type.trimEnd('?').substringAfterLast('.')
    return when (base) {
        "Boolean", "Int", "Long", "Short", "Float", "Double",
        "BigDecimal", "BigInteger",
        "LocalDate", "LocalDateTime", "OffsetDateTime", "Instant", "ZonedDateTime" -> false
        else -> true
    }
}

// =============================================================================
// 출력 생성
// =============================================================================

fun toUpperSnake(name: String): String = name.replace(Regex("([a-z])([A-Z])"), "$1_$2").uppercase()

fun renderMetadata(info: EntityInfo): String {
    val metaName = "${toUpperSnake(info.targetName)}_METADATA"
    val sb = StringBuilder()
    sb.appendLine("val $metaName = EntityMetadata(")
    sb.appendLine("    targetName = \"${info.targetName}\",")
    sb.appendLine("    sObjectName = \"${info.sObjectName}\",")
    sb.appendLine("    tableName = \"${info.tableName}\",")
    sb.appendLine("    pkColumn = \"${info.pkColumn}\",")
    sb.appendLine("    conflictKey = \"${info.conflictKey}\",")
    sb.appendLine("    fields = listOf(")
    for ((idx, f) in info.fields.withIndex()) {
        val isStr = isStringType(f.kotlinType)
        val parts = mutableListOf("\"${f.sfFieldName}\"", "\"${f.dbColumnName}\"")
        if (!f.nullable) parts.add("nullable = false")
        if (!isStr) parts.add("isString = false")
        val comma = if (idx == info.fields.size - 1) "" else ","
        sb.appendLine("        FieldMapping(${parts.joinToString(", ")})$comma")
    }
    sb.appendLine("    )")
    sb.appendLine(")")
    return sb.toString()
}

fun renderSoql(info: EntityInfo): String {
    val varName = "${toUpperSnake(info.targetName)}_SOQL"
    val sb = StringBuilder()
    sb.appendLine("$varName=\$(cat <<'EOF'")
    sb.appendLine("SELECT")
    val fieldList = info.fields.map { it.sfFieldName }
    // SOQL 한 줄에 약 80자 단위로 줄바꿈
    val chunks = mutableListOf<MutableList<String>>()
    chunks.add(mutableListOf())
    var currentLen = 4
    for (f in fieldList) {
        val addLen = f.length + 2
        if (currentLen + addLen > 80 && chunks.last().isNotEmpty()) {
            chunks.add(mutableListOf())
            currentLen = 4
        }
        chunks.last().add(f)
        currentLen += addLen
    }
    for ((idx, chunk) in chunks.withIndex()) {
        val joined = chunk.joinToString(", ")
        val suffix = if (idx == chunks.size - 1) "" else ","
        sb.appendLine("    $joined$suffix")
    }
    sb.appendLine("FROM ${info.sObjectName}")
    // IsDeleted 필드 있으면 WHERE 조건 자동
    if (fieldList.contains("IsDeleted")) {
        sb.appendLine("WHERE IsDeleted = FALSE")
    }
    sb.appendLine("EOF")
    sb.appendLine(")")
    return sb.toString()
}

fun renderTargetSpec(info: EntityInfo, entityRelPath: String): String {
    val metaName = "${toUpperSnake(info.targetName)}_METADATA"
    // csv 파일명 — table 의 복수형 또는 snake
    val csvName = info.tableName + "s.csv"   // 단순 규칙, 충돌 시 수동 조정
    return """    "${info.targetName}" to TargetSpec($metaName, "${info.sObjectName}", "$csvName", "$entityRelPath"),"""
}

fun renderExtractRunQuery(info: EntityInfo): String {
    val varName = "${toUpperSnake(info.targetName)}_SOQL"
    val csvName = info.tableName + "s.csv"
    return """
if contains_target "${info.targetName}"; then
    run_query "${info.targetName} (${info.sObjectName})" "${'$'}$varName" "${'$'}OUT_DIR/$csvName"
fi
""".trimIndent()
}

// =============================================================================
// Main
// =============================================================================

val info = parseEntity(entityFile, entityRelPath)

println("=".repeat(60))
println("자동 생성 결과: ${info.targetName} (${info.sObjectName})")
println("=".repeat(60))
println()

println("// === [1] common.kts 에 추가 ===")
println()
println(renderMetadata(info))
println()
println("// === [2] common.kts 의 TARGET_SPECS map 에 추가 ===")
println()
println(renderTargetSpec(info, entityRelPath))
println()
println("// === [3] common.kts 의 SUPPORTED_TARGETS / TARGET_DEPENDENCY_ORDER 에 추가 ===")
println()
println("\"${info.targetName}\"")
println()
println("# === [4] extract-csv.sh 에 추가 ===")
println()
println(renderSoql(info))
println(renderExtractRunQuery(info))
println()
println("=".repeat(60))
println("검토 후 수동 통합 — 그 후 verify-metadata.main.kts 로 정합 확인")
println("=".repeat(60))
