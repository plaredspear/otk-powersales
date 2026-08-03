package com.otoki.powersales.domain.org.employee.service

import com.otoki.powersales.domain.org.employee.dto.response.AppUninstalledFemaleStaffItem
import com.otoki.powersales.domain.org.employee.dto.response.AppUninstalledFemaleStaffSummary
import com.otoki.powersales.domain.org.employee.repository.EmployeeRepository
import com.otoki.powersales.platform.common.util.excel.ExcelResult
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * 앱 설치 현황 조회 서비스 (신규 도입 — 레거시 미존재).
 *
 * 레거시(SF/Heroku)에는 앱 설치 여부를 추적하는 화면·데이터가 없다. 신규 모바일 앱 배포 후
 * "아직 앱을 쓰지 않는 여사원" 에게 설치를 안내하기 위해 도입했다.
 *
 * ## 판정 원리 — 서버에 '설치' 신호는 없다
 *
 * 서버가 가진 것은 설치 여부가 아니라 **앱이 서버와 통신한 흔적**이다. 따라서 "미설치" 는
 * `앱 사용 흔적 0` 의 추정치이며, 설치만 하고 로그인하지 않은 사원도 여기에 포함된다.
 * 판정식과 그 근거(왜 `login_history` 가 아닌 `app_version_seen_at` 인지)는
 * [com.otoki.powersales.domain.org.employee.repository.EmployeeRepositoryCustom.findAppUninstalledFemaleStaff]
 * KDoc 참조.
 */
@Service
@Transactional(readOnly = true)
class EmployeeAppInstallService(
    private val employeeRepository: EmployeeRepository,
    private val appUninstalledFemaleStaffExcelExporter: AppUninstalledFemaleStaffExcelExporter,
) {

    /**
     * 앱 미설치 추정 여사원 집계 조회 — 미설치 인원 + 안내 대상 모수.
     *
     * 미설치 인원은 명단 조회 결과의 건수로 산출한다 (별도 count 쿼리를 두지 않음) — 화면 수치와
     * 엑셀 행 수가 같은 조회 결과에서 나와야 운영자가 두 값을 대조했을 때 어긋나지 않는다.
     */
    fun getUninstalledFemaleStaffSummary(): AppUninstalledFemaleStaffSummary =
        AppUninstalledFemaleStaffSummary(
            uninstalledCount = employeeRepository.findAppUninstalledFemaleStaff().size,
            targetCount = employeeRepository.countAppLoginTargetFemaleStaff(),
        )

    /**
     * 앱 미설치 추정 여사원 명단 엑셀 export — 사번 / 이름 / 지점명 3컬럼.
     *
     * 집계 수치와 동일한 조회를 사용하므로 행 수는 화면의 미설치 인원과 일치한다.
     * 파일명에 추출일(yyyyMMdd) 을 붙여 배포본의 기준일이 드러나게 한다.
     */
    fun exportUninstalledFemaleStaff(): ExcelResult {
        val items = employeeRepository.findAppUninstalledFemaleStaff()
            .map { AppUninstalledFemaleStaffItem.from(it) }
        val timestamp = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)
        return appUninstalledFemaleStaffExcelExporter.export(items, "앱미설치여사원_$timestamp.xlsx")
    }
}
