import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:intl/intl.dart';

import '../../core/theme/app_colors.dart';
import '../../core/theme/app_spacing.dart';
import '../../core/theme/app_typography.dart';
import '../../core/utils/error_utils.dart';
import '../../domain/repositories/my_account_repository.dart';
import '../providers/electronic_sales_provider.dart';
import '../providers/electronic_sales_state.dart';
import '../providers/my_accounts_provider.dart';
import '../providers/product_add_provider.dart';
import '../providers/product_add_state.dart';
import '../widgets/account/account_selector_sheet.dart';
import '../widgets/common/error_view.dart';
import '../widgets/common/loading_indicator.dart';
import '../widgets/common/range_calendar_picker.dart';
import '../widgets/electronic/electronic_product_picker_sheet.dart';
import '../widgets/electronic/electronic_sales_result_list.dart';
import 'barcode_scanner_screen.dart';

/// 매출 조회 제품(추가된 제품) 1건.
class _PickedProduct {
  final String productCode;
  final String productName;
  final String barcode;

  const _PickedProduct({
    required this.productCode,
    required this.productName,
    required this.barcode,
  });
}

/// 전산 매출 조회 화면.
///
/// 레거시 `promotion/month/abcmain.jsp` 정합 — 기간 + 거래처 + 매출 조회 제품(제품명 팝업/바코드)
/// 으로 제품별 전산매출을 조회한다. 제품 미선택 시 합계금액만 조회한다(레거시 `abcSumAmount`).
class ElectronicSalesScreen extends ConsumerStatefulWidget {
  const ElectronicSalesScreen({super.key});

  @override
  ConsumerState<ElectronicSalesScreen> createState() =>
      _ElectronicSalesScreenState();
}

class _ElectronicSalesScreenState extends ConsumerState<ElectronicSalesScreen> {
  static final _dateFormat = DateFormat('yyyy-MM-dd');
  static final _numberFormat = NumberFormat('#,###');

  late DateTime _startDate;
  late DateTime _endDate;
  int? _customerId;
  String? _customerName;

  /// 매출 조회 제품 목록 (바코드로 중복 제거)
  final List<_PickedProduct> _products = [];

  /// 최근 추가 바코드 번호 (표시용)
  String? _recentBarcode;

  @override
  void initState() {
    super.initState();
    final now = DateTime.now();
    _startDate = DateTime(now.year, now.month, 1); // 당월 1일
    _endDate = DateTime(now.year, now.month, now.day); // 오늘
    // 화면 진입 시 기본 거래처(내 거래처 첫 거래처)를 자동 선택하고 매출을 조회한다.
    // 레거시 abcmain.jsp 진입 동작 정합.
    WidgetsBinding.instance.addPostFrameCallback((_) => _loadDefaultCustomer());
  }

  /// 진입 시 매출 scope 거래처 목록의 첫 거래처를 자동 선택하고 매출을 조회한다.
  ///
  /// 레거시 abcMain — 컨트롤러가 권한별 거래처 목록 첫 행을 `defaultAccountCode`로 내려주고
  /// `window load` 에서 즉시 `searchSalesList()` 를 호출하던 동작 정합. 매출 scope(부서장이면
  /// 전체) 거래처 목록의 첫 거래처를 선택해 조회한다. 이미 선택돼 있으면 건너뛴다.
  Future<void> _loadDefaultCustomer() async {
    if (_customerId != null) return;
    try {
      final result = await ref
          .read(getMyAccountsUseCaseProvider)
          .call(scope: MyAccountScope.sales);
      if (!mounted || result.accounts.isEmpty) return;
      final first = result.accounts.first;
      setState(() {
        _customerId = first.accountId;
        _customerName = first.accountName;
      });
      _search();
    } catch (_) {
      // 기본 거래처 로딩 실패는 조용히 무시 (사용자가 수동 선택 가능)
    }
  }

  // ── 입력 핸들러 ──────────────────────────────────────────────

  /// 기간 시작일~종료일을 클레임 현황과 동일한 달력 UI 로 선택한다.
  /// 조회 가능 기간은 전산매출 조건(2020 ~ 내년 말)에 맞춘다. 범위 일수 제한은 없다.
  Future<void> _pickDateRange() async {
    final picked = await showRangeCalendar(
      context,
      initialStart: _startDate,
      initialEnd: _endDate,
      firstDate: DateTime(2020),
      lastDate: DateTime(DateTime.now().year + 1, 12, 31),
      maxRangeDays: null,
    );
    if (picked != null) {
      setState(() {
        _startDate = picked.start;
        _endDate = picked.end;
      });
    }
  }

