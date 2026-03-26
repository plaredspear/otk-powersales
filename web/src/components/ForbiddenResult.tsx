import { Button, Result } from 'antd';
import { useNavigate } from 'react-router-dom';

export default function ForbiddenResult() {
  const navigate = useNavigate();

  return (
    <Result
      status="403"
      title="접근 권한 없음"
      subTitle="해당 기능에 대한 접근 권한이 없습니다. 관리자에게 문의하세요."
      extra={
        <Button type="primary" onClick={() => navigate(-1)}>
          이전으로
        </Button>
      }
    />
  );
}
