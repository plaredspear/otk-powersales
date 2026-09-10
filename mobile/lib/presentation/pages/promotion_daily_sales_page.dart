import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../app_router.dart';
import '../../core/theme/app_colors.dart';
import '../../core/theme/app_spacing.dart';
import '../../core/theme/app_typography.dart';
import '../../core/utils/throttled_tap_mixin.dart';
import '../../core/utils/thousands_separator_input_formatter.dart';
import '../providers/promotion_daily_sales_provider.dart';
import '../widgets/common/loading_indicator.dart';
import '../widgets/daily_sales/photo_picker_widget.dart';
import '../widgets/daily_sales/product_input_form.dart';
import '../widgets/order_form/draft_restore_dialog.dart';
import '../widgets/promotion/my_assignment_picker_sheet.dart';

/// 여사원 일 매출 등록 화면 (레거시 Heroku `promotion/event/write.jsp` 정합).
///
/// 본인에게 배정된 행사조원(promotionEmployeeId)의 일매출을 입력·임시저장·최종 등록한다.
/// 상단에 날짜와 선택된 행사(`[행사유형]행사명`)를 계속 표시하며, 행사 행을 탭하면
/// 담당 행사를 다시 고를 수 있다.
class PromotionDailySalesPage extends ConsumerStatefulWidget {
  final int promotionEmployeeId;

  const PromotionDailySalesPage({super.key, required this.promotionEmployeeId});

  @override
  ConsumerState<PromotionDailySalesPage> createState() =>
      _PromotionDailySalesPageState();
}

