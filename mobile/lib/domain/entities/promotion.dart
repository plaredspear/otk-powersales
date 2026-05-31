/// 행사 목록 아이템 엔티티
class PromotionItem {
  final int id;
  final String promotionNumber;
  final String? promotionName;
  final String? promotionType;
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
    this.promotionType,
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
      promotionNumber: json['promotionNumber'] as String,
      promotionName: json['promotionName'] as String?,
      promotionType: json['promotionType'] as String?,
      accountName: json['accountName'] as String?,
      startDate: json['startDate'] as String,
      endDate: json['endDate'] as String,
      category: json['category'] as String?,
      standLocation: json['standLocation'] as String?,
      targetAmount: json['targetAmount'] as int?,
      actualAmount: json['actualAmount'] as int?,
      isClosed: json['isClosed'] as bool,
      myScheduleDate: json['myScheduleDate'] as String?,
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
  final String? promotionType;
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
    this.promotionType,
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
      promotionNumber: json['promotionNumber'] as String,
      promotionName: json['promotionName'] as String?,
      promotionType: json['promotionType'] as String?,
      accountName: json['accountName'] as String?,
      startDate: json['startDate'] as String,
      endDate: json['endDate'] as String,
      category: json['category'] as String?,
      standLocation: json['standLocation'] as String?,
      targetAmount: json['targetAmount'] as int?,
      actualAmount: json['actualAmount'] as int?,
      isClosed: json['isClosed'] as bool,
      primaryProductName: json['primaryProductName'] as String?,
      otherProduct: json['otherProduct'] as String?,
      message: json['message'] as String?,
      productType: json['productType'] as String?,
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

  /// 조회 사용자 본인에게 배정된 행 여부 (일매출 마감 진입점 노출용).
  final bool isMine;

  /// 여사원 일매출 마감 완료 여부.
  final bool isClosed;

  const PromotionEmployee({
    required this.id,
    this.employeeName,
    this.scheduleDate,
    this.workStatus,
    this.workType3,
    this.professionalPromotionTeam,
    this.targetAmount,
    this.actualAmount,
    this.isMine = false,
    this.isClosed = false,
  });

  factory PromotionEmployee.fromJson(Map<String, dynamic> json) {
    return PromotionEmployee(
      id: json['id'] as int,
      employeeName: json['employeeName'] as String?,
      scheduleDate: json['scheduleDate'] as String?,
      workStatus: json['workStatus'] as String?,
      workType3: json['workType3'] as String?,
      professionalPromotionTeam:
          json['professionalPromotionTeam'] as String?,
      targetAmount: json['targetAmount'] as int?,
      actualAmount: json['actualAmount'] as int?,
      isMine: json['isMine'] as bool? ?? false,
      isClosed: json['isClosed'] as bool? ?? false,
    );
  }
}
