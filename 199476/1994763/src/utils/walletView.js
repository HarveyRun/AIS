const WITHDRAWAL_STATUS = {
  PROCESSING: '处理中',
  EXPORTED: '支付处理中',
  COMPLETED: '已到账',
  FAILED: '已退回',
};

function formatMoney(amount) {
  return Number(amount || 0).toFixed(2);
}

export function walletTransactionFromApi(item) {
  if (item.direction === 'FREEZE') {
    return [
      '冻结',
      item.description,
      `¥${formatMoney(item.amount)}`,
      new Date(item.createdAt).toLocaleString(),
    ];
  }

  if (item.type === 'INQUIRY_REFUND') {
    return [
      '解冻',
      item.description,
      `¥${formatMoney(item.amount)}`,
      new Date(item.createdAt).toLocaleString(),
    ];
  }

  const isIncome = item.direction === 'IN';
  return [
    isIncome ? '收入' : '支出',
    item.description,
    `${isIncome ? '+' : '-'}¥${formatMoney(item.amount)}`,
    new Date(item.createdAt).toLocaleString(),
  ];
}

export function withdrawalFromApi(item) {
  const alipayLabel = item.alipayAccount
    ? `支付宝 ${item.alipayAccount}`
    : '支付宝账户不可用';

  return [
    `¥${formatMoney(item.amount)}`,
    WITHDRAWAL_STATUS[item.status] || '状态未知',
    new Date(item.createdAt).toLocaleString(),
    item.status === 'FAILED' ? '款项已退回可提现收入' : '',
    alipayLabel,
  ];
}
