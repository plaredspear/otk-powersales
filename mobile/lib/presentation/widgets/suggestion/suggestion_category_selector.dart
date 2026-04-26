import 'package:flutter/material.dart';

import '../../../domain/entities/suggestion_form.dart';

/// 제안하기 분류 선택 위젯
class SuggestionCategorySelector extends StatelessWidget {
  const SuggestionCategorySelector({
    super.key,
    required this.selectedCategory,
    required this.onCategoryChanged,
  });

  final SuggestionCategory selectedCategory;
  final ValueChanged<SuggestionCategory> onCategoryChanged;

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        const Text(
          '분류 *',
          style: TextStyle(
            fontSize: 14,
            fontWeight: FontWeight.w500,
          ),
        ),
        const SizedBox(height: 8),

        // 신제품 제안
        RadioListTile<SuggestionCategory>(
          contentPadding: EdgeInsets.zero,
          visualDensity: VisualDensity.compact,
          title: const Text('신제품 제안'),
          value: SuggestionCategory.newProduct,
          groupValue: selectedCategory,
          onChanged: (value) {
            if (value != null) onCategoryChanged(value);
          },
        ),

        // 기존제품 상품가치향상
        RadioListTile<SuggestionCategory>(
          contentPadding: EdgeInsets.zero,
          visualDensity: VisualDensity.compact,
          title: const Text('기존제품 상품가치향상'),
          value: SuggestionCategory.existingProduct,
          groupValue: selectedCategory,
          onChanged: (value) {
            if (value != null) onCategoryChanged(value);
          },
        ),
      ],
    );
  }
}
