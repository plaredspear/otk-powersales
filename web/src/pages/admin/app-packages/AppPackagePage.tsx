import { useState } from 'react';
import { Alert, Button, Card, Popconfirm, Space, Switch, Tabs, Tag, Tooltip, Typography, message } from 'antd';
import { PlusOutlined, DownloadOutlined, CrownOutlined, CopyOutlined } from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import dayjs from 'dayjs';
import ResizableTable from '@/components/common/ResizableTable';
import RefreshButton from '@/components/common/RefreshButton';
import {
  useAppPackages,
  useDeleteAppPackage,
  useDistributionUrls,
  useSetAppPackageLatest,
  useToggleAppPackageForceUpdate,
} from '@/hooks/appPackage/useAppPackages';
import { fetchAppPackageDetail, type AppPackageListItem, type AppPlatform } from '@/api/appPackage';
import AppPackageUploadModal from './components/AppPackageUploadModal';

const DEFAULT_SIZE = 20;

function formatSize(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

/** presigned URL 유효시간(초)을 "N분"/"N초" 등 사람이 읽는 문구로 변환. */
function formatTtl(seconds: number): string {
  if (seconds % 3600 === 0) return `${seconds / 3600}시간`;
  if (seconds % 60 === 0) return `${seconds / 60}분`;
  return `${seconds}초`;
}

/** navigator.clipboard 미지원(비 HTTPS 등) 환경 대비 fallback 복사. */
async function copyToClipboard(text: string): Promise<void> {
  if (navigator.clipboard?.writeText) {
    await navigator.clipboard.writeText(text);
    return;
  }
  const ta = document.createElement('textarea');
  ta.value = text;
  ta.style.position = 'fixed';
  ta.style.opacity = '0';
  document.body.appendChild(ta);
  ta.select();
  try {
    document.execCommand('copy');
  } finally {
    document.body.removeChild(ta);
  }
}

function PlatformTable({ platform }: { platform: AppPlatform }) {
  const [page, setPage] = useState(0);
  const [uploadOpen, setUploadOpen] = useState(false);
  const { data, isLoading, refetch, isFetching } = useAppPackages(platform, page, DEFAULT_SIZE);
  const isIos = platform === 'IOS';
  const { data: distributionUrls } = useDistributionUrls();
  const distributionUrl = isIos
    ? distributionUrls?.iosInstallUrl
    : distributionUrls?.androidDownloadUrl;
  const setLatest = useSetAppPackageLatest(platform);
  const toggleForce = useToggleAppPackageForceUpdate(platform);
  const remove = useDeleteAppPackage(platform);

  const handleSetLatest = async (id: number) => {
    try {
      await setLatest.mutateAsync(id);
      message.success('최신 버전으로 지정되었습니다');
    } catch (e) {
      message.error(e instanceof Error ? e.message : '최신 지정에 실패했습니다');
    }
  };

  const handleToggleForce = async (id: number, forceUpdate: boolean) => {
    try {
      await toggleForce.mutateAsync({ id, forceUpdate });
    } catch (e) {
      message.error(e instanceof Error ? e.message : '강제 업데이트 변경에 실패했습니다');
    }
  };

  const handleDelete = async (id: number) => {
    try {
      await remove.mutateAsync(id);
      message.success('삭제되었습니다');
    } catch (e) {
      message.error(e instanceof Error ? e.message : '삭제에 실패했습니다');
    }
  };

  const handleDownload = async (id: number) => {
    try {
      const detail = await fetchAppPackageDetail(id);
      // iOS 는 .ipa 직접 다운로드로 설치 불가. 고정 OTA 설치 안내 페이지(API public 도메인)를 새 탭으로 연다.
      if (platform === 'IOS') {
        if (!detail.iosInstallUrl) {
          message.warning('설치 링크를 사용할 수 없습니다 (API 도메인 미설정 환경)');
          return;
        }
        window.open(detail.iosInstallUrl, '_blank', 'noopener');
        return;
      }
      // Android: presigned 다운로드 URL 을 새 탭으로 연다.
      window.open(detail.downloadUrl, '_blank', 'noopener');
    } catch (e) {
      message.error(e instanceof Error ? e.message : '다운로드 URL 발급에 실패했습니다');
    }
  };

  const handleCopyUrl = async (id: number) => {
    try {
      const detail = await fetchAppPackageDetail(id);
      // iOS 는 고정 설치 안내 페이지 URL(API public 도메인)을 복사한다. 새 버전 배포 후 "최신 지정"만
      // 하면 같은 링크가 신버전을 가리키므로 사번 전체에 1회만 공지하면 된다. 카톡/문자로 공유하면
      // 받는 사람이 클릭 → Safari → "설치" 버튼으로 OTA 설치. 링크는 만료되지 않는다.
      if (platform === 'IOS') {
        if (!detail.iosInstallUrl) {
          message.warning('설치 링크를 사용할 수 없습니다 (API 도메인 미설정 환경)');
          return;
        }
        await copyToClipboard(detail.iosInstallUrl);
        message.success('고정 설치 링크가 복사되었습니다 — 사번 전체에 공지해 설치할 수 있습니다');
        return;
      }
      // Android: presigned URL 을 새로 발급받아 클립보드에 복사. URL 은 발급 시점부터 TTL 동안만 유효.
      const ttl = detail.downloadUrlExpiresInSeconds;
      const expiresAt = dayjs().add(ttl, 'second').format('YYYY-MM-DD HH:mm:ss');
      // 첫 줄은 순수 URL(주소창 붙여넣기 호환), 둘째 줄은 만료 안내 주석.
      await copyToClipboard(`${detail.downloadUrl}\n(${expiresAt}까지 유효 · ${formatTtl(ttl)})`);
      message.success(
        `다운로드 URL이 복사되었습니다 — ${formatTtl(ttl)}간 유효 (${expiresAt}까지)`,
      );
    } catch (e) {
      message.error(e instanceof Error ? e.message : 'URL 복사에 실패했습니다');
    }
  };

  const columns: ColumnsType<AppPackageListItem> = [
    {
      title: '버전',
      dataIndex: 'versionName',
      width: 140,
      render: (v: string, r) => (
        <Space>
          <span>{v}</span>
          {r.isLatest && <Tag color="green">최신</Tag>}
        </Space>
      ),
    },
    { title: '버전 코드', dataIndex: 'versionCode', width: 100 },
    {
      title: '강제 업데이트',
      dataIndex: 'forceUpdate',
      width: 120,
      render: (force: boolean, r) => (
        <Switch
          checked={force}
          loading={toggleForce.isPending}
          onChange={(checked) => handleToggleForce(r.id, checked)}
        />
      ),
    },
    {
      title: '릴리스 노트',
      dataIndex: 'releaseNote',
      width: 320,
      // ResizableTable 은 width 지정 컬럼에 ellipsis(한 줄 nowrap 축약) 를 기본 적용한다.
      // 릴리스 노트는 여러 줄로 펼쳐 보여야 하므로 명시적으로 ellipsis 를 끈다.
      ellipsis: false,
      // 고정 폭 안에서 줄바꿈을 보존하며 여러 줄로 펼쳐 보여준다. ellipsis(한 줄 축약) 대신
      // pre-wrap 으로 처리해야 긴 릴리스 노트가 테이블 폭을 밀어내 좌우 스크롤되는 것을 막는다.
      render: (note: string | null) =>
        note ? <span style={{ whiteSpace: 'pre-wrap', wordBreak: 'break-word' }}>{note}</span> : '-',
    },
    { title: '파일명', dataIndex: 'fileName', width: 200, ellipsis: true },
    {
      title: '크기',
      dataIndex: 'fileSize',
      width: 100,
      render: (size: number) => formatSize(size),
    },
    {
      title: '업로드일시',
      dataIndex: 'uploadedAt',
      width: 160,
      render: (v: string) => dayjs(v).format('YYYY-MM-DD HH:mm'),
    },
    {
      title: '액션',
      key: 'action',
      width: 260,
      render: (_: unknown, r) => (
        <Space>
          <Tooltip
            title={platform === 'IOS' ? '고정 설치 페이지 열기 (항상 최신, iPhone Safari)' : '다운로드 (새 탭)'}
          >
            <Button size="small" icon={<DownloadOutlined />} onClick={() => handleDownload(r.id)} />
          </Tooltip>
          <Tooltip title={platform === 'IOS' ? '고정 설치 링크 복사 (대규모 공지용)' : '다운로드 URL 복사'}>
            <Button size="small" icon={<CopyOutlined />} onClick={() => handleCopyUrl(r.id)} />
          </Tooltip>
          {!r.isLatest && (
            <Tooltip title="최신 버전으로 지정">
              <Button size="small" icon={<CrownOutlined />} onClick={() => handleSetLatest(r.id)}>
                최신 지정
              </Button>
            </Tooltip>
          )}
          <Popconfirm
            title="이 버전을 삭제할까요?"
            onConfirm={() => handleDelete(r.id)}
            okText="삭제"
            cancelText="취소"
            disabled={r.isLatest}
          >
            <Button size="small" danger disabled={r.isLatest}>
              삭제
            </Button>
          </Popconfirm>
        </Space>
      ),
    },
  ];

  const handleCopyDistributionUrl = async () => {
    if (!distributionUrl) return;
    try {
      await copyToClipboard(distributionUrl);
      message.success('고정 배포 링크가 복사되었습니다 — 사번 전체에 공지해 설치할 수 있습니다');
    } catch {
      message.error('링크 복사에 실패했습니다');
    }
  };

  return (
    <>
      <Alert
        type="info"
        showIcon
        style={{ marginBottom: 16 }}
        message={isIos ? 'iOS 고정 설치 링크 (대규모 배포용)' : 'Android 고정 설치 링크 (대규모 배포용)'}
        description={
          distributionUrl ? (
            <>
              <Typography.Paragraph style={{ marginBottom: 8, wordBreak: 'break-all' }}>
                <Typography.Text code>{distributionUrl}</Typography.Text>
              </Typography.Paragraph>
              <Typography.Text type="secondary" style={{ fontSize: 12 }}>
                · 항상 <b>최신으로 지정된 버전</b>을 가리킵니다. 새 버전 업로드 후 "최신 지정"만 하면 이 링크가
                신버전을 가리키므로 재공지가 필요 없습니다.
                {isIos ? (
                  <>
                    <br />· iPhone <b>Safari</b>에서 열어야 설치됩니다 (PC·다른 브라우저·인앱 브라우저 불가).
                  </>
                ) : (
                  <>
                    <br />· 링크를 열면 <b>최신 APK</b>가 다운로드됩니다. 설치하려면 기기에서 "출처를 알 수 없는 앱
                    설치"를 허용해야 합니다.
                  </>
                )}
              </Typography.Text>
              <div style={{ marginTop: 8 }}>
                <Button size="small" icon={<CopyOutlined />} onClick={handleCopyDistributionUrl}>
                  링크 복사
                </Button>
              </div>
            </>
          ) : (
            <Typography.Text type="secondary">
              현재 환경에서는 고정 배포 링크를 사용할 수 없습니다 (API 도메인 미설정).
            </Typography.Text>
          )
        }
      />
      <div style={{ marginBottom: 16, display: 'flex', justifyContent: 'flex-end', gap: 8 }}>
        <RefreshButton onRefresh={refetch} refreshing={isFetching} />
        <Button type="primary" icon={<PlusOutlined />} onClick={() => setUploadOpen(true)}>
          패키지 업로드
        </Button>
      </div>
      <ResizableTable<AppPackageListItem>
        rowKey="id"
        loading={isLoading}
        columns={columns}
        dataSource={data?.content ?? []}
        pagination={{
          current: page + 1,
          pageSize: DEFAULT_SIZE,
          total: data?.totalElements ?? 0,
          onChange: (p) => setPage(p - 1),
        }}
      />
      <AppPackageUploadModal
        open={uploadOpen}
        platform={platform}
        onClose={() => setUploadOpen(false)}
      />
    </>
  );
}

export default function AppPackagePage() {
  return (
    <Card title="앱 버전 관리">
      <Tabs
        defaultActiveKey="ANDROID"
        items={[
          { key: 'ANDROID', label: 'Android', children: <PlatformTable platform="ANDROID" /> },
          { key: 'IOS', label: 'iOS', children: <PlatformTable platform="IOS" /> },
        ]}
      />
    </Card>
  );
}
