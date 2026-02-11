/// 유통기한 항목 엔티티
///
/// 유통기한 관리 화면에서 표시되는 제품의 유통기한 정보를 담는 도메인 엔티티입니다.
class ShelfLifeItem {
  /// 유통기한 항목 ID
  final int id;

  /// 제품 코드
  final String productCode;

  /// 제품명
  final String productName;

  /// 거래처명
  final String storeName;

  /// 거래처 ID
  final int storeId;

  /// 유통기한 (만료 날짜)
  final DateTime expiryDate;

  /// 마감 전 푸시 알림 날짜
  final DateTime alertDate;

  /// D-DAY (오늘 기준 남은 일수, 0이면 당일, 음수이면 지남)
  final int dDay;

  /// 설명 (선택 입력)
  final String description;

  /// 유통기한 경과 여부 (D-DAY <= 0)
  final bool isExpired;

  const ShelfLifeItem({
    required this.id,
    required this.productCode,
    required this.productName,
    required this.storeName,
    required this.storeId,
    required this.expiryDate,
    required this.alertDate,
    required this.dDay,
    this.description = '',
    required this.isExpired,
  });

  ShelfLifeItem copyWith({
    int? id,
    String? productCode,
    String? productName,
    String? storeName,
    int? storeId,
    DateTime? expiryDate,
    DateTime? alertDate,
    int? dDay,
    String? description,
    bool? isExpired,
  }) {
    return ShelfLifeItem(
      id: id ?? this.id,
      productCode: productCode ?? this.productCode,
      productName: productName ?? this.productName,
      storeName: storeName ?? this.storeName,
      storeId: storeId ?? this.storeId,
      expiryDate: expiryDate ?? this.expiryDate,
      alertDate: alertDate ?? this.alertDate,
      dDay: dDay ?? this.dDay,
      description: description ?? this.description,
      isExpired: isExpired ?? this.isExpired,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'productCode': productCode,
      'productName': productName,
      'storeName': storeName,
      'storeId': storeId,
      'expiryDate': expiryDate.toIso8601String().substring(0, 10),
      'alertDate': alertDate.toIso8601String().substring(0, 10),
      'dDay': dDay,
      'description': description,
      'isExpired': isExpired,
    };
  }

  factory ShelfLifeItem.fromJson(Map<String, dynamic> json) {
    return ShelfLifeItem(
      id: json['id'] as int,
      productCode: json['productCode'] as String,
      productName: json['productName'] as String,
      storeName: json['storeName'] as String,
      storeId: json['storeId'] as int,
      expiryDate: DateTime.parse(json['expiryDate'] as String),
      alertDate: DateTime.parse(json['alertDate'] as String),
      dDay: json['dDay'] as int,
      description: json['description'] as String? ?? '',
      isExpired: json['isExpired'] as bool,
    );
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    if (other is! ShelfLifeItem) return false;
    return other.id == id &&
        other.productCode == productCode &&
        other.productName == productName &&
        other.storeName == storeName &&
        other.storeId == storeId &&
        other.expiryDate == expiryDate &&
        other.alertDate == alertDate &&
        other.dDay == dDay &&
        other.description == description &&
        other.isExpired == isExpired;
  }

  @override
  int get hashCode {
    return Object.hash(
      id,
      productCode,
      productName,
      storeName,
      storeId,
      expiryDate,
      alertDate,
      dDay,
      description,
      isExpired,
    );
  }

  @override
  String toString() {
    return 'ShelfLifeItem(id: $id, productCode: $productCode, '
        'productName: $productName, storeName: $storeName, '
        'storeId: $storeId, expiryDate: $expiryDate, '
        'alertDate: $alertDate, dDay: $dDay, '
        'description: $description, isExpired: $isExpired)';
  }
}

/// 유통기한 검색 필터
///
/// 유통기한 목록 조회 시 사용하는 검색 조건을 담는 값 객체입니다.
class ShelfLifeFilter {
  /// 거래처 ID (null이면 전체)
  final int? storeId;

  /// 검색 시작일
  final DateTime fromDate;

  /// 검색 종료일
  final DateTime toDate;

  const ShelfLifeFilter({
    this.storeId,
    required this.fromDate,
    required this.toDate,
  });

  /// 기본 필터 생성 (오늘 기준 앞/뒤 7일)
  factory ShelfLifeFilter.defaultFilter() {
    final now = DateTime.now();
    final today = DateTime(now.year, now.month, now.day);
    return ShelfLifeFilter(
      fromDate: today.subtract(const Duration(days: 7)),
      toDate: today.add(const Duration(days: 7)),
    );
  }

  ShelfLifeFilter copyWith({
    int? storeId,
    DateTime? fromDate,
    DateTime? toDate,
    bool clearStoreId = false,
  }) {
    return ShelfLifeFilter(
      storeId: clearStoreId ? null : (storeId ?? this.storeId),
      fromDate: fromDate ?? this.fromDate,
      toDate: toDate ?? this.toDate,
    );
  }

  /// 검색 기간이 최대 6개월 이내인지 검증
  bool get isValidDateRange {
    final difference = toDate.difference(fromDate).inDays;
    return difference >= 0 && difference <= 183; // 약 6개월
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    if (other is! ShelfLifeFilter) return false;
    return other.storeId == storeId &&
        other.fromDate == fromDate &&
        other.toDate == toDate;
  }

  @override
  int get hashCode => Object.hash(storeId, fromDate, toDate);

  @override
  String toString() {
    return 'ShelfLifeFilter(storeId: $storeId, '
        'fromDate: $fromDate, toDate: $toDate)';
  }
}
