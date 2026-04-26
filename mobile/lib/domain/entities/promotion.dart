/// 행사 목록 아이템 엔티티
class PromotionItem {
  final int id;
  final String promotionNumber;
  final String? promotionName;
  final String? promotionTypeName;
  final String? accountName;
  final String startDate;
  final String endDate;
  final String? category;
  final String? standLocation;
  final int? targetAmount;
  final int? actualAmount;
  final bool isClosed;
  final String? myScheduleDate;

  const PromotionItem({
    required this.id,
    required this.promotionNumber,
    this.promotionName,
    this.promotionTypeName,
    this.accountName,
    required this.startDate,
    required this.endDate,
    this.category,
    this.standLocation,
    this.targetAmount,
    this.actualAmount,
    required this.isClosed,
    this.myScheduleDate,
  });

  factory PromotionItem.fromJson(Map<String, dynamic> json) {
    return PromotionItem(
      id: json['id'] as int,
      promotionNumber: json['promotion_number'] as String,
      promotionName: json['promotion_name'] as String?,
      promotionTypeName: json['promotion_type_name'] as String?,
      accountName: json['account_name'] as String?,
      startDate: json['start_date'] as String,
      endDate: json['end_date'] as String,
      category: json['category'] as String?,
      standLocation: json['stand_location'] as String?,
      targetAmount: json['target_amount'] as int?,
      actualAmount: json['actual_amount'] as int?,
      isClosed: json['is_closed'] as bool,
      myScheduleDate: json['my_schedule_date'] as String?,
    );
  }

  @override
  bool operator ==(Object other) =>
      identical(this, other) || other is PromotionItem && id == other.id;

  @override
  int get hashCode => id.hashCode;
}

/// 행사 상세 응답 엔티티
class PromotionDetail {
  final int id;
  final String promotionNumber;
  final String? promotionName;
  final String? promotionTypeName;
  final String? accountName;
  final String startDate;
  final String endDate;
  final String? category;
  final String? standLocation;
  final int? targetAmount;
  final int? actualAmount;
  final bool isClosed;
  final String? primaryProductName;
  final String? otherProduct;
  final String? message;
  final String? productType;
  final String? remark;
  final List<PromotionEmployee> employees;

  const PromotionDetail({
    required this.id,
    required this.promotionNumber,
    this.promotionName,
    this.promotionTypeName,
    this.accountName,
    required this.startDate,
    required this.endDate,
    this.category,
    this.standLocation,
    this.targetAmount,
    this.actualAmount,
    required this.isClosed,
    this.primaryProductName,
    this.otherProduct,
    this.message,
    this.productType,
    this.remark,
    required this.employees,
  });

  factory PromotionDetail.fromJson(Map<String, dynamic> json) {
    final employeesJson = json['employees'] as List<dynamic>? ?? [];
    return PromotionDetail(
      id: json['id'] as int,
      promotionNumber: json['promotion_number'] as String,
      promotionName: json['promotion_name'] as String?,
      promotionTypeName: json['promotion_type_name'] as String?,
      accountName: json['account_name'] as String?,
      startDate: json['start_date'] as String,
      endDate: json['end_date'] as String,
      category: json['category'] as String?,
      standLocation: json['stand_location'] as String?,
      targetAmount: json['target_amount'] as int?,
      actualAmount: json['actual_amount'] as int?,
      isClosed: json['is_closed'] as bool,
      primaryProductName: json['primary_product_name'] as String?,
      otherProduct: json['other_product'] as String?,
      message: json['message'] as String?,
      productType: json['product_type'] as String?,
      remark: json['remark'] as String?,
      employees: employeesJson
          .map((e) => PromotionEmployee.fromJson(e as Map<String, dynamic>))
          .toList(),
    );
  }

  /// 달성률 계산 (target=0이면 null)
  double? get achievementRate {
    if (targetAmount == null || targetAmount == 0) return null;
    return (actualAmount ?? 0) / targetAmount! * 100;
  }
}

/// 행사 조원 엔티티
class PromotionEmployee {
  final int id;
  final String? employeeName;
  final String? scheduleDate;
  final String? workStatus;
  final String? workType3;
  final String? professionalPromotionTeam;
  final int? targetAmount;
  final int? actualAmount;

  const PromotionEmployee({
    required this.id,
    this.employeeName,
    this.scheduleDate,
    this.workStatus,
    this.workType3,
    this.professionalPromotionTeam,
    this.targetAmount,
    this.actualAmount,
  });

  factory PromotionEmployee.fromJson(Map<String, dynamic> json) {
    return PromotionEmployee(
      id: json['id'] as int,
      employeeName: json['employee_name'] as String?,
      scheduleDate: json['schedule_date'] as String?,
      workStatus: json['work_status'] as String?,
      workType3: json['work_type3'] as String?,
      professionalPromotionTeam:
          json['professional_promotion_team'] as String?,
      targetAmount: json['target_amount'] as int?,
      actualAmount: json['actual_amount'] as int?,
    );
  }
}
