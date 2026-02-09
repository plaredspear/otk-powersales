import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/theme/app_colors.dart';
import '../../core/theme/app_spacing.dart';
import '../../core/theme/app_typography.dart';
import '../providers/auth_provider.dart';

/// 비밀번호 변경 화면
///
/// 초기 비밀번호(otg1) 사용자가 최초 로그인 시 비밀번호 변경을 강제합니다.
/// 뒤로가기 불가 (초기 비밀번호 변경 시).
class ChangePasswordScreen extends ConsumerStatefulWidget {
  const ChangePasswordScreen({super.key});

  @override
  ConsumerState<ChangePasswordScreen> createState() =>
      _ChangePasswordScreenState();
}

class _ChangePasswordScreenState extends ConsumerState<ChangePasswordScreen> {
  final _formKey = GlobalKey<FormState>();
  final _currentPasswordController = TextEditingController();
  final _newPasswordController = TextEditingController();
  final _confirmPasswordController = TextEditingController();
  final _newPasswordFocusNode = FocusNode();
  final _confirmPasswordFocusNode = FocusNode();

  @override
  void dispose() {
    _currentPasswordController.dispose();
    _newPasswordController.dispose();
    _confirmPasswordController.dispose();
    _newPasswordFocusNode.dispose();
    _confirmPasswordFocusNode.dispose();
    super.dispose();
  }

  /// 비밀번호 변경 실행
  Future<void> _handleChangePassword() async {
    if (!_formKey.currentState!.validate()) return;

    ref.read(authProvider.notifier).changePassword(
          currentPassword: _currentPasswordController.text,
          newPassword: _newPasswordController.text,
        );
  }

  /// 새 비밀번호 유효성 검증
  String? _validateNewPassword(String? value) {
    if (value == null || value.isEmpty) {
      return '새 비밀번호를 입력해주세요';
    }
    if (value.length < 4) {
      return '비밀번호는 4글자 이상이어야 합니다';
    }
    if (value.split('').toSet().length == 1) {
      return '동일한 문자의 반복은 사용할 수 없습니다';
    }
    return null;
  }

  /// 비밀번호 확인 유효성 검증
  String? _validateConfirmPassword(String? value) {
    if (value == null || value.isEmpty) {
      return '비밀번호 확인을 입력해주세요';
    }
    if (value != _newPasswordController.text) {
      return '비밀번호가 일치하지 않습니다';
    }
    return null;
  }

  @override
  Widget build(BuildContext context) {
    final authState = ref.watch(authProvider);

    // 에러 메시지 SnackBar 표시
    ref.listen<String?>(
      authProvider.select((state) => state.errorMessage),
      (previous, next) {
        if (next != null && next.isNotEmpty) {
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(
              content: Text(next),
              backgroundColor: AppColors.error,
              behavior: SnackBarBehavior.floating,
            ),
          );
          ref.read(authProvider.notifier).clearError();
        }
      },
    );

