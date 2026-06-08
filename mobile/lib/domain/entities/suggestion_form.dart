import 'dart:io';

/// 제안하기 분류 enum
enum SuggestionCategory {
  /// 신제품 제안
  newProduct('NEW_PRODUCT', '신제품 제안'),

  /// 기존제품 상품가치향상
  existingProduct('EXISTING_PRODUCT', '기존제품 상품가치향상'),

  /// 물류 클레임
  logisticsClaim('LOGISTICS_CLAIM', '물류 클레임');

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

/// 물류 클레임 항목 (레거시 suggestWrite.jsp 하드코딩 6 옵션)
///
/// 레거시 Controller `suggest()` 가 `Arrays.asList(...)` 로 내려주던 고정 목록.
/// backend `claimType` 은 자유 텍스트(`@Size(max=200)`) 라서 선택값 문자열을
/// 그대로 전송한다.
const List<String> kSuggestionClaimTypeOptions = [
  '배송기준 미준수(검수/창고적치 미실시)',
  '취급부주의 제품 파손',
  '배송시간 지연',
  '실물 미입고 / 오입고',
  '용차배송 거래처 트러블',
  '기타',
];

/// 제안하기 등록 폼 Entity
///
/// 제안하기 등록 시 사용자가 입력한 모든 정보를 담습니다. 카테고리에 따라
/// 입력 필드가 분기됩니다 — 신제품/기존제품은 제품 정보, 물류 클레임은
/// 거래처/클레임 항목/발생일자/차량번호.
class SuggestionRegisterForm {
  const SuggestionRegisterForm({
    required this.category,
    this.productCode,
    this.productName,
    required this.title,
    required this.content,
    this.photos = const [],
    this.accountId,
    this.accountName,
    this.sapAccountCode,
    this.claimType,
    this.claimDate,
    this.carNumber,
  });

  /// 분류 (필수)
  final SuggestionCategory category;

  /// 제품 코드 (신제품 외 분류에서 필수)
  final String? productCode;

  /// 제품명 (로컬 표시용)
  final String? productName;

  /// 제안 제목 (필수)
  final String title;

  /// 제안 내용 / 클레임 상세 내용 (필수)
  final String content;

  /// 사진 (최대 2장, 물류 클레임 시 1장 이상 필수)
  final List<File> photos;

  /// 거래처 PK (물류 클레임 선택 시 필수)
  final int? accountId;

  /// 거래처명 (로컬 표시용)
  final String? accountName;

  /// SAP 거래처 코드 (denorm — backend 로 전달)
  final String? sapAccountCode;

  /// 클레임 항목 (물류 클레임 선택 시 필수, max 200)
  final String? claimType;

  /// 클레임 발생일자 (물류 클레임 선택 시 필수)
  final DateTime? claimDate;

  /// 차량번호 (물류 클레임 선택 시 선택, max 20)
  final String? carNumber;

  /// 신제품 제안 여부
  bool get isNewProduct => category == SuggestionCategory.newProduct;

  /// 기존제품 제안 여부
  bool get isExistingProduct => category == SuggestionCategory.existingProduct;

  /// 물류 클레임 여부
  bool get isLogisticsClaim => category == SuggestionCategory.logisticsClaim;

  /// 대표 제품 선택 필수 여부 — 신제품 제안 외 분류는 필수 (레거시 정합)
  bool get requiresProduct => !isNewProduct;

  /// 제품 선택 여부
  bool get hasProduct =>
      productCode != null &&
      productCode!.isNotEmpty &&
      productName != null &&
      productName!.isNotEmpty;

  /// 거래처 선택 여부
  bool get hasAccount => accountId != null;

  /// 사진 첨부 여부
  bool get hasPhotos => photos.isNotEmpty;

  /// 유효성 검증 (레거시 suggestWrite.jsp 검증 규칙 정합)
  ///
  /// Returns: 에러 메시지 리스트 (빈 리스트면 유효)
  List<String> validate() {
    final errors = <String>[];

    // 대표 제품 — 신제품 제안 외에는 필수 (레거시 step1)
    if (requiresProduct && !hasProduct) {
      errors.add('제품을 선택해주세요');
    }

    if (title.isEmpty) {
      errors.add('제목을 입력해주세요');
    }

    if (content.isEmpty) {
      errors.add(isLogisticsClaim ? '클레임 상세 내용을 입력해주세요' : '제안 내용을 입력해주세요');
    }

    if (isLogisticsClaim) {
      if (!hasAccount) {
        errors.add('거래처를 선택해주세요');
      }
      if (claimType == null || claimType!.isEmpty) {
        errors.add('클레임 항목을 선택해주세요');
      }
      if (claimDate == null) {
        errors.add('물류 클레임 발생일자를 선택해주세요');
      }
      // 레거시 step5 — 물류 클레임 시 사진 필수
      if (!hasPhotos) {
        errors.add('물류 클레임은 사진을 1장 이상 첨부해주세요');
      }
    }

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
    int? accountId,
    String? accountName,
    String? sapAccountCode,
    String? claimType,
    DateTime? claimDate,
    String? carNumber,
  }) {
    return SuggestionRegisterForm(
      category: category ?? this.category,
      productCode: productCode ?? this.productCode,
      productName: productName ?? this.productName,
      title: title ?? this.title,
      content: content ?? this.content,
      photos: photos ?? this.photos,
      accountId: accountId ?? this.accountId,
      accountName: accountName ?? this.accountName,
      sapAccountCode: sapAccountCode ?? this.sapAccountCode,
      claimType: claimType ?? this.claimType,
      claimDate: claimDate ?? this.claimDate,
      carNumber: carNumber ?? this.carNumber,
    );
  }

  /// copyWith에서 null로 초기화
  SuggestionRegisterForm copyWithNull({
    bool productCode = false,
    bool productName = false,
    bool accountId = false,
    bool accountName = false,
    bool sapAccountCode = false,
    bool claimType = false,
    bool claimDate = false,
    bool carNumber = false,
  }) {
    return SuggestionRegisterForm(
      category: this.category,
      productCode: productCode ? null : this.productCode,
      productName: productName ? null : this.productName,
      title: this.title,
      content: this.content,
      photos: this.photos,
      accountId: accountId ? null : this.accountId,
      accountName: accountName ? null : this.accountName,
      sapAccountCode: sapAccountCode ? null : this.sapAccountCode,
      claimType: claimType ? null : this.claimType,
      claimDate: claimDate ? null : this.claimDate,
      carNumber: carNumber ? null : this.carNumber,
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
        _listEquals(other.photos, photos) &&
        other.accountId == accountId &&
        other.accountName == accountName &&
        other.sapAccountCode == sapAccountCode &&
        other.claimType == claimType &&
        other.claimDate == claimDate &&
        other.carNumber == carNumber;
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
        accountId,
        accountName,
        sapAccountCode,
        claimType,
        claimDate,
        carNumber,
      );

  @override
  String toString() {
    return 'SuggestionRegisterForm('
        'category: $category, '
        'productCode: $productCode, '
        'productName: $productName, '
        'title: $title, '
        'content: $content, '
        'photos: ${photos.length}장, '
        'accountId: $accountId, '
        'accountName: $accountName, '
        'sapAccountCode: $sapAccountCode, '
        'claimType: $claimType, '
        'claimDate: $claimDate, '
        'carNumber: $carNumber'
        ')';
  }
}
