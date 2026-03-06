package com.otoki.internal.sap.service

import com.otoki.internal.sap.entity.User
import com.otoki.internal.sap.repository.UserRepository
import com.otoki.internal.sap.dto.SapEmployeeMasterRequest.ReqItem
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.LocalDate
import java.util.Optional

@ExtendWith(MockitoExtension::class)
@DisplayName("SapEmployeeMasterService 테스트")
class SapEmployeeMasterServiceTest {

    @Mock
    private lateinit var userRepository: UserRepository

    @InjectMocks
    private lateinit var sapEmployeeMasterService: SapEmployeeMasterService

    @Nested
    @DisplayName("sync - 신규 사원 등록")
    inner class NewEmployeeTests {

        @Test
        @DisplayName("정상 등록 - DB에 없는 사원코드 -> User + EmployeeMng 생성")
        fun sync_newEmployee_createsUser() {
            val items = listOf(createReqItem(employeeCode = "100234", employeeName = "홍길동", status = "1"))
            whenever(userRepository.findByEmployeeId("100234")).thenReturn(Optional.empty())
            whenever(userRepository.save(any<User>())).thenAnswer { it.getArgument<User>(0) }

            val result = sapEmployeeMasterService.sync(items)

            assertThat(result.successCount).isEqualTo(1)
            assertThat(result.failCount).isEqualTo(0)
            val captor = argumentCaptor<User>()
            verify(userRepository).save(captor.capture())
            val saved = captor.firstValue
            assertThat(saved.employeeId).isEqualTo("100234")
            assertThat(saved.name).isEqualTo("홍길동")
            assertThat(saved.status).isEqualTo("재직")
            assertThat(saved.appLoginActive).isTrue()
            assertThat(saved.passwordChangeRequired).isTrue()
        }

        @Test
        @DisplayName("신규 사원 필드 매핑 - birthDate, phone, startDate, costCenterCode")
        fun sync_newEmployee_fieldMapping() {
            val items = listOf(createReqItem(
                employeeCode = "100234",
                employeeName = "홍길동",
                status = "1",
                birthdate = "19900115",
                homePhone = "02-1234-5678",
                workPhone = "02-9876-5432",
                orgCode = "1111",
                startDate = "20150301"
            ))
            whenever(userRepository.findByEmployeeId("100234")).thenReturn(Optional.empty())
            whenever(userRepository.save(any<User>())).thenAnswer { it.getArgument<User>(0) }

            sapEmployeeMasterService.sync(items)

            val captor = argumentCaptor<User>()
            verify(userRepository).save(captor.capture())
            val saved = captor.firstValue
            assertThat(saved.birthDate).isEqualTo("19900115")
            assertThat(saved.homePhone).isEqualTo("02-1234-5678")
            assertThat(saved.workPhone).isEqualTo("02-9876-5432")
            assertThat(saved.costCenterCode).isEqualTo("1111")
            assertThat(saved.startDate).isEqualTo(LocalDate.of(2015, 3, 1))
        }
    }

    @Nested
    @DisplayName("sync - 기존 사원 업데이트")
    inner class ExistingEmployeeTests {

        @Test
        @DisplayName("기존 사원 업데이트 - 필드 변경, EmployeeMng 미변경")
        fun sync_existingEmployee_updates() {
            val existingUser = createUser(employeeId = "100234", name = "홍길동")
            val items = listOf(createReqItem(
                employeeCode = "100234",
                employeeName = "홍길동수정",
                status = "1",
                homePhone = "02-1111-2222",
                orgCode = "2222"
            ))
            whenever(userRepository.findByEmployeeId("100234")).thenReturn(Optional.of(existingUser))
            whenever(userRepository.save(any<User>())).thenAnswer { it.getArgument<User>(0) }

            val result = sapEmployeeMasterService.sync(items)

            assertThat(result.successCount).isEqualTo(1)
            assertThat(existingUser.name).isEqualTo("홍길동수정")
            assertThat(existingUser.homePhone).isEqualTo("02-1111-2222")
            assertThat(existingUser.costCenterCode).isEqualTo("2222")
        }
    }

