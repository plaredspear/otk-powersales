/// 수량 표시 문자열 유틸.
///
/// 서버는 납품수량을 `"10 BOX"` / `"10 BOX (300 EA)"` 같은 **표시 문자열**로 내려준다
/// (`OrderRequestDetailMapper.formatConfirmQuantity` / `ClientOrderItemResponse.formatDeliveredQuantity`).
/// 납품 확정 전 라인은 `"0 BOX"` 가 되는데, 0 수량은 정보 가치가 없어 화면에서 표기하지 않는다
/// (2026-08-07 사용자 요청 — 내 주문 상세 / 거래처별 주문 상세의 주문 처리 현황).
library;

/// 수량 문자열에서 숫자 토큰을 뽑는 패턴 (천단위 `,` 는 토큰 경계로 취급 — "1,234.5" → "1", "234.5").
final RegExp _quantityNumberPattern = RegExp(r'\d+(?:\.\d+)?');

/// 수량 표시 문자열의 숫자가 전부 0 이면 빈 문자열, 아니면 원본을 그대로 반환한다.
///
/// `"0 BOX"` / `"0 BOX (0 EA)"` → `""`, `"1 BOX (12개)"` → 원본 유지.
/// 숫자가 없는 문자열은 판정 대상이 아니므로 원본을 유지한다.
String hideZeroQuantity(String quantity) {
  final numbers = _quantityNumberPattern.allMatches(quantity);
  if (numbers.isEmpty) return quantity;
  final allZero = numbers.every((m) => double.tryParse(m.group(0)!) == 0);
  return allZero ? '' : quantity;
}
