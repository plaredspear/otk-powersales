/// 안전점검 항목 엔티티 (V1)
class SafetyCheckItem {
  /// 문항 내 순서
  final int seqNum;

  /// 점검 내용 텍스트
  final String contents;

  const SafetyCheckItem({
    required this.seqNum,
    required this.contents,
  });

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    return other is SafetyCheckItem &&
        other.seqNum == seqNum &&
        other.contents == contents;
  }

  @override
  int get hashCode => Object.hash(seqNum, contents);

  @override
  String toString() =>
      'SafetyCheckItem(seqNum: $seqNum, contents: $contents)';
}
