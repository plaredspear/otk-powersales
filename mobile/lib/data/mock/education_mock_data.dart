import '../../domain/entities/education_category.dart';
import '../../domain/entities/education_post.dart';
import '../../domain/entities/education_post_detail.dart';

/// 교육 Mock 데이터
class EducationMockData {
  /// 시식 매뉴얼 카테고리 게시물 목록
  static final List<EducationPost> tastingManualPosts = [
    EducationPost(
      id: 10,
      title: '이미지',
      createdAt: DateTime.parse('2020-08-11T00:00:00'),
    ),
    EducationPost(
      id: 9,
      title: '진짬뽕 시식 매뉴얼',
      createdAt: DateTime.parse('2020-08-10T00:00:00'),
    ),
    EducationPost(
      id: 8,
      title: '동영상 테스트',
      createdAt: DateTime.parse('2020-08-06T00:00:00'),
    ),
    EducationPost(
      id: 7,
      title: '미숫가루 시식매뉴얼',
      createdAt: DateTime.parse('2020-08-06T00:00:00'),
    ),
  ];

  /// CS/안전 카테고리 게시물 목록
  static final List<EducationPost> csSafetyPosts = [
    EducationPost(
      id: 20,
      title: '고객 응대 매뉴얼',
      createdAt: DateTime.parse('2020-08-15T00:00:00'),
    ),
    EducationPost(
      id: 19,
      title: '안전사고 대응 가이드',
      createdAt: DateTime.parse('2020-08-14T00:00:00'),
    ),
  ];

  /// 교육 평가 카테고리 게시물 목록
  static final List<EducationPost> evaluationPosts = [
    EducationPost(
      id: 30,
      title: '2020년 상반기 교육 평가',
      createdAt: DateTime.parse('2020-07-01T00:00:00'),
    ),
  ];

  /// 신제품 소개 카테고리 게시물 목록
  static final List<EducationPost> newProductPosts = [
    EducationPost(
      id: 40,
      title: '2020년 신제품 라인업',
      createdAt: DateTime.parse('2020-08-20T00:00:00'),
    ),
    EducationPost(
      id: 39,
      title: '진짬뽕 출시 안내',
      createdAt: DateTime.parse('2020-08-01T00:00:00'),
    ),
  ];

  /// 게시물 상세 (ID: 9 - 진짬뽕 시식 매뉴얼)
  static final EducationPostDetail postDetail9 = EducationPostDetail(
    id: 9,
    category: EducationCategory.tastingManual,
    categoryName: '시식 매뉴얼',
    title: '진짬뽕 시식 매뉴얼',
    content: '''진짬뽕 시식 매뉴얼

1. 시식 준비
- 조리 전 손 세척 필수
- 조리 시간: 3분 30초
- 물 500ml 끓이기

2. 조리 방법
- 끓는 물에 면과 스프를 넣는다
- 중불에서 3분간 끓인다
- 계란을 넣어 완성한다

3. 시식 제공
- 적당한 크기의 그릇에 담는다
- 일회용 젓가락과 포크 제공
- 냅킨과 휴지 비치

4. 주의사항
- 뜨거우므로 화상 주의
- 알레르기 유발 성분 고지 필수
- 시식 후 설문조사 진행''',
    createdAt: DateTime.parse('2020-08-10T00:00:00'),
    images: [
      EducationImage(
        id: 1,
        url: 'https://picsum.photos/seed/edu9-1/800/600',
        sortOrder: 1,
      ),
      EducationImage(
        id: 2,
        url: 'https://picsum.photos/seed/edu9-2/800/600',
        sortOrder: 2,
      ),
    ],
    attachments: [
      EducationAttachment(
        id: 1,
        fileName: '진짬뽕_시식_가이드.pdf',
        fileUrl: 'https://example.com/files/tasting-guide.pdf',
        fileSize: 2048576, // 2 MB
      ),
      EducationAttachment(
        id: 2,
        fileName: '고객_설문지.xlsx',
        fileUrl: 'https://example.com/files/survey.xlsx',
        fileSize: 512000, // 500 KB
      ),
    ],
  );

  /// 게시물 상세 (ID: 7 - 미숫가루 시식매뉴얼, 이미지/첨부파일 없음)
  static final EducationPostDetail postDetail7 = EducationPostDetail(
    id: 7,
    category: EducationCategory.tastingManual,
    categoryName: '시식 매뉴얼',
    title: '미숫가루 시식매뉴얼',
    content: '''미숫가루 시식 매뉴얼

1. 시식 준비
- 미숫가루 30g
- 우유 또는 물 200ml
- 얼음 (선택)

2. 조리 방법
- 미숫가루를 컵에 넣는다
- 우유 또는 물을 부어 잘 섞는다
- 얼음을 추가하여 시원하게 제공

3. 시식 제공
- 일회용 컵과 빨대 제공
- 시음 후 피드백 수집

4. 주의사항
- 우유 알레르기 확인 필수
- 보관 시 밀봉 보관''',
    createdAt: DateTime.parse('2020-08-06T00:00:00'),
    images: [],
    attachments: [],
  );

  /// 게시물 상세 (ID: 20 - 고객 응대 매뉴얼)
  static final EducationPostDetail postDetail20 = EducationPostDetail(
    id: 20,
    category: EducationCategory.csSafety,
    categoryName: 'CS/안전',
    title: '고객 응대 매뉴얼',
    content: '''고객 응대 매뉴얼

1. 기본 원칙
- 고객을 먼저 생각한다
- 친절하고 정중한 태도 유지
- 신속한 문제 해결

2. 응대 절차
- 인사 및 경청
- 문제 파악
- 해결 방안 제시
- 확인 및 감사 인사

3. 주의사항
- 감정적 대응 금지
- 고객 정보 보호
- 상급자 보고 기준 숙지''',
    createdAt: DateTime.parse('2020-08-15T00:00:00'),
    images: [
      EducationImage(
        id: 3,
        url: 'https://picsum.photos/seed/edu20-1/800/600',
        sortOrder: 1,
      ),
    ],
    attachments: [],
  );

  /// 카테고리별 게시물 목록 조회
  static List<EducationPost> getPostsByCategory(EducationCategory category) {
    switch (category) {
      case EducationCategory.tastingManual:
        return tastingManualPosts;
      case EducationCategory.csSafety:
        return csSafetyPosts;
      case EducationCategory.evaluation:
        return evaluationPosts;
      case EducationCategory.newProduct:
        return newProductPosts;
    }
  }

  /// 게시물 ID로 상세 조회
  static EducationPostDetail? getPostDetailById(int postId) {
    switch (postId) {
      case 9:
        return postDetail9;
      case 7:
        return postDetail7;
      case 20:
        return postDetail20;
      default:
        return null; // 404 시뮬레이션
    }
  }
}
