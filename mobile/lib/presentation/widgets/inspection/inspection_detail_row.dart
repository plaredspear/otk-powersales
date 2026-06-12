import 'package:flutter/material.dart';

import '../../../core/theme/app_colors.dart';
import '../../../core/theme/app_typography.dart';

/// 현장점검 상세 - 라벨/값 한 줄 (레거시 dl 정합)
///
/// 레거시 view.jsp 의 `dt`(라벨, 좌측 32%, 검정) / `dd`(값, 우측 68%, #666 회색)
/// 2단 구성을 재현한다. 라벨·값 모두 길면 줄바꿈된다.
class InspectionDetailRow extends StatelessWidget {
  final String label;
  final String value;

  const InspectionDetailRow({
    super.key,
    required this.label,
    required this.value,
  });

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 5),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          // dt: 라벨 (좌측 고정폭, 검정)
          SizedBox(
            width: 104,
            child: Text(
              label,
              style: AppTypography.bodyMedium.copyWith(
                color: AppColors.textPrimary,
              ),
            ),
          ),
          const SizedBox(width: 8),
          // dd: 값 (우측 가변폭, 회색)
          Expanded(
            child: Text(
              value.isEmpty ? '-' : value,
              style: AppTypography.bodyMedium.copyWith(
                color: AppColors.legacyTextMute,
              ),
            ),
          ),
        ],
      ),
    );
  }
}
