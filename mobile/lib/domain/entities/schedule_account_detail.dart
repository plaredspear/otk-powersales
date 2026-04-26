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

  const ScheduleAccountDetail({
    required this.accountId,
    required this.accountName,
    required this.workType1,
    required this.workType2,
    required this.workType3,
    required this.isRegistered,
  });

  ScheduleAccountDetail copyWith({
    int? accountId,
    String? accountName,
    String? workType1,
    String? workType2,
    String? workType3,
    bool? isRegistered,
  }) {
    return ScheduleAccountDetail(
      accountId: accountId ?? this.accountId,
      accountName: accountName ?? this.accountName,
      workType1: workType1 ?? this.workType1,
      workType2: workType2 ?? this.workType2,
      workType3: workType3 ?? this.workType3,
      isRegistered: isRegistered ?? this.isRegistered,
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

  @override
  String toString() {
    return 'ScheduleAccountDetail(accountId: $accountId, accountName: $accountName, '
        'workType1: $workType1, workType2: $workType2, workType3: $workType3, '
        'isRegistered: $isRegistered)';
  }
}
