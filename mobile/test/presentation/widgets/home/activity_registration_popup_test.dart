import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/app_router.dart';
import 'package:mobile/presentation/widgets/home/activity_registration_popup.dart';

void main() {
  group('ActivityRegistrationPopup.defaultMenuItems', () {
    test('레거시 GNB "활동 등록"과 동일하게 6개여야 한다', () {
      expect(ActivityRegistrationPopup.defaultMenuItems.length, 6);
    });

    test('레거시와 동일한 순서/라벨/라우트를 가져야 한다', () {
      final items = ActivityRegistrationPopup.defaultMenuItems;

      expect(items[0].label, '소비기한 관리');
      expect(items[0].route, AppRouter.productExpiration);

      expect(items[1].label, '현장 점검 등록');
      expect(items[1].route, AppRouter.inspectionRegister);

      expect(items[2].label, '제품 클레임 등록');
      expect(items[2].route, AppRouter.claimRegister);

      expect(items[3].label, '제품 클레임 조회');
      expect(items[3].route, AppRouter.claimList);

      expect(items[4].label, '물류 클레임 등록');
      expect(items[4].route, AppRouter.suggestionRegister);

      expect(items[5].label, '물류 클레임 조회');
      expect(items[5].route, AppRouter.suggestionList);
    });

    test('모든 메뉴가 연결된 라우트를 가져야 한다 (준비 중 항목 없음)', () {
      for (final item in ActivityRegistrationPopup.defaultMenuItems) {
        expect(item.route, isNotNull, reason: '${item.label} 라우트 누락');
      }
    });
  });
}
