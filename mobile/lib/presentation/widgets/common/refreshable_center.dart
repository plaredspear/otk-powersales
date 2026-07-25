import 'package:flutter/material.dart';

/// 로딩/에러/빈 데이터 상태에서도 아래로 당겨 새로고침 제스처가 동작하도록,
/// 자식을 뷰포트 높이만큼 채워 중앙 정렬하는 스크롤 가능한 컨테이너.
///
/// RefreshIndicator 는 스크롤 알림으로만 당김을 감지하므로, 내용이 짧아
/// 스크롤이 잡히지 않는 화면에서는 이 위젯으로 감싸야 제스처가 동작한다.
class RefreshableCenter extends StatelessWidget {
  const RefreshableCenter({super.key, required this.child});

  final Widget child;

  @override
  Widget build(BuildContext context) {
    return LayoutBuilder(
      builder: (context, constraints) {
        return SingleChildScrollView(
          physics: const AlwaysScrollableScrollPhysics(),
          child: ConstrainedBox(
            constraints: BoxConstraints(minHeight: constraints.maxHeight),
            child: Center(child: child),
          ),
        );
      },
    );
  }
}
