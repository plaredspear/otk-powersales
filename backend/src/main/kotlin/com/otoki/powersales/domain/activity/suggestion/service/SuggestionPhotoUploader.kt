package com.otoki.powersales.domain.activity.suggestion.service

import com.otoki.powersales.platform.common.image.LegacyImageResizer
import com.otoki.powersales.platform.common.service.FileStorageService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile

/**
 * 제안(물류클레임) 첨부 사진 1장의 저장 파이프라인 — 레거시 Heroku `FieldTalkController.suggestProc`
 * (hndSp="I", line 1717-1729) 정합.
 *
 * 레거시 1장 처리 순서:
 *  1. `img.getNewPath(...)` — 650×650 축소본 생성, 파일명 `<원본명>_resize.<ext>`, **원본은 삭제**
 *  2. `ImageUtil.getFileSize(file.length())` — **축소본** 크기를 포맷 문자열로
 *  3. `awsService.uploadAWS(축소본, 사번)` — SF 공유 버킷에 PublicRead 로 올리고 key 획득
 *  4. `S3ImageUniqueKey/FileName/FileSize` 3필드를 SF `/ProposalRegist` payload 에 채움
 *
 * 신규 시스템은 여기에 **앱 조회용 private 사본**이 하나 더 붙는다 — 레거시는 공개 객체 1장을 앱·SF 가
 * 공유했지만, 신규는 사진을 권한 통제 대상으로 보고 private/presigned 로 조회하기 때문이다
 * (제품클레임·현장점검과 동일 정책). 두 사본 모두 **같은 축소본 바이트**라 이미지 자체는 레거시와 동일하다.
 *
 * mobile 은 클라이언트에서 이미 650 으로 줄여 보내지만(`image_picker_helper.dart`), admin 웹 등록에는
 * 클라이언트 축소가 없어 서버에서 최종 보장한다 — 등록 경로가 달라도 SF 가 받는 이미지는 같아야 한다.
 */
@Service
class SuggestionPhotoUploader(
    private val fileStorageService: FileStorageService,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * 저장 결과 — `UploadFile` row 와 SF payload 슬롯을 동시에 채운다.
     *
     * @param uniqueKey 파워세일즈 전용 버킷의 private key (앱 조회용)
     * @param sfUniqueKey SF 공유 버킷 사본 key. 공유 버킷 미설정이면 null.
     * @param fileName 레거시 축소본 파일명 (`<원본명>_resize.<ext>`) — SF `UploadFile__c.Name` 값
     * @param fileSize 레거시 `getFileSize()` 포맷의 **축소본** 크기
     */
    data class StoredPhoto(
        val uniqueKey: String,
        val sfUniqueKey: String?,
        val fileName: String,
        val fileSize: String,
    )

    /**
     * 사진 1장을 축소해 private 사본 + SF 공유 버킷 사본으로 저장한다.
     *
     * @param employeeCode SF 공유 버킷 key 구성 요소 (레거시 `uploadAWS(file, employeeId)` 정합)
     */
    fun store(file: MultipartFile, employeeCode: String?): StoredPhoto {
        val resized = LegacyImageResizer.resize(file.bytes, file.originalFilename)
        val contentType = file.contentType ?: DEFAULT_CONTENT_TYPE

        val uniqueKey = fileStorageService.uploadSuggestionPhoto(
            bytes = resized.bytes,
            originalName = resized.fileName,
            contentType = contentType,
        )
        val sfUniqueKey = fileStorageService.uploadSuggestionPhotoForSf(
            bytes = resized.bytes,
            contentType = contentType,
            employeeCode = employeeCode,
        )
        if (sfUniqueKey == null) {
            log.warn("SF 공유 버킷 사본 미생성 — SF 화면에서 이미지가 보이지 않는다 fileName={}", resized.fileName)
        }

        return StoredPhoto(
            uniqueKey = uniqueKey,
            sfUniqueKey = sfUniqueKey,
            fileName = resized.fileName,
            fileSize = LegacyFileSizeFormatter.format(resized.bytes.size.toLong()),
        )
    }

    /**
     * 임시저장 사진 1장 저장 — private 사본만 만든다.
     *
     * 레거시 `tempSuggestProc`(line 1424-1437) 도 동일하게 650 축소본을 만들어 올린다. 다만 임시저장은 SF 로
     * 가지 않고(정식 등록 시 tmp 이미지는 삭제 후 폼에서 재업로드), 신규 시스템은 draft 사진을 앱에서만
     * 조회하므로 공유 버킷 사본은 만들지 않는다.
     *
     * @return private 사본 key
     */
    fun storeDraft(file: MultipartFile): String {
        val resized = LegacyImageResizer.resize(file.bytes, file.originalFilename)
        return fileStorageService.uploadSuggestionPhoto(
            bytes = resized.bytes,
            originalName = resized.fileName,
            contentType = file.contentType ?: DEFAULT_CONTENT_TYPE,
        )
    }

    private companion object {
        const val DEFAULT_CONTENT_TYPE = "image/jpeg"
    }
}

/**
 * 파일크기 포맷 — 레거시 Heroku `ImageUtil.getFileSize(long)` 완전 재현.
 *
 * SF `S3ImageFileSize`/`UploadFile__c.Size__c`(Text) 및 DB `upload_file.size` 정합을 위해 레거시와 동일한
 * 포맷을 쓴다. 레거시 특성:
 *  - `fileSize /= 1024` 를 `long` 으로 수행 → **정수 절삭**(예: 1536B → "1.0KB", "1.5KB" 아님)
 *  - 단위 배열 `{Byte, KB, MB}` — GB 이상(인덱스 초과)이면 예외 경로로 "0.0 Byte"
 *  - 0 byte 는 루프 미진입 → "0.0Byte"
 *  - 숫자는 항상 `.0` 으로 끝남(double 로 승격된 정수), 단위 앞 공백 없음(정상 경로)
 */
object LegacyFileSizeFormatter {

    fun format(bytes: Long): String {
        val units = arrayOf("Byte", "KB", "MB")
        return try {
            var fileSize = bytes
            var unitIndex = 0
            var changeSize = 0.0
            var x = 0
            while (fileSize / 1024.0 > 0) {
                unitIndex = x
                changeSize = fileSize.toDouble()
                x++
                fileSize /= 1024
            }
            "$changeSize${units[unitIndex]}"
        } catch (ex: Exception) {
            "0.0 Byte"
        }
    }
}
