import { Alert, Collapse, Table, Tag, Typography } from 'antd';
import { InfoCircleOutlined } from '@ant-design/icons';

const { Paragraph, Text } = Typography;

/**
 * 지점 코드 맵핑 화면 상단 안내 — "확장이 언제 적용되고, 무엇이 문제될 수 있는지".
 *
 * 표 자체는 "어떤 코드가 어떻게 확장되는가" 만 보여주므로, 운영자가 실제로 궁금해하는
 * "그래서 내 화면 숫자에 뭐가 영향을 주나" 는 알 수 없다. 아래 4개 패널이 그 간극을 메운다.
 * 접힌 상태가 기본이라 표를 보러 온 사용자의 동선은 방해하지 않는다.
 */

/** 적용 시점 2단 구조 — 셀렉터 목록에는 확장이 적용되지 않는다. */
const STAGE_ROWS = [
  {
    key: 'selector',
    stage: '① 지점 셀렉터 목록',
    when: '화면 진입 시',
    expand: false,
    detail: '조직 트리(권한 범위)만으로 만든다. 확장 코드는 드롭다운에 나타나지 않는다.',
  },
  {
    key: 'guard',
    stage: '② 선택 지점 권한 판정',
    when: '조회 요청 시',
    expand: false,
    detail: '사용자가 보낸 지점 코드가 ①의 목록에 있는지 확장 전 원본 코드로 확인한다.',
  },
  {
    key: 'query',
    stage: '③ 실제 데이터 조회',
    when: '판정 통과 후',
    expand: true,
    detail: '이때만 확장이 적용된다. 아래 표의 확장 결과가 그대로 조회 조건이 된다.',
  },
];

/** 유형별 실무 영향 — 표의 유형 태그가 각각 무엇을 의미하는지. */
const TYPE_ROWS = [
  {
    key: 'LEGACY',
    tag: <Tag color="gold">이력</Tag>,
    meaning: '자기 자신 + 조직 개편으로 폐기된 옛 코드',
    impact: '의도된 동작. 발령을 받지 못해 옛 코드가 남은 소속 사원을 누락 없이 조회한다.',
    check: '확인 불필요',
  },
  {
    key: 'ROLLUP',
    tag: <Tag color="volcano">롤업</Tag>,
    meaning: '현행 다른 조직을 함께 끌어온다 (부서 → 하위 지점 등)',
    impact:
      '지점 1개를 골라도 여러 조직 데이터가 합산된다. 화면에는 합산 사실이 표시되지 않는다.',
    check: '사용처별 확인 필요',
  },
  {
    key: 'DUAL_CODE',
    tag: <Tag color="purple">이중코드</Tag>,
    meaning: 'E{N} 과 {N} 이 같은 조직의 별칭이며 양쪽 다 현역',
    impact: '같은 조직을 두 코드로 모두 조회한다. 범위가 넓어지지 않는다.',
    check: '확인 불필요',
  },
  {
    key: 'NONE',
    tag: <Tag>없음</Tag>,
    meaning: '확장 결과가 자기 자신뿐',
    impact: '확장 효과 없음. 선택한 지점만 조회된다.',
    check: '확인 불필요',
  },
];

const STAGE_COLUMNS = [
  { title: '단계', dataIndex: 'stage', width: 170 },
  { title: '시점', dataIndex: 'when', width: 110 },
  {
    title: '확장 적용',
    dataIndex: 'expand',
    width: 90,
    align: 'center' as const,
    render: (applied: boolean) =>
      applied ? <Tag color="volcano">적용</Tag> : <Tag>미적용</Tag>,
  },
  { title: '설명', dataIndex: 'detail' },
];

const TYPE_COLUMNS = [
  { title: '유형', dataIndex: 'tag', width: 90, align: 'center' as const },
  { title: '의미', dataIndex: 'meaning', width: 260 },
  { title: '조회 시 영향', dataIndex: 'impact' },
  {
    title: '검토',
    dataIndex: 'check',
    width: 130,
    render: (val: string) =>
      val === '확인 불필요' ? (
        <Text type="secondary">{val}</Text>
      ) : (
        <Text type="warning" strong>
          {val}
        </Text>
      ),
  },
];

export default function BranchMappingGuide() {
  return (
    <Collapse
      size="small"
      style={{ marginBottom: 12 }}
      items={[
        {
          key: 'guide',
          label: (
            <span>
              <InfoCircleOutlined style={{ marginRight: 6 }} />
              지점 코드 확장이 조회 결과에 미치는 영향 — 읽어두면 좋은 사항
            </span>
          ),
          children: (
            <div>
              <Paragraph>
                <Text strong>1. 확장은 조회 단계에서만 적용됩니다.</Text> 지점 드롭다운에 보이는
                목록과 권한 판정은 확장 <Text underline>전</Text> 코드를 씁니다. 확장 코드가
                선택지를 늘리거나 권한을 넓히지 않습니다.
              </Paragraph>
              <Table
                size="small"
                pagination={false}
                rowKey="key"
                columns={STAGE_COLUMNS}
                dataSource={STAGE_ROWS}
                style={{ marginBottom: 20 }}
              />

              <Paragraph>
                <Text strong>2. 유형에 따라 검토 필요 여부가 다릅니다.</Text> 대부분은 옛 코드를
                함께 조회하는 의도된 동작이며, 확인이 필요한 것은 <Tag color="volcano">롤업</Tag>
                뿐입니다.
              </Paragraph>
              <Table
                size="small"
                pagination={false}
                rowKey="key"
                columns={TYPE_COLUMNS}
                dataSource={TYPE_ROWS}
                style={{ marginBottom: 20 }}
              />

              <Alert
                type="warning"
                showIcon
                style={{ marginBottom: 16 }}
                message="롤업 유형은 합산 범위가 화면에 표시되지 않습니다"
                description={
                  <>
                    <div style={{ marginBottom: 8 }}>
                      부서·팀 단위 코드를 선택하면 하위 지점 데이터가 함께 조회되지만, 조회 화면에는
                      &quot;몇 개 조직이 합산되었는지&quot;가 나타나지 않습니다. 수치가 예상보다
                      크다면 이 화면에서 해당 지점코드의 확장 결과를 먼저 확인하세요.
                    </div>
                    <div>
                      롤업은 대부분 부서·팀·직책 단위 코드라 지점 단위 사용자가 마주칠 일은 드뭅니다.
                      다만 라벨이 지점처럼 보이는 코드
                      <Text code>대구경북급식지점</Text>
                      같은 경우는 라벨과 실제 조회 범위가 어긋나 보일 수 있습니다.
                    </div>
                  </>
                }
              />

              <Alert
                type="info"
                showIcon
                message="화면마다 지점 기준이 다를 수 있습니다"
                description={
                  <>
                    본 확장은 <Text code>조직코드</Text> 축으로 동작합니다. 일부 화면(대시보드
                    기본현황 등)은 <Text code>조직명</Text> 축으로 집계하여 확장을 타지 않으므로,
                    같은 지점을 선택해도 화면 간 수치가 다를 수 있습니다. 2025년 5월 조직 개편 이후
                    발령을 받지 못해 옛 코드가 남은 사원에서 주로 발생합니다.
                  </>
                }
              />
            </div>
          ),
        },
      ]}
    />
  );
}
