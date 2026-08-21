UPDATE admin_permissions
SET name = CASE code
        WHEN 'APP_TEST_ACCOUNT_VIEW' THEN '查看测试账号'
        WHEN 'APP_TEST_ACCOUNT_CREATE' THEN '新增测试账号'
        WHEN 'APP_TEST_ACCOUNT_EDIT' THEN '编辑测试账号'
        WHEN 'APP_TEST_ACCOUNT_DELETE' THEN '删除测试账号'
        ELSE name
    END,
    module_name = '测试账号'
WHERE code IN (
    'APP_TEST_ACCOUNT_VIEW',
    'APP_TEST_ACCOUNT_CREATE',
    'APP_TEST_ACCOUNT_EDIT',
    'APP_TEST_ACCOUNT_DELETE'
);
