/// 일매출 상태 Enum
enum DailySalesStatus {
  /// 임시저장
  draft('DRAFT'),

  /// 등록완료
  registered('REGISTERED');

  final String value;
  const DailySalesStatus(this.value);

  static DailySalesStatus fromString(String value) {
    return DailySalesStatus.values.firstWhere(
      (status) => status.value == value,
      orElse: () => DailySalesStatus.draft,
    );
  }
}

/// 일매출 엔티티
///
/// 일일 행사 매출 등록 정보를 담는 도메인 엔티티입니다.
/// 대표 제품 또는 기타 제품 중 최소 하나의 제품 정보가 필수입니다.
class DailySales {
  /// 일매출 ID
  final String id;

  /// 행사 ID
  final String eventId;

  /// 매출 일자
  final DateTime salesDate;

  /// 대표제품 판매단가 (원)
  final int? mainProductPrice;

  /// 대표제품 판매수량 (개)
  final int? mainProductQuantity;

  /// 대표제품 총 판매금액 (원)
  final int? mainProductAmount;

  /// 기타제품 코드
  final String? subProductCode;

  /// 기타제품명
  final String? subProductName;

  /// 기타제품 판매수량 (개)
  final int? subProductQuantity;

  /// 기타제품 총 판매금액 (원)
  final int? subProductAmount;

  /// 사진 URL
  final String? photoUrl;

  /// 상태 (DRAFT: 임시저장, REGISTERED: 등록완료)
  final DailySalesStatus status;

  /// 등록 시각
  final DateTime? registeredAt;

  const DailySales({
    required this.id,
    required this.eventId,
    required this.salesDate,
    this.mainProductPrice,
    this.mainProductQuantity,
    this.mainProductAmount,
    this.subProductCode,
    this.subProductName,
    this.subProductQuantity,
    this.subProductAmount,
    this.photoUrl,
    required this.status,
    this.registeredAt,
  });

  /// 대표제품 정보가 입력되었는지 확인
  bool get hasMainProduct =>
      mainProductPrice != null &&
      mainProductQuantity != null &&
      mainProductAmount != null;

  /// 기타제품 정보가 입력되었는지 확인
  bool get hasSubProduct =>
      subProductCode != null &&
      subProductName != null &&
      subProductQuantity != null &&
      subProductAmount != null;

  /// 최소 하나의 제품 정보가 입력되었는지 확인
  bool get hasAnyProduct => hasMainProduct || hasSubProduct;

  /// 등록 가능 여부 (모든 필수 항목 충족)
  bool get canRegister => hasAnyProduct && photoUrl != null;

  /// 임시저장 가능 여부
  bool get canSaveDraft => true; // 임시저장은 항상 가능

  DailySales copyWith({
    String? id,
    String? eventId,
    DateTime? salesDate,
    int? mainProductPrice,
    int? mainProductQuantity,
    int? mainProductAmount,
    String? subProductCode,
    String? subProductName,
    int? subProductQuantity,
    int? subProductAmount,
    String? photoUrl,
    DailySalesStatus? status,
    DateTime? registeredAt,
  }) {
    return DailySales(
      id: id ?? this.id,
      eventId: eventId ?? this.eventId,
      salesDate: salesDate ?? this.salesDate,
      mainProductPrice: mainProductPrice ?? this.mainProductPrice,
      mainProductQuantity: mainProductQuantity ?? this.mainProductQuantity,
      mainProductAmount: mainProductAmount ?? this.mainProductAmount,
      subProductCode: subProductCode ?? this.subProductCode,
      subProductName: subProductName ?? this.subProductName,
      subProductQuantity: subProductQuantity ?? this.subProductQuantity,
      subProductAmount: subProductAmount ?? this.subProductAmount,
      photoUrl: photoUrl ?? this.photoUrl,
      status: status ?? this.status,
      registeredAt: registeredAt ?? this.registeredAt,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'eventId': eventId,
      'salesDate': salesDate.toIso8601String(),
      'mainProductPrice': mainProductPrice,
      'mainProductQuantity': mainProductQuantity,
      'mainProductAmount': mainProductAmount,
      'subProductCode': subProductCode,
      'subProductName': subProductName,
      'subProductQuantity': subProductQuantity,
      'subProductAmount': subProductAmount,
      'photoUrl': photoUrl,
      'status': status.value,
      'registeredAt': registeredAt?.toIso8601String(),
    };
  }

  factory DailySales.fromJson(Map<String, dynamic> json) {
    return DailySales(
      id: json['id'] as String,
      eventId: json['eventId'] as String,
      salesDate: DateTime.parse(json['salesDate'] as String),
      mainProductPrice: json['mainProductPrice'] as int?,
      mainProductQuantity: json['mainProductQuantity'] as int?,
      mainProductAmount: json['mainProductAmount'] as int?,
      subProductCode: json['subProductCode'] as String?,
      subProductName: json['subProductName'] as String?,
      subProductQuantity: json['subProductQuantity'] as int?,
      subProductAmount: json['subProductAmount'] as int?,
      photoUrl: json['photoUrl'] as String?,
      status: DailySalesStatus.fromString(json['status'] as String),
      registeredAt: json['registeredAt'] != null
          ? DateTime.parse(json['registeredAt'] as String)
          : null,
    );
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    return other is DailySales &&
        other.id == id &&
        other.eventId == eventId &&
        other.salesDate == salesDate &&
        other.mainProductPrice == mainProductPrice &&
        other.mainProductQuantity == mainProductQuantity &&
        other.mainProductAmount == mainProductAmount &&
        other.subProductCode == subProductCode &&
        other.subProductName == subProductName &&
        other.subProductQuantity == subProductQuantity &&
        other.subProductAmount == subProductAmount &&
        other.photoUrl == photoUrl &&
        other.status == status &&
        other.registeredAt == registeredAt;
  }

  @override
  int get hashCode {
    return Object.hash(
      id,
      eventId,
      salesDate,
      mainProductPrice,
      mainProductQuantity,
      mainProductAmount,
      subProductCode,
      subProductName,
      subProductQuantity,
      subProductAmount,
      photoUrl,
      status,
      registeredAt,
    );
  }

  @override
  String toString() {
    return 'DailySales(id: $id, eventId: $eventId, salesDate: $salesDate, '
        'mainProductPrice: $mainProductPrice, mainProductQuantity: $mainProductQuantity, '
        'mainProductAmount: $mainProductAmount, '
        'subProductCode: $subProductCode, subProductName: $subProductName, '
        'subProductQuantity: $subProductQuantity, subProductAmount: $subProductAmount, '
        'photoUrl: $photoUrl, status: $status, registeredAt: $registeredAt)';
  }
}
