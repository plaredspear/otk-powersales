import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:intl/intl.dart';
import '../../../domain/entities/pos_sales.dart';
import '../../providers/favorite_product_provider.dart';

/// POS 매출 리스트 아이템 위젯
///
/// 하나의 POS 매출 정보를 카드 형태로 표시합니다.
class PosSalesItem extends ConsumerWidget {
  /// 표시할 POS 매출 데이터
  final PosSales sales;

  /// 아이템 탭 콜백 (선택적)
  final VoidCallback? onTap;

  const PosSalesItem({
    super.key,
    required this.sales,
    this.onTap,
  });

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    // 제품 ID로 즐겨찾기 여부 확인 (제품명을 ID로 사용)
    final isFavoriteAsync = ref.watch(isFavoriteProvider(sales.productName));

    return Card(
      margin: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
      elevation: 2,
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(4),
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              // 헤더: 매장명 + 날짜
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  // 매장명
                  Row(
                    children: [
                      Icon(
                        Icons.store,
                        size: 20,
                        color: Colors.blue[700],
                      ),
                      const SizedBox(width: 8),
                      Text(
                        sales.storeName,
                        style: TextStyle(
                          fontSize: 16,
                          fontWeight: FontWeight.bold,
                          color: Colors.blue[900],
                        ),
                      ),
                    ],
                  ),
                  // 판매일자
                  Text(
                    _formatDate(sales.salesDate),
                    style: TextStyle(
                      fontSize: 14,
                      color: Colors.grey[600],
                    ),
                  ),
                ],
              ),

              const SizedBox(height: 12),

              // 제품명 + 즐겨찾기 버튼
              Row(
                children: [
                  Icon(
                    Icons.shopping_bag,
                    size: 18,
                    color: Colors.grey[700],
                  ),
                  const SizedBox(width: 8),
                  Expanded(
                    child: Text(
                      sales.productName,
                      style: const TextStyle(
                        fontSize: 15,
                        fontWeight: FontWeight.w500,
                      ),
                    ),
                  ),
                  // 즐겨찾기 버튼
                  isFavoriteAsync.when(
                    data: (isFavorite) => IconButton(
                      icon: Icon(
                        isFavorite ? Icons.star : Icons.star_border,
                        color: isFavorite ? Colors.amber : Colors.grey[400],
                        size: 24,
                      ),
                      onPressed: () async {
                        final notifier = ref.read(favoriteProductsProvider.notifier);
                        await notifier.toggleFavorite(sales.productName, sales.productName);
                        // Provider를 새로고침하여 UI 업데이트
                        ref.invalidate(isFavoriteProvider(sales.productName));
                      },
                      tooltip: isFavorite ? '즐겨찾기 해제' : '즐겨찾기 추가',
                    ),
                    loading: () => const SizedBox(
                      width: 24,
                      height: 24,
                      child: CircularProgressIndicator(strokeWidth: 2),
                    ),
                    error: (_, __) => Icon(
                      Icons.star_border,
                      color: Colors.grey[400],
                      size: 24,
                    ),
                  ),
                ],
              ),

              const SizedBox(height: 12),

              // 수량 + 금액
              Row(
                mainAxisAlignment: MainAxisAlignment.end,
                children: [
                  // 수량
                  _buildInfoChip(
                    icon: Icons.inventory_2,
                    label: '수량',
                    value: _formatQuantity(sales.quantity),
                    color: Colors.green,
                  ),
                  const SizedBox(width: 12),
                  // 금액
                  _buildInfoChip(
                    icon: Icons.payments,
                    label: '금액',
                    value: _formatCurrency(sales.amount),
                    color: Colors.orange,
                  ),
                ],
              ),

              // 카테고리 (선택적)
              if (sales.category != null) ...[
                const SizedBox(height: 8),
                Align(
                  alignment: Alignment.centerLeft,
                  child: Container(
                    padding: const EdgeInsets.symmetric(
                      horizontal: 8,
                      vertical: 4,
                    ),
                    decoration: BoxDecoration(
                      color: Colors.blue[50],
                      borderRadius: BorderRadius.circular(4),
                      border: Border.all(color: Colors.blue[200]!),
                    ),
                    child: Text(
                      sales.category!,
                      style: TextStyle(
                        fontSize: 12,
                        color: Colors.blue[800],
                      ),
                    ),
                  ),
                ),
              ],
            ],
          ),
        ),
      ),
    );
  }

  /// 정보 칩 위젯 (수량, 금액 표시용)
  Widget _buildInfoChip({
    required IconData icon,
    required String label,
    required String value,
    required Color color,
  }) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
      decoration: BoxDecoration(
        color: color.withOpacity(0.1),
        borderRadius: BorderRadius.circular(8),
        border: Border.all(color: color.withOpacity(0.3)),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          Icon(icon, size: 16, color: color),
          const SizedBox(width: 4),
          Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                label,
                style: TextStyle(
                  fontSize: 10,
                  color: Colors.grey[600],
                ),
              ),
              Text(
                value,
                style: TextStyle(
                  fontSize: 14,
                  fontWeight: FontWeight.bold,
                  color: color,
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }

  /// 날짜를 'yyyy-MM-dd (E)' 형식으로 포맷
  String _formatDate(DateTime date) {
    return DateFormat('yyyy-MM-dd (E)', 'ko_KR').format(date);
  }

  /// 수량을 '###,###개' 형식으로 포맷
  String _formatQuantity(int quantity) {
    final formatted = NumberFormat('#,###').format(quantity);
    return '$formatted개';
  }

  /// 금액을 '###,###원' 형식으로 포맷
  String _formatCurrency(int amount) {
    final formatted = NumberFormat('#,###').format(amount);
    return '$formatted원';
  }
}
