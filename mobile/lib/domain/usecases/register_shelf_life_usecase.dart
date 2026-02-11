import '../entities/shelf_life_item.dart';
import '../entities/shelf_life_form.dart';
import '../repositories/shelf_life_repository.dart';

/// 유통기한 등록 UseCase
///
/// 유통기한 신규 등록을 수행합니다.
/// 필수 필드 유효성을 검증한 후 등록합니다.
class RegisterShelfLife {
  final ShelfLifeRepository _repository;

  RegisterShelfLife(this._repository);

  /// 유통기한 등록 실행
  ///
  /// [form]: 등록 폼 데이터 (거래처, 제품, 유통기한, 알림일, 설명)
  /// Returns: 등록된 유통기한 항목
  /// Throws: [Exception] 필수 항목이 미입력된 경우
  Future<ShelfLifeItem> call(ShelfLifeRegisterForm form) async {
    if (!form.isValid) {
      throw Exception('필수 항목을 입력해주세요');
    }

    return await _repository.registerShelfLife(form);
  }
}
