import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/theme/app_colors.dart';
import '../providers/safety_check_provider.dart';
import '../providers/safety_check_state.dart';
import '../widgets/common/loading_indicator.dart';
import '../widgets/common/error_view.dart';
import '../widgets/safety_check/safety_check_category_section.dart';

/// 안전점검 화면
///
/// 출근등록 전 안전점검 체크리스트를 표시합니다.
/// 모든 필수 항목을 체크해야 제출 버튼이 활성화됩니다.
class SafetyCheckPage extends ConsumerStatefulWidget {
  const SafetyCheckPage({super.key});

  @override
  ConsumerState<SafetyCheckPage> createState() => _SafetyCheckPageState();
}

class _SafetyCheckPageState extends ConsumerState<SafetyCheckPage> {
  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      ref.read(safetyCheckProvider.notifier).fetchItems();
    });
  }

  @override
  Widget build(BuildContext context) {
    final state = ref.watch(safetyCheckProvider);

    // 제출 완료 시 다음 화면으로 이동
    ref.listen<SafetyCheckState>(safetyCheckProvider, (previous, next) {
      if (next.isSubmitted && !(previous?.isSubmitted ?? false)) {
        _onSubmitSuccess();
      }
    });

    return Scaffold(
      appBar: _buildAppBar(),
      body: _buildBody(state),
      bottomNavigationBar: _buildBottomBar(state),
    );
  }

  PreferredSizeWidget _buildAppBar() {
    return AppBar(
      leading: IconButton(
        icon: const Icon(Icons.arrow_back),
        onPressed: () => _onCancel(),
      ),
      title: const Text(
        '판매여사원 매장 일일 안전점검\n체크리스트 [출근시 작성]',
        style: TextStyle(fontSize: 15, height: 1.3),
        textAlign: TextAlign.center,
      ),
      centerTitle: true,
      toolbarHeight: 64,
    );
  }

  Widget _buildBody(SafetyCheckState state) {
    // 초기 로딩 (데이터 없음 + 로딩 중)
    if (state.isLoading && state.categories == null) {
      return const LoadingIndicator(message: '체크리스트를 불러오는 중...');
    }

    // 에러 상태 (데이터 없음)
    if (state.isError && state.categories == null) {
      return ErrorView(
        message: '체크리스트를 불러올 수 없습니다',
        description: state.errorMessage,
        onRetry: () {
          ref.read(safetyCheckProvider.notifier).fetchItems();
        },
      );
    }

    // 데이터가 없는 경우
    if (state.categories == null || state.categories!.isEmpty) {
      return const ErrorView.noData(
        message: '체크리스트 항목이 없습니다',
      );
    }

    // 제출 중 오버레이
    return Stack(
      children: [
        // 체크리스트 본문
        SingleChildScrollView(
          padding: const EdgeInsets.only(bottom: 100),
          child: Column(
            children: state.categories!
                .map(
                  (category) => SafetyCheckCategorySection(
                    category: category,
                    checkedItems: state.checkedItems,
                    onToggle: (itemId) {
                      ref
                          .read(safetyCheckProvider.notifier)
                          .toggleItem(itemId);
                    },
                  ),
                )
                .toList(),
          ),
        ),
        // 제출 중 오버레이
        if (state.isSubmitting)
          const OverlayLoadingIndicator(message: '제출 중...'),
      ],
    );
  }

  Widget _buildBottomBar(SafetyCheckState state) {
    final isEnabled = state.allRequiredChecked && !state.isSubmitting;

    return Container(
      padding: const EdgeInsets.all(16),
      decoration: const BoxDecoration(
        color: AppColors.background,
        border: Border(
          top: BorderSide(color: AppColors.divider, width: 1),
        ),
      ),
      child: SafeArea(
        child: Row(
          children: [
            // 취소 버튼
            Expanded(
              child: OutlinedButton(
                onPressed: state.isSubmitting ? null : () => _onCancel(),
                style: OutlinedButton.styleFrom(
                  padding: const EdgeInsets.symmetric(vertical: 14),
                  side: const BorderSide(color: AppColors.divider),
                  shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(8),
                  ),
                ),
                child: const Text(
                  '취소',
                  style: TextStyle(
                    fontSize: 16,
                    color: AppColors.textSecondary,
                  ),
                ),
              ),
            ),
            const SizedBox(width: 12),
            // 제출 버튼
            Expanded(
              flex: 2,
              child: ElevatedButton(
                onPressed: isEnabled
                    ? () =>
                        ref.read(safetyCheckProvider.notifier).submit()
                    : null,
                style: ElevatedButton.styleFrom(
                  padding: const EdgeInsets.symmetric(vertical: 14),
                  backgroundColor: AppColors.primary,
                  foregroundColor: AppColors.onPrimary,
                  disabledBackgroundColor: AppColors.surfaceVariant,
                  disabledForegroundColor: AppColors.textTertiary,
                  shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(8),
                  ),
                ),
                child: const Text(
                  '제출',
                  style: TextStyle(
                    fontSize: 16,
                    fontWeight: FontWeight.bold,
                  ),
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }

  /// 취소 버튼 탭 시
  void _onCancel() {
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('안전점검 취소'),
        content: const Text('안전점검을 취소하시겠습니까?\n작성 중인 내용은 저장되지 않습니다.'),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(context).pop(),
            child: const Text('계속 작성'),
          ),
          TextButton(
            onPressed: () {
              Navigator.of(context).pop(); // 다이얼로그 닫기
              Navigator.of(context).pop(); // 안전점검 화면 닫기 (홈으로)
            },
            child: const Text(
              '취소',
              style: TextStyle(color: AppColors.error),
            ),
          ),
        ],
      ),
    );
  }

  /// 제출 성공 시
  void _onSubmitSuccess() {
    ScaffoldMessenger.of(context).showSnackBar(
      const SnackBar(
        content: Text('안전점검이 완료되었습니다.'),
        backgroundColor: AppColors.success,
        duration: Duration(seconds: 2),
      ),
    );

    // TODO: F8 출근등록 거래처 목록 화면으로 이동
    // 현재는 이전 화면(홈)으로 돌아감
    Navigator.of(context).pop(true); // true = 안전점검 완료
  }
}
