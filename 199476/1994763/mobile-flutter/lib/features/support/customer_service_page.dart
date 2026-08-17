import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:intl/intl.dart';

import '../../app/providers.dart';
import '../../core/network/realtime_service.dart';
import '../../core/widgets/app_message.dart';
import '../../data/models/support_models.dart';

class CustomerServicePage extends ConsumerStatefulWidget {
  const CustomerServicePage({super.key});
  @override
  ConsumerState<CustomerServicePage> createState() =>
      _CustomerServicePageState();
}

class _CustomerServicePageState extends ConsumerState<CustomerServicePage> {
  final _controller = TextEditingController();
  final _scroll = ScrollController();
  StreamSubscription<RealtimeEvent>? _subscription;
  List<CustomerServiceMessage> _messages = const [];
  bool _loading = true;
  bool _sending = false;

  @override
  void initState() {
    super.initState();
    _load();
    _subscription = ref.read(realtimeProvider).events.listen((event) {
      if (event.type == 'CUSTOMER_SERVICE_MESSAGE') _load(silent: true);
    });
  }

  @override
  void dispose() {
    _subscription?.cancel();
    _controller.dispose();
    _scroll.dispose();
    super.dispose();
  }

  Future<void> _load({bool silent = false}) async {
    if (!silent) setState(() => _loading = true);
    try {
      final messages = await ref
          .read(repositoryProvider)
          .customerServiceMessages();
      await ref.read(repositoryProvider).readCustomerServiceMessages();
      if (!mounted) return;
      setState(() => _messages = messages);
      ref.read(customerServiceUnreadProvider.notifier).state = 0;
      WidgetsBinding.instance.addPostFrameCallback((_) {
        if (_scroll.hasClients) {
          _scroll.jumpTo(_scroll.position.maxScrollExtent);
        }
      });
    } catch (error) {
      if (!silent && mounted) AppMessage.show(context, '$error');
    } finally {
      if (mounted && !silent) setState(() => _loading = false);
    }
  }

  Future<void> _send() async {
    final content = _controller.text.trim();
    if (content.isEmpty || _sending) return;
    setState(() => _sending = true);
    _controller.clear();
    try {
      final saved = await ref
          .read(repositoryProvider)
          .sendCustomerServiceMessage(content);
      if (mounted) setState(() => _messages = [..._messages, saved]);
    } catch (error) {
      if (mounted) AppMessage.show(context, '$error');
    } finally {
      if (mounted) setState(() => _sending = false);
    }
  }

  @override
  Widget build(BuildContext context) => Scaffold(
    appBar: AppBar(title: const Text('在线客服')),
    body: Column(
      children: [
        Expanded(
          child: _loading
              ? const Center(child: CircularProgressIndicator(strokeWidth: 2))
              : Container(
                  color: Theme.of(context).brightness == Brightness.dark
                      ? const Color(0xFF151619)
                      : const Color(0xFFF2F2F2),
                  child: ListView.builder(
                    controller: _scroll,
                    padding: const EdgeInsets.all(16),
                    itemCount: _messages.length,
                    itemBuilder: (context, index) {
                      final item = _messages[index];
                      return Align(
                        alignment: item.isMine
                            ? Alignment.centerRight
                            : Alignment.centerLeft,
                        child: Padding(
                          padding: const EdgeInsets.only(bottom: 14),
                          child: Column(
                            crossAxisAlignment: item.isMine
                                ? CrossAxisAlignment.end
                                : CrossAxisAlignment.start,
                            children: [
                              Text(
                                item.isMine ? '我' : '客服',
                                style: Theme.of(context).textTheme.bodySmall,
                              ),
                              const SizedBox(height: 4),
                              Container(
                                constraints: BoxConstraints(
                                  maxWidth:
                                      MediaQuery.sizeOf(context).width * .72,
                                ),
                                padding: const EdgeInsets.symmetric(
                                  horizontal: 13,
                                  vertical: 10,
                                ),
                                decoration: BoxDecoration(
                                  color: item.isMine
                                      ? const Color(0xFFDFE9F2)
                                      : Theme.of(context).colorScheme.surface,
                                  borderRadius: BorderRadius.circular(13),
                                ),
                                child: Text(item.content),
                              ),
                              if (item.createdAt != null) ...[
                                const SizedBox(height: 3),
                                Text(
                                  DateFormat(
                                    'M/d HH:mm',
                                  ).format(item.createdAt!),
                                  style: Theme.of(context).textTheme.bodySmall,
                                ),
                              ],
                            ],
                          ),
                        ),
                      );
                    },
                  ),
                ),
        ),
        SafeArea(
          top: false,
          child: Padding(
            padding: const EdgeInsets.fromLTRB(12, 8, 12, 8),
            child: Row(
              children: [
                Expanded(
                  child: TextField(
                    controller: _controller,
                    textInputAction: TextInputAction.send,
                    onSubmitted: (_) => _send(),
                    decoration: const InputDecoration(hintText: '输入消息'),
                  ),
                ),
                const SizedBox(width: 8),
                IconButton.filled(
                  onPressed: _sending ? null : _send,
                  icon: const Icon(Icons.send_rounded),
                ),
              ],
            ),
          ),
        ),
      ],
    ),
  );
}
