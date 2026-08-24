import 'package:flutter/material.dart';

import '../../../core/theme/app_colors.dart';
import '../../../core/theme/app_spacing.dart';
import '../../../core/utils/clipboard_utils.dart';

/// 탭하면 값을 클립보드로 복사하는 텍스트 + 복사 아이콘
///
/// 바코드처럼 거래처 앱 등 외부에 옮겨 적어야 하는 값의 표시 지점에서 사용한다.
/// 값이 비었거나 `-` 이면 복사 affordance 없이 일반 텍스트로 표시한다.
class CopyableValue extends StatelessWidget {
  /// 실제 복사되는 값 (표시 문자열이 아닌 원본값)
  final String value;

  /// 복사 안내 문구의 항목명 (예: `바코드`)
  final String copyLabel;

  /// 화면 표시 문자열. 생략하면 [value] 를 그대로 표시한다.
  final String? displayText;

  final TextStyle? style;

  /// 복사 아이콘 크기
  final double iconSize;

  final int maxLines;

  /// true 면 복사 아이콘만 탭 대상이 된다.
  /// 카드 전체 탭이 선택/이동 등 다른 동작을 갖는 목록에서 오탭을 막는 용도.
  final bool iconOnly;

  const CopyableValue({
    super.key,
    required this.value,
    required this.copyLabel,
    this.displayText,
    this.style,
    this.iconSize = 15,
    this.maxLines = 1,
    this.iconOnly = false,
  });

  @override
  Widget build(BuildContext context) {
    final text = displayText ?? value;
    final label = Text(
      text,
      style: style,
      maxLines: maxLines,
      overflow: TextOverflow.ellipsis,
    );

    final trimmed = value.trim();
    if (trimmed.isEmpty || trimmed == '-') return label;

    void copy() => ClipboardUtils.copy(context, value, label: copyLabel);

    final icon = Padding(
      // 탭 영역 확보 — 아이콘 주변 여백까지 눌러도 복사되도록 한다.
      padding: const EdgeInsets.symmetric(
        horizontal: AppSpacing.xs,
        vertical: AppSpacing.xxs,
      ),
      child: Icon(
        Icons.copy_rounded,
        size: iconSize,
        color: AppColors.textTertiary,
      ),
    );

    return Row(
      mainAxisSize: MainAxisSize.min,
      children: [
        Flexible(
          child: iconOnly
              ? label
              : GestureDetector(
                  behavior: HitTestBehavior.opaque,
                  onTap: copy,
                  child: label,
                ),
        ),
        GestureDetector(
          behavior: HitTestBehavior.opaque,
          onTap: copy,
          child: icon,
        ),
      ],
    );
  }
}
