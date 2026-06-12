import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:intl/intl.dart';

import '../../../core/theme/app_colors.dart';
import '../../../core/theme/app_spacing.dart';
import '../../../core/theme/app_typography.dart';
import '../../../core/utils/error_utils.dart';
import '../../../core/utils/throttled_tap_mixin.dart';
import '../../../domain/entities/leader_account.dart';
import '../../providers/leader_schedule_provider.dart';
import '../../widgets/common/primary_button.dart';

/// 조장 진열 일정(마스터) 추가/변경 화면 — 레거시 `scheduleChange.jsp`(진열) 동등.
///
/// 진열 조회는 기간 마스터 기반이라 거래처/근무유형/기간 변경은 마스터를 편집한다.
/// (담당 여사원은 변경 대상 아님 — 진열 변경 화면 정합.) 변경/삭제는 기간 전체에 영향.
class LeaderDisplayScheduleEditScreen extends ConsumerStatefulWidget {
  /// 변경 모드: 대상 마스터 id. null 이면 추가 모드.
  final int? displayWorkScheduleId;

  /// 추가 모드 대상 여사원 (변경 모드에선 무시).
  final int? targetEmployeeId;

  /// 화면 표기용 여사원 이름.
  final String employeeName;

  /// 추가 모드 기본 시작일 (일별 현황 선택 날짜).
  final DateTime initialDate;

  const LeaderDisplayScheduleEditScreen({
    super.key,
    this.displayWorkScheduleId,
    this.targetEmployeeId,
    required this.employeeName,
    required this.initialDate,
  });

  bool get isEdit => displayWorkScheduleId != null;

  @override
  ConsumerState<LeaderDisplayScheduleEditScreen> createState() =>
      _LeaderDisplayScheduleEditScreenState();
}

