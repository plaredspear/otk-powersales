/// 현장 점검 목록 항목 엔티티
///
/// 현장 점검 목록 화면에서 표시되는 항목 정보를 담는 도메인 엔티티입니다.
class InspectionListItem {
  /// 점검 ID
  final int id;

  /// 분류 (자사/경쟁사)
  final InspectionCategory category;

  /// 거래처명
  final String storeName;

  /// 거래처 ID
  final int storeId;

  /// 점검일
  final DateTime inspectionDate;

  /// 현장 유형명
  final String fieldType;

  /// 현장 유형 코드
  final String fieldTypeCode;

  const InspectionListItem({
    required this.id,
    required this.category,
    required this.storeName,
    required this.storeId,
    required this.inspectionDate,
    required this.fieldType,
    required this.fieldTypeCode,
  });

  InspectionListItem copyWith({
    int? id,
    InspectionCategory? category,
    String? storeName,
    int? storeId,
    DateTime? inspectionDate,
    String? fieldType,
    String? fieldTypeCode,
  }) {
    return InspectionListItem(
      id: id ?? this.id,
      category: category ?? this.category,
      storeName: storeName ?? this.storeName,
      storeId: storeId ?? this.storeId,
      inspectionDate: inspectionDate ?? this.inspectionDate,
      fieldType: fieldType ?? this.fieldType,
      fieldTypeCode: fieldTypeCode ?? this.fieldTypeCode,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'category': category.toJson(),
      'storeName': storeName,
      'storeId': storeId,
      'inspectionDate': inspectionDate.toIso8601String().substring(0, 10),
      'fieldType': fieldType,
      'fieldTypeCode': fieldTypeCode,
    };
  }

  factory InspectionListItem.fromJson(Map<String, dynamic> json) {
    return InspectionListItem(
      id: json['id'] as int,
      category: InspectionCategoryExtension.fromJson(json['category'] as String),
      storeName: json['storeName'] as String,
      storeId: json['storeId'] as int,
      inspectionDate: DateTime.parse(json['inspectionDate'] as String),
      fieldType: json['fieldType'] as String,
      fieldTypeCode: json['fieldTypeCode'] as String,
    );
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    if (other is! InspectionListItem) return false;
    return other.id == id &&
        other.category == category &&
        other.storeName == storeName &&
        other.storeId == storeId &&
        other.inspectionDate == inspectionDate &&
        other.fieldType == fieldType &&
        other.fieldTypeCode == fieldTypeCode;
  }

  @override
  int get hashCode {
    return Object.hash(
      id,
      category,
      storeName,
      storeId,
      inspectionDate,
      fieldType,
      fieldTypeCode,
    );
  }

  @override
  String toString() {
    return 'InspectionListItem(id: $id, category: $category, '
        'storeName: $storeName, storeId: $storeId, '
        'inspectionDate: $inspectionDate, fieldType: $fieldType, '
        'fieldTypeCode: $fieldTypeCode)';
  }
}

/// 현장 점검 분류
enum InspectionCategory {
  /// 자사
  OWN,

  /// 경쟁사
  COMPETITOR,
}

extension InspectionCategoryExtension on InspectionCategory {
  /// 분류를 JSON 문자열로 변환
  String toJson() {
    switch (this) {
      case InspectionCategory.OWN:
        return 'OWN';
      case InspectionCategory.COMPETITOR:
        return 'COMPETITOR';
    }
  }

  /// 분류 한글명
  String get displayName {
    switch (this) {
      case InspectionCategory.OWN:
        return '자사';
      case InspectionCategory.COMPETITOR:
        return '경쟁사';
    }
  }

  /// JSON 문자열에서 분류로 변환
  static InspectionCategory fromJson(String value) {
    switch (value) {
      case 'OWN':
        return InspectionCategory.OWN;
      case 'COMPETITOR':
        return InspectionCategory.COMPETITOR;
      default:
        throw ArgumentError('Invalid InspectionCategory value: $value');
    }
  }
}

/// 현장 점검 검색 필터
///
/// 현장 점검 목록 조회 시 사용하는 검색 조건을 담는 값 객체입니다.
class InspectionFilter {
  /// 거래처 ID (null이면 전체)
  final int? storeId;

  /// 분류 (null이면 전체)
  final InspectionCategory? category;

  /// 점검일 시작
  final DateTime fromDate;

  /// 점검일 종료
  final DateTime toDate;

  const InspectionFilter({
    this.storeId,
    this.category,
    required this.fromDate,
    required this.toDate,
  });

  /// 기본 필터 생성 (오늘-7일 ~ 오늘)
  factory InspectionFilter.defaultFilter() {
    final now = DateTime.now();
    final today = DateTime(now.year, now.month, now.day);
    return InspectionFilter(
      fromDate: today.subtract(const Duration(days: 7)),
      toDate: today,
    );
  }

  InspectionFilter copyWith({
    int? storeId,
    InspectionCategory? category,
    DateTime? fromDate,
    DateTime? toDate,
    bool clearStoreId = false,
    bool clearCategory = false,
  }) {
    return InspectionFilter(
      storeId: clearStoreId ? null : (storeId ?? this.storeId),
      category: clearCategory ? null : (category ?? this.category),
      fromDate: fromDate ?? this.fromDate,
      toDate: toDate ?? this.toDate,
    );
  }

  /// 날짜 범위가 유효한지 검증 (시작일 <= 종료일)
  bool get isValidDateRange {
    return fromDate.isBefore(toDate) || fromDate.isAtSameMomentAs(toDate);
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    if (other is! InspectionFilter) return false;
    return other.storeId == storeId &&
        other.category == category &&
        other.fromDate == fromDate &&
        other.toDate == toDate;
  }

  @override
  int get hashCode => Object.hash(storeId, category, fromDate, toDate);

  @override
  String toString() {
    return 'InspectionFilter(storeId: $storeId, category: $category, '
        'fromDate: $fromDate, toDate: $toDate)';
  }
}
