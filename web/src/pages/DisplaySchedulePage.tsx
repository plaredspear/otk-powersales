import { useState } from 'react';
import { Button, Card, message, Select, Space, Typography } from 'antd';
import { DownloadOutlined } from '@ant-design/icons';
import { useScheduleBranches } from '@/hooks/schedule/useScheduleBranches';
import { downloadScheduleTemplate } from '@/api/schedule';

const { Text } = Typography;

export default function DisplaySchedulePage() {
  const [selectedBranch, setSelectedBranch] = useState<string | undefined>(undefined);
  const [downloading, setDownloading] = useState(false);
  const { data: branches, isLoading: branchesLoading } = useScheduleBranches();

  const handleDownload = async () => {
    if (!selectedBranch) {
      message.warning('지점을 선택해주세요');
      return;
    }

    setDownloading(true);
    try {
      await downloadScheduleTemplate(selectedBranch);
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : '양식 다운로드에 실패했습니다';
      message.error(errorMessage);
    } finally {
      setDownloading(false);
    }
  };

  const branchOptions = (branches ?? []).map((b) => ({
    label: b.branchName,
    value: b.costCenterCode,
  }));

  return (
    <div>
      <Card title="업로드용 양식 다운로드">
        <Space direction="vertical" size="middle">
          <div>
            <Text strong>지점 선택: </Text>
            <Select
              showSearch
              placeholder="지점을 선택해주세요"
              style={{ width: 250 }}
              value={selectedBranch}
              onChange={setSelectedBranch}
              options={branchOptions}
              loading={branchesLoading}
              filterOption={(input, option) =>
                (option?.label ?? '').toLowerCase().includes(input.toLowerCase())
              }
            />
          </div>
          <Button
            type="primary"
            icon={<DownloadOutlined />}
            loading={downloading}
            onClick={handleDownload}
          >
            양식 다운로드
          </Button>
          <Text type="secondary">
            ※ 지점을 선택하면 해당 지점 사원이 포함된 양식이 다운로드됩니다.
          </Text>
        </Space>
      </Card>
    </div>
  );
}
