import '../../domain/entities/safety_check_item.dart';

/// 안전점검 항목 모델 (V1 JSON 매핑)
class SafetyCheckItemModel {
  final int seqNum;
  final String contents;

  const SafetyCheckItemModel({
    required this.seqNum,
    required this.contents,
  });

  factory SafetyCheckItemModel.fromJson(Map<String, dynamic> json) {
    return SafetyCheckItemModel(
      seqNum: json['seqNum'] as int,
      contents: json['contents'] as String,
    );
  }

  SafetyCheckItem toEntity() {
    return SafetyCheckItem(
      seqNum: seqNum,
      contents: contents,
    );
  }
}
