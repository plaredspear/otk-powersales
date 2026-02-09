import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/theme/app_colors.dart';
import '../../core/theme/app_spacing.dart';
import '../../core/theme/app_typography.dart';
import '../providers/auth_provider.dart';
import '../widgets/common/primary_button.dart';

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
      backgroundColor: const Color(0xFFEFEFEF),
      body: SafeArea(
        child: LayoutBuilder(
          builder: (context, constraints) {
            return SingleChildScrollView(
              child: ConstrainedBox(
                constraints: BoxConstraints(
                  minHeight: constraints.maxHeight,
                ),
                child: Container(
                  width: double.infinity,
                  color: AppColors.white,
                  padding:
                      const EdgeInsets.symmetric(horizontal: 32),
                  child: Form(
                    key: _formKey,
                    child: Column(
                      mainAxisAlignment: MainAxisAlignment.center,
                      crossAxisAlignment: CrossAxisAlignment.stretch,
                      children: [
                        // 앱 로고 + 타이틀
                        _buildLogo(),
                        const SizedBox(height: 32),

                        // 아이디(사번) 입력
                        _buildEmployeeIdField(),
                        const SizedBox(height: 12),

                        // 비밀번호 입력
                        _buildPasswordField(),
                        const SizedBox(height: 16),

                        // 체크박스 영역 (가로 배치)
                        _buildCheckboxArea(),
                        const SizedBox(height: 20),

                        // 로그인 버튼
                        _buildLoginButton(authState.isLoading),
                        const SizedBox(height: 36),

                        // 저작권 문구
                        _buildCopyright(),
                      ],
                    ),
                  ),
                ),
              ),
            );
          },
        ),
      ),
    );
  }

  /// 앱 로고
  Widget _buildLogo() {
    return Column(
      children: [
        // 로고 플레이스홀더 (브랜드 로고 비워둠)
        const SizedBox(height: 16),
        RichText(
          textAlign: TextAlign.center,
          text: TextSpan(
            children: [
              TextSpan(
                text: '오뚜기 ',
                style: AppTypography.headlineLarge.copyWith(
                  fontSize: 22,
                  fontWeight: FontWeight.w700,
                  color: AppColors.textPrimary,
                ),
              ),
              TextSpan(
                text: '파워세일즈',
                style: AppTypography.headlineLarge.copyWith(
                  fontSize: 22,
                  fontWeight: FontWeight.w700,
                  color: AppColors.otokiRed,
                ),
              ),
            ],
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
        hintText: '아이디 입력 (사번)',
        hintStyle: AppTypography.bodyMedium.copyWith(
          color: AppColors.textTertiary,
          fontSize: 15,
        ),
        filled: true,
        fillColor: AppColors.white,
        contentPadding:
            const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
        border: OutlineInputBorder(
          borderRadius: BorderRadius.circular(AppSpacing.radiusSm),
          borderSide: const BorderSide(color: Color(0xFFD0D0D0)),
        ),
        enabledBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(AppSpacing.radiusSm),
          borderSide: const BorderSide(color: Color(0xFFD0D0D0)),
        ),
        focusedBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(AppSpacing.radiusSm),
          borderSide: const BorderSide(color: Color(0xFFAAAAAA), width: 1.5),
        ),
        errorBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(AppSpacing.radiusSm),
          borderSide: const BorderSide(color: AppColors.error),
        ),
        focusedErrorBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(AppSpacing.radiusSm),
          borderSide: const BorderSide(color: AppColors.error, width: 1.5),
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
        hintText: '비밀번호 입력',
        hintStyle: AppTypography.bodyMedium.copyWith(
          color: AppColors.textTertiary,
          fontSize: 15,
        ),
        filled: true,
        fillColor: AppColors.white,
        contentPadding:
            const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
        border: OutlineInputBorder(
          borderRadius: BorderRadius.circular(AppSpacing.radiusSm),
          borderSide: const BorderSide(color: Color(0xFFD0D0D0)),
        ),
        enabledBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(AppSpacing.radiusSm),
          borderSide: const BorderSide(color: Color(0xFFD0D0D0)),
        ),
        focusedBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(AppSpacing.radiusSm),
          borderSide: const BorderSide(color: Color(0xFFAAAAAA), width: 1.5),
        ),
        errorBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(AppSpacing.radiusSm),
          borderSide: const BorderSide(color: AppColors.error),
        ),
        focusedErrorBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(AppSpacing.radiusSm),
          borderSide: const BorderSide(color: AppColors.error, width: 1.5),
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

  /// 체크박스 영역 (가로 배치)
  Widget _buildCheckboxArea() {
    return Row(
      children: [
        // 아이디 기억하기
        SizedBox(
          height: 20,
          width: 20,
          child: Checkbox(
            value: _rememberEmployeeId,
            onChanged: (value) {
              setState(() {
                _rememberEmployeeId = value ?? false;
              });
            },
            activeColor: AppColors.secondary,
            materialTapTargetSize: MaterialTapTargetSize.shrinkWrap,
            visualDensity: VisualDensity.compact,
            side: const BorderSide(color: Color(0xFFBBBBBB), width: 1.5),
          ),
        ),
        const SizedBox(width: 6),
        GestureDetector(
          onTap: () {
            setState(() {
              _rememberEmployeeId = !_rememberEmployeeId;
            });
          },
          child: Text(
            '아이디 기억하기',
            style: AppTypography.bodyMedium.copyWith(
              fontSize: 13,
              color: AppColors.textSecondary,
            ),
          ),
        ),
        const SizedBox(width: 24),
        // 자동 로그인
        SizedBox(
          height: 20,
          width: 20,
          child: Checkbox(
            value: _autoLogin,
            onChanged: (value) {
              setState(() {
                _autoLogin = value ?? false;
              });
            },
            activeColor: AppColors.secondary,
            materialTapTargetSize: MaterialTapTargetSize.shrinkWrap,
            visualDensity: VisualDensity.compact,
            side: const BorderSide(color: Color(0xFFBBBBBB), width: 1.5),
          ),
        ),
        const SizedBox(width: 6),
        GestureDetector(
          onTap: () {
            setState(() {
              _autoLogin = !_autoLogin;
            });
          },
          child: Text(
            '자동로그인',
            style: AppTypography.bodyMedium.copyWith(
              fontSize: 13,
              color: AppColors.textSecondary,
            ),
          ),
        ),
      ],
    );
  }

  /// 로그인 버튼
  Widget _buildLoginButton(bool isLoading) {
    return PrimaryButton(
      text: '로그인',
      onPressed: _handleLogin,
      isLoading: isLoading,
      height: 50,
      fontSize: 16,
    );
  }

  /// 저작권 문구
  Widget _buildCopyright() {
    return Text(
      'Copyright ⓒ ottogi co,Ltd All Rights Reserved.',
      textAlign: TextAlign.center,
      style: AppTypography.bodySmall.copyWith(
        color: AppColors.textTertiary,
        fontSize: 12,
      ),
    );
  }
}
