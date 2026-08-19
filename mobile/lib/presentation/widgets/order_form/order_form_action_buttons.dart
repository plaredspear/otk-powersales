import 'package:flutter/material.dart';
import '../../../core/constants/order_limits.dart';
import '../../../core/theme/app_colors.dart';
import '../../../core/theme/app_typography.dart';
import '../../providers/order_form_state.dart';

/// 주문서 작성 액션 버튼 (삭제/임시저장/승인요청)
///
/// 하단 고정 바는 스크롤 밖에 있고 마감/여신 안내는 스크롤 안 상단에 있어, 버튼만 회색으로
/// 죽어 있으면 사용자가 이유를 알 수 없다. 그래서 버튼 자신이 사유를 라벨과 색으로 말한다.
///
/// 사유 판정은 하지 않는다 — [blockKind] 를 그대로 그린다. 화면이 자체 조건으로 라벨을
/// 다시 계산하면 검증 순서와 어긋나(거래처 미선택인데 `제품 100개 초과` 라벨) 토스트와
/// 딴소리를 하게 된다.
///
/// 색은 "이 화면에서 해소 가능한가" 를 기준으로 2단계로 나눈다.
/// - 마감 · 여신 초과 = 차단 — 붉은 계열. 상단 마감 안내([AppColors.error])와 같은 색
///   언어라 위아래가 같은 사건으로 읽힌다.
/// - 그 외 = 주의 — 회색 유지. 지금 고칠 수 있는 상태라 경고색으로 위협하지 않고,
///   활성(노랑)으로 오인되지도 않는다.
class OrderFormActionButtons extends StatelessWidget {
  final VoidCallback onDelete;
  final VoidCallback onSaveDraft;
  final VoidCallback onSubmit;
  final bool isSubmitting;

  /// 승인요청이 막힌 사유 — null 이면 제출 가능(활성).
  /// [OrderFormNotifier.submitBlock] 이 유일한 출처다.
  final SubmitBlockKind? blockKind;

  /// 총EA 가 0 인 라인 수 — 라벨에 건수를 노출해 어느 정도 규모인지 알린다.
  final int zeroQuantityLineCount;

  /// 담긴 제품이 1건이라도 있는지 — 임시저장 활성 조건.
  /// 레거시 write.jsp 의 임시저장 버튼도 `제품 1건 이상` 일 때만 활성이었다.
  final bool hasItems;

  /// 비활성 상태의 임시저장 버튼을 눌렀을 때 — 사유를 안내한다.
  final VoidCallback? onSaveDraftDisabledTap;

  /// 비활성 상태의 승인요청 버튼을 눌렀을 때 — 막힌 사유를 안내한다.
  ///
  /// 버튼 라벨은 사유를 한 단어로만 말할 수 있어(예: `제품 100개 초과`) 무엇을 해야 하는지는
  /// 전달하지 못한다. 그래서 비활성 상태에서도 탭은 살려 두고 사유를 토스트로 알린다.
  /// (전송 중에는 탭하지 않는다 — 사유가 아니라 진행 중 상태이기 때문.)
  final VoidCallback? onDisabledTap;

  const OrderFormActionButtons({
    super.key,
    required this.onDelete,
    required this.onSaveDraft,
    required this.onSubmit,
    required this.isSubmitting,
    this.blockKind,
    this.zeroQuantityLineCount = 0,
    this.hasItems = true,
    this.onDisabledTap,
    this.onSaveDraftDisabledTap,
  });

