import 'dart:convert';
import 'package:hive/hive.dart';

/// 주문 로컬 데이터소스
///
/// Hive로 주문서 임시저장 기능을 제공합니다.
class OrderLocalDataSource {
  // Hive box and keys
  static const String _orderDraftBoxName = 'order_draft_box';
  static const String _draftDataKey = 'draft_data';

  /// Hive Box 열기 (초기화)
  Future<Box> _openBox() async {
    if (Hive.isBoxOpen(_orderDraftBoxName)) {
      return Hive.box(_orderDraftBoxName);
    }
    return await Hive.openBox(_orderDraftBoxName);
  }

  /// 주문서 임시저장 (JSON string으로 저장)
  ///
  /// [draftJson] 주문서 데이터를 JSON Map 형태로 전달받아 String으로 인코딩하여 저장합니다.
  Future<void> saveDraft(Map<String, dynamic> draftJson) async {
    final box = await _openBox();
    final jsonString = jsonEncode(draftJson);
    await box.put(_draftDataKey, jsonString);
  }

  /// 임시저장된 주문서 불러오기 (JSON string에서 Map으로 복원)
  ///
  /// 저장된 JSON String을 Map으로 디코딩하여 반환합니다.
  /// 저장된 데이터가 없거나 파싱에 실패하면 null을 반환합니다.
  Future<Map<String, dynamic>?> loadDraft() async {
    try {
      final box = await _openBox();
      final jsonString = box.get(_draftDataKey) as String?;
      if (jsonString == null) return null;

      return jsonDecode(jsonString) as Map<String, dynamic>;
    } catch (e) {
      // 데이터가 손상되었거나 파싱 실패 시 null 반환
      return null;
    }
  }

  /// 임시저장 주문서 삭제
  ///
  /// 저장된 임시 주문서 데이터를 삭제합니다.
  Future<void> deleteDraft() async {
    final box = await _openBox();
    await box.delete(_draftDataKey);
  }

  /// 임시저장 데이터 존재 여부
  ///
  /// 임시저장된 주문서가 있는지 확인합니다.
  Future<bool> hasDraft() async {
    try {
      final box = await _openBox();
      return box.containsKey(_draftDataKey);
    } catch (e) {
      return false;
    }
  }
}
