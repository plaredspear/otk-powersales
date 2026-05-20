#!/usr/bin/env kotlin

/**
 * Stage 1 — Raw INSERT (Spec #764, v11 — K2 cache 무효화 / COPY FROM STDIN 전략 추가).
 *
 * 책임: 추출된 CSV 를 staging 영역에 1:1 로 JDBC batch INSERT 또는 COPY FROM STDIN.
 *       - enum 변환 / transform 일체 수행하지 않음 (Stage 2 의 책임).
 *       - SF 원본 값을 그대로 적재 (raw_columns_as_string 컬럼은 한글 picklist 그대로).
 *       - ON CONFLICT DO NOTHING 으로 멱등성 보장 (재실행 안전).
 *
 * 입력: input/{organizations.csv, employees.csv, users.csv, permission_set_assignments.csv}
 * DB 연결: db.properties (host/port/database/user/password)
 *
 * CLI 인자:
 *   --target=Organization,Employee,User,Permission  (default: 전체)
 *   --input-dir=<path>                              (default: ./input)
 *   --output-dir=<path>                             (default: ./output)   리포트만 출력
 *   --reset                                         시작 전 db-reset.sh truncate 자동 실행 (확인 없이)
 *   --no-reset                                      시작 전 reset 확인 프롬프트 건너뜀 (reset 안 함)
 *   (위 두 옵션 모두 미지정 시: 시작 전 사용자에게 reset 여부 묻고 응답에 따라 분기)
 *
 * 실행:
 *   kotlin scripts/sf-data-migration/migrate-stage1.main.kts [--target=...] [--input-dir=...] [--reset]
 */

@file:Repository("https://repo.maven.apache.org/maven2/")
@file:DependsOn("com.opencsv:opencsv:5.9")
@file:DependsOn("org.postgresql:postgresql:42.7.4")
@file:Import("common.kts")

import java.io.File
import java.sql.Connection
import java.sql.Types
import org.postgresql.PGConnection
import org.postgresql.copy.CopyManager

// =============================================================================
// JDBC batch INSERT (lambda-free — K2 cross-file safe)
// =============================================================================

val BATCH_SIZE = 500

/**
 * NOT NULL FieldMapping (nullable=false) 의 값이 모두 채워진 row 만 통과.
 * SF 시스템 사용자 (Platform Integration User 등 사번 NULL) 같이 신규 시스템에
 * 적재 불가능한 row 를 사전 차단하여 NOT NULL 제약 위반으로 batch 가 abort 되는 사고 회피.
 */
fun filterValidRows(
    fields: List<FieldMapping>,
    rows: List<Map<String, String?>>
): Pair<List<Map<String, String?>>, Int> {
    val requiredFields = fields.filter { !it.nullable }
    if (requiredFields.isEmpty()) return rows to 0
    val valid = mutableListOf<Map<String, String?>>()
    var skipped = 0
    for (row in rows) {
        val ok = requiredFields.all { row[it.sfFieldName]?.isNotBlank() == true }
        if (ok) valid.add(row) else skipped++
    }
    return valid to skipped
}

/**
 * EntityMetadata 기준 raw INSERT (ON CONFLICT DO NOTHING).
 * 반환: 실제 insert 된 row 수 (executeBatch 결과 합).
 */
fun applyStageOneRawInsert(
    conn: Connection,
    meta: EntityMetadata,
    rows: List<Map<String, String?>>,
    progress: ProgressBar? = null
): Int {
    if (rows.isEmpty()) return 0
    val allColumns = meta.fields.map { it.dbColumnName } + meta.extraStaticColumns.keys.toList()
    val columns = allColumns.joinToString(", ")
    val placeholders = allColumns.joinToString(", ") { "?" }
    val quotedTable = quoteTable(meta.tableName)
    val sql = "INSERT INTO ${meta.schemaName}.$quotedTable ($columns) VALUES ($placeholders) " +
              "ON CONFLICT DO NOTHING"
    var inserted = 0
    var processed = 0
    conn.prepareStatement(sql).use { ps ->
        for (row in rows) {
            for ((idx, f) in meta.fields.withIndex()) {
                val value = row[f.sfFieldName]
                if (value == null) {
                    ps.setNull(idx + 1, Types.NULL)
                } else {
                    ps.setString(idx + 1, value)
                }
            }
            val extraOffset = meta.fields.size
            for ((extraIdx, entry) in meta.extraStaticColumns.entries.withIndex()) {
                val value = entry.value
                if (value == null) {
                    ps.setNull(extraOffset + extraIdx + 1, Types.NULL)
                } else {
                    ps.setString(extraOffset + extraIdx + 1, value)
                }
            }
            ps.addBatch()
            processed++
            if (processed % BATCH_SIZE == 0) {
                val results = ps.executeBatch()
                inserted += results.count { it > 0 || it == java.sql.Statement.SUCCESS_NO_INFO }
                progress?.update(processed, "${meta.targetName} ($processed/${rows.size})")
            }
        }
        if (processed % BATCH_SIZE != 0) {
            val results = ps.executeBatch()
            inserted += results.count { it > 0 || it == java.sql.Statement.SUCCESS_NO_INFO }
            progress?.update(processed, "${meta.targetName} ($processed/${rows.size})")
        }
    }
    return inserted
}

