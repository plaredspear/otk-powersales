import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/theme/app_colors.dart';
import '../providers/auth_provider.dart';
import '../providers/password_provider.dart';

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
      final isValid = await notifier.verify(_passwordController.text);

      if (!mounted) return;

      if (isValid) {
        // 성공: 새 비밀번호 입력 화면으로 이동
        Navigator.of(context).pushNamed(
          '/change-password-new',
          arguments: _passwordController.text, // 현재 비밀번호 전달
        );
      } else {
        // 레거시: error == 'fail' 시 alert
        await _showAlert('비밀번호가 일치하지 않습니다.');
      }
    } catch (e) {
      if (!mounted) return;
      final errorString = e.toString();
      // 401 AUTH_CURRENT_PASSWORD_MISMATCH → 비밀번호 불일치 (레거시 메시지)
      if (errorString.contains('AUTH_CURRENT_PASSWORD_MISMATCH') ||
          errorString.contains('401')) {
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
