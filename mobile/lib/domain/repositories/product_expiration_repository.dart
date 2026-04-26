import '../entities/product_expiration_item.dart';
import '../entities/product_expiration_form.dart';

/// 유통기한 Repository 인터페이스
///
/// 유통기한 CRUD 기능을 추상화합니다.
abstract class ProductExpirationRepository {
  /// 유통기한 목록 조회
  ///
  /// [filter]: 검색 필터 (거래처, 기간)
  /// Returns: 유통기한 항목 목록
  Future<List<ProductExpirationItem>> getProductExpirationList(ProductExpirationFilter filter);

  /// 유통기한 등록
  ///
  /// [form]: 등록 폼 데이터
  /// Returns: 등록된 유통기한 항목
  Future<ProductExpirationItem> registerProductExpiration(ProductExpirationRegisterForm form);

  /// 유통기한 수정
  ///
  /// [seq]: 수정할 유통기한 항목 시퀀스
  /// [form]: 수정 폼 데이터
  /// Returns: 수정된 유통기한 항목
  Future<ProductExpirationItem> updateProductExpiration(int seq, ProductExpirationUpdateForm form);

  /// 유통기한 단건 삭제
  ///
  /// [seq]: 삭제할 유통기한 항목 시퀀스
  Future<void> deleteProductExpiration(int seq);

  /// 유통기한 일괄 삭제
  ///
  /// [seqs]: 삭제할 유통기한 항목 시퀀스 목록
  /// Returns: 삭제된 항목 수
  Future<int> deleteProductExpirationBatch(List<int> seqs);
}