/**
 * EntityMetadata 기준 raw INSERT — streaming 버전.
 *
 * parseCsvFile (전체 메모리 적재) 대신 streamCsvFile (row-by-row) 를 사용하여
 * 백만 단위 entity 에서도 OOM 없이 처리. filterValidRows 의 row 별 검증 inline 처리.
 *
 * 반환: Triple(inserted, totalRows, filteredOut). totalRows 는 blank 제외 입력 row 수.
 */
fun applyStageOneStreamingInsert(
    conn: Connection,
    meta: EntityMetadata,
    csvFile: File,
    progress: ProgressBar? = null
): Triple<Int, Int, Int> {
    val allColumns = meta.fields.map { it.dbColumnName } + meta.extraStaticColumns.keys.toList()
    val columns = allColumns.joinToString(", ")
    val placeholders = allColumns.joinToString(", ") { "?" }
    val quotedTable = quoteTable(meta.tableName)
    val sql = "INSERT INTO ${meta.schemaName}.$quotedTable ($columns) VALUES ($placeholders) " +
              "ON CONFLICT DO NOTHING"
    val requiredFields = meta.fields.filter { !it.nullable }
    var inserted = 0
    var processed = 0
    var filteredOut = 0
    var totalRows = 0
    conn.prepareStatement(sql).use { ps ->
        totalRows = streamCsvFile(csvFile) { row ->
            // NOT NULL pre-filter inline.
            val ok = requiredFields.all { row[it.sfFieldName]?.isNotBlank() == true }
            if (!ok) {
                filteredOut++
                return@streamCsvFile
            }
            for ((idx, f) in meta.fields.withIndex()) {
                val raw = row[f.sfFieldName]
                if (raw == null) {
                    ps.setNull(idx + 1, Types.NULL)
                } else {
                    // PostgreSQL `stringtype=unspecified` 로 모든 컬럼 타입 (NUMERIC / DATE / BOOLEAN
                    // 등) 자동 cast. SF describe scale=0 은 강제력 없음 (운영 데이터에 소수 가능) —
                    // 정수 도메인 가정 코드를 제거하고 모든 numeric 컬럼을 NUMERIC 으로 통일 (V168).
                    ps.setString(idx + 1, raw)
                }
            }
            val extraOffset = meta.fields.size
            for ((extraIdx, entry) in meta.extraStaticColumns.entries.withIndex()) {
                val value = entry.value
                if (value == null) {
                    ps.setNull(extraOffset + extraIdx + 1, Types.NULL)
                } else {
                    ps.setString(extraOffset + extraIdx + 1, value)
                }
            }
            ps.addBatch()
            processed++
            if (processed % BATCH_SIZE == 0) {
                val results = ps.executeBatch()
                inserted += results.count { it > 0 || it == java.sql.Statement.SUCCESS_NO_INFO }
                progress?.update(processed, "${meta.targetName} ($processed processed)")
            }
        }
        if (processed % BATCH_SIZE != 0) {
            val results = ps.executeBatch()
            inserted += results.count { it > 0 || it == java.sql.Statement.SUCCESS_NO_INFO }
            progress?.update(processed, "${meta.targetName} ($processed processed)")
        }
    }
    return Triple(inserted, totalRows, filteredOut)
}

