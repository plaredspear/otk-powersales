import '../repositories/attendance_repository.dart';

/// 거래처 목록 조회 UseCase
///
/// 오늘 출근해야 할 거래처 목록을 조회합니다.
class GetAccountList {
  final AttendanceRepository _repository;

  GetAccountList(this._repository);

  /// 거래처 목록 조회 실행
  Future<AccountListResult> call({String? keyword}) async {
    return await _repository.getAccountList(keyword: keyword);
  }
}
