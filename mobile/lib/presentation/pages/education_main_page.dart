import 'package:flutter/material.dart';
import 'package:mobile/app_router.dart';
import 'package:mobile/core/theme/app_colors.dart';
import 'package:mobile/core/theme/app_typography.dart';
import 'package:mobile/domain/entities/education_category.dart';
import 'package:mobile/presentation/widgets/education/education_category_card.dart';

/// 교육 메인 화면
///
/// Heroku 레거시 교육 메인(community/edu/main.jsp) 디자인에 정합.
/// 안내 문구와 4개의 교육 카테고리를 2x2 그리드로 표시한다.
/// 카테고리를 선택하면 해당 카테고리의 게시물 목록 화면으로 이동한다.
class EducationMainPage extends StatelessWidget {
  const EducationMainPage({super.key});

  /// 카테고리 선택 핸들러
  void _onCategoryTap(BuildContext context, EducationCategory category) {
    AppRouter.navigateTo(
      context,
      AppRouter.educationList,
      arguments: category,
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppColors.background,
      appBar: AppBar(
        backgroundColor: AppColors.white,
        foregroundColor: AppColors.textPrimary,
        elevation: 0,
        title: Text(
          '교육',
          style: AppTypography.headlineLarge.copyWith(
            color: AppColors.textPrimary,
          ),
        ),
        centerTitle: true,
        bottom: PreferredSize(
          preferredSize: const Size.fromHeight(1),
          child: Container(height: 1, color: AppColors.divider),
        ),
      ),
      body: SingleChildScrollView(
        // 그림자가 그리드 셀 경계에서 잘리지 않도록 좌우 여백은 li margin(5px)과 맞춤
        padding: const EdgeInsets.fromLTRB(15, 30, 15, 24),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            // 안내 문구
            const _EducationIntro(),
            const SizedBox(height: 20),

            // 카테고리 그리드 (2x2)
            GridView.count(
              crossAxisCount: 2,
              shrinkWrap: true,
              physics: const NeverScrollableScrollPhysics(),
              clipBehavior: Clip.none, // 카드 드롭섀도 클리핑 방지
              mainAxisSpacing: 20,
              crossAxisSpacing: 10,
              childAspectRatio: 1.1,
              children: EducationCategory.values
                  .map(
                    (category) => EducationCategoryCard(
                      category: category,
                      onTap: () => _onCategoryTap(context, category),
                    ),
                  )
                  .toList(),
            ),
          ],
        ),
      ),
    );
  }
}

/// 교육 메인 안내 문구
///
/// 레거시 .edu_main .text_wrap 정합 — "노하우와 꼭 필요한 알짜정보" 구간에
/// 노란 형광펜(strong::after, #FFE40C) 효과를 적용한다.
class _EducationIntro extends StatelessWidget {
  const _EducationIntro();

  static const TextStyle _base = TextStyle(
    color: AppColors.textPrimary,
    fontSize: 16,
    height: 1.6,
    fontWeight: FontWeight.w400,
    letterSpacing: -0.5,
  );

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.center,
      children: [
        const Text('이럴 땐 어떻게 해야 할까요?', style: _base, textAlign: TextAlign.center),
        // 형광펜 하이라이트가 들어가는 줄
        FittedBox(
          fit: BoxFit.scaleDown,
          child: Row(
            mainAxisSize: MainAxisSize.min,
            children: [
              const Text('현장에서의 ', style: _base),
              _Highlighted(
                '노하우와 꼭 필요한 알짜정보',
                style: _base.copyWith(fontWeight: FontWeight.w800),
              ),
              const Text('를', style: _base),
            ],
          ),
        ),
        const Text('지금 여기서 확인하세요!', style: _base, textAlign: TextAlign.center),
      ],
    );
  }
}

/// 노란 형광펜 하이라이트 텍스트
///
/// 레거시 strong::after { background:#FFE40C; height:6px; bottom:3px } 정합 —
/// 텍스트 하단에 노란 막대를 겹쳐 그린다.
class _Highlighted extends StatelessWidget {
  final String text;
  final TextStyle style;

  const _Highlighted(this.text, {required this.style});

  @override
  Widget build(BuildContext context) {
    return Stack(
      alignment: Alignment.bottomCenter,
      children: [
        Positioned(
          left: 0,
          right: 0,
          bottom: 3,
          child: Container(height: 7, color: AppColors.legacyYellow),
        ),
        Text(text, style: style),
      ],
    );
  }
}
