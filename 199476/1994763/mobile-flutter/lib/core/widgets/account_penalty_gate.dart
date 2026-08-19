import 'package:flutter/cupertino.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../app/providers.dart';
import '../network/api_client.dart';

class AccountPenaltyGate extends ConsumerWidget {
  const AccountPenaltyGate({super.key, required this.child});

  final Widget child;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final auth = ref.watch(authControllerProvider);
    final notice = auth.penaltyNotice;

    return PopScope(
      canPop: notice == null,
      child: Stack(
        children: [
          child,
          if (notice != null) ...[
            const Positioned.fill(child: ColoredBox(color: Color(0x73000000))),
            Positioned.fill(
              child: SafeArea(
                child: Center(
                  child: CupertinoTheme(
                    data: const CupertinoThemeData(
                      brightness: Brightness.light,
                    ),
                    child: CupertinoAlertDialog(
                      title: const Text(
                        '账号处罚通知',
                        style: TextStyle(
                          color: CupertinoColors.black,
                          fontSize: 16,
                          fontWeight: FontWeight.bold,
                        ),
                      ),
                      content: Padding(
                        padding: const EdgeInsets.only(top: 12),
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text(
                              _durationText(notice),
                              style: const TextStyle(
                                color: CupertinoColors.destructiveRed,
                                fontSize: 14,
                                fontWeight: FontWeight.w600,
                              ),
                            ),
                            const SizedBox(height: 9),
                            Text(
                              '处罚原因：${notice.reason}',
                              style: const TextStyle(
                                color: CupertinoColors.black,
                                fontSize: 14,
                                height: 1.5,
                              ),
                              textAlign: TextAlign.start,
                            ),
                          ],
                        ),
                      ),
                      actions: [
                        CupertinoDialogAction(
                          isDefaultAction: true,
                          onPressed: auth.acknowledgePenalty,
                          child: const Text(
                            '我知道了',
                            style: TextStyle(
                              color: CupertinoColors.destructiveRed,
                            ),
                          ),
                        ),
                      ],
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

  String _durationText(AccountPenaltyNotice notice) {
    if (notice.permanent || notice.banUntil == null) {
      return '该账号已被永久封禁';
    }
    final value = notice.banUntil!.toLocal();
    String two(int number) => number.toString().padLeft(2, '0');
    return '封禁至 ${value.year}-${two(value.month)}-${two(value.day)} '
        '${two(value.hour)}:${two(value.minute)}';
  }
}
