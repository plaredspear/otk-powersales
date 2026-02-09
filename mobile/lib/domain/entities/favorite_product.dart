class FavoriteProduct {
  final String id; // 고유 ID (제품코드로 사용)
  final String productName; // 제품명
  final DateTime addedAt; // 즐겨찾기 추가 시각

  const FavoriteProduct({
    required this.id,
    required this.productName,
    required this.addedAt,
  });

  // copyWith 메서드
  FavoriteProduct copyWith({
    String? id,
    String? productName,
    DateTime? addedAt,
  }) {
    return FavoriteProduct(
      id: id ?? this.id,
      productName: productName ?? this.productName,
      addedAt: addedAt ?? this.addedAt,
    );
  }

  // JSON 직렬화
  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'productName': productName,
      'addedAt': addedAt.toIso8601String(),
    };
  }

  // JSON 역직렬화
  factory FavoriteProduct.fromJson(Map<String, dynamic> json) {
    return FavoriteProduct(
      id: json['id'] as String,
      productName: json['productName'] as String,
      addedAt: DateTime.parse(json['addedAt'] as String),
    );
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    return other is FavoriteProduct &&
        other.id == id &&
        other.productName == productName &&
        other.addedAt == addedAt;
  }

  @override
  int get hashCode => Object.hash(id, productName, addedAt);

  @override
  String toString() {
    return 'FavoriteProduct(id: $id, productName: $productName, addedAt: $addedAt)';
  }
}
