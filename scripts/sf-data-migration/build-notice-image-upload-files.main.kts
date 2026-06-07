#!/usr/bin/env kotlin

/**
 * 공지 본문 RTA 인라인 이미지 마이그레이션 — scan CSV + 다운로드 결과 → notice_image_upload_files.csv 변환기.
 *
 * 배경:
 *   공지(DKRetail__Notice__c) 본문 인라인 이미지는 SF rich text area 의 blob 으로 본문 HTML 에
 *   rtaImage 서블릿 URL(refid=0EM...)로 박힌다. ContentDocumentLink / ContentVersion / UploadFile__c
 *   어디에도 행이 없어 SOQL 추출 불가하고, rtaImage 서블릿 GET 으로만 받는다. migrate-notice-rta-images.sh
 *   의 scan 단계가 본문을 파싱해 (notice_sfid, refid, eid, source_url, alt_name) 튜플 CSV 를 만들고,
 *   download 단계가 refid 별 이미지를 받아 {refid}.{ext} 로 저장한다. 본 스크립트는 그 둘을 합쳐
 *   Stage1 적재용 CSV 로 변환한다 (AWS 접근 없음 — 오프라인 단계).
 *
 * 입력:
 *   --scan-csv : scan 산출 CSV. 헤더: notice_sfid, refid, eid, source_url, alt_name
 *   --img-dir  : 다운로드된 이미지 폴더 ({refid}.{ext} 파일들). 확장자는 실제 다운로드 파일에서 결정.
 *
 * 출력 (notice_image_upload_files.csv — Stage1Targets.NoticeImageUploadFile = UPLOAD_FILE.fields 정합):
 *   Id, Name, UniqueKey__c, RecordId__c, Size__c, Object__c, UploadKbn__c,
 *   Date__c, IsDeleted, CreatedDate, LastModifiedDate
 *
 * 매핑 규칙 (scan CSV 한 행 → 한 행. 단, refid 기준 dedup):
 *   Id            = refid (0EM... — sfid 컬럼. 본문 인라인 이미지의 고유 식별자)
 *   Name          = alt_name 이 있으면 그 값, 없으면 {refid}.{ext}
 *   UniqueKey__c  = {image-prefix}/{refid}.{ext}   (= unique_key, public/ 없음)
 *   RecordId__c   = notice_sfid (= eid, a04... → Stage2 record_sfid 조인 키)
 *   Size__c       = (빈값 — 본문 이미지는 ContentSize 메타 없음)
 *   Object__c     = DKRetail__Notice__c
 *   UploadKbn__c  = (빈값)
 *   Date__c       = (빈값 — 본문 이미지 생성일 메타 없음)
 *   IsDeleted     = false
 *   CreatedDate / LastModifiedDate = (빈값 — Stage1 이 DB DEFAULT now() 로 채움)
 *   · ext = img-dir 의 {refid}.* 파일 확장자. 다운로드 안 된 refid 는 skip (실패분).
 *
 * 중복 처리: 같은 refid 가 여러 본문/여러 <img> 에 나오면 1회만 출력 (sfid=refid 기준 dedup).
 *   (단, 통상 refid 는 본문 인라인 이미지 1개당 고유.)
 *
 * 사용법:
 *   kotlinc -script build-notice-image-upload-files.main.kts -- \
 *       --scan-csv <scan CSV> --img-dir <images dir> --out <notice_image_upload_files.csv> \
 *       [--image-prefix uploads/notice/migrated]   (public/ 없음)
 *
 * 멱등: 입력이 같으면 출력도 동일. 반복 실행 안전 (출력 파일 덮어씀).
 */

@file:DependsOn("com.opencsv:opencsv:5.9")

import com.opencsv.CSVReader
import com.opencsv.CSVWriter
import java.io.File
import java.io.FileReader
import java.io.FileWriter

// -----------------------------------------------------------------------------
// 인자 파싱
// -----------------------------------------------------------------------------

var scanCsvPath: String? = null
var imgDirPath: String? = null
var outPath: String? = null
// unique_key prefix. public/ 을 포함하지 않는다 (PublicUrlResolver prefix 가 .../public/ 로 끝남).
var imagePrefix = "uploads/notice/migrated"

run {
    val it = args.iterator()
    while (it.hasNext()) {
        when (val a = it.next()) {
            "--scan-csv" -> scanCsvPath = it.next()
            "--img-dir" -> imgDirPath = it.next()
            "--out" -> outPath = it.next()
            "--image-prefix" -> imagePrefix = it.next().trimEnd('/')
            "-h", "--help" -> {
                println("Usage: kotlinc -script build-notice-image-upload-files.main.kts -- --scan-csv <in> --img-dir <dir> --out <out> [--image-prefix uploads/notice/migrated]")
                kotlin.system.exitProcess(0)
            }
            else -> {
                System.err.println("Unknown arg: $a")
                kotlin.system.exitProcess(1)
            }
        }
    }
}

val scanCsv = scanCsvPath ?: run {
    System.err.println("[error] --scan-csv 필수"); kotlin.system.exitProcess(1)
}
val imgDir = imgDirPath ?: run {
    System.err.println("[error] --img-dir 필수"); kotlin.system.exitProcess(1)
}
val out = outPath ?: run {
    System.err.println("[error] --out 필수"); kotlin.system.exitProcess(1)
}

