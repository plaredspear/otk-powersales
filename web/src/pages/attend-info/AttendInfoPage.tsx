import { Tabs, Typography } from 'antd';
import MonthlyWorkDetailTab from './components/monthly/MonthlyWorkDetailTab';
import WorkHistoryPeriodPage from './WorkHistoryPeriodPage';

const { Title } = Typography;

/**
 * 인사/근무 > 근무기간 조회.
 *
 * 'HR 적재 근무기간' 탭은 SAP HR 인바운드 적재 마스터라 기준정보 > HR 적재 근무기간
 * (`/settings/hr-attend-info`, HrAttendInfoPage) 으로 분리했다. 본 페이지는 조회 성격의 두 탭만 남는다.
 *
 * 메뉴 게이팅은 분리 후에도 `attend_info` 를 유지한다 — 월별 근무내역 탭이 쓰는 지점/사원 셀렉터
 * (`/admin/attend-infos/branches`, `/members`) 도 AdminAttendInfoController 의 `attend_info` READ 가드
 * 아래에 있어, 다른 entity 로 바꾸면 메뉴는 보이는데 셀렉터만 403 이 된다.
 */
export default function AttendInfoPage() {
  return (
    <div style={{ padding: 24 }}>
      <Title level={3} style={{ margin: '0 0 12px' }}>
        근무기간 조회
      </Title>
      <Tabs
        defaultActiveKey="monthly"
        items={[
          { key: 'monthly', label: '월별 근무내역 (개인)', children: <MonthlyWorkDetailTab /> },
          { key: 'period', label: '기간별 근무기간', children: <WorkHistoryPeriodPage /> },
        ]}
      />
    </div>
  );
}
