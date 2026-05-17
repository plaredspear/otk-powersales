#!/usr/bin/env kotlin

/**
 * Stage 2 — Logical 처리 (Spec #764).
 *
 * 책임: Stage 1 으로 적재된 raw 데이터에 대한 logical 변환을 JDBC 로 직접 적용.
 *       - Stage 2-B: 한글 picklist → enum 변환 (role / ppt / profile_type)
 *       - Stage 2-C: Transform (BCrypt password hash)
 *       - Stage 2-D: PermissionSet → AdminPermission 매핑
 *       - reset: dev DB 의 마이그레이션 산출물 일괄 삭제
 *
 * 각 substep 은 독립 transaction — 한 substep 실패 시 다른 substep 의 적용 결과 유지.
 *
 * 입력: input/users.csv (BCrypt password 생성용 employee_code 목록만 필요)
 * DB 연결: db.properties
 *
 * CLI 인자:
 *   --substep=all|role|ppt|profile|password|permission  (default: all)
 *   --target=Organization,Employee,User,Permission      (default: 전체)
 *   --input-dir=<path>                                  (default: ./input)
 *   --output-dir=<path>                                 (default: ./output) — 리포트만
 *   --reset                                             (dev DB reset 직접 실행)
 *
 * 실행:
 *   kotlin scripts/sf-data-migration/migrate-stage2.main.kts [--substep=...]
 *   kotlin scripts/sf-data-migration/migrate-stage2.main.kts --reset
 */

@file:Repository("https://repo.maven.apache.org/maven2/")
@file:DependsOn("com.opencsv:opencsv:5.9")
@file:DependsOn("org.mindrot:jbcrypt:0.4")
@file:DependsOn("org.postgresql:postgresql:42.7.4")
@file:Import("common.kts")

import java.io.File
import java.sql.Connection
import org.mindrot.jbcrypt.BCrypt

// =============================================================================
// Stage 2-B — Mapping (raw 한글 -> enum)
// =============================================================================

fun applyStageTwoMapping(
    conn: Connection,
    tableName: String,
    columnName: String,
    mapping: Map<String, String>,
    fallbackValue: String?,
    progress: ProgressBar? = null
): Int {
    val quotedTable = quoteTable(tableName)
    val updateSql = "UPDATE powersales.$quotedTable SET $columnName = ? WHERE $columnName = ?"
    var totalUpdated = 0
    var idx = 0
    conn.prepareStatement(updateSql).use { ps ->
        for ((rawValue, enumValue) in mapping) {
            ps.setString(1, enumValue)
            ps.setString(2, rawValue)
            totalUpdated += ps.executeUpdate()
            idx++
            progress?.update(idx, "$rawValue -> $enumValue")
        }
    }
    val enumList = mapping.values.toSet()
    val placeholders = enumList.joinToString(", ") { "?" }
    if (fallbackValue != null) {
        // 매칭 실패 row 를 명시적 fallback 값으로 (NOT NULL enum 컬럼 대응).
        val fallbackSql = "UPDATE powersales.$quotedTable SET $columnName = ? " +
                          "WHERE $columnName IS NOT NULL AND $columnName NOT IN ($placeholders)"
        conn.prepareStatement(fallbackSql).use { ps ->
            ps.setString(1, fallbackValue)
            for ((i, v) in enumList.withIndex()) ps.setString(i + 2, v)
            totalUpdated += ps.executeUpdate()
        }
    } else {
        // fallback 없음 — 매칭 실패 row 는 NULL 로 비움 (nullable enum 컬럼 대응).
        val nullSql = "UPDATE powersales.$quotedTable SET $columnName = NULL " +
                      "WHERE $columnName IS NOT NULL AND $columnName NOT IN ($placeholders)"
        conn.prepareStatement(nullSql).use { ps ->
            for ((i, v) in enumList.withIndex()) ps.setString(i + 1, v)
            totalUpdated += ps.executeUpdate()
        }
    }
    return totalUpdated
}

// =============================================================================
// Stage 2-C — Transform (BCrypt password)
// =============================================================================

fun applyStageTwoPassword(
    conn: Connection,
    employeeCodes: List<String>,
    progress: ProgressBar? = null
): Int {
    if (employeeCodes.isEmpty()) return 0
    val sql = "UPDATE powersales.\"user\" SET password = ?, password_change_required = TRUE " +
              "WHERE employee_code = ? AND (password IS NULL OR password = '')"
    var totalUpdated = 0
    var processed = 0
    conn.prepareStatement(sql).use { ps ->
        for (code in employeeCodes) {
            val hash = BCrypt.hashpw(code, BCrypt.gensalt(10))
            ps.setString(1, hash)
            ps.setString(2, code)
            totalUpdated += ps.executeUpdate()
            processed++
            progress?.update(processed, "BCrypt hash ($processed/${employeeCodes.size})")
        }
    }
    return totalUpdated
}

