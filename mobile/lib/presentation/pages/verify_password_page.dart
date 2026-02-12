import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../providers/password_provider.dart';

/// 현재 비밀번호 확인 페이지 (F54 1단계)
///
/// 마이페이지에서 비밀번호 변경 전 현재 비밀번호를 확인합니다.
/// - 로그인한 사번 표시 (읽기 전용)
/// - 현재 비밀번호 입력
/// - 확인 버튼: 성공 시 새 비밀번호 입력 화면으로 이동
class VerifyPasswordPage extends ConsumerStatefulWidget {
  const VerifyPasswordPage({super.key});

  @override
  ConsumerState<VerifyPasswordPage> createState() =>
      _VerifyPasswordPageState();
}

class _VerifyPasswordPageState extends ConsumerState<VerifyPasswordPage> {
  final _formKey = GlobalKey<FormState>();
  final _passwordController = TextEditingController();
  bool _isPasswordVisible = false;
  bool _isLoading = false;

  @override
  void dispose() {
    _passwordController.dispose();
    super.dispose();
  }

  /// 현재 비밀번호 확인
  Future<void> _handleVerify() async {
    if (!_formKey.currentState!.validate()) {
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
          '/change-password',
          arguments: _passwordController.text, // 현재 비밀번호 전달
        );
      } else {
        // 실패: 에러 메시지 표시
        _showErrorSnackBar('비밀번호가 일치하지 않습니다.');
      }
    } catch (e) {
      if (!mounted) return;
      _showErrorSnackBar('오류가 발생했습니다. 다시 시도해주세요.');
    } finally {
      if (mounted) {
        setState(() => _isLoading = false);
      }
    }
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

  @override
  Widget build(BuildContext context) {
    // TODO: 실제로는 auth provider에서 현재 로그인한 사용자 정보를 가져와야 함
    const employeeId = '12345678'; // Mock 데이터

    return Scaffold(
      appBar: AppBar(
        title: const Text('현재 비밀번호 확인'),
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
              // 아이디 표시 (읽기 전용)
              TextFormField(
                initialValue: employeeId,
                decoration: const InputDecoration(
                  labelText: '아이디',
                  filled: true,
                  fillColor: Color(0xFFE0E0E0), // 회색 배경
                  border: OutlineInputBorder(),
                ),
                readOnly: true,
                enabled: false,
              ),
              const SizedBox(height: 16),

              // 비밀번호 입력
              TextFormField(
                controller: _passwordController,
                decoration: InputDecoration(
                  labelText: '비밀번호*',
                  hintText: '비밀번호 입력',
                  border: const OutlineInputBorder(),
                  suffixIcon: IconButton(
                    icon: Icon(
                      _isPasswordVisible
                          ? Icons.visibility
                          : Icons.visibility_off,
                    ),
                    onPressed: () {
                      setState(() {
                        _isPasswordVisible = !_isPasswordVisible;
                      });
                    },
                  ),
                ),
                obscureText: !_isPasswordVisible,
                validator: (value) {
                  if (value == null || value.isEmpty) {
                    return '비밀번호를 입력해주세요';
                  }
                  return null;
                },
                onChanged: (_) => setState(() {}), // 버튼 활성화 상태 업데이트
              ),
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
                      '1. 비밀번호 입력',
                      '마이페이지 변경 전 현재 비밀번호 입력 필수',
                    ),
                    const SizedBox(height: 8),
                    _buildInfoItem(
                      '2. 확인 버튼',
                      '정상적으로 입력 후 선택 시, 비밀번호 변경 화면으로 이동',
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
                  '마이페이지에서 비밀번호 변경 시, 현재 비밀번호를 입력하여 확인합니다.',
                  style: TextStyle(
                    color: Colors.red,
                    fontSize: 14,
                  ),
                ),
              ),
              const SizedBox(height: 24),

              // 확인 버튼
              ElevatedButton(
                onPressed: _isLoading || _passwordController.text.isEmpty
                    ? null
                    : _handleVerify,
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
                        '확인',
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
