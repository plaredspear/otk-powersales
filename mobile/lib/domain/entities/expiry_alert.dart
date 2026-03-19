/// 유통기한 알림 엔티티
///
/// 홈화면에 표시되는 유통기한 임박 제품 알림 정보를 나타낸다.
class ExpiryAlert {
  final String branchName;
  final String employeeName;
  final String employeeNumber;
  final int expiryCount;

  const ExpiryAlert({
    required this.branchName,
    required this.employeeName,
    required this.employeeNumber,
    required this.expiryCount,
  });

  ExpiryAlert copyWith({
    String? branchName,
    String? employeeName,
    String? employeeNumber,
    int? expiryCount,
  }) {
    return ExpiryAlert(
      branchName: branchName ?? this.branchName,
      employeeName: employeeName ?? this.employeeName,
      employeeNumber: employeeNumber ?? this.employeeNumber,
      expiryCount: expiryCount ?? this.expiryCount,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'branchName': branchName,
      'employeeName': employeeName,
      'employeeNumber': employeeNumber,
      'expiryCount': expiryCount,
    };
  }

  factory ExpiryAlert.fromJson(Map<String, dynamic> json) {
    return ExpiryAlert(
      branchName: json['branchName'] as String,
      employeeName: json['employeeName'] as String,
      employeeNumber: json['employeeNumber'] as String,
      expiryCount: json['expiryCount'] as int,
    );
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    return other is ExpiryAlert &&
        other.branchName == branchName &&
        other.employeeName == employeeName &&
        other.employeeNumber == employeeNumber &&
        other.expiryCount == expiryCount;
  }

  @override
  int get hashCode {
    return Object.hash(
      branchName,
      employeeName,
      employeeNumber,
      expiryCount,
    );
  }

  @override
  String toString() {
    return 'ExpiryAlert(branchName: $branchName, employeeName: $employeeName, employeeNumber: $employeeNumber, expiryCount: $expiryCount)';
  }
}
