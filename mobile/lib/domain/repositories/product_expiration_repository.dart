import '../entities/product_expiration_item.dart';
import '../entities/product_expiration_form.dart';

/// 소비기한 Repository 인터페이스
///
/// 소비기한 CRUD 기능을 추상화합니다.
abstract class ProductExpirationRepository {
  /// 소비기한 목록 조회
  ///
  /// [filter]: 검색 필터 (거래처, 기간)
  /// Returns: 소비기한 항목 목록
  Future<List<ProductExpirationItem>> getProductExpirationList(ProductExpirationFilter filter);

  /// 소비기한 등록
  ///
  /// [form]: 등록 폼 데이터
  /// Returns: 등록된 소비기한 항목
  Future<ProductExpirationItem> registerProductExpiration(ProductExpirationRegisterForm form);

  /// 소비기한 수정
  ///
  /// [seq]: 수정할 소비기한 항목 시퀀스
  /// [form]: 수정 폼 데이터
  /// Returns: 수정된 소비기한 항목
  Future<ProductExpirationItem> updateProductExpiration(int seq, ProductExpirationUpdateForm form);

  /// 소비기한 단건 삭제
  ///
  /// [seq]: 삭제할 소비기한 항목 시퀀스
  Future<void> deleteProductExpiration(int seq);

  /// 소비기한 일괄 삭제
  ///
  /// [seqs]: 삭제할 소비기한 항목 시퀀스 목록
  /// Returns: 삭제된 항목 수
  Future<int> deleteProductExpirationBatch(List<int> seqs);
}
