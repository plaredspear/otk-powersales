import 'package:flutter/material.dart';

import '../../../core/theme/app_colors.dart';
import '../../../domain/entities/safety_check_category.dart';
import 'safety_check_item_tile.dart';

/// 안전점검 카테고리 섹션 위젯 (V1: inputType 기반 아코디언/체크박스 분기)
class SafetyCheckCategorySection extends StatelessWidget {
  final SafetyCheckCategory category;

  /// 섹션 1: 장비 라디오 응답 (seqNum → "예"/"해당없음")
  final Map<int, String> equipmentAnswers;

  /// 섹션 2: 예방사항 체크박스 (seqNum → checked)
  final Map<int, bool> precautionChecks;

  /// 현재 펼쳐진 항목의 seqNum (아코디언용)
  final int? expandedItemIndex;

  /// 라디오 선택 콜백
  final void Function(int seqNum, String answer)? onRadioSelect;

  /// 체크박스 토글 콜백
  final ValueChanged<int>? onCheckboxToggle;

  /// 아코디언 토글 콜백
  final ValueChanged<int>? onToggleExpand;

  const SafetyCheckCategorySection({
    super.key,
    required this.category,
    this.equipmentAnswers = const {},
    this.precautionChecks = const {},
    this.expandedItemIndex,
    this.onRadioSelect,
    this.onCheckboxToggle,
    this.onToggleExpand,
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
        if (isRadio) _buildRadioGuideText() else _buildCheckboxGuideText(),
        // 항목 목록
        if (isRadio)
          // 레거시(checkList.jsp): 항목 번호는 DB seqNum이 아니라 루프 1-based 순번
          ...category.items.asMap().entries.map(
            (entry) => SafetyCheckAccordionTile(
              item: entry.value,
              displayNumber: entry.key + 1,
              selectedAnswer: equipmentAnswers[entry.value.seqNum],
              options: category.options ?? ['예', '해당없음'],
              isExpanded: expandedItemIndex == entry.value.seqNum,
              onTap: () => onToggleExpand?.call(entry.value.seqNum),
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

  Widget _buildRadioGuideText() {
    return Padding(
      padding: const EdgeInsets.fromLTRB(16, 8, 16, 8),
      child: Container(
        width: double.infinity,
        padding: const EdgeInsets.all(12),
        decoration: BoxDecoration(
          color: AppColors.surfaceVariant.withValues(alpha: 0.3),
          borderRadius: BorderRadius.circular(8),
        ),
        child: RichText(
          text: const TextSpan(
            style: TextStyle(
              fontSize: 13,
              color: AppColors.textSecondary,
              height: 1.5,
            ),
            children: [
              TextSpan(text: '출근하시면, 본인 '),
              TextSpan(
                text: '안전장비 착용여부',
                style: TextStyle(fontWeight: FontWeight.bold),
              ),
              TextSpan(text: '와 '),
              TextSpan(
                text: '매장위험요소',
                style: TextStyle(fontWeight: FontWeight.bold),
              ),
              TextSpan(text: '를 인지하고 주의를 기울이도록 '),
              TextSpan(
                text: '매일 체크',
                style: TextStyle(fontWeight: FontWeight.bold),
              ),
              TextSpan(text: '합시다!\n\n'),
              TextSpan(text: '1. '),
              TextSpan(
                text: '오늘 안전예방 장비 착용',
                style: TextStyle(
                  color: AppColors.success,
                  fontWeight: FontWeight.w700,
                ),
              ),
              TextSpan(text: '을 했는지 체크바랍니다. '),
              TextSpan(
                text: '(아래 9개 항목 모두 체크하세요!)',
                style: TextStyle(
                  color: AppColors.error,
                  fontWeight: FontWeight.w700,
                ),
              ),
              TextSpan(text: '\n\n★ '),
              TextSpan(
                text: '지급받은 장비에 한하여 착용점검',
                style: TextStyle(
                  color: AppColors.error,
                  fontWeight: FontWeight.w700,
                ),
              ),
              TextSpan(
                text: ' 바랍니다. (지급장비에 문제가 있으면 판매조장에게 문의바랍니다.)',
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildCheckboxGuideText() {
    return Padding(
      padding: const EdgeInsets.fromLTRB(16, 8, 16, 4),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          RichText(
            text: const TextSpan(
              style: TextStyle(
                fontSize: 13,
                color: AppColors.textSecondary,
                height: 1.5,
              ),
              children: [
                TextSpan(text: '2. 매장 근무시, 아래 '),
                TextSpan(
                  text: '안전사고 예방사항을 준수',
                  style: TextStyle(
                    color: AppColors.error,
                    fontWeight: FontWeight.w700,
                  ),
                ),
                TextSpan(text: '하겠습니다. (내용을 읽고 각 사항을 체크)'),
              ],
            ),
          ),
          const SizedBox(height: 4),
          RichText(
            text: const TextSpan(
              style: TextStyle(
                fontSize: 13,
                color: AppColors.textTertiary,
                height: 1.5,
              ),
              children: [
                TextSpan(text: '★ '),
                TextSpan(
                  text: '위험요소',
                  style: TextStyle(
                    color: AppColors.error,
                    fontWeight: FontWeight.w700,
                  ),
                ),
                TextSpan(
                  text: '를 파악하고 ',
                  style: TextStyle(fontWeight: FontWeight.w700),
                ),
                TextSpan(
                  text: '준수사항',
                  style: TextStyle(
                    color: AppColors.success,
                    fontWeight: FontWeight.w700,
                  ),
                ),
                TextSpan(
                  text: '에 대하여 항상 주의 부탁드립니다.',
                  style: TextStyle(fontWeight: FontWeight.w700),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}
