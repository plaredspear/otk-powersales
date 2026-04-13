import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:intl/intl.dart';
import '../../core/theme/app_colors.dart';
import '../../core/theme/app_spacing.dart';
import '../../core/theme/app_typography.dart';
import '../../domain/entities/claim_detail.dart';
import '../../domain/entities/claim_photo.dart';
import '../providers/claim_detail_provider.dart';
import '../widgets/claim/claim_status_badge.dart';
import '../widgets/common/loading_indicator.dart';

/// 클레임 상세 페이지
class ClaimDetailPage extends ConsumerStatefulWidget {
  final int claimId;

  const ClaimDetailPage({super.key, required this.claimId});

  @override
  ConsumerState<ClaimDetailPage> createState() => _ClaimDetailPageState();
}

class _ClaimDetailPageState extends ConsumerState<ClaimDetailPage> {
  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      ref.read(claimDetailProvider.notifier).loadDetail(widget.claimId);
    });
  }

  @override
  Widget build(BuildContext context) {
    final state = ref.watch(claimDetailProvider);

    ref.listen<String?>(
      claimDetailProvider.select((s) => s.errorMessage),
      (prev, next) {
        if (next != null) {
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(content: Text(next)),
          );
          ref.read(claimDetailProvider.notifier).clearError();
          Navigator.of(context).pop();
        }
      },
    );

    return Scaffold(
      appBar: AppBar(title: const Text('클레임 상세')),
      body: _buildBody(state),
    );
  }

  Widget _buildBody(dynamic state) {
    if (state.isLoading || state.detail == null) {
      return const LoadingIndicator(message: '클레임 정보를 불러오는 중...');
    }

    final detail = state.detail as ClaimDetail;
    return SingleChildScrollView(
      child: Padding(
        padding: const EdgeInsets.all(AppSpacing.lg),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            _buildStatusSection(detail),
            const SizedBox(height: AppSpacing.lg),
            _buildProductSection(detail),
            const SizedBox(height: AppSpacing.lg),
            _buildDefectSection(detail),
            const SizedBox(height: AppSpacing.lg),
            _buildPhotosSection(detail),
            if (detail.purchaseAmount != null) ...[
              const SizedBox(height: AppSpacing.lg),
              _buildPurchaseSection(detail),
            ],
            if (detail.requestTypeName != null) ...[
              const SizedBox(height: AppSpacing.lg),
              _buildInfoRow('요청사항', detail.requestTypeName!),
            ],
            const SizedBox(height: AppSpacing.lg),
            _buildInfoRow(
              '등록일시',
              DateFormat('yyyy-MM-dd HH:mm').format(detail.createdAt),
            ),
            const SizedBox(height: AppSpacing.lg),
          ],
        ),
      ),
    );
  }

  Widget _buildStatusSection(ClaimDetail detail) {
    return Align(
      alignment: Alignment.centerRight,
      child: ClaimStatusBadge(
        status: detail.status,
        statusLabel: detail.statusLabel,
      ),
    );
  }

  Widget _buildProductSection(ClaimDetail detail) {
    return Container(
      padding: AppSpacing.cardPadding,
      decoration: BoxDecoration(
        color: AppColors.card,
        borderRadius: AppSpacing.cardBorderRadius,
        border: Border.all(color: AppColors.border),
      ),
      child: Column(
        children: [
          _buildInfoRow('거래처', detail.accountName ?? '-'),
          const SizedBox(height: AppSpacing.sm),
          _buildInfoRow('제품', detail.productName ?? '-'),
          const SizedBox(height: AppSpacing.sm),
          _buildInfoRow('제품코드', detail.productCode ?? '-'),
          if (detail.dateTypeLabel != null && detail.date != null) ...[
            const SizedBox(height: AppSpacing.sm),
            _buildInfoRow(
              detail.dateTypeLabel!,
              DateFormat('yyyy-MM-dd').format(detail.date!),
            ),
          ],
        ],
      ),
    );
  }

  Widget _buildDefectSection(ClaimDetail detail) {
    final category = [detail.categoryName, detail.subcategoryName]
        .where((e) => e != null)
        .join(' > ');
    return Container(
      padding: AppSpacing.cardPadding,
      decoration: BoxDecoration(
        color: AppColors.card,
        borderRadius: AppSpacing.cardBorderRadius,
        border: Border.all(color: AppColors.border),
      ),
      child: Column(
        children: [
          _buildInfoRow('클레임 종류', category.isNotEmpty ? category : '-'),
          if (detail.defectDescription != null) ...[
            const SizedBox(height: AppSpacing.sm),
            _buildInfoRow('불량 내역', detail.defectDescription!),
          ],
          if (detail.defectQuantity != null) ...[
            const SizedBox(height: AppSpacing.sm),
            _buildInfoRow('불량 수량', '${detail.defectQuantity}개'),
          ],
        ],
      ),
    );
  }

  Widget _buildPhotosSection(ClaimDetail detail) {
    final defectPhotos =
        detail.photos.where((p) => p.photoType == 'DEFECT').toList();
    final labelPhotos =
        detail.photos.where((p) => p.photoType == 'LABEL').toList();

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        if (defectPhotos.isNotEmpty) _buildPhotoGroup('불량 사진', defectPhotos),
        if (labelPhotos.isNotEmpty) ...[
          const SizedBox(height: AppSpacing.md),
          _buildPhotoGroup('일부인 사진', labelPhotos),
        ],
      ],
    );
  }

  Widget _buildPurchaseSection(ClaimDetail detail) {
    final amount = NumberFormat('#,###').format(detail.purchaseAmount);
    return Container(
      padding: AppSpacing.cardPadding,
      decoration: BoxDecoration(
        color: AppColors.card,
        borderRadius: AppSpacing.cardBorderRadius,
        border: Border.all(color: AppColors.border),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            '구매 정보',
            style: AppTypography.bodySmall.copyWith(
              fontWeight: FontWeight.w600,
            ),
          ),
          const SizedBox(height: AppSpacing.sm),
          _buildInfoRow('구매 금액', '$amount원'),
          if (detail.purchaseMethodName != null) ...[
            const SizedBox(height: AppSpacing.sm),
            _buildInfoRow('구매 방법', detail.purchaseMethodName!),
          ],
          ..._buildReceiptPhotos(detail),
        ],
      ),
    );
  }

  List<Widget> _buildReceiptPhotos(ClaimDetail detail) {
    final receiptPhotos =
        detail.photos.where((p) => p.photoType == 'RECEIPT').toList();
    if (receiptPhotos.isEmpty) return [];
    return [
      const SizedBox(height: AppSpacing.md),
      _buildPhotoGroup('영수증 사진', receiptPhotos),
    ];
  }

  Widget _buildPhotoGroup(String label, List<ClaimPhoto> photos) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          label,
          style: AppTypography.bodySmall.copyWith(
            fontWeight: FontWeight.w600,
            color: AppColors.textSecondary,
          ),
        ),
        const SizedBox(height: AppSpacing.xs),
        ...photos.map((photo) => _buildPhotoTile(photo)),
      ],
    );
  }

  Widget _buildPhotoTile(ClaimPhoto photo) {
    return Padding(
      padding: const EdgeInsets.only(bottom: AppSpacing.sm),
      child: GestureDetector(
        onTap: () => _showFullScreenImage(photo),
        child: ClipRRect(
          borderRadius: BorderRadius.circular(8),
          child: Image.network(
            photo.url,
            width: double.infinity,
            height: 200,
            fit: BoxFit.cover,
            errorBuilder: (context, error, stackTrace) => Container(
              width: double.infinity,
              height: 200,
              color: AppColors.border,
              child: const Icon(
                Icons.broken_image,
                size: 48,
                color: AppColors.textSecondary,
              ),
            ),
          ),
        ),
      ),
    );
  }

  void _showFullScreenImage(ClaimPhoto photo) {
    showDialog(
      context: context,
      builder: (context) => Dialog(
        insetPadding: EdgeInsets.zero,
        backgroundColor: Colors.black,
        child: Stack(
          children: [
            Center(
              child: InteractiveViewer(
                child: Image.network(
                  photo.url,
                  fit: BoxFit.contain,
                  errorBuilder: (context, error, stackTrace) =>
                      const Icon(Icons.broken_image,
                          size: 48, color: Colors.white),
                ),
              ),
            ),
            Positioned(
              top: 40,
              right: 16,
              child: IconButton(
                icon: const Icon(Icons.close, color: Colors.white, size: 28),
                onPressed: () => Navigator.of(context).pop(),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildInfoRow(String label, String value) {
    return Row(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        SizedBox(
          width: 90,
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
            style: AppTypography.bodySmall,
          ),
        ),
      ],
    );
  }
}
