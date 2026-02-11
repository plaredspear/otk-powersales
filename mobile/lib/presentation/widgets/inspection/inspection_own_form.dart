import 'package:flutter/material.dart';

/// 현장 점검 등록 - 자사 활동 정보 폼
///
/// 포함 필드:
/// - 설명 (선택)
/// - 제품 선택 (필수): 바코드 스캔 또는 수동 선택
class InspectionOwnForm extends StatelessWidget {
  /// 선택된 제품명
  final String? selectedProductName;

  /// 설명 텍스트
  final String? description;

  /// 설명 변경 콜백
  final ValueChanged<String> onDescriptionChanged;

  /// 바코드 스캔 콜백
  final VoidCallback onBarcodeScan;

  /// 제품 선택 콜백
  final VoidCallback onProductSelect;

  const InspectionOwnForm({
    super.key,
    required this.selectedProductName,
    required this.description,
    required this.onDescriptionChanged,
    required this.onBarcodeScan,
    required this.onProductSelect,
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
            '자사 활동 정보',
            style: TextStyle(
              fontSize: 18,
              fontWeight: FontWeight.bold,
            ),
          ),
        ),

        // 설명 필드 (선택)
        Padding(
          padding: const EdgeInsets.symmetric(horizontal: 16),
          child: TextField(
            controller: TextEditingController(text: description),
            onChanged: onDescriptionChanged,
            decoration: const InputDecoration(
              labelText: '설명',
              hintText: '예: 냉장고 앞 본매대',
              border: OutlineInputBorder(),
            ),
            maxLines: 3,
          ),
        ),

        const SizedBox(height: 16),

        // 제품 선택 필드 (필수)
        Padding(
          padding: const EdgeInsets.symmetric(horizontal: 16),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              RichText(
                text: const TextSpan(
                  children: [
                    TextSpan(
                      text: '제품',
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
              const SizedBox(height: 8),
              Row(
                children: [
                  // 선택된 제품 표시
                  Expanded(
                    child: Container(
                      padding: const EdgeInsets.all(12),
                      decoration: BoxDecoration(
                        border: Border.all(color: Colors.grey),
                        borderRadius: BorderRadius.circular(4),
                      ),
                      child: Text(
                        selectedProductName ?? '제품을 선택하세요',
                        style: TextStyle(
                          fontSize: 14,
                          color: selectedProductName != null
                              ? Colors.black
                              : Colors.grey,
                        ),
                      ),
                    ),
                  ),
                  const SizedBox(width: 8),
                  // 바코드 스캔 버튼
                  OutlinedButton.icon(
                    onPressed: onBarcodeScan,
                    icon: const Icon(Icons.qr_code_scanner),
                    label: const Text('바코드'),
                    style: OutlinedButton.styleFrom(
                      padding: const EdgeInsets.symmetric(
                        horizontal: 12,
                        vertical: 12,
                      ),
                    ),
                  ),
                  const SizedBox(width: 8),
                  // 제품 선택 버튼
                  OutlinedButton.icon(
                    onPressed: onProductSelect,
                    icon: const Icon(Icons.add),
                    label: const Text('선택'),
                    style: OutlinedButton.styleFrom(
                      padding: const EdgeInsets.symmetric(
                        horizontal: 12,
                        vertical: 12,
                      ),
                    ),
                  ),
                ],
              ),
            ],
          ),
        ),

        const SizedBox(height: 16),
      ],
    );
  }
}
