import 'package:flutter/material.dart';

import '../../../core/theme/app_colors.dart';
import '../../../domain/entities/safety_check_category.dart';
import 'safety_check_item_tile.dart';

/// 안전점검 카테고리 섹션 위젯
///
/// 카테고리 제목, 설명, 해당 카테고리의 체크리스트 항목을 표시합니다.
class SafetyCheckCategorySection extends StatelessWidget {
  /// 안전점검 카테고리
  final SafetyCheckCategory category;

  /// 각 항목의 체크 상태
  final Map<int, bool> checkedItems;

  /// 항목 체크 토글 콜백
  final ValueChanged<int> onToggle;

  const SafetyCheckCategorySection({
    super.key,
    required this.category,
    required this.checkedItems,
    required this.onToggle,
  });

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        // 카테고리 헤더
        Padding(
          padding: const EdgeInsets.fromLTRB(16, 20, 16, 4),
          child: Row(
            children: [
              Container(
                width: 4,
                height: 20,
                decoration: BoxDecoration(
                  color: AppColors.primary,
                  borderRadius: BorderRadius.circular(2),
                ),
              ),
              const SizedBox(width: 8),
              Expanded(
                child: Text(
                  category.name,
                  style: const TextStyle(
                    fontSize: 16,
                    fontWeight: FontWeight.bold,
                    color: AppColors.textPrimary,
                  ),
                ),
              ),
            ],
          ),
        ),
        // 구분선
        const Padding(
          padding: EdgeInsets.symmetric(horizontal: 16),
          child: Divider(color: AppColors.divider, height: 1),
        ),
        // 카테고리 설명 (안내 문구)
        if (category.description != null)
          Padding(
            padding: const EdgeInsets.fromLTRB(16, 8, 16, 4),
            child: Text(
              category.description!,
              style: const TextStyle(
                fontSize: 13,
                color: AppColors.textTertiary,
              ),
            ),
          ),
        // 체크리스트 항목들
        ...category.items.map(
          (item) => SafetyCheckItemTile(
            item: item,
            isChecked: checkedItems[item.id] ?? false,
            onToggle: onToggle,
          ),
        ),
      ],
    );
  }
}
