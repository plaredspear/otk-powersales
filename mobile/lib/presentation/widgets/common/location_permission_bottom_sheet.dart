import 'package:flutter/material.dart';

import '../../../core/theme/app_colors.dart';
import '../../../core/theme/app_spacing.dart';

/// 위치 권한 요청 바텀시트
///
/// 출근등록 시 OS 위치 권한이 없을 때 표시된다.
/// [showModalBottomSheet<bool>]로 표시하며:
/// - true: 사용자가 "권한 허용하기" 또는 "설정으로 이동" 버튼을 탭함
/// - null: 사용자가 "취소"를 탭하거나 바텀시트 외부를 탭하여 닫음
class LocationPermissionBottomSheet extends StatelessWidget {
  /// true: "설정으로 이동" 모드 (deniedForever / serviceDisabled)
  /// false: "권한 허용하기" 모드 (denied)
  final bool openSettings;

  const LocationPermissionBottomSheet({
    super.key,
    required this.openSettings,
  });

  /// 바텀시트 표시
  static Future<bool?> show(
    BuildContext context, {
    required bool openSettings,
  }) {
    return showModalBottomSheet<bool>(
      context: context,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(20)),
      ),
      builder: (context) => LocationPermissionBottomSheet(
        openSettings: openSettings,
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.all(20),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          // 핸들 바
          Container(
            width: 40,
            height: 4,
            decoration: BoxDecoration(
              color: AppColors.divider,
              borderRadius: BorderRadius.circular(2),
            ),
          ),
          const SizedBox(height: 24),

          // 제목
          const Text(
            '위치 권한이 필요합니다',
            style: TextStyle(
              fontSize: 18,
              fontWeight: FontWeight.w700,
              color: AppColors.textPrimary,
            ),
          ),
          const SizedBox(height: 12),

          // 설명
          Text(
            openSettings
                ? '설정에서 위치 권한을 허용해 주세요'
                : '출근등록을 위해 위치 정보\n접근을 허용해 주세요',
            textAlign: TextAlign.center,
            style: const TextStyle(
              fontSize: 15,
              color: AppColors.textSecondary,
              height: 1.5,
            ),
          ),
          const SizedBox(height: 24),

          // Primary 버튼
          SizedBox(
            width: double.infinity,
            height: AppSpacing.buttonHeight,
            child: ElevatedButton(
              onPressed: () => Navigator.of(context).pop(true),
              style: ElevatedButton.styleFrom(
                backgroundColor: AppColors.otokiBlue,
                foregroundColor: AppColors.white,
                shape: RoundedRectangleBorder(
                  borderRadius: AppSpacing.buttonBorderRadius,
                ),
                elevation: 0,
              ),
              child: Text(
                openSettings ? '설정으로 이동' : '권한 허용하기',
                style: const TextStyle(
                  fontSize: 15,
                  fontWeight: FontWeight.w600,
                ),
              ),
            ),
          ),
          const SizedBox(height: 8),

          // 취소 버튼
          SizedBox(
            width: double.infinity,
            child: TextButton(
              onPressed: () => Navigator.of(context).pop(),
              child: const Text(
                '취소',
                style: TextStyle(
                  fontSize: 15,
                  color: AppColors.textSecondary,
                ),
              ),
            ),
          ),
          const SizedBox(height: 8),
        ],
      ),
    );
  }
}
