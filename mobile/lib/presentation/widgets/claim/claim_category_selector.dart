import 'package:flutter/material.dart';

import '../../../domain/entities/claim_category.dart';
import 'claim_form_row.dart';

/// 클레임 종류 선택 위젯 (종류1 + 종류2)
class ClaimCategorySelector extends StatelessWidget {
  const ClaimCategorySelector({
    super.key,
    required this.categories,
    required this.selectedCategory,
    required this.selectedSubcategory,
    required this.onCategorySelected,
    required this.onSubcategorySelected,
  });

  final List<ClaimCategory> categories;
  final ClaimCategory? selectedCategory;
  final ClaimSubcategory? selectedSubcategory;
  final ValueChanged<ClaimCategory> onCategorySelected;
  final ValueChanged<ClaimSubcategory> onSubcategorySelected;

  @override
  Widget build(BuildContext context) {
    final hasCategory = selectedCategory != null;
    final subcategories = selectedCategory?.subcategories ?? [];

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        // 클레임 종류1 선택
        ClaimFormRow(
          label: '클레임 종류1',
          isRequired: true,
          onTap: () => _showCategorySelector(context),
          trailing: const ClaimRowChevron(),
          below: ClaimValueText(
            value: selectedCategory?.name,
            placeholder: '종류선택',
          ),
        ),

        // 클레임 종류2 선택
        ClaimFormRow(
          label: '클레임 종류2',
          isRequired: true,
          enabled: hasCategory,
          onTap: hasCategory
              ? () => _showSubcategorySelector(context, subcategories)
              : null,
          trailing: hasCategory ? const ClaimRowChevron() : null,
          below: hasCategory
              ? ClaimValueText(
                  value: selectedSubcategory?.name,
                  placeholder: '종류선택',
                )
              : const Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      '종류1을 먼저 선택하세요',
                      style: TextStyle(
                        fontSize: 14,
                        color: ClaimFormColors.value,
                      ),
                    ),
                    SizedBox(height: 2),
                    Text(
                      '종류선택',
                      style: TextStyle(
                        fontSize: 14,
                        color: ClaimFormColors.placeholder,
                      ),
                    ),
                  ],
                ),
        ),
      ],
    );
  }

  Future<void> _showCategorySelector(BuildContext context) async {
    final selected = await showModalBottomSheet<ClaimCategory>(
      context: context,
      builder: (context) => _CategoryListSheet(
        title: '클레임 종류1 선택',
        items: categories,
        selectedItem: selectedCategory,
        itemBuilder: (category) => category.name,
      ),
    );

    if (selected != null) {
      onCategorySelected(selected);
    }
  }

  Future<void> _showSubcategorySelector(
    BuildContext context,
    List<ClaimSubcategory> subcategories,
  ) async {
    final selected = await showModalBottomSheet<ClaimSubcategory>(
      context: context,
      builder: (context) => _CategoryListSheet(
        title: '클레임 종류2 선택',
        items: subcategories,
        selectedItem: selectedSubcategory,
        itemBuilder: (subcategory) => subcategory.name,
      ),
    );

    if (selected != null) {
      onSubcategorySelected(selected);
    }
  }
}

/// 종류 목록 바텀시트
class _CategoryListSheet<T> extends StatelessWidget {
  const _CategoryListSheet({
    required this.title,
    required this.items,
    required this.selectedItem,
    required this.itemBuilder,
  });

  final String title;
  final List<T> items;
  final T? selectedItem;
  final String Function(T) itemBuilder;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(vertical: 16),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          // 헤더
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
            child: Text(
              title,
              style: const TextStyle(
                fontSize: 16,
                fontWeight: FontWeight.bold,
              ),
            ),
          ),
          const Divider(height: 1),

          // 목록
          Flexible(
            child: ListView.builder(
              shrinkWrap: true,
              itemCount: items.length,
              itemBuilder: (context, index) {
                final item = items[index];
                final isSelected = item == selectedItem;

                return ListTile(
                  title: Text(itemBuilder(item)),
                  trailing: isSelected
                      ? const Icon(Icons.check, color: Colors.blue)
                      : null,
                  onTap: () => Navigator.pop(context, item),
                );
              },
            ),
          ),
        ],
      ),
    );
  }
}
