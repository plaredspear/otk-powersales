package com.otoki.powersales.promotion.service

import com.otoki.powersales.promotion.dto.request.PromotionTypeRequest
import com.otoki.powersales.promotion.dto.response.PromotionTypeResponse
import com.otoki.powersales.promotion.entity.PromotionType
import com.otoki.powersales.promotion.exception.PromotionTypeDuplicateException
import com.otoki.powersales.promotion.exception.PromotionTypeNotFoundException
import com.otoki.powersales.promotion.repository.PromotionTypeRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class AdminPromotionTypeService(
    private val promotionTypeRepository: PromotionTypeRepository
) {

    fun getPromotionTypes(): List<PromotionTypeResponse> {
        return promotionTypeRepository.findByIsActiveTrueOrderByDisplayOrderAsc()
            .map { PromotionTypeResponse.from(it) }
    }

    @Transactional
    fun createPromotionType(request: PromotionTypeRequest): PromotionTypeResponse {
        if (promotionTypeRepository.existsByName(request.name)) {
            throw PromotionTypeDuplicateException()
        }

        val promotionType = promotionTypeRepository.save(
            PromotionType(
                name = request.name,
                displayOrder = request.displayOrder
            )
        )

        return PromotionTypeResponse.from(promotionType)
    }

    @Transactional
    fun updatePromotionType(id: Long, request: PromotionTypeRequest): PromotionTypeResponse {
        val promotionType = promotionTypeRepository.findById(id)
            .orElseThrow { PromotionTypeNotFoundException() }

        if (promotionTypeRepository.existsByNameAndIdNot(request.name, id)) {
            throw PromotionTypeDuplicateException()
        }

        promotionType.update(name = request.name, displayOrder = request.displayOrder)

        return PromotionTypeResponse.from(promotionType)
    }

    @Transactional
    fun deletePromotionType(id: Long) {
        val promotionType = promotionTypeRepository.findById(id)
            .orElseThrow { PromotionTypeNotFoundException() }

        promotionType.deactivate()
    }
}
