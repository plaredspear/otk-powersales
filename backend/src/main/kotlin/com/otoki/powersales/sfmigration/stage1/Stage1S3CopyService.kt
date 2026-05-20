package com.otoki.powersales.sfmigration.stage1

import com.opencsv.CSVReaderBuilder
import org.postgresql.PGConnection
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import java.io.InputStreamReader
import javax.sql.DataSource

/**
 * SF migration Stage 1 — S3 의 SF export CSV 를 PostgreSQL COPY FROM STDIN 으로 직접 적재.
 *
 * 로컬 → SSM 터널 → RDS 경로의 broken pipe / 네트워크 병목을 회피하기 위해 backend 의
 * 같은 VPC RDS 직결 connection 을 활용. S3 stream → OpenCSV row 파싱 → COPY stream 직결
 * 패턴으로 in-memory 적재 없이 row 단위 흐름 유지 (RAM 일정).
 *
 * 권한: SYSTEM_ADMIN 만 호출 가능 (Controller 에서 enforce).
 *
 * 두 가지 진입점:
 *  - [copyFromS3] (단건): target 1개를 동기 실행. 단건 호출 시 [Stage1CopyProgress] 의
 *    SINGLE 모드를 가정. controller 가 begin() 후 호출하는 일반 경로.
 *  - [copyAllFromS3] (일괄): entity 의존성 순서대로 순차 실행. 1개 실패 시 즉시 중단 +
 *    나머지 PENDING entity 는 SKIPPED 로 마크. 각 entity 결과는 [Stage1CopyProgress.entityResults]
 *    에 누적되어 UI 가 실패 사유를 확인 가능.
 *
 * 적재 패턴:
 *  - UNLOGGED staging 생성 (WAL 우회) → S3 stream → CSV → COPY → INSERT-SELECT ON CONFLICT
 *    DO NOTHING → DROP. 멱등성 보장 (재실행 안전).
 *  - 적재 실패 시 트랜잭션 전체 rollback (chunk commit 없음).
 */