  @override
  Widget build(BuildContext context) {
    // 레거시 write.jsp 하단 고정 바: 삭제(회색) / 임시저장(다크) / 승인요청(옐로) 풀폭 3분할.
    final bool submitEnabled = !isSubmitting && blockKind == null;

    return SafeArea(
      top: false,
      child: SizedBox(
        height: 60,
        child: Row(
          children: [
            _Segment(
              flex: 2,
              label: '삭제',
              backgroundColor: AppColors.surfaceVariant,
              foregroundColor: AppColors.textSecondary,
              onPressed: isSubmitting ? null : onDelete,
            ),
            // 임시저장 — 담긴 제품이 없으면 비활성 (레거시 write.jsp 버튼 활성 조건 정합).
            _Segment(
              flex: 3,
              label: '임시저장',
              backgroundColor: AppColors.legacyTextSub,
              foregroundColor: AppColors.white,
              disabledBackgroundColor: AppColors.surfaceVariant,
              disabledForegroundColor: AppColors.legacyTextSub,
              loading: isSubmitting,
              onPressed: (isSubmitting || !hasItems) ? null : onSaveDraft,
              inactiveOnPressed: isSubmitting ? null : onSaveDraftDisabledTap,
            ),
            // 승인요청 — 비활성 시 사유를 라벨/색으로 표현하고,
            // 탭은 살려 두어 [onDisabledTap] 이 사유를 안내하게 한다 (색은 비활성 그대로).
            _Segment(
              flex: 3,
              label: _label,
              backgroundColor: AppColors.legacyYellow,
              foregroundColor: AppColors.onPrimary,
              disabledBackgroundColor:
                  _isHardBlock ? AppColors.errorLight : AppColors.surfaceVariant,
              disabledForegroundColor: _isHardBlock
                  ? AppColors.blockedForeground
                  : AppColors.legacyTextSub,
              loading: isSubmitting,
              onPressed: submitEnabled ? onSubmit : null,
              inactiveOnPressed: isSubmitting ? null : onDisabledTap,
            ),
          ],
        ),
      ),
    );
  }

  /// 라벨은 사유를 한 단어로 요약한다. 요약이 마땅치 않은 사유(거래처/납기일 미선택 등)는
  /// 기본 라벨을 유지하고 — 그 항목들은 화면 위쪽에 `*` 필수 표시로 이미 드러나 있다 —
  /// 구체적인 안내는 탭 시 토스트([onDisabledTap])가 맡는다.
  String get _label {
    switch (blockKind) {
      case SubmitBlockKind.deadline:
        return '마감시간 지남';
      case SubmitBlockKind.loanExceeded:
        return '여신한도 초과';
      case SubmitBlockKind.lineLimit:
        return '제품 ${OrderLimits.maxOrderLines}개 초과';
      case SubmitBlockKind.zeroQuantity:
        return '수량 미입력 $zeroQuantityLineCount건';
      case SubmitBlockKind.account:
      case SubmitBlockKind.deliveryDate:
      case SubmitBlockKind.duplicateProduct:
      case SubmitBlockKind.pastDeliveryDate:
      case SubmitBlockKind.noItems:
      case SubmitBlockKind.loanUnavailable:
      case null:
        return '승인요청';
    }
  }

  /// 이 화면에서 해소할 수 없는 차단 2종만 붉은 배경. 나머지는 회색을 유지한다.
  /// (회색 배경 위 전경색은 [AppColors.textTertiary](2.35:1) 대신
  /// [AppColors.legacyTextSub](11.09:1) 를 써서 disabled 라벨 가독성을 확보한다.)
  bool get _isHardBlock =>
      blockKind == SubmitBlockKind.deadline ||
      blockKind == SubmitBlockKind.loanExceeded;
}

/// 하단 고정 바의 단일 세그먼트 (풀-블리드, 모서리 없음).
class _Segment extends StatelessWidget {
  final int flex;
  final String label;
  final Color backgroundColor;
  final Color foregroundColor;
  final Color? disabledBackgroundColor;
  final Color? disabledForegroundColor;
  final bool loading;
  final VoidCallback? onPressed;

  /// 비활성 색을 유지한 채로만 반응하는 탭 (예: 수량 미입력 → 해당 줄로 이동).
  /// [onPressed] 가 null 일 때만 쓰인다.
  final VoidCallback? inactiveOnPressed;

  const _Segment({
    required this.flex,
    required this.label,
    required this.backgroundColor,
    required this.foregroundColor,
    this.disabledBackgroundColor,
    this.disabledForegroundColor,
    this.loading = false,
    required this.onPressed,
    this.inactiveOnPressed,
  });

  @override
  Widget build(BuildContext context) {
    final bool enabled = onPressed != null;
    final Color bg = enabled
        ? backgroundColor
        : (disabledBackgroundColor ?? backgroundColor);
    final Color fg = enabled
        ? foregroundColor
        : (disabledForegroundColor ?? foregroundColor);

    return Expanded(
      flex: flex,
      child: Material(
        color: bg,
        child: InkWell(
          onTap: onPressed ?? inactiveOnPressed,
          child: Center(
            child: loading
                ? SizedBox(
                    width: 20,
                    height: 20,
                    child: CircularProgressIndicator(
                      strokeWidth: 2,
                      valueColor: AlwaysStoppedAnimation<Color>(fg),
                    ),
                  )
                : Text(
                    label,
                    style: AppTypography.headlineSmall.copyWith(color: fg),
                  ),
          ),
        ),
      ),
    );
  }
}
