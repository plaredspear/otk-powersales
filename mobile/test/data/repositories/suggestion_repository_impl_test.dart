import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/data/datasources/suggestion_remote_datasource.dart';
import 'package:mobile/data/models/suggestion_detail_model.dart';
import 'package:mobile/data/models/suggestion_draft_request.dart';
import 'package:mobile/data/models/suggestion_list_item_model.dart';
import 'package:mobile/data/models/suggestion_register_request.dart';
import 'package:mobile/data/models/suggestion_register_result_model.dart';
import 'package:mobile/data/repositories/suggestion_repository_impl.dart';
import 'package:mobile/domain/entities/suggestion_draft.dart';
import 'package:mobile/domain/entities/suggestion_form.dart';

class _MockSuggestionRemoteDataSource implements SuggestionRemoteDataSource {
  SuggestionRegisterResultModel? result;
  Exception? error;
  SuggestionRegisterRequest? capturedRequest;
  SuggestionDraftRequest? capturedDraftRequest;
  SuggestionDraft? draftResult;
  bool deleteDraftCalled = false;

  @override
  Future<SuggestionRegisterResultModel> registerSuggestion(
      SuggestionRegisterRequest request) async {
    capturedRequest = request;
    if (error != null) throw error!;
    return result!;
  }

  @override
  Future<SuggestionListPageModel> getSuggestions({
    int page = 0,
    int size = 20,
    String? category,
    int? accountId,
    String? startDate,
    String? endDate,
  }) {
    throw UnimplementedError();
  }

  @override
  Future<SuggestionDetailModel> getSuggestionDetail(int suggestionId) {
    throw UnimplementedError();
  }

  @override
  Future<void> saveDraft(SuggestionDraftRequest request) async {
    capturedDraftRequest = request;
    if (error != null) throw error!;
  }

  @override
  Future<SuggestionDraft?> loadDraft() async {
    if (error != null) throw error!;
    return draftResult;
  }

  @override
  Future<void> deleteDraft() async {
    deleteDraftCalled = true;
    if (error != null) throw error!;
  }
}

