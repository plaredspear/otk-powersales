/// 유통기한 항목 엔티티
///
/// 유통기한 관리 화면에서 표시되는 제품의 유통기한 정보를 담는 도메인 엔티티입니다.
class ShelfLifeItem {
  /// 유통기한 항목 시퀀스 (PK)
  final int seq;

  /// 제품 코드
  final String productCode;

  /// 제품명
  final String productName;

  /// 거래처 코드
  final String accountCode;

  /// 거래처명
  final String accountName;

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
    required this.seq,
    required this.productCode,
    required this.productName,
    required this.accountCode,
    required this.accountName,
    required this.expiryDate,
    required this.alertDate,
    required this.dDay,
    this.description = '',
    required this.isExpired,
  });

  ShelfLifeItem copyWith({
    int? seq,
    String? productCode,
    String? productName,
    String? accountCode,
    String? accountName,
    DateTime? expiryDate,
    DateTime? alertDate,
    int? dDay,
    String? description,
    bool? isExpired,
  }) {
    return ShelfLifeItem(
      seq: seq ?? this.seq,
      productCode: productCode ?? this.productCode,
      productName: productName ?? this.productName,
      accountCode: accountCode ?? this.accountCode,
      accountName: accountName ?? this.accountName,
      expiryDate: expiryDate ?? this.expiryDate,
      alertDate: alertDate ?? this.alertDate,
      dDay: dDay ?? this.dDay,
      description: description ?? this.description,
      isExpired: isExpired ?? this.isExpired,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'seq': seq,
      'productCode': productCode,
      'productName': productName,
      'accountCode': accountCode,
      'accountName': accountName,
      'expiryDate': expiryDate.toIso8601String().substring(0, 10),
      'alertDate': alertDate.toIso8601String().substring(0, 10),
      'dDay': dDay,
      'description': description,
      'isExpired': isExpired,
    };
  }

  factory ShelfLifeItem.fromJson(Map<String, dynamic> json) {
    return ShelfLifeItem(
      seq: json['seq'] as int,
      productCode: json['productCode'] as String,
      productName: json['productName'] as String,
      accountCode: json['accountCode'] as String,
      accountName: json['accountName'] as String,
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
    return other.seq == seq &&
        other.productCode == productCode &&
        other.productName == productName &&
        other.accountCode == accountCode &&
        other.accountName == accountName &&
        other.expiryDate == expiryDate &&
        other.alertDate == alertDate &&
        other.dDay == dDay &&
        other.description == description &&
        other.isExpired == isExpired;
  }

  @override
  int get hashCode {
    return Object.hash(
      seq,
      productCode,
      productName,
      accountCode,
      accountName,
      expiryDate,
      alertDate,
      dDay,
      description,
      isExpired,
    );
  }

  @override
  String toString() {
    return 'ShelfLifeItem(seq: $seq, productCode: $productCode, '
        'productName: $productName, accountCode: $accountCode, '
        'accountName: $accountName, expiryDate: $expiryDate, '
        'alertDate: $alertDate, dDay: $dDay, '
        'description: $description, isExpired: $isExpired)';
  }
}

/// 유통기한 검색 필터
///
/// 유통기한 목록 조회 시 사용하는 검색 조건을 담는 값 객체입니다.
class ShelfLifeFilter {
  /// 거래처 코드 (null이면 전체)
  final String? accountCode;

  /// 검색 시작일
  final DateTime fromDate;

  /// 검색 종료일
  final DateTime toDate;

  const ShelfLifeFilter({
    this.accountCode,
    required this.fromDate,
    required this.toDate,
  });

  /// 기본 필터 생성 (시작: 오늘 - 7일, 종료: 오늘 + 3개월)
  factory ShelfLifeFilter.defaultFilter() {
    final now = DateTime.now();
    final today = DateTime(now.year, now.month, now.day);
    return ShelfLifeFilter(
      fromDate: today.subtract(const Duration(days: 7)),
      toDate: DateTime(today.year, today.month + 3, today.day),
    );
  }

  ShelfLifeFilter copyWith({
    String? accountCode,
    DateTime? fromDate,
    DateTime? toDate,
    bool clearAccountCode = false,
  }) {
    return ShelfLifeFilter(
      accountCode:
          clearAccountCode ? null : (accountCode ?? this.accountCode),
      fromDate: fromDate ?? this.fromDate,
      toDate: toDate ?? this.toDate,
    );
  }

  /// 검색 기간이 최대 180일(6개월) 이내인지 검증
  bool get isValidDateRange {
    final difference = toDate.difference(fromDate).inDays;
    return difference >= 0 && difference <= 180;
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    if (other is! ShelfLifeFilter) return false;
    return other.accountCode == accountCode &&
        other.fromDate == fromDate &&
        other.toDate == toDate;
  }

  @override
  int get hashCode => Object.hash(accountCode, fromDate, toDate);

  @override
  String toString() {
    return 'ShelfLifeFilter(accountCode: $accountCode, '
        'fromDate: $fromDate, toDate: $toDate)';
  }
}
