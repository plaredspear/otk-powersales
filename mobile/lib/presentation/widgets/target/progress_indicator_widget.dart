import 'package:flutter/material.dart';
import '../../../domain/entities/progress.dart';

/// 진도율 프로그레스 인디케이터 위젯
///
/// 목표 대비 실적의 진도율을 시각적으로 표시합니다.
///
/// 기능:
/// - 가로형 프로그레스 바
/// - 진도율에 따른 색상 코딩 (초과=녹색, 달성=파랑, 부족=빨강)
/// - 백분율 텍스트 표시
/// - 애니메이션 효과 (옵션)
class ProgressIndicatorWidget extends StatefulWidget {
  /// 표시할 진도율 데이터
  final Progress progress;

  /// 위젯 높이
  final double height;

  /// 위젯 너비 (기본값: 부모 너비에 맞춤)
  final double? width;

  /// 백분율 텍스트 표시 여부
  final bool showPercentage;

  /// 애니메이션 효과 사용 여부
  final bool animated;

  /// 애니메이션 지속 시간 (밀리초)
  final int animationDuration;

  const ProgressIndicatorWidget({
    super.key,
    required this.progress,
    this.height = 24.0,
    this.width,
    this.showPercentage = true,
    this.animated = true,
    this.animationDuration = 800,
  });

  @override
  State<ProgressIndicatorWidget> createState() =>
      _ProgressIndicatorWidgetState();
}

class _ProgressIndicatorWidgetState extends State<ProgressIndicatorWidget>
    with SingleTickerProviderStateMixin {
  late AnimationController _controller;
  late Animation<double> _animation;

  @override
  void initState() {
    super.initState();

    if (widget.animated) {
      _controller = AnimationController(
        duration: Duration(milliseconds: widget.animationDuration),
        vsync: this,
      );

      _animation = Tween<double>(
        begin: 0.0,
        end: widget.progress.percentage / 100.0,
      ).animate(CurvedAnimation(
        parent: _controller,
        curve: Curves.easeOut,
      ));

      _controller.forward();
    }
  }

  @override
  void didUpdateWidget(ProgressIndicatorWidget oldWidget) {
    super.didUpdateWidget(oldWidget);

    if (widget.animated && oldWidget.progress.percentage != widget.progress.percentage) {
      _controller.reset();
      _animation = Tween<double>(
        begin: 0.0,
        end: widget.progress.percentage / 100.0,
      ).animate(CurvedAnimation(
        parent: _controller,
        curve: Curves.easeOut,
      ));
      _controller.forward();
    }
  }

  @override
  void dispose() {
    if (widget.animated) {
      _controller.dispose();
    }
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final double value = widget.progress.percentage / 100.0;

    return Container(
      width: widget.width,
      height: widget.height,
      decoration: BoxDecoration(
        borderRadius: BorderRadius.circular(widget.height / 2),
        color: Colors.grey[200],
      ),
      child: Stack(
        alignment: Alignment.center,
        children: [
          // 프로그레스 바
          if (widget.animated)
            AnimatedBuilder(
              animation: _animation,
              builder: (context, child) {
                return _buildProgressBar(_animation.value);
              },
            )
          else
            _buildProgressBar(value),

          // 백분율 텍스트
          if (widget.showPercentage)
            Text(
              '${widget.progress.formattedPercentage}%',
              style: TextStyle(
                fontSize: widget.height * 0.5,
                fontWeight: FontWeight.bold,
                color: _getTextColor(),
              ),
            ),
        ],
      ),
    );
  }

  /// 프로그레스 바 빌드
  Widget _buildProgressBar(double value) {
    return FractionallySizedBox(
      widthFactor: value.clamp(0.0, 1.0),
      heightFactor: 1.0,
      alignment: Alignment.centerLeft,
      child: Container(
        decoration: BoxDecoration(
          borderRadius: BorderRadius.circular(widget.height / 2),
          color: widget.progress.color,
        ),
      ),
    );
  }

  /// 텍스트 색상 결정 (진도율에 따라)
  Color _getTextColor() {
    final double percentage = widget.progress.percentage;

    // 진도율이 50% 이상이면 프로그레스 바 위에 흰색 텍스트
    if (percentage >= 50) {
      return Colors.white;
    }

    // 진도율이 50% 미만이면 배경 위에 진한 색 텍스트
    return Colors.black87;
  }
}

/// 원형 진도율 인디케이터 위젯
///
/// 원형 프로그레스 바로 진도율을 표시합니다.
class CircularProgressIndicatorWidget extends StatefulWidget {
  /// 표시할 진도율 데이터
  final Progress progress;

