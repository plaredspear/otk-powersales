package com.example.demo

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.Duration
import java.time.Instant
import java.util.UUID

// Redis read/write 동작을 운영 환경에서 빠르게 점검하기 위한 진단용 엔드포인트.
// local 프로파일은 RedisAutoConfiguration 을 exclude 하므로 StringRedisTemplate
// 빈이 없고, 이 컨트롤러도 자동으로 비활성화된다(EB dev/prod 에서만 등록).
@RestController
@RequestMapping("/api/redis")
@ConditionalOnBean(StringRedisTemplate::class)
class RedisTestController(private val redis: StringRedisTemplate) {

    data class PingResponse(
        val ok: Boolean,
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

    data class WriteResponse(val key: String, val value: String, val ttlSeconds: Long?)

    data class ReadResponse(val key: String, val value: String?, val ttlSeconds: Long?)

    data class DeleteResponse(val key: String, val deleted: Boolean)

    // 라운드트립 검증: 임시 키 SET → GET → DEL 을 연속 수행하고 각 단계 소요시간과
    // 기대값 일치 여부를 반환한다. 배포 직후 상태 확인용.
    @GetMapping("/ping")
    fun ping(): ResponseEntity<PingResponse> {
        val key = "diag:ping:${UUID.randomUUID()}"
        val value = "pong-${Instant.now().toEpochMilli()}"
        val started = System.nanoTime()
        return try {
            val t0 = System.nanoTime()
            redis.opsForValue().set(key, value, Duration.ofSeconds(30))
            val t1 = System.nanoTime()
            val readValue = redis.opsForValue().get(key)
            val t2 = System.nanoTime()
            redis.delete(key)
            val t3 = System.nanoTime()
            val body = PingResponse(
                ok = readValue == value,
                key = key,
                writtenValue = value,
                readValue = readValue,
                matched = readValue == value,
                writeMs = Duration.ofNanos(t1 - t0).toMillis(),
                readMs = Duration.ofNanos(t2 - t1).toMillis(),
                deleteMs = Duration.ofNanos(t3 - t2).toMillis(),
                totalMs = Duration.ofNanos(t3 - started).toMillis(),
            )
            ResponseEntity.ok(body)
        } catch (e: Exception) {
            val failed = PingResponse(
                ok = false,
                key = key,
                writtenValue = value,
                readValue = null,
                matched = false,
                writeMs = -1,
                readMs = -1,
                deleteMs = -1,
                totalMs = Duration.ofNanos(System.nanoTime() - started).toMillis(),
                error = "${e.javaClass.simpleName}: ${e.message}",
            )
            ResponseEntity.status(503).body(failed)
        }
    }

    // 수동 쓰기. ttl 미지정 시 기본 5분(짧게 유지해 진단 흔적이 쌓이지 않도록).
    @PostMapping("/{key}")
    fun write(
        @PathVariable key: String,
        @RequestParam value: String,
        @RequestParam(required = false) ttl: Long?,
    ): WriteResponse {
        val effectiveTtl = ttl ?: 300
        redis.opsForValue().set(prefixed(key), value, Duration.ofSeconds(effectiveTtl))
        return WriteResponse(key = prefixed(key), value = value, ttlSeconds = effectiveTtl)
    }

    @GetMapping("/{key}")
    fun read(@PathVariable key: String): ReadResponse {
        val full = prefixed(key)
        val value = redis.opsForValue().get(full)
        val ttl = redis.getExpire(full)
        return ReadResponse(key = full, value = value, ttlSeconds = if (ttl >= 0) ttl else null)
    }

    @DeleteMapping("/{key}")
    fun delete(@PathVariable key: String): DeleteResponse {
        val full = prefixed(key)
        val removed = redis.delete(full)
        return DeleteResponse(key = full, deleted = removed)
    }

    // 모든 진단용 키는 diag: 네임스페이스로 고정해 운영 키와 섞이지 않도록 한다.
    private fun prefixed(key: String): String =
        if (key.startsWith("diag:")) key else "diag:$key"
}
