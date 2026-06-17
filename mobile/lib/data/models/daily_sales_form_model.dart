import '../../domain/entities/daily_sales_form.dart';

/// 일매출 폼/결과 JSON 매핑.
class DailySalesFormModel {
  static DailySalesForm fromJson(Map<String, dynamic> json) {
    return DailySalesForm(
      promotionEmployeeId: json['promotionEmployeeId'] as int,
      promotionId: json['promotionId'] as int?,
      scheduleDate: json['scheduleDate'] as String?,
      employeeName: json['employeeName'] as String?,
      isClosed: json['isClosed'] as bool? ?? false,
      editable: json['editable'] as bool? ?? false,
      attendanceRegistered: json['attendanceRegistered'] as bool? ?? false,
      hasDraft: json['hasDraft'] as bool? ?? false,
      basePrice: json['basePrice'] as num?,
      primarySalesQuantity: json['primarySalesQuantity'] as num?,
      primarySalesPrice: json['primarySalesPrice'] as num?,
      primaryProductAmount: json['primaryProductAmount'] as num?,
      otherSalesQuantity: json['otherSalesQuantity'] as num?,
      otherSalesAmount: json['otherSalesAmount'] as num?,
      description: json['description'] as String?,
      imageUrl: json['imageUrl'] as String?,
    );
  }
}

/// 일매출 마감 결과 JSON 매핑.
class DailySalesCloseResultModel {
  static DailySalesCloseResult fromJson(Map<String, dynamic> json) {
    return DailySalesCloseResult(
      promotionEmployeeId: json['promotionEmployeeId'] as int,
      isClosed: json['isClosed'] as bool? ?? true,
      actualAmount: (json['actualAmount'] as num?)?.toInt(),
      imageUrl: json['imageUrl'] as String?,
    );
  }
}
