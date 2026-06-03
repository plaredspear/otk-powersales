package com.otoki.powersales.savedsearch.service

import com.otoki.powersales.auth.permission.SfPermissionOperation
import com.otoki.powersales.auth.permission.SfPermissionResolver
import com.otoki.powersales.employee.repository.EmployeeRepository
import com.otoki.powersales.savedsearch.dto.request.SavedSearchCreateRequest
import com.otoki.powersales.savedsearch.dto.request.SavedSearchUpdateRequest
import com.otoki.powersales.savedsearch.entity.SavedSearch
import com.otoki.powersales.savedsearch.entity.SavedSearchScope
import com.otoki.powersales.savedsearch.exception.SavedSearchDuplicateNameException
import com.otoki.powersales.savedsearch.exception.SavedSearchForbiddenException
import com.otoki.powersales.savedsearch.exception.SavedSearchNotFoundException
import com.otoki.powersales.savedsearch.repository.SavedSearchRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.util.Optional

@DisplayName("SavedSearchService 테스트")
class SavedSearchServiceTest {

    private val savedSearchRepository: SavedSearchRepository = mockk(relaxed = true)
    private val employeeRepository: EmployeeRepository = mockk(relaxed = true)
    private val service = SavedSearchService(savedSearchRepository, employeeRepository)

    private val sharedEditKey = SfPermissionResolver.entityKey("saved_search", SfPermissionOperation.EDIT)
    private val withSharedEdit = setOf(sharedEditKey)
    private val noPermission = emptySet<String>()

    private fun savedSearch(
        id: Long = 1,
        ownerId: Long? = 10,
        scope: SavedSearchScope = SavedSearchScope.PRIVATE,
        name: String = "검색A",
        resourceKey: String = "promotion",
    ) = SavedSearch(
        resourceKey = resourceKey,
        name = name,
        scope = scope,
        ownerId = ownerId,
        filters = mapOf("keyword" to "라면"),
        sortOrder = 0,
        id = id,
    )

    @Nested
    @DisplayName("목록 조회")
    inner class ListTest {

        @Test
        @DisplayName("본인 PRIVATE 와 SHARED 가 함께 반환되고 editable 이 스코프/권한에 따라 설정된다")
        fun listVisible() {
            val mine = savedSearch(id = 1, ownerId = 10, scope = SavedSearchScope.PRIVATE, name = "내검색")
            val shared = savedSearch(id = 2, ownerId = 99, scope = SavedSearchScope.SHARED, name = "공용검색")
            every { savedSearchRepository.findVisible("promotion", 10) } returns listOf(mine, shared)
            every { employeeRepository.findAllById(any<List<Long>>()) } returns emptyList()

            val result = service.list("promotion", employeeId = 10, permissions = noPermission)

            assertThat(result).hasSize(2)
            // 본인 PRIVATE → editable true
            assertThat(result.first { it.id == 1L }.editable).isTrue()
            // SHARED + 권한 없음 → editable false
            assertThat(result.first { it.id == 2L }.editable).isFalse()
        }

        @Test
        @DisplayName("SHARED 는 saved_search EDIT 권한 보유 시 editable true")
        fun sharedEditableWithPermission() {
            val shared = savedSearch(id = 2, ownerId = 99, scope = SavedSearchScope.SHARED)
            every { savedSearchRepository.findVisible("promotion", 10) } returns listOf(shared)
            every { employeeRepository.findAllById(any<List<Long>>()) } returns emptyList()

            val result = service.list("promotion", employeeId = 10, permissions = withSharedEdit)

            assertThat(result.first().editable).isTrue()
        }

        @Test
        @DisplayName("시스템 기본 프리셋(owner=null SHARED)은 전 사용자에게 보이고 ownerName 은 null")
        fun systemPresetVisible() {
            val systemPreset = savedSearch(id = 3, ownerId = null, scope = SavedSearchScope.SHARED, name = "전체 행사 조회")
            every { savedSearchRepository.findVisible("promotion", 10) } returns listOf(systemPreset)
            every { employeeRepository.findAllById(any<List<Long>>()) } returns emptyList()

            val result = service.list("promotion", employeeId = 10, permissions = noPermission)

            assertThat(result).hasSize(1)
            assertThat(result.first().ownerId).isNull()
            assertThat(result.first().ownerName).isNull()
            // 권한 없으면 SHARED 라 editable false
            assertThat(result.first().editable).isFalse()
        }
    }

