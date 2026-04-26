/// 제품유형별 매출 엔티티
///
/// 상온, 냉장/냉동 등 제품 유형별 목표/달성 금액을 담는 도메인 엔티티입니다.
class CategorySales {
  /// 제품유형 (상온, 냉장/냉동)
  final String category;

  /// 목표 금액 (원)
  final int targetAmount;

  /// 달성 금액 (원)
  final int achievedAmount;

  /// 달성율 (%)
  final double achievementRate;

  const CategorySales({
    required this.category,
    required this.targetAmount,
    required this.achievedAmount,
    required this.achievementRate,
  });

  CategorySales copyWith({
    String? category,
    int? targetAmount,
    int? achievedAmount,
    double? achievementRate,
  }) {
    return CategorySales(
      category: category ?? this.category,
      targetAmount: targetAmount ?? this.targetAmount,
      achievedAmount: achievedAmount ?? this.achievedAmount,
      achievementRate: achievementRate ?? this.achievementRate,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'category': category,
      'targetAmount': targetAmount,
      'achievedAmount': achievedAmount,
      'achievementRate': achievementRate,
    };
  }

  factory CategorySales.fromJson(Map<String, dynamic> json) {
    return CategorySales(
      category: json['category'] as String,
      targetAmount: json['targetAmount'] as int,
      achievedAmount: json['achievedAmount'] as int,
      achievementRate: (json['achievementRate'] as num).toDouble(),
    );
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    return other is CategorySales &&
        other.category == category &&
        other.targetAmount == targetAmount &&
        other.achievedAmount == achievedAmount &&
        other.achievementRate == achievementRate;
  }

  @override
  int get hashCode {
    return Object.hash(
      category,
      targetAmount,
      achievedAmount,
      achievementRate,
    );
  }

  @override
  String toString() {
    return 'CategorySales(category: $category, targetAmount: $targetAmount, '
        'achievedAmount: $achievedAmount, achievementRate: $achievementRate)';
  }
}

/// 월 평균 실적 엔티티
///
/// 전년도와 금년도의 월 평균 실적을 담는 도메인 엔티티입니다.
class MonthlyAverage {
  /// 금년도 1월~현재월 평균 (원)
  final int currentYearAverage;

  /// 전년도 동기간 평균 (원)
  final int previousYearAverage;

  /// 시작월 (1)
  final int startMonth;

  /// 종료월 (현재 조회 월)
  final int endMonth;

  const MonthlyAverage({
    required this.currentYearAverage,
    required this.previousYearAverage,
    required this.startMonth,
    required this.endMonth,
  });

  MonthlyAverage copyWith({
    int? currentYearAverage,
    int? previousYearAverage,
    int? startMonth,
    int? endMonth,
  }) {
    return MonthlyAverage(
      currentYearAverage: currentYearAverage ?? this.currentYearAverage,
      previousYearAverage: previousYearAverage ?? this.previousYearAverage,
      startMonth: startMonth ?? this.startMonth,
      endMonth: endMonth ?? this.endMonth,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'currentYearAverage': currentYearAverage,
      'previousYearAverage': previousYearAverage,
      'startMonth': startMonth,
      'endMonth': endMonth,
    };
  }

  factory MonthlyAverage.fromJson(Map<String, dynamic> json) {
    return MonthlyAverage(
      currentYearAverage: json['currentYearAverage'] as int,
      previousYearAverage: json['previousYearAverage'] as int,
      startMonth: json['startMonth'] as int,
      endMonth: json['endMonth'] as int,
    );
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    return other is MonthlyAverage &&
        other.currentYearAverage == currentYearAverage &&
        other.previousYearAverage == previousYearAverage &&
        other.startMonth == startMonth &&
        other.endMonth == endMonth;
  }

  @override
  int get hashCode {
    return Object.hash(
      currentYearAverage,
      previousYearAverage,
      startMonth,
      endMonth,
    );
  }

  @override
  String toString() {
    return 'MonthlyAverage(currentYearAverage: $currentYearAverage, '
        'previousYearAverage: $previousYearAverage, startMonth: $startMonth, '
        'endMonth: $endMonth)';
  }
}

/// 월매출 엔티티
///
/// 거래처별 월매출 통계 정보를 담는 도메인 엔티티입니다.
class MonthlySales {
  /// 거래처 ID
  final String customerId;

