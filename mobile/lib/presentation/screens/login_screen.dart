import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/session/session_reset_controller.dart';
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
  final _employeeCodeController = TextEditingController();
  final _passwordController = TextEditingController();
  final _passwordFocusNode = FocusNode();

  bool _rememberEmployeeNumber = false;
  bool _autoLogin = false;
  bool _prefilledFromStorage = false;

  @override
  void initState() {
    super.initState();
    // 사번/비밀번호 입력값 변화 시 clear(X) 버튼 노출 여부를 갱신하기 위해 리스너를 건다.
    _employeeCodeController.addListener(_onInputChanged);
    _passwordController.addListener(_onInputChanged);

    // 저장된 사번/자동 로그인 설정을 로드한 뒤 프리필한다. 정상 기동은 스플래시의
    // initialize() 가 이미 로드하지만, 로그아웃 재생성 세션은 스플래시를 건너뛰므로
    // 여기서 직접 로드한다(멱등). 로드 완료 콜백에서만 프리필해, 저장된 사번이
    // 없는(=사번 기억 OFF) 경우에도 자동 로그인 선택 복원 타이밍을 놓치지 않으며,
    // 프리필/복원을 이 한 경로에서만 수행해 build 의 부수효과를 없앤다.
    Future.microtask(() async {
      await ref.read(authProvider.notifier).loadSavedEmployeeNumber();
      if (!mounted) return;
      setState(_loadSavedSettings);
    });

    // 강제 로그아웃으로 재진입한 세션이면 사유를 1회 안내한다.
    // (사용자가 직접 로그아웃한 경우 사유는 null → 무표시)
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (!mounted) return;
      final reason = SessionResetController.instance.consumeReason();
      if (reason != null) {
        _showLogoutReason(reason);
      }
    });
  }

  /// 강제 로그아웃 사유 안내.
  /// - [LogoutReason.deviceRevoked]: 다른 기기 로그인 → 명확한 다이얼로그(확인 필요)
  /// - [LogoutReason.sessionExpired]: 세션 만료 → 가벼운 SnackBar
  void _showLogoutReason(LogoutReason reason) {
    switch (reason) {
      case LogoutReason.deviceRevoked:
        showDialog<void>(
          context: context,
          builder: (dialogContext) => AlertDialog(
            title: Text('로그아웃 안내', style: AppTypography.headlineSmall),
            content: Text(
              '다른 기기에서 로그인되어 현재 기기에서 로그아웃되었습니다.\n'
              '본인이 맞다면 다시 로그인해 주세요.',
              style: AppTypography.bodyMedium,
            ),
            actions: [
              TextButton(
                onPressed: () => Navigator.of(dialogContext).pop(),
                child: Text(
                  '확인',
                  style: AppTypography.labelLarge.copyWith(
                    color: AppColors.secondary,
                  ),
                ),
              ),
            ],
          ),
        );
      case LogoutReason.sessionExpired:
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text('세션이 만료되어 로그아웃되었습니다. 다시 로그인해 주세요.'),
          ),
        );
      case LogoutReason.inactivityTimeout:
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text('장시간 미사용으로 로그아웃되었습니다. 다시 로그인해 주세요.'),
          ),
        );
    }
  }

  /// 입력값이 비었는지 여부에 따라 각 필드의 clear 버튼을 표시/숨김 처리한다.
  void _onInputChanged() {
    setState(() {});
  }

  @override
  void dispose() {
    _employeeCodeController.removeListener(_onInputChanged);
    _passwordController.removeListener(_onInputChanged);
    _employeeCodeController.dispose();
    _passwordController.dispose();
    _passwordFocusNode.dispose();
    super.dispose();
  }

  /// 저장된 사번과 설정을 로그인 화면 상태로 복원한다.
  ///
  /// 저장 설정 로드(`loadSavedEmployeeNumber`)가 완료된 시점(initState 의 로드 완료
  /// 콜백)에서만 호출된다. 저장된 사번(사번 기억 ON) 프리필과 함께, 자동 로그인
  /// 선택(`autoLogin`)을 사번 기억 여부와 독립적으로 복원한다 — 레거시 Heroku 는 앱이
  /// isAutoLogin 저장값을 보관해 로그인 화면에 이전 선택을 유지시키므로, 이에
  /// 정합하도록 저장소에서 복원한 값을 체크박스에 반영한다.
  ///
  /// 최초 1회만 복원하고 잠근다(`_prefilledFromStorage`). 이후 사용자가 체크박스를
  /// 직접 토글해도 rebuild 로 저장값이 덮어써지지 않는다.
  void _loadSavedSettings() {
    if (_prefilledFromStorage) return;

    final authState = ref.read(authProvider);
    final saved = authState.savedEmployeeNumber;
    if (authState.rememberEmployeeNumber && saved != null && saved.isNotEmpty) {
      _employeeCodeController.text = saved;
      _rememberEmployeeNumber = true;
    }
    _autoLogin = authState.autoLogin;
    _prefilledFromStorage = true;
  }

  /// 로그인 실행
  Future<void> _handleLogin() async {
    // 유효성 검증
    if (!_formKey.currentState!.validate()) return;

    ref.read(authProvider.notifier).login(
          employeeCode: _employeeCodeController.text.trim(),
          password: _passwordController.text,
          rememberEmployeeNumber: _rememberEmployeeNumber,
          autoLogin: _autoLogin,
        );
  }

  @override
  Widget build(BuildContext context) {
    final authState = ref.watch(authProvider);

    return Scaffold(
      backgroundColor: const Color(0xFFEFEFEF),
      body: SafeArea(
        // Stack 으로 폼(중앙 정렬)과 에러 박스(하단 고정)를 분리한다.
        // 에러를 Column 흐름에 넣으면 중앙 정렬이 재계산되어 폼이 위로 점프(화면 전환)하므로,
        // 하단 오버레이로 띄워 폼 레이아웃에 영향을 주지 않게 한다.
        child: Stack(
          children: [
            LayoutBuilder(
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
                            _buildEmployeeNumberField(),
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

            // 에러 메시지 (하단 고정 오버레이 — 화면 전환 없이 즉시 표시)
            _buildErrorMessage(authState.errorMessage),
          ],
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
  Widget _buildEmployeeNumberField() {
    return TextFormField(
      controller: _employeeCodeController,
      keyboardType: TextInputType.text,
      // Heroku 레거시 로그인과 동일하게 사번 형식 제약 없음 (8자리/숫자 전용 아님).
      // SF EmpCode__c string(100) 정합으로 길이 상한 100만 유지.
      inputFormatters: [
        LengthLimitingTextInputFormatter(100),
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
        // 입력값이 있을 때만 전체 삭제(X) 버튼을 노출한다.
        suffixIcon: _employeeCodeController.text.isEmpty
            ? null
            : IconButton(
                icon: const Icon(Icons.close, size: 18),
                color: AppColors.textTertiary,
                splashRadius: 18,
                tooltip: '입력 내용 지우기',
                onPressed: () {
                  _employeeCodeController.clear();
                },
              ),
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
        // 입력값이 있을 때만 전체 삭제(X) 버튼을 노출한다.
        suffixIcon: _passwordController.text.isEmpty
            ? null
            : IconButton(
                icon: const Icon(Icons.close, size: 18),
                color: AppColors.textTertiary,
                splashRadius: 18,
                tooltip: '입력 내용 지우기',
                onPressed: () {
                  _passwordController.clear();
                },
              ),
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
            value: _rememberEmployeeNumber,
            onChanged: (value) {
              setState(() {
                _rememberEmployeeNumber = value ?? false;
              });
            },
            activeColor: AppColors.otokiYellow,
            checkColor: AppColors.textPrimary,
            materialTapTargetSize: MaterialTapTargetSize.shrinkWrap,
            visualDensity: VisualDensity.compact,
            side: const BorderSide(color: Color(0xFFBBBBBB), width: 1.5),
          ),
        ),
        const SizedBox(width: 6),
        GestureDetector(
          onTap: () {
            setState(() {
              _rememberEmployeeNumber = !_rememberEmployeeNumber;
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
            activeColor: AppColors.otokiYellow,
            checkColor: AppColors.textPrimary,
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

  /// 에러 메시지 (하단 고정 오버레이)
  ///
  /// 로그인 실패 시 화면 전환/애니메이션 없이 화면 하단에 즉시 표시한다.
  /// Stack 오버레이라 중앙 정렬된 폼 레이아웃에 영향을 주지 않는다.
  /// `errorMessage`는 다음 로그인 시도(`toLoading()`)에서 null로 초기화된다.
  Widget _buildErrorMessage(String? message) {
    if (message == null || message.isEmpty) {
      return const SizedBox.shrink();
    }
    return Positioned(
      left: 16,
      right: 16,
      bottom: 16,
      child: Container(
        width: double.infinity,
        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 16),
        decoration: BoxDecoration(
          color: AppColors.error,
          borderRadius: BorderRadius.circular(AppSpacing.radiusSm),
        ),
        child: Text(
          message,
          textAlign: TextAlign.center,
          style: AppTypography.bodyMedium.copyWith(
            color: AppColors.white,
            fontSize: 15,
            fontWeight: FontWeight.w500,
          ),
        ),
      ),
    );
  }

  /// 저작권 문구
  Widget _buildCopyright() {
    return Text(
      'Copyright ⓒ Otoki co,Ltd All Rights Reserved.',
      textAlign: TextAlign.center,
      style: AppTypography.bodySmall.copyWith(
        color: AppColors.textTertiary,
        fontSize: 12,
      ),
    );
  }
}
