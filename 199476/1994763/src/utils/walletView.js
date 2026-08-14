const WITHDRAWAL_STATUS = {
  PROCESSING: '处理中',
  COMPLETED: '已到账',
  FAILED: '提现失败',
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
  const bankLabel = item.bankName && item.lastFour
    ? `${item.bankName}（${item.lastFour}）`
    : '银行卡信息不可用';

  return [
    `¥${formatMoney(item.amount)}`,
    WITHDRAWAL_STATUS[item.status] || '状态未知',
    new Date(item.createdAt).toLocaleString(),
    item.status === 'FAILED' ? '款项已退回账户余额' : '',
    bankLabel,
  ];
}
