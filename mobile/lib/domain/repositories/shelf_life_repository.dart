import '../entities/shelf_life_item.dart';
import '../entities/shelf_life_form.dart';

/// 유통기한 Repository 인터페이스
///
/// 유통기한 CRUD 기능을 추상화합니다.
/// 구현체는 Mock Repository 또는 실제 API Repository가 될 수 있습니다.
abstract class ShelfLifeRepository {
  /// 유통기한 목록 조회
  ///
  /// [filter]: 검색 필터 (거래처, 기간)
  /// Returns: 유통기한 항목 목록
  Future<List<ShelfLifeItem>> getShelfLifeList(ShelfLifeFilter filter);

  /// 유통기한 등록
  ///
  /// [form]: 등록 폼 데이터
  /// Returns: 등록된 유통기한 항목
  Future<ShelfLifeItem> registerShelfLife(ShelfLifeRegisterForm form);

  /// 유통기한 수정
  ///
  /// [id]: 수정할 유통기한 항목 ID
  /// [form]: 수정 폼 데이터
  /// Returns: 수정된 유통기한 항목
  Future<ShelfLifeItem> updateShelfLife(int id, ShelfLifeUpdateForm form);

  /// 유통기한 단건 삭제
  ///
  /// [id]: 삭제할 유통기한 항목 ID
  Future<void> deleteShelfLife(int id);

  /// 유통기한 일괄 삭제
  ///
  /// [ids]: 삭제할 유통기한 항목 ID 목록
  /// Returns: 삭제된 항목 수
  Future<int> deleteShelfLifeBatch(List<int> ids);
}
