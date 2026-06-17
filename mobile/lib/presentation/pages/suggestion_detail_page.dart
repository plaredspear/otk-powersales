import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:intl/intl.dart';

import '../../core/theme/app_colors.dart';
import '../../core/theme/app_spacing.dart';
import '../../core/theme/app_typography.dart';
import '../../core/utils/error_utils.dart';
import '../../domain/entities/suggestion_detail.dart';
import '../providers/suggestion_list_provider.dart';
import '../widgets/common/error_view.dart';
import '../widgets/common/loading_indicator.dart';

/// 제안/물류클레임 상세 화면
///
/// 레거시 `fieldTalk/suggest/logisticsclaimview.jsp` 대응.
/// 물류클레임은 OLS 조치사항(조치상태/물류책임/책임물류센터/중복접수)을 추가 표시한다.
class SuggestionDetailPage extends ConsumerWidget {
  final int suggestionId;

  const SuggestionDetailPage({super.key, required this.suggestionId});

  String _formatDate(DateTime date) =>
      DateFormat('yyyy.MM.dd(E)', 'ko_KR').format(date);

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final detailAsync = ref.watch(suggestionDetailProvider(suggestionId));

    return Scaffold(
      backgroundColor: AppColors.background,
      appBar: AppBar(
        title: const Text('제안 / 물류클레임 상세'),
        backgroundColor: AppColors.white,
        foregroundColor: AppColors.textPrimary,
        elevation: 0,
      ),
      body: detailAsync.when(
        loading: () => const LoadingIndicator(message: '상세 정보를 불러오는 중...'),
        error: (error, _) => ErrorView(
          message: '상세 정보를 불러올 수 없습니다',
          description: extractErrorMessage(error),
          onRetry: () =>
              ref.invalidate(suggestionDetailProvider(suggestionId)),
        ),
        data: (detail) => _buildContent(detail),
      ),
    );
  }

  Widget _buildContent(SuggestionDetail detail) {
    return SingleChildScrollView(
      padding: const EdgeInsets.all(AppSpacing.lg),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          // 분류 + 상태
          Row(
            children: [
              Container(
                padding: const EdgeInsets.symmetric(
                  horizontal: AppSpacing.sm,
                  vertical: AppSpacing.xxs,
                ),
                decoration: BoxDecoration(
                  color: detail.isLogisticsClaim
                      ? AppColors.secondaryLight
                      : AppColors.surface,
                  borderRadius: BorderRadius.circular(AppSpacing.radiusSm),
                ),
                child: Text(
                  detail.categoryName,
                  style: AppTypography.labelSmall.copyWith(
                    color: detail.isLogisticsClaim
                        ? AppColors.white
                        : AppColors.textSecondary,
                    fontWeight: FontWeight.w700,
                  ),
                ),
              ),
              const Spacer(),
              Text(
                detail.statusLabel,
                style: AppTypography.labelMedium.copyWith(
                  color: AppColors.textSecondary,
                ),
              ),
            ],
          ),
          const SizedBox(height: AppSpacing.md),

          // 제목
          Text(
            detail.title,
            style: AppTypography.headlineSmall.copyWith(
              color: AppColors.textPrimary,
              fontWeight: FontWeight.bold,
            ),
          ),
          const SizedBox(height: AppSpacing.xs),
          Text(
            '${detail.proposalNumber} · ${_formatDate(detail.createdAt)}',
            style: AppTypography.bodySmall.copyWith(
              color: AppColors.textTertiary,
            ),
          ),

          const Divider(height: AppSpacing.xl, color: AppColors.border),

          // 기본 정보
          if (detail.sapAccountCode != null)
            _InfoRow(label: '거래처', value: detail.sapAccountCode!),
          if (detail.productCode != null)
            _InfoRow(label: '제품', value: detail.productCode!),
          if (detail.isLogisticsClaim) ...[
            if (detail.claimType != null)
              _InfoRow(label: '클레임 항목', value: detail.claimType!),
            if (detail.claimDate != null)
              _InfoRow(label: '발생일자', value: _formatDate(detail.claimDate!)),
            if (detail.carNumber != null)
              _InfoRow(label: '차량번호', value: detail.carNumber!),
          ],

          const SizedBox(height: AppSpacing.md),
          Text(
            '내용',
            style: AppTypography.labelMedium.copyWith(
              color: AppColors.textSecondary,
              fontWeight: FontWeight.w700,
            ),
          ),
          const SizedBox(height: AppSpacing.xs),
          Text(
            detail.content,
            style: AppTypography.bodyMedium.copyWith(
              color: AppColors.textPrimary,
              height: 1.6,
            ),
          ),

          // OLS 조치사항 (물류클레임 조치 결과)
          if (detail.hasActionInfo) ...[
            const SizedBox(height: AppSpacing.xl),
            _ActionSection(detail: detail),
          ],

          // 오뚜기 접수사원 (조장만 노출 — 레거시 logisticsclaimview 권한분기)
          if (detail.hasReceptionEmployee) ...[
            const SizedBox(height: AppSpacing.xl),
            _ReceptionEmployeeSection(detail: detail),
          ],

          // 첨부 사진
          if (detail.hasAttachments) ...[
            const SizedBox(height: AppSpacing.xl),
            Text(
              '첨부 사진',
              style: AppTypography.labelMedium.copyWith(
                color: AppColors.textSecondary,
                fontWeight: FontWeight.w700,
              ),
            ),
            const SizedBox(height: AppSpacing.sm),
            ...detail.attachments.map((a) => Padding(
                  padding: const EdgeInsets.only(bottom: AppSpacing.md),
                  child: ClipRRect(
                    borderRadius: BorderRadius.circular(AppSpacing.radiusMd),
                    child: Image.network(
                      a.s3Url,
                      fit: BoxFit.cover,
                      errorBuilder: (context, error, stackTrace) => Container(
                        height: 180,
                        color: AppColors.surface,
                        child: const Center(
                          child: Icon(
                            Icons.broken_image,
                            size: 40,
                            color: AppColors.textTertiary,
                          ),
                        ),
                      ),
                    ),
                  ),
                )),
          ],
        ],
      ),
    );
  }
}

