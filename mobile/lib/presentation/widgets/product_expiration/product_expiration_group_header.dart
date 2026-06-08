import 'package:flutter/material.dart';

import '../../../core/theme/app_colors.dart';
import '../../../core/theme/app_typography.dart';

/// 유통기한 그룹 헤더 위젯
///
/// 레거시(`product/expiration/list.jsp` `bull_circle`) 정합:
/// "유통기한 지남" (빨간 점·빨강 #DC2C34) / "유통기한 전" (회색 점·회색 #999)
class ProductExpirationGroupHeader extends StatelessWidget {
  /// 만료 그룹인지 여부
  final bool isExpired;

  /// 그룹 내 항목 수
  final int count;

  const ProductExpirationGroupHeader({
    super.key,
    required this.isExpired,
    required this.count,
  });

  // 레거시 color_gray = #999
  static const Color _gray = Color(0xFF999999);

  // 레거시 `top_box + ul` 구분선 = 1px #CCC (헤더 바로 아래 라인)
  static const Color _headerBorder = Color(0xFFCCCCCC);

  @override
  Widget build(BuildContext context) {
    final color = isExpired ? AppColors.legacyDanger : _gray;

    return Container(
      height: 50,
      padding: const EdgeInsets.symmetric(horizontal: 20),
      alignment: Alignment.centerLeft,
      decoration: const BoxDecoration(
        border: Border(bottom: BorderSide(color: _headerBorder)),
      ),
      child: Row(
        children: [
          // 상태 점 (bull_circle)
          Container(
            width: 9,
            height: 9,
            decoration: BoxDecoration(
              shape: BoxShape.circle,
              color: color,
            ),
          ),
          const SizedBox(width: 6),
          // 그룹명 + 개수
          Text(
            isExpired ? '유통기한 지남 ($count)' : '유통기한 전 ($count)',
            style: AppTypography.headlineSmall.copyWith(color: color),
          ),
        ],
      ),
    );
  }
}
