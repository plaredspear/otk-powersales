import { useState } from 'react';
import { Alert, Card, Radio, Space, Tag, Tooltip } from 'antd';
import { type SapOutboundTestKind } from '@/api/admin/sapOutboundTest';
import SapOutboundSenderCard from './SapOutboundSenderCard';
import {
  SENDER_CONFIGS,
  TRIGGER_SEND_EFFECT,
  TRIGGER_TAG_COLOR,
} from './sapOutboundSenderConfigs';

/**
 * SAP Outbound 전용 페이지의 '테스트 송신' 탭.
 *
 * 7개 인터페이스를 라디오 버튼으로 선택해 한 화면에서 폼/미리보기/실송신한다.
 * 인터페이스별 폼·송신 로직은 `SapOutboundSenderCard` + `sapOutboundSenderConfigs` 가 단일 출처.
 */
export default function SapOutboundTestTab() {
  const [selectedKind, setSelectedKind] = useState<SapOutboundTestKind>(
    SENDER_CONFIGS[0].kind,
  );
  const config = SENDER_CONFIGS.find((c) => c.kind === selectedKind)!;

  return (
    <div>
      <Alert
        type="warning"
        showIcon
        style={{ marginBottom: 16 }}
        message="이 페이지는 실제 SAP 시스템으로 송신을 트리거합니다."
        description={
          <>
            SYSTEM_ADMIN 권한 필요. '실송신' 버튼은 현재 환경의 SAP REST Adapter 로 호출이
            전송됩니다. 선택된 <Tag color={TRIGGER_TAG_COLOR[config.triggerTag]}>{config.triggerTag}</Tag>{' '}
            인터페이스는 {TRIGGER_SEND_EFFECT[config.triggerTag]} 페이로드 형식 확인만 필요하면
            '미리보기' 만 사용하세요.
          </>
        }
      />

      <Card size="small" style={{ marginBottom: 16 }}>
        <Radio.Group
          value={selectedKind}
          onChange={(e) => setSelectedKind(e.target.value as SapOutboundTestKind)}
          buttonStyle="solid"
        >
          <Space wrap>
            {SENDER_CONFIGS.map((c) => (
              <Tooltip key={c.kind} title={c.description}>
                <Radio.Button value={c.kind}>
                  <Tag color={TRIGGER_TAG_COLOR[c.triggerTag]} style={{ marginRight: 6 }}>
                    {c.triggerTag}
                  </Tag>
                  {c.title.split(' (')[0]}
                </Radio.Button>
              </Tooltip>
            ))}
          </Space>
        </Radio.Group>
      </Card>

      {/* key 로 인터페이스 전환 시 카드 state 를 초기화 */}
      <SapOutboundSenderCard key={selectedKind} config={config} />
    </div>
  );
}
