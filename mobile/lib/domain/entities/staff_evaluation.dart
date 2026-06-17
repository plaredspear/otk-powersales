/// 거래처별 평가 행 엔티티 (여사원 평가조회 - 본부평가)
///
/// 레거시 `mypage/evaluationList.jsp` 본부평가 테이블의 한 행에 대응한다.
class AccountEvaluation {
  /// 거래처 SAP 코드
  final String accountCode;

  /// 거래처명
  final String accountName;

  /// 거래처유형 (없을 수 있음)
  final String? accountType;

  /// 목표 금액 (원)
  final int targetAmount;

  /// 실적 금액 (원)
  final int performanceAmount;

  /// 달성률 (%)
  final double attainmentRate;

  const AccountEvaluation({
    required this.accountCode,
    required this.accountName,
    required this.accountType,
    required this.targetAmount,
    required this.performanceAmount,
    required this.attainmentRate,
  });

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    return other is AccountEvaluation &&
        other.accountCode == accountCode &&
        other.accountName == accountName &&
        other.accountType == accountType &&
        other.targetAmount == targetAmount &&
        other.performanceAmount == performanceAmount &&
        other.attainmentRate == attainmentRate;
  }

  @override
  int get hashCode => Object.hash(accountCode, accountName, accountType,
      targetAmount, performanceAmount, attainmentRate);
}

/// 여사원 평가조회 엔티티
///
/// 레거시 `GET /employee/evaluationList` 응답 — 지점평가 점수 + 담당 거래처별 평가.
class StaffEvaluation {
  /// 조회 연월 (YYYYMM)
  final String yearMonth;

  /// 지점평가 점수 (평가 미생성 월은 null)
  final double? branchScore;

  /// 지점평가 만점 (레거시 30)
  final int branchMaxScore;

  /// 담당 거래처별 평가 행
  final List<AccountEvaluation> accounts;

  const StaffEvaluation({
    required this.yearMonth,
    required this.branchScore,
    required this.branchMaxScore,
    required this.accounts,
  });

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    return other is StaffEvaluation &&
        other.yearMonth == yearMonth &&
        other.branchScore == branchScore &&
        other.branchMaxScore == branchMaxScore &&
        _listEquals(other.accounts, accounts);
  }

  @override
  int get hashCode => Object.hash(
      yearMonth, branchScore, branchMaxScore, Object.hashAll(accounts));

  bool _listEquals<T>(List<T> a, List<T> b) {
    if (a.length != b.length) return false;
    for (var i = 0; i < a.length; i++) {
      if (a[i] != b[i]) return false;
    }
    return true;
  }
}
