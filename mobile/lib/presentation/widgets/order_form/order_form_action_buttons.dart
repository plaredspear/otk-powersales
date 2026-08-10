import 'package:flutter/material.dart';
import '../../../core/theme/app_colors.dart';
import '../../../core/theme/app_typography.dart';

/// 승인요청 버튼이 표현하는 상태.
///
/// 하단 고정 바는 스크롤 밖에 있고 마감/여신 안내는 스크롤 안 상단에 있어, 버튼만 회색으로
/// 죽어 있으면 사용자가 이유를 알 수 없다. 그래서 버튼 자신이 사유를 라벨과 색으로 말한다.
///
/// 색은 "이 화면에서 해소 가능한가" 를 기준으로 2단계로 나눈다.
/// - [pastDeadline] · [loanExceeded] = 차단 — 붉은 계열. 상단 마감 안내([AppColors.error])와
///   같은 색 언어라 위아래가 같은 사건으로 읽힌다.
/// - [quantityMissing] = 주의 — 회색 유지. 지금 고칠 수 있는 상태라 경고색으로 위협하지 않고,
///   활성(노랑)으로 오인되지도 않는다.
enum SubmitButtonState {
  /// 모든 조건 충족 — 탭 가능.
  ready,

  /// 납기일 전일 13:50 경과 — 이 화면에서 해소 불가.
  pastDeadline,

  /// 총 주문금액 > 여신잔액 — 이 화면에서 해소 불가.
  loanExceeded,

  /// 총EA 0 인 라인 존재 — 수량을 채우면 해소된다.
  quantityMissing,
}

/// 주문서 작성 액션 버튼 (삭제/임시저장/승인요청)
class OrderFormActionButtons extends StatelessWidget {
  final VoidCallback onDelete;
  final VoidCallback onSaveDraft;
  final VoidCallback onSubmit;
  final bool isSubmitting;

  /// 필수 항목(거래처/납기일/제품 + 모든 라인 수량 > 0) 입력 완료 여부.
  /// 미완료 시 승인요청 버튼을 비활성화한다.
  final bool requiredFieldsFilled;

  /// 여신 한도 초과 여부 — 레거시 write.jsp:188 처럼 초과 시 승인요청을 막는다.
  final bool loanExceeded;

  /// 주문 마감(납기일 전일 13:50) 경과 여부.
  /// 경과 시 승인요청을 막는다 — 서버가 `ORD_DEADLINE_PASSED` 로 거부할 요청을 미리 차단한다.
  final bool pastDeadline;

  /// 총EA 가 0 인 라인 수 — 라벨에 건수를 노출해 어느 정도 규모인지 알린다.
  final int zeroQuantityLineCount;

  const OrderFormActionButtons({
    super.key,
    required this.onDelete,
    required this.onSaveDraft,
    required this.onSubmit,
    required this.isSubmitting,
    required this.requiredFieldsFilled,
    this.loanExceeded = false,
    this.pastDeadline = false,
    this.zeroQuantityLineCount = 0,
  });

  /// 차단 사유 우선순위: 마감 > 여신 > 수량.
  /// 앞의 둘은 이 화면에서 해소할 수 없으므로, 고쳐도 소용없는 수량 안내보다 먼저 보여준다.
  SubmitButtonState get _submitState {
    if (pastDeadline) return SubmitButtonState.pastDeadline;
    if (loanExceeded) return SubmitButtonState.loanExceeded;
    if (!requiredFieldsFilled && zeroQuantityLineCount > 0) {
      return SubmitButtonState.quantityMissing;
    }
    return SubmitButtonState.ready;
  }

  @override
  Widget build(BuildContext context) {
    // 레거시 write.jsp 하단 고정 바: 삭제(회색) / 임시저장(다크) / 승인요청(옐로) 풀폭 3분할.
    final submitState = _submitState;
    // 필수 항목 미입력(수량 외 사유 포함)까지 포함해 최종 활성 여부를 결정한다.
    final bool submitEnabled = !isSubmitting &&
        requiredFieldsFilled &&
        submitState == SubmitButtonState.ready;

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
            _Segment(
              flex: 3,
              label: '임시저장',
              backgroundColor: AppColors.legacyTextSub,
              foregroundColor: AppColors.white,
              loading: isSubmitting,
              onPressed: isSubmitting ? null : onSaveDraft,
            ),
            // 승인요청 — 비활성 시 사유를 라벨/색으로 표현한다.
            _Segment(
              flex: 3,
              label: _labelFor(submitState),
              backgroundColor: AppColors.legacyYellow,
              foregroundColor: AppColors.onPrimary,
              disabledBackgroundColor: _disabledBackgroundFor(submitState),
              disabledForegroundColor: _disabledForegroundFor(submitState),
              loading: isSubmitting,
              onPressed: submitEnabled ? onSubmit : null,
            ),
          ],
        ),
      ),
    );
  }

  String _labelFor(SubmitButtonState state) {
    switch (state) {
      case SubmitButtonState.pastDeadline:
        return '마감시간 지남';
      case SubmitButtonState.loanExceeded:
        return '여신한도 초과';
      case SubmitButtonState.quantityMissing:
        return '수량 미입력 $zeroQuantityLineCount건';
      case SubmitButtonState.ready:
        return '승인요청';
    }
  }

  /// 차단 2종만 붉은 배경. 나머지는 기존 회색을 유지한다.
  Color _disabledBackgroundFor(SubmitButtonState state) {
    switch (state) {
      case SubmitButtonState.pastDeadline:
      case SubmitButtonState.loanExceeded:
        return AppColors.errorLight;
      case SubmitButtonState.quantityMissing:
      case SubmitButtonState.ready:
        return AppColors.surfaceVariant;
    }
  }

  /// 회색 배경 위 전경색은 [AppColors.textTertiary](2.35:1) 대신 [AppColors.legacyTextSub]
  /// (11.09:1) 를 쓴다 — 기존 disabled 라벨이 읽히지 않던 문제를 함께 해소한다.
  Color _disabledForegroundFor(SubmitButtonState state) {
    switch (state) {
      case SubmitButtonState.pastDeadline:
      case SubmitButtonState.loanExceeded:
        return AppColors.blockedForeground;
      case SubmitButtonState.quantityMissing:
      case SubmitButtonState.ready:
        return AppColors.legacyTextSub;
    }
  }
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

  const _Segment({
    required this.flex,
    required this.label,
    required this.backgroundColor,
    required this.foregroundColor,
    this.disabledBackgroundColor,
    this.disabledForegroundColor,
    this.loading = false,
    required this.onPressed,
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
          onTap: onPressed,
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
