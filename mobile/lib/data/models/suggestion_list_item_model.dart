import '../../domain/entities/suggestion_form.dart';
import '../../domain/entities/suggestion_list_item.dart';

/// 제안/물류클레임 목록 항목 Model
///
/// 백엔드 `SuggestionListItem` JSON 대응.
class SuggestionListItemModel {
  final int id;
  final String proposalNumber;
  final String category;
  final String categoryName;
  final String title;
  final String createdAt;

  const SuggestionListItemModel({
    required this.id,
    required this.proposalNumber,
    required this.category,
    required this.categoryName,
    required this.title,
    required this.createdAt,
  });

  factory SuggestionListItemModel.fromJson(Map<String, dynamic> json) {
    return SuggestionListItemModel(
      id: (json['id'] as num).toInt(),
      proposalNumber: json['proposalNumber'] as String? ?? '',
      category: json['category'] as String? ?? 'NEW_PRODUCT',
      categoryName: json['categoryName'] as String? ?? '',
      title: json['title'] as String? ?? '',
      createdAt: json['createdAt'] as String? ?? '',
    );
  }

  SuggestionListItem toEntity() {
    return SuggestionListItem(
      id: id,
      proposalNumber: proposalNumber,
      category: SuggestionCategory.fromCode(category),
      categoryName: categoryName,
      title: title,
      createdAt: DateTime.tryParse(createdAt) ??
          DateTime.fromMillisecondsSinceEpoch(0),
    );
  }
}

/// Spring `Page<SuggestionListItem>` 대응 Model
class SuggestionListPageModel {
  final List<SuggestionListItemModel> content;
  final int totalElements;
  final int totalPages;
  final int number;
  final int size;
  final bool first;
  final bool last;

  const SuggestionListPageModel({
    required this.content,
    required this.totalElements,
    required this.totalPages,
    required this.number,
    required this.size,
    required this.first,
    required this.last,
  });

  factory SuggestionListPageModel.fromJson(Map<String, dynamic> json) {
    final contentList = (json['content'] as List<dynamic>? ?? const [])
        .map((e) => SuggestionListItemModel.fromJson(e as Map<String, dynamic>))
        .toList();
    return SuggestionListPageModel(
      content: contentList,
      totalElements: (json['totalElements'] as num?)?.toInt() ?? 0,
      totalPages: (json['totalPages'] as num?)?.toInt() ?? 0,
      number: (json['number'] as num?)?.toInt() ?? 0,
      size: (json['size'] as num?)?.toInt() ?? 0,
      first: json['first'] as bool? ?? true,
      last: json['last'] as bool? ?? true,
    );
  }
}
