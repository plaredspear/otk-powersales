import '../../domain/entities/education_category.dart';
import '../../domain/entities/education_post.dart';
import '../../domain/entities/education_post_detail.dart';
import '../../domain/repositories/education_repository.dart';
import '../datasources/education_remote_datasource.dart';

/// Education Repository 구현체
///
/// Remote DataSource에서 데이터를 가져와 Domain Entity로 변환한다.
class EducationRepositoryImpl implements EducationRepository {
  final EducationRemoteDataSource _remoteDataSource;

  EducationRepositoryImpl({
    required EducationRemoteDataSource remoteDataSource,
  }) : _remoteDataSource = remoteDataSource;

  @override
  Future<EducationPostPage> getPosts({
    required EducationCategory category,
    String? search,
    int page = 1,
    int size = 10,
  }) async {
    final model = await _remoteDataSource.getPosts(
      category: category.code,
      search: search,
      page: page,
      size: size,
    );
    return model.toEntity();
  }

  @override
  Future<EducationPostDetail> getPostDetail(String postId) async {
    final model = await _remoteDataSource.getPostDetail(postId);
    return model.toEntity();
  }
}
