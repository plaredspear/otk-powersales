import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import EmployeeEditModal from './EmployeeEditModal';
import { updateEmployee } from '@/api/employee';
import type { EmployeeDetail, FemaleEmployeeFormMeta } from '@/api/employee';

vi.mock('@/api/employee', async (importOriginal) => ({
  ...(await importOriginal<typeof import('@/api/employee')>()),
  updateEmployee: vi.fn(),
}));

const mockedUpdate = vi.mocked(updateEmployee);

const employee: EmployeeDetail = {
  id: 12345,
  employeeCode: '100123',
  name: '홍길동',
  gender: '여',
  status: '재직',
  birthDate: null,
  startDate: null,
  endDate: null,
  appointmentDate: null,
  origin: 'MANUAL',
  costCenterCode: 'A001',
  orgName: '영업1팀',
  locationCode: null,
  workArea: null,
  jobCode: null,
  jikjong: null,
  jikwee: null,
  jikchak: null,
  jikgub: null,
  workType: null,
  ordDetailNode: null,
  phone: null,
  homePhone: null,
  workPhone: null,
  officePhone: null,
  workEmail: null,
  email: null,
  role: '여사원',
  appLoginActive: true,
  lockingFlag: false,
  professionalPromotionTeam: '라면세일조',
  agreementFlag: null,
  appVersionName: null,
  appVersionCode: null,
  appPlatform: null,
  appVersionSeenAt: null,
  crmWorkType: null,
  crmWorkStartDate: null,
  totalAnnualLeave: null,
  usedAnnualLeave: null,
};

/**
 * 서버 form-meta stub — 폴백 상수와 값이 겹치지 않게 라벨을 구분해, 화면이 실제로 서버 응답을
 * 쓰는지(상수를 그대로 쓰지 않는지) 판별할 수 있게 한다.
 */
const formMeta: FemaleEmployeeFormMeta = {
  statuses: [{ value: '재직', label: '재직(서버)' }],
  roles: [{ value: 'AccountViewAll', label: '영업부장 (AccountViewAll)' }],
  professionalPromotionTeams: [
    { value: '일반', label: '일반' },
    { value: '라면세일조', label: '라면세일조' },
  ],
};

function renderModal(meta?: FemaleEmployeeFormMeta) {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  });
  const onClose = vi.fn();
  render(
    <QueryClientProvider client={client}>
      <EmployeeEditModal employee={employee} open onClose={onClose} formMeta={meta} />
    </QueryClientProvider>,
  );
  return { onClose };
}

/**
 * antd Select 드롭다운 엘리먼트를 필드명으로 특정한다.
 *
 * 여러 Select 를 차례로 열면 닫힌 것도 DOM 에 남으므로, 첫 번째 드롭다운을 잡는 방식
 * (`document.querySelector('.ant-select-dropdown')`) 은 다른 필드의 옵션을 반환한다.
 * antd 가 옵션 목록에 붙이는 `{name}_list` id 를 기준으로 해당 필드의 드롭다운만 찾는다.
 */
function dropdownOf(name: string): HTMLElement {
  const list = document.getElementById(`${name}_list`);
  const dropdown = list?.closest('.ant-select-dropdown');
  if (!dropdown) throw new Error(`드롭다운을 찾지 못했습니다: ${name}`);
  return dropdown as HTMLElement;
}

/** antd Select 를 열고 드롭다운에 렌더된 옵션 라벨을 순서대로 반환한다. */
async function openSelectOptions(labelText: string, name: string): Promise<string[]> {
  const user = userEvent.setup();
  const item = screen.getByText(labelText).closest('.ant-form-item') as HTMLElement;
  await user.click(within(item).getByRole('combobox'));
  return Array.from(dropdownOf(name).querySelectorAll('.ant-select-item-option-content')).map(
    (el) => el.textContent ?? '',
  );
}

describe('EmployeeEditModal 폼 옵션 출처(form-meta)', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('form-meta 가 있으면 서버 옵션을 사용한다 (재직상태/권한/전문행사조)', async () => {
    renderModal(formMeta);

    expect(await openSelectOptions('재직 상태', 'status')).toEqual(['재직(서버)']);
    expect(await openSelectOptions('권한', 'role')).toEqual(['영업부장 (AccountViewAll)']);
    expect(await openSelectOptions('전문행사조', 'professionalPromotionTeam')).toEqual([
      '일반',
      '라면세일조',
    ]);
  });

  it('form-meta 가 없으면(설정 사원 진입 / 로딩 중) 프론트 상수로 폴백한다', async () => {
    renderModal(undefined);

    expect(await openSelectOptions('재직 상태', 'status')).toEqual(['재직', '휴직', '퇴직']);
    // 폴백 전문행사조도 '일반' 을 포함한 6개 — 서버 옵션과 구성이 같다.
    expect(await openSelectOptions('전문행사조', 'professionalPromotionTeam')).toEqual([
      '일반',
      '라면세일조',
      '프레시세일조_냉장',
      '프레시세일조_냉동',
      '프레시세일조_만두',
      '카레세일조',
    ]);
  });

  it("전문행사조 '일반' 선택은 그대로 전송된다 (서버가 미배정=null 로 해석)", async () => {
    const user = userEvent.setup();
    mockedUpdate.mockResolvedValue(employee);
    renderModal(formMeta);

    const item = screen.getByText('전문행사조').closest('.ant-form-item') as HTMLElement;
    await user.click(within(item).getByRole('combobox'));
    // 클릭 핸들러는 option 컨테이너가 아니라 안쪽 content 에 걸려 있다.
    await user.click(
      within(dropdownOf('professionalPromotionTeam')).getByText('일반', {
        selector: '.ant-select-item-option-content',
      }),
    );
    // 선택이 폼 상태에 반영된 뒤 저장 — 반영 전에 누르면 초기값이 그대로 전송된다.
    await waitFor(() => {
      expect(item.querySelector('.ant-select-selection-item')).toHaveAttribute('title', '일반');
    });

    await user.click(screen.getByRole('button', { name: '저장' }));

    expect(mockedUpdate).toHaveBeenCalledWith(
      12345,
      expect.objectContaining({ professionalPromotionTeam: '일반' }),
    );
  });
});
