# Web Conventions (React + TanStack Query + Ant Design)

> 이 문서는 실제 프로젝트 코드에서 추출한 패턴입니다. 새 기능 구현 시 이 패턴을 따르세요.

---

## 디렉토리 구조

```
web/src/
├── api/                    # API 클라이언트 함수 (도메인별 파일)
├── components/             # 재사용 가능한 UI 컴포넌트
├── config/                 # 설정 파일 (menuConfig.tsx)
├── contexts/               # React Context (BreadcrumbContext)
├── hooks/                  # 커스텀 훅 (도메인별 폴더)
│   ├── common/             # 공통 유틸리티 훅
│   ├── notice/             # useNotices, useNoticeMutation 등
│   └── [domain]/           # 도메인별 query/mutation 훅
├── layouts/                # 페이지 레이아웃 (AdminLayout)
├── lib/                    # 유틸리티 (queryClient)
├── pages/                  # 페이지 컴포넌트 (도메인별 폴더)
│   └── [domain]/
│       ├── [Domain]ListPage.tsx
│       ├── [Domain]DetailPage.tsx
│       ├── [Domain]FormPage.tsx
│       └── components/     # 페이지 전용 컴포넌트
├── stores/                 # Zustand 스토어 (authStore, forbiddenStore)
├── routes.tsx              # 라우트 정의
└── App.tsx                 # 라우터 프로바이더
```

---

## 데이터 흐름

```
Page → useQuery/useMutation (hooks/) → API function (api/) → axios client (api/client.ts) → Backend
```

**상태 관리 구분:**
- **서버 상태**: TanStack Query (useQuery, useMutation)
- **전역 클라이언트 상태**: Zustand (authStore)
- **로컬 UI 상태**: React useState

---

## API 함수 (api/)

```typescript
// api/notice.ts
export async function fetchNotices(params: NoticeListParams) {
  const { data } = await client.get('/api/v1/admin/notices', { params });
  return data.data;  // ApiResponse 언래핑
}

export async function createNotice(payload: NoticeCreateRequest) {
  const { data } = await client.post('/api/v1/admin/notices', payload);
  return data.data;
}
```

**규칙**:
- Axios 인스턴스(`client`) 사용
- `response.data.data`로 ApiResponse 언래핑
- snake_case → camelCase 변환은 API 함수에서 처리

---

## Query 훅 (hooks/)

```typescript
// hooks/notice/useNotices.ts
export function useNotices(params: NoticeListParams) {
  return useQuery({
    queryKey: ['admin', 'notices', params.category, params.search, params.page, params.size],
    queryFn: () => fetchNotices(params),
  });
}

export function useNoticeDetail(id: number) {
  return useQuery({
    queryKey: ['admin', 'notices', id],
    queryFn: () => fetchNoticeDetail(id),
    enabled: id > 0,
  });
}
```

**규칙**:
- queryKey는 `['admin', domain, ...params]` 형식
- `enabled` 옵션으로 조건부 fetch 제어
- 메타 데이터(드롭다운 옵션 등)는 `staleTime: 10분` 설정

---

## Mutation 훅 (hooks/)

```typescript
// hooks/notice/useNoticeMutation.ts
export function useCreateNotice() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: createNotice,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'notices'] });
    },
  });
}
```

**규칙**:
- `onSuccess`에서 관련 queryKey 무효화
- Create/Update/Delete 각각 별도 함수로 export
- 하나의 도메인 mutation 파일에 모든 mutation 정의

---

## List 페이지 패턴

```typescript
function NoticeListPage() {
  const navigate = useNavigate();
  const [category, setCategory] = useState<string>();
  const [search, setSearch] = useState('');
  const [page, setPage] = useState(1);

  const { data, isLoading } = useNotices({ category, search, page, size: 20 });

  return (
    <>
      {/* 필터 영역 */}
      <Space>
        <Select onChange={setCategory} />
        <Input.Search onSearch={setSearch} />
      </Space>

      {/* 생성 버튼 */}
      <Button type="primary" icon={<PlusOutlined />} onClick={() => navigate('/notices/new')}>
        작성
      </Button>

      {/* 테이블 */}
      <Table
        dataSource={data?.content}
        loading={isLoading}
        pagination={{ current: page, total: data?.totalElements, onChange: setPage }}
        onRow={(record) => ({
          onClick: () => navigate(`/notices/${record.id}`),
          style: { cursor: 'pointer' },
        })}
      />
    </>
  );
}
```

**규칙**:
- 필터 상태는 useState로 관리
- pagination은 서버 사이드 (page/size)
- 테이블 행 클릭으로 상세 페이지 이동

---

## Form 페이지 패턴

