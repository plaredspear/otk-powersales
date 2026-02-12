import '../../domain/entities/monthly_sales.dart';

/// CategorySales API 모델 (DTO)
///
/// API 응답의 JSON을 Domain Entity로 변환한다.
class CategorySalesModel {
  final String category;
  final int targetAmount;
  final int achievedAmount;
  final double achievementRate;

  const CategorySalesModel({
    required this.category,
    required this.targetAmount,
    required this.achievedAmount,
    required this.achievementRate,
  });

  factory CategorySalesModel.fromJson(Map<String, dynamic> json) {
    return CategorySalesModel(
      category: json['category'] as String,
      targetAmount: json['targetAmount'] as int,
      achievedAmount: json['achievedAmount'] as int,
      achievementRate: (json['achievementRate'] as num).toDouble(),
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

  CategorySales toEntity() {
    return CategorySales(
      category: category,
      targetAmount: targetAmount,
      achievedAmount: achievedAmount,
      achievementRate: achievementRate,
    );
  }

  factory CategorySalesModel.fromEntity(CategorySales entity) {
    return CategorySalesModel(
      category: entity.category,
      targetAmount: entity.targetAmount,
      achievedAmount: entity.achievedAmount,
      achievementRate: entity.achievementRate,
    );
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    return other is CategorySalesModel &&
        other.category == category &&
        other.targetAmount == targetAmount &&
        other.achievedAmount == achievedAmount &&
        other.achievementRate == achievementRate;
  }

  @override
  int get hashCode {
    return Object.hash(category, targetAmount, achievedAmount, achievementRate);
  }

  @override
  String toString() {
    return 'CategorySalesModel(category: $category, '
        'targetAmount: $targetAmount, achievedAmount: $achievedAmount, '
        'achievementRate: $achievementRate%)';
  }
}

/// MonthlyAverage API 모델 (DTO)
///
/// API 응답의 JSON을 Domain Entity로 변환한다.
class MonthlyAverageModel {
  final int currentYearAverage;
  final int previousYearAverage;
  final int startMonth;
  final int endMonth;

  const MonthlyAverageModel({
    required this.currentYearAverage,
    required this.previousYearAverage,
    required this.startMonth,
    required this.endMonth,
  });

  factory MonthlyAverageModel.fromJson(Map<String, dynamic> json) {
    return MonthlyAverageModel(
      currentYearAverage: json['currentYearAverage'] as int,
      previousYearAverage: json['previousYearAverage'] as int,
      startMonth: json['startMonth'] as int,
      endMonth: json['endMonth'] as int,
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

  MonthlyAverage toEntity() {
    return MonthlyAverage(
      currentYearAverage: currentYearAverage,
      previousYearAverage: previousYearAverage,
      startMonth: startMonth,
      endMonth: endMonth,
    );
  }

  factory MonthlyAverageModel.fromEntity(MonthlyAverage entity) {
    return MonthlyAverageModel(
      currentYearAverage: entity.currentYearAverage,
      previousYearAverage: entity.previousYearAverage,
      startMonth: entity.startMonth,
      endMonth: entity.endMonth,
    );
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    return other is MonthlyAverageModel &&
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
    return 'MonthlyAverageModel(currentYearAverage: $currentYearAverage, '
        'previousYearAverage: $previousYearAverage, '
        'startMonth: $startMonth, endMonth: $endMonth)';
  }
}

/// MonthlySales API 모델 (DTO)
///
/// API 응답의 JSON을 Domain Entity로 변환한다.
class MonthlySalesModel {
  final String customerId;
  final String customerName;
  final String yearMonth;
  final int targetAmount;
  final int achievedAmount;
  final double achievementRate;
  final List<CategorySalesModel> categorySales;
  final int previousYearSameMonth;
  final MonthlyAverageModel monthlyAverage;

  const MonthlySalesModel({
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

  factory MonthlySalesModel.fromJson(Map<String, dynamic> json) {
    return MonthlySalesModel(
      customerId: json['customerId'] as String,
      customerName: json['customerName'] as String,
      yearMonth: json['yearMonth'] as String,
      targetAmount: json['targetAmount'] as int,
      achievedAmount: json['achievedAmount'] as int,
      achievementRate: (json['achievementRate'] as num).toDouble(),
      categorySales: (json['categorySales'] as List<dynamic>)
          .map((item) => CategorySalesModel.fromJson(item as Map<String, dynamic>))
          .toList(),
      previousYearSameMonth: json['previousYearSameMonth'] as int,
      monthlyAverage: MonthlyAverageModel.fromJson(
        json['monthlyAverage'] as Map<String, dynamic>,
      ),
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
      'categorySales': categorySales.map((item) => item.toJson()).toList(),
      'previousYearSameMonth': previousYearSameMonth,
      'monthlyAverage': monthlyAverage.toJson(),
    };
  }

  MonthlySales toEntity() {
    return MonthlySales(
      customerId: customerId,
      customerName: customerName,
      yearMonth: yearMonth,
      targetAmount: targetAmount,
      achievedAmount: achievedAmount,
      achievementRate: achievementRate,
      categorySales: categorySales.map((model) => model.toEntity()).toList(),
      previousYearSameMonth: previousYearSameMonth,
      monthlyAverage: monthlyAverage.toEntity(),
    );
  }

  factory MonthlySalesModel.fromEntity(MonthlySales entity) {
    return MonthlySalesModel(
      customerId: entity.customerId,
      customerName: entity.customerName,
      yearMonth: entity.yearMonth,
      targetAmount: entity.targetAmount,
      achievedAmount: entity.achievedAmount,
      achievementRate: entity.achievementRate,
      categorySales: entity.categorySales
          .map((item) => CategorySalesModel.fromEntity(item))
          .toList(),
      previousYearSameMonth: entity.previousYearSameMonth,
      monthlyAverage: MonthlyAverageModel.fromEntity(entity.monthlyAverage),
    );
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    return other is MonthlySalesModel &&
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
    return 'MonthlySalesModel(customerId: $customerId, '
        'customerName: $customerName, yearMonth: $yearMonth, '
        'targetAmount: $targetAmount, achievedAmount: $achievedAmount, '
        'achievementRate: $achievementRate%, '
        'categorySales: ${categorySales.length} items, '
        'previousYearSameMonth: $previousYearSameMonth, '
        'monthlyAverage: $monthlyAverage)';
  }
}

/// List equality helper
bool _listEquals<T>(List<T> a, List<T> b) {
  if (a.length != b.length) return false;
  for (int i = 0; i < a.length; i++) {
    if (a[i] != b[i]) return false;
  }
  return true;
}
