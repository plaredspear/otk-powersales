/// `POST /api/v1/mobile/leader/team-member-schedule` 요청 본문 모델.
///
/// `working_type='근무'`, `working_category2='전담'` 은 백엔드 정책 보존을 위해
/// 본 클래스에서 자동 채움 (스펙 §1.4 / P1-B §3.4 의 INVALID_*).
class LeaderScheduleCreateRequestModel {
  final int targetEmployeeId;
  final String workingDate;
  final String workingType;
  final String workingCategory2;
  final String workingCategory3;
  final int accountId;
  final String? workingCategory1;

  const LeaderScheduleCreateRequestModel({
    required this.targetEmployeeId,
    required this.workingDate,
    required this.workingType,
    required this.workingCategory2,
    required this.workingCategory3,
    required this.accountId,
    this.workingCategory1,
  });

  Map<String, dynamic> toJson() => {
        'targetEmployeeId': targetEmployeeId,
        'workingDate': workingDate,
        'workingType': workingType,
        'workingCategory2': workingCategory2,
        'workingCategory3': workingCategory3,
        'accountId': accountId,
        if (workingCategory1 != null) 'workingCategory1': workingCategory1,
      };
}
