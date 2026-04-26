/// 물류 카테고리 Enum
/// 상온, 라면, 냉동/냉장 구분
enum LogisticsCategory {
  /// 상온
  normal('상온', 'NORMAL'),

  /// 라면
  ramen('라면', 'RAMEN'),

  /// 냉동/냉장
  frozen('냉동/냉장', 'FROZEN');

  const LogisticsCategory(this.displayName, this.code);

  /// 화면 표시용 이름
  final String displayName;

  /// API/DB 코드
  final String code;

  /// 코드로 Enum 찾기
  static LogisticsCategory fromCode(String code) {
    return LogisticsCategory.values.firstWhere(
      (category) => category.code == code,
      orElse: () => LogisticsCategory.normal,
    );
  }

  /// JSON 직렬화용
  String toJson() => code;

  /// JSON 역직렬화용
  static LogisticsCategory fromJson(String json) => fromCode(json);
}

/// 물류매출 엔티티
///
/// 당월: 물류예상실적 반영
/// 이전월: ABC물류배부 마감 실적
/// 카테고리별 실적: 상온, 라면, 냉동/냉장
class LogisticsSales {
  /// 년월 (예: 202601)
  final String yearMonth;

  /// 카테고리 (상온, 라면, 냉동/냉장)
  final LogisticsCategory category;

  /// 당월 실적 금액 (원)
  final int currentAmount;

  /// 전년 동월 실적 금액 (원)
  final int previousYearAmount;

  /// 전년 대비 증감 금액 (원)
  final int difference;

  /// 전년 대비 증감율 (%)
  final double growthRate;

  /// 당월 여부 (true: 물류예상실적, false: ABC물류배부 마감실적)
  final bool isCurrentMonth;

  /// 거래처명 (선택적)
  final String? customerName;

  const LogisticsSales({
    required this.yearMonth,
    required this.category,
    required this.currentAmount,
    required this.previousYearAmount,
    required this.difference,
    required this.growthRate,
    required this.isCurrentMonth,
    this.customerName,
  });

  /// 불변성을 유지하며 일부 필드를 변경한 새 인스턴스 생성
  LogisticsSales copyWith({
    String? yearMonth,
    LogisticsCategory? category,
    int? currentAmount,
    int? previousYearAmount,
    int? difference,
    double? growthRate,
    bool? isCurrentMonth,
    String? customerName,
  }) {
    return LogisticsSales(
      yearMonth: yearMonth ?? this.yearMonth,
      category: category ?? this.category,
      currentAmount: currentAmount ?? this.currentAmount,
      previousYearAmount: previousYearAmount ?? this.previousYearAmount,
      difference: difference ?? this.difference,
      growthRate: growthRate ?? this.growthRate,
      isCurrentMonth: isCurrentMonth ?? this.isCurrentMonth,
      customerName: customerName ?? this.customerName,
    );
  }

  /// JSON으로 변환
  Map<String, dynamic> toJson() {
    return {
      'yearMonth': yearMonth,
      'category': category.toJson(),
      'currentAmount': currentAmount,
      'previousYearAmount': previousYearAmount,
      'difference': difference,
      'growthRate': growthRate,
      'isCurrentMonth': isCurrentMonth,
      'customerName': customerName,
    };
  }

  /// JSON에서 엔티티 생성
  factory LogisticsSales.fromJson(Map<String, dynamic> json) {
    return LogisticsSales(
      yearMonth: json['yearMonth'] as String,
      category: LogisticsCategory.fromJson(json['category'] as String),
      currentAmount: json['currentAmount'] as int,
      previousYearAmount: json['previousYearAmount'] as int,
      difference: json['difference'] as int,
      growthRate: (json['growthRate'] as num).toDouble(),
      isCurrentMonth: json['isCurrentMonth'] as bool,
      customerName: json['customerName'] as String?,
    );
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;

    return other is LogisticsSales &&
        other.yearMonth == yearMonth &&
        other.category == category &&
        other.currentAmount == currentAmount &&
        other.previousYearAmount == previousYearAmount &&
        other.difference == difference &&
        other.growthRate == growthRate &&
        other.isCurrentMonth == isCurrentMonth &&
        other.customerName == customerName;
  }

  @override
  int get hashCode {
    return Object.hash(
      yearMonth,
      category,
      currentAmount,
      previousYearAmount,
      difference,
      growthRate,
      isCurrentMonth,
      customerName,
    );
  }

  @override
  String toString() {
    return 'LogisticsSales(yearMonth: $yearMonth, category: ${category.displayName}, '
        'currentAmount: $currentAmount, previousYearAmount: $previousYearAmount, '
        'difference: $difference, growthRate: $growthRate, '
        'isCurrentMonth: $isCurrentMonth, customerName: $customerName)';
  }
}