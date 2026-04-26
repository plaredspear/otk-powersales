import 'package:flutter/material.dart';
import 'package:mobile/app_router.dart';
import 'package:mobile/core/theme/app_colors.dart';
import 'package:mobile/core/theme/app_spacing.dart';
import 'package:mobile/core/theme/app_typography.dart';
import 'package:mobile/domain/entities/education_category.dart';
import 'package:mobile/presentation/widgets/education/education_category_card.dart';

/// 교육 자료 메인 화면
///
/// 4개의 교육 카테고리를 2x2 그리드로 표시합니다.
/// 카테고리를 선택하면 해당 카테고리의 게시물 목록 화면으로 이동합니다.
class EducationMainPage extends StatelessWidget {
  const EducationMainPage({super.key});

  /// 카테고리 선택 핸들러
  void _onCategoryTap(BuildContext context, EducationCategory category) {
    AppRouter.navigateTo(
      context,
      AppRouter.educationList,
      arguments: category,
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppColors.background,
      appBar: AppBar(
        backgroundColor: AppColors.otokiYellow,
        elevation: 0,
        title: Text(
          '교육 자료',
          style: AppTypography.headlineLarge.copyWith(
            color: AppColors.textPrimary,
          ),
        ),
        centerTitle: true,
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(AppSpacing.lg),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // 안내 문구
            Text(
              '카테고리를 선택하여\n교육 자료를 확인하세요',
              style: AppTypography.bodyLarge.copyWith(
                color: AppColors.textPrimary,
                height: 1.5,
              ),
            ),
            const SizedBox(height: AppSpacing.xxl),

            // 카테고리 그리드 (2x2)
            GridView.count(
              crossAxisCount: 2,
              shrinkWrap: true,
              physics: const NeverScrollableScrollPhysics(),
              mainAxisSpacing: AppSpacing.md,
              crossAxisSpacing: AppSpacing.md,
              childAspectRatio: 1.1,
              children: EducationCategory.values
                  .map(
                    (category) => EducationCategoryCard(
                      category: category,
                      onTap: () => _onCategoryTap(context, category),
                    ),
                  )
                  .toList(),
            ),

            const SizedBox(height: AppSpacing.xxl),

            // 하단 안내 문구
            Container(
              padding: const EdgeInsets.all(AppSpacing.md),
              decoration: BoxDecoration(
                color: AppColors.surface,
                borderRadius: BorderRadius.circular(AppSpacing.radiusMd),
                border: Border.all(color: AppColors.secondary, width: 1),
              ),
              child: Row(
                children: [
                  const Icon(
                    Icons.info_outline,
                    size: AppSpacing.iconSize,
                    color: AppColors.secondary,
                  ),
                  const SizedBox(width: AppSpacing.sm),
                  Expanded(
                    child: Text(
                      '교육 자료는 정기적으로 업데이트됩니다.',
                      style: AppTypography.bodySmall.copyWith(
                        color: AppColors.textSecondary,
                      ),
                    ),
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}
