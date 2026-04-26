/// 일별 매출 요약 엔티티 (조회용)
///
/// 행사 상세 화면에서 일별 판매금액 목록을 조회할 때 사용하는 도메인 엔티티입니다.
class DailySalesSummary {
  /// 일매출 ID
  final String dailySalesId;

  /// 매출 일자
  final DateTime salesDate;

  /// 총 판매금액 (원)
  final int totalAmount;

  /// 상태 (DRAFT: 임시저장, REGISTERED: 등록완료)
  final String status;

  const DailySalesSummary({
    required this.dailySalesId,
    required this.salesDate,
    required this.totalAmount,
    required this.status,
  });

  DailySalesSummary copyWith({
    String? dailySalesId,
    DateTime? salesDate,
    int? totalAmount,
    String? status,
  }) {
    return DailySalesSummary(
      dailySalesId: dailySalesId ?? this.dailySalesId,
      salesDate: salesDate ?? this.salesDate,
      totalAmount: totalAmount ?? this.totalAmount,
      status: status ?? this.status,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'dailySalesId': dailySalesId,
      'salesDate': salesDate.toIso8601String(),
      'totalAmount': totalAmount,
      'status': status,
    };
  }

  factory DailySalesSummary.fromJson(Map<String, dynamic> json) {
    return DailySalesSummary(
      dailySalesId: json['dailySalesId'] as String,
      salesDate: DateTime.parse(json['salesDate'] as String),
      totalAmount: json['totalAmount'] as int,
      status: json['status'] as String,
    );
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    return other is DailySalesSummary &&
        other.dailySalesId == dailySalesId &&
        other.salesDate == salesDate &&
        other.totalAmount == totalAmount &&
        other.status == status;
  }

  @override
  int get hashCode {
    return Object.hash(
      dailySalesId,
      salesDate,
      totalAmount,
      status,
    );
  }

  @override
  String toString() {
    return 'DailySalesSummary(dailySalesId: $dailySalesId, '
        'salesDate: $salesDate, totalAmount: $totalAmount, status: $status)';
  }
}