  /// 거래처명
  final String customerName;

  /// 조회 연월 (YYYYMM)
  final String yearMonth;

  /// 전체 목표 금액 (원)
  final int targetAmount;

  /// 달성 금액 (원)
  final int achievedAmount;

  /// 달성율 (%)
  final double achievementRate;

  /// 제품유형별 매출
  final List<CategorySales> categorySales;

  /// 전년 동월 매출 (원)
  final int previousYearSameMonth;

  /// 월 평균 실적
  final MonthlyAverage monthlyAverage;

  const MonthlySales({
    required this.customerId,
    required this.customerName,
    required this.yearMonth,
    required this.targetAmount,
    required this.achievedAmount,
    required this.achievementRate,
    required this.categorySales,
    required this.previousYearSameMonth,
    required this.monthlyAverage,
  });

  MonthlySales copyWith({
    String? customerId,
    String? customerName,
    String? yearMonth,
    int? targetAmount,
    int? achievedAmount,
    double? achievementRate,
    List<CategorySales>? categorySales,
    int? previousYearSameMonth,
    MonthlyAverage? monthlyAverage,
  }) {
    return MonthlySales(
      customerId: customerId ?? this.customerId,
      customerName: customerName ?? this.customerName,
      yearMonth: yearMonth ?? this.yearMonth,
      targetAmount: targetAmount ?? this.targetAmount,
      achievedAmount: achievedAmount ?? this.achievedAmount,
      achievementRate: achievementRate ?? this.achievementRate,
      categorySales: categorySales ?? this.categorySales,
      previousYearSameMonth:
          previousYearSameMonth ?? this.previousYearSameMonth,
      monthlyAverage: monthlyAverage ?? this.monthlyAverage,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'customerId': customerId,
      'customerName': customerName,
      'yearMonth': yearMonth,
      'targetAmount': targetAmount,
      'achievedAmount': achievedAmount,
      'achievementRate': achievementRate,
      'categorySales': categorySales.map((c) => c.toJson()).toList(),
      'previousYearSameMonth': previousYearSameMonth,
      'monthlyAverage': monthlyAverage.toJson(),
    };
  }

  factory MonthlySales.fromJson(Map<String, dynamic> json) {
    return MonthlySales(
      customerId: json['customerId'] as String,
      customerName: json['customerName'] as String,
      yearMonth: json['yearMonth'] as String,
      targetAmount: json['targetAmount'] as int,
      achievedAmount: json['achievedAmount'] as int,
      achievementRate: (json['achievementRate'] as num).toDouble(),
      categorySales: (json['categorySales'] as List)
          .map((item) => CategorySales.fromJson(item as Map<String, dynamic>))
          .toList(),
      previousYearSameMonth: json['previousYearSameMonth'] as int,
      monthlyAverage: MonthlyAverage.fromJson(
          json['monthlyAverage'] as Map<String, dynamic>),
    );
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    return other is MonthlySales &&
        other.customerId == customerId &&
        other.customerName == customerName &&
        other.yearMonth == yearMonth &&
        other.targetAmount == targetAmount &&
        other.achievedAmount == achievedAmount &&
        other.achievementRate == achievementRate &&
        _listEquals(other.categorySales, categorySales) &&
        other.previousYearSameMonth == previousYearSameMonth &&
        other.monthlyAverage == monthlyAverage;
  }

  @override
  int get hashCode {
    return Object.hash(
      customerId,
      customerName,
      yearMonth,
      targetAmount,
      achievedAmount,
      achievementRate,
      Object.hashAll(categorySales),
      previousYearSameMonth,
      monthlyAverage,
    );
  }

  @override
  String toString() {
    return 'MonthlySales(customerId: $customerId, customerName: $customerName, '
        'yearMonth: $yearMonth, targetAmount: $targetAmount, '
        'achievedAmount: $achievedAmount, achievementRate: $achievementRate, '
        'categorySales: $categorySales, '
        'previousYearSameMonth: $previousYearSameMonth, '
        'monthlyAverage: $monthlyAverage)';
  }

  /// List equality helper
  bool _listEquals<T>(List<T>? a, List<T>? b) {
    if (a == null) return b == null;
    if (b == null || a.length != b.length) return false;
    for (int i = 0; i < a.length; i++) {
      if (a[i] != b[i]) return false;
    }
    return true;
  }
}
