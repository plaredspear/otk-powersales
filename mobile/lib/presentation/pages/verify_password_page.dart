import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/theme/app_colors.dart';
import '../../core/utils/error_utils.dart';
import '../providers/auth_provider.dart';
import '../providers/password_provider.dart';
import '../widgets/auth/password_policy_checklist.dart';

/// 현재 비밀번호 확인 페이지 (F54 1단계)
///
/// 레거시 Heroku `mypage/modify.jsp` 정합.
/// 마이페이지에서 비밀번호 변경 전 현재 비밀번호를 확인합니다.
/// - 상단 안내 문구
/// - 로그인한 사번(아이디) 표시 (읽기 전용, 회색)
/// - 현재 비밀번호 입력
/// - 하단 고정 확인 버튼: 성공 시 새 비밀번호 입력 화면으로 이동
class VerifyPasswordPage extends ConsumerStatefulWidget {
  const VerifyPasswordPage({super.key});

  @override
  ConsumerState<VerifyPasswordPage> createState() =>
      _VerifyPasswordPageState();
}

class _VerifyPasswordPageState extends ConsumerState<VerifyPasswordPage> {
  final _passwordController = TextEditingController();
  bool _isLoading = false;

  @override
  void dispose() {
    _passwordController.dispose();
    super.dispose();
  }

  /// 확인 버튼 클릭 (레거시 btn-confirm 핸들러 정합)
  Future<void> _handleConfirm() async {
    if (_isLoading) return;

    // 레거시: 비밀번호 미입력 시 alert
    if (_passwordController.text.trim().isEmpty) {
      await _showAlert('비밀번호를 입력해 주세요');
      return;
    }

    setState(() => _isLoading = true);

    try {
      final notifier = ref.read(passwordVerificationProvider.notifier);
      // 검증 성공 시 true 반환. 불일치/네트워크 오류는 예외로 전파되어 catch 에서
      // error.code 로 구분 처리한다 (아래 참고).
      await notifier.verify(_passwordController.text);

      if (!mounted) return;

      // 성공: 새 비밀번호 입력 화면으로 이동
      Navigator.of(context).pushNamed(
        '/change-password-new',
        arguments: _passwordController.text, // 현재 비밀번호 전달
      );
    } catch (e) {
      if (!mounted) return;
      // 비밀번호 불일치는 서버 error.code(AUTH_CURRENT_PASSWORD_MISMATCH)로 판별한다.
      // HTTP 상태코드(400)에 의존하지 않으므로 서버 상태코드가 바뀌어도 안전하다.
      if (extractErrorCode(e) == 'AUTH_CURRENT_PASSWORD_MISMATCH') {
        await _showAlert('비밀번호가 일치하지 않습니다.');
      } else {
        await _showAlert('잠시 후 다시 시도해주세요.');
      }
    } finally {
      if (mounted) {
        setState(() => _isLoading = false);
      }
    }
  }

