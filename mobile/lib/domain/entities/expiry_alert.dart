/// 소비기한 알림 엔티티
///
/// 홈화면에 표시되는 소비기한 임박 제품 알림 정보를 나타낸다.
class ExpiryAlert {
  final String branchName;
  final String employeeName;
  final String employeeCode;
  final int expiryCount;

  const ExpiryAlert({
    required this.branchName,
    required this.employeeName,
    required this.employeeCode,
    required this.expiryCount,
  });

  ExpiryAlert copyWith({
    String? branchName,
    String? employeeName,
    String? employeeCode,
    int? expiryCount,
  }) {
    return ExpiryAlert(
      branchName: branchName ?? this.branchName,
      employeeName: employeeName ?? this.employeeName,
      employeeCode: employeeCode ?? this.employeeCode,
      expiryCount: expiryCount ?? this.expiryCount,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'branchName': branchName,
      'employeeName': employeeName,
      'employeeCode': employeeCode,
      'expiryCount': expiryCount,
    };
  }

  factory ExpiryAlert.fromJson(Map<String, dynamic> json) {
    return ExpiryAlert(
      branchName: json['branchName'] as String,
      employeeName: json['employeeName'] as String,
      employeeCode: json['employeeCode'] as String,
      expiryCount: json['expiryCount'] as int,
    );
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    return other is ExpiryAlert &&
        other.branchName == branchName &&
        other.employeeName == employeeName &&
        other.employeeCode == employeeCode &&
        other.expiryCount == expiryCount;
  }

  @override
  int get hashCode {
    return Object.hash(
      branchName,
      employeeName,
      employeeCode,
      expiryCount,
    );
  }

  @override
  String toString() {
    return 'ExpiryAlert(branchName: $branchName, employeeName: $employeeName, employeeCode: $employeeCode, expiryCount: $expiryCount)';
  }
}
