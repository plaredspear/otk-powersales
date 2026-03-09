package com.otoki.internal.admin.service

import com.otoki.internal.admin.dto.request.PromotionTypeRequest
import com.otoki.internal.promotion.entity.PromotionType
import com.otoki.internal.promotion.exception.PromotionTypeDuplicateException
import com.otoki.internal.promotion.exception.PromotionTypeNotFoundException
import com.otoki.internal.promotion.repository.PromotionTypeRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.*
import java.util.*

@ExtendWith(MockitoExtension::class)
@DisplayName("AdminPromotionTypeService 테스트")
class AdminPromotionTypeServiceTest {

    @Mock private lateinit var promotionTypeRepository: PromotionTypeRepository

    @InjectMocks private lateinit var adminPromotionTypeService: AdminPromotionTypeService

    @Nested
    @DisplayName("getPromotionTypes - 행사유형 목록 조회")
    inner class GetPromotionTypesTests {

        @Test
        @DisplayName("정상 조회 - 활성 행사유형 목록 반환")
        fun getPromotionTypes_success() {
            val types = listOf(
                createPromotionType(id = 1L, name = "시식", displayOrder = 1),
                createPromotionType(id = 2L, name = "시음", displayOrder = 2)
            )
            whenever(promotionTypeRepository.findByIsActiveTrueOrderByDisplayOrderAsc()).thenReturn(types)

            val result = adminPromotionTypeService.getPromotionTypes()

            assertThat(result).hasSize(2)
            assertThat(result[0].name).isEqualTo("시식")
            assertThat(result[1].name).isEqualTo("시음")
        }
    }

    @Nested
    @DisplayName("createPromotionType - 행사유형 생성")
    inner class CreatePromotionTypeTests {

        @Test
        @DisplayName("정상 생성 - 유효한 요청 -> 행사유형 반환")
        fun createPromotionType_success() {
            val request = PromotionTypeRequest(name = "시연", displayOrder = 6)
            whenever(promotionTypeRepository.existsByName("시연")).thenReturn(false)
            whenever(promotionTypeRepository.save(any<PromotionType>())).thenAnswer {
                val entity = it.getArgument<PromotionType>(0)
                PromotionType(id = 6L, name = entity.name, displayOrder = entity.displayOrder)
            }

            val result = adminPromotionTypeService.createPromotionType(request)

            assertThat(result.id).isEqualTo(6L)
            assertThat(result.name).isEqualTo("시연")
            assertThat(result.displayOrder).isEqualTo(6)
            assertThat(result.isActive).isTrue()
        }

        @Test
        @DisplayName("이름 중복 -> PromotionTypeDuplicateException")
        fun createPromotionType_duplicate() {
            val request = PromotionTypeRequest(name = "시식", displayOrder = 1)
            whenever(promotionTypeRepository.existsByName("시식")).thenReturn(true)

            assertThatThrownBy { adminPromotionTypeService.createPromotionType(request) }
                .isInstanceOf(PromotionTypeDuplicateException::class.java)
        }
    }

    @Nested
    @DisplayName("updatePromotionType - 행사유형 수정")
    inner class UpdatePromotionTypeTests {

        @Test
        @DisplayName("정상 수정 - 유효한 요청 -> 수정된 행사유형 반환")
        fun updatePromotionType_success() {
            val existing = createPromotionType(id = 1L, name = "시식", displayOrder = 1)
            whenever(promotionTypeRepository.findById(1L)).thenReturn(Optional.of(existing))
            whenever(promotionTypeRepository.existsByNameAndIdNot("시식행사", 1L)).thenReturn(false)

            val request = PromotionTypeRequest(name = "시식행사", displayOrder = 1)
            val result = adminPromotionTypeService.updatePromotionType(1L, request)

            assertThat(result.name).isEqualTo("시식행사")
        }

        @Test
        @DisplayName("미존재 ID -> PromotionTypeNotFoundException")
        fun updatePromotionType_notFound() {
            whenever(promotionTypeRepository.findById(999L)).thenReturn(Optional.empty())

            assertThatThrownBy {
                adminPromotionTypeService.updatePromotionType(999L, PromotionTypeRequest(name = "시연", displayOrder = 6))
            }.isInstanceOf(PromotionTypeNotFoundException::class.java)
        }

        @Test
        @DisplayName("이름 중복 (자기 자신 제외) -> PromotionTypeDuplicateException")
        fun updatePromotionType_duplicate() {
            val existing = createPromotionType(id = 2L, name = "시음", displayOrder = 2)
            whenever(promotionTypeRepository.findById(2L)).thenReturn(Optional.of(existing))
            whenever(promotionTypeRepository.existsByNameAndIdNot("시식", 2L)).thenReturn(true)

            assertThatThrownBy {
                adminPromotionTypeService.updatePromotionType(2L, PromotionTypeRequest(name = "시식", displayOrder = 2))
            }.isInstanceOf(PromotionTypeDuplicateException::class.java)
        }
    }

    @Nested
    @DisplayName("deletePromotionType - 행사유형 비활성화")
    inner class DeletePromotionTypeTests {

        @Test
        @DisplayName("정상 비활성화 - is_active=false로 변경")
        fun deletePromotionType_success() {
            val existing = createPromotionType(id = 5L, name = "기타", displayOrder = 5)
            whenever(promotionTypeRepository.findById(5L)).thenReturn(Optional.of(existing))

            adminPromotionTypeService.deletePromotionType(5L)

            assertThat(existing.isActive).isFalse()
        }

        @Test
        @DisplayName("미존재 ID -> PromotionTypeNotFoundException")
        fun deletePromotionType_notFound() {
            whenever(promotionTypeRepository.findById(999L)).thenReturn(Optional.empty())

            assertThatThrownBy { adminPromotionTypeService.deletePromotionType(999L) }
                .isInstanceOf(PromotionTypeNotFoundException::class.java)
        }
    }

    // Helper
    private fun createPromotionType(
        id: Long = 1L,
        name: String = "시식",
        displayOrder: Int = 1,
        isActive: Boolean = true
    ) = PromotionType(id = id, name = name, displayOrder = displayOrder, isActive = isActive)
}
