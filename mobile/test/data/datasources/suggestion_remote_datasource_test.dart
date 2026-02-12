import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/data/datasources/suggestion_remote_datasource.dart';
import 'package:mobile/data/models/suggestion_register_request.dart';
import 'package:mobile/data/models/suggestion_register_result_model.dart';

class _MockSuggestionRemoteDataSource implements SuggestionRemoteDataSource {
  SuggestionRegisterResultModel? result;
  Exception? error;

  @override
  Future<SuggestionRegisterResultModel> registerSuggestion(
      SuggestionRegisterRequest request) async {
    if (error != null) throw error!;
    return result!;
  }
}

void main() {
  group('SuggestionRemoteDataSource', () {
    late _MockSuggestionRemoteDataSource dataSource;

    setUp(() {
      dataSource = _MockSuggestionRemoteDataSource();
    });

    test('registerSuggestion 메서드가 호출 가능하다', () async {
      // Given
      final request = SuggestionRegisterRequest(
        category: 'NEW_PRODUCT',
        title: '신제품 제안',
        content: '제안 내용',
      );
      final expectedResult = SuggestionRegisterResultModel(
        id: 50,
        category: 'NEW_PRODUCT',
        categoryName: '신제품 제안',
        title: '신제품 제안',
        createdAt: '2026-02-11T11:00:00',
      );
      dataSource.result = expectedResult;

      // When
      final result = await dataSource.registerSuggestion(request);

      // Then
      expect(result, expectedResult);
    });

    test('DataSource에서 에러가 발생하면 전파된다', () async {
      // Given
      final request = SuggestionRegisterRequest(
        category: 'NEW_PRODUCT',
        title: '신제품 제안',
        content: '제안 내용',
      );
      dataSource.error = Exception('Network error');

      // When & Then
      expect(
        () => dataSource.registerSuggestion(request),
        throwsException,
      );
    });
  });
}