    @Nested
    @DisplayName("sync - Status 코드 매핑")
    inner class StatusMappingTests {

        @Test
        @DisplayName("재직 - status=1 -> 재직, appLoginActive=true")
        fun sync_status1_active() {
            val items = listOf(createReqItem(employeeCode = "100001", employeeName = "테스트", status = "1"))
            whenever(userRepository.findByEmployeeId("100001")).thenReturn(Optional.empty())
            whenever(userRepository.save(any<User>())).thenAnswer { it.getArgument<User>(0) }

            sapEmployeeMasterService.sync(items)

            val captor = argumentCaptor<User>()
            verify(userRepository).save(captor.capture())
            assertThat(captor.firstValue.status).isEqualTo("재직")
            assertThat(captor.firstValue.appLoginActive).isTrue()
        }

        @Test
        @DisplayName("휴직 - status=2 -> 휴직, appLoginActive=false")
        fun sync_status2_inactive() {
            val items = listOf(createReqItem(employeeCode = "100001", employeeName = "테스트", status = "2"))
            whenever(userRepository.findByEmployeeId("100001")).thenReturn(Optional.empty())
            whenever(userRepository.save(any<User>())).thenAnswer { it.getArgument<User>(0) }

            sapEmployeeMasterService.sync(items)

            val captor = argumentCaptor<User>()
            verify(userRepository).save(captor.capture())
            assertThat(captor.firstValue.status).isEqualTo("휴직")
            assertThat(captor.firstValue.appLoginActive).isFalse()
        }

        @Test
        @DisplayName("퇴직 - status=3 -> 퇴직, appLoginActive=false")
        fun sync_status3_inactive() {
            val items = listOf(createReqItem(employeeCode = "100001", employeeName = "테스트", status = "3"))
            whenever(userRepository.findByEmployeeId("100001")).thenReturn(Optional.empty())
            whenever(userRepository.save(any<User>())).thenAnswer { it.getArgument<User>(0) }

            sapEmployeeMasterService.sync(items)

            val captor = argumentCaptor<User>()
            verify(userRepository).save(captor.capture())
            assertThat(captor.firstValue.status).isEqualTo("퇴직")
            assertThat(captor.firstValue.appLoginActive).isFalse()
        }

        @Test
        @DisplayName("잠금 - status=1, locking_flag=Y -> appLoginActive=false")
        fun sync_lockingFlagY_inactive() {
            val items = listOf(createReqItem(
                employeeCode = "100001", employeeName = "테스트",
                status = "1", lockingFlag = "Y"
            ))
            whenever(userRepository.findByEmployeeId("100001")).thenReturn(Optional.empty())
            whenever(userRepository.save(any<User>())).thenAnswer { it.getArgument<User>(0) }

            sapEmployeeMasterService.sync(items)

            val captor = argumentCaptor<User>()
            verify(userRepository).save(captor.capture())
            assertThat(captor.firstValue.appLoginActive).isFalse()
        }

        @Test
        @DisplayName("재직 + 잠금해제 - status=1, locking_flag=N -> appLoginActive=true")
        fun sync_lockingFlagN_active() {
            val items = listOf(createReqItem(
                employeeCode = "100001", employeeName = "테스트",
                status = "1", lockingFlag = "N"
            ))
            whenever(userRepository.findByEmployeeId("100001")).thenReturn(Optional.empty())
            whenever(userRepository.save(any<User>())).thenAnswer { it.getArgument<User>(0) }

            sapEmployeeMasterService.sync(items)

            val captor = argumentCaptor<User>()
            verify(userRepository).save(captor.capture())
            assertThat(captor.firstValue.appLoginActive).isTrue()
        }
    }

