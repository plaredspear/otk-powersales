import 'package:flutter/material.dart';

import '../../../core/theme/app_colors.dart';

/// 레거시(Heroku) 정합 섹션 헤더 바
///
/// 점검 등록 화면의 "자사 활동 정보" / "경쟁사 활동 정보" 구분 바를 재현한다.
/// 풀폭 슬레이트(#7C91A7) 배경 위 흰색 볼드 텍스트(좌측 정렬).
class InspectionSectionHeader extends StatelessWidget {
  /// 헤더 텍스트
  final String title;

  const InspectionSectionHeader({super.key, required this.title});

  @override
  Widget build(BuildContext context) {
    return Container(
      width: double.infinity,
      color: AppColors.legacySlate,
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
      child: Text(
        title,
        style: const TextStyle(
          fontSize: 16,
          fontWeight: FontWeight.bold,
          color: Colors.white,
        ),
      ),
    );
  }
}
