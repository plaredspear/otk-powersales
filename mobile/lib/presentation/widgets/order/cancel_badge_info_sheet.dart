import 'package:flutter/material.dart';

import '../../../core/theme/app_colors.dart';

/// 취소 배지(취소요청 / 주문 취소) 설명 바텀시트.
///
/// "주문한 제품" 섹션 헤더의 info 아이콘 탭 시 표시. 제품 라인에 붙는 두 배지의 의미를
/// 라이프사이클(취소요청 → 주문 취소) 순서로 설명한다.
/// - 취소요청(주황): 사용자가 취소를 요청해 정상 접수된 상태.
/// - 주문 취소(빨강): 주문 취소 처리가 완료된 상태.
class CancelBadgeInfoSheet extends StatelessWidget {
  const CancelBadgeInfoSheet({super.key});

  /// 배지 1행: (라벨, 색, 표식 스타일, 설명) — 인라인 표시와 동일한 마커로 안내.
  static const List<_BadgeInfo> _badges = [
    _BadgeInfo(
      label: '취소요청',
      color: AppColors.warning,
      style: _MarkerStyle.dot,
      description: '취소를 요청해 정상적으로 접수된 상태입니다. 실제 취소 처리는 이후 진행됩니다.',
    ),
    _BadgeInfo(
      label: '주문 취소',
      color: AppColors.error,
      style: _MarkerStyle.pill,
      description: '주문 취소 처리가 완료된 상태입니다.',
    ),
  ];

  /// 바텀시트로 배지 설명 표시.
  static void show(BuildContext context) {
    showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(20)),
      ),
      builder: (context) => const CancelBadgeInfoSheet(),
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

          // 제목
          const Align(
            alignment: Alignment.centerLeft,
            child: Text(
              '취소 상태 안내',
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

          // 배지 설명 리스트
          Flexible(
            child: ListView.separated(
              shrinkWrap: true,
              itemCount: _badges.length,
              separatorBuilder: (_, _) => const SizedBox(height: 8),
              itemBuilder: (context, index) => _buildBadgeItem(_badges[index]),
            ),
          ),

          const SizedBox(height: 16),

          // 닫기 버튼
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

  /// 배지 1행: 인라인과 동일한 마커(동그라미 또는 pill) + 라벨 + 설명 문구.
  Widget _buildBadgeItem(_BadgeInfo info) {
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
          _buildMarker(info),
          const SizedBox(height: 8),
          Text(
            info.description,
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

  /// 마커 — dot 스타일이면 [색 동그라미 + 라벨], pill 스타일이면 [색 pill(라벨 내장)].
  Widget _buildMarker(_BadgeInfo info) {
    if (info.style == _MarkerStyle.dot) {
      return Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          Container(
            width: 10,
            height: 10,
            decoration: BoxDecoration(color: info.color, shape: BoxShape.circle),
          ),
          const SizedBox(width: 8),
          Text(
            info.label,
            style: const TextStyle(
              fontSize: 13,
              fontWeight: FontWeight.w700,
              color: AppColors.textPrimary,
            ),
          ),
        ],
      );
    }
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
      decoration: BoxDecoration(
        color: info.color,
        borderRadius: BorderRadius.circular(6),
      ),
      child: Text(
        info.label,
        style: const TextStyle(
          fontSize: 12,
          fontWeight: FontWeight.w700,
          color: AppColors.white,
        ),
      ),
    );
  }
}

/// 마커 표시 스타일 — 동그라미(문구 없음, 인라인) 또는 pill(문구 내장).
enum _MarkerStyle { dot, pill }

/// 배지 설명 데이터 (라벨 / 색 / 마커 스타일 / 설명).
class _BadgeInfo {
  final String label;
  final Color color;
  final _MarkerStyle style;
  final String description;

  const _BadgeInfo({
    required this.label,
    required this.color,
    required this.style,
    required this.description,
  });
}
