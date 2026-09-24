import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../app/providers.dart';
import 'app_message.dart';

Future<void> showPlatformIntroductionDialog(BuildContext context) {
  return showDialog<void>(
    context: context,
    barrierDismissible: false,
    builder: (dialogContext) => SafeArea(
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 18),
        child: Center(
          child: _IntroductionPanel(
            seconds: 0,
            submitting: false,
            onAcknowledge: () => Navigator.of(dialogContext).pop(),
          ),
        ),
      ),
    ),
  );
}

class PlatformIntroductionGate extends ConsumerStatefulWidget {
  const PlatformIntroductionGate({required this.child, super.key});

  final Widget child;

  @override
  ConsumerState<PlatformIntroductionGate> createState() =>
      _PlatformIntroductionGateState();
}

class _PlatformIntroductionGateState
    extends ConsumerState<PlatformIntroductionGate> {
  Timer? _timer;
  String? _shownForUid;
  bool _visible = false;
  bool _submitting = false;
  bool _confirmingDeletion = false;
  int _seconds = 30;

  @override
  void dispose() {
    _timer?.cancel();
    super.dispose();
  }

  void _syncPrompt() {
    final user = ref.read(authControllerProvider).user;
    if (user == null) {
      _timer?.cancel();
      _shownForUid = null;
      if (_visible && mounted) {
        setState(() => _visible = false);
      }
      return;
    }
    if (!user.platformIntroductionRequired ||
        _shownForUid == user.uid ||
        _visible) {
      return;
    }
    _shownForUid = user.uid;
    _timer?.cancel();
    setState(() {
      _visible = true;
      _confirmingDeletion = false;
      _seconds = 30;
    });
    _timer = Timer.periodic(const Duration(seconds: 1), (timer) {
      if (!mounted) return;
      if (_seconds <= 1) {
        timer.cancel();
        setState(() => _seconds = 0);
      } else {
        setState(() => _seconds--);
      }
    });
  }

  Future<void> _acknowledge() async {
    if (_seconds > 0 || _submitting) return;
    setState(() => _submitting = true);
    try {
      await ref.read(authControllerProvider).dismissPlatformIntroduction();
      if (mounted) setState(() => _visible = false);
    } catch (error) {
      if (mounted) AppMessage.show(context, '$error');
    } finally {
      if (mounted) setState(() => _submitting = false);
    }
  }

  Future<void> _deleteAccount() async {
    if (_submitting) return;
    setState(() => _submitting = true);
    try {
      await ref.read(authControllerProvider).deleteAccount();
    } catch (error) {
      if (mounted) AppMessage.show(context, '$error');
    } finally {
      if (mounted) {
        setState(() {
          _submitting = false;
          _confirmingDeletion = false;
        });
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    ref.watch(authControllerProvider);
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (mounted) _syncPrompt();
    });

    return PopScope(
      canPop: !_visible,
      child: Stack(
        children: [
          widget.child,
          if (_visible) ...[
            const Positioned.fill(
              child: ModalBarrier(dismissible: false, color: Color(0x73000000)),
            ),
            Positioned.fill(
              child: SafeArea(
                child: Padding(
                  padding: const EdgeInsets.symmetric(
                    horizontal: 16,
                    vertical: 18,
                  ),
                  child: Center(
                    child: _confirmingDeletion
                        ? _DeleteConfirmation(
                            submitting: _submitting,
                            onCancel: () =>
                                setState(() => _confirmingDeletion = false),
                            onConfirm: _deleteAccount,
                          )
                        : _IntroductionPanel(
                            seconds: _seconds,
                            submitting: _submitting,
                            onAcknowledge: _acknowledge,
                            onDeleteAccount: () {
                              setState(() => _confirmingDeletion = true);
                            },
                          ),
                  ),
                ),
              ),
            ),
          ],
        ],
      ),
    );
  }
}

class _IntroductionPanel extends StatelessWidget {
  const _IntroductionPanel({
    required this.seconds,
    required this.submitting,
    required this.onAcknowledge,
    this.onDeleteAccount,
  });

