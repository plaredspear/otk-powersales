/// 유통기한 알림 엔티티
///
/// 홈화면에 표시되는 유통기한 임박 제품 알림 정보를 나타낸다.
class ExpiryAlert {
  final String branchName;
  final String employeeName;
  final String employeeId;
  final int expiryCount;

  const ExpiryAlert({
    required this.branchName,
    required this.employeeName,
    required this.employeeId,
    required this.expiryCount,
  });

  ExpiryAlert copyWith({
    String? branchName,
    String? employeeName,
    String? employeeId,
    int? expiryCount,
  }) {
    return ExpiryAlert(
      branchName: branchName ?? this.branchName,
      employeeName: employeeName ?? this.employeeName,
      employeeId: employeeId ?? this.employeeId,
      expiryCount: expiryCount ?? this.expiryCount,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'branchName': branchName,
      'employeeName': employeeName,
      'employeeId': employeeId,
      'expiryCount': expiryCount,
    };
  }

  factory ExpiryAlert.fromJson(Map<String, dynamic> json) {
    return ExpiryAlert(
      branchName: json['branchName'] as String,
      employeeName: json['employeeName'] as String,
      employeeId: json['employeeId'] as String,
      expiryCount: json['expiryCount'] as int,
    );
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    return other is ExpiryAlert &&
        other.branchName == branchName &&
        other.employeeName == employeeName &&
        other.employeeId == employeeId &&
        other.expiryCount == expiryCount;
  }

  @override
  int get hashCode {
    return Object.hash(
      branchName,
      employeeName,
      employeeId,
      expiryCount,
    );
  }

  @override
  String toString() {
    return 'ExpiryAlert(branchName: $branchName, employeeName: $employeeName, employeeId: $employeeId, expiryCount: $expiryCount)';
  }
}
