import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../domain/entities/password_validation.dart';
import '../providers/password_provider.dart';

/// 새 비밀번호 입력 페이지 (F54 2단계)
///
/// 변경할 비밀번호를 입력하고 확인합니다.
/// - 새 비밀번호 입력 (실시간 유효성 검증)
/// - 새 비밀번호 확인 입력 (일치 여부 확인)
/// - 변경 버튼: 성공 시 성공 다이얼로그 표시 후 이전 화면으로 복귀
class ChangePasswordPage extends ConsumerStatefulWidget {
  final String currentPassword;

  const ChangePasswordPage({
    super.key,
    required this.currentPassword,
  });

  @override
  ConsumerState<ChangePasswordPage> createState() =>
      _ChangePasswordPageState();
}

class _ChangePasswordPageState extends ConsumerState<ChangePasswordPage> {
  final _formKey = GlobalKey<FormState>();
  final _newPasswordController = TextEditingController();
  final _confirmPasswordController = TextEditingController();
  bool _isNewPasswordVisible = false;
  bool _isConfirmPasswordVisible = false;
  bool _isLoading = false;

  @override
  void dispose() {
    _newPasswordController.dispose();
    _confirmPasswordController.dispose();
    super.dispose();
  }

  /// 비밀번호 변경
  Future<void> _handleChangePassword() async {
    if (!_formKey.currentState!.validate()) {
      return;
    }

    setState(() => _isLoading = true);

    try {
      final notifier = ref.read(passwordChangeProvider.notifier);
      await notifier.changePassword(
        currentPassword: widget.currentPassword,
        newPassword: _newPasswordController.text,
      );

      if (!mounted) return;

      // 성공: 다이얼로그 표시 후 이전 화면으로 복귀
      await _showSuccessDialog();
      if (!mounted) return;
      Navigator.of(context).pop(); // ChangePasswordPage 종료
      Navigator.of(context).pop(); // VerifyPasswordPage 종료
    } catch (e) {
      if (!mounted) return;
      _showErrorSnackBar('비밀번호 변경에 실패했습니다. 다시 시도해주세요.');
    } finally {
      if (mounted) {
        setState(() => _isLoading = false);
      }
    }
  }

  /// 성공 다이얼로그 표시
  Future<void> _showSuccessDialog() async {
    return showDialog<void>(
      context: context,
      barrierDismissible: false,
      builder: (context) {
        return AlertDialog(
          title: const Text('비밀번호 변경 완료'),
          content: const Text('비밀번호가 성공적으로 변경되었습니다.'),
          actions: [
            TextButton(
              onPressed: () => Navigator.of(context).pop(),
              child: const Text('확인'),
            ),
          ],
        );
      },
    );
  }