/// OLS 조치사항 섹션 (물류클레임 처리 결과)
class _ActionSection extends StatelessWidget {
  final SuggestionDetail detail;

  const _ActionSection({required this.detail});

  @override
  Widget build(BuildContext context) {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(AppSpacing.md),
      decoration: BoxDecoration(
        color: AppColors.secondaryLight.withValues(alpha: 0.08),
        borderRadius: BorderRadius.circular(AppSpacing.radiusMd),
        border: Border.all(color: AppColors.secondaryLight, width: 1),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            'OLS 조치사항',
            style: AppTypography.labelMedium.copyWith(
              color: AppColors.secondary,
              fontWeight: FontWeight.bold,
            ),
          ),
          const SizedBox(height: AppSpacing.sm),
          if (detail.actionNum != null && detail.actionNum!.isNotEmpty)
            _InfoRow(label: '조치번호', value: detail.actionNum!),
          if (detail.actionStatusLabel != null)
            _InfoRow(label: '조치상태', value: detail.actionStatusLabel!),
          if (detail.actionManager != null && detail.actionManager!.isNotEmpty)
            _InfoRow(label: '조치 담당자', value: detail.actionManager!),
          if (detail.logisticsResponsibility != null)
            _InfoRow(label: '물류책임', value: detail.logisticsResponsibility!),
          if (detail.claimTypeMeasures != null)
            _InfoRow(label: '클레임항목', value: detail.claimTypeMeasures!),
          if (detail.responsibleLogisticsCenter != null)
            _InfoRow(label: '책임물류센터', value: detail.responsibleLogisticsCenter!),
          if (detail.receptionLogisticsCenter != null)
            _InfoRow(label: '접수물류센터', value: detail.receptionLogisticsCenter!),
          if (detail.duplicateProposalNum != null)
            _InfoRow(label: '중복 제안번호', value: detail.duplicateProposalNum!),
          if (detail.actionContent != null && detail.actionContent!.isNotEmpty)
            _InfoRow(label: '조치내용', value: detail.actionContent!),
        ],
      ),
    );
  }
}

/// '오뚜기 접수사원' 섹션 — 물류클레임 상세에서 조장에게만 노출 (레거시 logisticsclaimview 권한분기).
/// 백엔드가 조장 권한일 때만 receptionEmployeeName/Code 를 채워 내려준다.
class _ReceptionEmployeeSection extends StatelessWidget {
  final SuggestionDetail detail;

  const _ReceptionEmployeeSection({required this.detail});

  @override
  Widget build(BuildContext context) {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(AppSpacing.md),
      decoration: BoxDecoration(
        color: AppColors.secondaryLight.withValues(alpha: 0.08),
        borderRadius: BorderRadius.circular(AppSpacing.radiusMd),
        border: Border.all(color: AppColors.secondaryLight, width: 1),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            '오뚜기 접수사원',
            style: AppTypography.labelMedium.copyWith(
              color: AppColors.secondary,
              fontWeight: FontWeight.bold,
            ),
          ),
          const SizedBox(height: AppSpacing.sm),
          _InfoRow(label: '접수사원', value: detail.receptionEmployeeName!),
          if (detail.receptionEmployeeCode != null &&
              detail.receptionEmployeeCode!.isNotEmpty)
            _InfoRow(label: '사번', value: detail.receptionEmployeeCode!),
        ],
      ),
    );
  }
}

class _InfoRow extends StatelessWidget {
  final String label;
  final String value;

  const _InfoRow({required this.label, required this.value});

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: AppSpacing.xs),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          SizedBox(
            width: 88,
            child: Text(
              label,
              style: AppTypography.bodySmall.copyWith(
                color: AppColors.textSecondary,
              ),
            ),
          ),
          Expanded(
            child: Text(
              value,
              style: AppTypography.bodyMedium.copyWith(
                color: AppColors.textPrimary,
              ),
            ),
          ),
        ],
      ),
    );
  }
}
