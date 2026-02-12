import '../../domain/entities/notice_category.dart';
import '../../domain/entities/notice_post.dart';
import '../../domain/entities/notice_post_detail.dart';
import '../../domain/repositories/notice_repository.dart';
import '../datasources/notice_remote_datasource.dart';

/// Notice Repository 구현체
///
/// Remote DataSource에서 데이터를 가져와 Domain Entity로 변환한다.
class NoticeRepositoryImpl implements NoticeRepository {
  final NoticeRemoteDataSource _remoteDataSource;

  NoticeRepositoryImpl({
    required NoticeRemoteDataSource remoteDataSource,
  }) : _remoteDataSource = remoteDataSource;

  @override
  Future<NoticePostPage> getPosts({
    NoticeCategory? category,
    String? search,
    int page = 1,
    int size = 10,
  }) async {
    // Category enum -> API 코드 변환
    final categoryCode = category?.code;

    // DataSource에서 데이터 조회
    final pageModel = await _remoteDataSource.getPosts(
      category: categoryCode,
      search: search,
      page: page,
      size: size,
    );

    // Model -> Entity 변환
    return pageModel.toEntity();
  }

  @override
  Future<NoticePostDetail> getPostDetail(int noticeId) async {
    // DataSource에서 데이터 조회
    final detailModel = await _remoteDataSource.getPostDetail(noticeId);

    // Model -> Entity 변환
    return detailModel.toEntity();
  }
}