/**
 * EntityMetadata 기준 COPY FROM STDIN 적재 (백만 단위 row entity 의 속도 개선용).
 *
 * 모드 분기 (resetMode 인자):
 *   - true (--reset 직후): 빈 타겟 테이블에 직접 COPY — 충돌 0건 보장, 최고 속도.
 *   - false (--no-reset 누적): UNLOGGED staging 테이블 → COPY → INSERT-SELECT ON CONFLICT
 *     DO NOTHING → DROP. 멱등성 보전 + WAL 우회로 속도 유지.
 *
 * 반환: Triple(inserted, totalRows, filteredOut).
 *   - resetMode=true: inserted = totalRows - filteredOut (COPY 직접, 충돌 없음)
 *   - resetMode=false: inserted = INSERT-SELECT 의 rowcount (ON CONFLICT 로 skip 된 row 제외)
 */
fun applyStageOneCopyInsert(
    conn: Connection,
    meta: EntityMetadata,
    csvFile: File,
    resetMode: Boolean,
    progress: ProgressBar? = null
): Triple<Int, Int, Int> {
    val allColumns = meta.fields.map { it.dbColumnName } + meta.extraStaticColumns.keys.toList()
    val columnsList = allColumns.joinToString(", ")
    val quotedTable = quoteTable(meta.tableName)
    val fullyQualified = "${meta.schemaName}.$quotedTable"
    val requiredFields = meta.fields.filter { !it.nullable }
    val extraValues = meta.extraStaticColumns.values.toList()

    val copyTargetTable: String
    val stagingTableName: String?
    if (resetMode) {
        copyTargetTable = fullyQualified
        stagingTableName = null
    } else {
        // UNLOGGED staging — WAL 우회로 적재 속도 + DROP 으로 깨끗하게 정리.
        // 세션 trx 내에서만 의미가 있으므로 session-local schema 가 아닌 powersales 에 직접 생성.
        stagingTableName = "${meta.schemaName}._copy_staging_${meta.tableName}"
        copyTargetTable = stagingTableName
        conn.createStatement().use { st ->
            st.executeUpdate("DROP TABLE IF EXISTS $stagingTableName")
            st.executeUpdate(
                "CREATE UNLOGGED TABLE $stagingTableName " +
                "(LIKE $fullyQualified INCLUDING DEFAULTS EXCLUDING CONSTRAINTS EXCLUDING INDEXES)"
            )
        }
    }

    val copySql = "COPY $copyTargetTable ($columnsList) FROM STDIN " +
                  "WITH (FORMAT csv, NULL '\\N')"
    val pgConn = conn.unwrap(PGConnection::class.java)
    val copyManager: CopyManager = pgConn.copyAPI
    val copyIn = copyManager.copyIn(copySql)

    var totalRows = 0
    var filteredOut = 0
    var processed = 0
    try {
        streamCsvFile(csvFile) { row ->
            totalRows++
            val ok = requiredFields.all { row[it.sfFieldName]?.isNotBlank() == true }
            if (!ok) {
                filteredOut++
                return@streamCsvFile
            }
            val values = ArrayList<String?>(meta.fields.size + extraValues.size)
            for (f in meta.fields) values.add(row[f.sfFieldName])
            for (v in extraValues) values.add(v)
            val bytes = pgCsvLine(values).toByteArray(Charsets.UTF_8)
            copyIn.writeToCopy(bytes, 0, bytes.size)
            processed++
            if (processed % 10_000 == 0) {
                progress?.update(processed, "${meta.targetName} ($processed COPY rows)")
            }
        }
        copyIn.endCopy()
    } catch (e: Throwable) {
        // copyIn 이 살아있다면 cancel — 트랜잭션은 호출자 rollback 책임.
        try { if (copyIn.isActive) copyIn.cancelCopy() } catch (_: Throwable) {}
        throw e
    }

    val copyRowCount = copyIn.handledRowCount.toInt()
    progress?.update(processed, "${meta.targetName} ($copyRowCount COPY rows)")

    val inserted: Int = if (resetMode) {
        copyRowCount
    } else {
        // staging → 타겟 테이블 INSERT-SELECT ON CONFLICT DO NOTHING.
        // RETURNING 으로 inserted row count 회수. ON CONFLICT 로 skip 된 row 는 미반환.
        val sql = "INSERT INTO $fullyQualified ($columnsList) " +
                  "SELECT $columnsList FROM $stagingTableName " +
                  "ON CONFLICT DO NOTHING"
        val rc = conn.createStatement().use { st -> st.executeUpdate(sql) }
        // staging 정리.
        conn.createStatement().use { st -> st.executeUpdate("DROP TABLE IF EXISTS $stagingTableName") }
        rc
    }
    return Triple(inserted, totalRows, filteredOut)
}