  /// 레거시 alert() 정합 안내 다이얼로그
  Future<void> _showAlert(String message) {
    return showDialog<void>(
      context: context,
      builder: (context) => AlertDialog(
        content: Text(message),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(context).pop(),
            child: const Text('확인'),
          ),
        ],
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final employeeCode = ref.watch(authProvider).user?.employeeCode ?? '';

    return Scaffold(
      backgroundColor: AppColors.white,
      appBar: AppBar(
        title: const Text('비밀번호'),
        leading: IconButton(
          icon: const Icon(Icons.arrow_back),
          onPressed: () => Navigator.of(context).pop(),
        ),
      ),
      body: Column(
        children: [
          Expanded(
            child: SingleChildScrollView(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  // 상단 안내 문구 (레거시 txt_box)
                  Container(
                    width: double.infinity,
                    padding: const EdgeInsets.all(20),
                    decoration: const BoxDecoration(
                      border: Border(
                        bottom: BorderSide(color: AppColors.legacyPlaceholder),
                      ),
                    ),
                    child: const Text(
                      '정보 변경을 위해,\n현재 비밀번호를 먼저 입력하세요.',
                      style: TextStyle(
                        fontSize: 14,
                        height: 1.4,
                        color: AppColors.black,
                      ),
                    ),
                  ),

                  // 아이디 (읽기 전용, 회색 배경)
                  _buildFieldRow(
                    label: '아이디',
                    labelColor: const Color(0xFF999999),
                    backgroundColor: const Color(0xFFF7F7F7),
                    field: Text(
                      employeeCode,
                      style: const TextStyle(
                        fontSize: 15,
                        color: Color(0xFF999999),
                      ),
                    ),
                  ),

                  // 비밀번호 입력
                  _buildFieldRow(
                    label: '비밀번호',
                    required: true,
                    field: TextField(
                      controller: _passwordController,
                      obscureText: true,
                      autofocus: true,
                      style: const TextStyle(fontSize: 15),
                      decoration: const InputDecoration(
                        isDense: true,
                        contentPadding: EdgeInsets.zero,
                        border: InputBorder.none,
                        hintText: '비밀번호 입력',
                        hintStyle: TextStyle(
                          fontSize: 15,
                          color: AppColors.legacyPlaceholder,
                        ),
                      ),
                      onSubmitted: (_) => _handleConfirm(),
                      onChanged: (_) => setState(() {}),
                    ),
                  ),

                  // 변경할 비밀번호 조건 안내 (로그인 강제변경 화면과 동일한 체크리스트 위젯)
                  // 여기는 '현재' 비밀번호 입력란이므로 입력값을 검증하지 않고,
                  // 다음 단계에서 입력할 새 비밀번호가 충족해야 할 조건만 중립 상태로 미리 안내한다.
                  _buildPasswordPolicyGuide(),
                ],
              ),
            ),
          ),

          // 하단 고정 확인 버튼 (레거시 fix_bottom_wrap / btn_yellow)
          _buildBottomButton(),
        ],
      ),
    );
  }

  /// 레거시 form_wrap 한 행 (라벨 위 / 밑줄 입력)
  Widget _buildFieldRow({
    required String label,
    required Widget field,
    bool required = false,
    Color labelColor = AppColors.black,
    Color? backgroundColor,
  }) {
    return Container(
      color: backgroundColor,
      child: Container(
        margin: const EdgeInsets.symmetric(horizontal: 20),
        padding: const EdgeInsets.only(top: 12, bottom: 8),
        decoration: const BoxDecoration(
          border: Border(
            bottom: BorderSide(color: Color(0xFFE6E6E6)),
          ),
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            RichText(
              text: TextSpan(
                text: label,
                style: TextStyle(fontSize: 14, color: labelColor),
                children: required
                    ? const [
                        TextSpan(
                          text: ' *',
                          style: TextStyle(color: AppColors.legacyDanger),
                        ),
                      ]
                    : null,
              ),
            ),
            const SizedBox(height: 8),
            field,
          ],
        ),
      ),
    );
  }

  /// 변경할 비밀번호 조건 안내.
  ///
  /// 새 비밀번호 입력 화면(로그인 강제변경·마이페이지 2단계)과 동일한
  /// [PasswordPolicyChecklist] 위젯을 재사용해 정책 요건을 표시한다.
  /// 이 화면은 현재 비밀번호를 확인하는 단계라 입력값을 검증하지 않으므로,
  /// 빈 문자열을 전달해 중립(대기) 상태의 요건 목록으로만 노출한다.
  Widget _buildPasswordPolicyGuide() {
    return Container(
      margin: const EdgeInsets.symmetric(horizontal: 20),
      padding: const EdgeInsets.only(top: 16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: const [
          Text(
            '변경할 비밀번호 조건',
            style: TextStyle(
              fontSize: 13,
              color: Color(0xFF999999),
            ),
          ),
          SizedBox(height: 8),
          PasswordPolicyChecklist(password: ''),
        ],
      ),
    );
  }

  /// 하단 고정 확인 버튼
  Widget _buildBottomButton() {
    return SafeArea(
      child: SizedBox(
        height: 50,
        width: double.infinity,
        child: TextButton(
          onPressed: _isLoading ? null : _handleConfirm,
          style: TextButton.styleFrom(
            backgroundColor: AppColors.legacyYellow,
            foregroundColor: AppColors.black,
            shape: const RoundedRectangleBorder(),
            disabledBackgroundColor: AppColors.legacyYellow,
          ),
          child: _isLoading
              ? const SizedBox(
                  height: 20,
                  width: 20,
                  child: CircularProgressIndicator(
                    strokeWidth: 2,
                    valueColor: AlwaysStoppedAnimation<Color>(AppColors.black),
                  ),
                )
              : const Text(
                  '확인',
                  style: TextStyle(
                    fontSize: 16,
                    fontWeight: FontWeight.bold,
                  ),
                ),
        ),
      ),
    );
  }
}
