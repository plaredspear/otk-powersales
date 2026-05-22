-- Spec #789 P1-B — team_member_schedule.attendance_log_id 운영 row backfill.
--
-- 선행 정합 (#749 V103): attendance_log_id BIGINT FK + 인덱스 + @ManyToOne(AttendanceLog) 매핑 신설 완료.
-- 그러나 운영 row 의 attendance_log_id 는 NULL 잔존 — commute_log_sfid 만 채워진 상태.
--
-- 본 마이그레이션: commute_log_sfid 양방향 해석 (sfid OR id 문자열) 으로 1회 backfill.
-- #587 P1-B §1.4 양방향 매칭 정책의 단방향 id-FK 전환 (#672 audit 권고 정합).
--
-- 신규 row 부터 NULL 미발생은 application 레이어 (AttendanceService 의 schedule.attendanceLog = saved backlink) 가 보장.

UPDATE powersales.team_member_schedule tms
   SET attendance_log_id = al.attendance_log_id
  FROM powersales.attendance_log al
 WHERE tms.attendance_log_id IS NULL
   AND tms.commute_log_sfid IS NOT NULL
   AND (tms.commute_log_sfid = al.sfid
        OR tms.commute_log_sfid = al.attendance_log_id::text);
