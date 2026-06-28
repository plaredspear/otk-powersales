/// 거래처 표시 기준 안내 엔티티
///
/// 백엔드(`GET /accounts/my`)가 로그인 사원의 권한·scope 별로 거래처를 분기 조회하므로,
/// "왜 이 거래처들만 보이는지"를 서버가 직접 문구로 내려준다(모바일 하드코딩 분기 제거).
class MyAccountMeta {
  /// 표시 기준 본문 (불릿 항목)
  final List<String> criteriaLines;

  /// 검색 동작 안내 (목록 내 검색 vs 전체 검색)
  final String searchHint;

  const MyAccountMeta({
    required this.criteriaLines,
    required this.searchHint,
  });

  factory MyAccountMeta.fromJson(Map<String, dynamic> json) {
    return MyAccountMeta(
      criteriaLines: (json['criteriaLines'] as List<dynamic>? ?? [])
          .map((e) => e as String)
          .toList(),
      searchHint: json['searchHint'] as String? ?? '',
    );
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    if (other is! MyAccountMeta) return false;
    if (other.searchHint != searchHint) return false;
    if (other.criteriaLines.length != criteriaLines.length) return false;
    for (var i = 0; i < criteriaLines.length; i++) {
      if (other.criteriaLines[i] != criteriaLines[i]) return false;
    }
    return true;
  }

  @override
  int get hashCode => Object.hash(Object.hashAll(criteriaLines), searchHint);

  @override
  String toString() =>
      'MyAccountMeta(criteriaLines: $criteriaLines, searchHint: $searchHint)';
}
