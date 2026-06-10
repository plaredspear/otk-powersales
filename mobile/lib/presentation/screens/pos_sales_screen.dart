import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/theme/app_colors.dart';
import '../../domain/entities/pos_product.dart';
import '../../domain/entities/pos_sales.dart';
import '../../domain/repositories/my_account_repository.dart';
import '../providers/pos_sales_provider.dart';
import '../providers/pos_sales_state.dart';
import '../widgets/account/account_selector_sheet.dart';
import '../widgets/common/range_calendar_picker.dart';
import '../widgets/pos/pos_product_search_sheet.dart';
import 'barcode_scanner_screen.dart';

/// POS 매출 조회 화면.
///
/// 레거시 Heroku `promotion/month/posmain.jsp` 동등 디자인 — 기간(날짜범위) + 거래처 선택,
/// 제품명 검색/바코드 스캔으로 매출 조회 제품을 누적한 뒤 "매출 조회" 로 합계금액/명세를 조회한다.
class PosSalesScreen extends ConsumerWidget {
  const PosSalesScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final state = ref.watch(posSalesProvider);
    final notifier = ref.read(posSalesProvider.notifier);

    return Scaffold(
      backgroundColor: AppColors.white,
      appBar: AppBar(
        title: const Text('POS 매출 조회'),
        actions: [
          IconButton(
            icon: const Icon(Icons.refresh),
            onPressed: notifier.reset,
            tooltip: '초기화',
          ),
        ],
      ),
      body: SafeArea(
        child: SingleChildScrollView(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              _periodRow(context, ref, state),
              _customerRow(context, ref, state),
              _recentProductRow(context, ref, state),
              _recentBarcodeRow(context, ref, state),
              _inquiryProductRow(context, ref, state),
              if (state.addedProducts.isNotEmpty)
                _addedProductList(ref, state),
              Container(height: 8, color: AppColors.surface),
              _totalAmountRow(state),
              if (state.detailMode) _resultList(state),
              if (state.isLoading)
                const Padding(
                  padding: EdgeInsets.all(24),
                  child: Center(child: CircularProgressIndicator()),
                ),
              const SizedBox(height: 24),
            ],
          ),
        ),
      ),
    );
  }

  // ── 기간 ──────────────────────────────────────────────
  Widget _periodRow(BuildContext context, WidgetRef ref, PosSalesState state) {
    return _row(
      label: '기간',
      required: true,
      onTap: () => _pickDateRange(context, ref, state),
      child: Text(
        '${state.startDate} - ${state.endDate}',
        style: const TextStyle(fontSize: 16, color: AppColors.textSecondary),
      ),
    );
  }

  /// 기간 시작일~종료일을 클레임 현황과 동일한 달력 UI 로 선택한다.
  /// 조회 가능 기간은 POS 매출 조건(2020 ~ 2030)에 맞추고,
  /// 레거시 daterangepicker maxSpan 정합으로 최대 31일까지만 선택할 수 있다.
  Future<void> _pickDateRange(
      BuildContext context, WidgetRef ref, PosSalesState state) async {
    final picked = await showRangeCalendar(
      context,
      initialStart: DateTime.parse(state.startDate),
      initialEnd: DateTime.parse(state.endDate),
      firstDate: DateTime(2020),
      lastDate: DateTime(2030),
      maxRangeDays: 31,
    );
    if (picked == null) return;

    ref
        .read(posSalesProvider.notifier)
        .setDateRange(_fmt(picked.start), _fmt(picked.end));
  }

  static String _fmt(DateTime d) =>
      '${d.year}-${d.month.toString().padLeft(2, '0')}-${d.day.toString().padLeft(2, '0')}';

  // ── 거래처 ────────────────────────────────────────────
  Widget _customerRow(BuildContext context, WidgetRef ref, PosSalesState state) {
    final name = state.selectedCustomerName;
    return _row(
      label: '거래처',
      required: true,
      trailing: const Icon(Icons.expand_more, color: AppColors.textSecondary),
      onTap: () async {
        final account = await AccountSelectorSheet.show(
          context,
          scope: MyAccountScope.sales,
        );
        if (account == null) return;
        await ref
            .read(posSalesProvider.notifier)
            .selectCustomer(account.accountId, account.accountName);
      },
      child: Text(
        name ?? '거래처를 선택하세요',
        style: TextStyle(
          fontSize: 16,
          color: name != null
              ? AppColors.textPrimary
              : AppColors.legacyPlaceholder,
        ),
      ),
    );
  }

  // ── 최근 추가 제품 ─────────────────────────────────────
  Widget _recentProductRow(
      BuildContext context, WidgetRef ref, PosSalesState state) {
    return _row(
      label: '최근 추가 제품',
      trailing: _pillButton(
        label: '제품명',
        onPressed: () async {
          final products = await PosProductSearchSheet.show(context);
          if (products == null || products.isEmpty) return;
          final notifier = ref.read(posSalesProvider.notifier);
          for (final p in products) {
            notifier.addProduct(p);
          }
        },
      ),
      child: Text(
        state.lastAddedProductName ?? '제품명 선택',
        style: TextStyle(
          fontSize: 16,
          color: state.lastAddedProductName != null
              ? AppColors.textSecondary
              : AppColors.legacyPlaceholder,
        ),
      ),
    );
  }

  // ── 최근 추가 바코드 ───────────────────────────────────
  Widget _recentBarcodeRow(
      BuildContext context, WidgetRef ref, PosSalesState state) {
    return _row(
      label: '최근 추가 바코드',
      trailing: _pillButton(
        label: '바코드',
        icon: Icons.qr_code_scanner,
        onPressed: () => _scanBarcode(context, ref),
      ),
      child: Text(
        state.lastScannedBarcode ?? '바코드를 스캔하세요',
        style: TextStyle(
          fontSize: 16,
          color: state.lastScannedBarcode != null
              ? AppColors.textSecondary
              : AppColors.legacyPlaceholder,
        ),
      ),
    );
  }

  Future<void> _scanBarcode(BuildContext context, WidgetRef ref) async {
    final barcode = await BarcodeScannerScreen.show(context);
    if (barcode == null || !context.mounted) return;

    final notifier = ref.read(posSalesProvider.notifier);
    notifier.setLastScannedBarcode(barcode);
    try {
      final product =
          await ref.read(posProductUseCaseProvider).findByBarcode(barcode);
      if (!context.mounted) return;
      if (product == null) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('해당 제품이 없습니다')),
        );
        return;
      }
      // 스캔한 바코드를 POS 매칭 키로 사용 (POS BARCODE 정합)
      notifier.addProduct(PosProduct(
        productCode: product.productCode,
        productName: product.productName,
        barcode: product.barcode.isNotEmpty ? product.barcode : barcode,
      ));
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('${product.productName} 추가됨')),
      );
    } catch (_) {
      if (!context.mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('제품 조회에 실패했습니다')),
      );
    }
  }

  // ── 매출 조회 제품 ─────────────────────────────────────
  Widget _inquiryProductRow(
      BuildContext context, WidgetRef ref, PosSalesState state) {
    final enabled = state.selectedCustomerId != null && !state.isLoading;
    return _row(
      label: '매출 조회 제품',
      required: true,
      child: Text(
        state.addedProducts.isEmpty
            ? '제품 미선택 시 거래처 전체 합계를 조회합니다'
            : '${state.addedProducts.length}개 제품',
        style: const TextStyle(fontSize: 14, color: AppColors.legacyPlaceholder),
      ),
      trailing: ElevatedButton(
        onPressed:
            enabled ? () => ref.read(posSalesProvider.notifier).fetchSales() : null,
        style: ElevatedButton.styleFrom(
          backgroundColor: AppColors.otokiRed,
          foregroundColor: AppColors.white,
          disabledBackgroundColor: AppColors.surface,
          disabledForegroundColor: AppColors.textSecondary,
          // 전역 테마의 full-width(minimumSize width=infinity) 기본값을 무시하고
          // 콘텐츠 크기에 맞춘다. Row 안에서 무한 너비 제약 크래시 방지.
          minimumSize: Size.zero,
          padding: const EdgeInsets.symmetric(horizontal: 18, vertical: 12),
          shape:
              RoundedRectangleBorder(borderRadius: BorderRadius.circular(8)),
        ),
        child: const Text('매출 조회',
            style: TextStyle(fontWeight: FontWeight.w600)),
      ),
    );
  }

  // ── 매출 조회 제품 누적 목록 ───────────────────────────
  Widget _addedProductList(WidgetRef ref, PosSalesState state) {
    final notifier = ref.read(posSalesProvider.notifier);
    return Container(
      color: AppColors.surface,
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
      child: Column(
        children: state.addedProducts.map((p) {
          final key = PosSalesState.keyOf(p);
          final checked = state.checkedKeys.contains(key);
          return Card(
            margin: const EdgeInsets.symmetric(vertical: 3),
            elevation: 0,
            shape: RoundedRectangleBorder(
              borderRadius: BorderRadius.circular(8),
              side: const BorderSide(color: AppColors.divider),
            ),
            child: ListTile(
              dense: true,
              leading: Checkbox(
                value: checked,
                activeColor: AppColors.otokiRed,
                onChanged: (_) => notifier.toggleChecked(key),
              ),
              title: Text(p.productName,
                  style: const TextStyle(fontSize: 14)),
              subtitle: Text(
                p.barcode.isEmpty ? '바코드 없음' : '바코드 ${p.barcode}',
                style: const TextStyle(
                    fontSize: 12, color: AppColors.textSecondary),
              ),
              trailing: IconButton(
                icon: const Icon(Icons.close, size: 20),
                color: AppColors.textSecondary,
                onPressed: () => notifier.removeProduct(key),
              ),
            ),
          );
        }).toList(),
      ),
    );
  }

  // ── 합계금액 ───────────────────────────────────────────
  Widget _totalAmountRow(PosSalesState state) {
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Text(
            '합계금액',
            style: TextStyle(
                fontSize: 15,
                fontWeight: FontWeight.bold,
                color: AppColors.textPrimary),
          ),
          const SizedBox(height: 6),
          Text(
            '${_currency(state.totalAmount)}원',
            style: const TextStyle(fontSize: 16, color: AppColors.textPrimary),
          ),
        ],
      ),
    );
  }

  // ── 명세 목록 (명세 모드) ─────────────────────────────
  Widget _resultList(PosSalesState state) {
    if (state.resultItems.isEmpty) {
      return const Padding(
        padding: EdgeInsets.symmetric(horizontal: 20, vertical: 8),
        child: Text('검색 결과가 존재하지 않습니다',
            style: TextStyle(fontSize: 14, color: AppColors.textSecondary)),
      );
    }
    return Column(
      children: [
        const Divider(height: 1),
        ...state.resultItems.map(_resultCard),
      ],
    );
  }

  Widget _resultCard(PosSales item) {
    return Container(
      decoration: const BoxDecoration(
        border: Border(bottom: BorderSide(color: AppColors.divider)),
      ),
      padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 12),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          _detailLine('제품명', item.productName),
          _detailLine('제품코드', _stripZero(item.productCode)),
          _detailLine('바코드', item.barcode ?? '-'),
          _detailLine('납품 수량', '${item.quantity}EA'),
          _detailLine('금액', '${_currency(item.amount)}원'),
        ],
      ),
    );
  }

  Widget _detailLine(String label, String value) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 2),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          SizedBox(
            width: 72,
            child: Text(label,
                style: const TextStyle(
                    fontSize: 13, color: AppColors.textSecondary)),
          ),
          Expanded(
            child: Text(value, style: const TextStyle(fontSize: 13)),
          ),
        ],
      ),
    );
  }

  // ── 공통 위젯 ─────────────────────────────────────────
  /// 레거시 plain list 행: 라벨(필수 *) + 값/플레이스홀더 + 우측 액션.
  Widget _row({
    required String label,
    bool required = false,
    required Widget child,
    Widget? trailing,
    VoidCallback? onTap,
  }) {
    return InkWell(
      onTap: onTap,
      child: Container(
        decoration: const BoxDecoration(
          border: Border(bottom: BorderSide(color: AppColors.divider)),
        ),
        padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 16),
        child: Row(
          crossAxisAlignment: CrossAxisAlignment.center,
          children: [
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  _label(label, required),
                  const SizedBox(height: 6),
                  child,
                ],
              ),
            ),
            if (trailing != null) ...[
              const SizedBox(width: 12),
              trailing,
            ],
          ],
        ),
      ),
    );
  }

  Widget _label(String label, bool required) {
    return RichText(
      text: TextSpan(
        text: label,
        style: const TextStyle(
          fontSize: 15,
          fontWeight: FontWeight.bold,
          color: AppColors.textPrimary,
        ),
        children: required
            ? const [
                TextSpan(
                  text: ' *',
                  style: TextStyle(color: AppColors.otokiRed),
                ),
              ]
            : null,
      ),
    );
  }

  Widget _pillButton({
    required String label,
    IconData? icon,
    required VoidCallback onPressed,
  }) {
    return OutlinedButton.icon(
      onPressed: onPressed,
      icon: icon != null
          ? Icon(icon, size: 16, color: AppColors.textPrimary)
          : const SizedBox.shrink(),
      label: Text(label,
          style: const TextStyle(fontSize: 14, color: AppColors.textPrimary)),
      style: OutlinedButton.styleFrom(
        side: const BorderSide(color: AppColors.textSecondary),
        shape: const StadiumBorder(),
        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
        minimumSize: const Size(0, 0),
        tapTargetSize: MaterialTapTargetSize.shrinkWrap,
      ),
    );
  }

  String _currency(int amount) => amount.toString().replaceAllMapped(
        RegExp(r'(\d{1,3})(?=(\d{3})+(?!\d))'),
        (m) => '${m[1]},',
      );

  String _stripZero(String code) {
    final stripped = code.replaceFirst(RegExp(r'^0+'), '');
    return stripped.isEmpty ? '0' : stripped;
  }
}