  final int seconds;
  final bool submitting;
  final VoidCallback onAcknowledge;
  final VoidCallback? onDeleteAccount;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Material(
      color: theme.colorScheme.surface,
      borderRadius: BorderRadius.circular(22),
      clipBehavior: Clip.antiAlias,
      child: ConstrainedBox(
        constraints: BoxConstraints(
          maxWidth: 420,
          maxHeight: MediaQuery.sizeOf(context).height * .82,
        ),
        child: Padding(
          padding: const EdgeInsets.fromLTRB(22, 24, 22, 16),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              Text(
                '我们的初衷',
                style: theme.textTheme.titleLarge?.copyWith(
                  fontWeight: FontWeight.w800,
                ),
              ),
              const SizedBox(height: 18),
              const Flexible(
                child: SingleChildScrollView(child: _IntroductionContent()),
              ),
              const SizedBox(height: 18),
              if (seconds > 0) ...[
                AnimatedSwitcher(
                  duration: const Duration(milliseconds: 180),
                  child: Text(
                    '$seconds 秒后可关闭',
                    key: ValueKey(seconds),
                    style: theme.textTheme.bodySmall?.copyWith(
                      color: theme.colorScheme.onSurfaceVariant,
                    ),
                  ),
                ),
                const SizedBox(height: 8),
              ],
              SizedBox(
                width: double.infinity,
                child: FilledButton(
                  onPressed: seconds == 0 && !submitting ? onAcknowledge : null,
                  child: Text(submitting ? '请稍候…' : '知道了'),
                ),
              ),
              if (onDeleteAccount != null)
                TextButton(
                  onPressed: submitting ? null : onDeleteAccount,
                  child: const Text('注销账号'),
                ),
            ],
          ),
        ),
      ),
    );
  }
}

class _IntroductionContent extends StatelessWidget {
  const _IntroductionContent();

  @override
  Widget build(BuildContext context) {
    final style = Theme.of(context).textTheme.bodyMedium?.copyWith(height: 1.7);
    final strong = style?.copyWith(fontWeight: FontWeight.w800);
    return Text.rich(
      TextSpan(
        style: style,
        children: [
          const TextSpan(text: '我们相信，每个人一路走来，都'),
          TextSpan(text: '在自己的生活、工作和事业中积累了一些属于自己的能力和经验', style: strong),
          const TextSpan(
            text:
                '，也走过一些只有亲身经历过才真正明白的路；这些对自己来说也许早已习以为常，但当他人恰好遇到同样的问题时，就可能变得很有价值。\n\n我们想做的，就是',
          ),
          TextSpan(text: '让这些能力、经验或经历', style: strong),
          const TextSpan(text: '，在他人迷茫无助、身边又无人可问时，'),
          TextSpan(text: '能帮上一把', style: strong),
        ],
      ),
    );
  }
}

class _DeleteConfirmation extends StatelessWidget {
  const _DeleteConfirmation({
    required this.submitting,
    required this.onCancel,
    required this.onConfirm,
  });

  final bool submitting;
  final VoidCallback onCancel;
  final VoidCallback onConfirm;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Material(
      color: theme.colorScheme.surface,
      borderRadius: BorderRadius.circular(20),
      child: SizedBox(
        width: 330,
        child: Padding(
          padding: const EdgeInsets.fromLTRB(22, 24, 22, 18),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              Text(
                '注销账号？',
                style: theme.textTheme.titleLarge?.copyWith(
                  fontWeight: FontWeight.w800,
                ),
              ),
              const SizedBox(height: 12),
              Text(
                '注销后将无法继续使用当前账号，且不能恢复。',
                style: theme.textTheme.bodyMedium,
                textAlign: TextAlign.center,
              ),
              const SizedBox(height: 22),
              Row(
                children: [
                  Expanded(
                    child: OutlinedButton(
                      onPressed: submitting ? null : onCancel,
                      child: const Text('暂不注销'),
                    ),
                  ),
                  const SizedBox(width: 10),
                  Expanded(
                    child: FilledButton(
                      onPressed: submitting ? null : onConfirm,
                      child: Text(submitting ? '处理中…' : '确认注销'),
                    ),
                  ),
                ],
              ),
            ],
          ),
        ),
      ),
    );
  }
}
