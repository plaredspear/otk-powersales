import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

/// 외부 상태(value)를 단방향으로 반영하면서 커서 위치를 보존하는 TextField 래퍼.
///
/// StatelessWidget 의 build() 안에서 `TextEditingController(text: ...)` 를 생성하면
/// 매 리빌드(키 입력마다 발생)에서 컨트롤러가 재생성되어 커서가 맨 앞으로 튀는
/// 버그가 발생한다. 이 위젯은 컨트롤러를 State 에 보관하고 didUpdateWidget 으로
/// 외부 값과만 동기화하므로, 부모가 StatelessWidget 이어도 안전하게 입력할 수 있다.
class SyncedTextField extends StatefulWidget {
  const SyncedTextField({
    super.key,
    required this.value,
    required this.onChanged,
    this.decoration,
    this.keyboardType,
    this.inputFormatters,
    this.maxLines = 1,
    this.style,
    this.textInputAction,
  });

  /// 외부 상태가 보유한 현재 값 (null 은 빈 문자열로 취급)
  final String value;

  /// 입력 변경 콜백
  final ValueChanged<String> onChanged;

  final InputDecoration? decoration;
  final TextInputType? keyboardType;
  final List<TextInputFormatter>? inputFormatters;
  final int? maxLines;
  final TextStyle? style;
  final TextInputAction? textInputAction;

  @override
  State<SyncedTextField> createState() => _SyncedTextFieldState();
}

class _SyncedTextFieldState extends State<SyncedTextField> {
  late final TextEditingController _controller;

  @override
  void initState() {
    super.initState();
    _controller = TextEditingController(text: widget.value);
  }

  @override
  void didUpdateWidget(covariant SyncedTextField oldWidget) {
    super.didUpdateWidget(oldWidget);
    // 외부 값이 컨트롤러와 다를 때만 동기화 (입력 중 커서 튐 방지)
    if (widget.value != _controller.text) {
      _controller.value = TextEditingValue(
        text: widget.value,
        selection: TextSelection.collapsed(offset: widget.value.length),
      );
    }
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return TextField(
      controller: _controller,
      onChanged: widget.onChanged,
      decoration: widget.decoration,
      keyboardType: widget.keyboardType,
      inputFormatters: widget.inputFormatters,
      maxLines: widget.maxLines,
      style: widget.style,
      textInputAction: widget.textInputAction,
    );
  }
}
