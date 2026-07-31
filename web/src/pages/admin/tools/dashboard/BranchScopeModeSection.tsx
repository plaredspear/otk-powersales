import { Alert, Card, Descriptions, Space, Switch, Tag, Typography, message } from 'antd';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  fetchBranchScopeMode,
  updateBranchScopeMode,
  type BranchScopeMode,
  type BranchScopeModeResponse,
} from '@/api/admin/branchScopeMode';

const { Paragraph, Text } = Typography;

const QUERY_KEY = ['admin', 'tools', 'branch-scope-mode'] as const;

/**
 * 개발자 도구 > 대시보드 > 지점 스코프 방식.
 *
 * 투입현황 대시보드의 지점 판정/확장 방식을 통합 리졸버(UNIFIED)와 전환 이전 동작(LEGACY) 사이에서
 * 전환해 같은 조건의 수치를 비교하기 위한 **한시적** 스위치다. 검증이 끝나면 UNIFIED 로 고정하고
 * 본 화면과 LEGACY 경로를 함께 제거한다.
 *
 * 상태는 Redis 에 지속 저장되어 재시작 후에도 유지되며, 전환은 **전체 사용자에게 즉시 적용**된다
 * (사용자별 설정이 아님). 시스템 관리자 전용 (백엔드 가드).
 */
export default function BranchScopeModeSection() {
  const queryClient = useQueryClient();

  const { data, isLoading, isError } = useQuery<BranchScopeModeResponse>({
    queryKey: QUERY_KEY,
    queryFn: fetchBranchScopeMode,
  });

  const updateMutation = useMutation({
    mutationFn: updateBranchScopeMode,
    onSuccess: (result: BranchScopeModeResponse) => {
      message.success(
        result.mode === 'UNIFIED'
          ? '통합 리졸버(신규 방식)로 전환되었습니다'
          : '전환 이전 방식(LEGACY)으로 되돌렸습니다',
      );
      queryClient.invalidateQueries({ queryKey: QUERY_KEY });
      // 대시보드 조회 결과도 방식에 따라 달라지므로 캐시를 비운다.
      queryClient.invalidateQueries({ queryKey: ['admin', 'dashboard'] });
    },
    onError: (err: Error) => {
      message.error(err.message || '지점 스코프 방식 변경에 실패했습니다');
    },
  });

  const mode: BranchScopeMode | undefined = data?.mode;

  return (
    <>
      <Paragraph type="secondary">
        투입현황 대시보드가 지점 조회 범위를 정하는 방식을 전환합니다. 두 방식의 <Text strong>지점 셀렉터
        목록은 동일</Text>하며, 선택한 지점의 <Text strong>판정과 코드 확장</Text>만 달라집니다.
        같은 계정·같은 조건으로 켜고/끄며 수치를 비교하는 용도입니다.
      </Paragraph>

      <Alert
        type="warning"
        style={{ marginBottom: 16 }}
        message="전환은 전체 사용자에게 즉시 적용됩니다 (사용자별 설정 아님). 비교 검증용 한시 스위치이며, 검증 후 통합 리졸버로 고정하고 이 화면은 제거됩니다."
      />

      {isError && (
        <Alert
          type="error"
          style={{ marginBottom: 16 }}
          message="지점 스코프 방식 조회에 실패했습니다. 시스템 관리자 권한이 필요합니다."
        />
      )}

      <Card size="small" loading={isLoading}>
        <Space direction="vertical" size="middle" style={{ width: '100%' }}>
          <Space size="middle" align="center">
            <Switch
              checked={mode === 'UNIFIED'}
              loading={updateMutation.isPending}
              disabled={isLoading || isError}
              onChange={(checked) => updateMutation.mutate(checked ? 'UNIFIED' : 'LEGACY')}
            />
            <Text strong>통합 리졸버 사용</Text>
            {mode && (
              <Tag color={mode === 'UNIFIED' ? 'green' : 'orange'}>
                현재: {mode === 'UNIFIED' ? '통합 리졸버 (UNIFIED)' : '전환 이전 방식 (LEGACY)'}
              </Tag>
            )}
          </Space>

          <Descriptions bordered size="small" column={1}>
            <Descriptions.Item label="ON — 통합 리졸버 (UNIFIED)">
              셀렉터 목록이 곧 판정 화이트리스트. 셀렉터에 보이는 지점은 항상 조회되며, 판정을 통과한
              코드만 BranchMapping 으로 확장해 조회합니다. 지점 미선택 시 셀렉터 목록 전체로 조회합니다.
            </Descriptions.Item>
            <Descriptions.Item label="OFF — 전환 이전 방식 (LEGACY)">
              판정 기준이 본인 소속 지점 코드(DataScope)입니다. 상위 조직(영업부·팀) 계정은 셀렉터에
              하위 지점이 보여도 선택하면 0건이 되고, 미선택 시 본인 코드 + 롤업 매핑 범위만 조회됩니다.
            </Descriptions.Item>
            <Descriptions.Item label="차이가 나타나는 대상">
              상위 조직 코드 소속 계정. 전사 권한자(34개 고정)와 단일 지점 계정은 두 방식의 결과가
              동일합니다.
            </Descriptions.Item>
          </Descriptions>
        </Space>
      </Card>
    </>
  );
}
