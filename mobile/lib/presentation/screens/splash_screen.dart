import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:url_launcher/url_launcher.dart';

import '../../core/services/app_version_checker.dart';
import '../../core/theme/app_colors.dart';
import '../../data/datasources/app_version_api_datasource.dart';
import '../providers/auth_provider.dart';

/// 앱 시작 스플래시 화면.
///
/// 순서: ① 버전 게이트(강제 업데이트면 진입 차단) → ② 자동 로그인 초기화.
/// 자동 로그인이 끝나면 main.dart 의 authProvider 리스너가 적절한 화면으로 전환한다.
/// (인증 상태 resolve 전까지는 이 화면이 유지된다)
class SplashScreen extends ConsumerStatefulWidget {
  const SplashScreen({super.key});

  @override
  ConsumerState<SplashScreen> createState() => _SplashScreenState();
}

class _SplashScreenState extends ConsumerState<SplashScreen> {
  @override
  void initState() {
    super.initState();
    // 첫 프레임 이후 실행 — context/navigator 준비 보장.
    WidgetsBinding.instance.addPostFrameCallback((_) => _bootstrap());
  }

  Future<void> _bootstrap() async {
    final result = await ref.read(appVersionCheckerProvider).check();

    // 강제 업데이트 → 진입 차단 (자동 로그인 시작하지 않음)
    if (result != null && result.forceUpdate) {
      await _showForceUpdateDialog(result);
      return;
    }

    // 권장 업데이트 → 안내 후 계속 진행
    if (result != null && result.updateAvailable) {
      await _showOptionalUpdateDialog(result);
    }

    if (!mounted) return;
    // 자동 로그인 초기화 (이후 main.dart 리스너가 화면 전환)
    ref.read(authProvider.notifier).initialize();
  }

  Future<void> _openDownload(String? downloadUrl) async {
    if (downloadUrl == null || downloadUrl.isEmpty) return;
    final uri = Uri.tryParse(downloadUrl);
    if (uri == null) return;
    await launchUrl(uri, mode: LaunchMode.externalApplication);
  }

  Future<void> _showForceUpdateDialog(AppVersionResult result) async {
    if (!mounted) return;
    await showDialog<void>(
      context: context,
      barrierDismissible: false,
      builder: (dialogContext) => PopScope(
        canPop: false,
        child: AlertDialog(
          title: const Text('업데이트가 필요합니다'),
          content: Text(
            result.releaseNote?.isNotEmpty == true
                ? result.releaseNote!
                : '원활한 사용을 위해 최신 버전으로 업데이트해 주세요.\n'
                    '업데이트 후 다시 실행해 주세요.',
          ),
          actions: [
            FilledButton(
              onPressed: () => _openDownload(result.downloadUrl),
              child: const Text('업데이트하기'),
            ),
          ],
        ),
      ),
    );
  }

  Future<void> _showOptionalUpdateDialog(AppVersionResult result) async {
    if (!mounted) return;
    await showDialog<void>(
      context: context,
      builder: (dialogContext) => AlertDialog(
        title: const Text('업데이트 안내'),
        content: Text(
          result.latestVersionName != null
              ? '새로운 버전(${result.latestVersionName})이 있습니다.'
              : '새로운 버전이 있습니다.',
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(dialogContext).pop(),
            child: const Text('나중에'),
          ),
          FilledButton(
            onPressed: () {
              Navigator.of(dialogContext).pop();
              _openDownload(result.downloadUrl);
            },
            child: const Text('업데이트'),
          ),
        ],
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppColors.background,
      body: Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: const [
            Text(
              '오뚜기 임직원 영업관리',
              style: TextStyle(
                fontSize: 20,
                fontWeight: FontWeight.bold,
                color: AppColors.secondary,
              ),
            ),
            SizedBox(height: 32),
            // 빈 화면 금지 원칙 — 로딩 상태를 네이비 스피너로 가시화
            CircularProgressIndicator(
              color: AppColors.secondary,
            ),
          ],
        ),
      ),
    );
  }
}