    @Nested
    @DisplayName("sync - 에러 처리")
    inner class ErrorHandlingTests {

        @Test
        @DisplayName("사원코드 누락 - 해당 레코드 실패, failCount=1")
        fun sync_missingEmployeeCode_fails() {
            val items = listOf(createReqItem(employeeCode = null, employeeName = "테스트", status = "1"))

            val result = sapEmployeeMasterService.sync(items)

            assertThat(result.successCount).isEqualTo(0)
            assertThat(result.failCount).isEqualTo(1)
            assertThat(result.errors[0].error).contains("employee_code")
        }

        @Test
        @DisplayName("부분 실패 - 3건 중 1건 에러 -> success=2, fail=1")
        fun sync_partialFailure() {
            val items = listOf(
                createReqItem(employeeCode = "100001", employeeName = "성공1", status = "1"),
                createReqItem(employeeCode = null, employeeName = "실패", status = "1"),
                createReqItem(employeeCode = "100003", employeeName = "성공2", status = "1")
            )
            whenever(userRepository.findByEmployeeId("100001")).thenReturn(Optional.empty())
            whenever(userRepository.findByEmployeeId("100003")).thenReturn(Optional.empty())
            whenever(userRepository.save(any<User>())).thenAnswer { it.getArgument<User>(0) }

            val result = sapEmployeeMasterService.sync(items)

            assertThat(result.successCount).isEqualTo(2)
            assertThat(result.failCount).isEqualTo(1)
            assertThat(result.errors[0].index).isEqualTo(1)
        }

        @Test
        @DisplayName("잘못된 날짜 형식 - 파싱 에러로 해당 레코드 실패")
        fun sync_invalidDateFormat_fails() {
            val items = listOf(createReqItem(
                employeeCode = "100001", employeeName = "테스트",
                status = "1", startDate = "2025-03-01"
            ))

            val result = sapEmployeeMasterService.sync(items)

            assertThat(result.failCount).isEqualTo(1)
        }

        @Test
        @DisplayName("알 수 없는 status 코드 - 해당 레코드 실패")
        fun sync_unknownStatusCode_fails() {
            val items = listOf(createReqItem(
                employeeCode = "100001", employeeName = "테스트", status = "9"
            ))

            val result = sapEmployeeMasterService.sync(items)

            assertThat(result.failCount).isEqualTo(1)
            assertThat(result.errors[0].error).contains("Unknown status code")
        }
    }

    @Nested
    @DisplayName("sync - 복수 건 처리")
    inner class BatchTests {

        @Test
        @DisplayName("복수 건 성공 - 신규 5건 + 기존 5건")
        fun sync_multipleMixed() {
            val newItems = (1..5).map {
                createReqItem(employeeCode = "NEW00$it", employeeName = "신규$it", status = "1")
            }
            val existingItems = (1..5).map {
                createReqItem(employeeCode = "EXT00$it", employeeName = "기존수정$it", status = "1")
            }
            val items = newItems + existingItems

            (1..5).forEach {
                whenever(userRepository.findByEmployeeId("NEW00$it")).thenReturn(Optional.empty())
            }
            (1..5).forEach {
                val user = createUser(employeeId = "EXT00$it", name = "기존$it")
                whenever(userRepository.findByEmployeeId("EXT00$it")).thenReturn(Optional.of(user))
            }
            whenever(userRepository.save(any<User>())).thenAnswer { it.getArgument<User>(0) }

            val result = sapEmployeeMasterService.sync(items)

            assertThat(result.successCount).isEqualTo(10)
            assertThat(result.failCount).isEqualTo(0)
        }
    }

    private fun createReqItem(
        employeeCode: String? = null,
        employeeName: String? = null,
        status: String? = null,
        sex: String? = null,
        homePhone: String? = null,
        workPhone: String? = null,
        startDate: String? = null,
        birthdate: String? = null,
        orgCode: String? = null,
        lockingFlag: String? = null
    ) = ReqItem(
        employeeCode = employeeCode,
        employeeName = employeeName,
        status = status,
        sex = sex,
        homePhone = homePhone,
        workPhone = workPhone,
        startDate = startDate,
        birthdate = birthdate,
        orgCode = orgCode,
        lockingFlag = lockingFlag
    )

    private fun createUser(
        id: Long = 1L,
        employeeId: String = "100234",
        name: String = "테스트"
    ) = User(
        id = id,
        employeeId = employeeId,
        name = name,
        status = "재직",
        appLoginActive = true
    )
}
