import 'package:flutter/material.dart';

/// 메뉴 그룹 (카테고리)
///
/// 전체메뉴의 그룹 단위. 아이콘 + 그룹명 + 하위 메뉴 아이템 목록으로 구성.
class MenuGroup {
  /// 그룹 식별자 (예: 'trade', 'order')
  final String id;

  /// 그룹 아이콘 (iconAsset 미지정 시 폴백)
  final IconData icon;

  /// 그룹 아이콘 이미지 에셋 경로 (레거시 Heroku icon_navN.png 정합)
  ///
  /// 지정 시 [icon] 대신 이 이미지를 렌더링한다.
  final String? iconAsset;

  /// 그룹 표시명 (예: "거래처", "주문")
  final String label;

  /// 하위 메뉴 아이템 목록
  final List<MenuItem> items;

  const MenuGroup({
    required this.id,
    required this.icon,
    this.iconAsset,
    required this.label,
    required this.items,
  });

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    if (other is! MenuGroup) return false;
    return other.id == id &&
        other.icon == icon &&
        other.iconAsset == iconAsset &&
        other.label == label &&
        _listEquals(other.items, items);
  }

  @override
  int get hashCode =>
      Object.hash(id, icon, iconAsset, label, Object.hashAll(items));

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

  /// 라우트 이동 시 전달할 인자 (예: 탭 화면의 초기 탭 인덱스)
  final Object? arguments;

  /// 라우트 이동이 아닌 특수 동작(예: 로그아웃) 항목 여부.
  /// route 가 없어도 기능이 동작하므로 미구현(흐린 색)으로 보이지 않도록 한다.
  final bool isAction;

  const MenuItem({
    required this.id,
    required this.label,
    this.route,
    this.arguments,
    this.isAction = false,
  });

  /// 구현 여부(라우트가 있거나 특수 동작 항목이면 구현된 것으로 간주)
  bool get isImplemented => route != null || isAction;

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    if (other is! MenuItem) return false;
    return other.id == id &&
        other.label == label &&
        other.route == route &&
        other.arguments == arguments &&
        other.isAction == isAction;
  }

  @override
  int get hashCode => Object.hash(id, label, route, arguments, isAction);

  @override
  String toString() =>
      'MenuItem(id: $id, label: $label, route: $route, arguments: $arguments, isAction: $isAction)';
}

/// List equality helper
bool _listEquals<T>(List<T> a, List<T> b) {
  if (a.length != b.length) return false;
  for (int i = 0; i < a.length; i++) {
    if (a[i] != b[i]) return false;
  }
  return true;
}
