package com.otoki.powersales.herokumigration.stage1

import com.opencsv.CSVReaderBuilder
import com.otoki.powersales.sfmigration.stage1.PgCsvHelper
import org.postgresql.PGConnection
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import java.io.InputStreamReader
import javax.sql.DataSource

/**
 * Heroku migration Stage 1 — S3 의 Heroku export CSV 를 PostgreSQL COPY FROM STDIN 으로 직접 적재.
 *
 * SF [com.otoki.powersales.sfmigration.stage1.Stage1S3CopyService] 와 동형 패턴 (UNLOGGED
 * staging → COPY → INSERT-SELECT → DROP). 로컬 → SSM 터널 → RDS 경로의 broken pipe / 네트워크
 * 병목을 회피하기 위해 backend 의 같은 VPC RDS 직결 connection 을 활용 + S3 stream → COPY 직결로
 * in-memory 적재 없이 row 단위 흐름 유지 (RAM 일정). 대용량 2개 (safetycheck__workschedule__member
 * 91만행 / product_favorites 38만행) 도 안전.
 *
 * SF 와의 차이:
 *  - 적재 메타가 [HerokuStage1Targets] 의 `@HCColumn` 리플렉션 자동 생성 (SF 는 손수 하드코딩).
 *  - 자연 키만 채우고 FK `*_id` 는 NULL 적재 (P2-B 가 사후 resolve). 따라서 staging 의 컬럼은
 *    `@HCColumn` 매핑된 자연 키 컬럼만이며, FK 컬럼은 staging 에 없으므로 INSERT 시 NULL.
 *  - EmployeeInfo 는 PK 가 employee 와 공유 PK(employee_id) 라 적재 시점에 employee_code →
 *    employee_id resolve 가 끝나 있어야 한다 (§6). employee 테이블 JOIN INSERT 로 분기.
 *  - 멱등성은 reset 모드 (적재 전 TRUNCATE, 스펙 Q1 옵션 1) 가 기본. reset=false 시
 *    `ON CONFLICT DO NOTHING` 으로 최소 안전.
 *
 * 권한: 로그인(authenticated)만 요구 (Controller 에서 enforce — 부트스트랩 닭-달걀 회피).
 */
