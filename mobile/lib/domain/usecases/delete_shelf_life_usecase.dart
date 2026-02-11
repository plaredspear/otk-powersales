import '../repositories/shelf_life_repository.dart';

/// 유통기한 단건 삭제 UseCase
///
/// 유통기한 수정 화면에서 단건 삭제를 수행합니다.
class DeleteShelfLife {
  final ShelfLifeRepository _repository;

  DeleteShelfLife(this._repository);

  /// 유통기한 단건 삭제 실행
  ///
  /// [id]: 삭제할 유통기한 항목 ID
  /// Throws: [Exception] ID가 유효하지 않은 경우
  Future<void> call(int id) async {
    if (id <= 0) {
      throw Exception('유효하지 않은 유통기한 ID입니다');
    }

    await _repository.deleteShelfLife(id);
  }
}
