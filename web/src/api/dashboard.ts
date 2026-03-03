import client from './client';

// --- Frontend interfaces (camelCase) ---

export interface DashboardData {
  salesSummary: SalesSummary;
  staffDeployment: StaffDeployment;
  basicStats: BasicStats;
}

export interface SalesSummary {
  yearMonth: string;
  branchName: string | null;
  targetAmount: number;
  actualAmount: number;
  progressRate: number;
  referenceProgressRate: number;
  lastYearAmount: number;
  lastYearRatio: number;
  channelSales: ChannelSalesItem[];
}

export interface ChannelSalesItem {
  channelName: string;
  targetAmount: number;
  actualAmount: number;
  progressRate: number;
}

export interface StaffDeployment {
  yearMonth: string;
  branchName: string | null;
  byAccountType: AccountTypeCount[];
  byWorkType: WorkTypeCount[];
  byChannelAndWorkType: ChannelWorkTypeItem[];
  previousMonth: {
    byWorkType: WorkTypeCount[];
  };
}

export interface AccountTypeCount {
  accountType: string;
  count: number;
}

export interface WorkTypeCount {
  workType: string;
  count: number;
}

export interface ChannelWorkTypeItem {
  channelName: string;
  fixed: number;
  alternating: number;
  visiting: number;
}

export interface BasicStats {
  branchName: string | null;
  staffType: { promotion: number; osc: number };
  totalByPosition: { active: number; onLeave: number };
  byAgeGroup: AgeGroupCount[];
  byWorkType: { fixed: number; alternating: number; visiting: number };
}

export interface AgeGroupCount {
  ageGroup: string;
  count: number;
}

// --- API response interfaces (snake_case) ---

interface ApiResponse<T> {
  success: boolean;
  data: T | null;
  message?: string;
}

interface DashboardRaw {
  sales_summary: {
    year_month: string;
    branch_name: string | null;
    target_amount: number;
    actual_amount: number;
    progress_rate: number;
    reference_progress_rate: number;
    last_year_amount: number;
    last_year_ratio: number;
    channel_sales: Array<{
      channel_name: string;
      target_amount: number;
      actual_amount: number;
      progress_rate: number;
    }>;
  };
  staff_deployment: {
    year_month: string;
    branch_name: string | null;
    by_account_type: Array<{ account_type: string; count: number }>;
    by_work_type: Array<{ work_type: string; count: number }>;
    by_channel_and_work_type: Array<{
      channel_name: string;
      fixed: number;
      alternating: number;
      visiting: number;
    }>;
    previous_month: {
      by_work_type: Array<{ work_type: string; count: number }>;
    };
  };
  basic_stats: {
    branch_name: string | null;
    staff_type: { promotion: number; osc: number };
    total_by_position: { active: number; on_leave: number };
    by_age_group: Array<{ age_group: string; count: number }>;
    by_work_type: { fixed: number; alternating: number; visiting: number };
  };
}

function mapDashboardData(raw: DashboardRaw): DashboardData {
  return {
    salesSummary: {
      yearMonth: raw.sales_summary.year_month,
      branchName: raw.sales_summary.branch_name,
      targetAmount: raw.sales_summary.target_amount,
      actualAmount: raw.sales_summary.actual_amount,
      progressRate: raw.sales_summary.progress_rate,
      referenceProgressRate: raw.sales_summary.reference_progress_rate,
      lastYearAmount: raw.sales_summary.last_year_amount,
      lastYearRatio: raw.sales_summary.last_year_ratio,
      channelSales: raw.sales_summary.channel_sales.map((c) => ({
        channelName: c.channel_name,
        targetAmount: c.target_amount,
        actualAmount: c.actual_amount,
        progressRate: c.progress_rate,
      })),
    },
    staffDeployment: {
      yearMonth: raw.staff_deployment.year_month,
      branchName: raw.staff_deployment.branch_name,
      byAccountType: raw.staff_deployment.by_account_type.map((a) => ({
        accountType: a.account_type,
        count: a.count,
      })),
      byWorkType: raw.staff_deployment.by_work_type.map((w) => ({
        workType: w.work_type,
        count: w.count,
      })),
      byChannelAndWorkType: raw.staff_deployment.by_channel_and_work_type.map((c) => ({
        channelName: c.channel_name,
        fixed: c.fixed,
        alternating: c.alternating,
        visiting: c.visiting,
      })),
      previousMonth: {
        byWorkType: raw.staff_deployment.previous_month.by_work_type.map((w) => ({
          workType: w.work_type,
          count: w.count,
        })),
      },
    },
    basicStats: {
      branchName: raw.basic_stats.branch_name,
      staffType: raw.basic_stats.staff_type,
      totalByPosition: {
        active: raw.basic_stats.total_by_position.active,
        onLeave: raw.basic_stats.total_by_position.on_leave,
      },
      byAgeGroup: raw.basic_stats.by_age_group.map((a) => ({
        ageGroup: a.age_group,
        count: a.count,
      })),
      byWorkType: raw.basic_stats.by_work_type,
    },
  };
}

export interface FetchDashboardParams {
  yearMonth?: string;
  branchCode?: string;
}

export async function fetchDashboard(params?: FetchDashboardParams): Promise<DashboardData> {
  const res = await client.get<ApiResponse<DashboardRaw>>('/api/v1/admin/dashboard', {
    params: {
      yearMonth: params?.yearMonth,
      branchCode: params?.branchCode,
    },
  });
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '대시보드 조회에 실패했습니다');
  }
  return mapDashboardData(res.data.data);
}
