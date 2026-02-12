/// 일정 거래처 상세 정보
class ScheduleStoreDetail {
  /// 거래처 ID
  final int storeId;

  /// 거래처명
  final String storeName;

  /// 근무 유형 1 (예: "진열")
  final String workType1;

  /// 근무 유형 2 (예: "전담")
  final String workType2;

  /// 근무 유형 3 (예: "순회", "격고", "고정")
  final String workType3;

  /// 등록 완료 여부
  final bool isRegistered;

  const ScheduleStoreDetail({
    required this.storeId,
    required this.storeName,
    required this.workType1,
    required this.workType2,
    required this.workType3,
    required this.isRegistered,
  });

  ScheduleStoreDetail copyWith({
    int? storeId,
    String? storeName,
    String? workType1,
    String? workType2,
    String? workType3,
    bool? isRegistered,
  }) {
    return ScheduleStoreDetail(
      storeId: storeId ?? this.storeId,
      storeName: storeName ?? this.storeName,
      workType1: workType1 ?? this.workType1,
      workType2: workType2 ?? this.workType2,
      workType3: workType3 ?? this.workType3,
      isRegistered: isRegistered ?? this.isRegistered,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'storeId': storeId,
      'storeName': storeName,
      'workType1': workType1,
      'workType2': workType2,
      'workType3': workType3,
      'isRegistered': isRegistered,
    };
  }

  factory ScheduleStoreDetail.fromJson(Map<String, dynamic> json) {
    return ScheduleStoreDetail(
      storeId: json['storeId'] as int,
      storeName: json['storeName'] as String,
      workType1: json['workType1'] as String,
      workType2: json['workType2'] as String,
      workType3: json['workType3'] as String,
      isRegistered: json['isRegistered'] as bool,
    );
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    return other is ScheduleStoreDetail &&
        other.storeId == storeId &&
        other.storeName == storeName &&
        other.workType1 == workType1 &&
        other.workType2 == workType2 &&
        other.workType3 == workType3 &&
        other.isRegistered == isRegistered;
  }

  @override
  int get hashCode {
    return Object.hash(
      storeId,
      storeName,
      workType1,
      workType2,
      workType3,
      isRegistered,
    );
  }

  @override
  String toString() {
    return 'ScheduleStoreDetail(storeId: $storeId, storeName: $storeName, '
        'workType1: $workType1, workType2: $workType2, workType3: $workType3, '
        'isRegistered: $isRegistered)';
  }
}
