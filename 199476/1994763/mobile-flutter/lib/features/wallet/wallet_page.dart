import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:intl/intl.dart';
import 'package:url_launcher/url_launcher.dart';

import '../../app/providers.dart';
import '../../core/widgets/app_message.dart';
import '../../data/models/wallet_models.dart';

enum WalletTab { transactions, withdrawals, recharge, withdraw }

class WalletPage extends ConsumerStatefulWidget {
  const WalletPage({super.key});
  @override
  ConsumerState<WalletPage> createState() => _WalletPageState();
}

class _WalletPageState extends ConsumerState<WalletPage> {
  WalletTab _tab = WalletTab.transactions;
  WalletInfo? _wallet;
  BankCardInfo? _bankCard;
  List<WalletTransaction> _transactions = const [];
  List<WithdrawalRecord> _withdrawals = const [];
  final _amount = TextEditingController();
  bool _loading = true;
  bool _submitting = false;

  @override
  void initState() {
    super.initState();
    _load();
  }

  @override
  void dispose() {
    _amount.dispose();
    super.dispose();
  }

  Future<void> _load() async {
    setState(() => _loading = true);
    try {
      final results = await Future.wait([
        ref.read(repositoryProvider).wallet(),
        ref.read(repositoryProvider).walletTransactions(),
        ref.read(repositoryProvider).withdrawals(),
        ref.read(repositoryProvider).bankCard(),
      ]);
      if (!mounted) return;
      setState(() {
        _wallet = results[0] as WalletInfo;
        _transactions = results[1] as List<WalletTransaction>;
        _withdrawals = results[2] as List<WithdrawalRecord>;
        _bankCard = results[3] as BankCardInfo?;
      });
    } catch (error) {
      if (mounted) AppMessage.show(context, '$error');
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  Future<void> _bindCard() async {
    final saved = await showModalBottomSheet<BankCardInfo>(
      context: context,
      isScrollControlled: true,
      builder: (context) => _BankCardSheet(current: _bankCard),
    );
    if (saved != null) setState(() => _bankCard = saved);
  }

  Future<void> _submit() async {
    final amount = double.tryParse(_amount.text);
    if (amount == null || amount <= 0) {
      AppMessage.show(context, '请输入有效金额');
      return;
    }
    setState(() => _submitting = true);
    try {
      if (_tab == WalletTab.recharge) {
        final capability = await ref
            .read(repositoryProvider)
            .rechargeCapability();
        if (!mounted) return;
        if (capability['available'] == false) {
          AppMessage.show(
            context,
            capability['message']?.toString() ?? '充值暂不可用',
          );
          return;
        }
        final order = await ref.read(repositoryProvider).createRecharge(amount);
        final paymentUrl = order['paymentUrl']?.toString() ?? '';
        if (paymentUrl.isNotEmpty) {
          await launchUrl(
            Uri.parse(paymentUrl),
            mode: LaunchMode.externalApplication,
          );
        } else if (mounted) {
          AppMessage.show(context, '充值订单已创建，请完成支付宝支付');
        }
      } else {
        if (_bankCard == null) {
          await _bindCard();
          if (_bankCard == null) return;
        }
        await ref.read(repositoryProvider).withdraw(amount);
        if (mounted) AppMessage.show(context, '提现申请已经提交');
      }
      _amount.clear();
      await _load();
    } catch (error) {
      if (mounted) AppMessage.show(context, '$error');
    } finally {
      if (mounted) setState(() => _submitting = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final wallet = _wallet;
    final amount = double.tryParse(_amount.text) ?? 0;
    final remainingFree = wallet == null
        ? 10000.0
        : (10000 - wallet.totalWithdrawn).clamp(0, 10000).toDouble();
    final fee = ((amount - remainingFree).clamp(0, double.infinity) * .2)
        .toDouble();
    return Scaffold(
      appBar: AppBar(title: const Text('账户余额')),
      body: _loading
          ? const Center(child: CircularProgressIndicator(strokeWidth: 2))
          : RefreshIndicator(
              onRefresh: _load,
              child: ListView(
                padding: const EdgeInsets.fromLTRB(17, 8, 17, 30),
                children: [
                  Container(
                    padding: const EdgeInsets.all(23),
                    decoration: BoxDecoration(
                      gradient: const LinearGradient(
                        begin: Alignment.topLeft,
                        end: Alignment.bottomRight,
                        colors: [Color(0xFF4A3835), Color(0xFF261E1D)],
                      ),
                      borderRadius: BorderRadius.circular(20),
                    ),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Row(
                          children: [
                            Expanded(
                              child: _Balance(
                                label: '可用余额',
                                value: wallet?.availableBalance ?? 0,
                              ),
                            ),
                            Expanded(
                              child: _Balance(
                                label: '冻结中',
                                value: wallet?.frozenBalance ?? 0,
                              ),
                            ),
                          ],
                        ),
                        const SizedBox(height: 14),
                        Text(
                          '累计已提现 ¥${(wallet?.totalWithdrawn ?? 0).toStringAsFixed(2)}  ·  剩余免费提现额度 ¥${remainingFree.toStringAsFixed(2)}',
                          style: const TextStyle(
                            color: Color(0xE6FFFFFF),
                            fontSize: 12,
                          ),
                        ),
                      ],
                    ),
                  ),
                  const SizedBox(height: 16),
                  _WalletTabs(
                    selected: _tab,
                    onChanged: (value) => setState(() => _tab = value),
                  ),
                  const SizedBox(height: 18),
                  if (_tab == WalletTab.transactions)
                    _TransactionList(items: _transactions),
                  if (_tab == WalletTab.withdrawals)
                    _WithdrawalList(items: _withdrawals),
                  if (_tab == WalletTab.recharge ||
                      _tab == WalletTab.withdraw) ...[
                    TextField(
                      controller: _amount,
                      onChanged: (_) => setState(() {}),
                      keyboardType: const TextInputType.numberWithOptions(
                        decimal: true,
                      ),
                      inputFormatters: [
                        FilteringTextInputFormatter.allow(
                          RegExp(r'^\d*\.?\d{0,2}'),
                        ),
                      ],
                      decoration: InputDecoration(
                        labelText: _tab == WalletTab.recharge ? '充值金额' : '提现金额',
                        prefixText: '¥ ',
                      ),
                    ),
                    const SizedBox(height: 14),
                    if (_tab == WalletTab.recharge)
                      ListTile(
                        contentPadding: EdgeInsets.zero,
                        leading: CircleAvatar(
                          backgroundColor: Theme.of(
                            context,
                          ).colorScheme.surfaceContainerHighest,
                          foregroundColor: Theme.of(
                            context,
                          ).colorScheme.primary,
                          child: const Text('支'),
                        ),
                        title: const Text('支付宝'),
                        subtitle: const Text('仅支持支付宝充值'),
                      ),
                    if (_tab == WalletTab.withdraw) ...[
                      ListTile(
                        contentPadding: EdgeInsets.zero,
                        leading: const Icon(Icons.account_balance_outlined),
                        title: const Text('到账银行卡'),
                        subtitle: Text(
                          _bankCard == null
                              ? '尚未绑定，点击添加'
                              : '${_bankCard!.bankName}（${_bankCard!.lastFour}）',
                        ),
                        trailing: TextButton(
                          onPressed: _bindCard,
                          child: Text(_bankCard == null ? '添加' : '修改'),
                        ),
                      ),
                      Text(
                        '本次手续费 ¥${fee.toStringAsFixed(2)}，预计到账 ¥${(amount - fee).clamp(0, double.infinity).toStringAsFixed(2)}。累计提现10,000元以内免费，超出部分收取20%。',
                        style: Theme.of(context).textTheme.bodySmall,
                      ),
                    ],
                    const SizedBox(height: 18),
                    FilledButton(
                      onPressed: amount > 0 && !_submitting ? _submit : null,
                      child: _submitting
                          ? const SizedBox.square(
                              dimension: 18,
                              child: CircularProgressIndicator(
                                strokeWidth: 2,
                                color: Colors.white,
                              ),
                            )
                          : Text(_tab == WalletTab.recharge ? '支付宝充值' : '确认提现'),
                    ),
                  ],
                ],
              ),
            ),
    );
  }
}

class _WalletTabs extends StatelessWidget {
  const _WalletTabs({required this.selected, required this.onChanged});

  final WalletTab selected;
  final ValueChanged<WalletTab> onChanged;

  @override
  Widget build(BuildContext context) {
    const tabs = [
      (WalletTab.transactions, '收支明细'),
      (WalletTab.withdrawals, '提现记录'),
      (WalletTab.recharge, '充值'),
      (WalletTab.withdraw, '提现'),
    ];
    return Container(
      height: 46,
      padding: const EdgeInsets.all(4),
      decoration: BoxDecoration(
        color: Theme.of(context).colorScheme.surfaceContainer,
        borderRadius: BorderRadius.circular(13),
      ),
      child: Row(
        children: tabs.map((tab) {
          final active = selected == tab.$1;
          return Expanded(
            child: InkWell(
              onTap: () => onChanged(tab.$1),
              borderRadius: BorderRadius.circular(10),
              child: AnimatedContainer(
                duration: const Duration(milliseconds: 160),
                alignment: Alignment.center,
                decoration: BoxDecoration(
                  color: active
                      ? Theme.of(context).colorScheme.primary
                      : Colors.transparent,
                  borderRadius: BorderRadius.circular(10),
                ),
                child: AnimatedDefaultTextStyle(
                  duration: const Duration(milliseconds: 160),
                  style: TextStyle(
                    fontSize: 10,
                    fontWeight: active ? FontWeight.w700 : FontWeight.w500,
                    color: active
                        ? Colors.white
                        : Theme.of(context).textTheme.bodySmall?.color,
                  ),
                  child: Text(tab.$2),
                ),
              ),
            ),
          );
        }).toList(),
      ),
    );
  }
}

class _Balance extends StatelessWidget {
  const _Balance({required this.label, required this.value});
  final String label;
  final double value;
  @override
  Widget build(BuildContext context) => Column(
    crossAxisAlignment: CrossAxisAlignment.start,
    children: [
      Text(
        label,
        style: const TextStyle(color: Color(0xCCFFFFFF), fontSize: 12),
      ),
      const SizedBox(height: 6),
      Text(
        '¥${value.toStringAsFixed(2)}',
        style: const TextStyle(
          color: Colors.white,
          fontSize: 24,
          fontWeight: FontWeight.w700,
        ),
      ),
    ],
  );
}

class _TransactionList extends StatelessWidget {
  const _TransactionList({required this.items});
  final List<WalletTransaction> items;
  @override
  Widget build(BuildContext context) {
    if (items.isEmpty) {
      return const Center(
        child: Padding(
          padding: EdgeInsets.only(top: 60),
          child: Text('暂无收支记录'),
        ),
      );
    }
    return Column(
      children: items.map((item) {
        final income = item.direction.toUpperCase() == 'INCOME';
        return Container(
          margin: const EdgeInsets.only(bottom: 8),
          decoration: BoxDecoration(
            color: Theme.of(context).colorScheme.surface,
            border: Border.all(
              color: Theme.of(context).colorScheme.outlineVariant,
            ),
            borderRadius: BorderRadius.circular(13),
          ),
          child: ListTile(
            contentPadding: const EdgeInsets.symmetric(horizontal: 12),
            leading: CircleAvatar(
              backgroundColor: Theme.of(
                context,
              ).colorScheme.surfaceContainerHighest,
              foregroundColor: Theme.of(context).colorScheme.primary,
              child: Text(
                income
                    ? '收'
                    : item.direction.toUpperCase() == 'FREEZE'
                    ? '冻'
                    : '支',
              ),
            ),
            title: Text(item.description),
            subtitle: Text(
              item.createdAt == null
                  ? ''
                  : DateFormat('yyyy-MM-dd HH:mm').format(item.createdAt!),
            ),
            trailing: Text(
              '${income ? '+' : '-'}¥${item.amount.toStringAsFixed(2)}',
              style: TextStyle(
                fontWeight: FontWeight.w700,
                color: income ? const Color(0xFF26865C) : null,
              ),
            ),
          ),
        );
      }).toList(),
    );
  }
}

class _WithdrawalList extends StatelessWidget {
  const _WithdrawalList({required this.items});
  final List<WithdrawalRecord> items;
  @override
  Widget build(BuildContext context) {
    if (items.isEmpty) {
      return const Center(
        child: Padding(
          padding: EdgeInsets.only(top: 60),
          child: Text('暂无提现记录'),
        ),
      );
    }
    return Column(
      children: items
          .map(
            (item) => ListTile(
              contentPadding: EdgeInsets.zero,
              title: Text('提现至${item.bankName}（${item.lastFour}）'),
              subtitle: Text(
                '手续费 ¥${item.fee.toStringAsFixed(2)}  ·  ${item.createdAt == null ? '' : DateFormat('yyyy-MM-dd HH:mm').format(item.createdAt!)}',
              ),
              trailing: Column(
                mainAxisAlignment: MainAxisAlignment.center,
                crossAxisAlignment: CrossAxisAlignment.end,
                children: [
                  Text(
                    '¥${item.amount.toStringAsFixed(2)}',
                    style: const TextStyle(fontWeight: FontWeight.w700),
                  ),
                  Text(
                    item.status,
                    style: Theme.of(context).textTheme.bodySmall,
                  ),
                ],
              ),
            ),
          )
          .toList(),
    );
  }
}

class _BankCardSheet extends ConsumerStatefulWidget {
  const _BankCardSheet({this.current});
  final BankCardInfo? current;
  @override
  ConsumerState<_BankCardSheet> createState() => _BankCardSheetState();
}

class _BankCardSheetState extends ConsumerState<_BankCardSheet> {
  final _holder = TextEditingController();
  final _number = TextEditingController();
  String _bank = '';
  bool _saving = false;
  static const _banks = [
    '中国工商银行',
    '中国农业银行',
    '中国银行',
    '中国建设银行',
    '交通银行',
    '招商银行',
    '中国邮政储蓄银行',
  ];

  @override
  void initState() {
    super.initState();
    _holder.text = widget.current?.holderName ?? '';
    _bank = widget.current?.bankName ?? '';
  }

  @override
  void dispose() {
    _holder.dispose();
    _number.dispose();
    super.dispose();
  }

  Future<void> _save() async {
    if (_holder.text.trim().isEmpty ||
        _bank.isEmpty ||
        !RegExp(r'^\d{12,19}$').hasMatch(_number.text)) {
      AppMessage.show(context, '请填写正确的银行卡信息');
      return;
    }
    setState(() => _saving = true);
    try {
      final saved = await ref
          .read(repositoryProvider)
          .bindBankCard(
            holderName: _holder.text.trim(),
            bankName: _bank,
            cardNumber: _number.text,
          );
      if (mounted) Navigator.pop(context, saved);
    } catch (error) {
      if (mounted) AppMessage.show(context, '$error');
    } finally {
      if (mounted) setState(() => _saving = false);
    }
  }

  @override
  Widget build(BuildContext context) => Padding(
    padding: EdgeInsets.fromLTRB(
      20,
      20,
      20,
      MediaQuery.viewInsetsOf(context).bottom + 24,
    ),
    child: Column(
      mainAxisSize: MainAxisSize.min,
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          widget.current == null ? '添加银行卡' : '修改银行卡',
          style: Theme.of(context).textTheme.titleLarge,
        ),
        const SizedBox(height: 4),
        Text('仅可绑定一张银行卡', style: Theme.of(context).textTheme.bodySmall),
        const SizedBox(height: 18),
        TextField(
          controller: _holder,
          decoration: const InputDecoration(labelText: '持卡人'),
        ),
        const SizedBox(height: 12),
        DropdownButtonFormField<String>(
          value: _bank.isEmpty ? null : _bank,
          items: _banks
              .map((bank) => DropdownMenuItem(value: bank, child: Text(bank)))
              .toList(),
          onChanged: (value) => setState(() => _bank = value ?? ''),
          decoration: const InputDecoration(labelText: '开户银行'),
        ),
        const SizedBox(height: 12),
        TextField(
          controller: _number,
          keyboardType: TextInputType.number,
          inputFormatters: [
            FilteringTextInputFormatter.digitsOnly,
            LengthLimitingTextInputFormatter(19),
          ],
          decoration: const InputDecoration(labelText: '银行卡号'),
        ),
        const SizedBox(height: 18),
        FilledButton(
          onPressed: _saving ? null : _save,
          child: Text(widget.current == null ? '确认绑定' : '保存修改'),
        ),
      ],
    ),
  );
}
