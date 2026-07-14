// 클레임 목록 표시용 공용 상수 (테이블/카드 뷰 공유)

/**
 * status(SF DKRetail__Status__c) -> Tag 색/한글 라벨 매핑.
 * status = 코스모스(고객상담 처리 시스템) 전송상태. 신규→알라딘 전송상태(sfSendStatus)는
 * 별개 축이며 상세 화면에서 별도 표시한다.
 */
export const STATUS_TAG: Record<string, { color: string; label: string }> = {
  DRAFT: { color: 'default', label: '임시저장' },
  SENT: { color: 'green', label: '전송완료' },
  SEND_FAILED: { color: 'red', label: '전송실패' },
  // 레거시/구버전 표시값 호환
  SUBMITTED: { color: 'blue', label: '접수' },
  IN_PROGRESS: { color: 'orange', label: '처리중' },
  RESOLVED: { color: 'green', label: '처리완료' },
  REJECTED: { color: 'red', label: '반려' },
};

/** 대분류(categoryValue) -> 카드 플레이스홀더 배경색 (이미지 없는 클레임) */
export const CATEGORY_COLOR: Record<string, string> = {
  A: '#fa8c16',
  B: '#722ed1',
  C: '#eb2f96',
};

/** 분류 미상/미매칭 시 플레이스홀더 기본 색 */
export const CATEGORY_FALLBACK_COLOR = '#8c8c8c';
