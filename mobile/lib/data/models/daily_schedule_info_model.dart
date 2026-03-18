import '../../domain/entities/daily_schedule_info.dart';
import '../../domain/entities/schedule_account_detail.dart';

/// 보고 진행 상황 모델 (DTO)
class ReportProgressModel {
  final int completed;
  final int total;
  final String workType;

  const ReportProgressModel({
    required this.completed,
    required this.total,
    required this.workType,
  });

  factory ReportProgressModel.fromJson(Map<String, dynamic> json) {
    return ReportProgressModel(
      completed: json['completed'] as int,
      total: json['total'] as int,
      workType: json['work_type'] as String,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'completed': completed,
      'total': total,
      'work_type': workType,
    };
  }

  ReportProgress toEntity() {
    return ReportProgress(
      completed: completed,
      total: total,
      workType: workType,
    );
  }

  factory ReportProgressModel.fromEntity(ReportProgress entity) {
    return ReportProgressModel(
      completed: entity.completed,
      total: entity.total,
      workType: entity.workType,
    );
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    return other is ReportProgressModel &&
        other.completed == completed &&
        other.total == total &&
        other.workType == workType;
  }

  @override
  int get hashCode => Object.hash(completed, total, workType);
}

/// 일정 거래처 상세 모델 (DTO)
class ScheduleAccountDetailModel {
  final int accountId;
  final String accountName;
  final String workType1;
  final String workType2;
  final String workType3;
  final bool isRegistered;

  const ScheduleAccountDetailModel({
    required this.accountId,
    required this.accountName,
    required this.workType1,
    required this.workType2,
    required this.workType3,
    required this.isRegistered,
  });

  factory ScheduleAccountDetailModel.fromJson(Map<String, dynamic> json) {
    return ScheduleAccountDetailModel(
      accountId: json['account_id'] as int,
      accountName: json['account_name'] as String,
      workType1: json['work_type_1'] as String,
      workType2: json['work_type_2'] as String,
      workType3: json['work_type_3'] as String,
      isRegistered: json['is_registered'] as bool,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'account_id': accountId,
      'account_name': accountName,
      'work_type_1': workType1,
      'work_type_2': workType2,
      'work_type_3': workType3,
      'is_registered': isRegistered,
    };
  }

  ScheduleAccountDetail toEntity() {
    return ScheduleAccountDetail(
      accountId: accountId,
      accountName: accountName,
      workType1: workType1,
      workType2: workType2,
      workType3: workType3,
      isRegistered: isRegistered,
    );
  }

  factory ScheduleAccountDetailModel.fromEntity(ScheduleAccountDetail entity) {
    return ScheduleAccountDetailModel(
      accountId: entity.accountId,
      accountName: entity.accountName,
      workType1: entity.workType1,
      workType2: entity.workType2,
      workType3: entity.workType3,
      isRegistered: entity.isRegistered,
    );
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    return other is ScheduleAccountDetailModel &&
        other.accountId == accountId &&
        other.accountName == accountName &&
        other.workType1 == workType1 &&
        other.workType2 == workType2 &&
        other.workType3 == workType3 &&
        other.isRegistered == isRegistered;
  }

  @override
  int get hashCode {
    return Object.hash(
      accountId,
      accountName,
      workType1,
      workType2,
      workType3,
      isRegistered,
    );
  }
}

/// 일간 일정 상세 정보 모델 (DTO)
///
/// Backend API의 snake_case JSON을 파싱하여 DailyScheduleInfo 엔티티로 변환합니다.
class DailyScheduleInfoModel {
  final String date;
  final String dayOfWeek;
  final String memberName;
  final String employeeNumber;
  final ReportProgressModel reportProgress;
  final List<ScheduleAccountDetailModel> accounts;

  const DailyScheduleInfoModel({
    required this.date,
    required this.dayOfWeek,
    required this.memberName,
    required this.employeeNumber,
    required this.reportProgress,
    required this.accounts,
  });

  /// snake_case JSON에서 파싱
  factory DailyScheduleInfoModel.fromJson(Map<String, dynamic> json) {
    return DailyScheduleInfoModel(
      date: json['date'] as String,
      dayOfWeek: json['day_of_week'] as String,
      memberName: json['member_name'] as String,
      employeeNumber: json['employee_number'] as String,
      reportProgress: ReportProgressModel.fromJson(
        json['report_progress'] as Map<String, dynamic>,
      ),
      accounts: (json['accounts'] as List<dynamic>)
          .map((account) => ScheduleAccountDetailModel.fromJson(
                account as Map<String, dynamic>,
              ))
          .toList(),
    );
  }

  /// snake_case JSON으로 직렬화
  Map<String, dynamic> toJson() {
    return {
      'date': date,
      'day_of_week': dayOfWeek,
      'member_name': memberName,
      'employee_number': employeeNumber,
      'report_progress': reportProgress.toJson(),
      'accounts': accounts.map((account) => account.toJson()).toList(),
    };
  }

  /// Domain Entity로 변환
  ///
  /// date와 dayOfWeek를 결합하여 포맷된 날짜 문자열을 생성합니다.
  /// 예: "2020-08-04" + "화" → "2020년 08월 04일(화)"
  DailyScheduleInfo toEntity() {
    // date는 "2020-08-04" 형식
    final dateParts = date.split('-');
    final formattedDate =
        '${dateParts[0]}년 ${dateParts[1]}월 ${dateParts[2]}일($dayOfWeek)';

    return DailyScheduleInfo(
      date: formattedDate,
      memberName: memberName,
      employeeNumber: employeeNumber,
      reportProgress: reportProgress.toEntity(),
      accounts: accounts.map((account) => account.toEntity()).toList(),
    );
  }

  /// Domain Entity에서 생성
  factory DailyScheduleInfoModel.fromEntity(DailyScheduleInfo entity) {
    // entity.date는 "2020년 08월 04일(화)" 형식
    // 파싱하여 date와 dayOfWeek로 분리
    final regex = RegExp(r'(\d{4})년 (\d{2})월 (\d{2})일\((.)\)');
    final match = regex.firstMatch(entity.date);

    if (match == null) {
      throw ArgumentError('Invalid date format: ${entity.date}');
    }

    final date = '${match.group(1)}-${match.group(2)}-${match.group(3)}';
    final dayOfWeek = match.group(4)!;

    return DailyScheduleInfoModel(
      date: date,
      dayOfWeek: dayOfWeek,
      memberName: entity.memberName,
      employeeNumber: entity.employeeNumber,
      reportProgress: ReportProgressModel.fromEntity(entity.reportProgress),
      accounts: entity.accounts
          .map((account) => ScheduleAccountDetailModel.fromEntity(account))
          .toList(),
    );
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    return other is DailyScheduleInfoModel &&
        other.date == date &&
        other.dayOfWeek == dayOfWeek &&
        other.memberName == memberName &&
        other.employeeNumber == employeeNumber &&
        other.reportProgress == reportProgress &&
        _listEquals(other.accounts, accounts);
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
      dayOfWeek,
      memberName,
      employeeNumber,
      reportProgress,
      Object.hashAll(accounts),
    );
  }

  @override
  String toString() {
    return 'DailyScheduleInfoModel(date: $date, dayOfWeek: $dayOfWeek, '
        'memberName: $memberName, employeeNumber: $employeeNumber, '
        'reportProgress: $reportProgress, accounts: $accounts)';
  }
}
