package com.example.demo

import io.awspring.cloud.s3.ObjectMetadata
import io.awspring.cloud.s3.S3Template
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile
import org.springframework.core.io.InputStreamResource
import org.springframework.core.io.Resource
import org.springframework.http.ContentDisposition
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import java.nio.charset.StandardCharsets
import java.time.Duration
import java.time.Instant
import java.util.UUID

// S3 put/get/delete 동작을 운영 환경에서 빠르게 점검하기 위한 진단용 엔드포인트.
// local 프로파일은 S3 auto-config 를 exclude 해 S3Template 빈이 없으므로
// @Profile("!local") 로 컨트롤러 자체를 제외한다. EB dev/prod 는
// SPRING_PROFILES_ACTIVE=${STAGE} 로 활성화되며, S3_BUCKET 환경 속성이
// `app.aws.s3.bucket` 으로 바인딩되어 주입된다.
//
// 모든 진단용 키는 `diag/` 네임스페이스로 고정해 운영 오브젝트와 섞이지 않도록
// 한다 (Redis 진단 컨트롤러의 `diag:` 접두사와 동일 전략).
@RestController
@RequestMapping("/api/s3")
@Profile("!local")
class S3TestController(
    private val s3: S3Template,
    @Value("\${app.aws.s3.bucket:}") private val bucket: String,
) {

    data class PingResponse(
        val ok: Boolean,
        val bucket: String,
        val key: String,
        val writtenValue: String,
        val readValue: String?,
        val matched: Boolean,
        val writeMs: Long,
        val readMs: Long,
        val deleteMs: Long,
        val totalMs: Long,
        val error: String? = null,
    )

    data class ErrorResponse(val error: String)

    data class WriteResponse(
        val bucket: String,
        val key: String,
        val size: Long,
        val contentType: String?,
    )

    data class ReadResponse(
        val bucket: String,
        val key: String,
        val size: Long,
        val contentType: String?,
        val value: String,
    )

    data class ListEntry(val key: String, val size: Long, val lastModifiedEpochMs: Long)

    data class ListResponse(val bucket: String, val prefix: String, val entries: List<ListEntry>)

    data class DeleteResponse(val bucket: String, val key: String, val deleted: Boolean)

    // 라운드트립 검증: 임시 키 PUT → GET → DEL 을 연속 수행하고 각 단계 소요시간과
    // 기대값 일치 여부를 반환한다. 배포 직후 상태 확인용.
    @GetMapping("/ping")
    fun ping(): ResponseEntity<PingResponse> {
        val key = "diag/ping/${UUID.randomUUID()}.txt"
        val value = "pong-${Instant.now().toEpochMilli()}"
        val started = System.nanoTime()
        if (bucket.isBlank()) {
            return ResponseEntity.status(503).body(
                failedPing(key, value, started, "app.aws.s3.bucket is not configured"),
            )
        }
        return try {
            val payload = value.toByteArray(StandardCharsets.UTF_8)
            val t0 = System.nanoTime()
            s3.upload(
                bucket,
                key,
                payload.inputStream(),
                ObjectMetadata.builder().contentType(MediaType.TEXT_PLAIN_VALUE).build(),
            )
            val t1 = System.nanoTime()
            val read = s3.download(bucket, key).inputStream.use { it.readAllBytes() }
                .toString(StandardCharsets.UTF_8)
            val t2 = System.nanoTime()
            s3.deleteObject(bucket, key)
            val t3 = System.nanoTime()
            ResponseEntity.ok(
                PingResponse(
                    ok = read == value,
                    bucket = bucket,
                    key = key,
                    writtenValue = value,
                    readValue = read,
                    matched = read == value,
                    writeMs = Duration.ofNanos(t1 - t0).toMillis(),
                    readMs = Duration.ofNanos(t2 - t1).toMillis(),
                    deleteMs = Duration.ofNanos(t3 - t2).toMillis(),
                    totalMs = Duration.ofNanos(t3 - started).toMillis(),
                ),
            )
        } catch (e: Exception) {
            ResponseEntity.status(503).body(
                failedPing(key, value, started, "${e.javaClass.simpleName}: ${e.message}"),
            )
        }
    }

    // 텍스트 쓰기. 작은 문자열을 S3 객체로 저장할 때 사용한다. 파일 업로드는
    // /objects 참조.
    @PostMapping("/text/{key}")
    fun writeText(
        @PathVariable key: String,
        @RequestParam value: String,
    ): ResponseEntity<Any> {
        if (bucket.isBlank()) return bucketUnconfigured()
        val full = prefixed(key)
        val bytes = value.toByteArray(StandardCharsets.UTF_8)
        val resource = s3.upload(
            bucket,
            full,
            bytes.inputStream(),
            ObjectMetadata.builder().contentType(MediaType.TEXT_PLAIN_VALUE).build(),
        )
        return ResponseEntity.ok(
            WriteResponse(
                bucket = bucket,
                key = full,
                size = runCatching { resource.contentLength() }.getOrDefault(bytes.size.toLong()),
                contentType = runCatching { resource.contentType() }.getOrNull(),
            ),
        )
    }

    // 텍스트 읽기. 바이너리일 경우 깨진 문자열이 반환될 수 있으므로 UI 에서는
    // 소량 텍스트 객체에만 쓰도록 안내한다. 바이너리는 /download 사용.
    @GetMapping("/text/{key}")
    fun readText(@PathVariable key: String): ResponseEntity<Any> {
        if (bucket.isBlank()) return bucketUnconfigured()
        val full = prefixed(key)
        val resource = s3.download(bucket, full)
        val bytes = resource.inputStream.use { it.readAllBytes() }
        return ResponseEntity.ok(
            ReadResponse(
                bucket = bucket,
                key = full,
                size = runCatching { resource.contentLength() }.getOrDefault(bytes.size.toLong()),
                contentType = runCatching { resource.contentType() }.getOrNull(),
                value = bytes.toString(StandardCharsets.UTF_8),
            ),
        )
    }

    // 멀티파트 파일 업로드. key 가 비어있으면 업로드된 파일 원본 이름을 사용한다.
    @PostMapping("/objects", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun uploadFile(
        @RequestPart("file") file: MultipartFile,
        @RequestParam(required = false) key: String?,
    ): ResponseEntity<Any> {
        if (bucket.isBlank()) return bucketUnconfigured()
        val rawKey = key?.takeIf { it.isNotBlank() }
            ?: file.originalFilename?.takeIf { it.isNotBlank() }
            ?: "upload-${UUID.randomUUID()}"
        val full = prefixed(rawKey)
        val metadata = ObjectMetadata.builder()
            .contentType(file.contentType ?: MediaType.APPLICATION_OCTET_STREAM_VALUE)
            .build()
        val resource = file.inputStream.use { s3.upload(bucket, full, it, metadata) }
        return ResponseEntity.ok(
            WriteResponse(
                bucket = bucket,
                key = full,
                size = runCatching { resource.contentLength() }.getOrDefault(file.size),
                contentType = runCatching { resource.contentType() }.getOrNull() ?: file.contentType,
            ),
        )
    }

    // 원본 바이트를 그대로 내려준다. 브라우저에서 다운로드 저장되도록
    // Content-Disposition 헤더를 설정한다.
    @GetMapping("/objects/{key}/download")
    fun download(@PathVariable key: String): ResponseEntity<Resource> {
        if (bucket.isBlank()) {
            return ResponseEntity.status(503).build()
        }
        val full = prefixed(key)
        val resource = s3.download(bucket, full)
        val filename = full.substringAfterLast('/').ifEmpty { full }
        val contentType = runCatching { resource.contentType() }.getOrNull()
            ?: MediaType.APPLICATION_OCTET_STREAM_VALUE
        val length = runCatching { resource.contentLength() }.getOrDefault(-1L)
        val disposition = ContentDisposition.attachment()
            .filename(filename, StandardCharsets.UTF_8)
            .build()
        val body = InputStreamResource(resource.inputStream)
        val builder = ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
            .contentType(MediaType.parseMediaType(contentType))
        if (length >= 0) builder.contentLength(length)
        return builder.body(body)
    }

    // 진단용 네임스페이스만 노출해 실수로 운영 객체를 훑지 않도록 한다.
    @GetMapping("/objects")
    fun list(@RequestParam(required = false) prefix: String?): ResponseEntity<Any> {
        if (bucket.isBlank()) return bucketUnconfigured()
        val effective = prefixed(prefix ?: "")
        val resources = s3.listObjects(bucket, effective)
        val entries = resources.map {
            ListEntry(
                key = runCatching { it.location.`object` }.getOrNull() ?: (it.filename ?: ""),
                size = runCatching { it.contentLength() }.getOrDefault(-1L),
                lastModifiedEpochMs = runCatching { it.lastModified() }.getOrDefault(-1L),
            )
        }
        return ResponseEntity.ok(ListResponse(bucket = bucket, prefix = effective, entries = entries))
    }

    @DeleteMapping("/objects/{key}")
    fun delete(@PathVariable key: String): ResponseEntity<Any> {
        if (bucket.isBlank()) return bucketUnconfigured()
        val full = prefixed(key)
        s3.deleteObject(bucket, full)
        return ResponseEntity.ok(DeleteResponse(bucket = bucket, key = full, deleted = true))
    }

    // 모든 진단용 키는 diag/ 네임스페이스로 고정한다. URL path 에서 받은 key 는
    // 이미 diag/ 로 시작하면 그대로, 아니면 접두사를 덧붙인다.
    private fun prefixed(key: String): String =
        if (key.startsWith("diag/")) key else "diag/$key"

    private fun bucketUnconfigured(): ResponseEntity<Any> =
        ResponseEntity.status(503).body(ErrorResponse("app.aws.s3.bucket is not configured"))

    private fun failedPing(key: String, value: String, started: Long, message: String): PingResponse =
        PingResponse(
            ok = false,
            bucket = bucket,
            key = key,
            writtenValue = value,
            readValue = null,
            matched = false,
            writeMs = -1,
            readMs = -1,
            deleteMs = -1,
            totalMs = Duration.ofNanos(System.nanoTime() - started).toMillis(),
            error = message,
        )
}
