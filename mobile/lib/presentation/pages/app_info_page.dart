import 'package:flutter/material.dart';
import 'package:package_info_plus/package_info_plus.dart';

import '../../core/theme/app_colors.dart';
import '../../core/theme/app_spacing.dart';
import '../../core/theme/app_typography.dart';

/// 앱 정보 화면
///
/// 앱 버전 정보와 오픈소스 라이선스를 표시합니다.
/// 레거시 `setting/view.jsp` 대응 화면.
class AppInfoPage extends StatelessWidget {
  const AppInfoPage({super.key});

  static const String _appName = '오뚜기 파워세일즈';

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppColors.background,
      appBar: AppBar(
        title: const Text('앱 정보'),
        backgroundColor: AppColors.white,
        foregroundColor: AppColors.textPrimary,
        elevation: 0,
      ),
      body: FutureBuilder<PackageInfo>(
        future: PackageInfo.fromPlatform(),
        builder: (context, snapshot) {
          final versionLabel = switch (snapshot) {
            AsyncSnapshot(connectionState: ConnectionState.done, data: final info?) =>
              '${info.version} (${info.buildNumber})',
            AsyncSnapshot(connectionState: ConnectionState.done) =>
              '정보를 불러올 수 없습니다',
            _ => '불러오는 중...',
          };

          return ListView(
            children: [
              const SizedBox(height: AppSpacing.sm),
              _InfoTile(
                label: '버전 정보',
                value: versionLabel,
              ),
              const Divider(height: 1, color: AppColors.border),
              ListTile(
                title: Text(
                  '오픈소스 라이선스',
                  style: AppTypography.bodyMedium.copyWith(
                    color: AppColors.textPrimary,
                  ),
                ),
                trailing: const Icon(
                  Icons.chevron_right,
                  color: AppColors.textTertiary,
                ),
                onTap: () => showLicensePage(
                  context: context,
                  applicationName: _appName,
                  applicationVersion: snapshot.data?.version,
                ),
              ),
              const Divider(height: 1, color: AppColors.border),
            ],
          );
        },
      ),
    );
  }
}

/// 라벨 + 값 형태의 정보 행 (탭 불가)
class _InfoTile extends StatelessWidget {
  final String label;
  final String value;

  const _InfoTile({
    required this.label,
    required this.value,
  });

  @override
  Widget build(BuildContext context) {
    return ListTile(
      title: Text(
        label,
        style: AppTypography.bodyMedium.copyWith(
          color: AppColors.textPrimary,
        ),
      ),
      trailing: Text(
        value,
        style: AppTypography.bodyMedium.copyWith(
          color: AppColors.textSecondary,
        ),
      ),
    );
  }
}
