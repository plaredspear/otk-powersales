import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:intl/intl.dart';

import '../../core/theme/app_colors.dart';
import '../../domain/entities/staff_evaluation.dart';
import '../providers/staff_evaluation_provider.dart';
import '../providers/staff_evaluation_state.dart';
import '../widgets/common/error_view.dart';
import '../widgets/common/loading_indicator.dart';

/// 여사원 평가조회 화면
///
/// 레거시 `mypage/evaluationList.jsp` 정합 — 월 네비게이션 + 지점평가 점수 +
/// 담당 거래처별 목표/실적/달성률(본부평가) 표시. 본인(인증 사용자) 기준 조회.
class StaffEvaluationPage extends ConsumerStatefulWidget {
  const StaffEvaluationPage({super.key});

  @override
  ConsumerState<StaffEvaluationPage> createState() =>
      _StaffEvaluationPageState();
}

class _StaffEvaluationPageState extends ConsumerState<StaffEvaluationPage> {
  static final _numberFormat = NumberFormat('#,###');

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      ref.read(staffEvaluationProvider.notifier).initialize();
    });
  }

  @override
  Widget build(BuildContext context) {
    final state = ref.watch(staffEvaluationProvider);
    final notifier = ref.read(staffEvaluationProvider.notifier);

    return Scaffold(
      backgroundColor: AppColors.white,
      appBar: AppBar(title: const Text('평가조회')),
      body: Column(
        children: [
          _MonthNavigator(
            label: state.displayYearMonth,
            canGoPrevious: state.canGoToPreviousMonth && !state.isLoading,
            canGoNext: state.canGoToNextMonth && !state.isLoading,
            onPrevious: notifier.goToPreviousMonth,
            onNext: notifier.goToNextMonth,
          ),
          const Divider(height: 1, color: AppColors.divider),
          Expanded(child: _buildBody(state, notifier)),
        ],
      ),
    );
  }

  Widget _buildBody(StaffEvaluationState state, StaffEvaluationNotifier notifier) {
    if (state.isLoading && !state.hasData) {
      return const LoadingIndicator(
        message: '평가 정보를 불러오는 중...',
        color: AppColors.secondary,
      );
    }

    if (state.errorMessage != null && !state.hasData) {
      return ErrorView(
        message: '평가 정보를 불러올 수 없습니다',
        description: state.errorMessage,
        onRetry: notifier.refresh,
      );
    }

    final evaluation = state.evaluation;
    if (evaluation == null) {
      return const Center(child: Text('평가 데이터가 없습니다'));
    }

    return RefreshIndicator(
      color: AppColors.secondary,
      onRefresh: notifier.refresh,
      child: ListView(
        padding: const EdgeInsets.symmetric(vertical: 16),
        children: [
          _BranchScoreCard(
            score: evaluation.branchScore,
            maxScore: evaluation.branchMaxScore,
          ),
          const SizedBox(height: 16),
          _SectionTitle('본부평가'),
          const SizedBox(height: 8),
          _AccountEvaluationTable(
            accounts: evaluation.accounts,
            numberFormat: _numberFormat,
          ),
        ],
      ),
    );
  }
}

/// 월 네비게이션 (이전/다음 + 연월 표시)
class _MonthNavigator extends StatelessWidget {
  final String label;
  final bool canGoPrevious;
  final bool canGoNext;
  final VoidCallback onPrevious;
  final VoidCallback onNext;

  const _MonthNavigator({
    required this.label,
    required this.canGoPrevious,
    required this.canGoNext,
    required this.onPrevious,
    required this.onNext,
  });

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 8),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          IconButton(
            onPressed: canGoPrevious ? onPrevious : null,
            icon: const Icon(Icons.chevron_left),
          ),
          SizedBox(
            width: 140,
            child: Text(
              label,
              textAlign: TextAlign.center,
              style: const TextStyle(
                fontSize: 18,
                fontWeight: FontWeight.bold,
                color: AppColors.black,
              ),
            ),
          ),
          IconButton(
            onPressed: canGoNext ? onNext : null,
            icon: const Icon(Icons.chevron_right),
          ),
        ],
      ),
    );
  }
}

/// 지점평가 점수 카드
class _BranchScoreCard extends StatelessWidget {
  final double? score;
  final int maxScore;

  const _BranchScoreCard({required this.score, required this.maxScore});

