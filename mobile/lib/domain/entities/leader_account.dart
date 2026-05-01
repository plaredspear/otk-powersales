/// 조장의 본인 거래처 정보.
class LeaderAccount {
  final int id;
  final String? name;
  final String? address1;
  final String? branchCode;
  final String? accountGroup;
  final String? accountType;

  const LeaderAccount({
    required this.id,
    required this.name,
    required this.address1,
    required this.branchCode,
    required this.accountGroup,
    required this.accountType,
  });

  @override
  bool operator ==(Object other) =>
      identical(this, other) || other is LeaderAccount && id == other.id;

  @override
  int get hashCode => id.hashCode;
}
