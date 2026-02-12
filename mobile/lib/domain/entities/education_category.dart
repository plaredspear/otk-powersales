/// 교육 카테고리
///
/// 교육 메인 화면에 표시되는 고정 4개 카테고리.
/// 앱 내 상수로 관리하며 서버 API 호출 없이 사용한다.
enum EducationCategory {
  /// 시식 매뉴얼
  tastingManual('TASTING_MANUAL', '시식 매뉴얼', 'assets/icons/education/tasting_manual.png'),

  /// CS/안전
  csSafety('CS_SAFETY', 'CS/안전', 'assets/icons/education/cs_safety.png'),

  /// 교육 평가
  evaluation('EVALUATION', '교육 평가', 'assets/icons/education/evaluation.png'),

  /// 신제품 소개
  newProduct('NEW_PRODUCT', '신제품 소개', 'assets/icons/education/new_product.png');

  /// API 호출 시 사용할 코드 값
  final String code;

  /// 사용자에게 표시할 카테고리명
  final String displayName;

  /// 카테고리 아이콘 경로 (로컬 에셋)
  final String iconPath;

  const EducationCategory(this.code, this.displayName, this.iconPath);

  /// 코드 값으로 카테고리 찾기
  static EducationCategory fromCode(String code) {
    return EducationCategory.values.firstWhere(
      (category) => category.code == code,
      orElse: () => throw ArgumentError('Unknown education category code: $code'),
    );
  }
}
