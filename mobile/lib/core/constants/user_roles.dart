/// 모바일 도메인 권한 상수 + 권한 판정.
///
/// 백엔드가 내려주는 SF picklist (`여사원` / `조장` / `지점장` / `AccountViewAll`) 는
/// `UserModel._toDomainRole` 에서 도메인 어휘로 번역된다.
///   - `여사원` / `AccountViewAll` / 미지정 → [user]
///   - `조장` → [leader]
///   - `지점장` → [branchManager]
///
/// `AccountViewAll`(부서장) 은 [user] 로 뭉개지므로, 부서장 판별이 필요한 곳은
/// 도메인 role 이 아니라 `User.rawRole == 'AccountViewAll'` 을 본다.
abstract final class UserRoles {
  /// 여사원 (+ AccountViewAll / 미지정 폴백)
  static const String user = 'USER';

  /// 조장
  static const String leader = 'LEADER';

  /// 지점장
  static const String branchManager = 'ADMIN';

  /// 팀(여사원) 관리 권한 — 조장 + 지점장.
  ///
  /// 여사원 관리 메뉴 / 팀원 월간일정 / 조장형 거래처·행사 필터가 이 판정을 공유한다.
  /// 레거시 GNB 는 `eq '조장'` 정확 일치였으나, 지점장도 동일하게 팀을 관리하도록
  /// 의도적으로 확장한 지점이다 (레거시 이탈).
  ///
  /// **출근등록 / 안전점검 특수처리에는 쓰지 않는다.** 조장만 안전점검 없이 바로
  /// 출근등록하는 분기(`home_page._handleRegisterTap`) 와 조장 팀 출근집계 뷰
  /// (`ScheduleCard._isLeaderView`) 는 [leader] 정확 일치를 유지한다. 지점장은
  /// 서버 `HomeService.attendanceApplicable == false` 로 출근 카드 자체가 비노출이다.
  static bool canManageTeam(String? role) =>
      role == leader || role == branchManager;
}
