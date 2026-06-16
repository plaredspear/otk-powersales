package com.otoki.powersales.domain.org.employee.sfsync

import com.otoki.powersales.domain.org.employee.entity.Employee
import com.otoki.powersales.domain.org.employee.entity.StaffReview
import com.otoki.powersales.domain.org.employee.repository.EmployeeRepository
import com.otoki.powersales.domain.org.employee.repository.StaffReviewRepository
import com.otoki.powersales.platform.common.enums.WorkingCategory1
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDate

@DisplayName("StaffReviewSyncService 테스트")
class StaffReviewSyncServiceTest {

    private val fetchClient: StaffReviewFetchClient = mockk()
    private val repository: StaffReviewRepository = mockk()
    private val employeeRepository: EmployeeRepository = mockk()

    private val service = StaffReviewSyncService(
        fetchClient = fetchClient,
        repository = repository,
        employeeRepository = employeeRepository,
    )

    private val savedSlot = slot<List<StaffReview>>()

    @BeforeEach
    fun setUp() {
        every { repository.saveAll(capture(savedSlot)) } answers { firstArg<List<StaffReview>>() }
        every { employeeRepository.findBySfidIn(any()) } returns emptyList()
        every { repository.findBySfidIn(any()) } returns emptyList()
    }

    @Test
    @DisplayName("빈 fetch 결과는 no-op (saveAll 미호출)")
    fun emptyFetchIsNoop() {
        val result = service.syncRecords(emptyList())

        assertThat(result.fetched).isEqualTo(0)
        verify(exactly = 0) { repository.saveAll(any<List<StaffReview>>()) }
    }

    @Test
    @DisplayName("기존 row 가 없으면 INSERT — 점수/근무유형/캐시 컬럼이 적재된다")
    fun insertsNewRecord() {
        val dto = dto(
            sfid = "a0X0000000000001AAA",
            name = "SR-0001",
            employeeTotalScore = 25.0,
            attendanceScore = 3.0,
            workingCategory1 = WorkingCategory1.DISPLAY,
        )
        every { fetchClient.fetch(any()) } returns listOf(dto)

        val result = service.sync()

        assertThat(result.inserted).isEqualTo(1)
        assertThat(result.updated).isEqualTo(0)
        val saved = savedSlot.captured.single()
        assertThat(saved.sfid).isEqualTo("a0X0000000000001AAA")
        assertThat(saved.name).isEqualTo("SR-0001")
        assertThat(saved.employeeTotalScore).isEqualTo(25.0)
        assertThat(saved.attendanceScore).isEqualTo(3.0)
        assertThat(saved.workingCategory1).isEqualTo(WorkingCategory1.DISPLAY)
    }

    @Test
    @DisplayName("기존 row 가 있으면 UPDATE — 동일 인스턴스의 점수가 갱신된다")
    fun updatesExistingRecord() {
        val existing = StaffReview(id = 7L, sfid = "a0X0000000000001AAA", name = "SR-0001").also {
            it.employeeTotalScore = 1.0
            it.attendanceScore = 1.0
        }
        every { repository.findBySfidIn(any()) } returns listOf(existing)

        val dto = dto(sfid = "a0X0000000000001AAA", name = "SR-0001", employeeTotalScore = 30.0, attendanceScore = 2.0)

        val result = service.syncRecords(listOf(dto))

        assertThat(result.updated).isEqualTo(1)
        assertThat(result.inserted).isEqualTo(0)
        val saved = savedSlot.captured.single()
        assertThat(saved.id).isEqualTo(7L)
        assertThat(saved.employeeTotalScore).isEqualTo(30.0)
        assertThat(saved.attendanceScore).isEqualTo(2.0)
    }

    @Test
    @DisplayName("사원 SF Id(employeeSfid) 로 Employee FK 를 resolve 하여 연결한다")
    fun resolvesEmployeeBySfid() {
        val employee = Employee(id = 100, sfid = "005EMP000000001AAA", employeeCode = "E100", name = "홍길동")
        every { employeeRepository.findBySfidIn(listOf("005EMP000000001AAA")) } returns listOf(employee)

        val dto = dto(sfid = "a0X0000000000001AAA", name = "SR-0001", employeeSfid = "005EMP000000001AAA")

        service.syncRecords(listOf(dto))

        assertThat(savedSlot.captured.single().employee).isSameAs(employee)
    }