/**
 * PermissionSetAssignment staging INSERT.
 */
fun applyPermissionStagingRawInsert(
    conn: Connection,
    meta: PermissionStagingMetadata,
    rows: List<Map<String, String?>>,
    progress: ProgressBar? = null
): Int {
    if (rows.isEmpty()) return 0
    val columns = meta.fields.joinToString(", ") { it.dbColumnName }
    val placeholders = meta.fields.joinToString(", ") { "?" }
    val sql = "INSERT INTO ${meta.schemaName}.${meta.stagingTableName} ($columns) VALUES ($placeholders) " +
              "ON CONFLICT DO NOTHING"
    var inserted = 0
    var processed = 0
    conn.prepareStatement(sql).use { ps ->
        for (row in rows) {
            for ((idx, f) in meta.fields.withIndex()) {
                val value = row[f.sfFieldName]
                if (value == null) {
                    ps.setNull(idx + 1, Types.NULL)
                } else {
                    ps.setString(idx + 1, value)
                }
            }
            ps.addBatch()
            processed++
            if (processed % BATCH_SIZE == 0) {
                val results = ps.executeBatch()
                inserted += results.count { it > 0 || it == java.sql.Statement.SUCCESS_NO_INFO }
                progress?.update(processed, "Permission ($processed/${rows.size})")
            }
        }
        if (processed % BATCH_SIZE != 0) {
            val results = ps.executeBatch()
            inserted += results.count { it > 0 || it == java.sql.Statement.SUCCESS_NO_INFO }
            progress?.update(processed, "Permission ($processed/${rows.size})")
        }
    }
    return inserted
}

// =============================================================================
// Main
// =============================================================================

val scriptDir = File(System.getProperty("user.dir"))
val argMap = parseKeyValueArgs(args)
val inputDir = File(argMap["input-dir"] ?: File(scriptDir, "input").absolutePath)
val outputDir = File(argMap["output-dir"] ?: File(scriptDir, "output").absolutePath)
val targets = parseTargets(argMap["target"])
outputDir.mkdirs()

val dbConfig = loadDbConfig(scriptDir)

println("=".repeat(60))
println("SF 데이터 마이그레이션 — Stage 1 (Raw INSERT)")
println("=".repeat(60))
println("targets    : ${targets.joinToString(", ")}")
println("input dir  : ${inputDir.absolutePath}")
println("jdbc url   : ${dbConfig.jdbcUrl}")
println("jdbc user  : ${dbConfig.user}")
println()

val sortedTargets = sortTargetsByDependency(targets)
val reports = mutableListOf<TargetReport>()

// 사전 검증 — 대상 target 의 CSV 가 모두 존재하는지 먼저 확인.
// 하나라도 누락 시 DB 접속 / batch 실행 없이 즉시 중단.
val missing = sortedTargets.mapNotNull { target ->
    val spec = TARGET_SPECS[target] ?: error("Unknown target: $target")
    val csv = File(inputDir, spec.csvFileName)
    if (csv.exists()) null else target to csv
}
if (missing.isNotEmpty()) {
    println("❌ 입력 CSV 누락 — Stage 1 중단:")
    for ((target, csv) in missing) {
        println("  - [$target] ${csv.absolutePath}")
    }
    println()
    println("먼저 ./extract-csv.sh --target=${missing.joinToString(",") { it.first }} 로 추출하거나,")
    println("--target 옵션으로 대상 범위를 좁혀 다시 실행하세요.")
    System.exit(1)
}

// =============================================================================
// DB Reset 확인 (db-reset.sh truncate 호출) — Stage 1 시작 전
// =============================================================================
// 정책:
//   --reset      → 확인 없이 reset 실행
//   --no-reset   → 확인 없이 reset 건너뜀
//   둘 다 없음   → 사용자에게 묻고 응답에 따라 분기 (대화형 stdin 필요)
val resetExplicit = args.any { it == "--reset" }
val noResetExplicit = args.any { it == "--no-reset" }
val shouldReset: Boolean = when {
    resetExplicit && noResetExplicit -> error("--reset 과 --no-reset 동시 지정 불가")
    resetExplicit -> true
    noResetExplicit -> false
    else -> {
        println("Stage 1 시작 전 powersales 스키마를 TRUNCATE RESTART IDENTITY CASCADE 로 초기화하시겠습니까?")
        println("  - 기존 모든 데이터 삭제 + IDENTITY PK 초기화")
        println("  - flyway_schema_history 는 보존")
        print("초기화하려면 'y' 입력, 아니면 Enter: ")
        val answer = readLine()?.trim()?.lowercase()
        if (answer == null) {
            println("(stdin 미가용 — reset 건너뜀. 강제로 reset 하려면 --reset 옵션 사용)")
            false
        } else {
            answer == "y" || answer == "yes"
        }
    }
}