  /// 위젯 크기 (지름)
  final double size;

  /// 프로그레스 바 두께
  final double strokeWidth;

  /// 백분율 텍스트 표시 여부
  final bool showPercentage;

  /// 애니메이션 효과 사용 여부
  final bool animated;

  /// 애니메이션 지속 시간 (밀리초)
  final int animationDuration;

  const CircularProgressIndicatorWidget({
    super.key,
    required this.progress,
    this.size = 80.0,
    this.strokeWidth = 8.0,
    this.showPercentage = true,
    this.animated = true,
    this.animationDuration = 800,
  });

  @override
  State<CircularProgressIndicatorWidget> createState() =>
      _CircularProgressIndicatorWidgetState();
}

class _CircularProgressIndicatorWidgetState
    extends State<CircularProgressIndicatorWidget>
    with SingleTickerProviderStateMixin {
  late AnimationController _controller;
  late Animation<double> _animation;

  @override
  void initState() {
    super.initState();

    if (widget.animated) {
      _controller = AnimationController(
        duration: Duration(milliseconds: widget.animationDuration),
        vsync: this,
      );

      _animation = Tween<double>(
        begin: 0.0,
        end: widget.progress.percentage / 100.0,
      ).animate(CurvedAnimation(
        parent: _controller,
        curve: Curves.easeOut,
      ));

      _controller.forward();
    }
  }

  @override
  void didUpdateWidget(CircularProgressIndicatorWidget oldWidget) {
    super.didUpdateWidget(oldWidget);

    if (widget.animated && oldWidget.progress.percentage != widget.progress.percentage) {
      _controller.reset();
      _animation = Tween<double>(
        begin: 0.0,
        end: widget.progress.percentage / 100.0,
      ).animate(CurvedAnimation(
        parent: _controller,
        curve: Curves.easeOut,
      ));
      _controller.forward();
    }
  }

  @override
  void dispose() {
    if (widget.animated) {
      _controller.dispose();
    }
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final double value = widget.progress.percentage / 100.0;

    return SizedBox(
      width: widget.size,
      height: widget.size,
      child: Stack(
        alignment: Alignment.center,
        children: [
          // 원형 프로그레스 바
          if (widget.animated)
            AnimatedBuilder(
              animation: _animation,
              builder: (context, child) {
                return CircularProgressIndicator(
                  value: _animation.value,
                  strokeWidth: widget.strokeWidth,
                  backgroundColor: Colors.grey[200],
                  valueColor: AlwaysStoppedAnimation<Color>(widget.progress.color),
                );
              },
            )
          else
            CircularProgressIndicator(
              value: value,
              strokeWidth: widget.strokeWidth,
              backgroundColor: Colors.grey[200],
              valueColor: AlwaysStoppedAnimation<Color>(widget.progress.color),
            ),

          // 백분율 텍스트
          if (widget.showPercentage)
            Text(
              '${widget.progress.formattedPercentage}%',
              style: TextStyle(
                fontSize: widget.size * 0.2,
                fontWeight: FontWeight.bold,
                color: widget.progress.color,
              ),
            ),
        ],
      ),
    );
  }
}

/// 진도율 상태 배지 위젯
///
/// 진도율 상태를 작은 배지로 표시합니다.
class ProgressBadge extends StatelessWidget {
  /// 표시할 진도율 데이터
  final Progress progress;

  /// 배지 크기
  final double size;

  /// 아이콘 표시 여부
  final bool showIcon;

  const ProgressBadge({
    super.key,
    required this.progress,
    this.size = 60.0,
    this.showIcon = false,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      width: size,
      height: size,
      decoration: BoxDecoration(
        shape: BoxShape.circle,
        color: progress.color,
      ),
      child: Center(
        child: showIcon
            ? Icon(
                _getStatusIcon(),
                color: Colors.white,
                size: size * 0.5,
              )
            : Text(
                progress.formattedPercentage,
                style: TextStyle(
                  fontSize: size * 0.25,
                  fontWeight: FontWeight.bold,
                  color: Colors.white,
                ),
              ),
      ),
    );
  }

  IconData _getStatusIcon() {
    switch (progress.status) {
      case ProgressStatus.exceeded:
        return Icons.trending_up;
      case ProgressStatus.achieved:
        return Icons.check_circle;
      case ProgressStatus.insufficient:
        return Icons.trending_down;
    }
  }
}
