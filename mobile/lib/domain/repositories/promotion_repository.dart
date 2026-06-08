import '../entities/promotion.dart';

/// 행사 목록 조회 결과
class PromotionListResult {
  final List<PromotionItem> items;
  final int totalElements;
  final int totalPages;
  final bool isLast;

  const PromotionListResult({
    required this.items,
    required this.totalElements,
    required this.totalPages,
    required this.isLast,
  });
}

/// 행사 Repository 인터페이스
abstract class PromotionRepository {
  /// 행사 목록 조회
  Future<PromotionListResult> getPromotions({
    String? startDate,
    String? endDate,
    String? keyword,
    int? accountId,
    int page = 0,
    int size = 20,
  });

  /// 행사 상세 조회
  Future<PromotionDetail> getPromotion(int id);
}
