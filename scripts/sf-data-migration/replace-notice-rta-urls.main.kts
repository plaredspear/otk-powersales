#!/usr/bin/env kotlin

/**
 * 공지 본문 RTA 인라인 이미지 마이그레이션 — 본문 HTML 의 rtaImage URL → 신규 public URL 치환 (DB UPDATE).
 *
 * 배경:
 *   공지(notice.contents) 본문 HTML 에 박힌 SF rtaImage 서블릿 URL 은 SF org 세션 인증에 묶여 신규
 *   시스템에서 깨진다. migrate-notice-rta-images.sh 가 이미지를 S3 로 옮기고 upload_file 에 적재한 뒤,
 *   본 스크립트가 본문의 rtaImage <img src> 를 신규 public URL 로 영구 치환한다 (Q1 결정 — DB UPDATE).
 *
 *   매칭 키는 scan CSV 의 source_url (본문에 박힌 원본 문자열, &amp; 인코딩 형태 그대로). 본문에서 그
 *   문자열을 신규 URL(= {public-url-prefix}/{image-prefix}/{refid}.{ext}) 로 치환한다. ext 는 적재
 *   CSV(notice_image_upload_files.csv)의 UniqueKey__c 에서 가져온다 (= 실제 다운로드 확장자).
 *
 * 멱등성:
 *   - 본문에 source_url 이 더 이상 없으면 (이미 치환됨) 해당 row 변경 없음.
 *   - 치환 후 rtaImage 가 남지 않은 notice 만 UPDATE 대상 카운트.
 *   - 2회 실행해도 동일 결과 (이미 신규 URL 인 본문은 source_url 미발견 → skip).
 *
 * 안전장치:
 *   - 기본 dry-run (변경 대상만 출력). 실제 UPDATE 는 --apply 명시 시에만.
 *   - notice_id 별 변경 전/후 rtaImage 잔존 개수를 리포트.
 *
 * 입력:
 *   --scan-csv          : scan 산출 CSV (notice_sfid, refid, eid, source_url, alt_name)
 *   --upload-csv        : build 산출 CSV (notice_image_upload_files.csv — refid 별 UniqueKey__c/ext 결정)
 *   --public-url-prefix : S3_PUBLIC_URL_PREFIX (예: https://<bucket>.s3.ap-northeast-2.amazonaws.com/public/)
 *   --image-prefix      : unique_key prefix (기본 uploads/notice/migrated — upload-csv 의 UniqueKey__c 와 정합)
 *   [--apply]           : 실제 UPDATE 수행 (없으면 dry-run)
 *
 * DB 연결: db.properties (loadDbConfig) 재사용. notice 본문은 운영(prod) DB 가 권위 출처이므로
 *   reset-dev 같은 prod 거부 가드는 두지 않되, 기본 dry-run 으로 오적용을 방지한다.
 *
 * 사용법:
 *   kotlinc -script replace-notice-rta-urls.main.kts -- \
 *       --scan-csv input/notice-images/notice-rta-scan.csv \
 *       --upload-csv input/notice-images/notice_image_upload_files.csv \
 *       --public-url-prefix https://<bucket>.s3.../public/ \
 *       [--image-prefix uploads/notice/migrated] [--apply]
 */

@file:DependsOn("com.opencsv:opencsv:5.9")
@file:DependsOn("org.postgresql:postgresql:42.7.4")
@file:Import("common.kts")

import com.opencsv.CSVReader
import java.io.File
import java.io.FileReader

// -----------------------------------------------------------------------------
// 인자 파싱
// -----------------------------------------------------------------------------

var scanCsvPath: String? = null
var uploadCsvPath: String? = null
var publicUrlPrefix: String? = null
var imagePrefix = "uploads/notice/migrated"
var apply = false

