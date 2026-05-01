import { ToolOutlined } from '@ant-design/icons';
import { Result } from 'antd';

export default function DashboardPage() {
  return (
    <div style={{ padding: 24 }}>
      <Result
        icon={<ToolOutlined />}
        title="작업 진행 중"
        subTitle={
          <>
            대시보드는 현재 개선 작업이 진행되고 있습니다.
            <br />
            잠시 후 다시 이용해 주세요.
          </>
        }
      />
    </div>
  );
}
