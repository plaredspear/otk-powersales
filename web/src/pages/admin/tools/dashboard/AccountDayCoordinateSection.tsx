import {
  Alert,
  Button,
  Card,
  Form,
  Input,
  InputNumber,
  Popconfirm,
  Select,
  Space,
  Tag,
  Tooltip,
  Typography,
  message,
} from 'antd';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { apiErrorMessage } from '@/lib/apiErrorMessage';
import {
  DAY_OF_WEEK_LABELS,
  fetchAccountDayCoordinate,
  resetAccountDayCoordinate,
  updateAccountDayCoordinate,
  type AccountDayCoordinateResponse,
  type DayOfWeekCode,
  type UpdateAccountDayCoordinateParams,
} from '@/api/admin/accountDayCoordinate';

const { Paragraph, Text } = Typography;

const QUERY_KEY = ['admin', 'tools', 'account-day-coordinate'] as const;

const DAY_OPTIONS = (Object.keys(DAY_OF_WEEK_LABELS) as DayOfWeekCode[]).map((code) => ({
  value: code,
  label: DAY_OF_WEEK_LABELS[code],
}));

/**
 * 개발자 도구 > 대시보드 > 이동매장 좌표 예외.
 *
 * 요일에 따라 영업 위치가 바뀌는 거래처는 거래처에 등록된 좌표 1쌍만으로는 출근등록 GPS 거리 검증이
 * 성립하지 않는다. 해당 요일에 한해 검증 기준 좌표를 대체하며, 여기서 요일과 좌표를 조정한다.
 *
 * 설정은 Redis 에 지속 저장되어 재시작 후에도 유지되고 전체 사용자에게 즉시 적용된다.
 * 초기화하면 백엔드 코드 기본값이 적용된다. 시스템 관리자 전용 (백엔드 가드).
 */
