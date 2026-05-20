package com.otoki.powersales.sfmigration.stage1

import com.opencsv.CSVReaderBuilder
import org.postgresql.PGConnection
import org.slf4j.LoggerFactory
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
 * POC 한계:
 *  - 단일 entity 단위 동기 호출 (jobId / async / 진행 SSE 등 정식 기능 없음).
 *  - ON CONFLICT DO NOTHING 으로 멱등성 보장. reset 모드 없음 (DB 가 비어있는 첫 적재 가정).
 *  - 적재 실패 시 트랜잭션 전체 rollback (chunk commit 없음). 정식 구축 시 chunk 도입 검토.
 */
@Service
class Stage1S3CopyService(
    private val dataSource: DataSource,
    private val s3Client: S3Client,
    private val progress: Stage1CopyProgress,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * S3 의 CSV 1개를 적재.
     *
     * @param targetName Stage1Targets.list() 중 하나 (예: "ErpOrderProduct")
     * @param s3Bucket  CSV 가 있는 S3 bucket
     * @param s3Key     CSV 의 S3 key (예: "sf-migration/input/erp_order_products.csv")
     * @return Stage1CopyResult — inserted / totalRows / filteredOut / elapsedMs
     */
    fun copyFromS3(targetName: String, s3Bucket: String, s3Key: String): Stage1CopyResult {
        val meta = Stage1Targets.get(targetName)
            ?: run {
                val msg = "Unknown target: $targetName (allowed: ${Stage1Targets.list().joinToString(", ")})"
                progress.finishWithFailure(msg)
                error(msg)
            }

        log.info("[stage1-copy] begin target={} s3=s3://{}/{}", targetName, s3Bucket, s3Key)
        // controller 가 이미 begin() 한 RUNNING 상태로 진입한 경우가 일반 경로. 다른 진입점
        // (테스트 / 동기 호출 등) 에서 IDLE 상태로 호출되었다면 여기서 begin() 보강.
        if (progress.status != Stage1CopyProgress.Status.RUNNING) {
            progress.begin(targetName, s3Bucket, s3Key)
        }
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
                // UNLOGGED staging 생성 (WAL 우회 + INSERT-SELECT ON CONFLICT 패턴).
                conn.createStatement().use { st ->
                    st.executeUpdate("DROP TABLE IF EXISTS $stagingTable")
                    st.executeUpdate(
                        "CREATE UNLOGGED TABLE $stagingTable " +
                            "(LIKE $fullyQualified INCLUDING DEFAULTS EXCLUDING CONSTRAINTS EXCLUDING INDEXES)"
                    )
                }

                // S3 stream → CSV reader → COPY writer.
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
                                    // NOT NULL pre-filter.
                                    val ok = requiredFields.all { field ->
                                        val idx = headerIndex[field.sfFieldName] ?: return@all false
                                        idx < arr.size && arr[idx].isNotBlank()
                                    }
                                    if (!ok) {
                                        filteredOut++
                                        progress.advanceFiltered()
                                        continue
                                    }
                                    // 컬럼 순서대로 값 구성 + placeholder 적용.
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

                // staging → 타겟 INSERT-SELECT ON CONFLICT DO NOTHING (멱등성).
                val insSql =
                    "INSERT INTO $fullyQualified ($columnsList) " +
                        "SELECT $columnsList FROM $stagingTable " +
                        "ON CONFLICT DO NOTHING"
                val inserted = conn.createStatement().use { st -> st.executeUpdate(insSql) }
                conn.createStatement().use { st -> st.executeUpdate("DROP TABLE IF EXISTS $stagingTable") }

                conn.commit()
                progress.setInserted(inserted.toLong())
                progress.finishOk()
                val elapsed = System.currentTimeMillis() - startedAt
                log.info(
                    "[stage1-copy] done target={} inserted={} total={} filtered={} elapsedMs={}",
                    targetName, inserted, totalRows, filteredOut, elapsed,
                )
                return Stage1CopyResult(
                    targetName = targetName,
                    inserted = inserted,
                    totalRows = totalRows,
                    filteredOut = filteredOut,
                    copyRowCount = copyRowCount,
                    elapsedMs = elapsed,
                )
            } catch (e: Throwable) {
                runCatching { conn.rollback() }
                val msg = "${e.javaClass.simpleName}: ${e.message ?: "unknown"}"
                progress.finishWithFailure(msg)
                log.error("[stage1-copy] FAILED target={} reason={}", targetName, msg, e)
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
