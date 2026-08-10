import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import '../common/synced_text_field.dart';
import '../../../core/theme/app_colors.dart';
import '../../../core/theme/app_spacing.dart';
import '../../../core/theme/app_typography.dart';
import '../../../domain/entities/order_draft.dart';
import '../../../domain/entities/validation_error.dart';

/// 주문서 제품 카드
class OrderProductCard extends StatelessWidget {
  /// 라인 순번 (레거시 "N. (코드) 제품명" 표기용, 0-based).
  final int index;
  final OrderDraftItem item;
  final ValidationError? validationError;
  final ValueChanged<bool?> onSelectionChanged;
  final Function(double boxes, int pieces) onQuantityChanged;

  /// 승인요청 버튼에서 "수량 미입력" 을 눌러 이 카드로 이동해 온 직후 여부.
  /// 목록이 길어 어느 줄이 문제인지 찾기 어려우므로, 도착 지점을 잠시 강조한다.
  final bool highlighted;

  const OrderProductCard({
    super.key,
    required this.index,
    required this.item,
    required this.validationError,
    required this.onSelectionChanged,
    required this.onQuantityChanged,
    this.highlighted = false,
  });

  /// 에러 상세 지표 — 레거시 "최소주문단위 40개 | 공급 0개 | DC 0개" 정합.
  List<String> get _errorMetrics {
    final error = validationError;
    if (error == null) return const [];
    return [
      if (error.minOrderQuantity != null) '최소주문단위 ${error.minOrderQuantity}개',
      if (error.supplyQuantity != null) '공급 ${error.supplyQuantity}개',
      if (error.dcQuantity != null) 'DC ${error.dcQuantity}개',
    ];
  }

  String _formatNumber(int value) {
    return value.toString().replaceAllMapped(
      RegExp(r'(\d)(?=(\d{3})+(?!\d))'),
      (m) => '${m[1]},',
    );
  }