  Future<void> _selectCustomer() async {
    final account = await AccountSelectorSheet.show(
      context,
      scope: MyAccountScope.sales,
    );
    if (account == null || !mounted) return;
    setState(() {
      _customerId = account.accountId;
      _customerName = account.accountName;
    });
  }

  /// 제품명 팝업에서 제품 선택 → 매출 조회 제품에 추가
  Future<void> _pickProduct() async {
    final item = await ElectronicProductPickerSheet.show(context);
    if (item == null || !mounted) return;
    final barcode = item.barcode;
    if (barcode == null || barcode.isEmpty) {
      _showSnack('바코드가 없는 제품은 추가할 수 없습니다');
      return;
    }
    _addProduct(
      _PickedProduct(
        productCode: item.productCode,
        productName: item.productName,
        barcode: barcode,
      ),
    );
  }

  /// 바코드 스캔 → 제품 조회 후 매출 조회 제품에 추가
  ///
  /// 레거시 네이티브 카메라 스캔(`powersales://barcode`) 동등 — 카메라로 스캔한 바코드로
  /// 제품을 조회해 매출 조회 제품에 추가한다(`barcodeValue()` 동등).
  Future<void> _addByBarcode() async {
    final barcode = await BarcodeScannerScreen.show(context);
    if (barcode == null || barcode.isEmpty || !mounted) return;

    try {
      final page = await ref
          .read(productAddDataSourceProvider)
          .searchByFilter(barcode: barcode, size: 20);
      final content = (page['content'] as List<dynamic>? ?? const [])
          .map((e) => ProductAddItem.fromJson(e as Map<String, dynamic>))
          .where((p) => p.barcode != null && p.barcode!.isNotEmpty)
          .toList();

      // 입력 바코드와 정확히 일치하는 제품 우선, 없으면 첫 결과.
      final match = content.firstWhere(
        (p) => p.barcode == barcode,
        orElse: () => content.isNotEmpty
            ? content.first
            : const ProductAddItem(productCode: '', productName: ''),
      );

      if (match.productCode.isEmpty) {
        _showSnack('해당 바코드의 제품을 찾을 수 없습니다');
        return;
      }

      setState(() => _recentBarcode = match.barcode);
      _addProduct(
        _PickedProduct(
          productCode: match.productCode,
          productName: match.productName,
          barcode: match.barcode!,
        ),
      );
    } catch (e) {
      if (mounted) _showSnack(extractErrorMessage(e));
    }
  }

  void _addProduct(_PickedProduct product) {
    if (_products.any((p) => p.barcode == product.barcode)) {
      _showSnack('이미 추가된 제품입니다');
      return;
    }
    setState(() => _products.add(product));
  }

  void _removeProduct(_PickedProduct product) {
    setState(() => _products.remove(product));
  }

