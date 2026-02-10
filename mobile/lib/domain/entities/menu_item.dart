import 'package:flutter/material.dart';

/// 메뉴 그룹 (카테고리)
///
/// 전체메뉴의 그룹 단위. 아이콘 + 그룹명 + 하위 메뉴 아이템 목록으로 구성.
class MenuGroup {
  /// 그룹 식별자 (예: 'trade', 'order')
  final String id;

  /// 그룹 아이콘
  final IconData icon;

  /// 그룹 표시명 (예: "거래처", "주문")
  final String label;

  /// 하위 메뉴 아이템 목록
  final List<MenuItem> items;

  const MenuGroup({
    required this.id,
    required this.icon,
    required this.label,
    required this.items,
  });

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    if (other is! MenuGroup) return false;
    return other.id == id &&
        other.icon == icon &&
        other.label == label &&
        _listEquals(other.items, items);
  }

  @override
  int get hashCode => Object.hash(id, icon, label, Object.hashAll(items));

  @override
  String toString() =>
      'MenuGroup(id: $id, label: $label, items: ${items.length})';
}

/// 개별 메뉴 아이템
///
/// 메뉴 그룹 내의 개별 항목. 탭 시 route로 이동하며,
/// route가 null이면 미구현(준비 중) 상태.
class MenuItem {
  /// 메뉴 식별자
  final String id;

  /// 메뉴 표시명 (예: "내 거래처")
  final String label;

  /// 이동할 라우트 경로 (null이면 미구현)
  final String? route;

  const MenuItem({
    required this.id,
    required this.label,
    this.route,
  });

  /// 구현 여부
  bool get isImplemented => route != null;

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    if (other is! MenuItem) return false;
    return other.id == id && other.label == label && other.route == route;
  }

  @override
  int get hashCode => Object.hash(id, label, route);

  @override
  String toString() => 'MenuItem(id: $id, label: $label, route: $route)';
}

/// List equality helper
bool _listEquals<T>(List<T> a, List<T> b) {
  if (a.length != b.length) return false;
  for (int i = 0; i < a.length; i++) {
    if (a[i] != b[i]) return false;
  }
  return true;
}
