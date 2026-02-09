import '../../domain/entities/target.dart';
import '../../domain/entities/progress.dart';

/// 목표 조회 필터
///
/// 월별, 카테고리별, 거래처별 필터 조건
class TargetFilter {
  /// 조회 년월 (예: '202601')
  final String yearMonth;

  /// 카테고리 필터 (선택적)
  final String? category;

  /// 거래처 코드 필터 (선택적)
  final String? customerCode;

  /// 진도율 부족 필터 (true: 진도율 부족만, false: 전체)
  final bool onlyInsufficient;

  /// 진도율 임계값 (기본: 100.0%)
  final double thresholdPercentage;

  const TargetFilter({
    required this.yearMonth,
    this.category,
    this.customerCode,
    this.onlyInsufficient = false,
    this.thresholdPercentage = 100.0,
  });

  /// 필터 복사
  TargetFilter copyWith({
    String? yearMonth,
    String? category,
    String? customerCode,
    bool? onlyInsufficient,
    double? thresholdPercentage,
  }) {
    return TargetFilter(
      yearMonth: yearMonth ?? this.yearMonth,
      category: category ?? this.category,
      customerCode: customerCode ?? this.customerCode,
      onlyInsufficient: onlyInsufficient ?? this.onlyInsufficient,
      thresholdPercentage: thresholdPercentage ?? this.thresholdPercentage,
    );
  }

  /// 필터 초기화
  TargetFilter clear() {
    return TargetFilter(
      yearMonth: yearMonth,
      category: null,
      customerCode: null,
      onlyInsufficient: false,
      thresholdPercentage: 100.0,
    );
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;

    return other is TargetFilter &&
        other.yearMonth == yearMonth &&
        other.category == category &&
        other.customerCode == customerCode &&
        other.onlyInsufficient == onlyInsufficient &&
        other.thresholdPercentage == thresholdPercentage;
  }

  @override
  int get hashCode {
    return Object.hash(
      yearMonth,
      category,
      customerCode,
      onlyInsufficient,
      thresholdPercentage,
    );
  }
}

/// 목표/진도율 상태
///
/// 목표 목록, 진도율 목록, 통계 데이터를 관리
class TargetState {
  /// 목표 목록
  final List<Target> targets;

  /// 진도율 목록 (목표 ID -> 진도율)
  final Map<String, Progress> progressList;

  /// 현재 필터 조건
  final TargetFilter filter;

  /// 로딩 상태
  final bool isLoading;

  /// 에러 메시지
  final String? errorMessage;

  /// 총 목표 금액
  final int totalTargetAmount;

  /// 총 실적 금액
  final int totalActualAmount;

  /// 전체 진도율
  final Progress? overallProgress;

  const TargetState({
    this.targets = const [],
    this.progressList = const {},
    required this.filter,
    this.isLoading = false,
    this.errorMessage,
    this.totalTargetAmount = 0,
    this.totalActualAmount = 0,
    this.overallProgress,
  });

  /// 초기 상태
  factory TargetState.initial(String yearMonth) {
    return TargetState(
      filter: TargetFilter(yearMonth: yearMonth),
    );
  }

  /// 로딩 상태로 전환
  TargetState toLoading() {
    return copyWith(
      isLoading: true,
      errorMessage: null,
    );
  }

  /// 에러 상태로 전환
  TargetState toError(String message) {
    return copyWith(
      isLoading: false,
      errorMessage: message,
    );
  }

  /// 데이터 상태로 전환
  TargetState toData({
    required List<Target> targets,
    required Map<String, Progress> progressList,
    required int totalTargetAmount,
    required int totalActualAmount,
    required Progress overallProgress,
  }) {
    return copyWith(
      targets: targets,
      progressList: progressList,
      totalTargetAmount: totalTargetAmount,
      totalActualAmount: totalActualAmount,
      overallProgress: overallProgress,
      isLoading: false,
      errorMessage: null,
    );
  }

  /// 상태 복사
  TargetState copyWith({
    List<Target>? targets,
    Map<String, Progress>? progressList,
    TargetFilter? filter,
    bool? isLoading,
    String? errorMessage,
    int? totalTargetAmount,
    int? totalActualAmount,
    Progress? overallProgress,
  }) {
    return TargetState(
      targets: targets ?? this.targets,
      progressList: progressList ?? this.progressList,
      filter: filter ?? this.filter,
      isLoading: isLoading ?? this.isLoading,
      errorMessage: errorMessage,
      totalTargetAmount: totalTargetAmount ?? this.totalTargetAmount,
      totalActualAmount: totalActualAmount ?? this.totalActualAmount,
      overallProgress: overallProgress ?? this.overallProgress,
    );
  }

  /// 진도율 부족 목표 개수
  int get insufficientCount {
    return progressList.values
        .where((p) => p.status == ProgressStatus.insufficient)
        .length;
  }

  /// 진도율 초과 목표 개수
  int get exceededCount {
    return progressList.values
        .where((p) => p.status == ProgressStatus.exceeded)
        .length;
  }

  /// 목표 달성 개수
  int get achievedCount {
    return progressList.values
        .where((p) => p.status == ProgressStatus.achieved)
        .length;
  }

  /// 필터링된 목표 목록
  List<Target> get filteredTargets {
    var result = targets;

    // 카테고리 필터
    if (filter.category != null) {
      result = result.where((t) => t.category == filter.category).toList();
    }

    // 거래처 필터
    if (filter.customerCode != null) {
      result = result
          .where((t) => t.customerCode == filter.customerCode)
          .toList();
    }

    // 진도율 부족 필터
    if (filter.onlyInsufficient) {
      result = result.where((t) {
        final progress = progressList[t.id];
        return progress != null &&
            progress.percentage < filter.thresholdPercentage;
      }).toList();
    }

    return result;
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;

    return other is TargetState &&
        other.targets == targets &&
        other.progressList == progressList &&
        other.filter == filter &&
        other.isLoading == isLoading &&
        other.errorMessage == errorMessage &&
        other.totalTargetAmount == totalTargetAmount &&
        other.totalActualAmount == totalActualAmount &&
        other.overallProgress == overallProgress;
  }

  @override
  int get hashCode {
    return Object.hash(
      targets,
      progressList,
      filter,
      isLoading,
      errorMessage,
      totalTargetAmount,
      totalActualAmount,
      overallProgress,
    );
  }
}
