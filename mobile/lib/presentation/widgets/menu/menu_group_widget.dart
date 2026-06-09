import 'package:flutter/material.dart';
import '../../../core/theme/app_colors.dart';
import '../../../core/theme/app_spacing.dart';
import '../../../core/theme/app_typography.dart';
import '../../../domain/entities/menu_item.dart' as domain;

/// 메뉴 그룹 위젯
///
/// 아이콘 + 그룹명과 하위 메뉴 아이템 목록을 표시한다.
/// 그룹 간 구분선으로 시각적 분리를 제공한다.
class MenuGroupWidget extends StatelessWidget {
  /// 메뉴 그룹 데이터
  final domain.MenuGroup group;

  /// 메뉴 아이템 탭 콜백
  final void Function(domain.MenuItem item)? onItemTap;

  /// 마지막 그룹인지 여부 (구분선 표시 결정)
  final bool isLast;

  const MenuGroupWidget({
    super.key,
    required this.group,
    this.onItemTap,
    this.isLast = false,
  });

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        // 레거시 Heroku 정합: 좌측 그룹(아이콘+그룹명) | 우측 아이템 목록 2단 구성
        Padding(
          padding: const EdgeInsets.fromLTRB(
            AppSpacing.xl,
            AppSpacing.md,
            AppSpacing.xl,
            AppSpacing.md,
          ),
          child: Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              // 좌측: 아이콘 + 그룹명 (고정 폭)
              // 우측 아이템의 세로 패딩(AppSpacing.sm + 2)만큼 상단 패딩을 줘서
              // 우측 첫 번째 아이템 줄과 baseline 높이를 맞춘다.
              Padding(
                padding: const EdgeInsets.only(top: AppSpacing.sm + 2),
                child: SizedBox(
                  width: 110,
                  child: Row(
                    children: [
                    // 레거시 Heroku 정합 아이콘(icon_navN.png) 우선, 없으면 Material 폴백
                    SizedBox(
                      width: 20,
                      height: 20,
                      child: Center(
                        child: group.iconAsset != null
                            ? Image.asset(
                                group.iconAsset!,
                                width: 18,
                                height: 18,
                                fit: BoxFit.contain,
                              )
                            : Icon(
                                group.icon,
                                size: 20,
                                color: AppColors.textPrimary,
                              ),
                      ),
                    ),
                    const SizedBox(width: AppSpacing.sm),
                      Expanded(
                        child: Text(
                          group.label,
                          style: AppTypography.headlineSmall.copyWith(
                            fontSize: 15,
                            fontWeight: FontWeight.w700,
                          ),
                        ),
                      ),
                    ],
                  ),
                ),
              ),
              const SizedBox(width: AppSpacing.md),
              // 우측: 메뉴 아이템 목록
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: group.items
                      .map((item) => _MenuItemWidget(
                            item: item,
                            onTap: onItemTap != null
                                ? () => onItemTap!(item)
                                : null,
                          ))
                      .toList(),
                ),
              ),
            ],
          ),
        ),
        // 구분선
        if (!isLast)
          const Divider(
            height: 1,
            thickness: 1,
            color: AppColors.divider,
            indent: AppSpacing.xl,
            endIndent: AppSpacing.xl,
          ),
      ],
    );
  }
}

/// 개별 메뉴 아이템 위젯
class _MenuItemWidget extends StatelessWidget {
  final domain.MenuItem item;
  final VoidCallback? onTap;

  const _MenuItemWidget({
    required this.item,
    this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    return InkWell(
      onTap: onTap,
      child: Padding(
        padding: const EdgeInsets.symmetric(
          vertical: AppSpacing.sm + 2,
        ),
        child: Text(
          item.label,
          style: AppTypography.bodyMedium.copyWith(
            color: item.isImplemented
                ? AppColors.textPrimary
                : AppColors.textSecondary,
          ),
        ),
      ),
    );
  }
}
