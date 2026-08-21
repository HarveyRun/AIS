import 'package:flutter/material.dart';

class AppStatusStyle {
  const AppStatusStyle({required this.foreground, required this.background});

  final Color foreground;
  final Color background;
}

AppStatusStyle appStatusStyle(BuildContext context, String rawStatus) {
  final status = rawStatus.trim().toUpperCase();

  if ({
    'APPROVED',
    'ACTIVE',
    'COMPLETED',
    'ENDED',
    'RESOLVED',
    '已认证',
    '已通过',
    '已完成',
    '已解决',
    '交流中',
  }.contains(status)) {
    const color = Color(0xFF27865C);
    return AppStatusStyle(
      foreground: color,
      background: color.withValues(alpha: .12),
    );
  }

  if ({
    'PENDING',
    'BOOKED',
    'PROCESSING',
    'AWAITING_CONFIRMATION',
    'SUBMITTED',
    '审核中',
    '已预约',
    '待处理',
    '处理中',
    '等待接受',
    '等待确认',
  }.contains(status)) {
    const color = Color(0xFFB36B18);
    return AppStatusStyle(
      foreground: color,
      background: color.withValues(alpha: .12),
    );
  }

  if ({
    'REJECTED',
    'FAILED',
    'CANCELLED',
    'EXPIRED',
    '已拒绝',
    '已失败',
    '已撤销',
    '已超时',
    '退回修改',
  }.contains(status)) {
    final color = Theme.of(context).colorScheme.error;
    return AppStatusStyle(
      foreground: color,
      background: color.withValues(alpha: .10),
    );
  }

  final color = Theme.of(context).colorScheme.onSurfaceVariant;
  return AppStatusStyle(
    foreground: color,
    background: Theme.of(context).colorScheme.surfaceContainerHighest,
  );
}