    @Nested
    @DisplayName("생성")
    inner class CreateTest {

        @Test
        @DisplayName("PRIVATE 생성은 권한 없이 가능하다")
        fun createPrivate() {
            val request = SavedSearchCreateRequest(
                resourceKey = "promotion", name = "내검색",
                scope = SavedSearchScope.PRIVATE, filters = mapOf("keyword" to "x"),
            )
            every {
                savedSearchRepository.existsByResourceKeyAndOwnerIdAndScopeAndName(
                    "promotion", 10, SavedSearchScope.PRIVATE, "내검색",
                )
            } returns false
            every { savedSearchRepository.save(any<SavedSearch>()) } answers { firstArg() }
            every { employeeRepository.findById(10) } returns Optional.empty()

            val result = service.create(request, employeeId = 10, permissions = noPermission)

            assertThat(result.scope).isEqualTo(SavedSearchScope.PRIVATE)
            assertThat(result.ownerId).isEqualTo(10)
        }

        @Test
        @DisplayName("SHARED 생성은 권한 미보유 시 403")
        fun createSharedForbidden() {
            val request = SavedSearchCreateRequest(
                resourceKey = "promotion", name = "공용", scope = SavedSearchScope.SHARED, filters = emptyMap(),
            )

            assertThatThrownBy {
                service.create(request, employeeId = 10, permissions = noPermission)
            }.isInstanceOf(SavedSearchForbiddenException::class.java)

            verify(exactly = 0) { savedSearchRepository.save(any<SavedSearch>()) }
        }

        @Test
        @DisplayName("동일 이름 중복 시 409")
        fun createDuplicate() {
            val request = SavedSearchCreateRequest(
                resourceKey = "promotion", name = "중복", scope = SavedSearchScope.PRIVATE, filters = emptyMap(),
            )
            every {
                savedSearchRepository.existsByResourceKeyAndOwnerIdAndScopeAndName(
                    "promotion", 10, SavedSearchScope.PRIVATE, "중복",
                )
            } returns true

            assertThatThrownBy {
                service.create(request, employeeId = 10, permissions = noPermission)
            }.isInstanceOf(SavedSearchDuplicateNameException::class.java)
        }
    }

    @Nested
    @DisplayName("수정")
    inner class UpdateTest {

        @Test
        @DisplayName("타인 PRIVATE 수정 시 403")
        fun updateOthersPrivate() {
            val entity = savedSearch(id = 1, ownerId = 99, scope = SavedSearchScope.PRIVATE)
            every { savedSearchRepository.findById(1) } returns Optional.of(entity)
            val request = SavedSearchUpdateRequest(name = "변경", filters = emptyMap())

            assertThatThrownBy {
                service.update(1, request, employeeId = 10, permissions = noPermission)
            }.isInstanceOf(SavedSearchForbiddenException::class.java)
        }

        @Test
        @DisplayName("본인 PRIVATE 수정 성공")
        fun updateOwnPrivate() {
            val entity = savedSearch(id = 1, ownerId = 10, scope = SavedSearchScope.PRIVATE, name = "원본")
            every { savedSearchRepository.findById(1) } returns Optional.of(entity)
            every { employeeRepository.findById(10) } returns Optional.empty()
            val request = SavedSearchUpdateRequest(name = "변경", filters = mapOf("keyword" to "y"), sortOrder = 3)

            val result = service.update(1, request, employeeId = 10, permissions = noPermission)

            assertThat(result.name).isEqualTo("변경")
            assertThat(result.sortOrder).isEqualTo(3)
        }

        @Test
        @DisplayName("미존재 id 수정 시 404")
        fun updateNotFound() {
            every { savedSearchRepository.findById(999) } returns Optional.empty()
            val request = SavedSearchUpdateRequest(name = "x", filters = emptyMap())

            assertThatThrownBy {
                service.update(999, request, employeeId = 10, permissions = noPermission)
            }.isInstanceOf(SavedSearchNotFoundException::class.java)
        }
    }

    @Nested
    @DisplayName("삭제")
    inner class DeleteTest {

        @Test
        @DisplayName("SHARED 삭제는 권한 보유 시 가능")
        fun deleteSharedWithPermission() {
            val entity = savedSearch(id = 1, ownerId = 99, scope = SavedSearchScope.SHARED)
            every { savedSearchRepository.findById(1) } returns Optional.of(entity)

            service.delete(1, employeeId = 10, permissions = withSharedEdit)

            verify { savedSearchRepository.delete(entity) }
        }

        @Test
        @DisplayName("SHARED 삭제는 권한 미보유 시 403")
        fun deleteSharedForbidden() {
            val entity = savedSearch(id = 1, ownerId = 99, scope = SavedSearchScope.SHARED)
            every { savedSearchRepository.findById(1) } returns Optional.of(entity)

            assertThatThrownBy {
                service.delete(1, employeeId = 10, permissions = noPermission)
            }.isInstanceOf(SavedSearchForbiddenException::class.java)

            verify(exactly = 0) { savedSearchRepository.delete(any<SavedSearch>()) }
        }
    }
}