run {
    val it = args.iterator()
    while (it.hasNext()) {
        when (val a = it.next()) {
            "--scan-csv" -> scanCsvPath = it.next()
            "--upload-csv" -> uploadCsvPath = it.next()
            "--public-url-prefix" -> publicUrlPrefix = it.next()
            "--image-prefix" -> imagePrefix = it.next().trimEnd('/')
            "--apply" -> apply = true
            "-h", "--help" -> {
                println("Usage: kotlinc -script replace-notice-rta-urls.main.kts -- --scan-csv <in> --upload-csv <in> --public-url-prefix <url> [--image-prefix uploads/notice/migrated] [--apply]")
                kotlin.system.exitProcess(0)
            }
            else -> { System.err.println("Unknown arg: $a"); kotlin.system.exitProcess(1) }
        }
    }
}

val scanCsv = scanCsvPath ?: run { System.err.println("[error] --scan-csv 필수"); kotlin.system.exitProcess(1) }
val uploadCsv = uploadCsvPath ?: run { System.err.println("[error] --upload-csv 필수"); kotlin.system.exitProcess(1) }
val urlPrefix = publicUrlPrefix?.trimEnd('/')
    ?: run { System.err.println("[error] --public-url-prefix 필수"); kotlin.system.exitProcess(1) }

// -----------------------------------------------------------------------------
// upload CSV 에서 refid → unique_key 매핑 로드 (= 신규 URL 의 path 부분)
// -----------------------------------------------------------------------------
// notice_image_upload_files.csv: Id(=refid), UniqueKey__c(= {image-prefix}/{refid}.{ext})
val uniqueKeyByRefid = HashMap<String, String>()
CSVReader(FileReader(File(uploadCsv))).use { reader ->
    val header = reader.readNext() ?: run { System.err.println("[error] upload CSV 비어 있음"); kotlin.system.exitProcess(1) }
    val idx = header.withIndex().associate { (i, n) -> n.trim() to i }
    val idIdx = idx["Id"] ?: error("upload CSV 에 Id 컬럼 없음")
    val keyIdx = idx["UniqueKey__c"] ?: error("upload CSV 에 UniqueKey__c 컬럼 없음")
    var row = reader.readNext()
    while (row != null) {
        val refid = row.getOrNull(idIdx)?.trim().orEmpty()
        val key = row.getOrNull(keyIdx)?.trim().orEmpty()
        if (refid.isNotEmpty() && key.isNotEmpty()) uniqueKeyByRefid[refid] = key
        row = reader.readNext()
    }
}

// -----------------------------------------------------------------------------
// scan CSV → notice_sfid 별 (source_url → 신규 public URL) 치환 맵
// -----------------------------------------------------------------------------
// 한 notice 본문에 여러 <img> 가능 → notice_sfid 별로 (원본 문자열, 신규 URL) 목록.
val replacementsByNotice = HashMap<String, MutableList<Pair<String, String>>>()
var scanRows = 0
var noUploadKey = 0
CSVReader(FileReader(File(scanCsv))).use { reader ->
    val header = reader.readNext() ?: run { System.err.println("[error] scan CSV 비어 있음"); kotlin.system.exitProcess(1) }
    val idx = header.withIndex().associate { (i, n) -> n.trim() to i }
    val nIdx = idx["notice_sfid"] ?: error("scan CSV 에 notice_sfid 컬럼 없음")
    val rIdx = idx["refid"] ?: error("scan CSV 에 refid 컬럼 없음")
    val uIdx = idx["source_url"] ?: error("scan CSV 에 source_url 컬럼 없음")
    var row = reader.readNext()
    while (row != null) {
        scanRows++
        val notice = row.getOrNull(nIdx)?.trim().orEmpty()
        val refid = row.getOrNull(rIdx)?.trim().orEmpty()
        val src = row.getOrNull(uIdx) ?: ""   // 본문 원본 문자열 (&amp; 형태 — trim 안 함, 그대로 매칭)
        if (notice.isEmpty() || refid.isEmpty() || src.isEmpty()) { row = reader.readNext(); continue }
        val uniqueKey = uniqueKeyByRefid[refid]
        if (uniqueKey == null) {
            // 다운로드/적재 안 된 refid (실패분) — 치환 대상 제외 (본문에 원본 URL 유지).
            noUploadKey++; row = reader.readNext(); continue
        }
        val newUrl = "$urlPrefix/$uniqueKey"
        replacementsByNotice.getOrPut(notice) { mutableListOf() }.add(src to newUrl)
        row = reader.readNext()
    }
}

