import 'dart:io';

import 'package:flutter/material.dart';
import 'package:url_launcher/url_launcher.dart';

import '../../../core/services/force_update_gate.dart';
import '../../../core/theme/app_colors.dart';
import '../../../data/datasources/app_version_api_datasource.dart';

/// 강제 업데이트 전역 차단 오버레이.
///
/// 루트 `MaterialApp.builder` 에서 앱 전체를 감싸 Navigator **위**에 놓인다. 그래서
/// 로그인 전/후 어느 화면이든, 다이얼로그·바텀시트가 떠 있든, 인증 전환으로 라우트
/// 스택이 통째로 교체되든([pushNamedAndRemoveUntil]) 차단이 풀리지 않는다.
/// 차단 여부는 [ForceUpdateGate.blockedBy] 가 결정한다.
class ForceUpdateOverlay extends StatelessWidget {
  const ForceUpdateOverlay({super.key, required this.child});

  final Widget child;

  @override
  Widget build(BuildContext context) {
    return ValueListenableBuilder<AppVersionResult?>(
      valueListenable: ForceUpdateGate.instance.blockedBy,
      builder: (context, blocked, _) => Stack(
        fit: StackFit.expand,
        children: [
          child,
          if (blocked != null) _ForceUpdateBlocker(result: blocked),
        ],
      ),
    );
  }
}

class _ForceUpdateBlocker extends StatefulWidget {
  const _ForceUpdateBlocker({required this.result});

  final AppVersionResult result;

  @override
  State<_ForceUpdateBlocker> createState() => _ForceUpdateBlockerState();
}

class _ForceUpdateBlockerState extends State<_ForceUpdateBlocker> {
  /// 다운로드(OTA 설치)를 한 번이라도 띄웠는지. 이후에는 앱 종료를 안내한다 —
  /// 대상 앱이 실행 중이면 설치가 시작되지 않으므로 사용자가 직접 종료해야 한다.
  bool _downloadLaunched = false;

  Future<void> _openDownload() async {
    final url = widget.result.downloadUrl;
    final uri = (url == null || url.isEmpty) ? null : Uri.tryParse(url);
    if (uri != null) {
      await launchUrl(uri, mode: LaunchMode.externalApplication);
    }
    if (!mounted) return;
    setState(() => _downloadLaunched = true);
  }

  @override
  Widget build(BuildContext context) {
    final result = widget.result;
    final versionSuffix = result.latestVersionName != null
        ? ' (${result.latestVersionName})'
        : '';

    return Stack(
      children: [
        // 뒤 화면으로의 모든 입력을 차단(탭·드래그·뒤로가기 제스처).
        const ModalBarrier(dismissible: false, color: Color(0xB3000000)),
        Material(
          type: MaterialType.transparency,
          child: Center(
            child: SingleChildScrollView(
              padding: const EdgeInsets.all(24),
              child: Container(
                padding: const EdgeInsets.all(24),
                decoration: BoxDecoration(
                  color: AppColors.background,
                  borderRadius: BorderRadius.circular(12),
                ),
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  crossAxisAlignment: CrossAxisAlignment.stretch,
                  children: [
                    Text(
                      _downloadLaunched ? '업데이트 진행' : '업데이트가 필요합니다',
                      textAlign: TextAlign.center,
                      style: const TextStyle(
                        fontSize: 18,
                        fontWeight: FontWeight.bold,
                        color: AppColors.textPrimary,
                      ),
                    ),
                    const SizedBox(height: 16),
                    Text(
                      _downloadLaunched
                          ? '업데이트가 시작됩니다.\n'
                              '아래 "앱 종료"를 누르면 앱이 종료되고 설치가 진행됩니다.\n'
                              '설치가 끝나면 앱을 다시 실행해 주세요.'
                          : (result.releaseNote?.isNotEmpty == true
                              ? result.releaseNote!
                              : '원활한 사용을 위해 최신 버전$versionSuffix으로 업데이트해 주세요.\n'
                                  '업데이트 후 다시 실행해 주세요.'),
                      style: const TextStyle(
                        fontSize: 14,
                        color: AppColors.textPrimary,
                      ),
                    ),
                    const SizedBox(height: 24),
                    FilledButton(
                      onPressed: _downloadLaunched ? () => exit(0) : _openDownload,
                      child: Text(_downloadLaunched ? '앱 종료' : '업데이트하기'),
                    ),
                    if (_downloadLaunched)
                      TextButton(
                        onPressed: _openDownload,
                        child: const Text('다시 다운로드'),
                      ),
                  ],
                ),
              ),
            ),
          ),
        ),
      ],
    );
  }
}
