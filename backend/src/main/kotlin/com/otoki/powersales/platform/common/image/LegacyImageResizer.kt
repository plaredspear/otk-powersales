package com.otoki.powersales.platform.common.image

import org.slf4j.LoggerFactory
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

/**
 * 레거시 Heroku `ImageUtil.resizeImage()` + `ImageUtil.getNewPath()` 의 이미지 처리 재현.
 *
 * 레거시 물류클레임(제안) 등록은 업로드된 원본을 그대로 S3 에 올리지 않는다 —
 * **650×650 이내로 축소한 뒤 그 축소본만** 공유 버킷에 올리고, 원본은 삭제한다. SF `UploadFile__c` 의
 * `Name`/`Size__c` 도 축소본 기준값이다. 그래서 SF 로 보내는 이미지의 픽셀·용량·파일명이 모두 축소본을
 * 따라야 레거시와 동일한 결과가 된다.
 *
 * 모바일은 이미 클라이언트에서 650×650 으로 줄여 보내지만(`image_picker_helper.dart`), admin 웹 등록
 * 경로에는 클라이언트 축소가 없고 구버전 앱도 있을 수 있어 **서버에서 최종 보장**한다. 이미 650 이하인
 * 이미지는 레거시 알고리즘상 축소 분기를 타지 않아 픽셀이 그대로 유지된다(재인코딩만 발생).
 *
 * 레거시 정합 세부:
 *  - 축소 계산은 width → height 순차 적용이며 각 단계에서 int 로 잘린다 (레거시와 동일하게 재현).
 *  - 확대는 하지 않는다 — 레거시가 `width > maxWidth` 일 때만 비율을 적용하기 때문.
 *  - 캔버스는 `TYPE_INT_RGB` — 알파 채널이 없어 투명 PNG 는 검정으로 합성된다(레거시 동일).
 *  - 출력 포맷은 원본 확장자 그대로 (`ImageIO.write(bi, ext, ...)`).
 *
 * 의도적 이탈 2건:
 *  1. 레거시 파일명 치환은 `replaceFirst("."+ext, "_resize."+ext)` 인데 첫 인자가 **정규식**이라
 *     `.` 가 임의 문자와 매칭된다 (`myjpgfile.jpg` → `m_resize.jpgfile.jpg`). 파일명을 망가뜨리는
 *     버그라 재현하지 않고, 의도대로 **마지막 확장자**만 치환한다.
 *  2. 디코딩/인코딩이 불가능한 포맷(HEIC 등 ImageIO 미지원)은 원본 바이트를 그대로 쓴다. 레거시는
 *     이때 `resizeImage` 가 null 을 반환해 NPE → 빈 경로로 0 byte 를 업로드하는 경로였는데,
 *     그 실패 모드까지 재현할 이유가 없다.
 */
object LegacyImageResizer {

    /** 레거시 `getNewPath` 의 `resizeImage(path, 650, 650)` 상한. */
    const val MAX_DIMENSION: Int = 650

    /** 레거시 축소본 파일명 접미 (`photo.jpg` → `photo_resize.jpg`). */
    private const val RESIZE_SUFFIX = "_resize"

    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * @param bytes 원본 이미지 바이트
     * @param originalFilename 클라이언트 파일명 (확장자로 출력 포맷을 결정)
     * @param resized 축소·재인코딩 성공 여부. false 면 [bytes] 가 원본 그대로다.
     */
    data class Result(
        val bytes: ByteArray,
        val fileName: String,
        val resized: Boolean,
    )

    /**
     * 레거시와 동일하게 650×650 이내로 축소한 바이트와 `_resize` 파일명을 돌려준다.
     * 축소가 불가능하면 원본 바이트 + 원본 파일명을 그대로 담아 반환한다(= [Result.resized] false).
     */
    fun resize(bytes: ByteArray, originalFilename: String?): Result {
        val safeName = originalFilename?.substringAfterLast('\\')?.takeIf { it.isNotBlank() } ?: DEFAULT_NAME
        val ext = safeName.substringAfterLast('.', "").lowercase()
        val fallback = Result(bytes = bytes, fileName = safeName, resized = false)

        if (ext.isBlank()) {
            log.warn("확장자가 없어 리사이즈 skip — fileName={}", safeName)
            return fallback
        }

        val source = runCatching { ImageIO.read(ByteArrayInputStream(bytes)) }.getOrNull()
        if (source == null) {
            // ImageIO 가 못 읽는 포맷(HEIC 등) — 원본 그대로 전달한다.
            log.warn("ImageIO 디코딩 불가로 리사이즈 skip — fileName={}", safeName)
            return fallback
        }

        val (width, height) = fitWithin(source.width, source.height)
        val dest = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        dest.createGraphics().apply {
            drawImage(source, 0, 0, width, height, null)
            dispose()
        }

        val out = ByteArrayOutputStream()
        val written = runCatching { ImageIO.write(dest, ext, out) }.getOrDefault(false)
        if (!written) {
            log.warn("ImageIO writer 없음으로 리사이즈 skip — ext={} fileName={}", ext, safeName)
            return fallback
        }

        return Result(bytes = out.toByteArray(), fileName = resizedName(safeName, ext), resized = true)
    }

    /**
     * 레거시 `resizeImage` 의 축소 비율 계산 재현 — width 판정 후 **갱신된** height 로 다시 판정하며,
     * 각 단계에서 float 곱 결과를 int 로 자른다.
     */
    private fun fitWithin(sourceWidth: Int, sourceHeight: Int): Pair<Int, Int> {
        var width = sourceWidth
        var height = sourceHeight

        if (width > MAX_DIMENSION) {
            val ratio = MAX_DIMENSION / width.toFloat()
            width = (width * ratio).toInt()
            height = (height * ratio).toInt()
        }
        if (height > MAX_DIMENSION) {
            val ratio = MAX_DIMENSION / height.toFloat()
            width = (width * ratio).toInt()
            height = (height * ratio).toInt()
        }
        // 극단적 종횡비에서 0 이 나오면 BufferedImage 생성이 터진다 — 레거시엔 없던 하한.
        return maxOf(width, 1) to maxOf(height, 1)
    }

    /** `photo.jpg` → `photo_resize.jpg` (마지막 확장자만 치환 — 레거시 정규식 버그 미재현). */
    private fun resizedName(fileName: String, ext: String): String =
        "${fileName.substringBeforeLast('.')}$RESIZE_SUFFIX.$ext"

    private const val DEFAULT_NAME = "unknown"
}