    @Test
    @DisplayName("INSERT 시 사원 미매칭이면 employee=null 로 적재한다")
    fun insertsWithNullEmployeeWhenUnmatched() {
        val dto = dto(sfid = "a0X0000000000001AAA", name = "SR-0001", employeeSfid = "005NOMATCH00001AAA")

        service.syncRecords(listOf(dto))

        assertThat(savedSlot.captured.single().employee).isNull()
    }

    @Test
    @DisplayName("UPDATE 시 사원 미매칭이면 기존 employee FK 를 보존한다")
    fun preservesExistingEmployeeFkWhenUnmatchedOnUpdate() {
        val existingEmployee = Employee(id = 55, sfid = "005EXISTING0001AAA", employeeCode = "E55", name = "기존사원")
        val existing = StaffReview(id = 7L, sfid = "a0X0000000000001AAA", name = "SR-0001").also {
            it.employee = existingEmployee
        }
        every { repository.findBySfidIn(any()) } returns listOf(existing)

        val dto = dto(sfid = "a0X0000000000001AAA", name = "SR-0001", employeeSfid = "005NOMATCH00001AAA", employeeTotalScore = 77.0)

        service.syncRecords(listOf(dto))

        val saved = savedSlot.captured.single()
        assertThat(saved.employeeTotalScore).isEqualTo(77.0)
        assertThat(saved.employee).isSameAs(existingEmployee)
    }

    @Test
    @DisplayName("sfid(매칭 키) 부재 row 는 skip 한다")
    fun skipsRecordWithoutSfid() {
        val good = dto(sfid = "a0X0000000000001AAA", name = "SR-0001")
        val bad = dto(sfid = null, name = "SR-9999")

        val result = service.syncRecords(listOf(good, bad))

        assertThat(result.fetched).isEqualTo(2)
        assertThat(result.inserted).isEqualTo(1)
        assertThat(result.skipped).isEqualTo(1)
        assertThat(savedSlot.captured).hasSize(1)
    }

    @Test
    @DisplayName("동일 sfid 가 fetch 응답에 중복이면 마지막 값으로 dedupe 한다")
    fun dedupesBySfid() {
        val first = dto(sfid = "a0X0000000000001AAA", name = "SR-0001", employeeTotalScore = 1.0)
        val last = dto(sfid = "a0X0000000000001AAA", name = "SR-0001", employeeTotalScore = 2.0)

        val result = service.syncRecords(listOf(first, last))

        assertThat(result.inserted).isEqualTo(1)
        assertThat(savedSlot.captured.single().employeeTotalScore).isEqualTo(2.0)
    }

    private fun dto(
        sfid: String?,
        name: String?,
        employeeSfid: String? = null,
        employeeTotalScore: Double? = null,
        attendanceScore: Double? = null,
        workingCategory1: WorkingCategory1? = null,
    ) = StaffReviewFetchDto(
        sfid = sfid,
        name = name,
        employeeSfid = employeeSfid,
        employeeName = "홍길동",
        employeeCode = "E100",
        employeeType = "정규직",
        entryDate = LocalDate.of(2020, 1, 1),
        branchReviewSfid = null,
        employeeTotalScore = employeeTotalScore,
        jikwee = "사원",
        jobCode = "J01",
        firstDayOfMonth = LocalDate.of(2026, 4, 1),
        branch = "서울지점",
        costCenterCode = "1001",
        workingCategory1 = workingCategory1,
        workingCategory2 = null,
        workingCategory3 = null,
        displayEventGoalScore = null,
        priorityItemEventScore = null,
        productManageCallmentScore = null,
        instructionDisobedienceScore = null,
        accountPartnershipScore = null,
        attendanceScore = attendanceScore,
        clothesHygieneScore = null,
        educationEvaluationScore = null,
        createdBySfid = null,
        lastModifiedBySfid = null,
    )
}