  /// 에러 메시지 표시
  void _showErrorSnackBar(String message) {
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text(message),
        backgroundColor: Colors.red,
        duration: const Duration(seconds: 3),
      ),
    );
  }

  /// 버튼 활성화 여부
  bool get _isFormValid {
    if (_newPasswordController.text.isEmpty ||
        _confirmPasswordController.text.isEmpty) {
      return false;
    }

    // 새 비밀번호 유효성 검증
    final validation = ref.watch(
      passwordValidationProvider(_newPasswordController.text),
    );
    if (!validation.isValid) {
      return false;
    }

    // 비밀번호 일치 여부
    if (_newPasswordController.text != _confirmPasswordController.text) {
      return false;
    }

    return true;
  }

  @override
  Widget build(BuildContext context) {
    final newPasswordValidation = _newPasswordController.text.isEmpty
        ? null
        : ref.watch(
            passwordValidationProvider(_newPasswordController.text),
          );

    final passwordsMatch = _confirmPasswordController.text.isEmpty
        ? true
        : _newPasswordController.text == _confirmPasswordController.text;

    return Scaffold(
      appBar: AppBar(
        title: const Text('비밀번호 변경'),
        leading: IconButton(
          icon: const Icon(Icons.arrow_back),
          onPressed: () => Navigator.of(context).pop(),
        ),
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(16),
        child: Form(
          key: _formKey,
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              // 새 비밀번호 입력
              TextFormField(
                controller: _newPasswordController,
                decoration: InputDecoration(
                  labelText: '변경할 비밀번호*',
                  hintText: '비밀번호 입력',
                  border: const OutlineInputBorder(),
                  suffixIcon: IconButton(
                    icon: Icon(
                      _isNewPasswordVisible
                          ? Icons.visibility
                          : Icons.visibility_off,
                    ),
                    onPressed: () {
                      setState(() {
                        _isNewPasswordVisible = !_isNewPasswordVisible;
                      });
                    },
                  ),
                ),
                obscureText: !_isNewPasswordVisible,
                onChanged: (_) => setState(() {}),
              ),
              const SizedBox(height: 8),

              // 새 비밀번호 유효성 피드백
              if (newPasswordValidation != null)
                _buildValidationFeedback(newPasswordValidation),
              const SizedBox(height: 16),

              // 새 비밀번호 확인 입력
              TextFormField(
                controller: _confirmPasswordController,
                decoration: InputDecoration(
                  labelText: '변경할 비밀번호 다시 입력*',
                  hintText: '비밀번호 입력',
                  border: const OutlineInputBorder(),
                  suffixIcon: IconButton(
                    icon: Icon(
                      _isConfirmPasswordVisible
                          ? Icons.visibility
                          : Icons.visibility_off,
                    ),
                    onPressed: () {
                      setState(() {
                        _isConfirmPasswordVisible = !_isConfirmPasswordVisible;
                      });
                    },
                  ),
                ),
                obscureText: !_isConfirmPasswordVisible,
                onChanged: (_) => setState(() {}),
              ),
              const SizedBox(height: 8),

              // 비밀번호 일치 여부 피드백
              if (_confirmPasswordController.text.isNotEmpty)
                _buildMatchFeedback(passwordsMatch),
              const SizedBox(height: 24),

              // 우측 설명 패널
              Container(
                padding: const EdgeInsets.all(16),
                decoration: BoxDecoration(
                  color: Colors.grey[100],
                  borderRadius: BorderRadius.circular(8),
                ),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    _buildInfoItem(
                      '1. 비밀번호 변경',
                      '변경할 비밀번호 입력',
                    ),
                    const SizedBox(height: 8),
                    _buildInfoItem(
                      '2. 변경 버튼',
                      '정상적으로 입력 후 선택 시, 변경 완료 후 이전 화면으로 이동',
                    ),
                  ],
                ),
              ),
              const SizedBox(height: 16),

              // 하단 안내 문구 (빨간색)
              Container(
                padding: const EdgeInsets.all(12),
                decoration: BoxDecoration(
                  color: Colors.red[50],
                  borderRadius: BorderRadius.circular(8),
                  border: Border.all(color: Colors.red[200]!),
                ),
                child: const Text(
                  '변경할 비밀번호를 입력합니다.',
                  style: TextStyle(
                    color: Colors.red,
                    fontSize: 14,
                  ),
                ),
              ),
              const SizedBox(height: 24),

              // 변경 버튼
              ElevatedButton(
                onPressed: _isLoading || !_isFormValid
                    ? null
                    : _handleChangePassword,
                style: ElevatedButton.styleFrom(
                  backgroundColor: Colors.amber,
                  foregroundColor: Colors.black,
                  padding: const EdgeInsets.symmetric(vertical: 16),
                  disabledBackgroundColor: Colors.grey[300],
                ),
                child: _isLoading
                    ? const SizedBox(
                        height: 20,
                        width: 20,
                        child: CircularProgressIndicator(
                          strokeWidth: 2,
                          valueColor: AlwaysStoppedAnimation<Color>(
                            Colors.black,
                          ),
                        ),
                      )
                    : const Text(
                        '변경',
                        style: TextStyle(
                          fontSize: 16,
                          fontWeight: FontWeight.bold,
                        ),
                      ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  /// 유효성 검증 피드백
  Widget _buildValidationFeedback(PasswordValidation validation) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        _buildValidationItem(
          '4글자 이상',
          validation.isLengthValid,
        ),
        _buildValidationItem(
          '동일 문자 반복 불가 (예: 1111, aaaa)',
          validation.isNotRepeating,
        ),
      ],
    );
  }

  /// 유효성 검증 항목
  Widget _buildValidationItem(String text, bool isValid) {
    return Row(
      children: [
        Icon(
          isValid ? Icons.check_circle : Icons.cancel,
          size: 16,
          color: isValid ? Colors.green : Colors.red,
        ),
        const SizedBox(width: 4),
        Text(
          text,
          style: TextStyle(
            fontSize: 12,
            color: isValid ? Colors.green : Colors.red,
          ),
        ),
      ],
    );
  }

  /// 비밀번호 일치 여부 피드백
  Widget _buildMatchFeedback(bool passwordsMatch) {
    return Row(
      children: [
        Icon(
          passwordsMatch ? Icons.check_circle : Icons.cancel,
          size: 16,
          color: passwordsMatch ? Colors.green : Colors.red,
        ),
        const SizedBox(width: 4),
        Text(
          passwordsMatch ? '비밀번호가 일치합니다' : '비밀번호가 일치하지 않습니다',
          style: TextStyle(
            fontSize: 12,
            color: passwordsMatch ? Colors.green : Colors.red,
          ),
        ),
      ],
    );
  }

  /// 설명 패널 항목
  Widget _buildInfoItem(String title, String description) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          title,
          style: const TextStyle(
            fontWeight: FontWeight.bold,
            fontSize: 14,
          ),
        ),
        const SizedBox(height: 4),
        Text(
          description,
          style: TextStyle(
            fontSize: 13,
            color: Colors.grey[700],
          ),
        ),
      ],
    );
  }
}
