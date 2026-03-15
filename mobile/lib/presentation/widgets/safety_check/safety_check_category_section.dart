import 'package:flutter/material.dart';

import '../../../core/theme/app_colors.dart';
import '../../../domain/entities/safety_check_category.dart';
import 'safety_check_item_tile.dart';

/// 안전점검 카테고리 섹션 위젯 (V1: inputType 기반 라디오/체크박스 분기)
class SafetyCheckCategorySection extends StatelessWidget {
  final SafetyCheckCategory category;

  /// 섹션 1: 장비 라디오 응답 (seqNum → "예"/"해당없음")
  final Map<int, String> equipmentAnswers;

  /// 섹션 2: 예방사항 체크박스 (seqNum → checked)
  final Map<int, bool> precautionChecks;

  /// 라디오 선택 콜백
  final void Function(int seqNum, String answer)? onRadioSelect;

  /// 체크박스 토글 콜백
  final ValueChanged<int>? onCheckboxToggle;

  const SafetyCheckCategorySection({
    super.key,
    required this.category,
    this.equipmentAnswers = const {},
    this.precautionChecks = const {},
    this.onRadioSelect,
    this.onCheckboxToggle,
  });

  @override
  Widget build(BuildContext context) {
    final isRadio = category.inputType == 'RADIO';

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        // 카테고리 헤더
        Padding(
          padding: const EdgeInsets.fromLTRB(16, 20, 16, 4),
          child: Row(
            children: [
              Container(
                width: 4,
                height: 20,
                decoration: BoxDecoration(
                  color: AppColors.primary,
                  borderRadius: BorderRadius.circular(2),
                ),
              ),
              const SizedBox(width: 8),
              Expanded(
                child: Text(
                  '${category.questionNum}. ${category.title}',
                  style: const TextStyle(
                    fontSize: 16,
                    fontWeight: FontWeight.bold,
                    color: AppColors.textPrimary,
                  ),
                ),
              ),
            ],
          ),
        ),
        // 구분선
        const Padding(
          padding: EdgeInsets.symmetric(horizontal: 16),
          child: Divider(color: AppColors.divider, height: 1),
        ),
        // 안내 문구
        Padding(
          padding: const EdgeInsets.fromLTRB(16, 8, 16, 4),
          child: Text(
            isRadio ? '모두 체크해 주세요' : '해당 항목에 체크해 주세요',
            style: const TextStyle(
              fontSize: 13,
              color: AppColors.textTertiary,
            ),
          ),
        ),
        // 라디오 헤더 (옵션 라벨)
        if (isRadio && category.options != null)
          Padding(
            padding: const EdgeInsets.fromLTRB(16, 4, 16, 0),
            child: Row(
              children: [
                const Expanded(flex: 3, child: SizedBox()),
                ...category.options!.map((option) => SizedBox(
                      width: 72,
                      child: Center(
                        child: Text(
                          option,
                          style: const TextStyle(
                            fontSize: 12,
                            fontWeight: FontWeight.w600,
                            color: AppColors.textSecondary,
                          ),
                        ),
                      ),
                    )),
              ],
            ),
          ),
        // 항목 목록
        if (isRadio)
          ...category.items.map(
            (item) => SafetyCheckRadioTile(
              item: item,
              selectedAnswer: equipmentAnswers[item.seqNum],
              options: category.options ?? ['예', '해당없음'],
              onSelect: (seqNum, answer) {
                onRadioSelect?.call(seqNum, answer);
              },
            ),
          )
        else
          ...category.items.map(
            (item) => SafetyCheckCheckboxTile(
              item: item,
              isChecked: precautionChecks[item.seqNum] ?? false,
              onToggle: (seqNum) {
                onCheckboxToggle?.call(seqNum);
              },
            ),
          ),
      ],
    );
  }
}
