import '../../domain/entities/expiry_alert.dart';

/// ExpiryAlert API 모델 (DTO)
///
/// API 응답의 snake_case JSON을 Domain Entity로 변환한다.
class ExpiryAlertModel {
  final String branchName;
  final String employeeName;
  final String employeeId;
  final int expiryCount;

  const ExpiryAlertModel({
    required this.branchName,
    required this.employeeName,
    required this.employeeId,
    required this.expiryCount,
  });

  factory ExpiryAlertModel.fromJson(Map<String, dynamic> json) {
    return ExpiryAlertModel(
      branchName: json['branch_name'] as String,
      employeeName: json['employee_name'] as String,
      employeeId: json['employee_id'] as String,
      expiryCount: json['expiry_count'] as int,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'branch_name': branchName,
      'employee_name': employeeName,
      'employee_id': employeeId,
      'expiry_count': expiryCount,
    };
  }

  ExpiryAlert toEntity() {
    return ExpiryAlert(
      branchName: branchName,
      employeeName: employeeName,
      employeeId: employeeId,
      expiryCount: expiryCount,
    );
  }

  factory ExpiryAlertModel.fromEntity(ExpiryAlert entity) {
    return ExpiryAlertModel(
      branchName: entity.branchName,
      employeeName: entity.employeeName,
      employeeId: entity.employeeId,
      expiryCount: entity.expiryCount,
    );
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    return other is ExpiryAlertModel &&
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
    return 'ExpiryAlertModel(branchName: $branchName, employeeName: $employeeName, employeeId: $employeeId, expiryCount: $expiryCount)';
  }
}