void main() {
  group('SuggestionRepositoryImpl', () {
    late _MockSuggestionRemoteDataSource dataSource;
    late SuggestionRepositoryImpl repository;

    setUp(() {
      dataSource = _MockSuggestionRemoteDataSource();
      repository = SuggestionRepositoryImpl(dataSource);
    });

    group('registerSuggestion', () {
      test('신제품 제안 폼을 Request로 변환하여 DataSource를 호출한다', () async {
        // Given
        final form = SuggestionRegisterForm(
          category: SuggestionCategory.newProduct,
          title: '저당 라면 시리즈 출시 제안',
          content: '건강을 생각하는 저당 라면 시리즈를 출시하면 좋을 것 같습니다.',
        );
        final resultModel = SuggestionRegisterResultModel(
          id: 50,
          category: 'NEW_PRODUCT',
          categoryName: '신제품 제안',
          title: '저당 라면 시리즈 출시 제안',
          createdAt: '2026-02-11T11:00:00',
        );
        dataSource.result = resultModel;

        // When
        final result = await repository.registerSuggestion(form);

        // Then
        expect(dataSource.capturedRequest, isNotNull);
        expect(dataSource.capturedRequest!.category, 'NEW_PRODUCT');
        expect(dataSource.capturedRequest!.title, '저당 라면 시리즈 출시 제안');
        expect(result.id, 50);
        expect(result.category, SuggestionCategory.newProduct);
        expect(result.createdAt, DateTime(2026, 2, 11, 11, 0, 0));
      });

      test('기존제품 제안 폼을 Request로 변환하여 DataSource를 호출한다', () async {
        // Given
        final form = SuggestionRegisterForm(
          category: SuggestionCategory.existingProduct,
          productCode: '12345678',
          productName: '진라면',
          title: '진라면 매운맛 개선 제안',
          content: '매운맛을 더 강화하면 좋을 것 같습니다.',
        );
        final resultModel = SuggestionRegisterResultModel(
          id: 51,
          category: 'EXISTING_PRODUCT',
          categoryName: '기존제품 상품가치향상',
          productCode: '12345678',
          productName: '진라면',
          title: '진라면 매운맛 개선 제안',
          createdAt: '2026-02-11T14:30:00',
        );
        dataSource.result = resultModel;

        // When
        final result = await repository.registerSuggestion(form);

        // Then
        expect(dataSource.capturedRequest, isNotNull);
        expect(dataSource.capturedRequest!.category, 'EXISTING_PRODUCT');
        expect(dataSource.capturedRequest!.productCode, '12345678');
        expect(result.id, 51);
        expect(result.category, SuggestionCategory.existingProduct);
        expect(result.productCode, '12345678');
      });

      test('DataSource에서 에러가 발생하면 전파된다', () async {
        // Given
        final form = SuggestionRegisterForm(
          category: SuggestionCategory.newProduct,
          title: '신제품 제안',
          content: '제안 내용',
        );
        dataSource.error = Exception('Network error');

        // When & Then
        expect(
          () => repository.registerSuggestion(form),
          throwsA(
            predicate((e) =>
                e is Exception && e.toString().contains('Network error')),
          ),
        );
      });

      test('DataSource에서 서버 에러가 발생하면 전파된다', () async {
        // Given
        final form = SuggestionRegisterForm(
          category: SuggestionCategory.existingProduct,
          productCode: '12345678',
          productName: '진라면',
          title: '진라면 개선 제안',
          content: '제안 내용',
        );
        dataSource.error = Exception('Server error: 500');

        // When & Then
        expect(
          () => repository.registerSuggestion(form),
          throwsException,
        );
      });

      test('Model을 Entity로 올바르게 변환한다', () async {
        // Given
        final form = SuggestionRegisterForm(
          category: SuggestionCategory.newProduct,
          title: '신제품 제안',
          content: '제안 내용',
        );
        final resultModel = SuggestionRegisterResultModel(
          id: 52,
          category: 'NEW_PRODUCT',
          categoryName: '신제품 제안',
          productCode: null,
          productName: null,
          title: '신제품 제안',
          createdAt: '2026-02-12T10:00:00',
        );
        dataSource.result = resultModel;

        // When
        final result = await repository.registerSuggestion(form);

        // Then
        expect(result.id, 52);
        expect(result.category, SuggestionCategory.newProduct);
        expect(result.categoryName, '신제품 제안');
        expect(result.productCode, null);
        expect(result.productName, null);
        expect(result.title, '신제품 제안');
        expect(result.createdAt, DateTime(2026, 2, 12, 10, 0, 0));
      });
    });

    group('draft', () {
      test('saveDraft 는 폼을 DraftRequest 로 변환하여 DataSource 를 호출한다', () async {
        // Given
        final form = SuggestionRegisterForm(
          category: SuggestionCategory.logisticsClaim,
          title: '임시 제목',
          content: '임시 내용',
          accountId: 100,
        );

        // When
        await repository.saveDraft(form);

        // Then
        expect(dataSource.capturedDraftRequest, isNotNull);
        final json = dataSource.capturedDraftRequest!.toJson();
        expect(json['category'], 'LOGISTICS_CLAIM');
        expect(json['title'], '임시 제목');
        expect(json['accountId'], 100);
      });

      test('loadDraft 는 DataSource 의 entity 를 그대로 반환한다', () async {
        // Given
        dataSource.draftResult = const SuggestionDraft(
          category: 'NEW_PRODUCT',
          title: '저장된 제목',
        );

        // When
        final draft = await repository.loadDraft();

        // Then
        expect(draft, isNotNull);
        expect(draft!.category, 'NEW_PRODUCT');
        expect(draft.title, '저장된 제목');
      });

      test('loadDraft 는 draft 없으면 null 을 반환한다', () async {
        // Given
        dataSource.draftResult = null;

        // When & Then
        expect(await repository.loadDraft(), null);
      });

      test('deleteDraft 는 DataSource 를 호출한다', () async {
        // When
        await repository.deleteDraft();

        // Then
        expect(dataSource.deleteDraftCalled, true);
      });
    });
  });
}
