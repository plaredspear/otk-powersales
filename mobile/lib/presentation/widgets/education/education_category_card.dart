import 'package:flutter/material.dart';
import 'package:mobile/core/theme/app_colors.dart';
import 'package:mobile/core/theme/app_spacing.dart';
import 'package:mobile/domain/entities/education_category.dart';

/// 교육 카테고리 카드 위젯
///
/// Heroku 레거시 교육 메인(.edu_main .nav_wrap li a) 디자인에 정합.
/// 흰 배경 + 드롭섀도 카드 안에 원형 일러스트 아이콘과 카테고리명을 표시한다.
/// (회색 원형 배경은 PNG 에셋 자체에 포함되어 있다.)
class EducationCategoryCard extends StatelessWidget {
  /// 카테고리
  final EducationCategory category;

  /// 카드 탭 콜백
  final VoidCallback? onTap;

  const EducationCategoryCard({
    super.key,
    required this.category,
    this.onTap,
  });

  /// 레거시 ico_edu*.png 표시 크기 (CSS width:72px)
  static const double _iconSize = 72;

  @override
  Widget build(BuildContext context) {
    return Material(
      color: AppColors.white,
      borderRadius: BorderRadius.circular(AppSpacing.homeCardRadius),
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(AppSpacing.homeCardRadius),
        child: Ink(
          decoration: BoxDecoration(
            color: AppColors.white,
            borderRadius: BorderRadius.circular(AppSpacing.homeCardRadius),
            boxShadow: AppSpacing.cardShadow,
          ),
          // 레거시 padding: 28px 0 20px
          padding: const EdgeInsets.only(top: 28, bottom: 20),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              // 카테고리 아이콘 (회색 원형 배경 포함 일러스트)
              Image.asset(
                category.iconPath,
                width: _iconSize,
                height: _iconSize,
                errorBuilder: (context, error, stackTrace) {
                  return const Icon(
                    Icons.folder_outlined,
                    size: _iconSize,
                    color: AppColors.textSecondary,
                  );
                },
              ),
              // 레거시 span padding-top: 12px
              const SizedBox(height: 12),

              // 카테고리 이름 (레거시 #333, 16px)
              Text(
                category.displayName,
                style: const TextStyle(
                  color: AppColors.legacyTextSub,
                  fontSize: 16,
                  height: 18 / 16,
                  fontWeight: FontWeight.w300,
                  letterSpacing: -0.8,
                ),
                textAlign: TextAlign.center,
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
              ),
            ],
          ),
        ),
      ),
    );
  }
}
