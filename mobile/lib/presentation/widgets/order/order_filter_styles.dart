import 'package:flutter/material.dart';

import '../../../core/theme/app_colors.dart';
import '../../../core/theme/app_spacing.dart';
import '../../../core/theme/app_typography.dart';

/// 주문 현황 필터 영역의 공통 디자인 토큰/위젯.
///
/// Heroku 레거시(order/list.jsp + common.css)의 `.search_top` 검색 영역에 정합합니다.
/// - 행 높이 50px(`.opt_list > li input{height:50px}`)
/// - 행 구분선 `#F0F0F0`(`.opt_list > li{border-top:1px solid #F0F0F0}`)
/// - 굵은 밴드 10px `#F0F0F0`(`.bline`)
/// - 검색 버튼 노란 pill `#FFE40C`(`.type_btn button`)
class OrderFilterStyles {
  OrderFilterStyles._();

  /// 검색 영역 한 행의 높이 (레거시 50px)
  static const double rowHeight = 50;

  /// 레거시 구분선/밴드 색 (#F0F0F0)
  static const Color dividerColor = Color(0xFFF0F0F0);

  /// 필터/목록 사이 굵은 밴드 높이 (레거시 .bline = 10px)
  static const double bandHeight = 10;

  /// 드롭다운/날짜 값 텍스트 스타일
  static TextStyle get valueText =>
      AppTypography.bodyMedium.copyWith(color: AppColors.textPrimary);

  /// 라벨(납기일 등) 텍스트 스타일
  static TextStyle get labelText =>
      AppTypography.bodyMedium.copyWith(color: AppColors.textSecondary);
}

/// 검색 영역 두 컬럼 사이의 세로 구분선 (레거시 `.nth02 select:nth-child(2){border-left}`)
class OrderFilterVerticalDivider extends StatelessWidget {
  const OrderFilterVerticalDivider({super.key});

  @override
  Widget build(BuildContext context) {
    return Container(
      width: 1,
      height: 24,
      color: OrderFilterStyles.dividerColor,
    );
  }
}

/// 검색 영역 행 사이의 가로 구분선 (레거시 `.opt_list > li{border-top}`)
class OrderFilterRowDivider extends StatelessWidget {
  const OrderFilterRowDivider({super.key});

  @override
  Widget build(BuildContext context) {
    return Container(
      height: 1,
      color: OrderFilterStyles.dividerColor,
    );
  }
}

/// 필터 영역과 목록 사이의 굵은 회색 밴드 (레거시 `.bline`)
class OrderFilterBand extends StatelessWidget {
  const OrderFilterBand({super.key});

  @override
  Widget build(BuildContext context) {
    return Container(
      height: OrderFilterStyles.bandHeight,
      color: OrderFilterStyles.dividerColor,
    );
  }
}

/// 노란 pill 검색 버튼 (레거시 `.type_btn button` - #FFE40C, 둥근 pill, bold)
class OrderSearchButton extends StatelessWidget {
  final VoidCallback? onPressed;

  const OrderSearchButton({super.key, required this.onPressed});

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      height: 32,
      child: ElevatedButton(
        onPressed: onPressed,
        style: ElevatedButton.styleFrom(
          backgroundColor: AppColors.legacyYellow,
          foregroundColor: AppColors.onPrimary,
          disabledBackgroundColor: AppColors.border,
          disabledForegroundColor: AppColors.textTertiary,
          elevation: 0,
          padding: const EdgeInsets.symmetric(horizontal: AppSpacing.lg),
          minimumSize: const Size(57, 32),
          tapTargetSize: MaterialTapTargetSize.shrinkWrap,
          shape: const StadiumBorder(),
          textStyle: const TextStyle(
            fontSize: 14,
            fontWeight: FontWeight.w700,
          ),
        ),
        child: const Text('검색'),
      ),
    );
  }
}
