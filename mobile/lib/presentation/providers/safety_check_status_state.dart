import '../../data/models/safety_check_status_model.dart';

/// 안전점검 현황 화면 상태
class SafetyCheckStatusState {
  final DateTime selectedDate;
  final SafetyCheckStatusModel? data;
  final bool isLoading;
  final String? errorMessage;
  final Set<int> expandedCardIds;

  const SafetyCheckStatusState({
    required this.selectedDate,
    this.data,
    this.isLoading = false,
    this.errorMessage,
    this.expandedCardIds = const {},
  });

  factory SafetyCheckStatusState.initial() {
    return SafetyCheckStatusState(selectedDate: DateTime.now());
  }

  SafetyCheckStatusState toLoading() {
    return SafetyCheckStatusState(
      selectedDate: selectedDate,
      data: data,
      isLoading: true,
    );
  }

  SafetyCheckStatusState toLoaded(SafetyCheckStatusModel newData) {
    return SafetyCheckStatusState(
      selectedDate: selectedDate,
      data: newData,
    );
  }

  SafetyCheckStatusState toError(String message) {
    return SafetyCheckStatusState(
      selectedDate: selectedDate,
      data: data,
      errorMessage: message,
    );
  }

  SafetyCheckStatusState withDate(DateTime date) {
    return SafetyCheckStatusState(
      selectedDate: date,
      data: data,
      isLoading: isLoading,
      errorMessage: errorMessage,
    );
  }

  SafetyCheckStatusState toggleCard(int memberId) {
    final newSet = Set<int>.from(expandedCardIds);
    if (newSet.contains(memberId)) {
      newSet.remove(memberId);
    } else {
      newSet.add(memberId);
    }
    return SafetyCheckStatusState(
      selectedDate: selectedDate,
      data: data,
      isLoading: isLoading,
      errorMessage: errorMessage,
      expandedCardIds: newSet,
    );
  }

  bool get isError => errorMessage != null;
  bool get isEmpty => data != null && data!.members.isEmpty;

  List<MemberStatusModel> get submittedMembers =>
      data?.members.where((m) => m.submitted).toList() ?? [];

  List<MemberStatusModel> get notSubmittedMembers =>
      data?.members.where((m) => !m.submitted).toList() ?? [];

  String get dateString {
    final d = selectedDate;
    return '${d.year}-${d.month.toString().padLeft(2, '0')}-${d.day.toString().padLeft(2, '0')}';
  }
}