export default function AccountDayCoordinateSection() {
  const queryClient = useQueryClient();

  const { data, isLoading, isError } = useQuery<AccountDayCoordinateResponse>({
    queryKey: QUERY_KEY,
    queryFn: fetchAccountDayCoordinate,
  });

  const updateMutation = useMutation({
    mutationFn: updateAccountDayCoordinate,
    onSuccess: (result: AccountDayCoordinateResponse) => {
      message.success(`${DAY_OF_WEEK_LABELS[result.dayOfWeek]} 기준 좌표로 저장되었습니다`);
      queryClient.invalidateQueries({ queryKey: QUERY_KEY });
    },
    onError: (err: unknown) => {
      message.error(apiErrorMessage(err, '이동매장 좌표 예외 변경에 실패했습니다'));
    },
  });

  const resetMutation = useMutation({
    mutationFn: resetAccountDayCoordinate,
    onSuccess: () => {
      message.success('기본값으로 초기화되었습니다');
      queryClient.invalidateQueries({ queryKey: QUERY_KEY });
    },
    onError: (err: unknown) => {
      message.error(apiErrorMessage(err, '이동매장 좌표 예외 초기화에 실패했습니다'));
    },
  });

  const pending = updateMutation.isPending || resetMutation.isPending;

  return (
    <>
      <Paragraph type="secondary">
        요일에 따라 영업 위치가 바뀌는 거래처의 <Text strong>출근등록 GPS 검증 기준 좌표</Text>를
        설정합니다. 지정한 요일에는 거래처에 등록된 좌표 대신 아래 좌표를 기준으로 거리를 판정하며,
        다른 요일에는 거래처 원본 좌표를 그대로 사용합니다.
      </Paragraph>

      <Alert
        type="warning"
        style={{ marginBottom: 16 }}
        message="변경은 전체 사용자에게 즉시 적용됩니다. 좌표가 실제 영업 위치와 어긋나면 해당 요일 출근등록이 거리 초과로 실패하므로, 위/경도를 지도에서 확인한 값으로 입력하세요."
      />

      {isError && (
        <Alert
          type="error"
          style={{ marginBottom: 16 }}
          message="이동매장 좌표 예외 조회에 실패했습니다. 시스템 관리자 권한이 필요합니다."
        />
      )}

      <Card size="small" loading={isLoading}>
        {/* 조회 실패/미완료 상태에서 빈 폼을 노출하면 어떤 거래처에 저장되는지 모른 채 조작하게 된다. */}
        {!isError && data && (
          <Space direction="vertical" size="middle" style={{ width: '100%' }}>
            <Space size="middle" align="center" wrap>
              <Text strong>대상 거래처</Text>
              <Tag>거래처코드 {data.externalKey}</Tag>
              <Tag color={data.customized ? 'blue' : 'default'}>
                {data.customized ? '설정값 적용 중' : '기본값 적용 중'}
              </Tag>
            </Space>

            <Form<UpdateAccountDayCoordinateParams>
              // 서버 값이 바뀌면 폼을 새로 마운트해 initialValues 를 다시 태운다.
              // useEffect + setFieldsValue 로 동기화하면 배경 refetch 가 입력 중인 값을 덮어쓴다.
              key={`${data.dayOfWeek}-${data.latitude}-${data.longitude}-${data.label}`}
              layout="vertical"
              initialValues={{
                dayOfWeek: data.dayOfWeek,
                latitude: data.latitude,
                longitude: data.longitude,
                label: data.label,
              }}
              onFinish={(values) =>
                updateMutation.mutate({
                  dayOfWeek: values.dayOfWeek,
                  latitude: values.latitude,
                  longitude: values.longitude,
                  label: values.label.trim(),
                })
              }
            >
              <Form.Item
                name="dayOfWeek"
                label="적용 요일"
                rules={[{ required: true, message: '요일을 선택하세요' }]}
              >
                <Select options={DAY_OPTIONS} style={{ maxWidth: 200 }} />
              </Form.Item>

              <Form.Item
                name="latitude"
                label="위도"
                rules={[
                  { required: true, message: '위도를 입력하세요' },
                  { type: 'number', min: -90, max: 90, message: '위도는 -90 ~ 90 범위여야 합니다' },
                ]}
              >
                <InputNumber
                  style={{ width: 240 }}
                  min={-90}
                  max={90}
                  step={0.0000001}
                  precision={7}
                  placeholder="38.1018113"
                />
              </Form.Item>

              <Form.Item
                name="longitude"
                label="경도"
                rules={[
                  { required: true, message: '경도를 입력하세요' },
                  {
                    type: 'number',
                    min: -180,
                    max: 180,
                    message: '경도는 -180 ~ 180 범위여야 합니다',
                  },
                ]}
              >
                <InputNumber
                  style={{ width: 240 }}
                  min={-180}
                  max={180}
                  step={0.0000001}
                  precision={7}
                  placeholder="127.9886619"
                />
              </Form.Item>

              <Form.Item
                name="label"
                label="장소 라벨"
                tooltip="어느 지점 기준인지 서버 로그에 남기는 식별 문구입니다. 사고 시 추적 단서라 필수입니다."
                rules={[
                  { required: true, whitespace: true, message: '장소 라벨을 입력하세요' },
                  { max: 50, message: '라벨은 50자 이하여야 합니다' },
                  {
                    pattern: /^[^\r\n|]*$/,
                    message: '라벨에 줄바꿈이나 | 문자는 사용할 수 없습니다',
                  },
                ]}
              >
                <Input style={{ maxWidth: 320 }} placeholder="제이마트 양구점" />
              </Form.Item>

              <Space>
                <Button
                  type="primary"
                  htmlType="submit"
                  loading={updateMutation.isPending}
                  disabled={resetMutation.isPending}
                >
                  저장
                </Button>
                {data.customized ? (
                  <Popconfirm
                    title="기본값으로 초기화"
                    description={`${DAY_OF_WEEK_LABELS[data.defaultDayOfWeek]} / ${data.defaultLatitude}, ${data.defaultLongitude} 로 되돌립니다.`}
                    okText="초기화"
                    cancelText="취소"
                    // Promise 를 반환해야 확인 버튼이 완료까지 로딩 상태를 유지한다.
                    // 실패 토스트는 onError 가 이미 띄우므로 rejection 은 여기서 흡수한다.
                    onConfirm={() => resetMutation.mutateAsync().then(() => undefined, () => undefined)}
                  >
                    <Button loading={resetMutation.isPending} disabled={pending}>
                      기본값으로 초기화
                    </Button>
                  </Popconfirm>
                ) : (
                  <Tooltip title="현재 기본값이 적용 중이라 초기화할 설정이 없습니다">
                    <span>
                      <Button disabled>기본값으로 초기화</Button>
                    </span>
                  </Tooltip>
                )}
              </Space>
            </Form>

            <Text type="secondary">
              기본값: {DAY_OF_WEEK_LABELS[data.defaultDayOfWeek]} / {data.defaultLatitude},{' '}
              {data.defaultLongitude}
              {data.defaultLabel ? ` (${data.defaultLabel})` : ''}
            </Text>
          </Space>
        )}
      </Card>
    </>
  );
}
