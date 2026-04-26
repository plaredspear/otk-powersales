import 'package:flutter/foundation.dart';

import 'safety_check_item.dart';

/// 안전점검 카테고리 엔티티 (V1)
class SafetyCheckCategory {
  /// 질문 번호 (1 = 장비착용, 2 = 예방사항)
  final int questionNum;

  /// 카테고리 제목
  final String title;

  /// 입력 유형 ("RADIO" / "CHECKBOX")
  final String inputType;

  /// 필수 여부
  final bool required;

  /// 선택지 (RADIO인 경우 ["예", "해당없음"])
  final List<String>? options;

  /// 항목 목록
  final List<SafetyCheckItem> items;

  const SafetyCheckCategory({
    required this.questionNum,
    required this.title,
    required this.inputType,
    required this.required,
    this.options,
    required this.items,
  });

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    return other is SafetyCheckCategory &&
        other.questionNum == questionNum &&
        other.title == title &&
        other.inputType == inputType &&
        other.required == required &&
        listEquals(other.options, options) &&
        listEquals(other.items, items);
  }

  @override
  int get hashCode {
    return Object.hash(
      questionNum,
      title,
      inputType,
      required,
      options != null ? Object.hashAll(options!) : null,
      Object.hashAll(items),
    );
  }

  @override
  String toString() =>
      'SafetyCheckCategory(questionNum: $questionNum, title: $title, inputType: $inputType, items: ${items.length})';
}
