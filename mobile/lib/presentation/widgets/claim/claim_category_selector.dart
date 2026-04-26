import 'package:flutter/material.dart';

import '../../../domain/entities/claim_category.dart';

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
        _CategoryField(
          label: '클레임 종류1 *',
          selectedName: selectedCategory?.name,
          placeholder: '종류선택',
          onTap: () => _showCategorySelector(context),
        ),
        const SizedBox(height: 16),

        // 클레임 종류2 선택
        _CategoryField(
          label: '클레임 종류2 *',
          selectedName: selectedSubcategory?.name,
          placeholder: hasCategory ? '종류선택' : '종류1을 먼저 선택하세요',
          enabled: hasCategory,
          onTap: hasCategory
              ? () => _showSubcategorySelector(context, subcategories)
              : null,
        ),

        // 선택된 종류 라벨 표시
        if (selectedCategory != null && selectedSubcategory != null) ...[
          const SizedBox(height: 16),
          Container(
            padding: const EdgeInsets.all(12),
            decoration: BoxDecoration(
              color: Colors.blue.shade50,
              borderRadius: BorderRadius.circular(4),
            ),
            child: Text(
              '${selectedCategory!.name} > ${selectedSubcategory!.name}',
              style: const TextStyle(
                fontSize: 14,
                fontWeight: FontWeight.w500,
              ),
            ),
          ),
        ],
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

/// 종류 선택 필드
class _CategoryField extends StatelessWidget {
  const _CategoryField({
    required this.label,
    required this.selectedName,
    required this.placeholder,
    this.enabled = true,
    this.onTap,
  });

  final String label;
  final String? selectedName;
  final String placeholder;
  final bool enabled;
  final VoidCallback? onTap;

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          label,
          style: const TextStyle(
            fontSize: 14,
            fontWeight: FontWeight.w500,
          ),
        ),
        const SizedBox(height: 8),
        ListTile(
          contentPadding: const EdgeInsets.symmetric(horizontal: 12),
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(4),
            side: BorderSide(
              color: enabled ? Colors.grey.shade300 : Colors.grey.shade200,
            ),
          ),
          title: Text(
            selectedName ?? placeholder,
            style: TextStyle(
              fontSize: 14,
              color: selectedName == null
                  ? Colors.grey.shade600
                  : Colors.black87,
            ),
          ),
          trailing: enabled
              ? const Icon(Icons.arrow_forward_ios, size: 16)
              : null,
          enabled: enabled,
          onTap: onTap,
        ),
      ],
    );
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
