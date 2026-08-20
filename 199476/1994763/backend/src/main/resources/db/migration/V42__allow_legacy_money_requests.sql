-- 兼容新旧后端短暂并行部署；新版 App 仍会为每笔请求提供唯一 request_no。
ALTER TABLE recharges
    MODIFY request_no VARCHAR(64) NULL;

ALTER TABLE withdrawals
    MODIFY request_no VARCHAR(64) NULL;
