import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/core/constants/menu_constants.dart';
import 'package:mobile/domain/entities/menu_item.dart';

void main() {
  group('MenuConstants', () {
    test('menuGroups는 7개의 그룹을 가진다', () {
      expect(MenuConstants.menuGroups.length, 7);
    });

    test('itemCount는 13을 반환한다', () {
      expect(MenuConstants.itemCount, 13);
    });

    test('groupCount는 7을 반환한다', () {
      expect(MenuConstants.groupCount, 7);
    });

    test('각 그룹은 올바른 id와 label을 가진다', () {
      final groups = MenuConstants.menuGroups;

      expect(groups[0].id, 'trade');
      expect(groups[0].label, '거래처');

      expect(groups[1].id, 'order');
      expect(groups[1].label, '주문');

      expect(groups[2].id, 'product');
      expect(groups[2].label, '제품');

      expect(groups[3].id, 'sales');
      expect(groups[3].label, '매출 현황');

      expect(groups[4].id, 'field');
      expect(groups[4].label, '현장톡');

      expect(groups[5].id, 'community');
      expect(groups[5].label, '커뮤니티');

      expect(groups[6].id, 'mypage');
      expect(groups[6].label, '마이페이지');
    });

    test('행사 매출은 route가 /pos-sales이다', () {
      final salesGroup = MenuConstants.menuGroups
          .firstWhere((group) => group.id == 'sales');
      final posSalesItem = salesGroup.items
          .firstWhere((item) => item.id == 'pos-sales');

      expect(posSalesItem.label, '행사 매출');
      expect(posSalesItem.route, '/pos-sales');
      expect(posSalesItem.isImplemented, true);
    });

    test('비밀번호 변경은 route가 /change-password이다', () {
      final mypageGroup = MenuConstants.menuGroups
          .firstWhere((group) => group.id == 'mypage');
      final changePasswordItem = mypageGroup.items
          .firstWhere((item) => item.id == 'change-password');

      expect(changePasswordItem.label, '비밀번호 변경');
      expect(changePasswordItem.route, '/change-password');
      expect(changePasswordItem.isImplemented, true);
    });

    test('미구현 항목들은 route가 null이다', () {
      // 구현된 항목 목록
      final implementedRoutes = {
        '/pos-sales',
        '/change-password',
        '/my-stores',
      };

      // 모든 아이템을 순회하며 확인
      final allItems = MenuConstants.menuGroups
          .expand((group) => group.items)
          .toList();

      // 구현된 항목만 route가 있어야 함
      final itemsWithRoute = allItems.where((item) => item.route != null);
      expect(itemsWithRoute.length, 3);

      for (final item in itemsWithRoute) {
        expect(implementedRoutes.contains(item.route), true,
            reason: '${item.label}의 route는 구현된 route 목록에 있어야 합니다');
      }

      // 나머지 10개 항목은 route가 null
      final itemsWithoutRoute = allItems.where((item) => item.route == null);
      expect(itemsWithoutRoute.length, 10);

      for (final item in itemsWithoutRoute) {
        expect(item.isImplemented, false,
            reason: '${item.label}은 미구현 상태여야 합니다');
      }
    });

    test('모든 그룹은 최소 1개 이상의 아이템을 가진다', () {
      for (final group in MenuConstants.menuGroups) {
        expect(group.items.isNotEmpty, true,
            reason: '${group.label} 그룹은 최소 1개 이상의 아이템을 가져야 합니다');
      }
    });

    group('각 그룹별 아이템 개수 검증', () {
      test('거래처 그룹은 1개의 아이템을 가진다', () {
        final tradeGroup = MenuConstants.menuGroups
            .firstWhere((group) => group.id == 'trade');
        expect(tradeGroup.items.length, 1);
        expect(tradeGroup.items[0].id, 'my-stores');
        expect(tradeGroup.items[0].label, '내 거래처');
      });

      test('주문 그룹은 2개의 아이템을 가진다', () {
        final orderGroup = MenuConstants.menuGroups
            .firstWhere((group) => group.id == 'order');
        expect(orderGroup.items.length, 2);
        expect(orderGroup.items[0].id, 'orders');
        expect(orderGroup.items[0].label, '내 주문 현황');
        expect(orderGroup.items[1].id, 'orders-by-store');
        expect(orderGroup.items[1].label, '거래처별 주문 현황');
      });

      test('제품 그룹은 1개의 아이템을 가진다', () {
        final productGroup = MenuConstants.menuGroups
            .firstWhere((group) => group.id == 'product');
        expect(productGroup.items.length, 1);
        expect(productGroup.items[0].id, 'expiry');
        expect(productGroup.items[0].label, '유통기한 관리');
      });

      test('매출 현황 그룹은 2개의 아이템을 가진다', () {
        final salesGroup = MenuConstants.menuGroups
            .firstWhere((group) => group.id == 'sales');
        expect(salesGroup.items.length, 2);
        expect(salesGroup.items[0].id, 'pos-sales');
        expect(salesGroup.items[0].label, '행사 매출');
        expect(salesGroup.items[1].id, 'monthly-sales');
        expect(salesGroup.items[1].label, '월 매출');
      });

      test('현장톡 그룹은 3개의 아이템을 가진다', () {
        final fieldGroup = MenuConstants.menuGroups
            .firstWhere((group) => group.id == 'field');
        expect(fieldGroup.items.length, 3);
        expect(fieldGroup.items[0].id, 'field-check');
        expect(fieldGroup.items[0].label, '현장 점검');
        expect(fieldGroup.items[1].id, 'claims');
        expect(fieldGroup.items[1].label, '클레임 등록');
        expect(fieldGroup.items[2].id, 'suggestions');
        expect(fieldGroup.items[2].label, '제안하기');
      });

      test('커뮤니티 그룹은 2개의 아이템을 가진다', () {
        final communityGroup = MenuConstants.menuGroups
            .firstWhere((group) => group.id == 'community');
        expect(communityGroup.items.length, 2);
        expect(communityGroup.items[0].id, 'education');
        expect(communityGroup.items[0].label, '교육');
        expect(communityGroup.items[1].id, 'notices');
        expect(communityGroup.items[1].label, '공지사항');
      });

      test('마이페이지 그룹은 2개의 아이템을 가진다', () {
        final mypageGroup = MenuConstants.menuGroups
            .firstWhere((group) => group.id == 'mypage');
        expect(mypageGroup.items.length, 2);
        expect(mypageGroup.items[0].id, 'my-schedule');
        expect(mypageGroup.items[0].label, '내 일정/등록');
        expect(mypageGroup.items[1].id, 'change-password');
        expect(mypageGroup.items[1].label, '비밀번호 변경');
      });
    });
  });
}
