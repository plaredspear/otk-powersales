import '../repositories/product_expiration_repository.dart';

/// 유통기한 일괄 삭제 UseCase
///
/// 유통기한 삭제 화면에서 선택된 항목을 일괄 삭제합니다.
class DeleteProductExpirationBatch {
  final ProductExpirationRepository _repository;

  DeleteProductExpirationBatch(this._repository);

  /// 유통기한 일괄 삭제 실행
  ///
  /// [ids]: 삭제할 유통기한 항목 ID 목록
  /// Returns: 삭제된 항목 수
  /// Throws: [Exception] ID 목록이 비어있는 경우
  Future<int> call(List<int> ids) async {
    if (ids.isEmpty) {
      throw Exception('삭제할 항목을 선택해주세요');
    }

    return await _repository.deleteProductExpirationBatch(ids);
  }
}
