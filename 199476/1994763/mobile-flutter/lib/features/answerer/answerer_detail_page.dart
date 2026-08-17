import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../app/providers.dart';
import '../../core/widgets/app_avatar.dart';
import '../../core/widgets/app_message.dart';
import '../../data/models/answerer_models.dart';

class AnswererDetailPage extends ConsumerStatefulWidget {
  const AnswererDetailPage({super.key, required this.uid});
  final String uid;

  @override
  ConsumerState<AnswererDetailPage> createState() => _AnswererDetailPageState();
}

class _AnswererDetailPageState extends ConsumerState<AnswererDetailPage> {
  Answerer? _answerer;
  bool _loading = true;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    setState(() => _loading = true);
    try {
      final person = await ref.read(repositoryProvider).answerer(widget.uid);
      if (mounted) setState(() => _answerer = person);
    } catch (error) {
      if (mounted) AppMessage.show(context, '$error');
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  Future<void> _ask() async {
    final person = _answerer!;
    if (!person.acceptingInquiries) return;
    try {
      final existing = await ref.read(repositoryProvider).inquiries();
      for (final inquiry in existing) {
        if (!inquiry.isIncoming &&
            inquiry.otherUserId == person.id &&
            const [
              'PENDING',
              'ACTIVE',
              'AWAITING_CONFIRMATION',
              'DISPUTED',
            ].contains(inquiry.status.toUpperCase())) {
          if (mounted) context.push('/chat/${inquiry.id}');
          return;
        }
      }
    } catch (error) {
      if (mounted) AppMessage.show(context, '$error');
      return;
    }
    if (!mounted) return;
    await showModalBottomSheet<void>(
      context: context,
      isScrollControlled: true,
      builder: (context) => _InquirySheet(answerer: person),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('个人档案')),
      body: _loading
          ? const Center(child: CircularProgressIndicator(strokeWidth: 2))
          : _answerer == null
          ? const Center(child: Text('档案不存在'))
          : RefreshIndicator(
              onRefresh: _load,
              child: ListView(
                padding: const EdgeInsets.fromLTRB(17, 8, 17, 92),
                children: [_WebAnswererOverview(answerer: _answerer!)],
              ),
            ),
      bottomNavigationBar: _answerer == null
          ? null
          : SafeArea(
              minimum: const EdgeInsets.fromLTRB(17, 7, 17, 9),
              child: FilledButton.icon(
                onPressed: _answerer!.acceptingInquiries ? _ask : null,
                icon: const Icon(Icons.chat_bubble_outline_rounded),
                label: Text(_answerer!.acceptingInquiries ? '询问' : '暂不接受询问'),
              ),
            ),
    );
  }
}

// Kept temporarily while the Web-matched profile layout is rolled out.
// ignore: unused_element
class _AnswererOverview extends StatelessWidget {
  const _AnswererOverview({required this.answerer});

  final Answerer answerer;

