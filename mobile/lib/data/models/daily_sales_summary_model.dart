import '../../domain/entities/daily_sales_summary.dart';

/// DailySalesSummary API 모델 (DTO)
///
/// API 응답의 JSON을 Domain Entity로 변환한다.
class DailySalesSummaryModel {
  final String dailySalesId;
  final String salesDate;
  final int totalAmount;
  final String status;

  const DailySalesSummaryModel({
    required this.dailySalesId,
    required this.salesDate,
    required this.totalAmount,
    required this.status,
  });

  factory DailySalesSummaryModel.fromJson(Map<String, dynamic> json) {
    return DailySalesSummaryModel(
      dailySalesId: json['dailySalesId'] as String,
      salesDate: json['salesDate'] as String,
      totalAmount: json['totalAmount'] as int,
      status: json['status'] as String,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'dailySalesId': dailySalesId,
      'salesDate': salesDate,
      'totalAmount': totalAmount,
      'status': status,
    };
  }

  DailySalesSummary toEntity() {
    return DailySalesSummary(
      dailySalesId: dailySalesId,
      salesDate: DateTime.parse(salesDate),
      totalAmount: totalAmount,
      status: status,
    );
  }

  factory DailySalesSummaryModel.fromEntity(DailySalesSummary entity) {
    return DailySalesSummaryModel(
      dailySalesId: entity.dailySalesId,
      salesDate: entity.salesDate.toIso8601String(),
      totalAmount: entity.totalAmount,
      status: entity.status,
    );
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    return other is DailySalesSummaryModel &&
        other.dailySalesId == dailySalesId &&
        other.salesDate == salesDate &&
        other.totalAmount == totalAmount &&
        other.status == status;
  }

  @override
  int get hashCode {
    return Object.hash(dailySalesId, salesDate, totalAmount, status);
  }

  @override
  String toString() {
    return 'DailySalesSummaryModel(dailySalesId: $dailySalesId, '
        'salesDate: $salesDate, totalAmount: $totalAmount, status: $status)';
  }
}
