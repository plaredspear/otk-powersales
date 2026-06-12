import 'package:flutter/material.dart';

import '../../../core/theme/app_colors.dart';

/// 레거시(Heroku) 정합 세그먼트 토글
///
/// 레거시 점검 등록 화면의 분류(자사/경쟁사)·시식여부(예/아니요) 토글 디자인을 재현한다.
/// - 선택 항목: 꽉 찬 오뚜기 노랑 배경 + 검정 글씨(볼드)
/// - 미선택 항목: 연회색 트랙 위 회색 글씨
class LegacySegmentedToggle extends StatelessWidget {
  /// 세그먼트 라벨 목록
  final List<String> labels;

  /// 선택된 인덱스
  final int selectedIndex;

  /// 선택 변경 콜백
  final ValueChanged<int> onChanged;

  const LegacySegmentedToggle({
    super.key,
    required this.labels,
    required this.selectedIndex,
    required this.onChanged,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      decoration: BoxDecoration(
        color: const Color(0xFFEDEDED),
        borderRadius: BorderRadius.circular(6),
        border: Border.all(color: const Color(0xFFD9D9D9)),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: List.generate(labels.length, (index) {
          final selected = index == selectedIndex;
          return GestureDetector(
            onTap: () => onChanged(index),
            child: Container(
              padding: const EdgeInsets.symmetric(horizontal: 22, vertical: 8),
              decoration: BoxDecoration(
                color: selected ? AppColors.otokiYellow : Colors.transparent,
                borderRadius: BorderRadius.circular(6),
              ),
              child: Text(
                labels[index],
                style: TextStyle(
                  fontSize: 15,
                  color: selected ? Colors.black : const Color(0xFF9E9E9E),
                  fontWeight: selected ? FontWeight.bold : FontWeight.normal,
                ),
              ),
            ),
          );
        }),
      ),
    );
  }
}