    // ignore: deprecated_member_use
    return WillPopScope(
      onWillPop: () async => false, // 뒤로가기 비활성화
      child: Scaffold(
        backgroundColor: AppColors.background,
        appBar: AppBar(
          title: const Text('비밀번호 변경'),
          automaticallyImplyLeading: false, // 뒤로가기 버튼 숨김
        ),
        body: SafeArea(
          child: SingleChildScrollView(
            padding: AppSpacing.screenHorizontal,
            child: Form(
              key: _formKey,
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  const SizedBox(height: AppSpacing.xxl),

                  // 안내 문구
                  _buildGuideText(),
                  const SizedBox(height: AppSpacing.xxxl),

                  // 현재 비밀번호
                  _buildCurrentPasswordField(),
                  const SizedBox(height: AppSpacing.lg),

                  // 새 비밀번호
                  _buildNewPasswordField(),
                  const SizedBox(height: AppSpacing.sm),

                  // 비밀번호 규칙 안내
                  _buildPasswordRules(),
                  const SizedBox(height: AppSpacing.lg),

                  // 새 비밀번호 확인
                  _buildConfirmPasswordField(),
                  const SizedBox(height: AppSpacing.xxxl),

                  // 변경 버튼
                  _buildChangeButton(authState.isLoading),
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }

  /// 안내 문구
  Widget _buildGuideText() {
    return Container(
      padding: AppSpacing.cardPadding,
      decoration: BoxDecoration(
        color: AppColors.secondaryLight.withAlpha(26),
        borderRadius: AppSpacing.cardBorderRadius,
        border: Border.all(
          color: AppColors.secondary.withAlpha(51),
        ),
      ),
      child: Row(
        children: [
          const Icon(
            Icons.info_outline,
            color: AppColors.secondary,
            size: AppSpacing.iconSize,
          ),
          const SizedBox(width: AppSpacing.md),
          Expanded(
            child: Text(
              '비밀번호를 변경해주세요.\n보안을 위해 초기 비밀번호는 반드시 변경해야 합니다.',
              style: AppTypography.bodyMedium.copyWith(
                color: AppColors.secondary,
              ),
            ),
          ),
        ],
      ),
    );
  }

  /// 현재 비밀번호 입력 필드
  Widget _buildCurrentPasswordField() {
    return TextFormField(
      controller: _currentPasswordController,
      obscureText: true,
      decoration: InputDecoration(
        labelText: '현재 비밀번호',
        hintText: '현재 비밀번호를 입력해주세요',
        prefixIcon: const Icon(Icons.lock_outline),
        filled: true,
        fillColor: AppColors.surfaceVariant,
        border: OutlineInputBorder(
          borderRadius: AppSpacing.inputBorderRadius,
          borderSide: BorderSide.none,
        ),
        focusedBorder: OutlineInputBorder(
          borderRadius: AppSpacing.inputBorderRadius,
          borderSide: const BorderSide(color: AppColors.secondary, width: 2),
        ),
        errorBorder: OutlineInputBorder(
          borderRadius: AppSpacing.inputBorderRadius,
          borderSide: const BorderSide(color: AppColors.error),
        ),
        focusedErrorBorder: OutlineInputBorder(
          borderRadius: AppSpacing.inputBorderRadius,
          borderSide: const BorderSide(color: AppColors.error, width: 2),
        ),
      ),
      textInputAction: TextInputAction.next,
      onFieldSubmitted: (_) {
        FocusScope.of(context).requestFocus(_newPasswordFocusNode);
      },
      validator: (value) {
        if (value == null || value.isEmpty) {
          return '현재 비밀번호를 입력해주세요';
        }
        return null;
      },
    );
  }

  /// 새 비밀번호 입력 필드
  Widget _buildNewPasswordField() {
    return TextFormField(
      controller: _newPasswordController,
      focusNode: _newPasswordFocusNode,
      obscureText: true,
      decoration: InputDecoration(
        labelText: '새 비밀번호',
        hintText: '새 비밀번호를 입력해주세요',
        prefixIcon: const Icon(Icons.lock_outline),
        filled: true,
        fillColor: AppColors.surfaceVariant,
        border: OutlineInputBorder(
          borderRadius: AppSpacing.inputBorderRadius,
          borderSide: BorderSide.none,
        ),
        focusedBorder: OutlineInputBorder(
          borderRadius: AppSpacing.inputBorderRadius,
          borderSide: const BorderSide(color: AppColors.secondary, width: 2),
        ),
        errorBorder: OutlineInputBorder(
          borderRadius: AppSpacing.inputBorderRadius,
          borderSide: const BorderSide(color: AppColors.error),
        ),
        focusedErrorBorder: OutlineInputBorder(
          borderRadius: AppSpacing.inputBorderRadius,
          borderSide: const BorderSide(color: AppColors.error, width: 2),
        ),
      ),
      textInputAction: TextInputAction.next,
      onFieldSubmitted: (_) {
        FocusScope.of(context).requestFocus(_confirmPasswordFocusNode);
      },
      onChanged: (_) {
        // 실시간 유효성 피드백을 위해 setState
        setState(() {});
      },
      validator: _validateNewPassword,
    );
  }

  /// 비밀번호 규칙 안내
  Widget _buildPasswordRules() {
    final password = _newPasswordController.text;
    final hasMinLength = password.length >= 4;
    final hasNoRepeating =
        password.isEmpty || password.split('').toSet().length > 1;

    return Padding(
      padding: const EdgeInsets.only(left: AppSpacing.xs),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          _buildRuleItem('4글자 이상', hasMinLength, password.isNotEmpty),
          const SizedBox(height: AppSpacing.xs),
          _buildRuleItem(
              '동일 문자 반복 불가', hasNoRepeating, password.isNotEmpty),
        ],
      ),
    );
  }

  /// 비밀번호 규칙 항목
  Widget _buildRuleItem(String text, bool isValid, bool showStatus) {
    final Color color;
    final IconData icon;

    if (!showStatus) {
      color = AppColors.textTertiary;
      icon = Icons.circle_outlined;
    } else if (isValid) {
      color = AppColors.success;
      icon = Icons.check_circle;
    } else {
      color = AppColors.error;
      icon = Icons.cancel;
    }

    return Row(
      children: [
        Icon(icon, size: 16, color: color),
        const SizedBox(width: AppSpacing.xs),
        Text(
          text,
          style: AppTypography.bodySmall.copyWith(color: color),
        ),
      ],
    );
  }

  /// 새 비밀번호 확인 입력 필드
  Widget _buildConfirmPasswordField() {
    return TextFormField(
      controller: _confirmPasswordController,
      focusNode: _confirmPasswordFocusNode,
      obscureText: true,
      decoration: InputDecoration(
        labelText: '새 비밀번호 확인',
        hintText: '새 비밀번호를 다시 입력해주세요',
        prefixIcon: const Icon(Icons.lock_outline),
        filled: true,
        fillColor: AppColors.surfaceVariant,
        border: OutlineInputBorder(
          borderRadius: AppSpacing.inputBorderRadius,
          borderSide: BorderSide.none,
        ),
        focusedBorder: OutlineInputBorder(
          borderRadius: AppSpacing.inputBorderRadius,
          borderSide: const BorderSide(color: AppColors.secondary, width: 2),
        ),
        errorBorder: OutlineInputBorder(
          borderRadius: AppSpacing.inputBorderRadius,
          borderSide: const BorderSide(color: AppColors.error),
        ),
        focusedErrorBorder: OutlineInputBorder(
          borderRadius: AppSpacing.inputBorderRadius,
          borderSide: const BorderSide(color: AppColors.error, width: 2),
        ),
      ),
      textInputAction: TextInputAction.done,
      onFieldSubmitted: (_) => _handleChangePassword(),
      validator: _validateConfirmPassword,
    );
  }

  /// 변경 버튼
  Widget _buildChangeButton(bool isLoading) {
    return SizedBox(
      height: AppSpacing.buttonHeight,
      child: ElevatedButton(
        onPressed: isLoading ? null : _handleChangePassword,
        style: ElevatedButton.styleFrom(
          backgroundColor: AppColors.primary,
          foregroundColor: AppColors.onPrimary,
          disabledBackgroundColor: AppColors.primaryLight,
          shape: RoundedRectangleBorder(
            borderRadius: AppSpacing.buttonBorderRadius,
          ),
          elevation: 0,
        ),
        child: isLoading
            ? const SizedBox(
                width: 24,
                height: 24,
                child: CircularProgressIndicator(
                  strokeWidth: 2,
                  color: AppColors.onPrimary,
                ),
              )
            : Text(
                '비밀번호 변경',
                style: AppTypography.labelLarge.copyWith(
                  fontSize: 16,
                  fontWeight: FontWeight.w600,
                ),
              ),
      ),
    );
  }
}
