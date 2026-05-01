import '../../domain/entities/leader_account.dart';

/// `GET /api/v1/mobile/leader/accounts` 응답의 단일 항목 모델.
class LeaderAccountModel {
  final int id;
  final String? name;
  final String? address1;
  final String? branchCode;
  final String? accountGroup;
  final String? accountType;

  const LeaderAccountModel({
    required this.id,
    required this.name,
    required this.address1,
    required this.branchCode,
    required this.accountGroup,
    required this.accountType,
  });

  factory LeaderAccountModel.fromJson(Map<String, dynamic> json) {
    return LeaderAccountModel(
      id: json['id'] as int,
      name: json['name'] as String?,
      address1: json['address1'] as String?,
      branchCode: json['branch_code'] as String?,
      accountGroup: json['account_group'] as String?,
      accountType: json['account_type'] as String?,
    );
  }

  LeaderAccount toEntity() => LeaderAccount(
        id: id,
        name: name,
        address1: address1,
        branchCode: branchCode,
        accountGroup: accountGroup,
        accountType: accountType,
      );
}
