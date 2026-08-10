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

/// NBSP(`&nbsp;`). 일반 공백과 달리 trim 대상이 아니라 따로 걷어내야 한다.
/// 소스에 리터럴로 두면 편집 과정에서 일반 공백으로 바뀌기 쉬워 이스케이프로 명시한다.
const String _nbsp = ' ';

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
        // 웹 에디터(Quill)가 내보내는 빈 줄은 두 형태이고 둘 다 그대로 두면 사라진다.
        //  - `<p></p>` (실제 저장 형태): HtmlWidget 이 높이 0 으로 렌더해 아예 안 보인다.
        //  - `<p><br></p>`: 1em 짜리 HeightPlaceholder 로 특수 처리되는데, 이어붙은
        //    HeightPlaceholder 들이 column 조립 단계에서 mergeWith 로 병합되고 병합 규칙이
        //    max 채택이라 개수와 무관하게 한 칸이 된다.
        // 어느 쪽이든 문단 margin 을 0 으로 주는 방식으로는 해결되지 않는다(빈 줄 본체가 문제).
        //
        // 빈 문단을 여기서 직접 SizedBox 로 렌더하면 `<p>` 의 BuildOp 자체를 타지 않아
        // (customWidget 반환 시 block bit 로 바로 append) 두 경로를 모두 벗어난다.
        //
        // 단, 텍스트가 없어도 `<img>` 등 눈에 보이는 요소를 품은 문단은 제외해야 한다 —
        // 빈 줄로 오인해 SizedBox 로 대체하면 그 요소가 통째로 사라진다.
        if (element.localName == 'p' &&
            element.text.replaceAll(_nbsp, ' ').trim().isEmpty &&
            element.children.every((child) => child.localName == 'br')) {
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
