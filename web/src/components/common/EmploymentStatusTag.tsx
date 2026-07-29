import { Tag } from 'antd';
import { getEmploymentStatusColor } from '@/constants/employmentStatus';

interface EmploymentStatusTagProps {
  /** 재직상태 문자열. 값 도메인은 `@/constants/employmentStatus` 주석 참조. */
  status: string | null | undefined;
  /**
   * 표시 형태. 기본 'tag'.
   * - 'tag'   : 색상 Tag. 재직상태가 화면의 주요 지표인 사원 목록/상세 계열.
   * - 'plain' : 색 없는 텍스트. 컬럼이 20개를 넘는 광폭 보고서형 테이블(진열스케줄마스터,
   *             여사원 배치 점검 등)에서 한 컬럼만 색을 입히면 시각적 노이즈가 되므로 사용한다.
   */
  variant?: 'tag' | 'plain';
  /** status 가 비었을 때 표시할 대체 텍스트. 기본 '-'. */
  fallback?: string;
}

/**
 * 재직상태를 일관된 색상으로 표시하는 공통 컴포넌트.
 *
 * 도입 전에는 동일한 색상 맵(`STATUS_TAG`)이 3개 페이지에 각각 복제되어 있었고,
 * 그 중 한 곳은 '퇴직(면직)' 키가 누락되어 무채색으로 표시되는 드리프트가 있었다.
 * 색상 맵은 `@/constants/employmentStatus` 단일 출처를 사용한다.
 *
 * 사용 예:
 *   render: (val: string | null) => <EmploymentStatusTag status={val} />
 */
export default function EmploymentStatusTag({
  status,
  variant = 'tag',
  fallback = '-',
}: EmploymentStatusTagProps) {
  if (!status) return <>{fallback}</>;
  if (variant === 'plain') return <>{status}</>;
  return <Tag color={getEmploymentStatusColor(status)}>{status}</Tag>;
}
