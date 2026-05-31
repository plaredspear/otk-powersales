import 'suggestion_form.dart';

/// 제안/물류클레임 목록 항목 엔티티
///
/// 백엔드 `SuggestionListItem` 대응. "내 제안/물류클레임 목록" 화면에 표시된다.
class SuggestionListItem {
  final int id;
  final String proposalNumber;
  final SuggestionCategory category;
  final String categoryName;
  final String title;
  final DateTime createdAt;

  const SuggestionListItem({
    required this.id,
    required this.proposalNumber,
    required this.category,
    required this.categoryName,
    required this.title,
    required this.createdAt,
  });

  /// 물류클레임 여부
  bool get isLogisticsClaim => category == SuggestionCategory.logisticsClaim;

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    return other is SuggestionListItem &&
        other.id == id &&
        other.proposalNumber == proposalNumber &&
        other.category == category &&
        other.categoryName == categoryName &&
        other.title == title &&
        other.createdAt == createdAt;
  }

  @override
  int get hashCode => Object.hash(
        id,
        proposalNumber,
        category,
        categoryName,
        title,
        createdAt,
      );
}
