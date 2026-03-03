import '../repositories/shelf_life_repository.dart';

/// 유통기한 단건 삭제 UseCase
///
/// 유통기한 수정 화면에서 단건 삭제를 수행합니다.
class DeleteShelfLife {
  final ShelfLifeRepository _repository;

  DeleteShelfLife(this._repository);

  /// 유통기한 단건 삭제 실행
  ///
  /// [seq]: 삭제할 유통기한 항목 시퀀스
  /// Throws: [Exception] 시퀀스가 유효하지 않은 경우
  Future<void> call(int seq) async {
    if (seq <= 0) {
      throw Exception('유효하지 않은 유통기한 시퀀스입니다');
    }

    await _repository.deleteShelfLife(seq);
  }
}
