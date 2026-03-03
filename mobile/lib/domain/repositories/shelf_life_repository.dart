import '../entities/shelf_life_item.dart';
import '../entities/shelf_life_form.dart';

/// 유통기한 Repository 인터페이스
///
/// 유통기한 CRUD 기능을 추상화합니다.
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
  /// [seq]: 수정할 유통기한 항목 시퀀스
  /// [form]: 수정 폼 데이터
  /// Returns: 수정된 유통기한 항목
  Future<ShelfLifeItem> updateShelfLife(int seq, ShelfLifeUpdateForm form);

  /// 유통기한 단건 삭제
  ///
  /// [seq]: 삭제할 유통기한 항목 시퀀스
  Future<void> deleteShelfLife(int seq);

  /// 유통기한 일괄 삭제
  ///
  /// [seqs]: 삭제할 유통기한 항목 시퀀스 목록
  /// Returns: 삭제된 항목 수
  Future<int> deleteShelfLifeBatch(List<int> seqs);
}