  @override
  Widget build(BuildContext context) {
    final scoreText = score == null
        ? '평가 없음'
        : '${_formatScore(score!)}점 / $maxScore점';
    return Container(
      margin: const EdgeInsets.symmetric(horizontal: 16),
      padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 18),
      decoration: BoxDecoration(
        color: AppColors.surface,
        borderRadius: BorderRadius.circular(8),
      ),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          const Text(
            '지점평가',
            style: TextStyle(
              fontSize: 15,
              fontWeight: FontWeight.w700,
              color: AppColors.black,
            ),
          ),
          Text(
            scoreText,
            style: TextStyle(
              fontSize: 16,
              fontWeight: FontWeight.w800,
              color: score == null ? AppColors.textSecondary : AppColors.secondary,
            ),
          ),
        ],
      ),
    );
  }

  /// 정수면 정수로, 소수면 소수 1자리까지 표기.
  String _formatScore(double value) {
    if (value == value.roundToDouble()) return value.toInt().toString();
    return value.toStringAsFixed(1);
  }
}

class _SectionTitle extends StatelessWidget {
  final String text;
  const _SectionTitle(this.text);

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 16),
      child: Text(
        text,
        style: const TextStyle(
          fontSize: 15,
          fontWeight: FontWeight.w700,
          color: AppColors.black,
        ),
      ),
    );
  }
}

/// 거래처별 평가 테이블 (거래처 / 목표 / 실적 / 달성률)
class _AccountEvaluationTable extends StatelessWidget {
  final List<AccountEvaluation> accounts;
  final NumberFormat numberFormat;

  const _AccountEvaluationTable({
    required this.accounts,
    required this.numberFormat,
  });

  @override
  Widget build(BuildContext context) {
    if (accounts.isEmpty) {
      return const Padding(
        padding: EdgeInsets.symmetric(horizontal: 16, vertical: 32),
        child: Center(
          child: Text(
            '검색된 데이터가 없습니다.',
            style: TextStyle(fontSize: 14, color: AppColors.textSecondary),
          ),
        ),
      );
    }

    return Column(
      children: [
        _buildHeader(),
        ...accounts.map(_buildRow),
      ],
    );
  }

  Widget _buildHeader() {
    return Container(
      color: AppColors.surface,
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 10),
      child: const Row(
        children: [
          Expanded(flex: 4, child: _HeaderCell('거래처', TextAlign.left)),
          Expanded(flex: 3, child: _HeaderCell('목표', TextAlign.right)),
          Expanded(flex: 3, child: _HeaderCell('실적', TextAlign.right)),
          Expanded(flex: 2, child: _HeaderCell('달성률', TextAlign.right)),
        ],
      ),
    );
  }

  Widget _buildRow(AccountEvaluation account) {
    final subtitle = [
      if (account.accountType != null && account.accountType!.isNotEmpty)
        account.accountType!,
      if (account.accountCode.isNotEmpty) '(${account.accountCode})',
    ].join(' ');

    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
      decoration: const BoxDecoration(
        border: Border(bottom: BorderSide(color: AppColors.divider)),
      ),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.center,
        children: [
          Expanded(
            flex: 4,
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  account.accountName,
                  style: const TextStyle(
                    fontSize: 14,
                    fontWeight: FontWeight.w600,
                    color: AppColors.black,
                  ),
                ),
                if (subtitle.isNotEmpty) ...[
                  const SizedBox(height: 2),
                  Text(
                    subtitle,
                    style: const TextStyle(
                      fontSize: 11,
                      color: AppColors.textSecondary,
                    ),
                  ),
                ],
              ],
            ),
          ),
          Expanded(
            flex: 3,
            child: _amountCell(numberFormat.format(account.targetAmount)),
          ),
          Expanded(
            flex: 3,
            child: _amountCell(numberFormat.format(account.performanceAmount)),
          ),
          Expanded(
            flex: 2,
            child: _amountCell('${account.attainmentRate.round()}%'),
          ),
        ],
      ),
    );
  }

  Widget _amountCell(String text) {
    return Text(
      text,
      textAlign: TextAlign.right,
      style: const TextStyle(fontSize: 13, color: AppColors.black),
    );
  }
}

class _HeaderCell extends StatelessWidget {
  final String text;
  final TextAlign align;
  const _HeaderCell(this.text, this.align);

  @override
  Widget build(BuildContext context) {
    return Text(
      text,
      textAlign: align,
      style: const TextStyle(
        fontSize: 12,
        fontWeight: FontWeight.w700,
        color: AppColors.legacyTextMute,
      ),
    );
  }
}
