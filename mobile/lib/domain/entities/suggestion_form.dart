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

/// 제안하기 등록 폼 Entity
///
/// 제안하기 등록 시 사용자가 입력한 모든 정보를 담습니다. 카테고리에 따라
/// 입력 필드가 분기됩니다 — 신제품/기존제품은 제품 정보, 물류 클레임은
/// 거래처/클레임 항목/일자 등 6 필드.
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
    this.logisticsResponsibility,
    this.duplicateProposalNum,
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

  /// 거래처 PK (물류 클레임 선택 시 필수)
  final int? accountId;

  /// 거래처명 (로컬 표시용)
  final String? accountName;

  /// SAP 거래처 코드 (denorm — backend 로 전달)
  final String? sapAccountCode;

  /// 클레임 항목 (물류 클레임 선택 시 필수, max 200)
  final String? claimType;

  /// 클레임 일자 (물류 클레임 선택 시 필수)
  final DateTime? claimDate;

  /// 차량번호 (물류 클레임 선택 시 선택, max 20)
  final String? carNumber;

  /// 물류책임 (물류 클레임 선택 시 선택, max 20)
  final String? logisticsResponsibility;

  /// 중복 제안번호 (물류 클레임 선택 시 선택, max 255)
  final String? duplicateProposalNum;

  /// 신제품 제안 여부
  bool get isNewProduct => category == SuggestionCategory.newProduct;

  /// 기존제품 제안 여부
  bool get isExistingProduct => category == SuggestionCategory.existingProduct;

  /// 물류 클레임 여부
  bool get isLogisticsClaim => category == SuggestionCategory.logisticsClaim;

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

  /// 유효성 검증
  ///
  /// Returns: 에러 메시지 리스트 (빈 리스트면 유효)
  List<String> validate() {
    final errors = <String>[];

    if (title.isEmpty) {
      errors.add('제목을 입력해주세요');
    }

    if (content.isEmpty) {
      errors.add('제안 내용을 입력해주세요');
    }

    if (isExistingProduct && !hasProduct) {
      errors.add('제품을 선택해주세요');
    }

    if (isLogisticsClaim) {
      if (!hasAccount) {
        errors.add('거래처를 선택해주세요');
      }
      if (claimType == null || claimType!.isEmpty) {
        errors.add('클레임 항목을 입력해주세요');
      }
      if (claimDate == null) {
        errors.add('클레임 일자를 선택해주세요');
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
    String? logisticsResponsibility,
    String? duplicateProposalNum,
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
      logisticsResponsibility:
          logisticsResponsibility ?? this.logisticsResponsibility,
      duplicateProposalNum: duplicateProposalNum ?? this.duplicateProposalNum,
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
    bool logisticsResponsibility = false,
    bool duplicateProposalNum = false,
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
      logisticsResponsibility:
          logisticsResponsibility ? null : this.logisticsResponsibility,
      duplicateProposalNum:
          duplicateProposalNum ? null : this.duplicateProposalNum,
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
        other.carNumber == carNumber &&
        other.logisticsResponsibility == logisticsResponsibility &&
        other.duplicateProposalNum == duplicateProposalNum;
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
        logisticsResponsibility,
        duplicateProposalNum,
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
        'carNumber: $carNumber, '
        'logisticsResponsibility: $logisticsResponsibility, '
        'duplicateProposalNum: $duplicateProposalNum'
        ')';
  }
}
