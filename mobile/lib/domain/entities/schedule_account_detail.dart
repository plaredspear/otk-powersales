/// 일정 거래처 상세 정보
class ScheduleAccountDetail {
  /// 거래처 ID
  final int accountId;

  /// 거래처명
  final String accountName;

  /// 근무 유형 1 (예: "진열")
  final String workType1;

  /// 근무 유형 2 (예: "전담")
  final String workType2;

  /// 근무 유형 3 (예: "순회", "격고", "고정")
  final String workType3;

  /// 등록 완료 여부
  final bool isRegistered;

  /// 레거시 화면에 노출되던 항목인지 여부.
  ///
  /// false 면 "출근등록 전이면서 진열·행사를 동시에 보유한 날" 에 레거시가 화면에서
  /// 버리던 항목이다. 방문 매장 누락을 막기 위해 함께 표시하되 부차 항목으로 구분하고,
  /// 상단 보고완료 카운터 집계에서는 제외한다 (백엔드 isLegacyVisible).
  final bool isLegacyVisible;

  const ScheduleAccountDetail({
    required this.accountId,
    required this.accountName,
    required this.workType1,
    required this.workType2,
    required this.workType3,
    required this.isRegistered,
    this.isLegacyVisible = true,
  });

  ScheduleAccountDetail copyWith({
    int? accountId,
    String? accountName,
    String? workType1,
    String? workType2,
    String? workType3,
    bool? isRegistered,
    bool? isLegacyVisible,
  }) {
    return ScheduleAccountDetail(
      accountId: accountId ?? this.accountId,
      accountName: accountName ?? this.accountName,
      workType1: workType1 ?? this.workType1,
      workType2: workType2 ?? this.workType2,
      workType3: workType3 ?? this.workType3,
      isRegistered: isRegistered ?? this.isRegistered,
      isLegacyVisible: isLegacyVisible ?? this.isLegacyVisible,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'accountId': accountId,
      'accountName': accountName,
      'workType1': workType1,
      'workType2': workType2,
      'workType3': workType3,
      'isRegistered': isRegistered,
      'isLegacyVisible': isLegacyVisible,
    };
  }

  factory ScheduleAccountDetail.fromJson(Map<String, dynamic> json) {
    return ScheduleAccountDetail(
      accountId: json['accountId'] as int,
      accountName: json['accountName'] as String,
      workType1: json['workType1'] as String,
      workType2: json['workType2'] as String,
      workType3: json['workType3'] as String,
      isRegistered: json['isRegistered'] as bool,
      isLegacyVisible: json['isLegacyVisible'] as bool? ?? true,
    );
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    return other is ScheduleAccountDetail &&
        other.accountId == accountId &&
        other.accountName == accountName &&
        other.workType1 == workType1 &&
        other.workType2 == workType2 &&
        other.workType3 == workType3 &&
        other.isRegistered == isRegistered &&
        other.isLegacyVisible == isLegacyVisible;
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
      isLegacyVisible,
    );
  }

  @override
  String toString() {
    return 'ScheduleAccountDetail(accountId: $accountId, accountName: $accountName, '
        'workType1: $workType1, workType2: $workType2, workType3: $workType3, '
        'isRegistered: $isRegistered, isLegacyVisible: $isLegacyVisible)';
  }
}