// S3Client 빈은 spring-cloud-aws S3AutoConfiguration 이 제공하며 local 프로파일에서 exclude 된다.
// SF Stage1S3CopyService 와 동일하게 dev/prod 에서만 활성화 (운영 도구 — local 시나리오 없음).
@Service
@Profile("dev | prod")
class HerokuStage1S3CopyService(
    private val dataSource: DataSource,
    private val s3Client: S3Client,
    private val progress: HerokuStage1CopyProgress,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * S3 의 CSV 1개를 적재 (SINGLE 모드).
     *
     * controller 가 이미 begin() 한 RUNNING 상태로 진입하는 게 일반 경로. IDLE 상태로 호출되면
     * (테스트 등) 여기서 begin() 보강.
     */
    fun copyFromS3(
        targetName: String,
        s3Bucket: String,
        s3Key: String,
        reset: Boolean,
        maxRows: Int? = null,
    ): HerokuStage1CopyResult {
        val meta = HerokuStage1Targets.get(targetName)
            ?: run {
                val msg = "Unknown target: $targetName (allowed: ${HerokuStage1Targets.list().joinToString(", ")})"
                progress.finishWithFailure(msg)
                error(msg)
            }

        if (progress.status != HerokuStage1CopyProgress.Status.RUNNING) {
            progress.begin(targetName, s3Bucket, s3Key)
        }
        try {
            val result = executeEntity(meta, s3Bucket, s3Key, reset, maxRows)
            progress.setInserted(result.inserted.toLong())
            progress.setUnmatched(result.unmatched.toLong())
            progress.finishOk()
            return result
        } catch (e: Throwable) {
            val msg = "${e.javaClass.simpleName}: ${e.message ?: "unknown"}"
            progress.finishWithFailure(msg)
            log.error("[heroku-stage1-copy] FAILED target={} reason={}", targetName, msg, e)
            throw e
        }
    }

    /**
     * S3 의 prefix 아래 모든 entity 의 CSV 를 의존성 순서대로 순차 적재 (BATCH 모드).
     *
     * S3 key 는 `<s3KeyPrefix>/<csvFileName>` 자동 조립. 1개 entity 실패 시 즉시 중단 (나머지 SKIPPED).
     */
    fun copyAllFromS3(
        s3Bucket: String,
        s3KeyPrefix: String,
        reset: Boolean,
        maxRows: Int? = null,
    ): HerokuStage1BatchSummary {
        val targets = HerokuStage1Targets.list()
        val prefix = s3KeyPrefix.removeSuffix("/")

        log.info(
            "[heroku-stage1-copy-all] begin s3=s3://{}/{} targets={} reset={} maxRows={}",
            s3Bucket, prefix, targets.size, reset, maxRows ?: "unlimited",
        )

        if (progress.status != HerokuStage1CopyProgress.Status.RUNNING) {
            progress.beginBatch(s3Bucket, targets)
        }

        var success = 0
        var failed = 0
        var totalInserted = 0L

        for (targetName in targets) {
            val meta = HerokuStage1Targets.get(targetName) ?: continue
            val s3Key = "$prefix/${meta.csvFileName}"
            progress.beginEntity(targetName, s3Key)

            val processedBefore = progress.processedRows
            val filteredBefore = progress.filteredOut

            try {
                val result = executeEntity(meta, s3Bucket, s3Key, reset, maxRows)
                val processedDelta = progress.processedRows - processedBefore
                val filteredDelta = progress.filteredOut - filteredBefore
                progress.addInserted(result.inserted.toLong())
                progress.addUnmatched(result.unmatched.toLong())
                progress.finishEntityOk(
                    targetName = targetName,
                    processed = processedDelta,
                    filteredOut = filteredDelta,
                    inserted = result.inserted.toLong(),
                    unmatched = result.unmatched.toLong(),
                )
                totalInserted += result.inserted
                success++
                log.info(
                    "[heroku-stage1-copy-all] entity OK target={} inserted={} unmatched={} processed={}",
                    targetName, result.inserted, result.unmatched, processedDelta,
                )
            } catch (e: Throwable) {
                val msg = "${e.javaClass.simpleName}: ${e.message ?: "unknown"}"
                val processedDelta = progress.processedRows - processedBefore
                val filteredDelta = progress.filteredOut - filteredBefore
                progress.finishEntityFailed(
                    targetName = targetName,
                    processed = processedDelta,
                    filteredOut = filteredDelta,
                    errorMessage = msg,
                )
                progress.markRemainingAsSkipped()
                progress.finishWithFailure("[$targetName] $msg")
                failed++
                log.error(
                    "[heroku-stage1-copy-all] entity FAILED target={} reason={} — batch abort",
                    targetName, msg, e,
                )
                return HerokuStage1BatchSummary(
                    totalTargets = targets.size,
                    success = success,
                    failed = failed,
                    skipped = targets.size - success - failed,
                    totalInserted = totalInserted,
                )
            }
        }

        progress.finishOk()
        log.info(
            "[heroku-stage1-copy-all] done totalTargets={} success={} totalInserted={}",
            targets.size, success, totalInserted,
        )
        return HerokuStage1BatchSummary(
            totalTargets = targets.size,
            success = success,
            failed = 0,
            skipped = 0,
            totalInserted = totalInserted,
        )
    }

    /**
     * Entity 1개 적재 — progress 의 entity 단위 시작/종료 호출은 호출자 책임.
     *
     * staging 에는 `@HCColumn` 매핑된 자연 키 컬럼만 COPY 한다 (FK `*_id` 는 NULL 적재 → P2-B resolve).
     * EmployeeInfo 는 employee JOIN 으로 employee_id PK 를 채워 INSERT (§6).
     *
     * @throws Exception 적재 실패 시 그대로 throw (호출자가 catch).
     */
    private fun executeEntity(
        meta: HerokuStage1Targets.HerokuEntityMeta,
        s3Bucket: String,
        s3Key: String,
        reset: Boolean,
        maxRows: Int? = null,
    ): HerokuStage1CopyResult {
        log.info(
            "[heroku-stage1-copy] begin target={} s3=s3://{}/{} reset={} maxRows={}",
            meta.targetName, s3Bucket, s3Key, reset, maxRows ?: "unlimited",
        )
        val startedAt = System.currentTimeMillis()
        val mappedColumns = meta.columns.map { it.dbColumn } // staging 에 COPY 할 컬럼 (자연 키)
        val columnsList = mappedColumns.joinToString(", ") { quoteIdent(it) }
        val quotedTable = quoteIdent(meta.tableName)
        val fullyQualified = "${meta.schemaName}.$quotedTable"
        val stagingTable = "${meta.schemaName}._hc_copy_staging_${meta.tableName}"

        dataSource.connection.use { conn ->
            conn.autoCommit = false
            try {
                if (reset) {
                    conn.createStatement().use { st ->
                        st.executeUpdate("TRUNCATE TABLE $fullyQualified RESTART IDENTITY CASCADE")
                    }
                    log.info("[heroku-stage1-copy] reset: TRUNCATE {} (RESTART IDENTITY CASCADE)", fullyQualified)
                }

                // staging 은 자연 키 컬럼만 가진 단순 텍스트 테이블 — LIKE 로 본 테이블 구조 복제 후
                // 매핑 안 된 컬럼(FK/PK)은 INSERT 시 본 테이블 default/NULL 로 처리. SF 와 동일하게
                // staging 의 모든 NOT NULL 제약을 해제해 NULL 운반 허용.
                conn.createStatement().use { st ->
                    st.executeUpdate("DROP TABLE IF EXISTS $stagingTable")
                    st.executeUpdate(
                        "CREATE UNLOGGED TABLE $stagingTable " +
                            "(LIKE $fullyQualified INCLUDING DEFAULTS EXCLUDING CONSTRAINTS EXCLUDING INDEXES)"
                    )
                }
                dropStagingNotNull(conn, meta)

                val getReq = GetObjectRequest.builder().bucket(s3Bucket).key(s3Key).build()
                val copySql =
                    "COPY $stagingTable ($columnsList) FROM STDIN WITH (FORMAT csv, NULL '\\N')"
                val pgConn = conn.unwrap(PGConnection::class.java)
                val copyIn = pgConn.copyAPI.copyIn(copySql)

                var totalRows = 0
                try {
                    s3Client.getObject(getReq).use { s3Stream ->
                        InputStreamReader(s3Stream, Charsets.UTF_8).use { reader ->
                            CSVReaderBuilder(reader).build().use { csv ->
                                val header = csv.readNext()
                                    ?: error("CSV header missing: s3://$s3Bucket/$s3Key")
                                val headerIndex = header.withIndex().associate { (i, h) -> h.trim() to i }
                                while (true) {
                                    if (maxRows != null && totalRows >= maxRows) break
                                    val arr = csv.readNext() ?: break
                                    if (arr.all { it.isBlank() }) continue
                                    totalRows++
                                    progress.advanceProcessed()
                                    val values = ArrayList<String?>(meta.columns.size)
                                    for (col in meta.columns) {
                                        val idx = headerIndex[col.herokuColumn]
                                        val rawCell = if (idx != null && idx < arr.size) arr[idx] else null
                                        values.add(if (rawCell.isNullOrBlank()) null else rawCell)
                                    }
                                    val line = PgCsvHelper.line(values).toByteArray(Charsets.UTF_8)
                                    copyIn.writeToCopy(line, 0, line.size)
                                }
                            }
                        }
                    }
                    copyIn.endCopy()
                } catch (e: Throwable) {
                    try {
                        if (copyIn.isActive) copyIn.cancelCopy()
                    } catch (_: Throwable) {
                    }
                    throw e
                }

                val copyRowCount = copyIn.handledRowCount.toInt()

                val (inserted, unmatched) =
                    if (meta.requiresPkResolve) {
                        insertWithPkResolve(conn, meta, stagingTable, fullyQualified, mappedColumns, columnsList, reset)
                    } else {
                        val ins = insertPlain(conn, fullyQualified, stagingTable, columnsList, reset)
                        ins to 0
                    }

                conn.createStatement().use { st -> st.executeUpdate("DROP TABLE IF EXISTS $stagingTable") }
                conn.commit()

                val elapsed = System.currentTimeMillis() - startedAt
                log.info(
                    "[heroku-stage1-copy] done target={} inserted={} unmatched={} total={} elapsedMs={}",
                    meta.targetName, inserted, unmatched, totalRows, elapsed,
                )
                return HerokuStage1CopyResult(
                    targetName = meta.targetName,
                    inserted = inserted,
                    unmatched = unmatched,
                    totalRows = totalRows,
                    copyRowCount = copyRowCount,
                    elapsedMs = elapsed,
                )
            } catch (e: Throwable) {
                runCatching { conn.rollback() }
                throw e
            }
        }
    }

    /**
     * 일반 INSERT — staging 의 자연 키 컬럼만 본 테이블로 옮긴다. 매핑 안 된 FK/PK 컬럼은 NULL/serial.
     *
     * reset=true 면 TRUNCATE 후라 충돌 없음. reset=false 면 ON CONFLICT DO NOTHING 으로 최소 안전
     * (serial PK 라 자연 키 UNIQUE 가 있는 entity 만 실제 충돌 매칭. 없으면 누적되므로 reset 권장).
     */
    private fun insertPlain(
        conn: java.sql.Connection,
        fullyQualified: String,
        stagingTable: String,
        columnsList: String,
        reset: Boolean,
    ): Int {
        val conflict = if (reset) "" else " ON CONFLICT DO NOTHING"
        val sql =
            "INSERT INTO $fullyQualified ($columnsList) " +
                "SELECT $columnsList FROM $stagingTable$conflict"
        return conn.createStatement().use { st -> st.executeUpdate(sql) }
    }

    /**
     * EmployeeInfo 공유 PK INSERT — staging 의 employee_code 로 employee 를 JOIN 해 employee_id PK 를
     * 채운 후 INSERT (§6). employee 에 없는 employee_code (고아) 는 INSERT 제외 → unmatched 집계.
     *
     * @return (inserted, unmatched)
     */
    private fun insertWithPkResolve(
        conn: java.sql.Connection,
        meta: HerokuStage1Targets.HerokuEntityMeta,
        stagingTable: String,
        fullyQualified: String,
        mappedColumns: List<String>,
        columnsList: String,
        reset: Boolean,
    ): Pair<Int, Int> {
        require(mappedColumns.contains("employee_code")) {
            "${meta.targetName} requires employee_code column for shared-PK resolve"
        }
        // INNER JOIN employee → employee_id 채움. employee_code 컬럼은 staging(자연 키)에 그대로 잔류.
        val selectCols = mappedColumns.joinToString(", ") { "s.${quoteIdent(it)}" }
        val conflict = if (reset) "" else " ON CONFLICT DO NOTHING"
        val sql =
            "INSERT INTO $fullyQualified (employee_id, $columnsList) " +
                "SELECT e.employee_id, $selectCols " +
                "FROM $stagingTable s " +
                "JOIN ${meta.schemaName}.employee e ON e.employee_code = s.employee_code$conflict"
        val inserted = conn.createStatement().use { st -> st.executeUpdate(sql) }

        // 고아 (employee 에 employee_code 가 없어 INSERT 안 된 staging row) 수.
        val unmatchedSql =
            "SELECT COUNT(*) FROM $stagingTable s " +
                "WHERE s.employee_code IS NOT NULL " +
                "AND NOT EXISTS (SELECT 1 FROM ${meta.schemaName}.employee e WHERE e.employee_code = s.employee_code)"
        val unmatched = conn.createStatement().use { st ->
            st.executeQuery(unmatchedSql).use { rs -> if (rs.next()) rs.getInt(1) else 0 }
        }
        if (unmatched > 0) {
            log.warn(
                "[heroku-stage1-copy] {} : {} row employee_code 미매칭 (employee 미적재 고아) — INSERT 제외",
                meta.targetName, unmatched,
            )
        }
        return inserted to unmatched
    }

    /**
     * staging 테이블의 모든 NOT NULL 제약 해제 — COPY 입력이 NULL 인 미매핑 컬럼(FK/PK)도 통과시킨다.
     * 무결성 검증은 INSERT-SELECT 시 본 테이블의 제약이 담당.
     */
    private fun dropStagingNotNull(conn: java.sql.Connection, meta: HerokuStage1Targets.HerokuEntityMeta) {
        conn.prepareStatement(
            "SELECT column_name FROM information_schema.columns " +
                "WHERE table_schema = ? AND table_name = ? AND is_nullable = 'NO'"
        ).use { ps ->
            ps.setString(1, meta.schemaName)
            ps.setString(2, "_hc_copy_staging_${meta.tableName}")
            val notNullCols = mutableListOf<String>()
            ps.executeQuery().use { rs ->
                while (rs.next()) notNullCols.add(rs.getString(1))
            }
            if (notNullCols.isNotEmpty()) {
                conn.createStatement().use { st ->
                    val staging = "${meta.schemaName}._hc_copy_staging_${meta.tableName}"
                    for (col in notNullCols) {
                        st.executeUpdate("ALTER TABLE $staging ALTER COLUMN \"$col\" DROP NOT NULL")
                    }
                }
            }
        }
    }

    /** PG reserved keyword 대응 — Heroku 대상 테이블/컬럼에는 reserved 가 없으나 안전 차원. */
    private fun quoteIdent(name: String): String =
        if (name in PG_RESERVED) "\"$name\"" else name

    companion object {
        private val PG_RESERVED = setOf("user", "group", "order", "desc")
    }
}

data class HerokuStage1CopyResult(
    val targetName: String,
    val inserted: Int,
    val unmatched: Int,
    val totalRows: Int,
    val copyRowCount: Int,
    val elapsedMs: Long,
)

data class HerokuStage1BatchSummary(
    val totalTargets: Int,
    val success: Int,
    val failed: Int,
    val skipped: Int,
    val totalInserted: Long,
)
