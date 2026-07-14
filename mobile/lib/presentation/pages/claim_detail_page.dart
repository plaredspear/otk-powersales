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
            ..._withGaps([
              _buildProductSection(detail),
              _buildClaimSection(detail),
              _buildComplaintSection(detail),
              _buildPhotosCard(detail),
              _buildChannelSection(detail),
              _buildActionSection(detail),
              _buildMetaSection(detail),
            ]),
            const SizedBox(height: AppSpacing.lg),
          ],
        ),
      ),
    );
  }

  /// null 섹션은 건너뛰고, 보이는 섹션 사이에만 간격을 둔다.
  List<Widget> _withGaps(List<Widget?> sections) {
    final visible = sections.whereType<Widget>().toList();
    final out = <Widget>[];
    for (var i = 0; i < visible.length; i++) {
      if (i > 0) out.add(const SizedBox(height: AppSpacing.lg));
      out.add(visible[i]);
    }
    return out;
  }

  String _fmtDate(DateTime d) => DateFormat('yyyy-MM-dd').format(d);

  String _fmtDateTime(DateTime d) => DateFormat('yyyy-MM-dd HH:mm').format(d);

  /// 값이 없으면 행을 생성하지 않는다(누락 항목은 표시 생략).
  Widget? _row(String label, String? value) {
    if (value == null || value.trim().isEmpty) return null;
    return _buildInfoRow(label, value);
  }

  /// 표시할 행이 하나도 없으면 카드 자체를 생성하지 않는다.
  Widget? _buildCard(String title, List<Widget?> rows) {
    final visible = rows.whereType<Widget>().toList();
    if (visible.isEmpty) return null;
    final children = <Widget>[
      Text(
        title,
        style: AppTypography.bodySmall.copyWith(fontWeight: FontWeight.w700),
      ),
      const SizedBox(height: AppSpacing.sm),
    ];
    for (var i = 0; i < visible.length; i++) {
      if (i > 0) children.add(const SizedBox(height: AppSpacing.sm));
      children.add(visible[i]);
    }
    return Container(
      width: double.infinity,
      padding: AppSpacing.cardPadding,
      decoration: BoxDecoration(
        color: AppColors.card,
        borderRadius: AppSpacing.cardBorderRadius,
        border: Border.all(color: AppColors.border),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: children,
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

  /// 제품정보 (레거시 view.jsp '제품정보' 섹션)
  Widget? _buildProductSection(ClaimDetail d) => _buildCard('제품정보', [
        _row('제품', d.productName ?? '-'),
        _row('제품코드', d.productCode ?? '-'),
        _row('제조일자',
            d.manufacturingDate != null ? _fmtDate(d.manufacturingDate!) : null),
        _row('출고처', d.logisticsCenter),
        _row('소비기한',
            d.expirationDate != null ? _fmtDate(d.expirationDate!) : null),
        _row('주문번호', d.orderNumber),
      ]);

  /// 클레임정보 (레거시 view.jsp '클레임정보' 섹션)
  Widget? _buildClaimSection(ClaimDetail d) {
    final category = [d.categoryLabel, d.subcategoryLabel]
        .where((e) => e != null && e.isNotEmpty)
        .join(' > ');
    return _buildCard('클레임정보', [
      _row('접수번호', d.claimNo),
      // 코스모스(고객상담 처리 시스템) 전송상태. 상단 뱃지와 동일 값이나 무엇의 상태인지 명확히 하기 위해 행으로도 표기.
      _row('코스모스전송상태', d.statusLabel),
      // 신규→알라딘 전송상태. 알라딘 이관(마이그레이션) 건은 null 이라 행 자체가 생략된다.
      _row('알라딘전송상태', d.sfSendStatusLabel),
      _row('거래처', d.accountName ?? '-'),
      _row('거래처코드', d.accountCode),
      _row('클레임 종류', category.isNotEmpty ? category : '-'),
      _row('불량 수량', d.defectQuantity != null ? '${d.defectQuantity}개' : null),
      _row(
        '샘플 회수여부',
        d.sampleCollectionFlag != null
            ? (d.sampleCollectionFlag! ? '회수' : '미회수')
            : null,
      ),
      _row('거래처납품일자',
          d.customerDeliveryDate != null ? _fmtDate(d.customerDeliveryDate!) : null),
      _row('세부점포명', d.detailSnsName),
      _row('발생일자', d.date != null ? _fmtDate(d.date!) : null),
      _row('구매방법', d.purchaseMethodName),
      _row(
        '구매금액',
        d.purchaseAmount != null
            ? '${NumberFormat('#,###').format(d.purchaseAmount)}원'
            : null,
      ),
      _row('요청사항', d.requestTypeName?.replaceAll(';', ', ')),
      _row('부서', d.division),
    ]);
  }

  /// 불만정보 (레거시 view.jsp '불만정보' 섹션)
  Widget? _buildComplaintSection(ClaimDetail d) => _buildCard('불만정보', [
        _row('불만내역', d.defectDescription),
      ]);

  /// 채널정보 (레거시 view.jsp '채널정보' 섹션)
  Widget? _buildChannelSection(ClaimDetail d) => _buildCard('채널정보', [
        _row('전송일자',
            d.interfaceDate != null ? _fmtDateTime(d.interfaceDate!) : null),
        _row('접수채널', d.channelLabel ?? d.channel),
        _row('작성자', d.employeeName),
        _row('직위', d.jikwee),
        _row('영업사원 연락처', d.employeePhone),
      ]);

  /// 처리·조치정보 (레거시 view.jsp '처리/조치정보' 섹션)
  Widget? _buildActionSection(ClaimDetail d) => _buildCard('처리·조치정보', [
        _row('상담번호', d.counselNumber),
        _row('조치코드', d.actionCode),
        _row('조치상태', d.actionStatus),
        _row('원인별 분류', d.reasonType),
        _row('조치내용', d.actContent),
      ]);

  /// 등록정보 (등록일시 — 레거시엔 별도 라벨 없으나 모바일은 명시)
  Widget? _buildMetaSection(ClaimDetail d) => _buildCard('등록정보', [
        _buildInfoRow('등록일시', _fmtDateTime(d.createdAt)),
      ]);

  /// 첨부 사진 (불량/일부인/영수증 분류, 미분류 사진도 누락 없이 노출)
  Widget? _buildPhotosCard(ClaimDetail d) {
    const known = ['DEFECT', 'LABEL', 'RECEIPT'];
    final defect = d.photos.where((p) => p.photoType == 'DEFECT').toList();
    final label = d.photos.where((p) => p.photoType == 'LABEL').toList();
    final receipt = d.photos.where((p) => p.photoType == 'RECEIPT').toList();
    final etc =
        d.photos.where((p) => !known.contains(p.photoType)).toList();

    final groups = <Widget>[];
    void add(String title, List<ClaimPhoto> list) {
      if (list.isEmpty) return;
      if (groups.isNotEmpty) groups.add(const SizedBox(height: AppSpacing.md));
      groups.add(_buildPhotoGroup(title, list));
    }

    add('불량 사진', defect);
    add('일부인 사진', label);
    add('영수증 사진', receipt);
    add('기타 사진', etc);
    if (groups.isEmpty) return null;

    return Container(
      width: double.infinity,
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
            '첨부 사진',
            style:
                AppTypography.bodySmall.copyWith(fontWeight: FontWeight.w700),
          ),
          const SizedBox(height: AppSpacing.sm),
          ...groups,
        ],
      ),
    );
  }

  Widget _buildPhotoGroup(String label, List<ClaimPhoto> photos) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          label,
          style: AppTypography.bodySmall.copyWith(
            fontWeight: FontWeight.w700,
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
          width: 100,
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
