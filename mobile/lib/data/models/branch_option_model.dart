import '../../domain/entities/branch_option.dart';

/// `GET /api/v1/mobile/proxy-attendance/branches` 응답의 단일 지점 항목 모델.
class BranchOptionModel {
  final String branchCode;
  final String branchName;

  const BranchOptionModel({
    required this.branchCode,
    required this.branchName,
  });

  factory BranchOptionModel.fromJson(Map<String, dynamic> json) {
    return BranchOptionModel(
      branchCode: json['branchCode'] as String,
      branchName: json['branchName'] as String,
    );
  }

  BranchOption toEntity() => BranchOption(
        branchCode: branchCode,
        branchName: branchName,
      );
}