class _LeaderDisplayScheduleEditScreenState
    extends ConsumerState<LeaderDisplayScheduleEditScreen> with ThrottledTapMixin {
  static const List<String> _type3Options = ['고정', '격고', '순회'];
  static const List<String> _type5Options = ['상시', '임시'];
  static const List<String> _type4Options = ['상온', '냉동/냉장'];

  int? _accountId;
  String? _accountName;
  DateTime? _startDate;
  DateTime? _endDate;
  String? _type3;
  String _type5 = '상시';
  String _type4 = '상온';

  bool _loading = false; // 초기 상세 로딩 (변경 모드)
  bool _submitting = false;

  @override
  void initState() {
    super.initState();
    if (widget.isEdit) {
      _loading = true;
      WidgetsBinding.instance.addPostFrameCallback((_) => _loadDetail());
    } else {
      _startDate = widget.initialDate;
    }
  }

  Future<void> _loadDetail() async {
    try {
      final detail = await ref
          .read(leaderScheduleRepositoryProvider)
          .getDisplaySchedule(widget.displayWorkScheduleId!);
      if (!mounted) return;
      setState(() {
        _accountId = detail.accountId;
        _accountName = detail.accountName;
        _startDate = detail.startDate ?? widget.initialDate;
        _endDate = detail.endDate;
        _type3 = detail.typeOfWork3;
        _type5 = detail.typeOfWork5 ?? '상시';
        _type4 = detail.typeOfWork4 ?? '상온';
        _loading = false;
      });
    } catch (e) {
      if (!mounted) return;
      setState(() => _loading = false);
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(extractErrorMessage(e))),
      );
      Navigator.of(context).pop(false);
    }
  }

  bool get _canSubmit =>
      _accountId != null && _startDate != null && _type3 != null && !_submitting;

  Future<void> _pickDate({required bool isStart}) async {
    final base = (isStart ? _startDate : _endDate) ?? _startDate ?? widget.initialDate;
    final picked = await showDatePicker(
      context: context,
      initialDate: base,
      firstDate: DateTime(base.year - 2, 1, 1),
      lastDate: DateTime(base.year + 2, 12, 31),
      helpText: isStart ? '시작일 선택' : '종료일 선택',
      cancelText: '취소',
      confirmText: '확인',
    );
    if (picked != null) {
      setState(() {
        if (isStart) {
          _startDate = picked;
        } else {
          _endDate = picked;
        }
      });
    }
  }

  Future<void> _pickAccount() async {
    throttledTapAsync(() async {
      final selected = await showModalBottomSheet<LeaderAccount>(
        context: context,
        isScrollControlled: true,
        shape: const RoundedRectangleBorder(
          borderRadius:
              BorderRadius.vertical(top: Radius.circular(AppSpacing.radiusLg)),
        ),
        builder: (_) => const _DisplayAccountPickerSheet(),
      );
      if (selected != null && mounted) {
        setState(() {
          _accountId = selected.id;
          _accountName = selected.name;
        });
      }
    });
  }

  Future<void> _submit() async {
    if (!_canSubmit) return;
    setState(() => _submitting = true);
    final notifier = ref.read(leaderDailyStatusProvider.notifier);
    final String? err;
    if (widget.isEdit) {
      err = await notifier.updateDisplaySchedule(
        displayWorkScheduleId: widget.displayWorkScheduleId!,
        accountId: _accountId!,
        startDate: _startDate!,
        endDate: _endDate,
        typeOfWork3: _type3!,
        typeOfWork4: _type4,
        typeOfWork5: _type5,
      );
    } else {
      err = await notifier.createDisplaySchedule(
        targetEmployeeId: widget.targetEmployeeId!,
        accountId: _accountId!,
        startDate: _startDate!,
        endDate: _endDate,
        typeOfWork3: _type3!,
        typeOfWork4: _type4,
        typeOfWork5: _type5,
      );
    }
    if (!mounted) return;
    setState(() => _submitting = false);
    final messenger = ScaffoldMessenger.of(context);
    if (err == null) {
      messenger.showSnackBar(SnackBar(
        content: Text(widget.isEdit ? '진열 일정이 변경되었습니다.' : '진열 일정이 등록되었습니다.'),
      ));
      Navigator.of(context).pop(true);
    } else {
      messenger.showSnackBar(SnackBar(content: Text(err)));
    }
  }

  Future<void> _delete() async {
    throttledTapAsync(() async {
      final ok = await showDialog<bool>(
        context: context,
        builder: (ctx) => AlertDialog(
          title: const Text('진열 일정 삭제'),
          content: const Text('이 진열 배정을 삭제하시겠습니까? 기간 전체가 해제됩니다.'),
          actions: [
            TextButton(
                onPressed: () => Navigator.of(ctx).pop(false),
                child: const Text('취소')),
            TextButton(
              onPressed: () => Navigator.of(ctx).pop(true),
              child: const Text('삭제', style: TextStyle(color: AppColors.error)),
            ),
          ],
        ),
      );
      if (ok != true || !mounted) return;
      setState(() => _submitting = true);
      final err = await ref
          .read(leaderDailyStatusProvider.notifier)
          .deleteDisplaySchedule(widget.displayWorkScheduleId!);
      if (!mounted) return;
      setState(() => _submitting = false);
      final messenger = ScaffoldMessenger.of(context);
      if (err == null) {
        messenger
            .showSnackBar(const SnackBar(content: Text('진열 일정이 삭제되었습니다.')));
        Navigator.of(context).pop(true);
      } else {
        messenger.showSnackBar(SnackBar(content: Text(err)));
      }
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text(widget.isEdit ? '진열 일정 변경' : '진열 일정 추가'),
      ),
      body: _loading
          ? const Center(child: CircularProgressIndicator(color: AppColors.secondary))
          : SafeArea(
              child: SingleChildScrollView(
                padding: const EdgeInsets.all(AppSpacing.lg),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    _label('대상 여사원'),
                    _readonly(widget.employeeName),
                    const SizedBox(height: AppSpacing.lg),
                    _label('거래처 (필수)'),
                    InkWell(
                      onTap: _pickAccount,
                      borderRadius: BorderRadius.circular(AppSpacing.radiusMd),
                      child: _boxed(Row(children: [
                        Expanded(
                          child: Text(
                            _accountName ?? '거래처를 선택하세요',
                            style: AppTypography.bodyMedium.copyWith(
                              color: _accountName == null
                                  ? AppColors.textTertiary
                                  : AppColors.textPrimary,
                            ),
                          ),
                        ),
                        const Icon(Icons.search, size: 18),
                      ])),
                    ),
                    const SizedBox(height: AppSpacing.lg),
                    Row(children: [
                      Expanded(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            _label('시작일 (필수)'),
                            _dateField(_startDate, () => _pickDate(isStart: true)),
                          ],
                        ),
                      ),
                      const SizedBox(width: AppSpacing.md),
                      Expanded(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            _label('종료일 (선택)'),
                            _dateField(_endDate, () => _pickDate(isStart: false),
                                hint: '무기한'),
                          ],
                        ),
                      ),
                    ]),
                    const SizedBox(height: AppSpacing.lg),
                    _label('근무유형 (필수)'),
                    _dropdown(_type3, _type3Options, '고정 / 격고 / 순회',
                        (v) => setState(() => _type3 = v)),
                    const SizedBox(height: AppSpacing.md),
                    _label('진열 구분'),
                    _dropdown(_type5, _type5Options, '상시 / 임시',
                        (v) => setState(() => _type5 = v ?? '상시')),
                    const SizedBox(height: AppSpacing.md),
                    _label('온도 구분'),
                    _dropdown(_type4, _type4Options, '상온 / 냉동·냉장',
                        (v) => setState(() => _type4 = v ?? '상온')),
                    const SizedBox(height: AppSpacing.xxl),
                    PrimaryButton(
                      text: '저장',
                      isLoading: _submitting,
                      onPressed: _canSubmit ? _submit : null,
                    ),
                    if (widget.isEdit) ...[
                      const SizedBox(height: AppSpacing.md),
                      SizedBox(
                        width: double.infinity,
                        child: OutlinedButton(
                          style: OutlinedButton.styleFrom(
                            foregroundColor: AppColors.error,
                            side: const BorderSide(color: AppColors.error),
                            padding: const EdgeInsets.symmetric(
                                vertical: AppSpacing.md),
                          ),
                          onPressed: _submitting ? null : _delete,
                          child: const Text('삭제'),
                        ),
                      ),
                    ],
                  ],
                ),
              ),
            ),
    );
  }

  Widget _label(String t) => Padding(
        padding: const EdgeInsets.only(bottom: AppSpacing.xs),
        child: Text(t,
            style: AppTypography.bodyMedium.copyWith(
                fontWeight: FontWeight.w700, color: AppColors.textPrimary)),
      );

  Widget _boxed(Widget child) => Container(
        width: double.infinity,
        padding: const EdgeInsets.symmetric(
            horizontal: AppSpacing.md, vertical: AppSpacing.md),
        decoration: BoxDecoration(
          color: AppColors.white,
          borderRadius: BorderRadius.circular(AppSpacing.radiusMd),
          border: Border.all(color: AppColors.border),
        ),
        child: child,
      );

  Widget _readonly(String value) => Container(
        width: double.infinity,
        padding: const EdgeInsets.symmetric(
            horizontal: AppSpacing.md, vertical: AppSpacing.md),
        decoration: BoxDecoration(
          color: AppColors.surface,
          borderRadius: BorderRadius.circular(AppSpacing.radiusMd),
          border: Border.all(color: AppColors.border),
        ),
        child: Text(value, style: AppTypography.bodyMedium),
      );

  Widget _dateField(DateTime? date, VoidCallback onTap, {String hint = '선택'}) {
    final label = date == null
        ? hint
        : DateFormat('yyyy-MM-dd', 'ko_KR').format(date);
    return InkWell(
      onTap: onTap,
      borderRadius: BorderRadius.circular(AppSpacing.radiusMd),
      child: _boxed(Row(children: [
        Expanded(
          child: Text(label,
              style: AppTypography.bodyMedium.copyWith(
                  color: date == null
                      ? AppColors.textTertiary
                      : AppColors.textPrimary)),
        ),
        const Icon(Icons.calendar_today, size: 16),
      ])),
    );
  }

  Widget _dropdown(String? value, List<String> options, String hint,
      ValueChanged<String?> onChanged) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: AppSpacing.md),
      decoration: BoxDecoration(
        color: AppColors.white,
        borderRadius: BorderRadius.circular(AppSpacing.radiusMd),
        border: Border.all(color: AppColors.border),
      ),
      child: DropdownButtonHideUnderline(
        child: DropdownButton<String>(
          value: value,
          isExpanded: true,
          hint: Text(hint,
              style: AppTypography.bodyMedium
                  .copyWith(color: AppColors.textTertiary)),
          items: options
              .map((o) => DropdownMenuItem<String>(value: o, child: Text(o)))
              .toList(),
          onChanged: onChanged,
        ),
      ),
    );
  }
}