// 가드 — image-prefix 는 public/ 으로 시작할 수 없다 (PublicUrlResolver prefix 중복 방지).
if (imagePrefix.trimStart('/').lowercase().startsWith("public/")) {
    System.err.println("[error] --image-prefix 는 public/ 으로 시작할 수 없습니다: $imagePrefix")
    System.err.println("        PublicUrlResolver prefix 가 .../public/ 로 끝나 중복됩니다.")
    System.err.println("        예: uploads/notice/migrated (public 없이)")
    kotlin.system.exitProcess(1)
}

val scanFile = File(scanCsv)
if (!scanFile.isFile) {
    System.err.println("[error] scan CSV 없음: $scanCsv"); kotlin.system.exitProcess(1)
}
val imgDirFile = File(imgDir)
if (!imgDirFile.isDirectory) {
    System.err.println("[error] img-dir 없음: $imgDir"); kotlin.system.exitProcess(1)
}

// refid → 다운로드된 실제 파일 확장자 (img-dir 의 {refid}.* 1개). 없으면 다운로드 실패분 → skip.
val extByRefid: Map<String, String> = (imgDirFile.listFiles() ?: emptyArray())
    .filter { it.isFile && it.name.contains('.') }
    .associate { f ->
        val refid = f.name.substringBeforeLast('.')
        val ext = f.name.substringAfterLast('.').lowercase()
        refid to ext
    }

// 출력 헤더 — UPLOAD_FILE.fields 의 CSV 컬럼명 (SF 필드명) 과 정합. Stage1 은 헤더명으로 매핑하므로
// 미사용 컬럼(Url__c 등)은 출력하지 않아도 무방 (nullable).
val outHeader = arrayOf(
    "Id", "Name", "UniqueKey__c", "RecordId__c", "Size__c", "Object__c",
    "UploadKbn__c", "Date__c", "IsDeleted", "CreatedDate", "LastModifiedDate",
)

// scan CSV 컬럼명.
val COL_NOTICE = "notice_sfid"
val COL_REFID = "refid"
val COL_ALT = "alt_name"

File(out).parentFile?.mkdirs()

var total = 0
var written = 0
var skippedNoRefid = 0
var skippedNoImage = 0
var skippedDup = 0
val seenRefid = HashSet<String>()

CSVReader(FileReader(scanFile)).use { reader ->
    val header = reader.readNext()
        ?: run { System.err.println("[error] scan CSV 가 비어 있음"); kotlin.system.exitProcess(1) }
    val idx = header.withIndex().associate { (i, name) -> name.trim() to i }

    fun col(row: Array<String>, name: String): String {
        val i = idx[name] ?: return ""
        return if (i < row.size) row[i].trim() else ""
    }

    for (required in listOf(COL_NOTICE, COL_REFID)) {
        if (!idx.containsKey(required)) {
            System.err.println("[error] scan CSV 에 필수 컬럼 없음: $required (헤더=${header.joinToString(",")})")
            kotlin.system.exitProcess(1)
        }
    }

    CSVWriter(FileWriter(out)).use { writer ->
        writer.writeNext(outHeader)

        var row = reader.readNext()
        while (row != null) {
            total++
            val refid = col(row, COL_REFID)
            val noticeSfid = col(row, COL_NOTICE)

            if (refid.isEmpty() || noticeSfid.isEmpty()) {
                skippedNoRefid++; row = reader.readNext(); continue
            }
            val ext = extByRefid[refid]
            if (ext == null) {
                // 다운로드 실패분 (img-dir 에 {refid}.* 없음) — 적재 제외.
                skippedNoImage++; row = reader.readNext(); continue
            }
            if (!seenRefid.add(refid)) {
                skippedDup++; row = reader.readNext(); continue
            }

            val uniqueKey = "$imagePrefix/$refid.$ext"
            val alt = col(row, COL_ALT)
            val name = if (alt.isNotEmpty()) alt else "$refid.$ext"

            writer.writeNext(
                arrayOf(
                    refid,                  // Id          → sfid (본문 인라인 이미지 식별자 0EM...)
                    name,                   // Name        → alt 파일명 우선, 없으면 {refid}.{ext}
                    uniqueKey,              // UniqueKey__c → unique_key (public/ 없음)
                    noticeSfid,             // RecordId__c  → record_sfid (= notice SFID = eid)
                    "",                     // Size__c     → (본문 이미지 메타 없음)
                    "DKRetail__Notice__c",  // Object__c    → object_type
                    "",                     // UploadKbn__c
                    "",                     // Date__c
                    "false",                // IsDeleted
                    "",                     // CreatedDate  → Stage1 DB DEFAULT now()
                    "",                     // LastModifiedDate → Stage1 DB DEFAULT now()
                )
            )
            written++
            row = reader.readNext()
        }
    }
}

println("[done] build-notice-image-upload-files")
println("  scan rows read       : $total")
println("  written              : $written  -> $out")
println("  skipped (no refid)   : $skippedNoRefid")
println("  skipped (no image)   : $skippedNoImage  (다운로드 안 된 refid — 실패분)")
println("  skipped (dup refid)  : $skippedDup")
println("  image prefix         : $imagePrefix")