if (shouldReset) {
    val dbResetScript = File(scriptDir.parentFile ?: File(".."), "db-reset.sh")
    val dbPropsPath = File(scriptDir, "db.properties").absolutePath
    if (!dbResetScript.exists()) {
        error("db-reset.sh 를 찾을 수 없습니다: ${dbResetScript.absolutePath}")
    }
    println()
    println("=== DB 초기화 실행 (db-reset.sh truncate) ===")
    val pb = ProcessBuilder(
        "bash",
        dbResetScript.absolutePath,
        "--db-properties", dbPropsPath,
        "--mode", "truncate",
        "--yes"
    ).inheritIO()
    val exitCode = pb.start().waitFor()
    if (exitCode != 0) {
        error("db-reset.sh 가 비정상 종료되었습니다 (exit=$exitCode)")
    }
    println("=== DB 초기화 완료 ===")
    println()
} else {
    println("(reset 건너뜀 — 기존 데이터 위에 ON CONFLICT DO NOTHING 로 누적 적재)")
    println()
}

for (target in sortedTargets) {
    val spec = TARGET_SPECS[target]
        ?: error("Unknown target: $target")
    var report = TargetReport(targetName = target, sObjectName = spec.sObjectName)

    val csvFile = File(inputDir, spec.csvFileName)
    val conn = openConnection(dbConfig)
    try {
        when (val m = spec.meta) {
            is EntityMetadata -> {
                // streaming path — CSV 를 row 단위로 읽으면서 즉시 INSERT (백만 단위 OOM 회피).
                // useCopyStrategy=true 인 entity 는 COPY FROM STDIN path 로 분기 (속도 5~20×).
                val estimatedTotal = countCsvDataRows(csvFile)
                val strategyLabel = if (m.useCopyStrategy) "COPY" else "INSERT"
                val pb = ProgressBar("Stage 1 $target ($strategyLabel)", estimatedTotal)
                val (inserted, totalRows, filteredOut) = if (m.useCopyStrategy) {
                    applyStageOneCopyInsert(conn, m, csvFile, resetMode = shouldReset, progress = pb)
                } else {
                    applyStageOneStreamingInsert(conn, m, csvFile, pb)
                }
                conn.commit()
                if (filteredOut > 0) {
                    println("\n[$target] 필수 필드 누락 row 사전 제외: ${filteredOut}건")
                }
                val validRows = totalRows - filteredOut
                val conflictSkipped = validRows - inserted
                pb.done("inserted=$inserted (filtered=$filteredOut, conflict_skipped=$conflictSkipped)")
                report = report.copy(rawRowsCount = totalRows, insertedCount = inserted, stage1Applied = true)
            }
            is PermissionStagingMetadata -> {
                // Permission staging 은 row 수 적음 — 기존 readAll path 유지.
                val allRows = parseCsvFile(csvFile)
                val (rows, filteredOut) = filterValidRows(m.fields, allRows)
                if (filteredOut > 0) {
                    println("[$target] 필수 필드 누락 row 사전 제외: ${filteredOut}건")
                }
                val pb = ProgressBar("Stage 1 $target", rows.size)
                val inserted = applyPermissionStagingRawInsert(conn, m, rows, pb)
                conn.commit()
                val conflictSkipped = rows.size - inserted
                pb.done("inserted=$inserted (filtered=$filteredOut, conflict_skipped=$conflictSkipped)")
                report = report.copy(rawRowsCount = allRows.size, insertedCount = inserted, stage1Applied = true)
            }
            else -> {}
        }
    } catch (e: Exception) {
        conn.rollback()
        val rootMessage = formatJdbcError(e)
        println("\n[$target] FAILED: $rootMessage")
        report = report.copy(errors = listOf("INSERT failed: $rootMessage"))
    } finally {
        conn.close()
    }
    reports.add(report)
}

val reportFile = File(outputDir, "migration_report_stage1.txt")
writeReport(reports, "1", reportFile)
println()
println("✅ Stage 1 완료. 리포트: ${reportFile.absolutePath}")
println("다음 단계: kotlin migrate-stage2.main.kts")
