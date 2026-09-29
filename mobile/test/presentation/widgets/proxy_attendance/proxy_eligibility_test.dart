import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/presentation/widgets/proxy_attendance/proxy_register_sheet.dart';

/// 대리출근 등록 가능 판정 — 오늘·과거일 허용, 미래일 차단.
void main() {
  final now = DateTime(2026, 9, 29, 14, 30);

  group('proxyEligibility', () {
    test('오늘 -> 등록 가능', () {
      final result = proxyEligibility(DateTime(2026, 9, 29), now);

      expect(result, ProxyEligibility.ok);
      expect(result.canRegister, isTrue);
      expect(result.reason, isNull);
    });

    test('오후 5시 이후에도 오늘이면 등록 가능 (레거시 17시 제한 미적용)', () {
      final result = proxyEligibility(
        DateTime(2026, 9, 29),
        DateTime(2026, 9, 29, 18, 0),
      );

      expect(result, ProxyEligibility.ok);
    });

    test('과거일 -> 소급 등록 가능', () {
      expect(proxyEligibility(DateTime(2026, 9, 28), now), ProxyEligibility.ok);
      expect(proxyEligibility(DateTime(2026, 8, 1), now), ProxyEligibility.ok);
    });

    test('미래일 -> 차단 + 사유 문구', () {
      final result = proxyEligibility(DateTime(2026, 9, 30), now);

      expect(result, ProxyEligibility.future);
      expect(result.canRegister, isFalse);
      expect(result.reason, '미래 일정은 대리출근 등록할 수 없습니다.');
    });

    test('같은 날 이른 시각을 선택해도 시각은 판정에 영향 없음', () {
      final result = proxyEligibility(DateTime(2026, 9, 29, 0, 0), now);

      expect(result, ProxyEligibility.ok);
    });
  });
}
