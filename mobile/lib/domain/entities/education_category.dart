/// 교육 카테고리
///
/// 교육 메인 화면에 표시되는 고정 4개 카테고리.
/// 앱 내 상수로 관리하며 서버 API 호출 없이 사용한다.
///
/// 코드 값은 레거시 `education_code_mng`(현 `education_code`) 테이블의
/// `edu_code` 값(c00001~c00004)과 1:1로 매핑된다.
enum EducationCategory {
  /// APP 매뉴얼 (구 시식 매뉴얼)
  tastingManual('c00001', 'APP 매뉴얼', 'assets/images/education/app_manual.png'),

  /// 안전교육 (구 CS / 안전)
  csSafety('c00002', '안전교육', 'assets/images/education/cs_safety.png'),

  /// 교육 평가 — 외부 LMS(멀티캠퍼스) 링크로 연결한다.
  evaluation(
    'c00003',
    '교육 평가',
    'assets/images/education/ossam_retail.png',
    externalUrl:
        'https://lc.multicampus.com/otoki/#/connect/LCB20220622100094379',
  ),

  /// 설문조사 (구 신제품 소개) — 아이콘은 구 교육 평가 아이콘 재사용
  newProduct('c00004', '설문조사', 'assets/images/education/evaluation.png');

  /// API 호출 시 사용할 코드 값 (edu_code)
  final String code;

  /// 사용자에게 표시할 카테고리명
  final String displayName;

  /// 카테고리 아이콘 경로 (로컬 에셋)
  final String iconPath;

  /// 외부 링크 URL — 지정 시 게시물 목록 대신 외부 브라우저로 연결한다.
  final String? externalUrl;

  const EducationCategory(
    this.code,
    this.displayName,
    this.iconPath, {
    this.externalUrl,
  });

  /// 외부 링크로 연결되는 카테고리 여부
  bool get isExternalLink => externalUrl != null;

  /// 코드 값으로 카테고리 찾기
  static EducationCategory fromCode(String code) {
    return EducationCategory.values.firstWhere(
      (category) => category.code == code,
      orElse: () => throw ArgumentError('Unknown education category code: $code'),
    );
  }
}
