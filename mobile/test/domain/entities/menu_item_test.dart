import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/menu_item.dart';

void main() {
  group('MenuItem', () {
    group('생성', () {
      test('모든 필드를 포함하여 생성 가능', () {
        // Given & When
        const menuItem = MenuItem(
          id: 'my-clients',
          label: '내 거래처',
          route: '/clients/my',
        );

        // Then
        expect(menuItem.id, equals('my-clients'));
        expect(menuItem.label, equals('내 거래처'));
        expect(menuItem.route, equals('/clients/my'));
      });

      test('route 없이 생성 가능 (null)', () {
        // Given & When
        const menuItem = MenuItem(
          id: 'coming-soon',
          label: '준비중',
        );

        // Then
        expect(menuItem.id, equals('coming-soon'));
        expect(menuItem.label, equals('준비중'));
        expect(menuItem.route, isNull);
      });
    });

    group('isImplemented', () {
      test('route가 있으면 true 반환', () {
        // Given
        const menuItem = MenuItem(
          id: 'implemented',
          label: '구현됨',
          route: '/implemented',
        );

        // When & Then
        expect(menuItem.isImplemented, isTrue);
      });

      test('route가 null이면 false 반환', () {
        // Given
        const menuItem = MenuItem(
          id: 'not-implemented',
          label: '미구현',
        );

        // When & Then
        expect(menuItem.isImplemented, isFalse);
      });
    });

    group('동등성 비교', () {
      test('동일한 값을 가진 객체는 같음', () {
        // Given
        const menuItem1 = MenuItem(
          id: 'test',
          label: '테스트',
          route: '/test',
        );
        const menuItem2 = MenuItem(
          id: 'test',
          label: '테스트',
          route: '/test',
        );

        // When & Then
        expect(menuItem1, equals(menuItem2));
        expect(menuItem1 == menuItem2, isTrue);
      });

      test('route가 null인 경우도 동등성 비교 가능', () {
        // Given
        const menuItem1 = MenuItem(id: 'test', label: '테스트');
        const menuItem2 = MenuItem(id: 'test', label: '테스트');

        // When & Then
        expect(menuItem1, equals(menuItem2));
      });

      test('id가 다르면 같지 않음', () {
        // Given
        const menuItem1 = MenuItem(
          id: 'test1',
          label: '테스트',
          route: '/test',
        );
        const menuItem2 = MenuItem(
          id: 'test2',
          label: '테스트',
          route: '/test',
        );

        // When & Then
        expect(menuItem1, isNot(equals(menuItem2)));
        expect(menuItem1 == menuItem2, isFalse);
      });

      test('label이 다르면 같지 않음', () {
        // Given
        const menuItem1 = MenuItem(
          id: 'test',
          label: '테스트1',
          route: '/test',
        );
        const menuItem2 = MenuItem(
          id: 'test',
          label: '테스트2',
          route: '/test',
        );

        // When & Then
        expect(menuItem1, isNot(equals(menuItem2)));
      });

      test('route가 다르면 같지 않음', () {
        // Given
        const menuItem1 = MenuItem(
          id: 'test',
          label: '테스트',
          route: '/test1',
        );
        const menuItem2 = MenuItem(
          id: 'test',
          label: '테스트',
          route: '/test2',
        );

        // When & Then
        expect(menuItem1, isNot(equals(menuItem2)));
      });

      test('같은 인스턴스는 항상 같음 (identical)', () {
        // Given
        const menuItem = MenuItem(
          id: 'test',
          label: '테스트',
          route: '/test',
        );

        // When & Then
        expect(menuItem, equals(menuItem));
        expect(identical(menuItem, menuItem), isTrue);
      });
    });

    group('hashCode', () {
      test('동일한 값을 가진 객체는 같은 hashCode', () {
        // Given
        const menuItem1 = MenuItem(
          id: 'test',
          label: '테스트',
          route: '/test',
        );
        const menuItem2 = MenuItem(
          id: 'test',
          label: '테스트',
          route: '/test',
        );

        // When & Then
        expect(menuItem1.hashCode, equals(menuItem2.hashCode));
      });

      test('다른 값을 가진 객체는 다른 hashCode (일반적으로)', () {
        // Given
        const menuItem1 = MenuItem(
          id: 'test1',
          label: '테스트',
          route: '/test',
        );
        const menuItem2 = MenuItem(
          id: 'test2',
          label: '테스트',
          route: '/test',
        );

        // When & Then
        // Note: 해시 충돌 가능성이 있지만 일반적으로 다름
        expect(menuItem1.hashCode, isNot(equals(menuItem2.hashCode)));
      });
    });

    group('toString', () {
      test('route가 있는 경우 관련 정보를 포함', () {
        // Given
        const menuItem = MenuItem(
          id: 'test',
          label: '테스트',
          route: '/test',
        );

        // When
        final result = menuItem.toString();

        // Then
        expect(result, contains('MenuItem'));
        expect(result, contains('test'));
        expect(result, contains('테스트'));
        expect(result, contains('/test'));
      });

      test('route가 null인 경우에도 정보를 포함', () {
        // Given
        const menuItem = MenuItem(
          id: 'test',
          label: '테스트',
        );

        // When
        final result = menuItem.toString();

        // Then
        expect(result, contains('MenuItem'));
        expect(result, contains('test'));
        expect(result, contains('테스트'));
        expect(result, contains('null'));
      });
    });
  });

  group('MenuGroup', () {
    const testIcon = Icons.home;

    group('생성', () {
      test('아이템 리스트와 함께 생성 가능', () {
        // Given & When
        const menuGroup = MenuGroup(
          id: 'trade',
          icon: testIcon,
          label: '거래처',
          items: [
            MenuItem(id: 'my-clients', label: '내 거래처', route: '/clients/my'),
            MenuItem(id: 'all-clients', label: '전체 거래처'),
          ],
        );

        // Then
        expect(menuGroup.id, equals('trade'));
        expect(menuGroup.icon, equals(testIcon));
        expect(menuGroup.label, equals('거래처'));
        expect(menuGroup.items.length, equals(2));
        expect(menuGroup.items[0].id, equals('my-clients'));
        expect(menuGroup.items[1].id, equals('all-clients'));
      });

      test('빈 아이템 리스트로 생성 가능', () {
        // Given & When
        const menuGroup = MenuGroup(
          id: 'empty',
          icon: testIcon,
          label: '빈 그룹',
          items: [],
        );

        // Then
        expect(menuGroup.items, isEmpty);
      });
    });

    group('동등성 비교', () {
      test('동일한 값을 가진 객체는 같음', () {
        // Given
        const menuGroup1 = MenuGroup(
          id: 'trade',
          icon: testIcon,
          label: '거래처',
          items: [
            MenuItem(id: 'my-clients', label: '내 거래처', route: '/clients/my'),
          ],
        );
        const menuGroup2 = MenuGroup(
          id: 'trade',
          icon: testIcon,
          label: '거래처',
          items: [
            MenuItem(id: 'my-clients', label: '내 거래처', route: '/clients/my'),
          ],
        );

        // When & Then
        expect(menuGroup1, equals(menuGroup2));
        expect(menuGroup1 == menuGroup2, isTrue);
      });

      test('id가 다르면 같지 않음', () {
        // Given
        const menuGroup1 = MenuGroup(
          id: 'trade1',
          icon: testIcon,
          label: '거래처',
          items: [],
        );
        const menuGroup2 = MenuGroup(
          id: 'trade2',
          icon: testIcon,
          label: '거래처',
          items: [],
        );

        // When & Then
        expect(menuGroup1, isNot(equals(menuGroup2)));
      });

      test('icon이 다르면 같지 않음', () {
        // Given
        const menuGroup1 = MenuGroup(
          id: 'trade',
          icon: Icons.home,
          label: '거래처',
          items: [],
        );
        const menuGroup2 = MenuGroup(
          id: 'trade',
          icon: Icons.settings,
          label: '거래처',
          items: [],
        );

        // When & Then
        expect(menuGroup1, isNot(equals(menuGroup2)));
      });

      test('label이 다르면 같지 않음', () {
        // Given
        const menuGroup1 = MenuGroup(
          id: 'trade',
          icon: testIcon,
          label: '거래처1',
          items: [],
        );
        const menuGroup2 = MenuGroup(
          id: 'trade',
          icon: testIcon,
          label: '거래처2',
          items: [],
        );

        // When & Then
        expect(menuGroup1, isNot(equals(menuGroup2)));
      });

      test('items가 다르면 같지 않음', () {
        // Given
        const menuGroup1 = MenuGroup(
          id: 'trade',
          icon: testIcon,
          label: '거래처',
          items: [
            MenuItem(id: 'my-clients', label: '내 거래처'),
          ],
        );
        const menuGroup2 = MenuGroup(
          id: 'trade',
          icon: testIcon,
          label: '거래처',
          items: [
            MenuItem(id: 'all-clients', label: '전체 거래처'),
          ],
        );

        // When & Then
        expect(menuGroup1, isNot(equals(menuGroup2)));
      });

      test('items 순서가 다르면 같지 않음', () {
        // Given
        const menuGroup1 = MenuGroup(
          id: 'trade',
          icon: testIcon,
          label: '거래처',
          items: [
            MenuItem(id: 'my-clients', label: '내 거래처'),
            MenuItem(id: 'all-clients', label: '전체 거래처'),
          ],
        );
        const menuGroup2 = MenuGroup(
          id: 'trade',
          icon: testIcon,
          label: '거래처',
          items: [
            MenuItem(id: 'all-clients', label: '전체 거래처'),
            MenuItem(id: 'my-clients', label: '내 거래처'),
          ],
        );

        // When & Then
        expect(menuGroup1, isNot(equals(menuGroup2)));
      });

      test('items 개수가 다르면 같지 않음', () {
        // Given
        const menuGroup1 = MenuGroup(
          id: 'trade',
          icon: testIcon,
          label: '거래처',
          items: [
            MenuItem(id: 'my-clients', label: '내 거래처'),
          ],
        );
        const menuGroup2 = MenuGroup(
          id: 'trade',
          icon: testIcon,
          label: '거래처',
          items: [
            MenuItem(id: 'my-clients', label: '내 거래처'),
            MenuItem(id: 'all-clients', label: '전체 거래처'),
          ],
        );

        // When & Then
        expect(menuGroup1, isNot(equals(menuGroup2)));
      });

      test('같은 인스턴스는 항상 같음 (identical)', () {
        // Given
        const menuGroup = MenuGroup(
          id: 'trade',
          icon: testIcon,
          label: '거래처',
          items: [],
        );

        // When & Then
        expect(menuGroup, equals(menuGroup));
        expect(identical(menuGroup, menuGroup), isTrue);
      });
    });

    group('hashCode', () {
      test('동일한 값을 가진 객체는 같은 hashCode', () {
        // Given
        const menuGroup1 = MenuGroup(
          id: 'trade',
          icon: testIcon,
          label: '거래처',
          items: [
            MenuItem(id: 'my-clients', label: '내 거래처'),
          ],
        );
        const menuGroup2 = MenuGroup(
          id: 'trade',
          icon: testIcon,
          label: '거래처',
          items: [
            MenuItem(id: 'my-clients', label: '내 거래처'),
          ],
        );

        // When & Then
        expect(menuGroup1.hashCode, equals(menuGroup2.hashCode));
      });

      test('다른 값을 가진 객체는 다른 hashCode (일반적으로)', () {
        // Given
        const menuGroup1 = MenuGroup(
          id: 'trade1',
          icon: testIcon,
          label: '거래처',
          items: [],
        );
        const menuGroup2 = MenuGroup(
          id: 'trade2',
          icon: testIcon,
          label: '거래처',
          items: [],
        );

        // When & Then
        // Note: 해시 충돌 가능성이 있지만 일반적으로 다름
        expect(menuGroup1.hashCode, isNot(equals(menuGroup2.hashCode)));
      });
    });

    group('toString', () {
      test('그룹 정보를 포함', () {
        // Given
        const menuGroup = MenuGroup(
          id: 'trade',
          icon: testIcon,
          label: '거래처',
          items: [
            MenuItem(id: 'my-clients', label: '내 거래처'),
            MenuItem(id: 'all-clients', label: '전체 거래처'),
          ],
        );

        // When
        final result = menuGroup.toString();

        // Then
        expect(result, contains('MenuGroup'));
        expect(result, contains('trade'));
        expect(result, contains('거래처'));
        expect(result, contains('2')); // items count
      });

      test('빈 아이템 리스트의 경우 개수 0 표시', () {
        // Given
        const menuGroup = MenuGroup(
          id: 'empty',
          icon: testIcon,
          label: '빈 그룹',
          items: [],
        );

        // When
        final result = menuGroup.toString();

        // Then
        expect(result, contains('MenuGroup'));
        expect(result, contains('empty'));
        expect(result, contains('빈 그룹'));
        expect(result, contains('0')); // items count
      });
    });
  });
}
