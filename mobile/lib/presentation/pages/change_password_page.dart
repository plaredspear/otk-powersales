import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/theme/app_colors.dart';
import '../../core/theme/app_spacing.dart';
import '../../core/theme/app_typography.dart';
import '../../core/utils/error_utils.dart';
import '../../domain/entities/password_validation.dart';
import '../providers/auth_provider.dart';
import '../providers/password_provider.dart';
import '../widgets/auth/password_policy_checklist.dart';

/// 비밀번호 변경 페이지 (마이페이지 진입, 단일 폼).
///
/// 레거시 Heroku 는 현재 비밀번호 확인(modify.jsp) → 새 비밀번호 입력(chgPwd.jsp) 2단계지만,
/// 백엔드 `POST /auth/change-password` 가 `currentPassword` 를 함께 검증하므로
/// 한 화면에서 현재/새/새 확인을 모두 받는다. 재인증(현재 비밀번호 확인)이라는 보안 장치는
/// 그대로 유지하면서 단계만 1개로 합쳤다.
///
/// 스타일/레이아웃은 임시 비밀번호 강제 변경 화면(ChangePasswordScreen)과 동일:
/// 둥근 filled 입력창 + 네이비 플로팅 라벨 + 검정 커서 + 정책 체크리스트 +
/// 둥근 버튼(활성 노랑 / 비활성 회색).
class ChangePasswordPage extends ConsumerStatefulWidget {
  const ChangePasswordPage({super.key});

  @override
  ConsumerState<ChangePasswordPage> createState() =>
      _ChangePasswordPageState();
}

class _ChangePasswordPageState extends ConsumerState<ChangePasswordPage> {
  final _formKey = GlobalKey<FormState>();
  final _currentPasswordController = TextEditingController();
  final _newPasswordController = TextEditingController();
  final _confirmPasswordController = TextEditingController();
  final _newPasswordFocusNode = FocusNode();
  final _confirmPasswordFocusNode = FocusNode();
  bool _isLoading = false;

  @override
  void initState() {
    super.initState();
    _currentPasswordController.addListener(_onChanged);
    _newPasswordController.addListener(_onChanged);
    _confirmPasswordController.addListener(_onChanged);
  }

  @override
  void dispose() {
    _currentPasswordController.dispose();
    _newPasswordController.dispose();
    _confirmPasswordController.dispose();
    _newPasswordFocusNode.dispose();
    _confirmPasswordFocusNode.dispose();
    super.dispose();
  }

  void _onChanged() => setState(() {});

  bool get _isFormValid {
    final current = _currentPasswordController.text;
    final pwd = _newPasswordController.text;
    final confirm = _confirmPasswordController.text;
    if (current.isEmpty || pwd.isEmpty || confirm.isEmpty) return false;
    if (pwd != confirm) return false;
    return PasswordValidation.fromPassword(pwd).isValid;
  }

  Future<void> _handleChangePassword() async {
    if (_isLoading || !_isFormValid) return;
    if (!_formKey.currentState!.validate()) return;

    setState(() => _isLoading = true);
    try {
      await ref.read(passwordChangeProvider.notifier).changePassword(
            currentPassword: _currentPasswordController.text,
            newPassword: _newPasswordController.text,
          );

      if (!mounted) return;
      await _showSuccessDialog();
      if (!mounted) return;
      Navigator.of(context).pop();
    } catch (e) {
      if (!mounted) return;
      // 현재 비밀번호 불일치는 서버 error.code 로 판별(HTTP 상태코드에 의존하지 않음).
      if (extractErrorCode(e) == 'AUTH_CURRENT_PASSWORD_MISMATCH') {
        _showErrorSnackBar('현재 비밀번호가 일치하지 않습니다.');
      } else {
        _showErrorSnackBar('비밀번호 변경에 실패했습니다. 다시 시도해주세요.');
      }
    } finally {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  Future<void> _showSuccessDialog() {
    return showDialog<void>(
      context: context,
      barrierDismissible: false,
      builder: (context) => AlertDialog(
        title: const Text('비밀번호 변경 완료'),
        content: const Text('비밀번호가 성공적으로 변경되었습니다.'),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(context).pop(),
            child: const Text('확인'),
          ),
        ],
      ),
    );
  }

