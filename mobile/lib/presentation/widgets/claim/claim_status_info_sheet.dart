import 'package:flutter/material.dart';

import '../../../core/theme/app_colors.dart';
import 'claim_status_badge.dart';

/// 클레임 상태 설명 바텀시트.
///
/// 목록/상세 상단의 info 아이콘 탭 시 표시. 주문 요청 상태 안내([OrderStatusInfoSheet]) 와 동일 구성.
///
/// 표시 값은 고정 목록이 아니다 — 고객상담 시스템(코스모스)이 회신한 조치상태 원문을 그대로 쓰고,
/// 회신 전이면 "미확인", 알라딘 전송에 실패한 건이면 "전송실패" 를 대신 보여준다. 그래서 값을
/// 나열하는 대신 **어떤 값이 왜 나오는지**를 설명한다.
class ClaimStatusInfoSheet extends StatelessWidget {
  const ClaimStatusInfoSheet({super.key});

  /// (표시 문구, 설명) 3종. 세 번째는 코스모스 회신값 예시라 대표값 하나로 보여준다.
  static const List<(String, String)> _items = [
    ('미확인', '클레임이 접수됐고, 고객상담 시스템에서 아직 조치 내용을 회신하지 않은 상태입니다.'),
    ('조치중', '고객상담 시스템이 회신한 조치 상태를 그대로 표시합니다. 진행에 따라 처리완료 등으로 바뀝니다.'),
    ('전송실패', '알라딘으로 전송하지 못한 상태입니다. 클레임 등록 자체는 완료됐으며, 관리자 재전송 후 조치가 시작됩니다.'),
  ];

  static void show(BuildContext context) {
    showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(20)),
      ),
      builder: (context) => const ClaimStatusInfoSheet(),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Container(
      constraints: BoxConstraints(
        maxHeight: MediaQuery.of(context).size.height * 0.6,
      ),
      padding: const EdgeInsets.all(20),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          // 핸들 바
          Container(
            width: 40,
            height: 4,
            decoration: BoxDecoration(
              color: AppColors.divider,
              borderRadius: BorderRadius.circular(2),
            ),
          ),
          const SizedBox(height: 16),

          const Align(
            alignment: Alignment.centerLeft,
            child: Text(
              '클레임 상태 안내',
              style: TextStyle(
                fontSize: 18,
                fontWeight: FontWeight.w700,
                color: AppColors.textPrimary,
              ),
            ),
          ),
          const SizedBox(height: 16),
          const Divider(height: 1),
          const SizedBox(height: 8),

          Flexible(
            child: ListView.separated(
              shrinkWrap: true,
              itemCount: _items.length,
              separatorBuilder: (_, _) => const SizedBox(height: 8),
              itemBuilder: (context, index) {
                final (label, description) = _items[index];
                return _buildStatusItem(label, description);
              },
            ),
          ),
          const SizedBox(height: 12),

          const Align(
            alignment: Alignment.centerLeft,
            child: Text(
              '상태는 매시간 자동으로 갱신됩니다.',
              style: TextStyle(
                fontSize: 12,
                height: 1.4,
                color: AppColors.textTertiary,
              ),
            ),
          ),
          const SizedBox(height: 16),

          SizedBox(
            width: double.infinity,
            child: OutlinedButton(
              onPressed: () => Navigator.of(context).pop(),
              style: OutlinedButton.styleFrom(
                padding: const EdgeInsets.symmetric(vertical: 14),
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(12),
                ),
                side: const BorderSide(color: AppColors.border),
              ),
              child: const Text(
                '닫기',
                style: TextStyle(
                  fontSize: 15,
                  fontWeight: FontWeight.w500,
                  color: AppColors.textSecondary,
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }

  /// 상태 1행: 뱃지 + 설명 문구
  Widget _buildStatusItem(String label, String description) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
      decoration: BoxDecoration(
        color: AppColors.white,
        border: Border.all(color: AppColors.border),
        borderRadius: BorderRadius.circular(10),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Align(
            alignment: Alignment.centerLeft,
            child: ClaimStatusBadge(label: label),
          ),
          const SizedBox(height: 8),
          Text(
            description,
            style: const TextStyle(
              fontSize: 13,
              height: 1.4,
              color: AppColors.textSecondary,
            ),
          ),
        ],
      ),
    );
  }
}
