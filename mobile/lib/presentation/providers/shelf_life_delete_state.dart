import '../../domain/entities/shelf_life_item.dart';

/// 유통기한 삭제 화면 상태
class ShelfLifeDeleteState {
  /// 로딩 상태
  final bool isLoading;

  /// 에러 메시지
  final String? errorMessage;

  /// 삭제할 유통기한 목록 (관리 화면에서 전달받은 목록)
  final List<ShelfLifeItem> items;

  /// 선택된 항목 ID 목록
  final Set<int> selectedIds;

  /// 삭제 완료 여부
  final bool isDeleted;

  const ShelfLifeDeleteState({
    this.isLoading = false,
    this.errorMessage,
    this.items = const [],
    this.selectedIds = const {},
    this.isDeleted = false,
  });

  /// 초기 상태
  factory ShelfLifeDeleteState.initial() {
    return const ShelfLifeDeleteState();
  }

  /// 로딩 상태로 전환
  ShelfLifeDeleteState toLoading() {
    return copyWith(isLoading: true, errorMessage: null);
  }

  /// 에러 상태로 전환
  ShelfLifeDeleteState toError(String message) {
    return copyWith(isLoading: false, errorMessage: message);
  }

  /// 만료된 항목 (D-DAY <= 0)
  List<ShelfLifeItem> get expiredItems =>
      items.where((item) => item.isExpired).toList()
        ..sort((a, b) => a.dDay.compareTo(b.dDay));

  /// 만료 전 항목 (D-DAY > 0)
  List<ShelfLifeItem> get activeItems =>
      items.where((item) => !item.isExpired).toList()
        ..sort((a, b) => a.dDay.compareTo(b.dDay));

  /// 선택된 항목 수
  int get selectedCount => selectedIds.length;

  /// 삭제 가능 여부 (1개 이상 선택)
  bool get canDelete => selectedIds.isNotEmpty;

  /// 전체 선택 여부
  bool get isAllSelected =>
      items.isNotEmpty && selectedIds.length == items.length;

  /// 만료 그룹 전체 선택 여부
  bool get isExpiredGroupSelected {
    final expiredIds = expiredItems.map((e) => e.id).toSet();
    return expiredIds.isNotEmpty && expiredIds.every(selectedIds.contains);
  }

  /// 만료 전 그룹 전체 선택 여부
  bool get isActiveGroupSelected {
    final activeIds = activeItems.map((e) => e.id).toSet();
    return activeIds.isNotEmpty && activeIds.every(selectedIds.contains);
  }

  ShelfLifeDeleteState copyWith({
    bool? isLoading,
    String? errorMessage,
    List<ShelfLifeItem>? items,
    Set<int>? selectedIds,
    bool? isDeleted,
  }) {
    return ShelfLifeDeleteState(
      isLoading: isLoading ?? this.isLoading,
      errorMessage: errorMessage,
      items: items ?? this.items,
      selectedIds: selectedIds ?? this.selectedIds,
      isDeleted: isDeleted ?? this.isDeleted,
    );
  }
}