  @override
  Widget build(BuildContext context) {
    final hasError = validationError != null;
    // 레거시 write.jsp: 총 EA = 박스 × 1박스당 EA + 낱개.
    final totalEach =
        (item.quantityBoxes * item.boxSize).round() + item.quantityPieces;
    // 에러가 표시된 줄에서 수량이 **에러 시점 수량과 달라지면** 사유(메시지)는 유지한 채 사유 블록
    // 배경만 분홍(errorLight) → 연한 파랑(infoLight) 으로 바꿔 "고친 줄 / 아직 안 고친 줄" 을 구분한다
    // (2026-07-25 사용자 결정 — 테두리색 변경은 인지가 어려워 배경으로 변경. 테두리는 빨강 유지).
    // 수정 이벤트가 아니라 값 비교이므로, 원래 수량으로 되돌리면 다시 분홍으로 돌아온다.
    final validatedQuantity = validationError?.requestedQuantity;
    final isQuantityChanged =
        validatedQuantity != null && totalEach != validatedQuantity;

    return Card(
      margin: const EdgeInsets.only(bottom: AppSpacing.md),
      // 강조 중에는 테두리를 굵은 주황으로 — 에러(빨강)와 구분되는 "여기를 보라" 신호.
      // 값 자체는 유효/무효가 아니라 미입력이므로 에러색을 쓰지 않는다.
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(AppSpacing.radiusMd),
        side: BorderSide(
          color: highlighted
              ? AppColors.warning
              : (hasError ? AppColors.error : AppColors.border),
          width: highlighted ? 2 : 1,
        ),
      ),
      color: highlighted ? AppColors.warningLight : null,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Padding(
            padding: const EdgeInsets.all(AppSpacing.md),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                // 레거시: "N. (제품코드) 제품명" + 우측 체크박스 (라인별 X 버튼 없음).
                Row(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Expanded(
                      child: Padding(
                        padding: const EdgeInsets.only(top: AppSpacing.sm),
                        child: Text(
                          '${index + 1}. (${item.productCode}) ${item.productName}',
                          // 레거시: 위반 행은 제목까지 붉게 표시해 어느 제품인지 바로 보이게 한다.
                          style: hasError
                              ? AppTypography.headlineSmall.copyWith(
                                  color: AppColors.error,
                                )
                              : AppTypography.headlineSmall,
                        ),
                      ),
                    ),
                    const SizedBox(width: AppSpacing.sm),
                    Checkbox(
                      value: item.isSelected,
                      onChanged: onSelectionChanged,
                    ),
                  ],
                ),
                const SizedBox(height: AppSpacing.sm),
                // 레거시: [박스] [낱개(개)] 입력 + 총 EA 표시.
                Row(
                  children: [
                    Expanded(
                      child: SyncedTextField(
                        // 레거시 write.jsp: 박스 입력칸(.unitQty)은 parseInt 로
                        // 정수만 유효. 표시/입력 모두 정수 단위로 맞춘다.
                        value: item.quantityBoxes > 0
                            ? item.quantityBoxes.toInt().toString()
                            : '',
                        keyboardType: TextInputType.number,
                        inputFormatters: [
                          FilteringTextInputFormatter.digitsOnly,
                        ],
                        decoration: InputDecoration(
                          isDense: true,
                          hintText: '0',
                          suffixText: '박스',
                          contentPadding: const EdgeInsets.symmetric(
                            horizontal: AppSpacing.sm,
                            vertical: AppSpacing.sm,
                          ),
                          border: OutlineInputBorder(
                            borderRadius: BorderRadius.circular(
                              AppSpacing.radiusSm,
                            ),
                          ),
                        ),
                        onChanged: (value) {
                          final boxes = (int.tryParse(value) ?? 0).toDouble();
                          onQuantityChanged(boxes, item.quantityPieces);
                        },
                      ),
                    ),
                    const SizedBox(width: AppSpacing.sm),
                    Expanded(
                      child: SyncedTextField(
                        value: item.quantityPieces > 0
                            ? item.quantityPieces.toString()
                            : '',
                        keyboardType: TextInputType.number,
                        inputFormatters: [
                          FilteringTextInputFormatter.digitsOnly,
                        ],
                        decoration: InputDecoration(
                          isDense: true,
                          hintText: '0',
                          suffixText: '개',
                          contentPadding: const EdgeInsets.symmetric(
                            horizontal: AppSpacing.sm,
                            vertical: AppSpacing.sm,
                          ),
                          border: OutlineInputBorder(
                            borderRadius: BorderRadius.circular(
                              AppSpacing.radiusSm,
                            ),
                          ),
                        ),
                        onChanged: (value) {
                          final pieces = int.tryParse(value) ?? 0;
                          onQuantityChanged(item.quantityBoxes, pieces);
                        },
                      ),
                    ),
                    const SizedBox(width: AppSpacing.md),
                    Text(
                      '총 ${_formatNumber(totalEach)}개',
                      style: AppTypography.bodyMedium.copyWith(
                        fontWeight: FontWeight.bold,
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: AppSpacing.sm),
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    Text(
                      '1박스 = ${item.boxSize}개',
                      style: AppTypography.bodySmall.copyWith(
                        color: AppColors.textSecondary,
                      ),
                    ),
                    Text(
                      '소계: ${_formatNumber(item.totalPrice)}원',
                      style: AppTypography.labelLarge,
                    ),
                  ],
                ),
              ],
            ),
          ),
          if (hasError)
            Container(
              width: double.infinity,
              padding: AppSpacing.cardPadding,
              decoration: BoxDecoration(
                // 수량을 고친 줄은 연한 주황 — 아직 안 고친 분홍 줄과 구분되면서도 "해결됨" 으로는
                // 읽히지 않는다(초록·파랑 회피). 실제 해소 여부는 승인요청 재검증으로만 확정된다.
                color: isQuantityChanged
                    ? AppColors.warningLight
                    : AppColors.errorLight,
                borderRadius: const BorderRadius.only(
                  bottomLeft: Radius.circular(AppSpacing.radiusMd),
                  bottomRight: Radius.circular(AppSpacing.radiusMd),
                ),
              ),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  // 레거시 write.jsp 문구/배치: "최소주문단위 N개 | 공급 N개 | DC N개" 한 줄.
                  if (_errorMetrics.isNotEmpty)
                    Text(
                      _errorMetrics.join('  |  '),
                      style: AppTypography.bodySmall.copyWith(
                        color: AppColors.error,
                      ),
                    ),
                  if (_errorMetrics.isNotEmpty)
                    const SizedBox(height: AppSpacing.xs),
                  Text(
                    validationError!.message,
                    style: AppTypography.bodySmall.copyWith(
                      color: AppColors.error,
                      fontWeight: FontWeight.bold,
                    ),
                  ),
                ],
              ),
            ),
        ],
      ),
    );
  }
}
