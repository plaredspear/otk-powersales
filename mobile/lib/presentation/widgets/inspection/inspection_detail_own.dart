import 'package:flutter/material.dart';

import '../../../domain/entities/inspection_detail.dart';
import 'inspection_detail_row.dart';

/// 자사 현장점검 상세 정보 위젯
///
/// 레거시 view.jsp 정합 — 별도 섹션 헤더/카드 없이 제품·설명 행만 공통 정보에 이어 표시.
class InspectionDetailOwnWidget extends StatelessWidget {
  final InspectionDetail detail;

  const InspectionDetailOwnWidget({
    super.key,
    required this.detail,
  });

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        // 제품 (레거시: ProductName 만 표시)
        if (detail.productName != null || detail.productCode != null)
          InspectionDetailRow(
            label: '제품',
            value: detail.productName ?? detail.productCode ?? '-',
          ),

        // 설명
        if (detail.description != null && detail.description!.isNotEmpty)
          InspectionDetailRow(label: '설명', value: detail.description!),
      ],
    );
  }
}
