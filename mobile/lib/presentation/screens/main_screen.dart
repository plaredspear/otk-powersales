import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../pages/home_page.dart';
import '../widgets/menu/full_menu_drawer.dart';

/// 메인 화면
///
/// 홈 화면 + 전체메뉴 endDrawer 구성.
class MainScreen extends ConsumerStatefulWidget {
  const MainScreen({super.key});

  @override
  ConsumerState<MainScreen> createState() => _MainScreenState();
}

class _MainScreenState extends ConsumerState<MainScreen> {
  final GlobalKey<ScaffoldState> _scaffoldKey = GlobalKey<ScaffoldState>();

  /// 전체메뉴 Drawer 열기 (외부에서 호출 가능하도록)
  void openFullMenu() {
    _scaffoldKey.currentState?.openEndDrawer();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      key: _scaffoldKey,
      body: const HomePage(),
      endDrawer: const FullMenuDrawer(),
      // endDrawer의 scrim 기본 동작: 배경 딤 + 탭 시 닫힘
      endDrawerEnableOpenDragGesture: false,
    );
  }
}