println("=".repeat(60))
println("공지 본문 rtaImage URL 치환 ${if (apply) "(APPLY)" else "(DRY-RUN)"}")
println("=".repeat(60))
println("scan rows           : $scanRows")
println("치환 대상 공지 수    : ${replacementsByNotice.size}")
println("치환 대상 이미지 수  : ${replacementsByNotice.values.sumOf { it.size }}")
println("적재 키 없음(skip)   : $noUploadKey  (다운로드 실패분 — 본문 원본 유지)")
println("public url prefix    : $urlPrefix")
println("image prefix         : $imagePrefix")
println()

if (replacementsByNotice.isEmpty()) {
    println("[done] 치환 대상 없음.")
    kotlin.system.exitProcess(0)
}

// -----------------------------------------------------------------------------
// DB 연결 + 치환
// -----------------------------------------------------------------------------

val scriptDir = File(System.getProperty("user.dir"))
val dbConfig = loadDbConfig(scriptDir)
println("jdbc url : ${dbConfig.jdbcUrl}")
println("jdbc user: ${dbConfig.user}")
println()

val conn = openConnection(dbConfig)
var updated = 0
var unchanged = 0
var notFound = 0
var rtaRemaining = 0
try {
    val selectStmt = conn.prepareStatement(
        "SELECT contents FROM ${dbConfig.schema}.notice WHERE sfid = ?"
    )
    val updateStmt = conn.prepareStatement(
        "UPDATE ${dbConfig.schema}.notice SET contents = ? WHERE sfid = ?"
    )

    for ((noticeSfid, pairs) in replacementsByNotice) {
        selectStmt.setString(1, noticeSfid)
        val rs = selectStmt.executeQuery()
        if (!rs.next()) {
            // sfid 로 notice 미발견 (신규 DB 미적재 / 삭제 등) — skip.
            notFound++; rs.close(); continue
        }
        val original = rs.getString("contents") ?: ""
        rs.close()

        var replaced = original
        for ((src, newUrl) in pairs) {
            if (replaced.contains(src)) replaced = replaced.replace(src, newUrl)
        }

        if (replaced == original) {
            // 본문에 source_url 이 없음 (이미 치환됨 / 본문 변경됨) — 멱등 skip.
            unchanged++
            continue
        }

        val remaining = Regex("rtaImage").findAll(replaced).count()
        if (remaining > 0) rtaRemaining++

        if (apply) {
            updateStmt.setString(1, replaced)
            updateStmt.setString(2, noticeSfid)
            updateStmt.executeUpdate()
        }
        updated++
        if (!apply && updated <= 10) {
            println("[dry-run] notice sfid=$noticeSfid : ${pairs.size}개 치환" +
                (if (remaining > 0) "  ⚠️ 치환 후 rtaImage $remaining 개 잔존(미적재 refid)" else ""))
        }
    }

    if (apply) {
        conn.commit()
        println("\n[applied] commit 완료.")
    } else {
        conn.rollback()
        println("\n[dry-run] 변경 없음 (--apply 로 실제 UPDATE).")
    }
} catch (e: Exception) {
    conn.rollback()
    System.err.println("[error] 치환 실패 — rollback: ${e.message}")
    throw e
} finally {
    conn.close()
}

println("=".repeat(60))
println("결과:")
println("  변경(예정) notice   : $updated")
println("  변경 없음(멱등 skip) : $unchanged")
println("  notice 미발견(skip)  : $notFound")
println("  치환 후 rtaImage 잔존 notice : $rtaRemaining  (미적재 refid 가 본문에 남은 경우)")
println("=".repeat(60))
