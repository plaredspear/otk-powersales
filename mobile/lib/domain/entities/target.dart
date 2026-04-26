/// 목표 엔티티
///
/// 거래처별 월 목표금액 및 실적금액을 관리합니다.
/// 알라딘/SAP 시스템과 연동하여 목표를 입력받습니다.
class Target {
  /// 목표 ID (고유 식별자)
  final String id;

  /// 거래처명
  final String customerName;

  /// 거래처 코드
  final String customerCode;

  /// 년월 (예: 202601)
  final String yearMonth;

  /// 월 목표금액 (원)
  final int targetAmount;

  /// 실적금액 (원)
  final int actualAmount;

  /// 카테고리 (선택적, 예: '상온', '라면', '냉동/냉장')
  final String? category;

  /// 비고 (메모)
  final String? note;

  /// 생성일시
  final DateTime createdAt;

  /// 수정일시
  final DateTime updatedAt;

  const Target({
    required this.id,
    required this.customerName,
    required this.customerCode,
    required this.yearMonth,
    required this.targetAmount,
    required this.actualAmount,
    this.category,
    this.note,
    required this.createdAt,
    required this.updatedAt,
  });

  /// 불변성을 유지하며 일부 필드를 변경한 새 인스턴스 생성
  Target copyWith({
    String? id,
    String? customerName,
    String? customerCode,
    String? yearMonth,
    int? targetAmount,
    int? actualAmount,
    String? category,
    String? note,
    DateTime? createdAt,
    DateTime? updatedAt,
  }) {
    return Target(
      id: id ?? this.id,
      customerName: customerName ?? this.customerName,
      customerCode: customerCode ?? this.customerCode,
      yearMonth: yearMonth ?? this.yearMonth,
      targetAmount: targetAmount ?? this.targetAmount,
      actualAmount: actualAmount ?? this.actualAmount,
      category: category ?? this.category,
      note: note ?? this.note,
      createdAt: createdAt ?? this.createdAt,
      updatedAt: updatedAt ?? this.updatedAt,
    );
  }

  /// JSON으로 변환
  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'customerName': customerName,
      'customerCode': customerCode,
      'yearMonth': yearMonth,
      'targetAmount': targetAmount,
      'actualAmount': actualAmount,
      'category': category,
      'note': note,
      'createdAt': createdAt.toIso8601String(),
      'updatedAt': updatedAt.toIso8601String(),
    };
  }

  /// JSON에서 엔티티 생성
  factory Target.fromJson(Map<String, dynamic> json) {
    return Target(
      id: json['id'] as String,
      customerName: json['customerName'] as String,
      customerCode: json['customerCode'] as String,
      yearMonth: json['yearMonth'] as String,
      targetAmount: json['targetAmount'] as int,
      actualAmount: json['actualAmount'] as int,
      category: json['category'] as String?,
      note: json['note'] as String?,
      createdAt: DateTime.parse(json['createdAt'] as String),
      updatedAt: DateTime.parse(json['updatedAt'] as String),
    );
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;

    return other is Target &&
        other.id == id &&
        other.customerName == customerName &&
        other.customerCode == customerCode &&
        other.yearMonth == yearMonth &&
        other.targetAmount == targetAmount &&
        other.actualAmount == actualAmount &&
        other.category == category &&
        other.note == note &&
        other.createdAt == createdAt &&
        other.updatedAt == updatedAt;
  }

  @override
  int get hashCode {
    return Object.hash(
      id,
      customerName,
      customerCode,
      yearMonth,
      targetAmount,
      actualAmount,
      category,
      note,
      createdAt,
      updatedAt,
    );
  }

  @override
  String toString() {
    return 'Target(id: $id, customerName: $customerName, '
        'customerCode: $customerCode, yearMonth: $yearMonth, '
        'targetAmount: $targetAmount, actualAmount: $actualAmount, '
        'category: $category, note: $note, '
        'createdAt: $createdAt, updatedAt: $updatedAt)';
  }
}
