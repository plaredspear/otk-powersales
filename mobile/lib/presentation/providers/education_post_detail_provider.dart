import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:mobile/domain/entities/education_post_detail.dart';
import 'package:mobile/domain/usecases/get_education_post_detail_usecase.dart';
import 'package:mobile/presentation/providers/education_posts_provider.dart';

/// 교육 게시물 상세 조회 UseCase Provider
final getEducationPostDetailUseCaseProvider =
    Provider<GetEducationPostDetailUseCase>((ref) {
  final repository = ref.watch(educationRepositoryProvider);
  return GetEducationPostDetailUseCase(repository);
});

/// 교육 게시물 상세 Provider (Family)
///
/// 게시물 ID를 받아 해당 게시물의 상세 정보를 조회한다.
/// [postId] 게시물 ID
final educationPostDetailProvider =
    FutureProvider.family<EducationPostDetail, int>((ref, postId) async {
  final useCase = ref.watch(getEducationPostDetailUseCaseProvider);
  return await useCase(postId);
});
