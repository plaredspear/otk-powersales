import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:intl/date_symbol_data_local.dart';
import 'app_router.dart';
import 'core/theme/app_theme.dart';

void main() async {
  // Flutter 바인딩 초기화
  WidgetsFlutterBinding.ensureInitialized();

  // 한국어 locale 데이터 초기화
  await initializeDateFormatting('ko_KR', null);

  runApp(
    const ProviderScope(
      child: OtokiApp(),
    ),
  );
}

class OtokiApp extends StatelessWidget {
  const OtokiApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: '오뚜기 임직원 영업관리',
      theme: AppTheme.light,
      initialRoute: AppRouter.initialRoute,
      routes: AppRouter.routes,
      onUnknownRoute: AppRouter.onUnknownRoute,
      debugShowCheckedModeBanner: false,
    );
  }
}