#!/usr/bin/env kotlin

/**
 * 공지 본문 RTA 인라인 이미지 마이그레이션 — 본문 HTML 의 rtaImage <img> → placeholder 치환 (DB UPDATE).
 *
 * 배경:
 *   공지(notice.contents) 본문 HTML 에 박힌 SF rtaImage 서블릿 URL 은 SF org 세션 인증에 묶여 신규
 *   시스템에서 깨진다. 공지 이미지는 private S3 저장 + presigned URL 로만 조회되는데(권한 통제),
 *   presigned 는 만료되므로 본문에 완성 URL 을 박을 수 없다. 따라서 본문에는 만료 없는 placeholder 만
 *   영구 저장하고, 조회 시점에 backend(NoticeService.getNoticeDetail)가 presigned URL 로 rewrite 한다.
 *
 *   본 스크립트는 본문의 rtaImage `<img ...>` 태그 전체를 다음 placeholder 로 치환한다:
 *       <img src="notice-image://{refid}" data-refid="{refid}" alt="{alt}">
 *   - data-refid = backend rewrite lookup 키 (= upload_file.sfid) + mobile cacheKey (권위 식별자)
 *   - src="notice-image://{refid}" = 커스텀 스킴 placeholder (http 아님 → rewrite 누락 시에도 잘못된
 *     GET 안 나가고 깨진 아이콘만, DB 본문이 만료 URL 로 오염되는 것을 구조적으로 차단)
 *
 *   매칭 키는 scan CSV 의 source_tag (본문에 박힌 `<img ...>` 태그 전체 원본 문자열). 본문에서 그 태그를
 *   찾아 placeholder 로 통째 교체한다 (src 만 바꾸면 data-refid 속성을 추가할 수 없으므로 태그 전체 교체).
 *
 * 멱등성:
 *   - 본문에 이미 data-refid="{refid}" 가 있으면 (이미 치환됨) 해당 이미지 skip.
 *   - 본문에 source_tag 가 더 이상 없으면 변경 없음.
 *   - 2회 실행해도 동일 결과.
 *
 * 안전장치:
 *   - 기본 dry-run (변경 대상만 출력). 실제 UPDATE 는 --apply 명시 시에만.
 *   - notice 별 변경 전/후 rtaImage 잔존 개수를 리포트.
 *
 * 입력:
 *   --scan-csv : scan 산출 CSV (notice_sfid, refid, eid, source_tag, source_url, alt_name)
 *   [--apply]  : 실제 UPDATE 수행 (없으면 dry-run)
 *   (placeholder 치환은 refid 만 있으면 되므로 upload-csv / public-url-prefix / image-prefix 불요.)
 *
 * DB 연결: db.properties (loadDbConfig) 재사용. notice 본문은 운영(prod) DB 가 권위 출처이므로
 *   reset-dev 같은 prod 거부 가드는 두지 않되, 기본 dry-run 으로 오적용을 방지한다.
 *
 * 사용법:
 *   kotlinc -script replace-notice-rta-urls.main.kts -- \
 *       --scan-csv input/notice-images/notice-rta-scan.csv [--apply]
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
var apply = false

run {
    val it = args.iterator()
    while (it.hasNext()) {
        when (val a = it.next()) {
            "--scan-csv" -> scanCsvPath = it.next()
            "--apply" -> apply = true
            "-h", "--help" -> {
                println("Usage: kotlinc -script replace-notice-rta-urls.main.kts -- --scan-csv <in> [--apply]")
                kotlin.system.exitProcess(0)
            }
            else -> { System.err.println("Unknown arg: $a"); kotlin.system.exitProcess(1) }
        }
    }
}

val scanCsv = scanCsvPath ?: run { System.err.println("[error] --scan-csv 필수"); kotlin.system.exitProcess(1) }

// HTML attribute 값 이스케이프 (alt 안의 " / & 안전 처리).
fun attr(s: String): String = s.replace("&", "&amp;").replace("\"", "&quot;")

// placeholder <img> 생성 — backend NoticeService.rewriteInlineImages 가 data-refid 로 lookup.
fun placeholder(refid: String, alt: String): String =
    "<img src=\"notice-image://$refid\" data-refid=\"${attr(refid)}\"" +
        (if (alt.isNotEmpty()) " alt=\"${attr(alt)}\"" else "") + ">"

// -----------------------------------------------------------------------------
// scan CSV → notice_sfid 별 (source_tag → placeholder) 치환 목록
// -----------------------------------------------------------------------------
// 한 notice 본문에 여러 <img> 가능 → notice_sfid 별로 (원본 태그, refid, placeholder) 목록.
data class Replacement(val sourceTag: String, val refid: String, val placeholder: String)

val replacementsByNotice = HashMap<String, MutableList<Replacement>>()
var scanRows = 0
CSVReader(FileReader(File(scanCsv))).use { reader ->
    val header = reader.readNext() ?: run { System.err.println("[error] scan CSV 비어 있음"); kotlin.system.exitProcess(1) }
    val idx = header.withIndex().associate { (i, n) -> n.trim() to i }
    val nIdx = idx["notice_sfid"] ?: error("scan CSV 에 notice_sfid 컬럼 없음")
    val rIdx = idx["refid"] ?: error("scan CSV 에 refid 컬럼 없음")
    val tIdx = idx["source_tag"] ?: error("scan CSV 에 source_tag 컬럼 없음 (migrate-notice-rta-images.sh 재실행 필요)")
    val aIdx = idx["alt_name"] ?: -1
    var row = reader.readNext()
    while (row != null) {
        scanRows++
        val notice = row.getOrNull(nIdx)?.trim().orEmpty()
        val refid = row.getOrNull(rIdx)?.trim().orEmpty()
        val tag = row.getOrNull(tIdx) ?: ""   // <img> 태그 전체 (원본 — trim 안 함, 그대로 매칭)
        val alt = if (aIdx >= 0) row.getOrNull(aIdx).orEmpty() else ""
        if (notice.isEmpty() || refid.isEmpty() || tag.isEmpty()) { row = reader.readNext(); continue }
        replacementsByNotice.getOrPut(notice) { mutableListOf() }
            .add(Replacement(tag, refid, placeholder(refid, alt)))
        row = reader.readNext()
    }
}

println("=".repeat(60))
println("공지 본문 rtaImage <img> → placeholder 치환 ${if (apply) "(APPLY)" else "(DRY-RUN)"}")
println("=".repeat(60))
println("scan rows           : $scanRows")
println("치환 대상 공지 수    : ${replacementsByNotice.size}")
println("치환 대상 이미지 수  : ${replacementsByNotice.values.sumOf { it.size }}")
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

    for ((noticeSfid, repls) in replacementsByNotice) {
        selectStmt.setString(1, noticeSfid)
        val rs = selectStmt.executeQuery()
        if (!rs.next()) {
            // sfid 로 notice 미발견 (신규 DB 미적재 / 삭제 등) — skip.
            notFound++; rs.close(); continue
        }
        val original = rs.getString("contents") ?: ""
        rs.close()

        var replaced = original
        for (r in repls) {
            // 멱등: 이미 placeholder(data-refid) 가 박혀 있으면 해당 이미지 skip.
            if (replaced.contains("data-refid=\"${r.refid}\"")) continue
            if (replaced.contains(r.sourceTag)) replaced = replaced.replace(r.sourceTag, r.placeholder)
        }

        if (replaced == original) {
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
            println("[dry-run] notice sfid=$noticeSfid : ${repls.size}개 placeholder 치환" +
                (if (remaining > 0) "  ⚠️ 치환 후 rtaImage $remaining 개 잔존" else ""))
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
println("  치환 후 rtaImage 잔존 notice : $rtaRemaining  (원본 태그 미발견 등 — 확인 필요)")
println("=".repeat(60))
