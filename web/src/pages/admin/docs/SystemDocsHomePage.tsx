import { Card, Col, Divider, Row, Tag, Typography } from 'antd';
import {
  ApartmentOutlined,
  ApiOutlined,
  ClusterOutlined,
  DatabaseOutlined,
  DeploymentUnitOutlined,
  FileTextOutlined,
  HistoryOutlined,
  SafetyCertificateOutlined,
  TableOutlined,
} from '@ant-design/icons';
import { Link } from 'react-router-dom';

const { Title, Paragraph, Text } = Typography;

/**
 * 시스템 안내 홈 — 시스템 관리자 / 개발자를 위한 안내 허브.
 * 신규 안내 페이지와 기존에 흩어져 있던 관리/가이드 페이지로 가는 진입점을 카드로 모은다.
 * 시스템 관리자(profile)만 접근 (라우터 가드).
 */

interface DocCard {
  key: string;
  to: string;
  title: string;
  desc: string;
  icon: React.ReactNode;
  status?: 'ready' | 'planned';
}

interface DocSection {
  title: string;
  cards: DocCard[];
}

const SECTIONS: DocSection[] = [
  {
    title: '시스템 안내',
    cards: [
      {
        key: 'overview',
        to: '/admin/docs/overview',
        title: '시스템 개요',
        desc: '아키텍처 · 외부 연동 · 기술 스택 · 인증/권한 · 배치를 한눈에. 처음 보는 분이 먼저 읽을 문서.',
        icon: <DeploymentUnitOutlined />,
        status: 'ready',
      },
      {
        key: 'domains',
        to: '/admin/docs/domains',
        title: '도메인 / 모듈 맵',
        desc: '어떤 기능이 어느 코드(도메인 패키지)에 있는지, 담당 화면과 함께 매핑.',
        icon: <ApartmentOutlined />,
        status: 'ready',
      },
      {
        key: 'flows',
        to: '/admin/docs/overview',
        title: '데이터 흐름 (DFD)',
        desc: 'SAP/Orora 유입부터 화면 노출까지 주요 데이터 흐름 다이어그램. (준비 중)',
        icon: <ClusterOutlined />,
        status: 'planned',
      },
      {
        key: 'api',
        to: '/admin/docs/overview',
        title: 'API 카탈로그',
        desc: 'REST 엔드포인트 목록과 설명. (준비 중)',
        icon: <ApiOutlined />,
        status: 'planned',
      },
    ],
  },
  {
    title: '권한 / 사용자',
    cards: [
      {
        key: 'perm-guide',
        to: '/admin/permissions/guide',
        title: '권한 사용 가이드',
        desc: 'Profile / PermissionSet / 시스템 권한이 어떻게 합산되는지, 시뮬레이터 포함.',
        icon: <SafetyCertificateOutlined />,
      },
      {
        key: 'perm-matrix',
        to: '/admin/permissions/matrix',
        title: '권한 매트릭스',
        desc: 'Profile/PermissionSet 별 엔티티 CRUD 권한 전체 매트릭스.',
        icon: <TableOutlined />,
      },
      {
        key: 'page-access',
        to: '/admin/permissions/page-access-guide',
        title: '페이지별 필요 권한',
        desc: '각 화면에 접근하려면 어떤 권한이 필요한지 일람.',
        icon: <FileTextOutlined />,
      },
    ],
  },
  {
    title: '운영 / 연동',
    cards: [
      {
        key: 'jobs',
        to: '/admin/tools/scheduled-jobs',
        title: '스케줄 잡 실행 이력',
        desc: '배치 작업의 최근 실행 상태와 이력.',
        icon: <HistoryOutlined />,
      },
      {
        key: 'sap-in',
        to: '/admin/tools/sap-inbound',
        title: 'SAP Inbound',
        desc: 'SAP 으로부터 수신한 마스터 데이터 적재 현황.',
        icon: <DatabaseOutlined />,
      },
      {
        key: 'sap-out',
        to: '/admin/tools/sap-outbound',
        title: 'SAP Outbound',
        desc: 'SAP 으로 전송하는 Outbox 메시지 현황.',
        icon: <DatabaseOutlined />,
      },
    ],
  },
];

export default function SystemDocsHomePage() {
  return (
    <Typography>
      <Title level={2}>시스템 안내</Title>
      <Paragraph type="secondary">
        시스템 관리자와 개발자가 본 시스템을 이해하고 운영하는 데 필요한 안내 문서·도구 모음입니다.
        처음이라면 <Text strong>시스템 개요</Text> 부터 읽어 보세요.
      </Paragraph>

      {SECTIONS.map((section) => (
        <div key={section.title}>
          <Divider orientation="left" orientationMargin={0}>
            {section.title}
          </Divider>
          <Row gutter={[16, 16]} style={{ marginBottom: 8 }}>
            {section.cards.map((card) => {
              const isPlanned = card.status === 'planned';
              return (
                <Col key={card.key} xs={24} sm={12} lg={8} xxl={6}>
                  <Link
                    to={card.to}
                    style={{ pointerEvents: isPlanned ? 'none' : undefined }}
                    tabIndex={isPlanned ? -1 : undefined}
                  >
                    <Card
                      hoverable={!isPlanned}
                      size="small"
                      style={{ height: '100%', opacity: isPlanned ? 0.6 : 1 }}
                    >
                      <Card.Meta
                        avatar={<span style={{ fontSize: 22, color: '#1677ff' }}>{card.icon}</span>}
                        title={
                          <span>
                            {card.title}
                            {isPlanned && (
                              <Tag color="orange" style={{ marginLeft: 8 }}>
                                준비 중
                              </Tag>
                            )}
                          </span>
                        }
                        description={card.desc}
                      />
                    </Card>
                  </Link>
                </Col>
              );
            })}
          </Row>
        </div>
      ))}
    </Typography>
  );
}
