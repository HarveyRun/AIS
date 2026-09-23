import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:intl/intl.dart';

import '../../app/providers.dart';
import '../../core/formatters/money_formatter.dart';
import '../../core/widgets/app_message.dart';
import '../../data/models/wallet_models.dart';

class WalletTransactionDetailPage extends ConsumerStatefulWidget {
  const WalletTransactionDetailPage({super.key, required this.transactionId});

  final int transactionId;

  @override
  ConsumerState<WalletTransactionDetailPage> createState() =>
      _WalletTransactionDetailPageState();
}

class _WalletTransactionDetailPageState
    extends ConsumerState<WalletTransactionDetailPage> {
  WalletTransaction? _item;
  String? _error;
  bool _loading = true;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      final item = await ref
          .read(repositoryProvider)
          .walletTransaction(widget.transactionId);
      if (mounted) setState(() => _item = item);
    } catch (error) {
      if (mounted) setState(() => _error = '$error');
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('收支详情')),
      body: _loading
          ? const SizedBox.shrink()
          : _error != null
          ? _ErrorState(message: _error!, onRetry: _load)
          : _Detail(item: _item!),
    );
  }
}

class _Detail extends StatelessWidget {
  const _Detail({required this.item});

  final WalletTransaction item;

  @override
  Widget build(BuildContext context) {
    final direction = item.direction.trim().toUpperCase();
    final income = direction == 'IN' || direction == 'INCOME';
    final expense = direction == 'OUT' || direction == 'EXPENSE';
    final prefix = income
        ? '+'
        : expense
        ? '-'
        : '';
    final amountColor = income
        ? const Color(0xFF26865C)
        : expense
        ? Theme.of(context).colorScheme.error
        : Theme.of(context).colorScheme.onSurface;
    return ListView(
      padding: const EdgeInsets.fromLTRB(10, 12, 10, 30),
      children: [
        Container(
          padding: const EdgeInsets.fromLTRB(18, 24, 18, 20),
          decoration: BoxDecoration(
            color: Theme.of(context).colorScheme.surface,
            borderRadius: BorderRadius.circular(18),
          ),
          child: Column(
            children: [
              Text(
                item.description,
                style: Theme.of(context).textTheme.titleMedium,
              ),
              const SizedBox(height: 12),
              Text(
                '$prefix¥${formatMoney(item.amount)}',
                style: Theme.of(context).textTheme.headlineMedium?.copyWith(
                  color: amountColor,
                  fontWeight: FontWeight.w800,
                ),
              ),
              const SizedBox(height: 8),
              Text(
                _status(item.direction),
                style: Theme.of(context).textTheme.bodySmall?.copyWith(
                  color: Theme.of(context).colorScheme.onSurfaceVariant,
                ),
              ),
            ],
          ),
        ),
        const SizedBox(height: 12),
        Container(
          padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 6),
          decoration: BoxDecoration(
            color: Theme.of(context).colorScheme.surface,
            borderRadius: BorderRadius.circular(18),
          ),
          child: Column(
            children: [
              _Row(label: '交易类型', value: _typeLabel(item.type)),
              _Row(
                label: '交易时间',
                value: item.createdAt == null
                    ? '-'
                    : DateFormat('yyyy-MM-dd HH:mm:ss').format(item.createdAt!),
              ),
              _Row(
                label: '交易单号',
                value: item.transactionNo.isEmpty
                    ? 'SXW-TX-${item.id}'
                    : item.transactionNo,
                copyable: true,
              ),
              if (item.referenceId != null)
                _Row(
                  label: '关联业务',
                  value:
                      '${_referenceLabel(item.referenceType)} #${item.referenceId}',
                ),
              _Row(label: '交易说明', value: item.description),
              _Row(
                label: '变动后可用余额',
                value: '¥${formatMoney(item.availableAfter)}',
              ),
              _Row(
                label: '变动后冻结金额',
                value: '¥${formatMoney(item.frozenAfter)}',
                last: true,
              ),
            ],
          ),
        ),
      ],
    );
  }

  String _status(String direction) => switch (direction.toUpperCase()) {
    'IN' || 'INCOME' => '已入账',
    'OUT' || 'EXPENSE' => '已支出',
    'FREEZE' => '已冻结',
    'HOLD' => '待解冻',
    _ => '资金变动已记录',
  };

  String _referenceLabel(String value) => switch (value.toUpperCase()) {
    'INQUIRY' => '询问',
    'VOICE_CALL' => '语音通话',
    'WITHDRAWAL' => '提现',
    'RECHARGE' => '充值',
    'EXPERIENCE_TIP' => '经历打赏',
    'CURATED_MEMBERSHIP' => '严选直聊',
    'FIRST_EXPERIENCE_REWARD' => '首次发布奖励',
    _ => '平台业务',
  };

  String _typeLabel(String value) {
    const labels = {
      'RECHARGE': '充值',
      'WITHDRAWAL': '提现',
      'EXPERIENCE_TIP': '经历打赏',
      'INQUIRY_FREEZE': '询问金额冻结',
      'INQUIRY_REFUND': '询问退款',
      'INQUIRY_SETTLEMENT': '询问结算',
      'VOICE_CALL_FREEZE': '语音通话预扣',
      'VOICE_CALL_REFUND': '语音通话退款',
      'VOICE_CALL_SETTLEMENT': '语音通话结算',
      'CURATED_MEMBERSHIP_PURCHASE': '严选直聊开通',
      'FIRST_EXPERIENCE_REWARD': '首次发布经历奖励',
    };
    return labels[value.toUpperCase()] ?? item.description;
  }
}

class _Row extends StatelessWidget {
  const _Row({
    required this.label,
    required this.value,
    this.copyable = false,
    this.last = false,
  });

  final String label;
  final String value;
  final bool copyable;
  final bool last;

  @override
  Widget build(BuildContext context) => Container(
    padding: const EdgeInsets.symmetric(vertical: 14),
    decoration: BoxDecoration(
      border: last
          ? null
          : Border(
              bottom: BorderSide(
                color: Theme.of(context).dividerColor.withValues(alpha: .25),
              ),
            ),
    ),
    child: Row(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        SizedBox(
          width: 104,
          child: Text(
            label,
            style: TextStyle(
              color: Theme.of(context).colorScheme.onSurfaceVariant,
            ),
          ),
        ),
        Expanded(child: Text(value, textAlign: TextAlign.right)),
        if (copyable) ...[
          const SizedBox(width: 6),
          InkWell(
            onTap: () async {
              await Clipboard.setData(ClipboardData(text: value));
              if (context.mounted) AppMessage.show(context, '交易单号已复制');
            },
            child: Icon(
              Icons.copy_rounded,
              size: 17,
              color: Theme.of(context).colorScheme.primary,
            ),
          ),
        ],
      ],
    ),
  );
}

class _ErrorState extends StatelessWidget {
  const _ErrorState({required this.message, required this.onRetry});

  final String message;
  final VoidCallback onRetry;

  @override
  Widget build(BuildContext context) => Center(
    child: Padding(
      padding: const EdgeInsets.all(28),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          Text(message, textAlign: TextAlign.center),
          const SizedBox(height: 16),
          FilledButton.tonal(onPressed: onRetry, child: const Text('重新加载')),
        ],
      ),
    ),
  );
}