  @override
  Widget build(BuildContext context) {
    return Material(
      color: Theme.of(context).colorScheme.surface,
      borderRadius: BorderRadius.circular(20),
      clipBehavior: Clip.antiAlias,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Padding(
            padding: const EdgeInsets.all(16),
            child: Row(
              children: [
                AppAvatar(
                  url: answerer.avatarUrl,
                  name: answerer.displayName,
                  radius: 30,
                  verified: true,
                ),
                const SizedBox(width: 13),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        answerer.displayName,
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                        style: Theme.of(context).textTheme.titleLarge,
                      ),
                      const SizedBox(height: 4),
                      Text(
                        'UID ${answerer.uid}',
                        style: Theme.of(context).textTheme.bodySmall,
                      ),
                    ],
                  ),
                ),
                Row(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    Icon(
                      Icons.verified_rounded,
                      size: 14,
                      color: Theme.of(context).colorScheme.primary,
                    ),
                    const SizedBox(width: 4),
                    Text(
                      '已核实',
                      style: Theme.of(context).textTheme.bodySmall?.copyWith(
                        color: Theme.of(context).colorScheme.primary,
                      ),
                    ),
                  ],
                ),
              ],
            ),
          ),
          const Divider(height: 1, indent: 16, endIndent: 16),
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
            child: Row(
              children: [
                SizedBox(
                  width: 72,
                  child: Text(
                    '我的岗位',
                    style: Theme.of(context).textTheme.bodySmall,
                  ),
                ),
                Expanded(
                  child: Text(
                    answerer.mainJob,
                    style: Theme.of(context).textTheme.titleMedium,
                  ),
                ),
                Text(
                  '${answerer.mainJobYears}年经验',
                  style: Theme.of(context).textTheme.bodySmall,
                ),
              ],
            ),
          ),
          const Divider(height: 1, indent: 16, endIndent: 16),
          Padding(
            padding: const EdgeInsets.fromLTRB(16, 14, 16, 16),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text('亲身经历', style: Theme.of(context).textTheme.bodySmall),
                const SizedBox(height: 10),
                if (answerer.experiences.isEmpty)
                  Text(
                    '暂未填写亲身经历',
                    style: Theme.of(context).textTheme.bodyMedium,
                  )
                else
                  ...answerer.experiences.map(
                    (item) => Padding(
                      padding: const EdgeInsets.only(bottom: 9),
                      child: Row(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Padding(
                            padding: const EdgeInsets.only(top: 7),
                            child: Container(
                              width: 5,
                              height: 5,
                              decoration: BoxDecoration(
                                color: Theme.of(context).colorScheme.primary,
                                shape: BoxShape.circle,
                              ),
                            ),
                          ),
                          const SizedBox(width: 9),
                          Expanded(
                            child: Text(
                              item.title,
                              style: Theme.of(context).textTheme.bodyMedium,
                            ),
                          ),
                        ],
                      ),
                    ),
                  ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

class _WebAnswererOverview extends StatelessWidget {
  const _WebAnswererOverview({required this.answerer});

  final Answerer answerer;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        Padding(
          padding: const EdgeInsets.fromLTRB(8, 16, 8, 22),
          child: Column(
            children: [
              AppAvatar(
                url: answerer.avatarUrl,
                name: answerer.displayName,
                radius: 36,
                verified: true,
              ),
              const SizedBox(height: 12),
              Text(
                answerer.displayName,
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
                style: theme.textTheme.headlineMedium,
              ),
              const SizedBox(height: 4),
              Text(
                'UID ${answerer.uid} · 信息已经核实',
                style: theme.textTheme.bodySmall,
              ),
            ],
          ),
        ),
        _ProfileFactBox(
          title: '做过的工作',
          icon: Icons.verified_user_outlined,
          child: Row(
            children: [
              SizedBox(
                width: 60,
                child: Text(
                  '主职',
                  style: theme.textTheme.bodySmall?.copyWith(
                    color: theme.colorScheme.primary,
                  ),
                ),
              ),
              Expanded(
                child: Text(
                  answerer.mainJob,
                  style: theme.textTheme.bodyMedium?.copyWith(
                    fontWeight: FontWeight.w700,
                  ),
                ),
              ),
              Text(
                '${answerer.mainJobYears}年经验',
                style: theme.textTheme.bodySmall,
              ),
            ],
          ),
        ),
        const SizedBox(height: 12),
        _ProfileFactBox(
          title: '亲身经历过的事',
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Wrap(
                spacing: 6,
                runSpacing: 6,
                children: answerer.experiences.isEmpty
                    ? const [_ExperienceLabel(text: '暂未填写亲身经历')]
                    : answerer.experiences
                          .map((item) => _ExperienceLabel(text: item.title))
                          .toList(),
              ),
              const SizedBox(height: 10),
              Text('这些经历的基础材料已经核实。', style: theme.textTheme.bodySmall),
            ],
          ),
        ),
      ],
    );
  }
}

class _ProfileFactBox extends StatelessWidget {
  const _ProfileFactBox({required this.title, required this.child, this.icon});

