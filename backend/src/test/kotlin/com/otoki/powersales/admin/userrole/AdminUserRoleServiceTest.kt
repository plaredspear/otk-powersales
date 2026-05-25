package com.otoki.powersales.admin.userrole

import com.otoki.powersales.auth.entity.UserRole
import com.otoki.powersales.auth.repository.UserRoleRepository
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("AdminUserRoleService 테스트")
class AdminUserRoleServiceTest {

    private val repository: UserRoleRepository = mockk()
    private val service = AdminUserRoleService(repository)

    private fun role(id: Long, name: String, developerName: String? = null, parentId: Long? = null): UserRole =
        UserRole(
            id = id,
            name = name,
            developerName = developerName,
            parentUserRoleId = parentId,
        )

    @Test
    @DisplayName("getTree - 부모-자식 트리 + parentName 채움 + name 정렬")
    fun getTree_buildsHierarchy() {
        val ceo = role(1, "회장님", "CEO")
        val rep = role(2, "사장님", "representative", parentId = 1)
        val hq = role(3, "영업본부", "G0091", parentId = 2)
        val be = role(4, "BE팀", "GXA11", parentId = 3)
        val cvs = role(5, "CVS사업부", "CVS", parentId = 3)
        every { repository.findAll() } returns listOf(ceo, rep, hq, cvs, be) // 순서 무관

        val tree = service.getTree()

        assertThat(tree).hasSize(1)
        val rootNode = tree.first()
        assertThat(rootNode.userRoleId).isEqualTo(1)
        assertThat(rootNode.name).isEqualTo("회장님")
        assertThat(rootNode.parentName).isNull()

        val repNode = rootNode.children.first()
        assertThat(repNode.parentName).isEqualTo("회장님")

        val hqNode = repNode.children.first()
        // BE팀 (B...) 이 CVS사업부 (C...) 보다 name asc 정렬에서 앞
        assertThat(hqNode.children.map { it.name }).containsExactly("BE팀", "CVS사업부")
    }

    @Test
    @DisplayName("getTree - 빈 결과는 빈 리스트")
    fun getTree_empty() {
        every { repository.findAll() } returns emptyList()
        assertThat(service.getTree()).isEmpty()
    }

    @Test
    @DisplayName("getTree - parent 가 다중 root 인 경우 모두 반환")
    fun getTree_multipleRoots() {
        every { repository.findAll() } returns listOf(
            role(10, "RootA"),
            role(20, "RootB"),
        )
        val tree = service.getTree()
        assertThat(tree).hasSize(2)
        assertThat(tree.map { it.name }).containsExactly("RootA", "RootB")
    }
}
