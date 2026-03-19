import '../../domain/entities/expiry_alert.dart';

/// ExpiryAlert API 모델 (DTO)
///
/// API 응답의 snake_case JSON을 Domain Entity로 변환한다.
class ExpiryAlertModel {
  final String branchName;
  final String employeeName;
  final String employeeNumber;
  final int expiryCount;

  const ExpiryAlertModel({
    required this.branchName,
    required this.employeeName,
    required this.employeeNumber,
    required this.expiryCount,
  });

  factory ExpiryAlertModel.fromJson(Map<String, dynamic> json) {
    return ExpiryAlertModel(
      branchName: json['branch_name'] as String,
      employeeName: json['employee_name'] as String,
      employeeNumber: json['employee_number'] as String,
      expiryCount: json['expiry_count'] as int,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'branch_name': branchName,
      'employee_name': employeeName,
      'employee_number': employeeNumber,
      'expiry_count': expiryCount,
    };
  }

  ExpiryAlert toEntity() {
    return ExpiryAlert(
      branchName: branchName,
      employeeName: employeeName,
      employeeNumber: employeeNumber,
      expiryCount: expiryCount,
    );
  }

  factory ExpiryAlertModel.fromEntity(ExpiryAlert entity) {
    return ExpiryAlertModel(
      branchName: entity.branchName,
      employeeName: entity.employeeName,
      employeeNumber: entity.employeeNumber,
      expiryCount: entity.expiryCount,
    );
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    return other is ExpiryAlertModel &&
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
    return 'ExpiryAlertModel(branchName: $branchName, employeeName: $employeeName, employeeNumber: $employeeNumber, expiryCount: $expiryCount)';
  }
}
