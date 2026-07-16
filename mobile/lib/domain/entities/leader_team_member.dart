/// 조장의 본인 팀원 정보.
class LeaderTeamMember {
  final int id;
  final String employeeCode;
  final String name;
  final String? status;
  final String? costCenterCode;

  /// 전화번호(SF Phone__c). null/공백이면 명단 화면에서 전화 버튼 미노출.
  final String? phone;

  /// 단말 바인딩 여부(서버 deviceUuid != null). 상세 화면 단말 초기화 버튼 상태 표시용.
  final bool deviceBound;

  /// 앱 로그인 활성 여부(서버 appLoginActive). false면 초기화 불가(버튼 비활성).
  final bool loginActive;

  const LeaderTeamMember({
    required this.id,
    required this.employeeCode,
    required this.name,
    required this.status,
    required this.costCenterCode,
    this.phone,
    this.deviceBound = false,
    this.loginActive = false,
  });

  /// 휴직/퇴직 여부 — 화면에서 등록 버튼 비활성 처리에 사용.
  bool get isInactive => status == '휴직' || status == '퇴직';

  /// 전화 버튼 노출 여부 — 레거시 `isEmpty(phone__c) != ''` 정합.
  bool get hasPhone => phone != null && phone!.trim().isNotEmpty;

  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      other is LeaderTeamMember &&
          id == other.id &&
          employeeCode == other.employeeCode;

  @override
  int get hashCode => id.hashCode ^ employeeCode.hashCode;
}
