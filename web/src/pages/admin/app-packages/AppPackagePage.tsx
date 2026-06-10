import { useState } from 'react';
import { Button, Card, Popconfirm, Space, Switch, Tabs, Tag, Tooltip, message } from 'antd';
import { PlusOutlined, DownloadOutlined, CrownOutlined, CopyOutlined } from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import dayjs from 'dayjs';
import ResizableTable from '@/components/common/ResizableTable';
import {
  useAppPackages,
  useDeleteAppPackage,
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
  const { data, isLoading } = useAppPackages(platform, page, DEFAULT_SIZE);
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
      // 목록엔 URL 이 없으므로 상세 조회로 presigned 다운로드 URL 발급 후 새 탭 열기.
      const detail = await fetchAppPackageDetail(id);
      window.open(detail.downloadUrl, '_blank', 'noopener');
    } catch (e) {
      message.error(e instanceof Error ? e.message : '다운로드 URL 발급에 실패했습니다');
    }
  };

  const handleCopyUrl = async (id: number) => {
    try {
      // presigned URL 을 새로 발급받아 클립보드에 복사. URL 은 발급 시점부터 TTL 동안만 유효.
      const detail = await fetchAppPackageDetail(id);
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
      ellipsis: true,
      render: (note: string | null) => note ?? '-',
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
          <Tooltip title="다운로드 (새 탭)">
            <Button size="small" icon={<DownloadOutlined />} onClick={() => handleDownload(r.id)} />
          </Tooltip>
          <Tooltip title="다운로드 URL 복사">
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

  return (
    <>
      <div style={{ marginBottom: 16, textAlign: 'right' }}>
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
