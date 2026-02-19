/*
package com.otoki.internal.service

import com.otoki.internal.dto.request.DailySalesCreateRequest
import com.otoki.internal.dto.response.DailySalesCreateResponse
import com.otoki.internal.entity.DailySales
import com.otoki.internal.exception.*
import com.otoki.internal.repository.DailySalesRepository
import com.otoki.internal.repository.EventProductRepository
import com.otoki.internal.repository.EventRepository
import com.otoki.internal.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/ **
 * 일매출 관련 비즈니스 로직
 * /
@Service
class DailySalesService(
    private val dailySalesRepository: DailySalesRepository,
    private val eventRepository: EventRepository,
    private val eventProductRepository: EventProductRepository,
    private val userRepository: UserRepository,
    private val fileStorageService: FileStorageService
) {

    companion object {
        private val DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd")
    }

    / **
     * 일매출 등록
     *
     * @param userId 현재 로그인 사용자 ID
     * @param eventId 행사 ID
     * @param request 일매출 등록 요청
     * @return 일매출 등록 결과
     * /
    @Transactional
    fun registerDailySales(
        userId: Long,
        eventId: String,
        request: DailySalesCreateRequest
    ): DailySalesCreateResponse {
        // 1. 사용자 조회
        val user = userRepository.findById(userId)
            .orElseThrow { RuntimeException("사용자를 찾을 수 없습니다") }
        val employeeId = user.employeeId
        // 1. 행사 존재 여부 확인
        val event = eventRepository.findByEventId(eventId)
            .orElseThrow { EventNotFoundException() }

        // 2. 현재 사용자가 행사 담당자인지 확인
        if (event.assigneeId != employeeId) {
            throw DailySalesForbiddenException("행사 담당자만 매출을 등록할 수 있습니다")
        }

        // 3. 오늘이 행사 기간 내인지 확인
        val today = LocalDate.now()
        if (today !in event.startDate..event.endDate) {
            throw EventPeriodExpiredException()
        }

        // 4. 오늘 이미 REGISTERED 상태의 일매출이 있는지 확인 (중복 방지)
        val alreadyRegistered = dailySalesRepository.existsByEventIdAndEmployeeIdAndSalesDateAndStatus(
            eventId = eventId,
            employeeId = employeeId,
            salesDate = today,
            status = DailySales.STATUS_REGISTERED
        )
        if (alreadyRegistered) {
            throw DailySalesAlreadyRegisteredException()
        }

        // 5. 기타제품이 부분적으로만 입력되었는지 확인 (먼저 검증)
        if (request.hasPartialSubProduct()) {
            throw DailySalesInvalidParameterException("기타제품은 코드, 수량, 금액을 모두 입력해야 합니다")
        }

        // 6. 대표제품 또는 기타제품 중 최소 하나 입력 확인
        if (!request.hasMainProduct() && !request.hasSubProduct()) {
            throw DailySalesInvalidParameterException("대표제품 또는 기타제품 중 최소 하나를 입력해야 합니다")
        }

        // 7. 사진 파일 필수 확인
        if (request.photo == null || request.photo.isEmpty) {
            throw DailySalesInvalidPhotoException("사진 파일이 필요합니다")
        }

        // 8. 기타제품 코드 유효성 확인 (행사 제품 목록 내 존재)
        if (request.hasSubProduct()) {
            val productExists = eventProductRepository.existsByEventIdAndProductCode(
                eventId = eventId,
                productCode = request.subProductCode!!
            )
            if (!productExists) {
                throw DailySalesInvalidProductException("행사 제품 목록에 없는 제품입니다")
            }
        }

        // 9. 사진 파일 저장
        val photoUrl = fileStorageService.uploadDailySalesPhoto(
            file = request.photo,
            userId = userId,
            eventId = eventId,
            salesDate = today.format(DATE_FORMATTER)
        )

        // 10. 대표제품 총금액 자동 계산
        val mainProductAmount = request.calculateMainProductAmount()

        // 11. DailySales Entity 생성 및 저장
        val dailySales = DailySales(
            eventId = eventId,
            employeeId = employeeId,
            salesDate = today,
            mainProductPrice = request.mainProductPrice,
            mainProductQuantity = request.mainProductQuantity,
            mainProductAmount = mainProductAmount,
            subProductCode = request.subProductCode,
            subProductQuantity = request.subProductQuantity,
            subProductAmount = request.subProductAmount,
            photoUrl = photoUrl,
            status = DailySales.STATUS_REGISTERED
        )

        val saved = dailySalesRepository.save(dailySales)

        return DailySalesCreateResponse.from(saved)
    }

    / **
     * 일매출 임시저장
     *
     * @param userId 현재 로그인 사용자 ID
     * @param eventId 행사 ID
     * @param request 일매출 임시저장 요청
     * @return 일매출 저장 결과
     * /
    @Transactional
    fun saveDailySalesDraft(
        userId: Long,
        eventId: String,
        request: DailySalesCreateRequest
    ): DailySalesCreateResponse {
        // 1. 사용자 조회
        val user = userRepository.findById(userId)
            .orElseThrow { RuntimeException("사용자를 찾을 수 없습니다") }
        val employeeId = user.employeeId

        // 2. 행사 존재 여부 확인
        val event = eventRepository.findByEventId(eventId)
            .orElseThrow { EventNotFoundException() }

        // 3. 현재 사용자가 행사 담당자인지 확인
        if (event.assigneeId != employeeId) {
            throw DailySalesForbiddenException("행사 담당자만 매출을 저장할 수 있습니다")
        }

        val today = LocalDate.now()

        // 4. 기존 DRAFT 상태 데이터가 있으면 업데이트 (upsert)
        val existingDraft = dailySalesRepository.findByEventIdAndEmployeeIdAndSalesDateAndStatus(
            eventId = eventId,
            employeeId = employeeId,
            salesDate = today,
            status = DailySales.STATUS_DRAFT
        )

        // 5. 사진 파일이 있으면 저장
        val photoUrl = if (request.photo != null && !request.photo.isEmpty) {
            fileStorageService.uploadDailySalesPhoto(
                file = request.photo,
                userId = userId,
                eventId = eventId,
                salesDate = today.format(DATE_FORMATTER)
            )
        } else {
            existingDraft.map { it.photoUrl }.orElse(null)
        }

        // 6. 대표제품 총금액 자동 계산
        val mainProductAmount = request.calculateMainProductAmount()

        // 7. 기존 DRAFT가 있으면 업데이트, 없으면 신규 생성
        val dailySales = if (existingDraft.isPresent) {
            val draft = existingDraft.get()
            draft.mainProductPrice = request.mainProductPrice
            draft.mainProductQuantity = request.mainProductQuantity
            draft.mainProductAmount = mainProductAmount
            draft.subProductCode = request.subProductCode
            draft.subProductQuantity = request.subProductQuantity
            draft.subProductAmount = request.subProductAmount
            draft.photoUrl = photoUrl
            draft
        } else {
            DailySales(
                eventId = eventId,
                employeeId = employeeId,
                salesDate = today,
                mainProductPrice = request.mainProductPrice,
                mainProductQuantity = request.mainProductQuantity,
                mainProductAmount = mainProductAmount,
                subProductCode = request.subProductCode,
                subProductQuantity = request.subProductQuantity,
                subProductAmount = request.subProductAmount,
                photoUrl = photoUrl,
                status = DailySales.STATUS_DRAFT
            )
        }

        val saved = dailySalesRepository.save(dailySales)

        return DailySalesCreateResponse.from(saved)
    }
}
*/
