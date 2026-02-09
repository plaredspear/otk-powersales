import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/theme/app_colors.dart';
import '../../core/theme/app_spacing.dart';
import '../../core/theme/app_typography.dart';
import '../providers/auth_provider.dart';

/// 로그인 화면
///
/// 사번과 비밀번호를 입력하여 로그인합니다.
/// 아이디 기억하기, 자동 로그인 기능을 제공합니다.
class LoginScreen extends ConsumerStatefulWidget {
  const LoginScreen({super.key});

  @override
  ConsumerState<LoginScreen> createState() => _LoginScreenState();
}

class _LoginScreenState extends ConsumerState<LoginScreen> {
  final _formKey = GlobalKey<FormState>();
  final _employeeIdController = TextEditingController();
  final _passwordController = TextEditingController();
  final _passwordFocusNode = FocusNode();

  bool _rememberEmployeeId = false;
  bool _autoLogin = false;
  bool _isInitialized = false;

  @override
  void dispose() {
    _employeeIdController.dispose();
    _passwordController.dispose();
    _passwordFocusNode.dispose();
    super.dispose();
  }

  /// 저장된 사번과 설정을 로드
  void _loadSavedSettings() {
    if (_isInitialized) return;
    _isInitialized = true;

    final authState = ref.read(authProvider);
    if (authState.savedEmployeeId != null &&
        authState.savedEmployeeId!.isNotEmpty) {
      _employeeIdController.text = authState.savedEmployeeId!;
      _rememberEmployeeId = true;
    }
    if (authState.rememberEmployeeId) {
      _rememberEmployeeId = true;
    }
  }

  /// 로그인 실행
  Future<void> _handleLogin() async {
    // 유효성 검증
    if (!_formKey.currentState!.validate()) return;

    ref.read(authProvider.notifier).login(
          employeeId: _employeeIdController.text.trim(),
          password: _passwordController.text,
          rememberEmployeeId: _rememberEmployeeId,
          autoLogin: _autoLogin,
        );
  }

  @override
  Widget build(BuildContext context) {
    final authState = ref.watch(authProvider);

    // 저장된 사번 로드
    _loadSavedSettings();

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

    return Scaffold(
      backgroundColor: AppColors.background,
      body: SafeArea(
        child: SingleChildScrollView(
          padding: AppSpacing.screenHorizontal,
          child: Form(
            key: _formKey,
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                const SizedBox(height: 80),

                // 앱 로고 텍스트
                _buildLogo(),
                const SizedBox(height: 48),

                // 아이디(사번) 입력
                _buildEmployeeIdField(),
                const SizedBox(height: AppSpacing.lg),

                // 비밀번호 입력
                _buildPasswordField(),
                const SizedBox(height: AppSpacing.md),

                // 체크박스 영역
                _buildCheckboxArea(),
                const SizedBox(height: AppSpacing.xxl),

                // 로그인 버튼
                _buildLoginButton(authState.isLoading),
                const SizedBox(height: 48),

                // 저작권 문구
                _buildCopyright(),
              ],
            ),
          ),
        ),
      ),
    );
  }

  /// 앱 로고
  Widget _buildLogo() {
    return Column(
      children: [
        // 오뚜기 로고 색상 표현
        Container(
          width: 64,
          height: 64,
          decoration: BoxDecoration(
            color: AppColors.brandYellow,
            borderRadius: BorderRadius.circular(AppSpacing.radiusLg),
          ),
          child: const Center(
            child: Text(
              'O',
              style: TextStyle(
                fontSize: 36,
                fontWeight: FontWeight.w900,
                color: AppColors.brandRed,
              ),
            ),
          ),
        ),
        const SizedBox(height: AppSpacing.lg),
        Text(
          '오뚜기 파워세일즈',
          style: AppTypography.headlineLarge.copyWith(
            fontSize: 24,
            color: AppColors.textPrimary,
          ),
        ),
      ],
    );
  }

  /// 아이디(사번) 입력 필드
  Widget _buildEmployeeIdField() {
    return TextFormField(
      controller: _employeeIdController,
      keyboardType: TextInputType.number,
      inputFormatters: [
        FilteringTextInputFormatter.digitsOnly,
        LengthLimitingTextInputFormatter(8),
      ],
      decoration: InputDecoration(
        labelText: '아이디(사번)',
        hintText: '8자리 사번을 입력해주세요',
        prefixIcon: const Icon(Icons.person_outline),
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
        FocusScope.of(context).requestFocus(_passwordFocusNode);
      },
      validator: (value) {
        if (value == null || value.trim().isEmpty) {
          return '사번을 입력해주세요';
        }
        return null;
      },
    );
  }

  /// 비밀번호 입력 필드
  Widget _buildPasswordField() {
    return TextFormField(
      controller: _passwordController,
      focusNode: _passwordFocusNode,
      obscureText: true,
      decoration: InputDecoration(
        labelText: '비밀번호',
        hintText: '비밀번호를 입력해주세요',
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
      onFieldSubmitted: (_) => _handleLogin(),
      validator: (value) {
        if (value == null || value.isEmpty) {
          return '비밀번호를 입력해주세요';
        }
        return null;
      },
    );
  }

  /// 체크박스 영역
  Widget _buildCheckboxArea() {
    return Column(
      children: [
        // 아이디 기억하기
        Row(
          children: [
            SizedBox(
              height: 24,
              width: 24,
              child: Checkbox(
                value: _rememberEmployeeId,
                onChanged: (value) {
                  setState(() {
                    _rememberEmployeeId = value ?? false;
                  });
                },
                activeColor: AppColors.secondary,
              ),
            ),
            const SizedBox(width: AppSpacing.sm),
            GestureDetector(
              onTap: () {
                setState(() {
                  _rememberEmployeeId = !_rememberEmployeeId;
                });
              },
              child: Text(
                '아이디 기억하기',
                style: AppTypography.bodyMedium,
              ),
            ),
          ],
        ),
        const SizedBox(height: AppSpacing.sm),
        // 자동 로그인
        Row(
          children: [
            SizedBox(
              height: 24,
              width: 24,
              child: Checkbox(
                value: _autoLogin,
                onChanged: (value) {
                  setState(() {
                    _autoLogin = value ?? false;
                  });
                },
                activeColor: AppColors.secondary,
              ),
            ),
            const SizedBox(width: AppSpacing.sm),
            GestureDetector(
              onTap: () {
                setState(() {
                  _autoLogin = !_autoLogin;
                });
              },
              child: Text(
                '자동 로그인',
                style: AppTypography.bodyMedium,
              ),
            ),
          ],
        ),
      ],
    );
  }

  /// 로그인 버튼
  Widget _buildLoginButton(bool isLoading) {
    return SizedBox(
      height: AppSpacing.buttonHeight,
      child: ElevatedButton(
        onPressed: isLoading ? null : _handleLogin,
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
                '로그인',
                style: AppTypography.labelLarge.copyWith(
                  fontSize: 16,
                  fontWeight: FontWeight.w600,
                ),
              ),
      ),
    );
  }

  /// 저작권 문구
  Widget _buildCopyright() {
    return Text(
      'Copyright (C) ottogi.co.Ltd All Rights Reserved.',
      textAlign: TextAlign.center,
      style: AppTypography.bodySmall.copyWith(
        color: AppColors.textTertiary,
      ),
    );
  }
}
