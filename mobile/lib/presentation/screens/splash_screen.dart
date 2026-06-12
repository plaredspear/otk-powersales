import 'dart:io';

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
      final didChooseUpdate = await _showOptionalUpdateDialog(result);
      if (didChooseUpdate) {
        // OTA 설치 요청 후, 실행 중에는 설치가 진행되지 않음을 안내.
        // (권장 업데이트는 강제 종료하지 않고 사용자가 직접 종료하도록 둔다)
        await _openDownload(result.downloadUrl);
        await _showInstallGuide();
      }
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
              onPressed: () async {
                // OTA 설치 요청 후, 설치를 시작하려면 앱이 종료돼야 하므로
                // 안내 다이얼로그를 띄우고 사용자가 직접 종료하게 한다.
                await _openDownload(result.downloadUrl);
                await _showInstallGuideAndExit();
              },
              child: const Text('업데이트하기'),
            ),
          ],
        ),
      ),
    );
  }

  /// 권장 업데이트 다이얼로그. 사용자가 '업데이트'를 선택했으면 true 반환.
  Future<bool> _showOptionalUpdateDialog(AppVersionResult result) async {
    if (!mounted) return false;
    final didChoose = await showDialog<bool>(
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
            onPressed: () => Navigator.of(dialogContext).pop(false),
            child: const Text('나중에'),
          ),
          FilledButton(
            onPressed: () => Navigator.of(dialogContext).pop(true),
            child: const Text('업데이트'),
          ),
        ],
      ),
    );
    return didChoose ?? false;
  }

  /// 권장 업데이트용 설치 안내(앱 종료는 사용자 선택).
  Future<void> _showInstallGuide() async {
    if (!mounted) return;
    await showDialog<void>(
      context: context,
      builder: (dialogContext) => AlertDialog(
        title: const Text('업데이트 진행 안내'),
        content: const Text(
          '업데이트가 시작됩니다.\n'
          '설치를 완료하려면 앱을 완전히 종료한 뒤 잠시 기다려 주세요.\n'
          '설치가 끝나면 앱을 다시 실행해 주세요.',
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(dialogContext).pop(),
            child: const Text('확인'),
          ),
        ],
      ),
    );
  }

  /// 강제 업데이트용 설치 안내 + 앱 종료.
  ///
  /// iOS는 대상 앱이 실행 중이면 OTA 설치를 시작하지 않으므로,
  /// 사용자가 '앱 종료'를 누르면 즉시 프로세스를 종료해 설치가 진행되게 한다.
  Future<void> _showInstallGuideAndExit() async {
    if (!mounted) return;
    await showDialog<void>(
      context: context,
      barrierDismissible: false,
      builder: (dialogContext) => PopScope(
        canPop: false,
        child: AlertDialog(
          title: const Text('업데이트 진행'),
          content: const Text(
            '업데이트가 시작됩니다.\n'
            '아래 "앱 종료"를 누르면 앱이 종료되고 설치가 진행됩니다.\n'
            '설치가 끝나면 앱을 다시 실행해 주세요.',
          ),
          actions: [
            FilledButton(
              onPressed: () => exit(0),
              child: const Text('앱 종료'),
            ),
          ],
        ),
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
