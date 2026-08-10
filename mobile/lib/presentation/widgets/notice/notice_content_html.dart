import 'package:cached_network_image/cached_network_image.dart';
import 'package:flutter/material.dart';
import 'package:flutter_widget_from_html_core/flutter_widget_from_html_core.dart';
import 'package:url_launcher/url_launcher.dart';
import 'package:mobile/core/theme/app_colors.dart';
import 'package:mobile/core/theme/app_spacing.dart';
import 'package:mobile/core/theme/app_typography.dart';

/// 본문 빈 줄 1개의 높이.
///
/// HtmlWidget 이 `<p><br></p>` 에 부여하는 1em 과 동일하게 맞춘다
/// (본문 textStyle 은 [AppTypography.bodyMedium] = fontSize 14).
const double kNoticeBlankLineHeight = 14;

/// 공지 본문 HTML 렌더러.
///
/// 본문 인라인 이미지는 backend 가 presigned URL 로 rewrite 해서 내려준다(만료/매 조회 변동).
/// `<img>` 를 가로채 [CachedNetworkImage] 로 렌더하되 cacheKey 를 data-refid(안정 식별자)로
/// 지정해 presigned URL 변동과 무관하게 캐시를 재사용한다.
class NoticeContentHtml extends StatelessWidget {
  /// 공지 본문 HTML.
  final String html;

  const NoticeContentHtml({super.key, required this.html});

  @override
  Widget build(BuildContext context) {
    return HtmlWidget(
      html,
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
        // 작성자가 넣은 빈 줄을 넣은 개수만큼 보존한다.
        //
        // 웹 에디터(Quill)는 빈 줄을 `<p><br></p>` 로 내보낸다. HtmlWidget 은 이 형태를
        // 1em 짜리 HeightPlaceholder 위젯으로 특수 처리하는데, 이어붙은 HeightPlaceholder
        // 들은 column 조립 단계에서 mergeWith 로 병합되고 병합 규칙이 max 채택이라 같은
        // 1em 끼리는 한 칸으로 합쳐진다. 문단 margin 을 0 으로 줘도 병합 대상은 빈 줄
        // 본체라서 막히지 않는다.
        // 빈 문단을 여기서 직접 SizedBox 로 렌더하면 `<p>` 의 BuildOp 자체를 타지 않아
        // (customWidget 반환 시 block bit 로 바로 append) 병합 경로를 벗어난다.
        if (element.localName == 'p' &&
            element.text.trim().isEmpty &&
            element.children.length == 1 &&
            element.children.first.localName == 'br') {
          return const SizedBox(height: kNoticeBlankLineHeight);
        }
        if (element.localName != 'img') return null;
        final src = element.attributes['src'];
        // placeholder(notice-image://) 잔존 = rewrite 미적용/실패분 → 깨진 이미지 박스.
        if (src == null || !src.startsWith('http')) {
          return const NoticeBrokenImageBox();
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
              errorWidget: (context, url, error) => const NoticeBrokenImageBox(),
            ),
          ),
        );
      },
    );
  }
}

/// 이미지 로드 실패 / placeholder 잔존 시 표시할 깨진 이미지 박스.
class NoticeBrokenImageBox extends StatelessWidget {
  const NoticeBrokenImageBox({super.key});

  @override
  Widget build(BuildContext context) {
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
