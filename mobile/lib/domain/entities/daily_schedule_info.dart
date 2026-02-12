import 'schedule_store_detail.dart';

/// 보고 진행 상황
class ReportProgress {
  /// 보고 완료 건수
  final int completed;

  /// 전체 건수
  final int total;

  /// 근무 유형 (예: "진열")
  final String workType;

  const ReportProgress({
    required this.completed,
    required this.total,
    required this.workType,
  });

  ReportProgress copyWith({
    int? completed,
    int? total,
    String? workType,
  }) {
    return ReportProgress(
      completed: completed ?? this.completed,
      total: total ?? this.total,
      workType: workType ?? this.workType,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'completed': completed,
      'total': total,
      'workType': workType,
    };
  }

  factory ReportProgress.fromJson(Map<String, dynamic> json) {
    return ReportProgress(
      completed: json['completed'] as int,
      total: json['total'] as int,
      workType: json['workType'] as String,
    );
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    return other is ReportProgress &&
        other.completed == completed &&
        other.total == total &&
        other.workType == workType;
  }

  @override
  int get hashCode => Object.hash(completed, total, workType);

  @override
  String toString() {
    return 'ReportProgress(completed: $completed, total: $total, workType: $workType)';
  }
}

/// 일간 일정 상세 정보
class DailyScheduleInfo {
  /// 날짜 (포맷: "2020년 08월 04일(화)")
  final String date;

  /// 조원명
  final String memberName;

  /// 사원번호
  final String employeeNumber;

  /// 보고 진행 상황
  final ReportProgress reportProgress;

  /// 거래처 목록
  final List<ScheduleStoreDetail> stores;

  const DailyScheduleInfo({
    required this.date,
    required this.memberName,
    required this.employeeNumber,
    required this.reportProgress,
    required this.stores,
  });

  DailyScheduleInfo copyWith({
    String? date,
    String? memberName,
    String? employeeNumber,
    ReportProgress? reportProgress,
    List<ScheduleStoreDetail>? stores,
  }) {
    return DailyScheduleInfo(
      date: date ?? this.date,
      memberName: memberName ?? this.memberName,
      employeeNumber: employeeNumber ?? this.employeeNumber,
      reportProgress: reportProgress ?? this.reportProgress,
      stores: stores ?? this.stores,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'date': date,
      'memberName': memberName,
      'employeeNumber': employeeNumber,
      'reportProgress': reportProgress.toJson(),
      'stores': stores.map((store) => store.toJson()).toList(),
    };
  }

  factory DailyScheduleInfo.fromJson(Map<String, dynamic> json) {
    return DailyScheduleInfo(
      date: json['date'] as String,
      memberName: json['memberName'] as String,
      employeeNumber: json['employeeNumber'] as String,
      reportProgress: ReportProgress.fromJson(
        json['reportProgress'] as Map<String, dynamic>,
      ),
      stores: (json['stores'] as List<dynamic>)
          .map((store) => ScheduleStoreDetail.fromJson(
                store as Map<String, dynamic>,
              ))
          .toList(),
    );
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    return other is DailyScheduleInfo &&
        other.date == date &&
        other.memberName == memberName &&
        other.employeeNumber == employeeNumber &&
        other.reportProgress == reportProgress &&
        _listEquals(other.stores, stores);
  }

  bool _listEquals<T>(List<T> a, List<T> b) {
    if (a.length != b.length) return false;
    for (int i = 0; i < a.length; i++) {
      if (a[i] != b[i]) return false;
    }
    return true;
  }

  @override
  int get hashCode {
    return Object.hash(
      date,
      memberName,
      employeeNumber,
      reportProgress,
      Object.hashAll(stores),
    );
  }

  @override
  String toString() {
    return 'DailyScheduleInfo(date: $date, memberName: $memberName, '
        'employeeNumber: $employeeNumber, reportProgress: $reportProgress, '
        'stores: $stores)';
  }
}
