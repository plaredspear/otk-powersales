import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../app_router.dart';
import '../../core/theme/app_colors.dart';
import '../providers/safety_check_provider.dart';
import '../providers/safety_check_state.dart';
import '../widgets/common/loading_indicator.dart';
import '../widgets/common/error_view.dart';
import '../widgets/safety_check/safety_check_category_section.dart';

/// 안전점검 화면 (V1: 라디오 + 체크박스 2섹션)
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
        onPressed: () => Navigator.of(context).pop(),
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
    if (state.isLoading && state.categories == null) {
      return const LoadingIndicator(message: '체크리스트를 불러오는 중...');
    }

    if (state.isError && state.categories == null) {
      return ErrorView(
        message: '체크리스트를 불러올 수 없습니다',
        description: state.errorMessage,
        onRetry: () {
          ref.read(safetyCheckProvider.notifier).fetchItems();
        },
      );
    }

    if (state.categories == null || state.categories!.isEmpty) {
      return const ErrorView.noData(
        message: '체크리스트 항목이 없습니다',
      );
    }

    return Stack(
      children: [
        SingleChildScrollView(
          padding: const EdgeInsets.only(bottom: 100),
          child: Column(
            children: state.categories!
                .map(
                  (category) => SafetyCheckCategorySection(
                    category: category,
                    equipmentAnswers: state.equipmentAnswers,
                    precautionChecks: state.precautionChecks,
                    expandedItemIndex: state.expandedItemIndex,
                    onRadioSelect: (seqNum, answer) {
                      ref
                          .read(safetyCheckProvider.notifier)
                          .setEquipmentAnswer(seqNum, answer);
                    },
                    onCheckboxToggle: (seqNum) {
                      ref
                          .read(safetyCheckProvider.notifier)
                          .togglePrecaution(seqNum);
                    },
                    onToggleExpand: (seqNum) {
                      ref
                          .read(safetyCheckProvider.notifier)
                          .toggleExpand(seqNum);
                    },
                  ),
                )
                .toList(),
          ),
        ),
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
            Expanded(
              child: OutlinedButton(
                onPressed:
                    state.isSubmitting ? null : () => Navigator.of(context).pop(),
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
            Expanded(
              flex: 2,
              child: ElevatedButton(
                onPressed: isEnabled
                    ? () => ref.read(safetyCheckProvider.notifier).submit()
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

  /// 제출 성공 → 토스트 + AttendancePage로 교체
  void _onSubmitSuccess() {
    ScaffoldMessenger.of(context).showSnackBar(
      const SnackBar(
        content: Text('안전점검이 완료되었습니다.'),
        backgroundColor: AppColors.success,
        duration: Duration(seconds: 2),
      ),
    );

    // SafetyCheckPage를 스택에서 교체하여 AttendancePage로 이동
    Navigator.of(context).pushReplacementNamed(AppRouter.attendance);
  }
}
