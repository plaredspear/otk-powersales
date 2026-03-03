import '../entities/shelf_life_item.dart';
import '../entities/shelf_life_form.dart';
import '../repositories/shelf_life_repository.dart';

/// 유통기한 수정 UseCase
///
/// 기존 유통기한 항목의 수정을 수행합니다.
/// 수정 가능 항목: 유통기한, 마감 전 알림 날짜, 설명
class UpdateShelfLife {
  final ShelfLifeRepository _repository;

  UpdateShelfLife(this._repository);

  /// 유통기한 수정 실행
  ///
  /// [seq]: 수정할 유통기한 항목 시퀀스
  /// [form]: 수정 폼 데이터 (유통기한, 알림일, 설명)
  /// Returns: 수정된 유통기한 항목
  /// Throws: [Exception] 시퀀스가 유효하지 않은 경우
  Future<ShelfLifeItem> call(int seq, ShelfLifeUpdateForm form) async {
    if (seq <= 0) {
      throw Exception('유효하지 않은 유통기한 시퀀스입니다');
    }

    return await _repository.updateShelfLife(seq, form);
  }
}
