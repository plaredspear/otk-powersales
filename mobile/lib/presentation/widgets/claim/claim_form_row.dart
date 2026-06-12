import 'package:flutter/material.dart';

/// 클레임 등록 폼의 공통 색상 토큰 (레거시 리스트 UI 기준)
class ClaimFormColors {
  ClaimFormColors._();

  /// 행 구분선
  static const divider = Color(0xFFEEEEEE);

  /// 비활성 행 배경
  static const disabledBackground = Color(0xFFF5F5F5);

  /// 라벨 텍스트
  static const label = Color(0xFF333333);

  /// 비활성 라벨 텍스트
  static const labelDisabled = Color(0xFFAAAAAA);

  /// 필수(*) 표시
  static const required = Color(0xFFE53935);

  /// 선택된 값 텍스트
  static const value = Color(0xFF222222);

  /// 플레이스홀더 텍스트
  static const placeholder = Color(0xFF999999);

  /// 우측 화살표
  static const chevron = Color(0xFFBBBBBB);

  /// 단위(개/원) 텍스트
  static const unit = Color(0xFF888888);

  /// 버튼 테두리
  static const buttonBorder = Color(0xFFDDDDDD);
}

/// 클레임 등록 폼의 단일 입력 행.
///
/// 레거시 UI 처럼 테두리 박스 없이 라벨 + 하단 값/플레이스홀더로 구성되고
/// 행 하단에 얇은 구분선이 그려진다. 우측에는 화살표·버튼·단위 등을 배치한다.
class ClaimFormRow extends StatelessWidget {
  const ClaimFormRow({
    super.key,
    required this.label,
    this.isRequired = false,
    this.enabled = true,
    this.below,
    this.trailing,
    this.onTap,
    this.alignTrailingTop = false,
    this.showDivider = true,
  });

  /// 라벨 텍스트 (필수 표시는 [isRequired] 로 부여)
  final String label;

  /// 필수 여부 (라벨 뒤 빨간 * 표시)
  final bool isRequired;

  /// 활성 여부 (false 면 회색 배경 + 비활성 라벨)
  final bool enabled;

  /// 라벨 아래에 표시할 위젯 (값/플레이스홀더/입력 필드 등)
  final Widget? below;

  /// 우측에 표시할 위젯 (화살표/버튼/단위 등)
  final Widget? trailing;

  /// 행 탭 콜백
  final VoidCallback? onTap;

  /// 우측 위젯을 상단 정렬할지 (제품/사진 버튼 등) 여부
  final bool alignTrailingTop;

  /// 하단 구분선 표시 여부
  final bool showDivider;

  @override
  Widget build(BuildContext context) {
    final row = Row(
      crossAxisAlignment:
          alignTrailingTop ? CrossAxisAlignment.start : CrossAxisAlignment.center,
      children: [
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              _ClaimFieldLabel(
                label: label,
                isRequired: isRequired,
                enabled: enabled,
              ),
              if (below != null) ...[
                const SizedBox(height: 6),
                below!,
              ],
            ],
          ),
        ),
        if (trailing != null) ...[
          const SizedBox(width: 12),
          trailing!,
        ],
      ],
    );

    final content = Container(
      width: double.infinity,
      color: enabled ? null : ClaimFormColors.disabledBackground,
      padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 16),
      child: row,
    );

    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        if (onTap != null && enabled)
          InkWell(onTap: onTap, child: content)
        else
          content,
        if (showDivider)
          const Divider(
            height: 1,
            thickness: 1,
            color: ClaimFormColors.divider,
          ),
      ],
    );
  }
}

/// 라벨 + 필수(*) 표시
class _ClaimFieldLabel extends StatelessWidget {
  const _ClaimFieldLabel({
    required this.label,
    required this.isRequired,
    required this.enabled,
  });

  final String label;
  final bool isRequired;
  final bool enabled;

  @override
  Widget build(BuildContext context) {
    return Text.rich(
      TextSpan(
        text: label,
        style: TextStyle(
          fontSize: 15,
          fontWeight: FontWeight.w700,
          color: enabled ? ClaimFormColors.label : ClaimFormColors.labelDisabled,
        ),
        children: [
          if (isRequired)
            const TextSpan(
              text: ' *',
              style: TextStyle(color: ClaimFormColors.required),
            ),
        ],
      ),
    );
  }
}

/// 값/플레이스홀더 텍스트 (값이 없으면 회색 플레이스홀더)
class ClaimValueText extends StatelessWidget {
  const ClaimValueText({
    super.key,
    required this.value,
    required this.placeholder,
  });

  final String? value;
  final String placeholder;

  @override
  Widget build(BuildContext context) {
    final hasValue = value != null && value!.isNotEmpty;
    return Text(
      hasValue ? value! : placeholder,
      style: TextStyle(
        fontSize: 14,
        color: hasValue ? ClaimFormColors.value : ClaimFormColors.placeholder,
      ),
    );
  }
}

/// 우측 화살표 (행 탭 가능 표시)
class ClaimRowChevron extends StatelessWidget {
  const ClaimRowChevron({super.key});

  @override
  Widget build(BuildContext context) {
    return const Icon(
      Icons.chevron_right,
      size: 26,
      color: ClaimFormColors.chevron,
    );
  }
}

/// 레거시 스타일 알약형 아웃라인 버튼 (바코드/선택/사진 선택 등)
class ClaimPillButton extends StatelessWidget {
  const ClaimPillButton({
    super.key,
    required this.icon,
    required this.label,
    required this.onPressed,
  });

  final IconData icon;
  final String label;
  final VoidCallback onPressed;

  @override
  Widget build(BuildContext context) {
    return OutlinedButton.icon(
      onPressed: onPressed,
      icon: Icon(icon, size: 16),
      label: Text(label),
      style: OutlinedButton.styleFrom(
        foregroundColor: ClaimFormColors.label,
        side: const BorderSide(color: ClaimFormColors.buttonBorder),
        shape: const StadiumBorder(),
        padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 6),
        minimumSize: const Size(0, 34),
        visualDensity: VisualDensity.compact,
        tapTargetSize: MaterialTapTargetSize.shrinkWrap,
        textStyle: const TextStyle(fontSize: 13, fontWeight: FontWeight.w500),
      ),
    );
  }
}
