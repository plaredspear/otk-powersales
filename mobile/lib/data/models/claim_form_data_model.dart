import '../../domain/entities/claim_form_data.dart';
import 'claim_category_model.dart';
import 'claim_code_model.dart';

/// 클레임 폼 초기화 데이터 Model
class ClaimFormDataModel {
  const ClaimFormDataModel({
    required this.categories,
    required this.purchaseMethods,
    required this.requestTypes,
  });

  final List<ClaimCategoryModel> categories;
  final List<PurchaseMethodModel> purchaseMethods;
  final List<ClaimRequestTypeModel> requestTypes;

  /// JSON 역직렬화
  factory ClaimFormDataModel.fromJson(Map<String, dynamic> json) {
    return ClaimFormDataModel(
      categories: (json['categories'] as List)
          .map((c) => ClaimCategoryModel.fromJson(c as Map<String, dynamic>))
          .toList(),
      purchaseMethods: (json['purchaseMethods'] as List)
          .map((p) => PurchaseMethodModel.fromJson(p as Map<String, dynamic>))
          .toList(),
      requestTypes: (json['requestTypes'] as List)
          .map((r) => ClaimRequestTypeModel.fromJson(r as Map<String, dynamic>))
          .toList(),
    );
  }

  /// Entity로 변환
  ClaimFormData toEntity() {
    return ClaimFormData(
      categories: categories.map((c) => c.toEntity()).toList(),
      purchaseMethods: purchaseMethods.map((p) => p.toEntity()).toList(),
      requestTypes: requestTypes.map((r) => r.toEntity()).toList(),
    );
  }
}