  void _showErrorSnackBar(String message) {
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text(message),
        backgroundColor: AppColors.error,
        behavior: SnackBarBehavior.floating,
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final employeeCode = ref.watch(authProvider).user?.employeeCode ?? '';

    return Scaffold(
      backgroundColor: AppColors.background,
      appBar: AppBar(
        title: const Text('비밀번호 변경'),
        leading: IconButton(
          icon: const Icon(Icons.arrow_back),
          onPressed: () => Navigator.of(context).pop(),
        ),
      ),
      body: SafeArea(
        child: SingleChildScrollView(
          padding: AppSpacing.screenHorizontal,
          child: Form(
            key: _formKey,
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                const SizedBox(height: AppSpacing.xl),
                Text(
                  '정보 변경을 위해,\n현재 비밀번호와 새 비밀번호를 입력하세요.',
                  style: AppTypography.bodyMedium.copyWith(
                    height: 1.4,
                    color: AppColors.textSecondary,
                  ),
                ),
                const SizedBox(height: AppSpacing.xl),
                _buildIdField(employeeCode),
                const SizedBox(height: AppSpacing.lg),
                _buildCurrentPasswordField(),
                const SizedBox(height: AppSpacing.lg),
                _buildNewPasswordField(),
                const SizedBox(height: AppSpacing.lg),
                _buildConfirmPasswordField(),
                const SizedBox(height: AppSpacing.lg),
                PasswordPolicyChecklist(
                  password: _newPasswordController.text,
                ),
                _buildMatchFeedback(),
                const SizedBox(height: AppSpacing.xxxl),
                _buildChangeButton(),
              ],
            ),
          ),
        ),
      ),
    );
  }

  /// 아이디 (읽기 전용) — filled 스타일, 비활성 표시.
  Widget _buildIdField(String employeeCode) {
    return TextFormField(
      initialValue: employeeCode,
      readOnly: true,
      enabled: false,
      style: const TextStyle(color: AppColors.textSecondary),
      decoration: InputDecoration(
        labelText: '아이디',
        prefixIcon: const Icon(Icons.badge_outlined),
        filled: true,
        fillColor: AppColors.surfaceVariant,
        border: OutlineInputBorder(
          borderRadius: AppSpacing.inputBorderRadius,
          borderSide: BorderSide.none,
        ),
        disabledBorder: OutlineInputBorder(
          borderRadius: AppSpacing.inputBorderRadius,
          borderSide: BorderSide.none,
        ),
      ),
    );
  }

  Widget _buildCurrentPasswordField() {
    return TextFormField(
      controller: _currentPasswordController,
      obscureText: true,
      autofocus: true,
      cursorColor: AppColors.black,
      decoration: _passwordDecoration(
        label: '현재 비밀번호',
        hint: '현재 비밀번호를 입력해주세요',
      ),
      textInputAction: TextInputAction.next,
      onFieldSubmitted: (_) =>
          FocusScope.of(context).requestFocus(_newPasswordFocusNode),
      validator: (value) {
        if (value == null || value.isEmpty) {
          return '현재 비밀번호를 입력해주세요';
        }
        return null;
      },
    );
  }

  Widget _buildNewPasswordField() {
    return TextFormField(
      controller: _newPasswordController,
      focusNode: _newPasswordFocusNode,
      obscureText: true,
      cursorColor: AppColors.black,
      decoration: _passwordDecoration(
        label: '새 비밀번호',
        hint: '새 비밀번호를 입력해주세요',
      ),
      textInputAction: TextInputAction.next,
      onFieldSubmitted: (_) =>
          FocusScope.of(context).requestFocus(_confirmPasswordFocusNode),
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
      decoration: _passwordDecoration(
        label: '새 비밀번호 확인',
        hint: '새 비밀번호를 다시 입력해주세요',
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

  /// 비밀번호 입력창 공통 데코레이션 (네이비 플로팅 라벨 + 둥근 filled).
  InputDecoration _passwordDecoration({
    required String label,
    required String hint,
  }) {
    return InputDecoration(
      labelText: label,
      hintText: hint,
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
    );
  }

  /// 새 비밀번호와 확인 값의 일치 여부 실시간 피드백.
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

  Widget _buildChangeButton() {
    final canSubmit = !_isLoading && _isFormValid;
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
        child: _isLoading
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
