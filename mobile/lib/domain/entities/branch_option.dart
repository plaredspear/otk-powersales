/// 지점 선택 옵션 (백엔드 `BranchResponse` 대응).
///
/// 대리출근 화면에서 AccountViewAll 사용자가 지점을 고를 때 드롭다운 항목으로 사용.
class BranchOption {
  final String branchCode;
  final String branchName;

  const BranchOption({
    required this.branchCode,
    required this.branchName,
  });

  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      other is BranchOption && other.branchCode == branchCode;

  @override
  int get hashCode => branchCode.hashCode;
}
