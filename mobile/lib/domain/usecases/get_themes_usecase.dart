import '../entities/inspection_theme.dart';
import '../repositories/inspection_repository.dart';

/// 테마 목록 조회 UseCase
///
/// 오늘 기준 기간 포함 테마 목록을 조회합니다.
class GetThemesUseCase {
  final InspectionRepository _repository;

  GetThemesUseCase(this._repository);

  /// 테마 목록 조회 실행
  ///
  /// Returns: 테마 목록
  Future<List<InspectionTheme>> call() async {
    return await _repository.getThemes();
  }
}
