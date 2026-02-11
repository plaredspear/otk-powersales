import '../../domain/entities/inspection_list_item.dart';

/// 현장 점검 목록 항목 모델 (DTO)
///
/// Backend API의 JSON을 파싱하여 InspectionListItem 엔티티로 변환합니다.
class InspectionListItemModel {
  final int id;
  final String category;
  final String storeName;
  final int storeId;
  final String inspectionDate;
  final String fieldType;
  final String fieldTypeCode;

  const InspectionListItemModel({
    required this.id,
    required this.category,
    required this.storeName,
    required this.storeId,
    required this.inspectionDate,
    required this.fieldType,
    required this.fieldTypeCode,
  });

  /// JSON에서 파싱
  factory InspectionListItemModel.fromJson(Map<String, dynamic> json) {
    return InspectionListItemModel(
      id: json['id'] as int,
      category: json['category'] as String,
      storeName: json['storeName'] as String,
      storeId: json['storeId'] as int,
      inspectionDate: json['inspectionDate'] as String,
      fieldType: json['fieldType'] as String,
      fieldTypeCode: json['fieldTypeCode'] as String,
    );
  }

  /// JSON으로 직렬화
  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'category': category,
      'storeName': storeName,
      'storeId': storeId,
      'inspectionDate': inspectionDate,
      'fieldType': fieldType,
      'fieldTypeCode': fieldTypeCode,
    };
  }

  /// Domain Entity로 변환
  InspectionListItem toEntity() {
    return InspectionListItem(
      id: id,
      category: InspectionCategoryExtension.fromJson(category),
      storeName: storeName,
      storeId: storeId,
      inspectionDate: DateTime.parse(inspectionDate),
      fieldType: fieldType,
      fieldTypeCode: fieldTypeCode,
    );
  }

  /// Domain Entity에서 생성
  factory InspectionListItemModel.fromEntity(InspectionListItem entity) {
    return InspectionListItemModel(
      id: entity.id,
      category: entity.category.toJson(),
      storeName: entity.storeName,
      storeId: entity.storeId,
      inspectionDate: entity.inspectionDate.toIso8601String().substring(0, 10),
      fieldType: entity.fieldType,
      fieldTypeCode: entity.fieldTypeCode,
    );
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    return other is InspectionListItemModel &&
        other.id == id &&
        other.category == category &&
        other.storeName == storeName &&
        other.storeId == storeId &&
        other.inspectionDate == inspectionDate &&
        other.fieldType == fieldType &&
        other.fieldTypeCode == fieldTypeCode;
  }

  @override
  int get hashCode {
    return Object.hash(
      id,
      category,
      storeName,
      storeId,
      inspectionDate,
      fieldType,
      fieldTypeCode,
    );
  }

  @override
  String toString() {
    return 'InspectionListItemModel(id: $id, category: $category, '
        'storeName: $storeName, storeId: $storeId, '
        'inspectionDate: $inspectionDate, fieldType: $fieldType, '
        'fieldTypeCode: $fieldTypeCode)';
  }
}
