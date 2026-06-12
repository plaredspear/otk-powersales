import 'package:flutter/material.dart';

import '../common/synced_text_field.dart';
import 'inspection_section_header.dart';

/// 현장 점검 등록 - 자사 활동 정보 폼
///
/// 포함 필드:
/// - 설명 (필수): 레거시 fieldChk 정합 — 자사 점검 시 설명 필수
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
        // 활동 정보 섹션 헤더 (레거시 슬레이트 바)
        const InspectionSectionHeader(title: '자사 활동 정보'),

        // 설명 필드 (필수) — 레거시 정합: 자사 설명 필수, 인라인 리스트 행
        Padding(
          padding: const EdgeInsets.fromLTRB(16, 12, 16, 12),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              _buildRequiredLabel('설명'),
              SyncedTextField(
                value: description ?? '',
                onChanged: onDescriptionChanged,
                decoration: const InputDecoration(
                  hintText: '내용 입력',
                  hintStyle: TextStyle(color: Colors.grey),
                  isDense: true,
                  contentPadding: EdgeInsets.symmetric(vertical: 6),
                  border: InputBorder.none,
                ),
                maxLines: 2,
              ),
            ],
          ),
        ),
        const Divider(height: 1),

        // 제품 선택 필드 (필수) — 라벨 우측에 바코드/선택 알약버튼 (레거시 정합)
        Padding(
          padding: const EdgeInsets.fromLTRB(16, 12, 16, 12),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                children: [
                  _buildRequiredLabel('제품'),
                  const Spacer(),
                  // 바코드 스캔 버튼 (알약형)
                  _buildPillButton(
                    icon: Icons.qr_code_scanner,
                    label: '바코드',
                    onPressed: onBarcodeScan,
                  ),
                  const SizedBox(width: 8),
                  // 제품 선택 버튼 (알약형)
                  _buildPillButton(
                    icon: Icons.add,
                    label: '선택',
                    onPressed: onProductSelect,
                  ),
                ],
              ),
              const SizedBox(height: 4),
              // 선택된 제품 표시 (플레이스홀더)
              Text(
                selectedProductName ?? '제품 선택',
                style: TextStyle(
                  fontSize: 14,
                  color: selectedProductName != null
                      ? Colors.black
                      : Colors.grey,
                ),
              ),
            ],
          ),
        ),
        const Divider(height: 1),
      ],
    );
  }

  /// 필수(*) 라벨
  Widget _buildRequiredLabel(String text) {
    return RichText(
      text: TextSpan(
        children: [
          TextSpan(
            text: text,
            style: const TextStyle(fontSize: 16, color: Colors.black87),
          ),
          const TextSpan(
            text: ' *',
            style: TextStyle(fontSize: 16, color: Colors.red),
          ),
        ],
      ),
    );
  }

  /// 알약형 아웃라인 버튼 (레거시 정합 — 둥근 스타디움 테두리)
  Widget _buildPillButton({
    required IconData icon,
    required String label,
    required VoidCallback onPressed,
  }) {
    return OutlinedButton.icon(
      onPressed: onPressed,
      icon: Icon(icon, size: 18),
      label: Text(label),
      style: OutlinedButton.styleFrom(
        foregroundColor: Colors.black87,
        side: const BorderSide(color: Color(0xFFBDBDBD)),
        shape: const StadiumBorder(),
        // Row 안 무한폭 제약 크래시 방지 (전역 테마 덮어쓰기).
        minimumSize: Size.zero,
        padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 8),
        tapTargetSize: MaterialTapTargetSize.shrinkWrap,
      ),
    );
  }
}