```typescript
function NoticeFormPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [form] = Form.useForm();
  const isEdit = !!id;

  const { data: detail } = useNoticeDetail(Number(id) || 0);
  const createMutation = useCreateNotice();
  const updateMutation = useUpdateNotice();
  const isSubmitting = createMutation.isPending || updateMutation.isPending;

  const handleFinish = async (values: NoticeFormValues) => {
    if (isEdit) {
      await updateMutation.mutateAsync({ id: Number(id), ...values });
    } else {
      await createMutation.mutateAsync(values);
    }
    navigate('/notices');
  };

  return (
    <Form form={form} onFinish={handleFinish} initialValues={detail}>
      {/* 폼 필드 */}
      <Button type="primary" htmlType="submit" loading={isSubmitting}>
        저장
      </Button>
    </Form>
  );
}
```

**규칙**:
- URL param `id`로 등록/수정 모드 판별
- `isPending` 상태로 버튼 loading 표시 (중복 제출 방지)
- `mutateAsync` + await로 완료 대기 후 navigate

---

## Detail 페이지 패턴

```typescript
function NoticeDetailPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { data } = useNoticeDetail(Number(id) || 0);
  const deleteMutation = useDeleteNotice();

  const handleDelete = () => {
    Modal.confirm({
      title: '삭제 확인',
      content: '이 공지사항을 삭제하시겠습니까?',
      okButtonProps: { danger: true },
      onOk: async () => {
        await deleteMutation.mutateAsync(Number(id));
        message.success('삭제되었습니다');
        navigate('/notices');
      },
    });
  };

  return (/* 상세 내용 표시 */);
}
```

**규칙**:
- 삭제는 `Modal.confirm` 2단계 확인
- `mutateAsync` + await로 완료 대기

---

## 모달 패턴

```typescript
function SomePage() {
  const [modalOpen, setModalOpen] = useState(false);
  const createMutation = useCreateSomething();

  const handleOk = async () => {
    await createMutation.mutateAsync(formData);
    setModalOpen(false);
  };

  return (
    <Modal
      open={modalOpen}
      onOk={handleOk}
      onCancel={() => setModalOpen(false)}
      confirmLoading={createMutation.isPending}
    >
      {/* 모달 내용 */}
    </Modal>
  );
}
```

**규칙**:
- `confirmLoading={mutation.isPending}`으로 확인 버튼 중복 클릭 방지
- `mutateAsync` + await 완료 후 모달 닫기

---

## 클릭 중복 실행 방지 (Click Throttle)

새 페이지를 구현하거나 기존 페이지에 onClick 핸들러를 추가할 때, 아래 체크리스트를 반드시 검토한다.

### 판별 기준

| onClick 유형 | 보호 방식 | 비고 |
|-------------|----------|------|
| 폼 제출 (mutation) | Button `loading={isPending}` | mutation 상태와 연동 |
| 모달 확인 (mutation) | Modal `confirmLoading={isPending}` | mutation 상태와 연동 |
| 네비게이션 (navigate) | `useThrottleClick` 훅 | 500ms 쓰로틀 |
| 테이블 행 클릭 → navigate | `useThrottleClick` 훅 | 500ms 쓰로틀 |
| 모달 오픈 (setState) | 추가 보호 불필요 | React 상태 업데이트가 동기적 |

### 적용 방법

```
// navigate를 트리거하는 onClick에 useThrottleClick 적용
const handleNavigate = useThrottleClick((id: number) => {
  navigate(`/notices/${id}`);
});

// 테이블 행 클릭
<Table onRow={(record) => ({ onClick: () => handleNavigate(record.id) })} />

// 버튼 클릭
<Button onClick={() => handleNavigate()}>작성</Button>
```

### 구현 시 체크리스트

- [ ] 새로 추가하는 onClick이 navigate를 트리거하는가? → `useThrottleClick` 사용
- [ ] 새로 추가하는 onClick이 mutation을 트리거하는가? → Button `loading={isPending}` 사용
- [ ] 모달 확인 버튼인가? → Modal `confirmLoading={isPending}` 사용
- [ ] 이미 `isPending`/`loading`으로 보호되는 버튼인가? → 추가 보호 불필요

---

## 라우트 설정 (routes.tsx)

```typescript
// LazyWrapper로 코드 스플리팅
<Route path="/notices" element={<LazyWrapper><NoticeListPage /></LazyWrapper>} />
<Route path="/notices/new" element={<LazyWrapper><NoticeFormPage /></LazyWrapper>} />
<Route path="/notices/:id" element={<LazyWrapper><NoticeDetailPage /></LazyWrapper>} />
<Route path="/notices/:id/edit" element={<LazyWrapper><NoticeFormPage /></LazyWrapper>} />
```

**규칙**:
- 모든 페이지는 `LazyWrapper` (Suspense + Spin)로 감싸기
- `ProtectedRoute`로 인증 필수 라우트 보호
- `PermissionRoute`로 권한별 접근 제어

---

## Breadcrumb (BreadcrumbContext)

```typescript
// 페이지에서 동적 제목 설정
const { setCustomTitle } = useBreadcrumb();

useEffect(() => {
  if (detail) setCustomTitle(detail.title);
}, [detail]);
```

**규칙**: Detail/Form 페이지에서 데이터 로드 후 `setCustomTitle`로 브레드크럼 동적 업데이트
