import 'package:flutter/material.dart';

import '../../../core/theme/app_colors.dart';
import '../../../domain/entities/order_request.dart';
import './approval_status_badge.dart';

/// 주문 요청 상태 설명 바텀시트
///
/// 주문 상세 헤더의 "주문 요청 상태" 옆 info 아이콘 탭 시 표시.
/// 상태 목록은 단일 진실인 [OrderStatusCode.filterOptions](전송/전송완료/전송실패/주문취소요청완료)에
/// 맞추고, 각 상태의 의미를 라이프사이클(전송 → 전송완료 또는 전송실패 → 주문취소요청완료) 기준으로 설명한다.
/// (표시명은 서버가 내려주며, `APPROVED`→"전송완료"·`CANCELED`→"주문취소요청완료" 로 백엔드가 변환해 전송한다.)
class OrderStatusInfoSheet extends StatelessWidget {
  const OrderStatusInfoSheet({super.key});

  /// 코드별 설명 문구.
  static const Map<String, String> _descriptions = {
    OrderStatusCode.sent: '주문 요청이 전송되어 접수 처리 중인 상태입니다.',
    OrderStatusCode.approved: '주문 요청이 정상적으로 전송·접수된 상태입니다.',
    OrderStatusCode.sendFailed: '주문 요청 전송에 실패한 상태입니다. 재주문이 필요합니다.',
    OrderStatusCode.canceled:
        '전송 완료된 주문에 대해 취소를 요청한 상태입니다. SAP에서 취소 요청을 접수한 뒤 실제 취소 처리까지는 시간이 걸릴 수 있습니다.',
  };

  /// 바텀시트로 상태 설명 표시
  static void show(BuildContext context) {
    showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(20)),
      ),
      builder: (context) => const OrderStatusInfoSheet(),
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
              '주문 요청 상태 안내',
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

          // 상태 설명 리스트
          Flexible(
            child: ListView.separated(
              shrinkWrap: true,
              itemCount: OrderStatusCode.filterOptions.length,
              separatorBuilder: (_, _) => const SizedBox(height: 8),
              itemBuilder: (context, index) {
                final option = OrderStatusCode.filterOptions[index];
                return _buildStatusItem(option.code, option.label);
              },
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

  /// 상태 1행: 배지 + 설명 문구
  Widget _buildStatusItem(String code, String label) {
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
            child: OrderRequestStatusBadge(
              statusCode: code,
              statusName: label,
            ),
          ),
          const SizedBox(height: 8),
          Text(
            _descriptions[code] ?? '',
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
