import 'dart:io';

import '../../domain/entities/inspection_field_type.dart';
import '../../domain/entities/inspection_form.dart';
import '../../domain/entities/inspection_list_item.dart';
import '../../domain/entities/inspection_theme.dart';

/// 현장 점검 등록 화면 상태
class InspectionRegisterState {
  /// 로딩 상태
  final bool isLoading;

  /// 에러 메시지
  final String? errorMessage;

  /// 등록 폼 데이터
  final InspectionRegisterForm? form;

  /// 테마 목록
  final List<InspectionTheme> themes;

  /// 현장 유형 목록
  final List<InspectionFieldType> fieldTypes;

  /// 거래처 목록 (ID -> 이름)
  final Map<int, String> stores;

  /// 선택된 제품명 (자사 점검 시)
  final String? selectedProductName;

  /// 임시 저장 데이터 존재 여부
  final bool hasDraft;

  const InspectionRegisterState({
    this.isLoading = false,
    this.errorMessage,
    this.form,
    this.themes = const [],
    this.fieldTypes = const [],
    this.stores = const {},
    this.selectedProductName,
    this.hasDraft = false,
  });

  /// 초기 상태
  factory InspectionRegisterState.initial() {
    return InspectionRegisterState(
      form: InspectionRegisterForm(
        themeId: 0,
        category: InspectionCategory.OWN,
        storeId: 0,
        inspectionDate: DateTime.now(),
        fieldTypeCode: '',
        photos: const [],
      ),
    );
  }

  /// 로딩 상태로 전환
  InspectionRegisterState toLoading() {
    return copyWith(isLoading: true, errorMessage: null);
  }

  /// 에러 상태로 전환
  InspectionRegisterState toError(String message) {
    return copyWith(isLoading: false, errorMessage: message);
  }

  /// 데이터 로드 완료
  InspectionRegisterState toLoaded({
    required List<InspectionTheme> themes,
    required List<InspectionFieldType> fieldTypes,
    required Map<int, String> stores,
  }) {
    return copyWith(
      isLoading: false,
      errorMessage: null,
      themes: themes,
      fieldTypes: fieldTypes,
      stores: stores,
    );
  }

  /// 분류 (자사/경쟁사)
  InspectionCategory get category => form?.category ?? InspectionCategory.OWN;

  /// 자사 점검 여부
  bool get isOwn => category == InspectionCategory.OWN;

  /// 경쟁사 점검 여부
  bool get isCompetitor => category == InspectionCategory.COMPETITOR;

  /// 시식 여부 (경쟁사 점검 시)
  bool? get competitorTasting => form?.competitorTasting;

  /// 시식=예 여부
  bool get hasTasting => competitorTasting == true;

  /// 선택된 테마
  InspectionTheme? get selectedTheme {
    if (form == null || form!.themeId <= 0) return null;
    try {
      return themes.firstWhere((t) => t.id == form!.themeId);
    } catch (_) {
      return null;
    }
  }

  /// 선택된 거래처명
  String? get selectedStoreName {
    if (form == null || form!.storeId <= 0) return null;
    return stores[form!.storeId];
  }

  /// 선택된 현장 유형
  InspectionFieldType? get selectedFieldType {
    if (form == null || form!.fieldTypeCode.isEmpty) return null;
    try {
      return fieldTypes.firstWhere((ft) => ft.code == form!.fieldTypeCode);
    } catch (_) {
      return null;
    }
  }

  /// 사진 개수
  int get photoCount => form?.photos.length ?? 0;

  /// 사진 추가 가능 여부 (최대 2장)
  bool get canAddPhoto => photoCount < 2;

  /// 폼 유효성 검증
  ValidationResult validate() {
    if (form == null) {
      return const ValidationResult(
        isValid: false,
        errors: ['폼 데이터가 없습니다'],
      );
    }
    return form!.validate();
  }

  InspectionRegisterState copyWith({
    bool? isLoading,
    String? errorMessage,
    bool clearError = false,
    InspectionRegisterForm? form,
    List<InspectionTheme>? themes,
    List<InspectionFieldType>? fieldTypes,
    Map<int, String>? stores,
    String? selectedProductName,
    bool clearProductName = false,
    bool? hasDraft,
  }) {
    return InspectionRegisterState(
      isLoading: isLoading ?? this.isLoading,
      errorMessage: clearError ? null : (errorMessage ?? this.errorMessage),
      form: form ?? this.form,
      themes: themes ?? this.themes,
      fieldTypes: fieldTypes ?? this.fieldTypes,
      stores: stores ?? this.stores,
      selectedProductName: clearProductName
          ? null
          : (selectedProductName ?? this.selectedProductName),
      hasDraft: hasDraft ?? this.hasDraft,
    );
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    if (other is! InspectionRegisterState) return false;
    return other.isLoading == isLoading &&
        other.errorMessage == errorMessage &&
        other.form == form &&
        _listEquals(other.themes, themes) &&
        _listEquals(other.fieldTypes, fieldTypes) &&
        _mapEquals(other.stores, stores) &&
        other.selectedProductName == selectedProductName &&
        other.hasDraft == hasDraft;
  }

  bool _listEquals<T>(List<T> a, List<T> b) {
    if (a.length != b.length) return false;
    for (int i = 0; i < a.length; i++) {
      if (a[i] != b[i]) return false;
    }
    return true;
  }

  bool _mapEquals<K, V>(Map<K, V> a, Map<K, V> b) {
    if (a.length != b.length) return false;
    for (final key in a.keys) {
      if (!b.containsKey(key) || a[key] != b[key]) return false;
    }
    return true;
  }

  @override
  int get hashCode {
    return Object.hash(
      isLoading,
      errorMessage,
      form,
      Object.hashAll(themes),
      Object.hashAll(fieldTypes),
      Object.hashAll(stores.entries),
      selectedProductName,
      hasDraft,
    );
  }

  @override
  String toString() {
    return 'InspectionRegisterState(isLoading: $isLoading, '
        'errorMessage: $errorMessage, form: $form, '
        'themes: ${themes.length}, fieldTypes: ${fieldTypes.length}, '
        'stores: ${stores.length}, selectedProductName: $selectedProductName, '
        'hasDraft: $hasDraft)';
  }
}