class _PromotionDailySalesPageState
    extends ConsumerState<PromotionDailySalesPage>
    with ThrottledTapMixin {
  static const _weekdays = ['월', '화', '수', '목', '금', '토', '일'];

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) => _initialize());
  }

  /// 진입 초기화. 데이터 로드 후 임시저장이 있으면 복원 팝업을 명령형으로 띄운다.
  ///
  /// build 의 리스너로 값 전이(false→true)를 감지하는 방식은 로드가 리스너 등록보다
  /// 먼저 끝나면 전이를 놓쳐 팝업이 누락될 수 있으므로, 진입 시 여기서 직접 띄운다.
  /// State 는 매 진입마다 새로 생성되어 initState 가 항상 재실행되므로 매번 보장된다.
  Future<void> _initialize() async {
    final notifier = ref.read(
      promotionDailySalesProvider(widget.promotionEmployeeId).notifier,
    );
    await notifier.load();
    if (!mounted) return;

    final state = ref.read(
      promotionDailySalesProvider(widget.promotionEmployeeId),
    );
    if (state.pendingDraftRestore) {
      DraftRestoreDialog.show(
        context,
        onAccept: notifier.acceptDraft,
        onDecline: notifier.declineDraft,
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    final provider = promotionDailySalesProvider(widget.promotionEmployeeId);
    final state = ref.watch(provider);
    final notifier = ref.read(provider.notifier);

    ref.listen<String?>(provider.select((s) => s.errorMessage), (prev, next) {
      if (next != null) {
        ScaffoldMessenger.of(
          context,
        ).showSnackBar(SnackBar(content: Text(next)));
        notifier.clearError();
      }
    });

    // 임시저장 복원 팝업은 진입 시 _initialize() 에서 명령형으로 띄운다.
    // (리스너 값 전이 감지 방식은 로드 타이밍에 따라 누락될 수 있어 사용 안 함)

    return Scaffold(
      appBar: AppBar(title: const Text('일 매출 등록')),
      body: _buildBody(state, notifier),
      bottomNavigationBar: state.editable
          ? _buildBottomBar(state, notifier)
          : null,
    );
  }

  Widget _buildBody(
    PromotionDailySalesState state,
    PromotionDailySalesNotifier notifier,
  ) {
    if (state.isLoading && state.form == null) {
      return const LoadingIndicator(message: '일매출 정보를 불러오는 중...');
    }
    if (state.form == null) {
      return Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(Icons.error_outline, size: 64, color: AppColors.textTertiary),
            const SizedBox(height: AppSpacing.lg),
            Text(
              '정보를 불러올 수 없습니다',
              style: AppTypography.bodyMedium.copyWith(
                color: AppColors.textSecondary,
              ),
            ),
            const SizedBox(height: AppSpacing.lg),
            ElevatedButton(
              onPressed: () => throttledTapAsync(notifier.load),
              child: const Text('재시도'),
            ),
          ],
        ),
      );
    }

    // 숫자 키패드는 iOS 에 '완료' 버튼이 없어 빈 영역 탭 / 스크롤로 키보드를 닫는다.
    return GestureDetector(
      behavior: HitTestBehavior.opaque,
      onTap: () => FocusManager.instance.primaryFocus?.unfocus(),
      child: SingleChildScrollView(
        keyboardDismissBehavior: ScrollViewKeyboardDismissBehavior.onDrag,
        padding: AppSpacing.screenAll,
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            _buildHeader(state),
            const SizedBox(height: AppSpacing.lg),
            if (state.isClosed) ...[
              _buildClosedBanner(),
              const SizedBox(height: AppSpacing.lg),
            ],
            if (state.editable && !state.attendanceRegistered) ...[
              _buildAttendanceWarning(),
              const SizedBox(height: AppSpacing.lg),
            ],
            if (state.editable)
              _buildEditableForm(state, notifier)
            else
              _buildReadOnly(state),
          ],
        ),
      ),
    );
  }

  /// 날짜 + 행사 헤더 (레거시 write.jsp 상단 `날짜` / `행사 *` 필드).
  Widget _buildHeader(PromotionDailySalesState state) {
    final form = state.form!;
    return Container(
      width: double.infinity,
      decoration: BoxDecoration(
        color: AppColors.surface,
        borderRadius: AppSpacing.cardBorderRadius,
        border: Border.all(color: AppColors.border),
      ),
      child: Column(
        children: [
          _headerRow(label: '날짜', value: _formatDate(form.scheduleDate)),
          const Divider(height: 1, color: AppColors.divider),
          _headerRow(
            label: '행사',
            required: true,
            value: form.promotionLabel ?? '-',
            // 마감 전에는 레거시와 동일하게 다른 담당 행사로 바꿀 수 있다.
            onTap: state.editable
                ? () => throttledTapAsync(_changeAssignment)
                : null,
          ),
        ],
      ),
    );
  }

  Widget _headerRow({
    required String label,
    required String value,
    bool required = false,
    VoidCallback? onTap,
  }) {
    final content = Padding(
      padding: AppSpacing.cardPadding,
      child: Row(
        children: [
          Text(
            label,
            style: AppTypography.bodyMedium.copyWith(
              color: AppColors.textSecondary,
            ),
          ),
          if (required)
            Text(
              ' *',
              style: AppTypography.bodyMedium.copyWith(color: AppColors.error),
            ),
          const SizedBox(width: AppSpacing.md),
          Expanded(
            child: Text(
              value,
              textAlign: TextAlign.right,
              style: AppTypography.bodyMedium,
            ),
          ),
          if (onTap != null) ...[
            const SizedBox(width: AppSpacing.xs),
            const Icon(Icons.chevron_right, color: AppColors.textTertiary),
          ],
        ],
      ),
    );
    if (onTap == null) return content;
    return InkWell(onTap: onTap, child: content);
  }

  /// `yyyy-MM-dd` → `yyyy.MM.dd(요일)` (레거시 `today.format('yyyy.MM.dd(KS)')`).
  String _formatDate(String? isoDate) {
    if (isoDate == null || isoDate.isEmpty) return '-';
    final date = DateTime.tryParse(isoDate);
    if (date == null) return isoDate;
    String two(int n) => n.toString().padLeft(2, '0');
    return '${date.year}.${two(date.month)}.${two(date.day)}'
        '(${_weekdays[date.weekday - 1]})';
  }

  /// 담당 행사 재선택 → 선택한 행사조원의 등록 화면으로 교체 이동.
  Future<void> _changeAssignment() async {
    final assignment = await MyAssignmentPickerSheet.show(context);
    if (assignment == null || !mounted) return;
    if (assignment.promotionEmployeeId == widget.promotionEmployeeId) return;

    await AppRouter.navigateToAndReplace<bool>(
      context,
      AppRouter.promotionDailySales,
      arguments: assignment.promotionEmployeeId,
    );
  }

  Widget _buildClosedBanner() {
    return Container(
      width: double.infinity,
      padding: AppSpacing.cardPadding,
      decoration: BoxDecoration(
        color: AppColors.textTertiary.withValues(alpha: 0.12),
        borderRadius: AppSpacing.cardBorderRadius,
      ),
      child: Row(
        children: [
          const Icon(Icons.lock_outline, size: 20),
          const SizedBox(width: AppSpacing.sm),
          Expanded(
            child: Text(
              '이미 등록된 일 매출입니다. 수정할 수 없습니다.',
              style: AppTypography.bodySmall,
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildAttendanceWarning() {
    return Container(
      width: double.infinity,
      padding: AppSpacing.cardPadding,
      decoration: BoxDecoration(
        color: AppColors.warning.withValues(alpha: 0.12),
        borderRadius: AppSpacing.cardBorderRadius,
        border: Border.all(color: AppColors.warning.withValues(alpha: 0.4)),
      ),
      child: Row(
        children: [
          Icon(Icons.warning_amber_rounded, size: 20, color: AppColors.warning),
          const SizedBox(width: AppSpacing.sm),
          Expanded(
            child: Text(
              '출근등록을 완료해야 등록할 수 있습니다.',
              style: AppTypography.bodySmall,
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildEditableForm(
    PromotionDailySalesState state,
    PromotionDailySalesNotifier notifier,
  ) {
    final form = state.form!;
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        ProductInputForm(
          type: ProductType.main,
          productName: form.primaryProductName,
          productCode: form.primaryProductCode,
          quantity: state.mainQuantity,
          amount: state.mainAmount,
          onChanged: ({name, quantity, amount}) {
            notifier.updateMainProduct(quantity: quantity, amount: amount);
          },
        ),
        const SizedBox(height: AppSpacing.md),
        ProductInputForm(
          type: ProductType.sub,
          masterOtherProduct: form.otherProduct,
          name: state.subName,
          quantity: state.subQuantity,
          amount: state.subAmount,
          onChanged: ({name, quantity, amount}) {
            notifier.updateSubProduct(
              name: name,
              quantity: quantity,
              amount: amount,
            );
          },
        ),
        const SizedBox(height: AppSpacing.lg),
        if (state.photo == null && state.form?.imageUrl != null) ...[
          _buildExistingImage(state.form!.imageUrl!),
          const SizedBox(height: AppSpacing.sm),
        ],
        PhotoPickerWidget(
          photo: state.photo,
          onPhotoChanged: notifier.updatePhoto,
        ),
      ],
    );
  }

  Widget _buildReadOnly(PromotionDailySalesState state) {
    final form = state.form!;
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text('일 매출 내역', style: AppTypography.headlineSmall),
        const SizedBox(height: AppSpacing.md),
        _row('대표 제품', form.primaryProductName ?? '-'),
        _row('대표제품 코드', form.primaryProductCode ?? '-'),
        _row('판매 수량', _count(form.primarySalesQuantity)),
        _row('총 판매 금액', _won(form.primaryProductAmount)),
        const SizedBox(height: AppSpacing.md),
        _row('행사 대체 제품', form.description ?? '-'),
        _row('기타제품 판매 수량', _count(form.otherSalesQuantity)),
        _row('기타제품 총 판매 금액', _won(form.otherSalesAmount)),
        if (form.imageUrl != null) ...[
          const SizedBox(height: AppSpacing.lg),
          _buildExistingImage(form.imageUrl!),
        ],
      ],
    );
  }

  Widget _buildExistingImage(String url) {
    return ClipRRect(
      borderRadius: BorderRadius.circular(AppSpacing.radiusSm),
      child: Image.network(
        url,
        width: double.infinity,
        height: 200,
        fit: BoxFit.cover,
        errorBuilder: (_, _, _) => Container(
          height: 200,
          alignment: Alignment.center,
          color: AppColors.surface,
          child: const Text('이미지를 불러올 수 없습니다'),
        ),
      ),
    );
  }

  Widget _row(String label, String value) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: AppSpacing.xs),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          SizedBox(
            width: 140,
            child: Text(
              label,
              style: AppTypography.bodyMedium.copyWith(
                color: AppColors.textSecondary,
              ),
            ),
          ),
          Expanded(child: Text(value, style: AppTypography.bodyMedium)),
        ],
      ),
    );
  }

  String _won(num? amount) {
    if (amount == null) return '-';
    return '${ThousandsSeparatorInputFormatter.format(amount.toInt())}원';
  }

  String _count(num? quantity) {
    if (quantity == null) return '-';
    return '${ThousandsSeparatorInputFormatter.format(quantity.toInt())}개';
  }

  /// 하단 고정 버튼 (레거시: 좌 `임시저장` / 우 `등록`).
  Widget _buildBottomBar(
    PromotionDailySalesState state,
    PromotionDailySalesNotifier notifier,
  ) {
    return Container(
      padding: AppSpacing.screenAll,
      decoration: BoxDecoration(
        color: AppColors.surface,
        border: Border(top: BorderSide(color: AppColors.border)),
      ),
      child: SafeArea(
        child: Row(
          children: [
            Expanded(
              child: SizedBox(
                height: 50,
                child: OutlinedButton(
                  onPressed: state.isSubmitting
                      ? null
                      : () => throttledTapAsync(() => _saveDraft(notifier)),
                  child: const Text('임시저장'),
                ),
              ),
            ),
            const SizedBox(width: AppSpacing.md),
            Expanded(
              child: SizedBox(
                height: 50,
                child: ElevatedButton(
                  onPressed: state.canSubmit && !state.isSubmitting
                      ? () => throttledTapAsync(() => _submit(state, notifier))
                      : null,
                  child: state.isSubmitting
                      ? const SizedBox(
                          width: 24,
                          height: 24,
                          child: CircularProgressIndicator(strokeWidth: 2),
                        )
                      : const Text('등록'),
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }

  /// 등록. 레거시와 동일하게 검증 통과 후 전송 여부를 다시 확인받는다.
  Future<void> _submit(
    PromotionDailySalesState state,
    PromotionDailySalesNotifier notifier,
  ) async {
    if (!notifier.validateForSubmit()) return;

    final label = state.form?.promotionLabel ?? '';
    final confirmed = await _confirmSend(label);
    if (!confirmed || !mounted) return;

    final success = await notifier.submit();
    if (!success || !mounted) return;

    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(content: Text('$label 일 매출이 전송되었습니다')),
    );

    // 레거시는 전송 후 해당 행사 상세(`/sales/view?seq=`)로 이동한다.
    final promotionId = state.form?.promotionId;
    if (promotionId == null) {
      Navigator.pop(context, true);
      return;
    }
    await AppRouter.navigateToAndReplace<void>(
      context,
      AppRouter.promotionDetail,
      arguments: promotionId,
    );
  }

  Future<bool> _confirmSend(String label) async {
    final result = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        content: Text(
          label.isEmpty ? '일 매출을 전송하겠습니까?' : '$label 일 매출을 전송하겠습니까?',
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(ctx).pop(false),
            child: const Text('취소'),
          ),
          TextButton(
            onPressed: () => Navigator.of(ctx).pop(true),
            child: const Text('확인'),
          ),
        ],
      ),
    );
    return result ?? false;
  }

  Future<void> _saveDraft(PromotionDailySalesNotifier notifier) async {
    final success = await notifier.saveDraft();
    if (success && mounted) {
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(const SnackBar(content: Text('작성 중인 내용을 임시 저장했습니다')));
    }
  }
}