  void _showSnack(String message) {
    ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(message)));
  }

  /// 매출 조회 실행 (거래처 필수)
  void _search() {
    final customerId = _customerId;
    if (customerId == null) {
      _showSnack('거래처를 선택하세요');
      return;
    }
    ref.read(electronicSalesProvider.notifier).fetchSales(
          customerId: customerId,
          startDate: _dateFormat.format(_startDate),
          endDate: _dateFormat.format(_endDate),
          barcodes: _products.map((p) => p.barcode).toList(),
        );
  }

  void _reset() {
    final now = DateTime.now();
    setState(() {
      _startDate = DateTime(now.year, now.month, 1);
      _endDate = DateTime(now.year, now.month, now.day);
      _customerId = null;
      _customerName = null;
      _products.clear();
      _recentBarcode = null;
    });
    ref.read(electronicSalesProvider.notifier).reset();
  }

  // ── 빌드 ────────────────────────────────────────────────────

  @override
  Widget build(BuildContext context) {
    final state = ref.watch(electronicSalesProvider);

    return Scaffold(
      appBar: AppBar(
        title: const Text('전산 매출 조회'),
        actions: [
          IconButton(
            icon: const Icon(Icons.refresh),
            onPressed: _reset,
            tooltip: '초기화',
          ),
        ],
      ),
      body: SingleChildScrollView(
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            _periodField(),
            const Divider(height: 1),
            _customerField(),
            const Divider(height: 1),
            _productNameField(),
            const Divider(height: 1),
            _barcodeField(),
            const Divider(height: 1),
            _searchProductField(),
            const Divider(height: 1),
            _totalAmountField(state),
            const Divider(height: 1),
            _resultSection(state),
          ],
        ),
      ),
    );
  }

  /// 공통 필드 컨테이너 (라벨 + 본문)
  Widget _fieldRow({required Widget label, required Widget child}) {
    return Padding(
      padding: const EdgeInsets.symmetric(
        horizontal: AppSpacing.lg,
        vertical: AppSpacing.lg,
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          label,
          const SizedBox(height: AppSpacing.sm),
          child,
        ],
      ),
    );
  }

  Widget _label(String text, {bool required = false}) {
    return RichText(
      text: TextSpan(
        text: text,
        style: AppTypography.bodyLarge.copyWith(
          fontWeight: FontWeight.w700,
          color: AppColors.textPrimary,
        ),
        children: required
            ? [
                TextSpan(
                  text: ' *',
                  style: AppTypography.bodyLarge
                      .copyWith(color: AppColors.legacyDanger),
                ),
              ]
            : null,
      ),
    );
  }

  // 기간
  Widget _periodField() {
    return _fieldRow(
      label: _label('기간', required: true),
      child: InkWell(
        onTap: _pickDateRange,
        child: Padding(
          padding: const EdgeInsets.symmetric(vertical: AppSpacing.xs),
          child: Row(
            children: [
              Expanded(
                child: Text(
                  '${_dateFormat.format(_startDate)} - ${_dateFormat.format(_endDate)}',
                  style: AppTypography.bodyMedium
                      .copyWith(color: AppColors.textSecondary),
                ),
              ),
              const Icon(Icons.calendar_today,
                  size: 18, color: AppColors.textTertiary),
            ],
          ),
        ),
      ),
    );
  }

  // 거래처
  Widget _customerField() {
    return _fieldRow(
      label: _label('거래처', required: true),
      child: InkWell(
        onTap: _selectCustomer,
        child: Padding(
          padding: const EdgeInsets.symmetric(vertical: AppSpacing.xs),
          child: Row(
            children: [
              Expanded(
                child: Text(
                  _customerName ?? '거래처를 선택하세요',
                  style: AppTypography.bodyMedium.copyWith(
                    color: _customerName != null
                        ? AppColors.textPrimary
                        : AppColors.textTertiary,
                  ),
                ),
              ),
              const Icon(Icons.keyboard_arrow_down,
                  color: AppColors.textTertiary),
            ],
          ),
        ),
      ),
    );
  }

  // 제품명
  Widget _productNameField() {
    return _fieldRow(
      label: _label('제품명'),
      child: Row(
        children: [
          Expanded(
            child: Text(
              '제품명 선택',
              style: AppTypography.bodyMedium
                  .copyWith(color: AppColors.textTertiary),
            ),
          ),
          _pillButton(label: '제품명', onPressed: _pickProduct),
        ],
      ),
    );
  }

  // 최근 추가 바코드 번호
  Widget _barcodeField() {
    final hasBarcode = _recentBarcode != null && _recentBarcode!.isNotEmpty;
    return _fieldRow(
      label: _label('최근 추가 바코드 번호'),
      child: Row(
        children: [
          Expanded(
            child: Text(
              hasBarcode ? _recentBarcode! : '바코드를 스캔하세요',
              style: AppTypography.bodyMedium.copyWith(
                color: hasBarcode
                    ? AppColors.textPrimary
                    : AppColors.textTertiary,
              ),
            ),
          ),
          _pillButton(
            label: '바코드',
            icon: Icons.qr_code_scanner,
            onPressed: _addByBarcode,
          ),
        ],
      ),
    );
  }

  // 매출 조회 제품 (+ 매출 조회 버튼)
  Widget _searchProductField() {
    return _fieldRow(
      label: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          _label('매출 조회 제품', required: true),
          ElevatedButton(
            onPressed: _search,
            style: ElevatedButton.styleFrom(
              backgroundColor: AppColors.legacyDanger,
              foregroundColor: AppColors.white,
              padding: const EdgeInsets.symmetric(
                horizontal: AppSpacing.lg,
                vertical: AppSpacing.sm,
              ),
              // 전역 테마의 full-width 기본값 무시 → Row 무한 너비 크래시 방지.
              minimumSize: Size.zero,
              tapTargetSize: MaterialTapTargetSize.shrinkWrap,
            ),
            child: const Text('매출 조회'),
          ),
        ],
      ),
      child: _products.isEmpty
          ? Padding(
              padding: const EdgeInsets.symmetric(vertical: AppSpacing.xs),
              child: Text(
                '제품명/바코드로 조회할 제품을 추가하세요 (미추가 시 합계금액만 조회)',
                style: AppTypography.bodySmall
                    .copyWith(color: AppColors.textTertiary),
              ),
            )
          : Column(
              children: _products
                  .map((p) => _productChip(p))
                  .toList(),
            ),
    );
  }

  Widget _productChip(_PickedProduct product) {
    return Container(
      margin: const EdgeInsets.only(top: AppSpacing.sm),
      padding: const EdgeInsets.symmetric(
        horizontal: AppSpacing.md,
        vertical: AppSpacing.sm,
      ),
      decoration: BoxDecoration(
        color: AppColors.surface,
        borderRadius: BorderRadius.circular(AppSpacing.radiusMd),
      ),
      child: Row(
        children: [
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  product.productName,
                  style: AppTypography.bodyMedium
                      .copyWith(fontWeight: FontWeight.w700),
                ),
                const SizedBox(height: AppSpacing.xxs),
                Text(
                  '바코드 : ${product.barcode}',
                  style: AppTypography.bodySmall
                      .copyWith(color: AppColors.textSecondary),
                ),
              ],
            ),
          ),
          IconButton(
            icon: const Icon(Icons.close, size: 20),
            color: AppColors.textTertiary,
            onPressed: () => _removeProduct(product),
            tooltip: '삭제',
          ),
        ],
      ),
    );
  }

  // 합계금액
  Widget _totalAmountField(ElectronicSalesState state) {
    final amount = state.hasSearched ? state.totalAmount : 0;
    return _fieldRow(
      label: _label('합계금액'),
      child: Text(
        '${_numberFormat.format(amount)}원',
        style: AppTypography.bodyLarge.copyWith(
          fontWeight: FontWeight.w700,
          color: AppColors.legacyDanger,
        ),
      ),
    );
  }

  // 결과 (로딩/에러/제품별 카드)
  Widget _resultSection(ElectronicSalesState state) {
    if (state.isLoading) {
      return const Padding(
        padding: EdgeInsets.symmetric(vertical: AppSpacing.xxxl),
        child: LoadingIndicator(message: '전산매출 조회 중...'),
      );
    }

    if (state.errorMessage != null) {
      return Padding(
        padding: const EdgeInsets.symmetric(vertical: AppSpacing.xxl),
        child: ErrorView(
          message: '조회 중 오류가 발생했습니다',
          description: state.errorMessage,
          onRetry: _search,
          isFullScreen: false,
        ),
      );
    }

    if (state.sales.isEmpty) {
      // 미조회 또는 (합계만 조회/결과 없음) — 빈 영역 유지
      return const SizedBox.shrink();
    }

    return ElectronicSalesResultList(salesList: state.sales);
  }

  Widget _pillButton({
    required String label,
    required VoidCallback onPressed,
    IconData? icon,
  }) {
    final style = OutlinedButton.styleFrom(
      foregroundColor: AppColors.textPrimary,
      side: const BorderSide(color: AppColors.divider),
      shape: const StadiumBorder(),
      padding: const EdgeInsets.symmetric(
        horizontal: AppSpacing.lg,
        vertical: AppSpacing.sm,
      ),
      // 전역 테마의 full-width(minimumSize width=infinity) 기본값을 무시하고
      // 콘텐츠 크기에 맞춘다. Row 안에서 무한 너비 제약 크래시 방지.
      minimumSize: Size.zero,
      tapTargetSize: MaterialTapTargetSize.shrinkWrap,
    );
    if (icon == null) {
      return OutlinedButton(
        onPressed: onPressed,
        style: style,
        child: Text(label),
      );
    }
    return OutlinedButton.icon(
      onPressed: onPressed,
      icon: Icon(icon, size: 16),
      label: Text(label),
      style: style,
    );
  }
}