/// 진열 거래처 선택 시트 (조장 거래처, 검색). 자체적으로 repository 에서 조회.
class _DisplayAccountPickerSheet extends ConsumerStatefulWidget {
  const _DisplayAccountPickerSheet();

  @override
  ConsumerState<_DisplayAccountPickerSheet> createState() =>
      _DisplayAccountPickerSheetState();
}

class _DisplayAccountPickerSheetState
    extends ConsumerState<_DisplayAccountPickerSheet> {
  final TextEditingController _keyword = TextEditingController();
  bool _loading = true;
  String? _error;
  List<LeaderAccount> _accounts = const [];

  @override
  void initState() {
    super.initState();
    _load();
  }

  @override
  void dispose() {
    _keyword.dispose();
    super.dispose();
  }

  Future<void> _load({String? keyword}) async {
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      final list = await ref
          .read(leaderScheduleRepositoryProvider)
          .getAccounts(keyword: keyword);
      if (!mounted) return;
      setState(() {
        _accounts = list;
        _loading = false;
      });
    } catch (e) {
      if (!mounted) return;
      setState(() {
        _error = extractErrorMessage(e);
        _loading = false;
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    return DraggableScrollableSheet(
      expand: false,
      initialChildSize: 0.7,
      maxChildSize: 0.9,
      builder: (_, scrollController) {
        return Padding(
          padding: const EdgeInsets.symmetric(
              horizontal: AppSpacing.lg, vertical: AppSpacing.md),
          child: Column(
            children: [
              TextField(
                controller: _keyword,
                decoration: InputDecoration(
                  hintText: '거래처명/주소 검색',
                  prefixIcon: const Icon(Icons.search),
                  border: OutlineInputBorder(
                      borderRadius:
                          BorderRadius.circular(AppSpacing.radiusMd)),
                ),
                onSubmitted: (v) => _load(keyword: v.trim()),
              ),
              const SizedBox(height: AppSpacing.md),
              Expanded(child: _buildList(scrollController)),
            ],
          ),
        );
      },
    );
  }

  Widget _buildList(ScrollController controller) {
    if (_loading) {
      return const Center(
          child: CircularProgressIndicator(color: AppColors.secondary));
    }
    if (_error != null) {
      return Center(child: Text(_error!, style: AppTypography.bodyMedium));
    }
    if (_accounts.isEmpty) {
      return const Center(child: Text('거래처가 없습니다.'));
    }
    return ListView.separated(
      controller: controller,
      itemCount: _accounts.length,
      separatorBuilder: (_, _) => const Divider(height: 1),
      itemBuilder: (_, i) {
        final a = _accounts[i];
        return ListTile(
          title: Text(a.name ?? '-'),
          subtitle: a.address1 != null ? Text(a.address1!) : null,
          onTap: () => Navigator.of(context).pop(a),
        );
      },
    );
  }
}
