import { useEffect, useMemo, useState } from 'react';
import {
  Alert,
  Checkbox,
  DatePicker,
  Empty,
  Input,
  Select,
  Space,
  Tag,
  Typography,
} from 'antd';
import dayjs, { type Dayjs } from 'dayjs';
import { useAttendInfoBranches, useAttendInfoMembers } from '@/hooks/attend-info/useAttendInfo';
import type { TeamMember } from '@/api/team-schedule';
import { MonthlyMemberSelectPanel } from './components/monthly/MonthlyMemberSelectPanel';
import PeriodAccountBreakdown from './components/period/PeriodAccountBreakdown';

const { Text } = Typography;

// 조회 가능한 최대 기간(개월). backend WorkHistoryPeriodSummaryService.MAX_RANGE_MONTHS 와 정합.
const MAX_RANGE_MONTHS = 6;

export default function WorkHistoryPeriodPage() {
  const now = dayjs();
  // 시작/종료 년월 — 월 단위 캘린더(DatePicker picker="month") 로 선택.
  const [fromMonthDate, setFromMonthDate] = useState<Dayjs>(now);
  const [toMonthDate, setToMonthDate] = useState<Dayjs>(now);
  const [selectedCodes, setSelectedCodes] = useState<string[]>([]);
  const [keyword, setKeyword] = useState('');
  // 좌측 패널 여사원 목록의 조회 지점 (다중/전사 권한자 선택). 단일지점 사용자는 자동 채움.
  const [memberBranchCode, setMemberBranchCode] = useState<string | undefined>(undefined);
  // 좌측 패널에서 선택한 여사원 — 선택 시 우측이 거래처별 집계 뷰로 전환.
  const [selectedMember, setSelectedMember] = useState<TeamMember | undefined>(undefined);

  const { data: branches = [] } = useAttendInfoBranches();
  const branchOptions = useMemo(
    () => branches.map((b) => ({ value: b.branchCode, label: b.branchName })),
    [branches],
  );
  const allCodes = useMemo(() => branches.map((b) => b.branchCode), [branches]);
  const allSelected = allCodes.length > 0 && selectedCodes.length === allCodes.length;
  const someSelected = selectedCodes.length > 0 && !allSelected;
  const singleBranch = branches.length === 1;

  // 좌측 패널 여사원 목록 — 월별 근무내역(개인) 탭과 동일 소스 (퇴사/휴직 포함, attend_info 권한).
  // 단일지점 사용자는 자동선택 effect 가 지점을 채우기 전의 무의미한 fetch 를 억제 (이중 조회 방지).
  const membersEnabled = branches.length > 1 || memberBranchCode != null;
  const membersQuery = useAttendInfoMembers(memberBranchCode, { enabled: membersEnabled });

  // 단일지점 사용자는 본인 지점을 자동 선택. stale 지점 코드(권한 주체 변경)는 정리.
  useEffect(() => {
    if (branches.length === 0) return;
    if (memberBranchCode && !branches.some((b) => b.branchCode === memberBranchCode)) {
      setMemberBranchCode(undefined);
      setSelectedMember(undefined);
      return;
    }
    if (branches.length === 1 && !memberBranchCode) {
      setMemberBranchCode(branches[0].branchCode);
    }
  }, [branches, memberBranchCode]);

  // 패널 지점 변경 시 이전 지점에서 고른 여사원은 무효 — 선택 해제.
  const handleMemberBranchChange = (code: string) => {
    setMemberBranchCode(code);
    setSelectedMember(undefined);
  };

  // 같은 여사원 재클릭 시 선택 해제, 다른 여사원이면 교체. 선택 즉시 우측이 거래처별 뷰로 조회된다
  // (월별 근무내역 탭과 동일 — 조회 버튼 없이 선택이 곧 조회).
  const handleMemberSelect = (member: TeamMember) => {
    setSelectedMember((prev) => (prev?.employeeId === member.employeeId ? undefined : member));
  };

  const fromYm = fromMonthDate.format('YYYY-MM');
  const toYm = toMonthDate.format('YYYY-MM');
  const reversed = fromYm > toYm;
  // 시작~종료 포함 개월 수 (예: 2026-01 ~ 2026-06 = 6). day 성분 영향 없도록 월초 기준 diff.
  // 역순일 땐 의미 없으므로 0 처리.
  const inclusiveMonths = reversed
    ? 0
    : toMonthDate.startOf('month').diff(fromMonthDate.startOf('month'), 'month') + 1;
  const exceedsMax = inclusiveMonths > MAX_RANGE_MONTHS;
  const rangeInvalid = reversed || exceedsMax;

  const handleToggleAll = () => {
    setSelectedCodes(allSelected ? [] : allCodes);
  };

  return (
    <div>
      <div
        style={{
          marginBottom: 16,
          display: 'flex',
          flexWrap: 'wrap',
          alignItems: 'flex-end',
          justifyContent: 'space-between',
          gap: 8,
        }}
      >
        <Space wrap align="end">
          <Space direction="vertical" size={4}>
            <span>지점명:</span>
            {singleBranch ? (
              <Tag color="geekblue" style={{ fontSize: 14, padding: '5px 12px', marginInlineEnd: 0 }}>
                지점: {branches[0].branchName}
              </Tag>
            ) : (
              <Select
                mode="multiple"
                value={selectedCodes}
                onChange={(values) => setSelectedCodes(values as string[])}
                options={branchOptions}
                placeholder="지점 선택 (미선택 시 전체)"
                style={{ minWidth: 280, maxWidth: 480 }}
                maxTagCount="responsive"
                allowClear
                showSearch
                optionFilterProp="label"
                filterOption={(input, option) => (option?.label ?? '').toString().includes(input)}
                popupRender={(menu) => (
                  <>
                    <div style={{ padding: '4px 12px', borderBottom: '1px solid #f0f0f0' }}>
                      <Checkbox checked={allSelected} indeterminate={someSelected} onChange={handleToggleAll}>
                        전체 ({selectedCodes.length}/{allCodes.length})
                      </Checkbox>
                    </div>
                    {menu}
                  </>
                )}
                notFoundContent="항목 없음"
              />
            )}
          </Space>
          <Space direction="vertical" size={4}>
            <span>시작 년월:</span>
            <DatePicker
              picker="month"
              value={fromMonthDate}
              onChange={(v) => v && setFromMonthDate(v)}
              allowClear={false}
              disabledDate={(d) => d.year() < 2020 || d.year() > 2099}
              style={{ width: 130 }}
            />
          </Space>
          <Space direction="vertical" size={4}>
            <span>종료 년월:</span>
            <DatePicker
              picker="month"
              value={toMonthDate}
              onChange={(v) => v && setToMonthDate(v)}
              allowClear={false}
              disabledDate={(d) => d.year() < 2020 || d.year() > 2099}
              style={{ width: 130 }}
            />
          </Space>
          <Space direction="vertical" size={4}>
            <span>사번/이름:</span>
            <Input
              allowClear
              placeholder="사번 또는 이름"
              value={keyword}
              onChange={(e) => setKeyword(e.target.value)}
              style={{ width: 160 }}
            />
          </Space>
        </Space>
      </div>

      {reversed && (
        <Alert
          type="warning"
          message="시작 년월은 종료 년월보다 이후일 수 없습니다"
          style={{ marginBottom: 16 }}
        />
      )}
      {!reversed && exceedsMax && (
        <Alert
          type="warning"
          message={`조회 기간은 최대 ${MAX_RANGE_MONTHS}개월까지 가능합니다`}
          style={{ marginBottom: 16 }}
        />
      )}

      <div style={{ display: 'flex', gap: 16, alignItems: 'flex-start' }}>
        {/* 좌측: 월별 근무내역(개인) 탭과 동일한 여사원 선택 UI. 선택 시 우측이 거래처별 뷰로 전환. */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: 8, flexShrink: 0 }}>
          {!singleBranch && (
            <div style={{ width: 240 }}>
              <Text type="secondary" style={{ fontSize: 12 }}>
                지점명
              </Text>
              <Select
                size="small"
                style={{ width: '100%', marginTop: 4 }}
                placeholder="지점 선택"
                value={memberBranchCode}
                onChange={handleMemberBranchChange}
                options={branchOptions}
                showSearch
                optionFilterProp="label"
                notFoundContent="지점 없음"
              />
            </div>
          )}
          <MonthlyMemberSelectPanel
            members={membersQuery.data ?? []}
            isLoading={membersQuery.isFetching}
            selectedId={selectedMember?.employeeId}
            onSelect={handleMemberSelect}
          />
        </div>

        <div style={{ flex: 1, minWidth: 0 }}>
          {membersQuery.isError && (
            <Alert
              type="error"
              message={(membersQuery.error as Error)?.message ?? '여사원 목록 조회 실패'}
              style={{ marginBottom: 8 }}
            />
          )}

          {selectedMember ? (
            // 여사원 선택 즉시 거래처별 뷰 조회 (월별 근무내역 탭과 동일 — 조회 버튼 없음).
            <PeriodAccountBreakdown
              member={selectedMember}
              fromYearMonth={fromYm}
              toYearMonth={toYm}
              rangeInvalid={rangeInvalid}
            />
          ) : (
            // 여사원 미선택 안내 — 선택하면 즉시 조회된다.
            <Empty
              image={Empty.PRESENTED_IMAGE_SIMPLE}
              description="좌측에서 여사원을 선택하면 거래처별 근무내역을 조회합니다."
              style={{ marginTop: 48 }}
            />
          )}
        </div>
      </div>
    </div>
  );
}
