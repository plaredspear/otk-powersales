import '../../core/utils/error_utils.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/theme/app_colors.dart';
import '../../core/theme/app_spacing.dart';
import '../../core/theme/app_typography.dart';
import '../providers/auth_provider.dart';

/// GPS 사용 동의 화면
///
/// GPS 위치정보 수집 동의 약관을 표시하고 동의를 기록합니다.
/// - 뒤로가기 비활성화 (동의 없이 우회 불가)
/// - 약관 조회 API로 contents 표시
/// - 동의 버튼 탭 시 동의 기록 API 호출 → 새 access_token 저장
class GpsConsentScreen extends ConsumerStatefulWidget {
  const GpsConsentScreen({super.key});

  @override
  ConsumerState<GpsConsentScreen> createState() => _GpsConsentScreenState();
}

class _GpsConsentScreenState extends ConsumerState<GpsConsentScreen> {
  bool _isLoading = true;
  bool _isSaving = false;
  String? _errorMessage;
  String _contents = '';
  String? _agreementNumber;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      _loadTerms();
    });
  }

  Future<void> _loadTerms() async {
    setState(() {
      _isLoading = true;
      _errorMessage = null;
    });

    try {
      final repository = ref.read(authRepositoryProvider);
      final terms = await repository.getGpsConsentTerms();
      setState(() {
        _contents = terms.contents;
        _agreementNumber = terms.agreementNumber;
        _isLoading = false;
      });
    } catch (e) {
      setState(() {
        _errorMessage = extractErrorMessage(e);
        _isLoading = false;
      });
    }
  }

  Future<void> _handleAgree() async {
    setState(() {
      _isSaving = true;
    });

    try {
      await ref.read(authProvider.notifier).recordGpsConsent(
            agreementNumber: _agreementNumber,
          );
      // 동의 완료 후 pop (원래 화면으로 복귀)
      if (mounted) {
        Navigator.of(context).pop();
      }
    } catch (e) {
      setState(() {
        _isSaving = false;
      });
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(extractErrorMessage(e)),
            backgroundColor: AppColors.error,
            behavior: SnackBarBehavior.floating,
          ),
        );
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    // ignore: deprecated_member_use
    return WillPopScope(
      onWillPop: () async => false, // 뒤로가기 비활성화
      child: Scaffold(
        backgroundColor: AppColors.background,
        appBar: AppBar(
          title: const Text('GPS 사용 동의'),
          automaticallyImplyLeading: false, // 뒤로가기 버튼 숨김
        ),
        body: SafeArea(
          child: _buildBody(),
        ),
      ),
    );
  }

  Widget _buildBody() {
    if (_isLoading) {
      return const Center(
        child: CircularProgressIndicator(),
      );
    }

    if (_errorMessage != null) {
      return _buildErrorView();
    }

    return Column(
      children: [
        Expanded(
          child: SingleChildScrollView(
            padding: AppSpacing.screenHorizontal,
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                const SizedBox(height: AppSpacing.xxl),
                _buildTitle(),
                const SizedBox(height: AppSpacing.lg),
                _buildTermsContent(),
                const SizedBox(height: AppSpacing.xxl),
              ],
            ),
          ),
        ),
        _buildAgreeButton(),
      ],
    );
  }

  Widget _buildTitle() {
    return Text(
      '개인정보, 위치정보의 수집 및\n이용에 대한 동의서',
      style: AppTypography.headlineSmall.copyWith(
        fontWeight: FontWeight.w600,
      ),
      textAlign: TextAlign.center,
    );
  }

  Widget _buildTermsContent() {
    return Container(
      padding: AppSpacing.cardPadding,
      decoration: BoxDecoration(
        color: AppColors.surface,
        borderRadius: AppSpacing.cardBorderRadius,
        border: Border.all(color: AppColors.border),
      ),
      child: Text(
        _contents,
        style: AppTypography.bodyMedium.copyWith(
          height: 1.6,
        ),
      ),
    );
  }

  Widget _buildAgreeButton() {
    return Container(
      padding: const EdgeInsets.all(AppSpacing.lg),
      decoration: BoxDecoration(
        color: AppColors.background,
        boxShadow: [
          BoxShadow(
            color: Colors.black.withAlpha(13),
            blurRadius: 4,
            offset: const Offset(0, -2),
          ),
        ],
      ),
      child: SizedBox(
        height: AppSpacing.buttonHeight,
        child: ElevatedButton(
          onPressed: _isSaving ? null : _handleAgree,
          style: ElevatedButton.styleFrom(
            backgroundColor: AppColors.primary,
            foregroundColor: AppColors.onPrimary,
            disabledBackgroundColor: AppColors.primaryLight,
            shape: RoundedRectangleBorder(
              borderRadius: AppSpacing.buttonBorderRadius,
            ),
            elevation: 0,
          ),
          child: _isSaving
              ? const SizedBox(
                  width: 24,
                  height: 24,
                  child: CircularProgressIndicator(
                    strokeWidth: 2,
                    color: AppColors.onPrimary,
                  ),
                )
              : Text(
                  '동의',
                  style: AppTypography.labelLarge.copyWith(
                    fontSize: 16,
                    fontWeight: FontWeight.w600,
                  ),
                ),
        ),
      ),
    );
  }

  Widget _buildErrorView() {
    return Center(
      child: Padding(
        padding: AppSpacing.screenHorizontal,
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(
              Icons.error_outline,
              size: 48,
              color: Colors.grey[400],
            ),
            const SizedBox(height: AppSpacing.lg),
            Text(
              _errorMessage!,
              style: AppTypography.bodyMedium.copyWith(
                color: AppColors.textSecondary,
              ),
              textAlign: TextAlign.center,
            ),
            const SizedBox(height: AppSpacing.xxl),
            SizedBox(
              height: AppSpacing.buttonHeight,
              child: ElevatedButton(
                onPressed: _loadTerms,
                style: ElevatedButton.styleFrom(
                  backgroundColor: AppColors.primary,
                  foregroundColor: AppColors.onPrimary,
                  shape: RoundedRectangleBorder(
                    borderRadius: AppSpacing.buttonBorderRadius,
                  ),
                  elevation: 0,
                ),
                child: const Text('재시도'),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