// S3Client 빈은 spring-cloud-aws S3AutoConfiguration 이 제공하며 local 프로파일에서는 exclude
// 된다 (application-local.yml). 그에 맞춰 본 서비스도 dev/prod 에서만 활성화하여 local 부팅 시
// S3Client 빈 미등록으로 인한 의존 해소 실패를 회피한다. SF migration Stage1 자체가 운영 도구라
// local 시나리오 없음.
@Service
@Profile("dev | prod")
class Stage1S3CopyService(
    private val dataSource: DataSource,
    private val s3Client: S3Client,
    private val progress: Stage1CopyProgress,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * S3 의 CSV 1개를 적재 (SINGLE 모드).
     *
     * @param targetName Stage1Targets.list() 중 하나 (예: "ErpOrderProduct")
     * @param s3Bucket   CSV 가 있는 S3 bucket
     * @param s3Key      CSV 의 S3 key (예: "sf-migration/input/erp_order_products.csv")
     * @return Stage1CopyResult — inserted / totalRows / filteredOut / elapsedMs
     */
    fun copyFromS3(targetName: String, s3Bucket: String, s3Key: String): Stage1CopyResult {
        val meta = Stage1Targets.get(targetName)
            ?: run {
                val msg = "Unknown target: $targetName (allowed: ${Stage1Targets.list().joinToString(", ")})"
                progress.finishWithFailure(msg)
                error(msg)
            }

        // controller 가 이미 begin() 한 RUNNING 상태로 진입한 경우가 일반 경로. 다른 진입점
        // (테스트 / 동기 호출 등) 에서 IDLE 상태로 호출되었다면 여기서 begin() 보강.
        if (progress.status != Stage1CopyProgress.Status.RUNNING) {
            progress.begin(targetName, s3Bucket, s3Key)
        }
        try {
            val result = executeEntity(meta, s3Bucket, s3Key)
            progress.setInserted(result.inserted.toLong())
            progress.finishOk()
            return result
        } catch (e: Throwable) {
            val msg = "${e.javaClass.simpleName}: ${e.message ?: "unknown"}"
            progress.finishWithFailure(msg)
            log.error("[stage1-copy] FAILED target={} reason={}", targetName, msg, e)
            throw e
        }
    }

    /**
     * S3 의 prefix 아래 모든 entity 의 CSV 를 의존성 순서대로 순차 적재 (BATCH 모드).
     *
     * S3 key 는 `<s3KeyPrefix.removeSuffix("/")>/<EntityMetadata.csvFileName>` 으로 자동 조립.
     *
     * 1개 entity 실패 시 즉시 중단 (나머지 entity 는 SKIPPED). entity 별 적재 결과는
     * [Stage1CopyProgress.entityResults] 에 누적되어 UI 가 성공/실패/실패 사유 확인.
     *
     * @param s3Bucket    CSV 보관 bucket
     * @param s3KeyPrefix CSV 들의 공통 prefix (예: "sf-migration/input")
     * @return Stage1BatchSummary — 처리된 entity 수 / 성공 / 실패 / 누적 inserted
     */
    fun copyAllFromS3(s3Bucket: String, s3KeyPrefix: String): Stage1BatchSummary {
        val targets = Stage1Targets.list()
        val prefix = s3KeyPrefix.removeSuffix("/")

        log.info(
            "[stage1-copy-all] begin s3=s3://{}/{} targets={}",
            s3Bucket, prefix, targets.size,
        )

        // controller 가 이미 beginBatch() 한 RUNNING 상태로 진입한 경우가 일반 경로.
        if (progress.status != Stage1CopyProgress.Status.RUNNING) {
            progress.beginBatch(s3Bucket, targets)
        }

        var success = 0
        var failed = 0
        var totalInserted = 0L

        for (targetName in targets) {
            val meta = Stage1Targets.get(targetName) ?: continue
            val s3Key = "$prefix/${meta.csvFileName}"
            progress.beginEntity(targetName, s3Key)

            val processedBefore = progress.processedRows
            val filteredBefore = progress.filteredOut

            try {
                val result = executeEntity(meta, s3Bucket, s3Key)
                val processedDelta = progress.processedRows - processedBefore
                val filteredDelta = progress.filteredOut - filteredBefore
                progress.addInserted(result.inserted.toLong())
                progress.finishEntityOk(
                    targetName = targetName,
                    processed = processedDelta,
                    filteredOut = filteredDelta,
                    inserted = result.inserted.toLong(),
                )
                totalInserted += result.inserted
                success++
                log.info(
                    "[stage1-copy-all] entity OK target={} inserted={} processed={} filtered={}",
                    targetName, result.inserted, processedDelta, filteredDelta,
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
                    "[stage1-copy-all] entity FAILED target={} reason={} — batch abort",
                    targetName, msg, e,
                )
                return Stage1BatchSummary(
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
            "[stage1-copy-all] done totalTargets={} success={} totalInserted={}",
            targets.size, success, totalInserted,
        )
        return Stage1BatchSummary(
            totalTargets = targets.size,
            success = success,
            failed = 0,
            skipped = 0,
            totalInserted = totalInserted,
        )
    }

    /**
     * Entity 1개의 적재만 담당 — progress 의 entity 단위 시작/종료 호출은 호출자 책임.
     * processed/filtered 누적은 본 함수 내부에서 갱신, inserted 는 반환값 통해 호출자가 누적.
     *
     * @throws Exception 적재 실패 시 그대로 throw (호출자가 catch 해서 entity 결과/전체 상태 갱신).
     */
    private fun executeEntity(
        meta: EntityMetadata,
        s3Bucket: String,
        s3Key: String,
    ): Stage1CopyResult {
        log.info("[stage1-copy] begin target={} s3=s3://{}/{}", meta.targetName, s3Bucket, s3Key)
        val startedAt = System.currentTimeMillis()
        val allColumns = meta.fields.map { it.dbColumnName } + meta.extraStaticColumns.keys.toList()
        val columnsList = allColumns.joinToString(", ")
        val quotedTable = quoteIdent(meta.tableName)
        val fullyQualified = "${meta.schemaName}.$quotedTable"
        val stagingTable = "${meta.schemaName}._copy_staging_${meta.tableName}"
        val requiredFields = meta.fields.filter { !it.nullable }
        val extraValues = meta.extraStaticColumns.values.toList()

        // PG connection 획득 — autoCommit=false 로 trx 명시 관리.
        dataSource.connection.use { conn ->
            conn.autoCommit = false
            try {
                conn.createStatement().use { st ->
                    st.executeUpdate("DROP TABLE IF EXISTS $stagingTable")
                    st.executeUpdate(
                        "CREATE UNLOGGED TABLE $stagingTable " +
                            "(LIKE $fullyQualified INCLUDING DEFAULTS EXCLUDING CONSTRAINTS EXCLUDING INDEXES)"
                    )
                }

                val getReq = GetObjectRequest.builder().bucket(s3Bucket).key(s3Key).build()
                val copySql =
                    "COPY $stagingTable ($columnsList) FROM STDIN WITH (FORMAT csv, NULL '\\N')"
                val pgConn = conn.unwrap(PGConnection::class.java)
                val copyIn = pgConn.copyAPI.copyIn(copySql)

                var totalRows = 0
                var filteredOut = 0
                try {
                    s3Client.getObject(getReq).use { s3Stream ->
                        InputStreamReader(s3Stream, Charsets.UTF_8).use { reader ->
                            CSVReaderBuilder(reader).build().use { csv ->
                                val header = csv.readNext()
                                    ?: error("CSV header missing: s3://$s3Bucket/$s3Key")
                                val headerIndex = header.withIndex().associate { (i, h) -> h.trim() to i }
                                while (true) {
                                    val arr = csv.readNext() ?: break
                                    if (arr.all { it.isBlank() }) continue
                                    totalRows++
                                    progress.advanceProcessed()
                                    val ok = requiredFields.all { field ->
                                        val idx = headerIndex[field.sfFieldName] ?: return@all false
                                        idx < arr.size && arr[idx].isNotBlank()
                                    }
                                    if (!ok) {
                                        filteredOut++
                                        progress.advanceFiltered()
                                        continue
                                    }
                                    val values = ArrayList<String?>(meta.fields.size + extraValues.size)
                                    for (f in meta.fields) {
                                        val idx = headerIndex[f.sfFieldName]
                                        val raw = if (idx != null && idx < arr.size) arr[idx] else null
                                        val effective = when {
                                            raw.isNullOrBlank() && f.nullPlaceholder != null -> f.nullPlaceholder
                                            raw.isNullOrBlank() -> null
                                            else -> raw
                                        }
                                        values.add(effective)
                                    }
                                    for (v in extraValues) values.add(v)
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

                val insSql =
                    "INSERT INTO $fullyQualified ($columnsList) " +
                        "SELECT $columnsList FROM $stagingTable " +
                        "ON CONFLICT DO NOTHING"
                val inserted = conn.createStatement().use { st -> st.executeUpdate(insSql) }
                conn.createStatement().use { st -> st.executeUpdate("DROP TABLE IF EXISTS $stagingTable") }

                conn.commit()
                val elapsed = System.currentTimeMillis() - startedAt
                log.info(
                    "[stage1-copy] done target={} inserted={} total={} filtered={} elapsedMs={}",
                    meta.targetName, inserted, totalRows, filteredOut, elapsed,
                )
                return Stage1CopyResult(
                    targetName = meta.targetName,
                    inserted = inserted,
                    totalRows = totalRows,
                    filteredOut = filteredOut,
                    copyRowCount = copyRowCount,
                    elapsedMs = elapsed,
                )
            } catch (e: Throwable) {
                runCatching { conn.rollback() }
                throw e
            }
        }
    }

    private fun quoteIdent(name: String): String =
        if (name == "user") "\"user\"" else name
}

data class Stage1CopyResult(
    val targetName: String,
    val inserted: Int,
    val totalRows: Int,
    val filteredOut: Int,
    val copyRowCount: Int,
    val elapsedMs: Long,
)

data class Stage1BatchSummary(
    val totalTargets: Int,
    val success: Int,
    val failed: Int,
    val skipped: Int,
    val totalInserted: Long,
)
