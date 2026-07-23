import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
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
  /// 항목(seqNum)별 GlobalKey — 답변 후 다음 항목 스크롤용
  final Map<int, GlobalKey> _itemKeys = {};

  GlobalKey _keyFor(int seqNum) =>
      _itemKeys.putIfAbsent(seqNum, () => GlobalKey());

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

    // 상태바 영역은 흰색 유지, 그 아래 올리브그린 띠(KV 헤더)만 배치
    return AnnotatedRegion<SystemUiOverlayStyle>(
      value: SystemUiOverlayStyle.dark,
      child: Scaffold(
        backgroundColor: AppColors.background,
        body: SafeArea(
          bottom: false,
          child: Column(
            children: [
              _buildHeaderBand(),
              Expanded(child: _buildBody(state)),
            ],
          ),
        ),
        bottomNavigationBar: _buildBottomBar(state),
      ),
    );
  }

  /// 레거시(checkList.jsp) KV 헤더: 올리브그린 띠 + 오뚜기 엠블럼 + 좌측 타이틀(#333)
  Widget _buildHeaderBand() {
    return Container(
      width: double.infinity,
      color: AppColors.legacyKvGreen,
      padding: const EdgeInsets.fromLTRB(4, 12, 16, 12),
      child: Row(
        children: [
          IconButton(
            icon: const Icon(Icons.arrow_back),
            color: AppColors.legacyTextSub,
            onPressed: () => Navigator.of(context).pop(),
          ),
          Image.asset(
            'assets/images/otoki_emblem.png',
            height: 40,
            fit: BoxFit.contain,
          ),
          const SizedBox(width: 12),
          const Expanded(
            child: Text(
              '판매여사원 매장 일일 안전점검\n체크리스트 [출근시 작성]',
              style: TextStyle(
                fontSize: 15,
                height: 1.3,
                fontWeight: FontWeight.w700,
                color: AppColors.legacyTextSub,
              ),
            ),
          ),
        ],
      ),
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

    // RADIO 항목 GlobalKey 사전 확보 (스크롤 대상)
    for (final category in state.categories!) {
      if (category.inputType == 'RADIO') {
        for (final item in category.items) {
          _keyFor(item.seqNum);
        }
      }
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
                    expandedSeqNums: state.expandedSeqNums,
                    itemKeys: _itemKeys,
                    onRadioSelect: (seqNum, answer) {
                      ref
                          .read(safetyCheckProvider.notifier)
                          .setEquipmentAnswer(seqNum, answer);
                      _scrollToNextItem(seqNum);
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
        // 레거시 wrapper_buttons: 취소(35%)·제출(65%) 모두 #006DB2 흰 글씨
        child: Row(
          children: [
            Expanded(
              flex: 35,
              child: ElevatedButton(
                onPressed:
                    state.isSubmitting ? null : () => Navigator.of(context).pop(),
                style: ElevatedButton.styleFrom(
                  padding: const EdgeInsets.symmetric(vertical: 14),
                  backgroundColor: AppColors.legacyCheckBlue,
                  foregroundColor: AppColors.white,
                  elevation: 0,
                  shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(8),
                  ),
                ),
                child: const Text(
                  '취소',
                  style: TextStyle(
                    fontSize: 16,
                    fontWeight: FontWeight.bold,
                  ),
                ),
              ),
            ),
            const SizedBox(width: 12),
            Expanded(
              flex: 65,
              child: ElevatedButton(
                onPressed: isEnabled
                    ? () => ref.read(safetyCheckProvider.notifier).submit()
                    : null,
                style: ElevatedButton.styleFrom(
                  padding: const EdgeInsets.symmetric(vertical: 14),
                  backgroundColor: AppColors.legacyCheckBlue,
                  foregroundColor: AppColors.white,
                  disabledBackgroundColor: AppColors.surfaceVariant,
                  disabledForegroundColor: AppColors.textTertiary,
                  elevation: 0,
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

  /// 답변 선택 후, 다음 항목이 화면에 충분히 보이도록 스크롤
  void _scrollToNextItem(int answeredSeqNum) {
    final categories = ref.read(safetyCheckProvider).categories;
    if (categories == null) return;

    // RADIO 항목 seqNum 순서 목록
    final radioSeqNums = <int>[
      for (final category in categories)
        if (category.inputType == 'RADIO')
          for (final item in category.items) item.seqNum,
    ];

    final idx = radioSeqNums.indexOf(answeredSeqNum);
    if (idx == -1 || idx + 1 >= radioSeqNums.length) return;
    final nextSeqNum = radioSeqNums[idx + 1];

    // 선택 항목 접힘(300ms) 이후 레이아웃 안정 시점에 스크롤
    Future.delayed(const Duration(milliseconds: 350), () {
      if (!mounted) return;
      final ctx = _itemKeys[nextSeqNum]?.currentContext;
      if (ctx == null || !ctx.mounted) return;
      Scrollable.ensureVisible(
        ctx,
        alignment: 0.3,
        duration: const Duration(milliseconds: 300),
        curve: Curves.easeInOut,
      );
    });
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
