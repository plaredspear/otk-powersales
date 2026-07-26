import { Button } from 'antd';

interface Props {
  /** "더 보기" 클릭 시 고급 검색 모달을 연다. */
  onMore: () => void;
  /** 빠른 검색의 전체 결과 건수. 검색 전(null)이면 건수를 표시하지 않는다. */
  total: number | null;
}

/**
 * 행사마스터 거래처/대표제품 lookup 드롭다운 하단 푸터.
 *
 * 빠른 검색은 첫 페이지 20건(상세 인라인 편집은 10건)만 노출하므로, 전체 결과 건수를 알리고
 * 고급 검색으로 이어지는 진입로를 상시 제공한다. 좌측 "더 보기" / 우측 "총 X개" 양끝 배치.
 */
export default function LookupDropdownFooter({ onMore, total }: Props) {
  return (
    <div
      style={{
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
        padding: '6px 12px',
        borderTop: '1px solid #f0f0f0',
      }}
    >
      <Button
        type="link"
        size="small"
        style={{ padding: 0 }}
        // 드롭다운이 blur 로 닫히며 onClick 이 유실되지 않게 mousedown 기본 동작을 막는다.
        onMouseDown={(e) => e.preventDefault()}
        onClick={onMore}
      >
        더 보기
      </Button>
      {total != null && (
        <span style={{ color: '#8c8c8c', fontSize: 12 }}>총 {total.toLocaleString()}개</span>
      )}
    </div>
  );
}
