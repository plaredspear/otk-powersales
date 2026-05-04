import '../../domain/entities/expiry_alert.dart';

/// ExpiryAlert API 모델 (DTO)
///
/// API 응답의 snake_case JSON을 Domain Entity로 변환한다.
class ExpiryAlertModel {
  final String branchName;
  final String employeeName;
  final String employeeCode;
  final int expiryCount;

  const ExpiryAlertModel({
    required this.branchName,
    required this.employeeName,
    required this.employeeCode,
    required this.expiryCount,
  });

  factory ExpiryAlertModel.fromJson(Map<String, dynamic> json) {
    return ExpiryAlertModel(
      branchName: json['branchName'] as String,
      employeeName: json['employeeName'] as String,
      employeeCode: json['employeeCode'] as String,
      expiryCount: json['expiryCount'] as int,
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

  ExpiryAlert toEntity() {
    return ExpiryAlert(
      branchName: branchName,
      employeeName: employeeName,
      employeeCode: employeeCode,
      expiryCount: expiryCount,
    );
  }

  factory ExpiryAlertModel.fromEntity(ExpiryAlert entity) {
    return ExpiryAlertModel(
      branchName: entity.branchName,
      employeeName: entity.employeeName,
      employeeCode: entity.employeeCode,
      expiryCount: entity.expiryCount,
    );
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    return other is ExpiryAlertModel &&
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
    return 'ExpiryAlertModel(branchName: $branchName, employeeName: $employeeName, employeeCode: $employeeCode, expiryCount: $expiryCount)';
  }
}
