import 'package:flutter/material.dart';
import '../../../core/theme/app_colors.dart';

/// 일정 거래처 아이템 위젯
///
/// 레거시 myDaily.jsp 정합 — 탭에 따라 행 구조가 다릅니다.
/// - 일정 탭: 한 줄 `거래처명 | 근무유형1/2/3` (구분자 `|` #CCC, 본문 15px/#666)
/// - 등록 탭: 거래처명(굵게 16px) 1줄 + `근무유형1/2/3`(15px/#666) 1줄, `|` 없음,
///   우측 상단에 등록 상태 텍스트(완료=초록 / 전=회색)
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

  /// 등록 상태 표시 여부 (= 등록 탭 레이아웃 사용 여부)
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

  // 레거시: ${c1}/${c2}/${c3} (빈 값은 그대로 슬래시만 노출)
  String get _categories => '$workType1/$workType2/$workType3';

  /// 거래처명 + 카테고리 본문 색 (레거시 .board_list02 li p span #666)
  static const TextStyle _bodyStyle = TextStyle(
    fontSize: 15,
    fontWeight: FontWeight.w400,
    color: AppColors.legacyTextMute,
    height: 1.2,
  );

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 6),
      child: showRegistrationStatus
          ? _buildRegistrationRow()
          : _buildScheduleRow(),
    );
  }

  /// 일정 탭: `거래처명  |  c1/c2/c3` 한 줄
  Widget _buildScheduleRow() {
    return Text.rich(
      TextSpan(
        style: _bodyStyle,
        children: [
          TextSpan(text: accountName),
          const TextSpan(
            text: '  |  ',
            style: TextStyle(color: AppColors.legacyPlaceholder),
          ),
          TextSpan(text: _categories),
        ],
      ),
    );
  }

  /// 등록 탭: 거래처명(굵게) + 카테고리 2줄 + 우측 상단 등록 상태
  Widget _buildRegistrationRow() {
    return Row(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              // 거래처명 (레거시 .board_list02 li p strong: 16px bold #000)
              if (accountName.isNotEmpty)
                Padding(
                  padding: const EdgeInsets.only(bottom: 6),
                  child: Text(
                    accountName,
                    style: const TextStyle(
                      fontSize: 16,
                      fontWeight: FontWeight.w700,
                      color: AppColors.black,
                      height: 1.2,
                    ),
                  ),
                ),
              // 근무유형
              Text(_categories, style: _bodyStyle),
            ],
          ),
        ),
        const SizedBox(width: 8),
        // 등록 상태 (레거시 .ex_day color_gray/color_green)
        Text(
          (isRegistered ?? false) ? '등록 완료' : '등록 전',
          style: TextStyle(
            fontSize: 12,
            height: 1.2,
            color: (isRegistered ?? false)
                ? AppColors.legacyRegisteredGreen
                : AppColors.legacyRegisteredGray,
          ),
        ),
      ],
    );
  }
}
