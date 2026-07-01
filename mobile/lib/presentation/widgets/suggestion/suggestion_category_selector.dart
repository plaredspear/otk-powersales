import 'package:flutter/material.dart';

import '../../../domain/entities/suggestion_form.dart';
import 'suggestion_logistics_claim_fields.dart';

/// 제안하기 분류 선택 위젯 (레거시 suggestWrite.jsp 정합)
///
/// "분류 *" 라벨 + 컴팩트 라디오를 풀폭 하단 구분선으로 구획한다.
/// 진입점에 따라 노출 분류가 달라진다.
/// - 물류 클레임 등록: 물류 클레임 1행만
/// - 제안하기(신제품 제안 등): 신제품 제안 / 기존제품 상품가치향상
class SuggestionCategorySelector extends StatelessWidget {
  const SuggestionCategorySelector({
    super.key,
    required this.selectedCategory,
    required this.onCategoryChanged,
    this.categories = const [SuggestionCategory.logisticsClaim],
  });

  final SuggestionCategory selectedCategory;
  final ValueChanged<SuggestionCategory> onCategoryChanged;

  /// 노출할 분류 목록 (진입점별로 다름)
  final List<SuggestionCategory> categories;

  @override
  Widget build(BuildContext context) {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.fromLTRB(16, 14, 16, 8),
      decoration: const BoxDecoration(
        border: Border(bottom: BorderSide(color: kSuggestionDividerColor)),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const SuggestionFieldLabel(text: '분류', required: true),
          const SizedBox(height: 6),
          RadioGroup<SuggestionCategory>(
            groupValue: selectedCategory,
            onChanged: (value) {
              if (value != null) onCategoryChanged(value);
            },
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                for (final category in categories)
                  _CategoryOption(
                    label: category.displayName,
                    value: category,
                    selectedCategory: selectedCategory,
                    onChanged: onCategoryChanged,
                  ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

/// 컴팩트 라디오 행 — 레거시의 좁은 간격 라디오 목록 정합.
class _CategoryOption extends StatelessWidget {
  const _CategoryOption({
    required this.label,
    required this.value,
    required this.selectedCategory,
    required this.onChanged,
  });

  final String label;
  final SuggestionCategory value;
  final SuggestionCategory selectedCategory;
  final ValueChanged<SuggestionCategory> onChanged;

  @override
  Widget build(BuildContext context) {
    final selected = value == selectedCategory;
    return InkWell(
      onTap: () => onChanged(value),
      child: Padding(
        padding: const EdgeInsets.symmetric(vertical: 2),
        child: Row(
          children: [
            Radio<SuggestionCategory>(
              value: value,
              activeColor: Colors.black87,
              visualDensity: VisualDensity.compact,
              materialTapTargetSize: MaterialTapTargetSize.shrinkWrap,
            ),
            const SizedBox(width: 6),
            Text(
              label,
              style: TextStyle(
                fontSize: 15,
                color: selected ? kSuggestionLabelColor : const Color(0xFF555555),
                fontWeight: selected ? FontWeight.w700 : FontWeight.w300,
              ),
            ),
          ],
        ),
      ),
    );
  }
}
