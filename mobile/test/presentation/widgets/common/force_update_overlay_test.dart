import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/core/services/force_update_gate.dart';
import 'package:mobile/data/datasources/app_version_api_datasource.dart';
import 'package:mobile/presentation/widgets/common/force_update_overlay.dart';

void main() {
  final navigatorKey = GlobalKey<NavigatorState>();

  tearDown(() {
    // 싱글턴 게이트 상태가 다음 테스트로 새지 않게 되돌린다.
    ForceUpdateGate.instance.blockedBy.value = null;
  });

  const blocked = AppVersionResult(
    updateAvailable: true,
    forceUpdate: true,
    latestVersionName: '1.0.99',
    releaseNote: '필수 보안 업데이트',
    downloadUrl: 'https://example.com/app.apk',
  );

  /// 실제 앱과 동일하게 `MaterialApp.builder` 로 오버레이를 얹은 테스트 앱.
  Widget buildApp() => MaterialApp(
        navigatorKey: navigatorKey,
        initialRoute: '/home',
        routes: {
          '/home': (_) => const Scaffold(body: Text('홈 화면')),
          '/login': (_) => const Scaffold(body: Text('로그인 화면')),
        },
        builder: (context, child) =>
            ForceUpdateOverlay(child: child ?? const SizedBox.shrink()),
      );

  testWidgets('차단 상태가 아니면 오버레이를 표시하지 않는다', (tester) async {
    await tester.pumpWidget(buildApp());

    expect(find.text('홈 화면'), findsOneWidget);
    expect(find.text('업데이트가 필요합니다'), findsNothing);
  });

  testWidgets('강제 업데이트로 차단되면 안내와 업데이트 버튼을 표시한다', (tester) async {
    await tester.pumpWidget(buildApp());

    ForceUpdateGate.instance.blockedBy.value = blocked;
    await tester.pump();

    expect(find.text('업데이트가 필요합니다'), findsOneWidget);
    expect(find.text('필수 보안 업데이트'), findsOneWidget);
    expect(find.text('업데이트하기'), findsOneWidget);
    // 뒤 화면으로의 입력을 막는 배리어가 함께 깔린다.
    expect(find.byType(ModalBarrier), findsWidgets);
  });

  testWidgets('라우트 스택이 통째로 교체돼도 차단이 유지된다', (tester) async {
    await tester.pumpWidget(buildApp());

    ForceUpdateGate.instance.blockedBy.value = blocked;
    await tester.pump();

    // 인증 상태 전환(로그인/로그아웃)이 하는 것과 동일한 스택 전체 교체.
    navigatorKey.currentState!.pushNamedAndRemoveUntil('/login', (_) => false);
    await tester.pumpAndSettle();

    expect(find.text('로그인 화면'), findsOneWidget);
    // 다이얼로그 라우트였다면 여기서 사라졌을 차단 UI 가 그대로 남아 있어야 한다.
    expect(find.text('업데이트가 필요합니다'), findsOneWidget);
  });
}
