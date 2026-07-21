import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/theme/app_colors.dart';
import '../../core/theme/app_spacing.dart';
import '../../core/theme/app_typography.dart';
import '../../domain/entities/password_validation.dart';
import '../providers/auth_provider.dart';
import '../widgets/auth/password_policy_checklist.dart';

/// 임시 비밀번호 강제 변경 화면 (Spec #584 P2-M).
///
/// 운영자가 임시 비밀번호로 리셋한 사원이 첫 로그인 시 진입한다.
/// - currentPassword 입력 없음 (백엔드는 토큰 클레임으로 강제 변경 식별)
/// - PopScope 로 OS 백버튼 차단
/// - PasswordPolicyChecklist 로 정책 실시간 표시
/// - 변경 성공 시 새 토큰으로 자동 로그인 갱신 후 홈 진입
class ChangePasswordScreen extends ConsumerStatefulWidget {
  const ChangePasswordScreen({super.key});

  @override
  ConsumerState<ChangePasswordScreen> createState() =>
      _ChangePasswordScreenState();
}

class _ChangePasswordScreenState extends ConsumerState<ChangePasswordScreen> {
  final _formKey = GlobalKey<FormState>();
  final _newPasswordController = TextEditingController();
  final _confirmPasswordController = TextEditingController();
  final _confirmPasswordFocusNode = FocusNode();

  @override
  void initState() {
    super.initState();
    _newPasswordController.addListener(_onPasswordChanged);
    _confirmPasswordController.addListener(_onPasswordChanged);
  }

  @override
  void dispose() {
    _newPasswordController.dispose();
    _confirmPasswordController.dispose();
    _confirmPasswordFocusNode.dispose();
    super.dispose();
  }

  void _onPasswordChanged() {
    setState(() {});
  }

  bool get _isFormValid {
    final pwd = _newPasswordController.text;
    final confirm = _confirmPasswordController.text;
    if (pwd.isEmpty || confirm.isEmpty) return false;
    if (pwd != confirm) return false;
    return PasswordValidation.fromPassword(pwd).isValid;
  }

  Future<void> _handleChangePassword() async {
    if (!_formKey.currentState!.validate()) return;
    if (!_isFormValid) return;

    // 강제 변경: currentPassword 미전달 (토큰 클레임으로 분기)
    await ref.read(authProvider.notifier).changePassword(
          currentPassword: null,
          newPassword: _newPasswordController.text,
        );
  }

  @override
  Widget build(BuildContext context) {
    final authState = ref.watch(authProvider);

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

    return PopScope(
      canPop: false,
      child: Scaffold(
        backgroundColor: AppColors.background,
        appBar: AppBar(
          title: const Text('비밀번호 변경'),
          automaticallyImplyLeading: false,
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
                  _buildNewPasswordField(),
                  const SizedBox(height: AppSpacing.lg),
                  _buildConfirmPasswordField(),
                  const SizedBox(height: AppSpacing.lg),
                  PasswordPolicyChecklist(
                    password: _newPasswordController.text,
                  ),
                  _buildMatchFeedback(),
                  const SizedBox(height: AppSpacing.xxxl),
                  _buildChangeButton(authState.isLoading),
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }

  Widget _buildNewPasswordField() {
    return TextFormField(
      controller: _newPasswordController,
      obscureText: true,
      cursorColor: AppColors.black,
      decoration: InputDecoration(
        labelText: '새 비밀번호',
        hintText: '새 비밀번호를 입력해주세요',
        floatingLabelStyle: const TextStyle(
          color: AppColors.secondary,
          fontWeight: FontWeight.w600,
        ),
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
      validator: (value) {
        if (value == null || value.isEmpty) {
          return '새 비밀번호를 입력해주세요';
        }
        return null;
      },
    );
  }

  Widget _buildConfirmPasswordField() {
    return TextFormField(
      controller: _confirmPasswordController,
      focusNode: _confirmPasswordFocusNode,
      obscureText: true,
      cursorColor: AppColors.black,
      decoration: InputDecoration(
        labelText: '새 비밀번호 확인',
        hintText: '새 비밀번호를 다시 입력해주세요',
        floatingLabelStyle: const TextStyle(
          color: AppColors.secondary,
          fontWeight: FontWeight.w600,
        ),
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
      validator: (value) {
        if (value == null || value.isEmpty) {
          return '비밀번호 확인을 입력해주세요';
        }
        if (value != _newPasswordController.text) {
          return '비밀번호가 일치하지 않습니다';
        }
        return null;
      },
    );
  }

  /// 새 비밀번호와 확인 값의 일치 여부 실시간 피드백.
  /// 확인 입력이 비어 있으면 표시하지 않는다.
  Widget _buildMatchFeedback() {
    if (_confirmPasswordController.text.isEmpty) {
      return const SizedBox.shrink();
    }
    final isMatch =
        _newPasswordController.text == _confirmPasswordController.text;
    final color = isMatch ? AppColors.success : AppColors.error;
    return Padding(
      padding: const EdgeInsets.only(top: AppSpacing.xs, left: AppSpacing.xs),
      child: Row(
        children: [
          Icon(
            isMatch ? Icons.check_circle : Icons.cancel,
            size: 16,
            color: color,
          ),
          const SizedBox(width: AppSpacing.xs),
          Text(
            isMatch ? '비밀번호가 일치합니다' : '비밀번호가 일치하지 않습니다',
            style: AppTypography.bodySmall.copyWith(color: color),
          ),
        ],
      ),
    );
  }

  Widget _buildChangeButton(bool isLoading) {
    final canSubmit = !isLoading && _isFormValid;
    return SizedBox(
      height: AppSpacing.buttonHeight,
      child: ElevatedButton(
        onPressed: canSubmit ? _handleChangePassword : null,
        style: ElevatedButton.styleFrom(
          backgroundColor: AppColors.primary,
          foregroundColor: AppColors.onPrimary,
          disabledBackgroundColor: AppColors.divider,
          disabledForegroundColor: AppColors.textTertiary,
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
                  fontWeight: FontWeight.w700,
                ),
              ),
      ),
    );
  }
}
