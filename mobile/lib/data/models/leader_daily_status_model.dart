import '../../domain/entities/leader_daily_status.dart';

/// `GET /api/v1/mobile/leader/daily-status` 응답 모델.
class LeaderDailyStatusModel {
  final String date;
  final LeaderDailyStatusSummaryModel summary;
  final List<LeaderDailyWorkerModel> displayWorkers;
  final List<LeaderDailyWorkerModel> eventWorkers;
  final List<LeaderDailyEmployeeModel> annualLeaveWorkers;

  const LeaderDailyStatusModel({
    required this.date,
    required this.summary,
    required this.displayWorkers,
    required this.eventWorkers,
    required this.annualLeaveWorkers,
  });

  factory LeaderDailyStatusModel.fromJson(Map<String, dynamic> json) {
    return LeaderDailyStatusModel(
      date: json['date'] as String? ?? '',
      summary: LeaderDailyStatusSummaryModel.fromJson(
        (json['summary'] as Map<String, dynamic>?) ?? const {},
      ),
      displayWorkers: ((json['displayWorkers'] as List<dynamic>?) ?? const [])
          .map((e) => LeaderDailyWorkerModel.fromJson(e as Map<String, dynamic>))
          .toList(),
      eventWorkers: ((json['eventWorkers'] as List<dynamic>?) ?? const [])
          .map((e) => LeaderDailyWorkerModel.fromJson(e as Map<String, dynamic>))
          .toList(),
      annualLeaveWorkers:
          ((json['annualLeaveWorkers'] as List<dynamic>?) ?? const [])
              .map((e) =>
                  LeaderDailyEmployeeModel.fromJson(e as Map<String, dynamic>))
              .toList(),
    );
  }

  LeaderDailyStatus toEntity() => LeaderDailyStatus(
        date: DateTime.tryParse(date) ?? DateTime.fromMillisecondsSinceEpoch(0),
        summary: summary.toEntity(),
        displayWorkers: displayWorkers.map((e) => e.toEntity()).toList(),
        eventWorkers: eventWorkers.map((e) => e.toEntity()).toList(),
        annualLeaveWorkers:
            annualLeaveWorkers.map((e) => e.toEntity()).toList(),
      );
}

class LeaderDailyStatusSummaryModel {
  final int displayTotal;
  final int displayAttended;
  final int eventTotal;
  final int eventAttended;
  final int annualLeaveCount;

  const LeaderDailyStatusSummaryModel({
    required this.displayTotal,
    required this.displayAttended,
    required this.eventTotal,
    required this.eventAttended,
    required this.annualLeaveCount,
  });

  factory LeaderDailyStatusSummaryModel.fromJson(Map<String, dynamic> json) {
    return LeaderDailyStatusSummaryModel(
      displayTotal: json['displayTotal'] as int? ?? 0,
      displayAttended: json['displayAttended'] as int? ?? 0,
      eventTotal: json['eventTotal'] as int? ?? 0,
      eventAttended: json['eventAttended'] as int? ?? 0,
      annualLeaveCount: json['annualLeaveCount'] as int? ?? 0,
    );
  }

  LeaderDailyStatusSummary toEntity() => LeaderDailyStatusSummary(
        displayTotal: displayTotal,
        displayAttended: displayAttended,
        eventTotal: eventTotal,
        eventAttended: eventAttended,
        annualLeaveCount: annualLeaveCount,
      );
}

class LeaderDailyWorkerModel {
  final int scheduleId;
  final int? displayWorkScheduleId;
  final int? employeeId;
  final String employeeName;
  final String employeeCode;
  final String accountName;
  final String accountCode;
  final String? workingCategory1;
  final String? workingCategory2;
  final String? workingCategory3;
  final bool attended;

  const LeaderDailyWorkerModel({
    required this.scheduleId,
    this.displayWorkScheduleId,
    this.employeeId,
    required this.employeeName,
    required this.employeeCode,
    required this.accountName,
    required this.accountCode,
    this.workingCategory1,
    this.workingCategory2,
    this.workingCategory3,
    required this.attended,
  });

  factory LeaderDailyWorkerModel.fromJson(Map<String, dynamic> json) {
    return LeaderDailyWorkerModel(
      scheduleId: json['scheduleId'] as int,
      displayWorkScheduleId: json['displayWorkScheduleId'] as int?,
      employeeId: json['employeeId'] as int?,
      employeeName: json['employeeName'] as String? ?? '',
      employeeCode: json['employeeCode'] as String? ?? '',
      accountName: json['accountName'] as String? ?? '',
      accountCode: json['accountCode'] as String? ?? '',
      workingCategory1: json['workingCategory1'] as String?,
      workingCategory2: json['workingCategory2'] as String?,
      workingCategory3: json['workingCategory3'] as String?,
      attended: json['attended'] as bool? ?? false,
    );
  }

  LeaderDailyWorker toEntity() => LeaderDailyWorker(
        scheduleId: scheduleId,
        displayWorkScheduleId: displayWorkScheduleId,
        employeeId: employeeId,
        employeeName: employeeName,
        employeeCode: employeeCode,
        accountName: accountName,
        accountCode: accountCode,
        workingCategory1: workingCategory1,
        workingCategory2: workingCategory2,
        workingCategory3: workingCategory3,
        attended: attended,
      );
}

class LeaderDailyEmployeeModel {
  final int? employeeId;
  final String employeeName;
  final String employeeCode;

  const LeaderDailyEmployeeModel({
    this.employeeId,
    required this.employeeName,
    required this.employeeCode,
  });

  factory LeaderDailyEmployeeModel.fromJson(Map<String, dynamic> json) {
    return LeaderDailyEmployeeModel(
      employeeId: json['employeeId'] as int?,
      employeeName: json['employeeName'] as String? ?? '',
      employeeCode: json['employeeCode'] as String? ?? '',
    );
  }

  LeaderDailyEmployee toEntity() => LeaderDailyEmployee(
        employeeId: employeeId,
        employeeName: employeeName,
        employeeCode: employeeCode,
      );
}
