import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:font_awesome_flutter/font_awesome_flutter.dart';
import 'package:intl/intl.dart';
import 'package:tobias/tobias.dart' as tobias;
import 'package:url_launcher/url_launcher.dart';

import '../../app/providers.dart';
import '../../core/formatters/money_formatter.dart';
import '../../core/config/app_config.dart';
import '../../core/theme/app_status_style.dart';
import '../../core/input/app_input_formatters.dart';
import '../../core/network/request_id.dart';
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
  AlipayAccountInfo? _alipayAccount;
  List<WalletTransaction> _transactions = const [];
  List<WithdrawalRecord> _withdrawals = const [];
  final _amount = TextEditingController();
  bool _loading = true;
  bool _submitting = false;
  bool _authorizingAlipay = false;
  String? _rechargeRequestId;
  String? _withdrawalRequestId;

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
        ref.read(repositoryProvider).alipayAccount(),
      ]);
      if (!mounted) return;
      setState(() {
        _wallet = results[0] as WalletInfo;
        _transactions = results[1] as List<WalletTransaction>;
        _withdrawals = results[2] as List<WithdrawalRecord>;
        _alipayAccount = results[3] as AlipayAccountInfo?;
      });
    } catch (error) {
      if (mounted) AppMessage.show(context, '$error');
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  Future<void> _authorizeAlipay() async {
    if (_authorizingAlipay) return;
    if (!await tobias.Tobias().isAliPayInstalled) {
      if (mounted) AppMessage.show(context, '请先安装支付宝');
      return;
    }
    setState(() => _authorizingAlipay = true);
    try {
      final payload = await ref
          .read(repositoryProvider)
          .alipayAuthorizationPayload();
      if (payload.isEmpty) throw Exception('支付宝授权信息生成失败');
      final result = await tobias.Tobias().auth(payload);
      final resultStatus = result['resultStatus']?.toString() ?? '';
      if (resultStatus == '6001') {
        if (mounted) AppMessage.show(context, '已取消支付宝授权');
        return;
      }
      final values = Uri.splitQueryString(result['result']?.toString() ?? '');
      if (resultStatus != '9000' || values['result_code'] != '200') {
        throw Exception('支付宝授权未完成');
      }
      final authCode = values['auth_code'] ?? '';
      if (authCode.isEmpty) throw Exception('支付宝未返回授权码');
      final saved = await ref
          .read(repositoryProvider)
          .completeAlipayAuthorization(authCode);
      if (!mounted) return;
      setState(() => _alipayAccount = saved);
      AppMessage.show(context, '支付宝授权成功');
    } catch (error) {
      if (mounted) AppMessage.show(context, '$error');
    } finally {
      if (mounted) setState(() => _authorizingAlipay = false);
    }
  }

  Future<void> _submit() async {
    final amount = int.tryParse(_amount.text);
    if (amount == null || amount <= 0 || amount > 9999) {
      AppMessage.show(context, '请输入1—9999的整数金额');
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
        final requestId = _rechargeRequestId ?? RequestId.create('recharge');
        _rechargeRequestId = requestId;
        final order = await ref
            .read(repositoryProvider)
            .createRecharge(amount, requestId);
        final paymentPayload = order['paymentPayload']?.toString() ?? '';
        final paymentMode = capability['paymentMode']?.toString() ?? '';
        if (paymentMode == 'TEST' && order['status']?.toString() == 'PAID') {
          if (mounted) AppMessage.show(context, '余额已到账');
        } else {
          if (paymentPayload.isEmpty) {
            throw Exception('支付信息生成失败');
          }
          final mockPayment =
              paymentMode == 'MOCK_WEB' ||
              paymentPayload.startsWith('/api/recharges/mock-cashier');
          final alipayAppPayment =
              paymentMode == 'ALIPAY_APP' ||
              _looksLikeAlipayAppOrder(paymentPayload);
          if (alipayAppPayment) {
            await _payWithAlipayApp(order, paymentPayload);
          } else if (mockPayment) {
            await launchUrl(
              AppConfig.resolveResource(paymentPayload),
              mode: LaunchMode.externalApplication,
            );
          } else {
            throw Exception('支付方式返回异常，请稍后重试');
          }
        }
      } else {
        if (_alipayAccount == null) {
          AppMessage.show(context, '请先完成支付宝授权');
          return;
        }
        if (!mounted || !await _confirmWithdrawal(amount)) return;
        final requestId =
            _withdrawalRequestId ?? RequestId.create('withdrawal');
        _withdrawalRequestId = requestId;
        final verificationCode = await _requestWithdrawalCode();
        if (verificationCode == null) return;
        final withdrawal = await ref
            .read(repositoryProvider)
            .withdraw(amount, requestId, verificationCode);
        if (mounted) {
          AppMessage.show(
            context,
            withdrawal.status == 'COMPLETED' ? '提现已完成' : '提现申请已经提交',
          );
        }
      }
      _amount.clear();
      _rechargeRequestId = null;
      _withdrawalRequestId = null;
      await _load();
    } catch (error) {
      if (mounted) AppMessage.show(context, '$error');
    } finally {
      if (mounted) setState(() => _submitting = false);
    }
  }

  Future<bool> _confirmWithdrawal(int amount) async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('确认提现'),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text('提现金额  ¥${formatMoney(amount)}'),
            const SizedBox(height: 8),
            Text('支付宝账户  ${_alipayAccount?.accountMasked ?? ''}'),
          ],
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context, false),
            child: const Text('取消'),
          ),
          FilledButton(
            onPressed: () => Navigator.pop(context, true),
            child: const Text('确认'),
          ),
        ],
      ),
    );
    return confirmed == true;
  }

  Future<String?> _requestWithdrawalCode() async {
    try {
      await ref
          .read(repositoryProvider)
          .sendWalletVerificationCode('WITHDRAWAL');
    } catch (error) {
      if (mounted) AppMessage.show(context, '$error');
      return null;
    }
    if (!mounted) return null;
    return showDialog<String>(
      context: context,
      builder: (context) => const _WithdrawalCodeDialog(),
    );
  }

  Future<void> _payWithAlipayApp(
    Map<String, dynamic> order,
    String paymentPayload,
  ) async {
    if (!await tobias.Tobias().isAliPayInstalled) {
      if (mounted) AppMessage.show(context, '请先安装支付宝');
      return;
    }
    final result = await tobias.Tobias().pay(paymentPayload);
    final resultStatus = result['resultStatus']?.toString() ?? '';
    if (resultStatus == '6001') {
      if (mounted) AppMessage.show(context, '支付已取消');
      return;
    }
    if (resultStatus != '9000') {
      if (mounted) AppMessage.show(context, '支付未完成');
      return;
    }
    final orderNo = order['orderNo']?.toString() ?? '';
    final latest = await ref.read(repositoryProvider).recharge(orderNo);
    if (!mounted) return;
    if (latest['status'] == 'PAID') {
      AppMessage.show(context, '充值成功');
    } else {
      AppMessage.show(context, '支付结果确认中，请稍后刷新');
    }
  }

  bool _looksLikeAlipayAppOrder(String payload) {
    return payload.contains('app_id=') &&
        payload.contains('method=alipay.trade.app.pay');
  }

  @override
  Widget build(BuildContext context) {
    final wallet = _wallet;
    final amount = int.tryParse(_amount.text) ?? 0;
    return Scaffold(
      appBar: AppBar(title: const Text('账户余额')),
      body: _loading
          ? const Center(child: CircularProgressIndicator(strokeWidth: 2))
          : RefreshIndicator(
              onRefresh: _load,
              child: ListView(
                padding: const EdgeInsets.fromLTRB(10, 8, 10, 30),
                children: [
                  Container(
                    padding: const EdgeInsets.all(20),
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
                            const SizedBox(width: 12),
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
                          '累计已提现 ¥${formatMoney(wallet?.totalWithdrawn ?? 0)}',
                          style: const TextStyle(
                            color: Color(0xE6FFFFFF),
                            fontSize: 12,
                          ),
                        ),
                        const SizedBox(height: 8),
                        Wrap(
                          spacing: 16,
                          runSpacing: 4,
                          children: [
                            Text(
                              '充值余额 ¥${formatMoney(wallet?.rechargeBalance ?? 0)} · 不可提现',
                              style: const TextStyle(
                                color: Color(0xBFFFFFFF),
                                fontSize: 10,
                              ),
                            ),
                            Text(
                              '回答收入 ¥${formatMoney(wallet?.withdrawableIncome ?? 0)} · 可提现',
                              style: const TextStyle(
                                color: Color(0xBFFFFFFF),
                                fontSize: 10,
                              ),
                            ),
                          ],
                        ),
                      ],
                    ),
                  ),
                  const SizedBox(height: 16),
                  _WalletTabs(
                    selected: _tab,
                    onChanged: (value) => setState(() => _tab = value),
                  ),
                  const SizedBox(height: 16),
                  if (_tab == WalletTab.transactions)
                    _TransactionList(items: _transactions),
                  if (_tab == WalletTab.withdrawals)
                    _WithdrawalList(items: _withdrawals),
                  if (_tab == WalletTab.recharge ||
                      _tab == WalletTab.withdraw) ...[
                    _WalletAmountField(
                      controller: _amount,
                      label: _tab == WalletTab.recharge ? '充值金额' : '提现金额',
                      maxAmount: 9999,
                      onChanged: (_) => setState(() {
                        _rechargeRequestId = null;
                        _withdrawalRequestId = null;
                      }),
                    ),
                    if (_tab == WalletTab.withdraw) ...[
                      const SizedBox(height: 7),
                      Text(
                        '只有回答收入可以提现，充值余额不可提现。',
                        style: Theme.of(context).textTheme.bodySmall,
                      ),
                    ],
                    const SizedBox(height: 12),
                    if (_tab == WalletTab.recharge)
                      Container(
                        decoration: BoxDecoration(
                          color: Theme.of(context).colorScheme.surface,
                          borderRadius: BorderRadius.circular(14),
                        ),
                        child: ListTile(
                          contentPadding: const EdgeInsets.symmetric(
                            horizontal: 16,
                          ),
                          leading: Container(
                            width: 42,
                            height: 42,
                            alignment: Alignment.center,
                            decoration: BoxDecoration(
                              color: const Color(
                                0xFF1677FF,
                              ).withValues(alpha: .10),
                              borderRadius: BorderRadius.circular(12),
                            ),
                            child: const FaIcon(
                              FontAwesomeIcons.alipay,
                              color: Color(0xFF1677FF),
                              size: 25,
                            ),
                          ),
                          title: const Text('支付宝'),
                          subtitle: const Text('仅支持支付宝充值'),
                        ),
                      ),
                    if (_tab == WalletTab.withdraw) ...[
                      Container(
                        decoration: BoxDecoration(
                          color: Theme.of(context).colorScheme.surfaceContainer,
                          borderRadius: BorderRadius.circular(14),
                        ),
                        child: ListTile(
                          contentPadding: const EdgeInsets.symmetric(
                            horizontal: 16,
                          ),
                          leading: const FaIcon(FontAwesomeIcons.alipay),
                          title: const Text('支付宝收款账户'),
                          subtitle: Text(
                            _alipayAccount == null
                                ? '授权后用于接收提现'
                                : '${_alipayAccount!.displayName} · ${_alipayAccount!.accountMasked}',
                          ),
                          trailing: TextButton(
                            onPressed: _authorizingAlipay
                                ? null
                                : _authorizeAlipay,
                            child: Text(
                              _authorizingAlipay
                                  ? '授权中'
                                  : _alipayAccount == null
                                  ? '去授权'
                                  : '重新授权',
                            ),
                          ),
                        ),
                      ),
                    ],
                    const SizedBox(height: 16),
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

class _WithdrawalCodeDialog extends StatefulWidget {
  const _WithdrawalCodeDialog();

  @override
  State<_WithdrawalCodeDialog> createState() => _WithdrawalCodeDialogState();
}

class _WithdrawalCodeDialogState extends State<_WithdrawalCodeDialog> {
  final _controller = TextEditingController();

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return AlertDialog(
      title: const Text('确认是本人操作'),
      content: TextField(
        controller: _controller,
        autofocus: true,
        keyboardType: TextInputType.number,
        inputFormatters: [
          FilteringTextInputFormatter.digitsOnly,
          LengthLimitingTextInputFormatter(4),
        ],
        decoration: const InputDecoration(hintText: '请输入短信验证码'),
      ),
      actions: [
        TextButton(
          onPressed: () => Navigator.pop(context),
          child: const Text('取消'),
        ),
        FilledButton(
          onPressed: () {
            if (RegExp(r'^\d{4}$').hasMatch(_controller.text)) {
              Navigator.pop(context, _controller.text);
            }
          },
          child: const Text('确认提现'),
        ),
      ],
    );
  }
}

class _WalletAmountField extends StatelessWidget {
  const _WalletAmountField({
    required this.controller,
    required this.label,
    required this.maxAmount,
    required this.onChanged,
  });

  final TextEditingController controller;
  final String label;
  final int maxAmount;
  final ValueChanged<String> onChanged;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Container(
      padding: const EdgeInsets.fromLTRB(16, 14, 16, 10),
      decoration: BoxDecoration(
        color: theme.colorScheme.surfaceContainerHighest,
        borderRadius: BorderRadius.circular(16),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(label, style: theme.textTheme.labelLarge),
          const SizedBox(height: 4),
          Row(
            crossAxisAlignment: CrossAxisAlignment.center,
            children: [
              Text(
                '¥',
                style: theme.textTheme.headlineLarge?.copyWith(
                  fontWeight: FontWeight.w600,
                ),
              ),
              const SizedBox(width: 9),
              Expanded(
                child: TextField(
                  controller: controller,
                  onChanged: onChanged,
                  keyboardType: TextInputType.number,
                  inputFormatters: AppInputFormatters.positiveInteger(
                    max: maxAmount,
                  ),
                  style: theme.textTheme.displaySmall?.copyWith(
                    fontWeight: FontWeight.w600,
                  ),
                  decoration: InputDecoration(
                    hintText: '0',
                    filled: false,
                    isDense: true,
                    contentPadding: const EdgeInsets.symmetric(vertical: 7),
                    hintStyle: theme.textTheme.displaySmall?.copyWith(
                      color: theme.colorScheme.onSurfaceVariant.withValues(
                        alpha: .55,
                      ),
                      fontWeight: FontWeight.w500,
                    ),
                  ),
                ),
              ),
            ],
          ),
        ],
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
        '¥${formatMoney(value)}',
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
              '${income ? '+' : '-'}¥${formatMoney(item.amount)}',
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
      children: items.map((item) {
        final statusStyle = appStatusStyle(context, item.status);
        return Container(
          margin: const EdgeInsets.only(bottom: 8),
          decoration: BoxDecoration(
            color: Theme.of(context).colorScheme.surface,
            borderRadius: BorderRadius.circular(13),
          ),
          child: ListTile(
            contentPadding: const EdgeInsets.symmetric(horizontal: 12),
            title: Text('提现至支付宝 ${item.alipayAccount}'),
            subtitle: Text(
              '${item.payeeName}  ·  ${item.createdAt == null ? '' : DateFormat('yyyy-MM-dd HH:mm').format(item.createdAt!)}',
            ),
            trailing: Column(
              mainAxisAlignment: MainAxisAlignment.center,
              crossAxisAlignment: CrossAxisAlignment.end,
              children: [
                Text(
                  '¥${formatMoney(item.amount)}',
                  style: const TextStyle(fontWeight: FontWeight.w700),
                ),
                Text(
                  _withdrawalStatusLabel(item.status),
                  style: Theme.of(context).textTheme.bodySmall?.copyWith(
                    color: statusStyle.foreground,
                    fontWeight: FontWeight.w700,
                  ),
                ),
              ],
            ),
          ),
        );
      }).toList(),
    );
  }
}

String _withdrawalStatusLabel(String status) {
  return switch (status.toUpperCase()) {
    'PROCESSING' => '待处理',
    'EXPORTED' => '支付处理中',
    'COMPLETED' => '已到账',
    'FAILED' => '已退回',
    _ => status,
  };
}