// =============================================================================
// Stage 2-D — Permission mapping
// =============================================================================

fun applyPermissionMapping(
    conn: Connection,
    progress: ProgressBar? = null
): Int {
    val sql = """
        INSERT INTO powersales.user_permission (user_id, permission, created_at)
        SELECT DISTINCT u.user_id, ?, NOW()
        FROM powersales.sf_permission_set_assignment_raw psa
        JOIN powersales."user" u ON u.employee_code = psa.assignee_employee_code
        WHERE psa.permission_set_name = ?
        ON CONFLICT DO NOTHING
    """.trimIndent()
    var totalInserted = 0
    var idx = 0
    conn.prepareStatement(sql).use { ps ->
        for ((permSetName, adminPermissions) in PERMISSION_SET_TO_PERMISSIONS) {
            for (adminPermission in adminPermissions) {
                ps.setString(1, adminPermission)
                ps.setString(2, permSetName)
                totalInserted += ps.executeUpdate()
                idx++
                progress?.update(idx, "$permSetName -> $adminPermission")
            }
        }
    }
    return totalInserted
}

// =============================================================================
// DB Reset (dev only)
// =============================================================================

fun applyDbReset(conn: Connection) {
    conn.prepareStatement("TRUNCATE TABLE powersales.sf_permission_set_assignment_raw").use { it.executeUpdate() }
    // user 삭제 전 FK 의존 (user_permission.user_id → user.user_id) 해소
    conn.prepareStatement(
        "DELETE FROM powersales.user_permission WHERE user_id IN " +
        "(SELECT user_id FROM powersales.\"user\" WHERE sfid IS NOT NULL)"
    ).use { it.executeUpdate() }

    // TARGET_SPECS 의 모든 EntityMetadata 의 sfid IS NOT NULL row 일괄 삭제 (역순 — FK 의존 회피).
    // K2 cross-file lambda bug 회피 — for loop 으로 entity 수집.
    val tablesToReset = mutableListOf<String>()
    for ((_, spec) in TARGET_SPECS) {
        val meta = spec.meta
        if (meta is EntityMetadata) {
            tablesToReset.add(meta.tableName)
        }
    }
    // 의존 안전을 위해 dependency 역순 처리 (user/employee 먼저, organization 마지막).
    for (table in tablesToReset.reversed()) {
        val quoted = quoteTable(table)
        try {
            conn.prepareStatement("DELETE FROM powersales.$quoted WHERE sfid IS NOT NULL").use { it.executeUpdate() }
        } catch (e: Exception) {
            // 일부 entity 는 FK 의존으로 단일 commit 안에서 실패 가능 — 로그만 남기고 진행.
            println("[reset warn] $table: ${e.message}")
        }
    }
}

// =============================================================================
// Main
// =============================================================================

val scriptDir = File(System.getProperty("user.dir"))
val argMap = parseKeyValueArgs(args)
val resetFlag = args.any { it == "--reset" } || argMap["reset"] == "true"
val inputDir = File(argMap["input-dir"] ?: File(scriptDir, "input").absolutePath)
val outputDir = File(argMap["output-dir"] ?: File(scriptDir, "output").absolutePath)
val targets = parseTargets(argMap["target"])
val substep = argMap["substep"] ?: "all"
require(substep in setOf("all", "role", "ppt", "profile", "password", "permission")) {
    "--substep must be all/role/ppt/profile/password/permission"
}
outputDir.mkdirs()

val dbConfig = loadDbConfig(scriptDir)

println("=".repeat(60))
println("SF 데이터 마이그레이션 — Stage 2 (Logical)")
println("=".repeat(60))
if (resetFlag) {
    println("mode       : RESET (dev DB 초기화)")
} else {
    println("substep    : $substep")
    println("targets    : ${targets.joinToString(", ")}")
}
println("jdbc url   : ${dbConfig.jdbcUrl}")
println("jdbc user  : ${dbConfig.user}")
println()

if (resetFlag) {
    val pb = ProgressBar("Reset", 5)
    val conn = openConnection(dbConfig)
    try {
        applyDbReset(conn)
        conn.commit()
        pb.done("reset 완료")
        println("✅ dev DB reset 완료")
    } catch (e: Exception) {
        conn.rollback()
        pb.done("FAILED")
        println("\nRESET FAILED: ${e.message}")
        throw e
    } finally {
        conn.close()
    }
    System.exit(0)
}