  final String title;
  final Widget child;
  final IconData? icon;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(18),
      decoration: BoxDecoration(
        color: Theme.of(context).colorScheme.surface,
        border: Border.all(color: Theme.of(context).colorScheme.outlineVariant),
        borderRadius: BorderRadius.circular(19),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              if (icon != null) ...[
                Icon(icon, size: 19, color: const Color(0xFF639579)),
                const SizedBox(width: 7),
              ],
              Text(
                title,
                style: Theme.of(
                  context,
                ).textTheme.titleMedium?.copyWith(fontWeight: FontWeight.w700),
              ),
            ],
          ),
          const SizedBox(height: 14),
          child,
        ],
      ),
    );
  }
}

class _ExperienceLabel extends StatelessWidget {
  const _ExperienceLabel({required this.text});

  final String text;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 9, vertical: 7),
      decoration: BoxDecoration(
        color: const Color(0xFFF6F1EB),
        borderRadius: BorderRadius.circular(8),
      ),
      child: Text(text, style: Theme.of(context).textTheme.bodySmall),
    );
  }
}

class _InquirySheet extends ConsumerStatefulWidget {
  const _InquirySheet({required this.answerer});
  final Answerer answerer;
  @override
  ConsumerState<_InquirySheet> createState() => _InquirySheetState();
}

class _InquirySheetState extends ConsumerState<_InquirySheet> {
  final _question = TextEditingController();
  final _amount = TextEditingController();
  bool _submitting = false;

  @override
  void dispose() {
    _question.dispose();
    _amount.dispose();
    super.dispose();
  }

  Future<void> _submit() async {
    final question = _question.text.trim();
    final amount = double.tryParse(_amount.text);
    if (question.isEmpty) {
      AppMessage.show(context, '请先写下你想问的事情');
      return;
    }
    if (amount == null || amount <= 0) {
      AppMessage.show(context, '请输入有效金额');
      return;
    }
    setState(() => _submitting = true);
    try {
      final created = await ref
          .read(repositoryProvider)
          .createInquiry(
            answererId: widget.answerer.id,
            topic: widget.answerer.mainJob,
            sourceType: 'PROFILE',
            question: question,
            amount: double.parse(amount.toStringAsFixed(2)),
          );
      if (!mounted) return;
      Navigator.pop(context);
      context.push('/chat/${created.id}');
    } catch (error) {
      if (mounted) AppMessage.show(context, '$error');
    } finally {
      if (mounted) setState(() => _submitting = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: EdgeInsets.fromLTRB(
        20,
        20,
        20,
        MediaQuery.viewInsetsOf(context).bottom + 22,
      ),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text('发起询问', style: Theme.of(context).textTheme.titleLarge),
                    const SizedBox(height: 4),
                    Text(
                      '对方接受后即可开始私聊',
                      style: Theme.of(context).textTheme.bodySmall,
                    ),
                  ],
                ),
              ),
              IconButton(
                onPressed: () => Navigator.pop(context),
                icon: const Icon(Icons.close_rounded),
              ),
            ],
          ),
          const SizedBox(height: 18),
          TextField(
            controller: _question,
            autofocus: true,
            maxLength: 120,
            maxLines: 3,
            decoration: const InputDecoration(
              labelText: '你想问什么',
              hintText: '把想了解的事情简单说清楚',
            ),
          ),
          const SizedBox(height: 10),
          TextField(
            controller: _amount,
            keyboardType: const TextInputType.numberWithOptions(decimal: true),
            inputFormatters: [
              FilteringTextInputFormatter.allow(RegExp(r'^\d*\.?\d{0,2}')),
            ],
            decoration: const InputDecoration(
              labelText: '你打算给多少钱',
              prefixText: '¥ ',
            ),
          ),
          const SizedBox(height: 8),
          Text(
            '发起后金额暂时冻结，对方未接受会自动退回。',
            style: Theme.of(context).textTheme.bodySmall,
          ),
          const SizedBox(height: 18),
          FilledButton(
            onPressed: _submitting ? null : _submit,
            child: _submitting
                ? const SizedBox.square(
                    dimension: 18,
                    child: CircularProgressIndicator(
                      strokeWidth: 2,
                      color: Colors.white,
                    ),
                  )
                : const Text('确认发起'),
          ),
        ],
      ),
    );
  }
}
