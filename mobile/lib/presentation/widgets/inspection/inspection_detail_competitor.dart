import 'package:flutter/material.dart';
import 'package:intl/intl.dart';

import '../../../domain/entities/inspection_detail.dart';
import 'inspection_detail_row.dart';

/// 경쟁사 현장점검 상세 정보 위젯
///
/// 레거시 view.jsp 정합 — 별도 섹션 헤더/카드 없이 경쟁사 관련 행만 공통 정보에 이어 표시.
class InspectionDetailCompetitorWidget extends StatelessWidget {
  final InspectionDetail detail;

  const InspectionDetailCompetitorWidget({
    super.key,
    required this.detail,
  });

  @override
  Widget build(BuildContext context) {
    final numberFormat = NumberFormat('#,###');

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        // 경쟁사
        if (detail.competitorName != null && detail.competitorName!.isNotEmpty)
          InspectionDetailRow(label: '경쟁사', value: detail.competitorName!),

        // 경쟁사 활동 내용
        if (detail.competitorActivity != null &&
            detail.competitorActivity!.isNotEmpty)
          InspectionDetailRow(
            label: '경쟁사 활동 내용',
            value: detail.competitorActivity!,
          ),

        // 경쟁사 상품 시식 여부
        if (detail.competitorTasting != null)
          InspectionDetailRow(
            label: '경쟁사 상품 시식 여부',
            value: detail.competitorTasting! ? '예' : '아니요',
          ),

        // 시식 정보 (시식=예인 경우만)
        if (detail.competitorTasting == true) ...[
          if (detail.competitorProductName != null &&
              detail.competitorProductName!.isNotEmpty)
            InspectionDetailRow(
              label: '경쟁사 상품',
              value: detail.competitorProductName!,
            ),
          if (detail.competitorProductPrice != null)
            InspectionDetailRow(
              label: '제품 가격',
              value: '${numberFormat.format(detail.competitorProductPrice)}원',
            ),
          if (detail.competitorSalesQuantity != null)
            InspectionDetailRow(
              label: '판매 수량',
              value: '${numberFormat.format(detail.competitorSalesQuantity)}개',
            ),
        ],
      ],
    );
  }
}
