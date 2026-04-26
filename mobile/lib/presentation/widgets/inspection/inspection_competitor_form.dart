import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

/// 현장 점검 등록 - 경쟁사 활동 정보 폼
///
/// 포함 필드:
/// - 경쟁사명 (필수)
/// - 경쟁사 활동 내용 (필수)
/// - 시식 여부 (예/아니요 토글)
/// - 시식=예 시 조건부 필드:
///   - 경쟁사 상품명 (필수)
///   - 제품 가격 (필수, 숫자)
///   - 판매 수량 (필수, 숫자)
class InspectionCompetitorForm extends StatelessWidget {
  /// 경쟁사명
  final String? competitorName;

  /// 경쟁사 활동 내용
  final String? competitorActivity;

  /// 시식 여부
  final bool? competitorTasting;

  /// 경쟁사 상품명 (시식=예 시)
  final String? competitorProductName;

  /// 제품 가격 (시식=예 시)
  final int? competitorProductPrice;

  /// 판매 수량 (시식=예 시)
  final int? competitorSalesQuantity;

  /// 경쟁사명 변경 콜백
  final ValueChanged<String> onCompetitorNameChanged;

  /// 경쟁사 활동 내용 변경 콜백
  final ValueChanged<String> onCompetitorActivityChanged;

  /// 시식 여부 변경 콜백
  final ValueChanged<bool> onCompetitorTastingChanged;

  /// 경쟁사 상품명 변경 콜백
  final ValueChanged<String> onCompetitorProductNameChanged;

  /// 제품 가격 변경 콜백
  final ValueChanged<String> onCompetitorProductPriceChanged;

  /// 판매 수량 변경 콜백
  final ValueChanged<String> onCompetitorSalesQuantityChanged;

  const InspectionCompetitorForm({
    super.key,
    required this.competitorName,
    required this.competitorActivity,
    required this.competitorTasting,
    required this.competitorProductName,
    required this.competitorProductPrice,
    required this.competitorSalesQuantity,
    required this.onCompetitorNameChanged,
    required this.onCompetitorActivityChanged,
    required this.onCompetitorTastingChanged,
    required this.onCompetitorProductNameChanged,
    required this.onCompetitorProductPriceChanged,
    required this.onCompetitorSalesQuantityChanged,
  });

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        // 활동 정보 섹션 헤더
        const Padding(
          padding: EdgeInsets.all(16),
          child: Text(
            '경쟁사 활동 정보',
            style: TextStyle(
              fontSize: 18,
              fontWeight: FontWeight.bold,
            ),
          ),
        ),

        // 경쟁사명 (필수)
        Padding(
          padding: const EdgeInsets.symmetric(horizontal: 16),
          child: TextField(
            controller: TextEditingController(text: competitorName),
            onChanged: onCompetitorNameChanged,
            decoration: const InputDecoration(
              labelText: '경쟁사명 *',
              hintText: '예: 농심',
              border: OutlineInputBorder(),
            ),
          ),
        ),

        const SizedBox(height: 16),

        // 경쟁사 활동 내용 (필수)
        Padding(
          padding: const EdgeInsets.symmetric(horizontal: 16),
          child: TextField(
            controller: TextEditingController(text: competitorActivity),
            onChanged: onCompetitorActivityChanged,
            decoration: const InputDecoration(
              labelText: '경쟁사 활동 내용 *',
              hintText: '예: 시식 행사',
              border: OutlineInputBorder(),
            ),
            maxLines: 3,
          ),
        ),

        const SizedBox(height: 16),

        // 시식 여부 토글
        Padding(
          padding: const EdgeInsets.symmetric(horizontal: 16),
          child: Row(
            children: [
              RichText(
                text: const TextSpan(
                  children: [
                    TextSpan(
                      text: '시식 여부',
                      style: TextStyle(
                        fontSize: 16,
                        color: Colors.black87,
                      ),
                    ),
                    TextSpan(
                      text: ' *',
                      style: TextStyle(
                        fontSize: 16,
                        color: Colors.red,
                      ),
                    ),
                  ],
                ),
              ),
              const Spacer(),
              ToggleButtons(
                isSelected: [
                  competitorTasting == true,
                  competitorTasting == false,
                ],
                onPressed: (index) {
                  onCompetitorTastingChanged(index == 0);
                },
                borderRadius: BorderRadius.circular(8),
                constraints: const BoxConstraints(
                  minWidth: 80,
                  minHeight: 36,
                ),
                children: const [
                  Padding(
                    padding: EdgeInsets.symmetric(horizontal: 16),
                    child: Text('예'),
                  ),
                  Padding(
                    padding: EdgeInsets.symmetric(horizontal: 16),
                    child: Text('아니요'),
                  ),
                ],
              ),
            ],
          ),
        ),

        // 시식=예 시 조건부 필드들
        if (competitorTasting == true) ...[
          const SizedBox(height: 16),
          const Divider(height: 1),
          const SizedBox(height: 16),

          // 경쟁사 상품명 (시식=예 시 필수)
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: 16),
            child: TextField(
              controller: TextEditingController(text: competitorProductName),
              onChanged: onCompetitorProductNameChanged,
              decoration: const InputDecoration(
                labelText: '경쟁사 상품명 *',
                hintText: '예: 신라면 블랙',
                border: OutlineInputBorder(),
              ),
            ),
          ),

          const SizedBox(height: 16),

          // 제품 가격 (시식=예 시 필수, 숫자만)
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: 16),
            child: TextField(
              controller: TextEditingController(
                text: competitorProductPrice?.toString() ?? '',
              ),
              onChanged: onCompetitorProductPriceChanged,
              decoration: const InputDecoration(
                labelText: '제품 가격 *',
                hintText: '숫자만 입력 (원)',
                border: OutlineInputBorder(),
                suffixText: '원',
              ),
              keyboardType: TextInputType.number,
              inputFormatters: [
                FilteringTextInputFormatter.digitsOnly,
              ],
            ),
          ),

          const SizedBox(height: 16),

          // 판매 수량 (시식=예 시 필수, 숫자만)
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: 16),
            child: TextField(
              controller: TextEditingController(
                text: competitorSalesQuantity?.toString() ?? '',
              ),
              onChanged: onCompetitorSalesQuantityChanged,
              decoration: const InputDecoration(
                labelText: '판매 수량 *',
                hintText: '숫자만 입력 (개)',
                border: OutlineInputBorder(),
                suffixText: '개',
              ),
              keyboardType: TextInputType.number,
              inputFormatters: [
                FilteringTextInputFormatter.digitsOnly,
              ],
            ),
          ),
        ],

        const SizedBox(height: 16),
      ],
    );
  }
}
