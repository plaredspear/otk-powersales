import '../../domain/entities/claim_form.dart';
import '../../domain/entities/claim_form_data.dart';
import '../../domain/entities/claim_result.dart';
import '../../domain/repositories/claim_repository.dart';
import '../datasources/claim_remote_datasource.dart';
import '../models/claim_register_request.dart';

/// ClaimRepository 구현체
class ClaimRepositoryImpl implements ClaimRepository {
  const ClaimRepositoryImpl(this._dataSource);

  final ClaimRemoteDataSource _dataSource;

  @override
  Future<ClaimRegisterResult> registerClaim(ClaimRegisterForm form) async {
    final request = ClaimRegisterRequest.fromEntity(form);
    final model = await _dataSource.registerClaim(request);
    return model.toEntity();
  }

  @override
  Future<ClaimFormData> getFormData() async {
    final model = await _dataSource.getFormData();
    return model.toEntity();
  }
}
