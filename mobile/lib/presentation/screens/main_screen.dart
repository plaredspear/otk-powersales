import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../pages/home_page.dart';
import 'pos_sales_screen.dart';
import 'electronic_sales_screen.dart';
import 'logistics_sales_screen.dart';
import 'target_dashboard_screen.dart';

/// 메인 화면
///
/// BottomNavigationBar를 사용하여 5개 주요 기능 화면을 전환합니다.
/// - 홈
/// - POS 매출 조회
/// - 전산매출 조회
/// - 물류매출 조회
/// - 목표/진도율 관리
class MainScreen extends ConsumerStatefulWidget {
  const MainScreen({super.key});

  @override
  ConsumerState<MainScreen> createState() => _MainScreenState();
}

class _MainScreenState extends ConsumerState<MainScreen> {
  int _currentIndex = 0;

  // 각 탭의 화면 목록
  static final List<Widget> _screens = [
    const HomePage(), // 홈
    const PosSalesScreen(), // POS 매출 조회
    const ElectronicSalesScreen(), // 전산매출 조회
    const LogisticsSalesScreen(), // 물류매출 조회
    const TargetDashboardScreen(), // 목표/진도율 관리
  ];

  // BottomNavigationBar 아이템 목록
  static const List<BottomNavigationBarItem> _navItems = [
    BottomNavigationBarItem(
      icon: Icon(Icons.home_outlined),
      activeIcon: Icon(Icons.home),
      label: '홈',
      tooltip: '홈',
    ),
    BottomNavigationBarItem(
      icon: Icon(Icons.point_of_sale),
      label: 'POS 매출',
      tooltip: 'POS 매출 조회',
    ),
    BottomNavigationBarItem(
      icon: Icon(Icons.computer),
      label: '전산매출',
      tooltip: '전산매출 조회',
    ),
    BottomNavigationBarItem(
      icon: Icon(Icons.local_shipping),
      label: '물류매출',
      tooltip: '물류매출 조회',
    ),
    BottomNavigationBarItem(
      icon: Icon(Icons.trending_up),
      label: '목표/진도율',
      tooltip: '목표 및 진도율 관리',
    ),
  ];

  void _onItemTapped(int index) {
    setState(() {
      _currentIndex = index;
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: IndexedStack(
        index: _currentIndex,
        children: _screens,
      ),
      bottomNavigationBar: BottomNavigationBar(
        currentIndex: _currentIndex,
        onTap: _onItemTapped,
        type: BottomNavigationBarType.fixed,
        selectedItemColor: Theme.of(context).primaryColor,
        unselectedItemColor: Colors.grey[600],
        selectedFontSize: 12,
        unselectedFontSize: 11,
        showUnselectedLabels: true,
        items: _navItems,
      ),
    );
  }
}