import 'package:flutter/material.dart';
import 'package:intl/intl.dart';

import '../../../core/theme/app_colors.dart';
import '../../../core/theme/app_spacing.dart';
import '../../../core/theme/app_typography.dart';
import '../../../domain/entities/client_order.dart';

/// 역참조 후속 주문(취소/변경 등) 요약 섹션
///
/// 상세 화면의 원본 주문번호를 `ref_sap_order_number` 로 역참조하는 후속 주문들을 표시합니다.
/// 후속 주문의 제품 라인은 원주문 "주문한 제품" 목록에 통합 dedup 되므로, 여기서는 제품표 없이
/// 헤더 요약(주문번호·유형·주문일·납기일·승인금액)만 노출합니다.
class RelatedClientOrderSection extends StatelessWidget {
  final List<RelatedClientOrder> relatedOrders;

  const RelatedClientOrderSection({
    super.key,
    required this.relatedOrders,
  });

  /// 주문유형 표시 라벨. 알려진 코드는 한글(+영문명)로, 미정의 코드는 영문명/코드로 폴백.
  /// SAP 원본은 영문명만 보유하므로 한글 라벨은 코드 매핑으로 파생한다.
  static const Map<String, String> _koreanTypeByCode = {
    'ZOR1': '주문',
    'ZRE1': '취소',
  };

  String _formatOrderType(String? code, String? name) {
    final korean = code != null ? _koreanTypeByCode[code] : null;
    final hasName = name != null && name.isNotEmpty;
    if (korean != null && hasName) return '$korean ($name)';
    if (korean != null) return korean;
    if (hasName) return name;
    return code ?? '-';
  }

  String _formatDate(DateTime? date) {
    if (date == null) return '-';
    const weekdays = ['월', '화', '수', '목', '금', '토', '일'];
    final weekday = weekdays[date.weekday - 1];
    return '${DateFormat('yyyy-MM-dd').format(date)} ($weekday)';
  }

  String _formatAmount(int? amount) {
    if (amount == null) return '-';
    return '${NumberFormat('#,###').format(amount)}원';
  }

  @override
  Widget build(BuildContext context) {
    if (relatedOrders.isEmpty) return const SizedBox.shrink();

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        // 섹션 구분선 + 타이틀
        Container(
          width: double.infinity,
          color: AppColors.surface,
          padding: const EdgeInsets.symmetric(
            horizontal: AppSpacing.lg,
            vertical: AppSpacing.md,
          ),
          child: Text(
            '관련 주문 (${relatedOrders.length})',
            style: AppTypography.headlineSmall,
          ),
        ),
        for (final order in relatedOrders) _buildRelatedOrder(order),
      ],
    );
  }

  Widget _buildRelatedOrder(RelatedClientOrder order) {
    return Container(
      padding: const EdgeInsets.all(AppSpacing.md),
      decoration: const BoxDecoration(
        border: Border(bottom: BorderSide(color: AppColors.border)),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          _infoRow('SAP 주문번호', order.sapOrderNumber),
          const SizedBox(height: AppSpacing.sm),
          _infoRow('주문유형',
              _formatOrderType(order.orderTypeCode, order.orderTypeName)),
          const SizedBox(height: AppSpacing.sm),
          _infoRow('주문일', _formatDate(order.orderDate)),
          const SizedBox(height: AppSpacing.sm),
          _infoRow('납기일', _formatDate(order.deliveryDate)),
          const SizedBox(height: AppSpacing.sm),
          _infoRow(
            '승인 금액',
            _formatAmount(order.totalApprovedAmount),
            valueColor: AppColors.secondaryDark,
            valueBold: true,
          ),
        ],
      ),
    );
  }

  Widget _infoRow(
    String label,
    String value, {
    Color? valueColor,
    bool valueBold = false,
  }) {
    return Row(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        SizedBox(
          width: 90,
          child: Text(
            label,
            style: AppTypography.bodyMedium.copyWith(
              fontWeight: FontWeight.bold,
            ),
          ),
        ),
        Expanded(
          child: Text(
            value,
            style: AppTypography.bodyMedium.copyWith(
              color: valueColor,
              fontWeight: valueBold ? FontWeight.bold : null,
            ),
          ),
        ),
      ],
    );
  }
}
