import 'package:cached_network_image/cached_network_image.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_widget_from_html_core/flutter_widget_from_html_core.dart';
import 'package:intl/intl.dart';
import 'package:url_launcher/url_launcher.dart';
import 'package:mobile/core/theme/app_colors.dart';
import 'package:mobile/core/theme/app_spacing.dart';
import 'package:mobile/core/theme/app_typography.dart';

import '../../domain/entities/notice_category.dart';
import '../providers/notice_detail_provider.dart';
import '../providers/notice_detail_state.dart';

/// 공지사항 상세 화면
///
/// 공지사항 제목, 분류, 날짜, 본문, 이미지를 표시합니다.
class NoticeDetailPage extends ConsumerStatefulWidget {
  /// 공지사항 ID
  final int noticeId;

  const NoticeDetailPage({super.key, required this.noticeId});

  @override
  ConsumerState<NoticeDetailPage> createState() => _NoticeDetailPageState();
}

class _NoticeDetailPageState extends ConsumerState<NoticeDetailPage> {
  @override
  void initState() {
    super.initState();
    // 상세 정보 로딩
    WidgetsBinding.instance.addPostFrameCallback((_) {
      ref.read(noticeDetailProvider.notifier).loadDetail(widget.noticeId);
    });
  }

  /// 날짜를 "yyyy.MM.dd(요일)" 형식으로 변환
  String _formatDate(DateTime date) {
    final formatter = DateFormat('yyyy.MM.dd(E)', 'ko_KR');
    return formatter.format(date);
  }

  @override
  Widget build(BuildContext context) {
    final state = ref.watch(noticeDetailProvider);
    final notifier = ref.read(noticeDetailProvider.notifier);

    return Scaffold(
      backgroundColor: AppColors.background,
      appBar: AppBar(
        title: const Text('공지 상세'),
        backgroundColor: AppColors.white,
        foregroundColor: AppColors.textPrimary,
        elevation: 0,
      ),
      body: _buildContent(state, notifier),
    );
  }

  Widget _buildContent(NoticeDetailState state, NoticeDetailNotifier notifier) {
    // 로딩 중
    if (state.isLoading) {
      return const Center(child: CircularProgressIndicator());
    }

    // 에러
    if (state.errorMessage != null) {
      return Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            const Icon(Icons.error_outline, size: 48, color: AppColors.error),
            const SizedBox(height: AppSpacing.md),
            Text(
              state.errorMessage!,
              style: const TextStyle(color: AppColors.textSecondary),
            ),
            const SizedBox(height: AppSpacing.md),
            ElevatedButton(
              onPressed: () => notifier.refresh(widget.noticeId),
              child: const Text('다시 시도'),
            ),
          ],
        ),
      );
    }

    // 데이터 없음
    if (!state.hasData || state.detail == null) {
      return const Center(
        child: Text(
          '공지사항을 찾을 수 없습니다',
          style: TextStyle(color: AppColors.textSecondary),
        ),
      );
    }

    final detail = state.detail!;

    return SingleChildScrollView(
      padding: const EdgeInsets.all(AppSpacing.lg),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          // 분류 태그
          Container(
            padding: const EdgeInsets.symmetric(
              horizontal: AppSpacing.md,
              vertical: AppSpacing.xs,
            ),
            decoration: BoxDecoration(
              color: detail.category == NoticeCategory.company
                  ? AppColors.primaryLight
                  : AppColors.secondaryLight,
              borderRadius: BorderRadius.circular(AppSpacing.radiusSm),
            ),
            child: Text(
              detail.categoryName,
              style: AppTypography.labelSmall.copyWith(
                color: detail.category == NoticeCategory.company
                    ? AppColors.primary
                    : AppColors.white,
                fontWeight: FontWeight.w700,
              ),
            ),
          ),

          const SizedBox(height: AppSpacing.md),

          // 제목
          Text(
            detail.title,
            style: AppTypography.headlineSmall.copyWith(
              color: AppColors.textPrimary,
              fontWeight: FontWeight.bold,
            ),
          ),

          const SizedBox(height: AppSpacing.sm),

          // 등록일
          Text(
            _formatDate(detail.createdAt),
            style: AppTypography.bodySmall.copyWith(
              color: AppColors.textTertiary,
            ),
          ),

          const Divider(height: AppSpacing.xl, color: AppColors.border),

          // 본문 (HTML 렌더링)
          // 본문 인라인 이미지는 backend 가 presigned URL 로 rewrite 해서 내려준다(만료/매 조회 변동).
          // <img> 를 가로채 CachedNetworkImage 로 렌더하되 cacheKey 를 data-refid(안정 식별자)로 지정해
          // presigned URL 변동과 무관하게 캐시를 재사용한다.
          HtmlWidget(
            detail.content,
            textStyle: AppTypography.bodyMedium.copyWith(
              color: AppColors.textPrimary,
              height: 1.6,
            ),
            // 본문 링크(<a href>) 탭 시 외부 브라우저로 연다. 색상/정렬 등 인라인 style 은 기본 렌더.
            onTapUrl: (url) async {
              final uri = Uri.tryParse(url);
              if (uri == null) return false;
              return launchUrl(uri, mode: LaunchMode.externalApplication);
            },
            customWidgetBuilder: (element) {
              if (element.localName != 'img') return null;
              final src = element.attributes['src'];
              // placeholder(notice-image://) 잔존 = rewrite 미적용/실패분 → 깨진 이미지 박스.
              if (src == null || !src.startsWith('http')) {
                return _brokenImageBox();
              }
              final refid = element.attributes['data-refid'];
              return Padding(
                padding: const EdgeInsets.symmetric(vertical: AppSpacing.sm),
                child: ClipRRect(
                  borderRadius: BorderRadius.circular(AppSpacing.radiusMd),
                  child: CachedNetworkImage(
                    imageUrl: src,
                    cacheKey: refid, // null 이면 imageUrl 로 fallback
                    fit: BoxFit.fitWidth,
                    placeholder: (context, url) => const Center(
                      child: Padding(
                        padding: EdgeInsets.all(AppSpacing.lg),
                        child: CircularProgressIndicator(),
                      ),
                    ),
                    errorWidget: (context, url, error) => _brokenImageBox(),
                  ),
                ),
              );
            },
          ),

          // 이미지 목록
          if (detail.images.isNotEmpty) ...[
            const SizedBox(height: AppSpacing.xl),
            // 첨부 이미지도 presigned URL → cacheKey 를 안정적 id 로 지정해 캐싱.
            ...detail.images.map((image) {
              return Padding(
                padding: const EdgeInsets.only(bottom: AppSpacing.md),
                child: ClipRRect(
                  borderRadius: BorderRadius.circular(AppSpacing.radiusMd),
                  child: CachedNetworkImage(
                    imageUrl: image.url,
                    cacheKey: 'notice-image-${image.id}',
                    fit: BoxFit.cover,
                    placeholder: (context, url) => const Center(
                      child: Padding(
                        padding: EdgeInsets.all(AppSpacing.lg),
                        child: CircularProgressIndicator(),
                      ),
                    ),
                    errorWidget: (context, url, error) => _brokenImageBox(),
                  ),
                ),
              );
            }),
          ],
        ],
      ),
    );
  }

  /// 이미지 로드 실패 / placeholder 잔존 시 표시할 깨진 이미지 박스.
  Widget _brokenImageBox() {
    return Container(
      height: 200,
      color: AppColors.surface,
      child: const Center(
        child: Icon(
          Icons.broken_image,
          size: 48,
          color: AppColors.textTertiary,
        ),
      ),
    );
  }
}
