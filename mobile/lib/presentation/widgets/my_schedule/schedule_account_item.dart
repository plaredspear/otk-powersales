import 'package:flutter/material.dart';
import '../../../core/theme/app_colors.dart';

/// 일정 거래처 아이템 위젯
///
/// 레거시 myDaily.jsp 정합: 한 줄에 `거래처명 | 근무유형1/근무유형2/근무유형3`.
/// - 거래처명 + 카테고리: 15px / weight 400 / #666 (legacyTextMute)
/// - 구분자 `|`: #CCC (legacyPlaceholder), 좌우 5px 여백
/// 등록 탭에서는 우측 상단에 등록 상태 텍스트(완료=초록 / 전=회색)를 표시합니다.
class ScheduleAccountItem extends StatelessWidget {
  /// 거래처명
  final String accountName;

  /// 근무 유형 1
  final String workType1;

  /// 근무 유형 2
  final String workType2;

  /// 근무 유형 3
  final String workType3;

  /// 등록 여부 (등록 탭에서만 사용)
  final bool? isRegistered;

  /// 등록 상태 표시 여부
  final bool showRegistrationStatus;

  const ScheduleAccountItem({
    super.key,
    required this.accountName,
    required this.workType1,
    required this.workType2,
    required this.workType3,
    this.isRegistered,
    this.showRegistrationStatus = false,
  });

  @override
  Widget build(BuildContext context) {
    // 레거시: ${c1}/${c2}/${c3} (빈 값은 그대로 슬래시만 노출)
    final categories = '$workType1/$workType2/$workType3';

    const baseStyle = TextStyle(
      fontSize: 15,
      fontWeight: FontWeight.w400,
      color: AppColors.legacyTextMute,
      height: 1.2,
    );

    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 6),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          // 거래처명 | 카테고리 (한 줄)
          Expanded(
            child: Text.rich(
              TextSpan(
                style: baseStyle,
                children: [
                  TextSpan(text: accountName),
                  const TextSpan(
                    text: '  |  ',
                    style: TextStyle(color: AppColors.legacyPlaceholder),
                  ),
                  TextSpan(text: categories),
                ],
              ),
            ),
          ),

          // 등록 상태 (등록 탭에서만)
          if (showRegistrationStatus && isRegistered != null) ...[
            const SizedBox(width: 8),
            Text(
              isRegistered! ? '등록 완료' : '등록 전',
              style: TextStyle(
                fontSize: 12,
                height: 1.2,
                color: isRegistered!
                    ? AppColors.legacyRegisteredGreen
                    : AppColors.legacyRegisteredGray,
              ),
            ),
          ],
        ],
      ),
    );
  }
}
