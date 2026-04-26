import '../repositories/home_repository.dart';

/// 홈 데이터 조회 UseCase
///
/// 홈 화면에 필요한 오늘 일정, 유통기한 알림, 공지사항 데이터를 조회한다.
class GetHomeData {
  final HomeRepository _repository;

  GetHomeData(this._repository);

  /// 홈 데이터를 조회한다.
  ///
  /// [HomeData]를 반환하며, API 오류 시 예외를 전파한다.
  Future<HomeData> call() async {
    return await _repository.getHomeData();
  }
}
