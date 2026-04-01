-- LoginHistory → EmployeeInfo FK (employee_code)
-- NOT VALID: 기존 데이터에 고아 참조가 있을 수 있으므로 제약 조건 추가 시 기존 행 검증을 건너뜀
ALTER TABLE login_history
    ADD CONSTRAINT fk_login_history_employee_info
        FOREIGN KEY (employee_code) REFERENCES employee_info (employee_code) NOT VALID;
