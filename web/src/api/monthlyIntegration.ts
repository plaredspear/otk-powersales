import client from './client';

// --- Raw API response interfaces (snake_case from backend) ---

interface ApiResponse<T> {
  success: boolean;
  data: T | null;
  message?: string;
  error?: { code: string; message: string };
}

interface MonthlyIntegrationScheduleItemRaw {
  branch_name: string;
  account_branch_name: string | null;
  account_code: string;
  account_name: string;
  employee_number: string;
  title: string | null;
  employee_name: string;
  working_category1: string;
  working_category3: string | null;
  working_category4: string | null;
  working_category5: string | null;
  total_input_count: number;
  equivalent_working_days: number;
  converted_headcount: number;
  avg_closing_amount: number;
}

interface MonthlyIntegrationScheduleResponseRaw {
  year: number;
  month: number;
  items: MonthlyIntegrationScheduleItemRaw[];
  total_count: number;
}

interface CategoryScheduleItemRaw {
  branch_name: string;
  current_month_total: number;
  previous_month_total: number;
  total_change: number;
  display_fixed: number;
  display_alternate: number;
  display_patrol: number;
  current_month_display_total: number;
  previous_month_display_total: number;
  display_change: number;
  event_ambient: number;
  event_frozen_chilled: number;
  current_month_event_total: number;
  previous_month_event_total: number;
  event_change: number;
}

interface CategoryScheduleResponseRaw {
  year: number;
  month: number;
  items: CategoryScheduleItemRaw[];
}

// --- Frontend interfaces (camelCase) ---

export interface MonthlyIntegrationScheduleItem {
  branchName: string;
  accountBranchName: string | null;
  accountCode: string;
  accountName: string;
  employeeNumber: string;
  title: string | null;
  employeeName: string;
  workingCategory1: string;
  workingCategory3: string | null;
  workingCategory4: string | null;
  workingCategory5: string | null;
  totalInputCount: number;
  equivalentWorkingDays: number;
  convertedHeadcount: number;
  avgClosingAmount: number;
}

export interface MonthlyIntegrationScheduleResponse {
  year: number;
  month: number;
  items: MonthlyIntegrationScheduleItem[];
  totalCount: number;
}

export interface CategoryScheduleItem {
  branchName: string;
  currentMonthTotal: number;
  previousMonthTotal: number;
  totalChange: number;
  displayFixed: number;
  displayAlternate: number;
  displayPatrol: number;
  currentMonthDisplayTotal: number;
  previousMonthDisplayTotal: number;
  displayChange: number;
  eventAmbient: number;
  eventFrozenChilled: number;
  currentMonthEventTotal: number;
  previousMonthEventTotal: number;
  eventChange: number;
}

export interface CategoryScheduleResponse {
  year: number;
  month: number;
  items: CategoryScheduleItem[];
}

// --- Mappers ---

function mapIntegrationItem(raw: MonthlyIntegrationScheduleItemRaw): MonthlyIntegrationScheduleItem {
  return {
    branchName: raw.branch_name,
    accountBranchName: raw.account_branch_name,
    accountCode: raw.account_code,
    accountName: raw.account_name,
    employeeNumber: raw.employee_number,
    title: raw.title,
    employeeName: raw.employee_name,
    workingCategory1: raw.working_category1,
    workingCategory3: raw.working_category3,
    workingCategory4: raw.working_category4,
    workingCategory5: raw.working_category5,
    totalInputCount: raw.total_input_count,
    equivalentWorkingDays: raw.equivalent_working_days,
    convertedHeadcount: raw.converted_headcount,
    avgClosingAmount: raw.avg_closing_amount,
  };
}

function mapCategoryItem(raw: CategoryScheduleItemRaw): CategoryScheduleItem {
  return {
    branchName: raw.branch_name,
    currentMonthTotal: raw.current_month_total,
    previousMonthTotal: raw.previous_month_total,
    totalChange: raw.total_change,
    displayFixed: raw.display_fixed,
    displayAlternate: raw.display_alternate,
    displayPatrol: raw.display_patrol,
    currentMonthDisplayTotal: raw.current_month_display_total,
    previousMonthDisplayTotal: raw.previous_month_display_total,
    displayChange: raw.display_change,
    eventAmbient: raw.event_ambient,
    eventFrozenChilled: raw.event_frozen_chilled,
    currentMonthEventTotal: raw.current_month_event_total,
    previousMonthEventTotal: raw.previous_month_event_total,
    eventChange: raw.event_change,
  };
}

// --- API functions ---

export async function fetchMonthlyIntegrationSchedule(
  year: number,
  month: number,
  costCenterCodes: string[],
): Promise<MonthlyIntegrationScheduleResponse> {
  const res = await client.get<ApiResponse<MonthlyIntegrationScheduleResponseRaw>>(
    '/api/v1/admin/schedules/monthly-integration',
    { params: { year, month, costCenterCodes: costCenterCodes.join(',') } },
  );
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.error?.message || res.data.message || '통합일정 조회에 실패했습니다');
  }
  const raw = res.data.data;
  return {
    year: raw.year,
    month: raw.month,
    items: raw.items.map(mapIntegrationItem),
    totalCount: raw.total_count,
  };
}

export async function fetchMonthlyIntegrationExport(
  year: number,
  month: number,
  costCenterCodes: string[],
): Promise<void> {
  const res = await client.get('/api/v1/admin/schedules/monthly-integration/export', {
    params: { year, month, costCenterCodes: costCenterCodes.join(',') },
    responseType: 'blob',
  });

  const contentDisposition = res.headers['content-disposition'] as string | undefined;
  let filename = `${year}년${month}월_여사원_통합일정.xlsx`;
  if (contentDisposition) {
    const match = contentDisposition.match(/filename="?([^";\n]+)"?/);
    if (match) filename = decodeURIComponent(match[1]);
  }

  const blob = new Blob([res.data], {
    type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
  });
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = filename;
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
  URL.revokeObjectURL(url);
}

export async function fetchCategorySchedule(
  year: number,
  month: number,
  costCenterCodes: string[],
): Promise<CategoryScheduleResponse> {
  const res = await client.get<ApiResponse<CategoryScheduleResponseRaw>>(
    '/api/v1/admin/schedules/monthly-integration/category',
    { params: { year, month, costCenterCodes: costCenterCodes.join(',') } },
  );
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.error?.message || res.data.message || '근무형태별 인원현황 조회에 실패했습니다');
  }
  const raw = res.data.data;
  return {
    year: raw.year,
    month: raw.month,
    items: raw.items.map(mapCategoryItem),
  };
}

export async function fetchCategoryExport(
  year: number,
  month: number,
  costCenterCodes: string[],
): Promise<void> {
  const res = await client.get('/api/v1/admin/schedules/monthly-integration/category/export', {
    params: { year, month, costCenterCodes: costCenterCodes.join(',') },
    responseType: 'blob',
  });

  const contentDisposition = res.headers['content-disposition'] as string | undefined;
  let filename = `${year}년${month}월_근무형태별_인원현황.xlsx`;
  if (contentDisposition) {
    const match = contentDisposition.match(/filename="?([^";\n]+)"?/);
    if (match) filename = decodeURIComponent(match[1]);
  }

  const blob = new Blob([res.data], {
    type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
  });
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = filename;
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
  URL.revokeObjectURL(url);
}
