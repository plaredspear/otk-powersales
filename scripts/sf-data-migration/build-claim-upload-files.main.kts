#!/usr/bin/env kotlin

/**
 * 클레임 이미지 마이그레이션 — ContentVersion 메타 CSV → upload_files.csv 변환기.
 *
 * 배경:
 *   클레임(DKRetail__Claim__c) 첨부 이미지는 레거시에서 SF Files(ContentVersion)에만 저장되었고
 *   (IF_REST_MOBILE_ClaimRegist), UploadFile__c 에는 적재된 적이 없다. 신규 시스템은 claim 이미지를
 *   upload_file (parent_type='Claim', unique_key=S3 key) 로 조회하므로, ContentVersion 을 추출해
 *   S3 에 재업로드하고 그 메타를 upload_file 로 적재해야 한다.
 *
 *   본 스크립트는 그중 "메타 CSV → upload_files.csv 변환" 만 담당하는 오프라인 단계다 (AWS 접근 없음).
 *   바이너리 다운로드 / S3 업로드 / Stage1·Stage2 트리거는 migrate-claim-images.sh 가 오케스트레이션.
 *
 * 입력 (ContentVersion 메타 CSV — migrate-claim-images.sh 의 query 단계 산출):
 *   컬럼: Id, RecordId__c, Type__c, Title, PathOnClient, FileExtension, ContentSize,
 *         CreatedDate, LastModifiedDate
 *
 * 출력 (upload_files.csv — Stage1Targets.UPLOAD_FILE 헤더 정합):
 *   Id, Name, UniqueKey__c, RecordId__c, Size__c, Object__c, UploadKbn__c,
 *   Date__c, IsDeleted, CreatedDate, LastModifiedDate
 *   (Stage1 FieldMapping 에 없는 Url__c / FileId__c / Owner/Created/LastModifiedById 는 공란 — nullable)
 *
 * 매핑 규칙 (한 행/ContentVersion → upload_files.csv 한 행):
 *   Id            = ContentVersion.Id           (068... → upload_file.sfid, unique)
 *   Name          = Title
 *   UniqueKey__c  = {image-prefix}/{CV.Id}.{ext}   (= unique_key, 신규 클레임 상세 조회 key)
 *   RecordId__c   = ContentVersion.RecordId__c  (= claim.Id, a01... → Stage2 record_sfid 조인 키)
 *   Size__c       = ContentSize
 *   Object__c     = DKRetail__Claim__c          (보존용. Stage2 record_sfid 조인이라 분기엔 미사용)
 *   UploadKbn__c  = claim|part|receipt          (Type__c: 클레임→claim/일부인→part/영수증→receipt)
 *   Date__c       = CreatedDate (date 부분)
 *   IsDeleted     = false
 *   CreatedDate / LastModifiedDate = SF 값 그대로
 *   · ext = FileExtension 우선, 없으면 PathOnClient 확장자, 둘 다 없으면 jpg.
 *
 * 사용법:
 *   kotlinc -script build-claim-upload-files.main.kts -- \
 *       --meta-csv <ContentVersion 메타 CSV> \
 *       --out <upload_files.csv> \
 *       [--image-prefix uploads/claim/migrated]
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
var imagePrefix = "uploads/claim/migrated"

run {
    val it = args.iterator()
    while (it.hasNext()) {
        when (val a = it.next()) {
            "--meta-csv" -> metaCsvPath = it.next()
            "--out" -> outPath = it.next()
            "--image-prefix" -> imagePrefix = it.next().trimEnd('/')
            "-h", "--help" -> {
                println("Usage: kotlinc -script build-claim-upload-files.main.kts -- --meta-csv <in> --out <out> [--image-prefix uploads/claim/migrated]")
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

val metaFile = File(metaCsv)
if (!metaFile.isFile) {
    System.err.println("[error] 메타 CSV 없음: $metaCsv")
    kotlin.system.exitProcess(1)
}

// -----------------------------------------------------------------------------
// Type__c → upload_kbn 매핑 (UploadFileKbnTypes SoT 정합)
// -----------------------------------------------------------------------------

// SF Type__c (한글) → 신규 upload_kbn 어간. UploadFileKbnTypes: claim/part/receipt.
val typeToKbn = mapOf(
    "클레임" to "claim",
    "일부인" to "part",
    "영수증" to "receipt",
)

// 출력 헤더 — Stage1Targets.UPLOAD_FILE 의 CSV 컬럼명 (SF 필드명) 과 정합.
// Stage1 은 헤더명으로 매핑하므로 미사용 컬럼(Url__c 등)은 출력하지 않아도 무방 (nullable).
val outHeader = arrayOf(
    "Id", "Name", "UniqueKey__c", "RecordId__c", "Size__c", "Object__c",
    "UploadKbn__c", "Date__c", "IsDeleted", "CreatedDate", "LastModifiedDate",
)

// -----------------------------------------------------------------------------
// 헬퍼
// -----------------------------------------------------------------------------

fun extOf(fileExtension: String, pathOnClient: String): String {
    val fe = fileExtension.trim()
    if (fe.isNotEmpty()) return fe.trimStart('.').lowercase()
    val dot = pathOnClient.lastIndexOf('.')
    if (dot in 0 until pathOnClient.length - 1) {
        return pathOnClient.substring(dot + 1).trim().lowercase()
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
var skippedNoRecordId = 0
var skippedUnknownType = 0

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

    for (required in listOf("Id", "RecordId__c", "Type__c")) {
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
            val cvId = col(row, "Id")
            val recordId = col(row, "RecordId__c")
            val type = col(row, "Type__c")

            if (cvId.isEmpty() || recordId.isEmpty()) {
                skippedNoRecordId++
                row = reader.readNext()
                continue
            }
            val kbn = typeToKbn[type]
            if (kbn == null) {
                // 클레임 첨부가 아닌 Type 은 건너뜀 (SOQL 에서 이미 필터하나 방어).
                skippedUnknownType++
                row = reader.readNext()
                continue
            }

            val ext = extOf(col(row, "FileExtension"), col(row, "PathOnClient"))
            val uniqueKey = "$imagePrefix/$cvId.$ext"
            val createdDate = col(row, "CreatedDate")

            writer.writeNext(
                arrayOf(
                    cvId,                       // Id          → sfid
                    col(row, "Title"),          // Name
                    uniqueKey,                  // UniqueKey__c → unique_key
                    recordId,                   // RecordId__c  → record_sfid (=claim.Id)
                    col(row, "ContentSize"),    // Size__c
                    "DKRetail__Claim__c",       // Object__c    → object_type (보존)
                    kbn,                        // UploadKbn__c → upload_kbn
                    dateOf(createdDate),        // Date__c
                    "false",                    // IsDeleted
                    createdDate,                // CreatedDate
                    col(row, "LastModifiedDate"), // LastModifiedDate
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
println("  skipped (no Id/RecordId__c) : $skippedNoRecordId")
println("  skipped (unknown Type__c)   : $skippedUnknownType")
println("  image prefix   : $imagePrefix")
