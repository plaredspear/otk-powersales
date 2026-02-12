import 'dart:io';

/// 제안하기 분류 enum
enum SuggestionCategory {
  /// 신제품 제안
  newProduct('NEW_PRODUCT', '신제품 제안'),

  /// 기존제품 상품가치향상
  existingProduct('EXISTING_PRODUCT', '기존제품 상품가치향상');

  const SuggestionCategory(this.code, this.displayName);

  /// API 전송용 코드
  final String code;

  /// 화면 표시용 이름
  final String displayName;

  /// 코드로부터 enum 값 찾기
  static SuggestionCategory fromCode(String code) {
    return SuggestionCategory.values.firstWhere(
      (category) => category.code == code,
      orElse: () => SuggestionCategory.newProduct,
    );
  }
}

/// 제안하기 등록 폼 Entity
///
/// 제안하기 등록 시 사용자가 입력한 모든 정보를 담습니다.
class SuggestionRegisterForm {
  const SuggestionRegisterForm({
    required this.category,
    this.productCode,
    this.productName,
    required this.title,
    required this.content,
    this.photos = const [],
  });

  /// 분류 (필수)
  final SuggestionCategory category;

  /// 제품 코드 (기존제품 선택 시 필수)
  final String? productCode;

  /// 제품명 (로컬 표시용)
  final String? productName;

  /// 제안 제목 (필수)
  final String title;

  /// 제안 내용 (필수)
  final String content;

  /// 사진 (최대 2장, 선택)
  final List<File> photos;

  /// 신제품 제안인지 확인
  bool get isNewProduct => category == SuggestionCategory.newProduct;

  /// 기존제품 제안인지 확인
  bool get isExistingProduct => category == SuggestionCategory.existingProduct;

  /// 제품 선택 여부
  bool get hasProduct =>
      productCode != null &&
      productCode!.isNotEmpty &&
      productName != null &&
      productName!.isNotEmpty;

  /// 사진 첨부 여부
  bool get hasPhotos => photos.isNotEmpty;

  /// 유효성 검증
  ///
  /// Returns: 에러 메시지 리스트 (빈 리스트면 유효)
  List<String> validate() {
    final errors = <String>[];

    // 필수 필드: 제목
    if (title.isEmpty) {
      errors.add('제목을 입력해주세요');
    }

    // 필수 필드: 제안 내용
    if (content.isEmpty) {
      errors.add('제안 내용을 입력해주세요');
    }

    // 조건부 필수: 기존제품 선택 시 제품 선택 필수
    if (isExistingProduct && !hasProduct) {
      errors.add('제품을 선택해주세요');
    }

    // 사진 개수 제한
    if (photos.length > 2) {
      errors.add('사진은 최대 2장까지 첨부 가능합니다');
    }

    return errors;
  }

  /// 유효한 폼인지 확인
  bool get isValid => validate().isEmpty;

  /// copyWith
  SuggestionRegisterForm copyWith({
    SuggestionCategory? category,
    String? productCode,
    String? productName,
    String? title,
    String? content,
    List<File>? photos,
  }) {
    return SuggestionRegisterForm(
      category: category ?? this.category,
      productCode: productCode ?? this.productCode,
      productName: productName ?? this.productName,
      title: title ?? this.title,
      content: content ?? this.content,
      photos: photos ?? this.photos,
    );
  }

  /// copyWith에서 null로 초기화
  SuggestionRegisterForm copyWithNull({
    bool productCode = false,
    bool productName = false,
  }) {
    return SuggestionRegisterForm(
      category: this.category,
      productCode: productCode ? null : this.productCode,
      productName: productName ? null : this.productName,
      title: this.title,
      content: this.content,
      photos: this.photos,
    );
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    return other is SuggestionRegisterForm &&
        other.category == category &&
        other.productCode == productCode &&
        other.productName == productName &&
        other.title == title &&
        other.content == content &&
        other.photos.length == photos.length &&
        _listEquals(other.photos, photos);
  }

  bool _listEquals(List<File> a, List<File> b) {
    if (a.length != b.length) return false;
    for (int i = 0; i < a.length; i++) {
      if (a[i].path != b[i].path) return false;
    }
    return true;
  }

  @override
  int get hashCode => Object.hash(
        category,
        productCode,
        productName,
        title,
        content,
        Object.hashAll(photos.map((f) => f.path)),
      );

  @override
  String toString() {
    return 'SuggestionRegisterForm('
        'category: $category, '
        'productCode: $productCode, '
        'productName: $productName, '
        'title: $title, '
        'content: $content, '
        'photos: ${photos.length}장'
        ')';
  }
}
