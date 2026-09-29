import { Alert, Space, Tag, Tooltip } from 'antd';
import type { ProductAppVisibility } from '@/api/product';

/**
 * 제품의 모바일 앱 노출/주문 진단 표기 공용 컴포넌트.
 *
 * 판정은 전부 백엔드(`ProductAppVisibilityEvaluator`)가 수행하며 화면은 표시만 담당한다 —
 * 프론트가 조건을 다시 계산하면 앱의 실제 검색 술어와 어긋날 수 있다.
 *
 * "제품상태는 판매중인데 앱에서 안 보인다" 문의가 운영자 선에서 종결되도록,
 * 사유(무엇이 문제인지)와 조치처(어느 시스템의 어느 값을 볼지)를 함께 노출한다.
 */

/** 목록 컬럼용 한 줄 배지. 사유는 tooltip 으로만 보여 컬럼 폭을 유지한다. */
export function ProductAppVisibilityTag({ visibility }: { visibility?: ProductAppVisibility }) {
  if (!visibility) return <>-</>;

  const { searchable, orderable, issues } = visibility;
  const tooltip =
    issues.length > 0 ? (
      <div>
        {issues.map((issue) => (
          <div key={issue.code} style={{ marginBottom: 4 }}>
            · {issue.message}
          </div>
        ))}
      </div>
    ) : null;

  if (!searchable) {
    return (
      <Tooltip title={tooltip}>
        <Tag color="red">검색불가</Tag>
      </Tooltip>
    );
  }
  if (!orderable) {
    return (
      <Tooltip title={tooltip}>
        <Tag color="orange">주문불가</Tag>
      </Tooltip>
    );
  }
  return <Tag color="green">정상</Tag>;
}

/** 상세 화면 최상단 진단 패널. 사유마다 조치처까지 펼쳐 보여준다. */
export function ProductAppVisibilityPanel({ visibility }: { visibility?: ProductAppVisibility }) {
  if (!visibility) return null;

  const { searchable, orderable, issues } = visibility;

  if (searchable && orderable) {
    return (
      <Alert
        type="success"
        showIcon
        style={{ marginBottom: 16 }}
        message="모바일 앱에서 정상 노출되는 제품입니다 (검색 가능 / 주문 가능)."
      />
    );
  }

  return (
    <Alert
      type={searchable ? 'warning' : 'error'}
      showIcon
      style={{ marginBottom: 16 }}
      message={
        <Space size={4} wrap>
          <span>모바일 앱 노출 상태:</span>
          <Tag color={searchable ? 'green' : 'red'}>
            제품검색 {searchable ? '노출' : '미노출'}
          </Tag>
          <Tag color={orderable ? 'green' : 'orange'}>
            주문담기 {orderable ? '가능' : '불가'}
          </Tag>
        </Space>
      }
      description={
        <ul style={{ margin: '8px 0 0', paddingLeft: 18 }}>
          {issues.map((issue) => (
            <li key={issue.code} style={{ marginBottom: 6 }}>
              <div>
                <Tag color={issue.scope === 'SEARCH' ? 'red' : 'orange'}>
                  {issue.scope === 'SEARCH' ? '검색 제외' : '주문 차단'}
                </Tag>
                {issue.message}
              </div>
              <div style={{ color: '#8c8c8c', marginTop: 2 }}>→ {issue.action}</div>
            </li>
          ))}
        </ul>
      }
    />
  );
}
