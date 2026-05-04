import '../../domain/entities/leader_schedule_created.dart';

/// `POST /api/v1/mobile/leader/team-member-schedule` 응답 모델.
class LeaderScheduleCreateResponseModel {
  final int scheduleId;
  final int targetEmployeeId;
  final String workingDate;
  final String workingType;
  final String workingCategory3;
  final int proxyRegisteredBy;

  const LeaderScheduleCreateResponseModel({
    required this.scheduleId,
    required this.targetEmployeeId,
    required this.workingDate,
    required this.workingType,
    required this.workingCategory3,
    required this.proxyRegisteredBy,
  });

  factory LeaderScheduleCreateResponseModel.fromJson(
      Map<String, dynamic> json) {
    return LeaderScheduleCreateResponseModel(
      scheduleId: json['scheduleId'] as int,
      targetEmployeeId: json['targetEmployeeId'] as int,
      workingDate: json['workingDate'] as String,
      workingType: json['workingType'] as String,
      workingCategory3: json['workingCategory3'] as String,
      proxyRegisteredBy: json['proxyRegisteredBy'] as int,
    );
  }

  LeaderScheduleCreated toEntity() => LeaderScheduleCreated(
        scheduleId: scheduleId,
        targetEmployeeId: targetEmployeeId,
        workingDate: DateTime.parse(workingDate),
        workingType: workingType,
        workingCategory3: workingCategory3,
        proxyRegisteredBy: proxyRegisteredBy,
      );
}
