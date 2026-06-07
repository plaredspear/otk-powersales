#!/usr/bin/env kotlin

/**
 * 클레임 이미지 마이그레이션 — ContentDocumentLink 메타 CSV → claim_upload_files.csv 변환기.
 *
 * 배경:
 *   클레임(DKRetail__Claim__c) 첨부 이미지는 레거시에서 SF Files 에 저장되며, claim 과의 연결은
 *   **ContentDocumentLink.LinkedEntityId = claim.Id** 다 (SF UI 의 Util.contentdocument 조회 경로).
 *   ContentVersion.RecordId__c / Type__c 는 운영에서 미사용(전부 null)이라 신뢰할 수 없다.
 *   신규 시스템은 claim 이미지를 upload_file (parent_type='Claim', unique_key=S3 key) 로 조회하므로,
 *   ContentDocumentLink → 최신 ContentVersion 을 추출해 S3 에 재업로드하고 메타를 upload_file 로 적재한다.
 *
 *   본 스크립트는 그중 "메타 CSV → claim_upload_files.csv 변환" 만 담당하는 오프라인 단계다 (AWS 접근 없음).
 *   바이너리 다운로드 / S3 업로드 / Stage1·Stage2 는 migrate-claim-images.sh + web 화면이 처리.
 *
 * 입력 (ContentDocumentLink 메타 CSV — migrate-claim-images.sh 의 query 단계 산출):
 *   SOQL: SELECT LinkedEntityId, ContentDocumentId,
 *                ContentDocument.LatestPublishedVersionId, ContentDocument.Title,
 *                ContentDocument.FileExtension, ContentDocument.ContentSize,
 *                ContentDocument.CreatedDate, ContentDocument.LastModifiedDate
 *         FROM ContentDocumentLink WHERE LinkedEntityId IN (SELECT Id FROM DKRetail__Claim__c)
 *   CSV 헤더(컬럼): LinkedEntityId, ContentDocumentId,
 *                  ContentDocument.LatestPublishedVersionId, ContentDocument.Title,
 *                  ContentDocument.FileExtension, ContentDocument.ContentSize,
 *                  ContentDocument.CreatedDate, ContentDocument.LastModifiedDate
 *
 * 출력 (claim_upload_files.csv — Stage1Targets.ClaimImageUploadFile 헤더 정합. fields 는
 *        레거시 UploadFile 타겟과 동일 컬럼이나 csvFileName 만 분리):
 *   Id, Name, UniqueKey__c, RecordId__c, Size__c, Object__c, UploadKbn__c,
 *   Date__c, IsDeleted, CreatedDate, LastModifiedDate
 *   (Stage1 FieldMapping 에 없는 Url__c / FileId__c / Owner/Created/LastModifiedById 는 공란 — nullable)
 *
 * 매핑 규칙 (한 행/ContentDocumentLink → claim_upload_files.csv 한 행):
 *   Id            = ContentDocument.LatestPublishedVersionId (=최신 ContentVersion.Id, 068... → sfid, unique)
 *   Name          = ContentDocument.Title
 *   UniqueKey__c  = {image-prefix}/{CV.Id}.{ext}   (= unique_key, 신규 클레임 상세 조회 key)
 *   RecordId__c   = LinkedEntityId               (= claim.Id, a01... → Stage2 record_sfid 조인 키)
 *   Size__c       = ContentDocument.ContentSize
 *   Object__c     = DKRetail__Claim__c           (보존용. Stage2 record_sfid 조인이라 분기엔 미사용)
 *   UploadKbn__c  = (빈값)                         (Type__c 운영 미사용 — 클레임 상세는 사진 전체 노출이라 구분 불요)
 *   Date__c       = ContentDocument.CreatedDate (date 부분)
 *   IsDeleted     = false
 *   CreatedDate / LastModifiedDate = ContentDocument 값
 *   · ext = ContentDocument.FileExtension, 없으면 Title 확장자, 둘 다 없으면 jpg.
 *
 * 중복 처리: 한 ContentDocument 가 여러 claim 에 링크된 경우 같은 sfid(CV.Id) 가 여러 행에 나올 수
 *   있다. 본 변환기는 sfid 기준 1회만 출력(첫 등장)하고 중복은 skip — Stage1 의 ON CONFLICT(sfid)
 *   DO NOTHING 과 동일 의미. (클레임 이미지는 통상 claim 1:1.)
 *
 * 사용법:
 *   kotlinc -script build-claim-upload-files.main.kts -- \
 *       --meta-csv <ContentDocumentLink 메타 CSV> \
 *       --out <claim_upload_files.csv> \
 *       [--image-prefix uploads/claim/migrated]   (segment 없음 — backend 가 조회 시 private/ 합성)
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

var metaCsvPath: String? = null
var outPath: String? = null
// unique_key 의 prefix. segment(private/·public/) 를 포함하지 않는다 — 클레임 이미지는 private/
// 저장 + presigned 조회이고, 조회 시 backend(StorageConstants.privateKey)가 unique_key 앞에
// private/ 를 합성한다. segment 를 넣으면 private/private/... 로 중복된다.
// S3 실제 객체 key = private/ + unique_key.
var imagePrefix = "uploads/claim/migrated"
// 이미지 확장자 화이트리스트 (클레임 이미지만 적재 — PDF 등 비이미지 첨부 제외).
var imageExts = "jpg,jpeg,png,gif,bmp,webp,heic,heif"

run {
    val it = args.iterator()
    while (it.hasNext()) {
        when (val a = it.next()) {
            "--meta-csv" -> metaCsvPath = it.next()
            "--out" -> outPath = it.next()
            "--image-prefix" -> imagePrefix = it.next().trimEnd('/')
            "--image-exts" -> imageExts = it.next()
            "-h", "--help" -> {
                println("Usage: kotlinc -script build-claim-upload-files.main.kts -- --meta-csv <in> --out <out> [--image-prefix uploads/claim/migrated] [--image-exts jpg,jpeg,png,...]")
                kotlin.system.exitProcess(0)
            }
            else -> {
                System.err.println("Unknown arg: $a")
                kotlin.system.exitProcess(1)
            }
        }
    }
}

val metaCsv = metaCsvPath ?: run {
    System.err.println("[error] --meta-csv 필수")
    kotlin.system.exitProcess(1)
}
val out = outPath ?: run {
    System.err.println("[error] --out 필수")
    kotlin.system.exitProcess(1)
}

// 가드 — image-prefix 는 segment(private/·public/) 로 시작할 수 없다. backend 가 조회 시
// StorageConstants.privateKey 로 unique_key 앞에 private/ 를 합성하므로, segment 를 넣으면
// private/private/... (또는 private/public/...) 로 어긋난다. (과거 사고 재발 방지)
val imagePrefixLc = imagePrefix.trimStart('/').lowercase()
if (imagePrefixLc.startsWith("private/") || imagePrefixLc.startsWith("public/")) {
    System.err.println("[error] --image-prefix 는 private/ 또는 public/ 으로 시작할 수 없습니다: $imagePrefix")
    System.err.println("        backend 가 조회 시 private/ 를 자동 합성하므로 unique_key 는 segment 없이 둡니다.")
    System.err.println("        예: uploads/claim/migrated")
    kotlin.system.exitProcess(1)
}

val metaFile = File(metaCsv)
if (!metaFile.isFile) {
    System.err.println("[error] 메타 CSV 없음: $metaCsv")
    kotlin.system.exitProcess(1)
}

val imageExtSet = imageExts.split(",").map { it.trim().lowercase() }.filter { it.isNotEmpty() }.toSet()

// 입력 CSV 의 ContentDocumentLink (relationship) 컬럼명.
val COL_LINKED = "LinkedEntityId"
val COL_CVID = "ContentDocument.LatestPublishedVersionId"
val COL_TITLE = "ContentDocument.Title"
val COL_EXT = "ContentDocument.FileExtension"
val COL_SIZE = "ContentDocument.ContentSize"
val COL_CREATED = "ContentDocument.CreatedDate"
val COL_MODIFIED = "ContentDocument.LastModifiedDate"

// 출력 헤더 — Stage1Targets.UPLOAD_FILE 의 CSV 컬럼명 (SF 필드명) 과 정합.
// Stage1 은 헤더명으로 매핑하므로 미사용 컬럼(Url__c 등)은 출력하지 않아도 무방 (nullable).
val outHeader = arrayOf(
    "Id", "Name", "UniqueKey__c", "RecordId__c", "Size__c", "Object__c",
    "UploadKbn__c", "Date__c", "IsDeleted", "CreatedDate", "LastModifiedDate",
)

// -----------------------------------------------------------------------------
// 헬퍼
// -----------------------------------------------------------------------------

// ext = FileExtension 우선, 없으면 Title 의 확장자, 둘 다 없으면 jpg.
fun extOf(fileExtension: String, title: String): String {
    val fe = fileExtension.trim()
    if (fe.isNotEmpty()) return fe.trimStart('.').lowercase()
    val dot = title.lastIndexOf('.')
    if (dot in 0 until title.length - 1) {
        return title.substring(dot + 1).trim().lowercase()
    }
    return "jpg"
}

// SF datetime (2024-06-06T14:30:22.000+0000) → date (2024-06-06). 빈 값은 그대로.
fun dateOf(dt: String): String {
    val t = dt.indexOf('T')
    return if (t > 0) dt.substring(0, t) else dt
}

// -----------------------------------------------------------------------------
// 변환
// -----------------------------------------------------------------------------

File(out).parentFile?.mkdirs()

var total = 0
var written = 0
var skippedNoKey = 0
var skippedNonImage = 0
var skippedDup = 0
val seenSfid = HashSet<String>()

CSVReader(FileReader(metaFile)).use { reader ->
    val header = reader.readNext()
        ?: run {
            System.err.println("[error] 메타 CSV 가 비어 있음")
            kotlin.system.exitProcess(1)
        }
    val idx = header.withIndex().associate { (i, name) -> name.trim() to i }

    fun col(row: Array<String>, name: String): String {
        val i = idx[name] ?: return ""
        return if (i < row.size) row[i].trim() else ""
    }

    for (required in listOf(COL_LINKED, COL_CVID)) {
        if (!idx.containsKey(required)) {
            System.err.println("[error] 메타 CSV 에 필수 컬럼 없음: $required (헤더=${header.joinToString(",")})")
            kotlin.system.exitProcess(1)
        }
    }

    CSVWriter(FileWriter(out)).use { writer ->
        writer.writeNext(outHeader)

        var row = reader.readNext()
        while (row != null) {
            total++
            val cvId = col(row, COL_CVID)         // 최신 ContentVersion.Id → sfid + 파일명
            val recordId = col(row, COL_LINKED)   // claim.Id (a01...) → record_sfid

            if (cvId.isEmpty() || recordId.isEmpty()) {
                skippedNoKey++
                row = reader.readNext()
                continue
            }

            val ext = extOf(col(row, COL_EXT), col(row, COL_TITLE))
            if (ext !in imageExtSet) {
                // 클레임에 링크됐어도 이미지가 아닌 첨부(PDF 등)는 제외.
                skippedNonImage++
                row = reader.readNext()
                continue
            }

            if (!seenSfid.add(cvId)) {
                // 한 ContentDocument 가 여러 claim 에 링크된 경우 — sfid 중복은 1회만.
                skippedDup++
                row = reader.readNext()
                continue
            }

            val uniqueKey = "$imagePrefix/$cvId.$ext"
            val createdDate = col(row, COL_CREATED)

            writer.writeNext(
                arrayOf(
                    cvId,                       // Id          → sfid (최신 ContentVersion.Id)
                    col(row, COL_TITLE),        // Name        → ContentDocument.Title
                    uniqueKey,                  // UniqueKey__c → unique_key
                    recordId,                   // RecordId__c  → record_sfid (=claim.Id)
                    col(row, COL_SIZE),         // Size__c     → ContentDocument.ContentSize
                    "DKRetail__Claim__c",       // Object__c    → object_type (보존)
                    "",                         // UploadKbn__c → 빈값 (Type__c 운영 미사용)
                    dateOf(createdDate),        // Date__c
                    "false",                    // IsDeleted
                    createdDate,                // CreatedDate
                    col(row, COL_MODIFIED),     // LastModifiedDate
                )
            )
            written++
            row = reader.readNext()
        }
    }
}

println("[done] build-claim-upload-files")
println("  meta rows read : $total")
println("  written        : $written  -> $out")
println("  skipped (no key)      : $skippedNoKey")
println("  skipped (non-image)   : $skippedNonImage  (허용 확장자: ${imageExtSet.sorted().joinToString(",")})")
println("  skipped (dup sfid)    : $skippedDup")
println("  image prefix   : $imagePrefix")