val sortedTargets = sortTargetsByDependency(targets)
val reports = mutableListOf<TargetReport>()
val runRole = substep == "all" || substep == "role"
val runPpt = substep == "all" || substep == "ppt"
val runProfile = substep == "all" || substep == "profile"
val runPassword = substep == "all" || substep == "password"
val runPermission = substep == "all" || substep == "permission"

fun runSubstep(label: String, total: Int, block: (Connection, ProgressBar) -> Int): Pair<Int, String?> {
    val pb = ProgressBar(label, total)
    val conn = openConnection(dbConfig)
    return try {
        val n = block(conn, pb)
        conn.commit()
        pb.done("$n rows affected")
        n to null
    } catch (e: Exception) {
        conn.rollback()
        pb.done("FAILED")
        println("\n[$label] FAILED: ${e.message}")
        0 to "$label failed: ${e.message}"
    } finally {
        conn.close()
    }
}

for (target in sortedTargets) {
    val sObject = when (target) {
        "Organization" -> "Org__c"
        "Employee" -> "DKRetail__Employee__c"
        "User" -> "User"
        "Permission" -> "PermissionSetAssignment"
        else -> "?"
    }
    var report = TargetReport(targetName = target, sObjectName = sObject)
    val errs = mutableListOf<String>()

    when (target) {
        "Employee" -> {
            if (runRole) {
                val (n, err) = runSubstep(
                    "Stage 2-B role (Employee)",
                    APP_AUTHORITY_TO_USER_ROLE.size
                ) { conn, pb ->
                    applyStageTwoMapping(conn, "employee", "role",
                        APP_AUTHORITY_TO_USER_ROLE, USER_ROLE_FALLBACK, pb)
                }
                if (err == null) report = report.copy(stage2MappingApplied = true, insertedCount = report.insertedCount + n)
                else errs.add(err)
            }
            if (runPpt) {
                val (n, err) = runSubstep(
                    "Stage 2-B professional_promotion_team (Employee)",
                    PPT_KOREAN_TO_ENUM.size
                ) { conn, pb ->
                    // SF free text 필드라 fallback NULL — 미정의 한글 값은 NOT IN 매칭으로 NULL 처리.
                    applyStageTwoMapping(conn, "employee", "professional_promotion_team",
                        PPT_KOREAN_TO_ENUM, null, pb)
                }
                if (err == null) report = report.copy(stage2MappingApplied = true, insertedCount = report.insertedCount + n)
                else errs.add(err)
            }
        }
        "User" -> {
            if (runProfile) {
                val (n, err) = runSubstep(
                    "Stage 2-B profile_type (User)",
                    PROFILE_NAME_TO_PROFILE_TYPE.size
                ) { conn, pb ->
                    applyStageTwoMapping(conn, "user", "profile_type",
                        PROFILE_NAME_TO_PROFILE_TYPE, PROFILE_TYPE_FALLBACK, pb)
                }
                if (err == null) report = report.copy(stage2MappingApplied = true, insertedCount = report.insertedCount + n)
                else errs.add(err)
            }
            if (runPassword) {
                val csv = File(inputDir, "users.csv")
                if (!csv.exists()) {
                    println("[User password] users.csv not found: ${csv.absolutePath}")
                    errs.add("CSV not found: users.csv (password substep 용)")
                } else {
                    val rows = parseCsvFile(csv)
                    val codes = mutableListOf<String>()
                    for (r in rows) {
                        val c = r["DKRetail__EmployeeNumber__c"]
                        if (c != null && c !in codes) codes.add(c)
                    }
                    val (n, err) = runSubstep("Stage 2-C password (User)", codes.size) { conn, pb ->
                        applyStageTwoPassword(conn, codes, pb)
                    }
                    if (err == null) report = report.copy(stage2TransformApplied = true, insertedCount = report.insertedCount + n)
                    else errs.add(err)
                }
            }
        }
        "Permission" -> {
            if (runPermission) {
                val totalSteps = PERMISSION_SET_TO_PERMISSIONS.values.sumOf { it.size }
                val (n, err) = runSubstep("Stage 2-D Permission mapping", totalSteps) { conn, pb ->
                    applyPermissionMapping(conn, pb)
                }
                if (err == null) report = report.copy(stage2MappingApplied = true, insertedCount = report.insertedCount + n)
                else errs.add(err)
            }
        }
        "Organization" -> { /* Stage 2 작업 없음 */ }
    }
    if (errs.isNotEmpty()) report = report.copy(errors = errs)
    reports.add(report)
}

val reportFile = File(outputDir, "migration_report_stage2.txt")
writeReport(reports, "2", reportFile)
println()
println("✅ Stage 2 완료. 리포트: ${reportFile.absolutePath}")
