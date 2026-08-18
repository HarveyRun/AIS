import 'package:flutter/gestures.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../../core/legal/legal_links.dart';
import '../../core/widgets/app_message.dart';

class PrePrivacyPage extends StatefulWidget {
  const PrePrivacyPage({required this.onAgree, super.key});

  final Future<void> Function() onAgree;

  @override
  State<PrePrivacyPage> createState() => _PrePrivacyPageState();
}

class _PrePrivacyPageState extends State<PrePrivacyPage> {
  late final TapGestureRecognizer _privacyRecognizer;

  @override
  void initState() {
    super.initState();
    _privacyRecognizer = TapGestureRecognizer()..onTap = _openPrivacyPolicy;
  }

  @override
  void dispose() {
    _privacyRecognizer.dispose();
    super.dispose();
  }

  Future<void> _openPrivacyPolicy() async {
    final opened = await LegalLinks.openPrivacyPolicy();
    if (!opened && mounted) {
      AppMessage.show(context, '暂时无法打开隐私政策');
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.white,
      body: Stack(
        fit: StackFit.expand,
        children: [
          Image.asset('assets/images/privacy-intro.png', fit: BoxFit.cover),
          const ColoredBox(color: Color(0x66000000)),
          SafeArea(
            child: Center(
              child: LayoutBuilder(
                builder: (context, constraints) {
                  return Container(
                    width: constraints.maxWidth * .90,
                    constraints: BoxConstraints(
                      maxHeight: constraints.maxHeight - 30,
                    ),
                    padding: const EdgeInsets.fromLTRB(15, 20, 15, 10),
                    decoration: BoxDecoration(
                      color: Colors.white,
                      borderRadius: BorderRadius.circular(16),
                    ),
                    child: Column(
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        const Text(
                          '隐私保护提示',
                          style: TextStyle(
                            color: Colors.black,
                            fontSize: 16,
                            fontWeight: FontWeight.w600,
                          ),
                        ),
                        const SizedBox(height: 16),
                        const SizedBox(
                          height: 300,
                          child: SingleChildScrollView(
                            child: Text.rich(
                              TextSpan(
                                text:
                                    '欢迎来到光忆！  为了更好地保护您的权益，在此为您介绍在服务的过程中我们将如何规范安全地收集、存储、保护、使用及对外提供您的信息，请您充分了解：',
                                children: [
                                  TextSpan(text: '\n\n'),
                                  TextSpan(
                                    text:
                                        '为向您提供包括生活服务和网络支付在内的基本功能，光忆会基于具体业务场景收集您的必要个人信息。为提供生活服务，光忆需要收集您的移动电话号码。为提供网络支付服务，光忆为采取风险防范措施保障您的账户及资金安全，并依法履行实名制管理、反洗钱等法定义务，需要在必要范围内收集您的身份基本信息、账户信息、交易信息以及设备信息。',
                                  ),
                                  TextSpan(text: '\n\n'),
                                  TextSpan(
                                    text:
                                        '此外，为保障 APP稳定运行或提供服务，光忆APP需向您申请必要的设备权限以及隐私权政策中所列举的其他使用具体功能时所需申请的权限类型。当您开启权限后，光忆APP才会收集必要的信息。',
                                  ),
                                ],
                              ),
                              style: TextStyle(
                                color: Color(0xFF403D3D),
                                fontSize: 13,
                                height: 1.75,
                              ),
                            ),
                          ),
                        ),
                        const SizedBox(height: 30),
                        Align(
                          alignment: Alignment.centerLeft,
                          child: Text.rich(
                            TextSpan(
                              text: '更多详情，敬请查阅',
                              children: [
                                TextSpan(
                                  text: '《客户端隐私权政策》',
                                  style: const TextStyle(
                                    color: Color(0xFFD33A2C),
                                  ),
                                  recognizer: _privacyRecognizer,
                                ),
                                const TextSpan(
                                  text: '全文。我们承诺:将以业界领先的个人信息安全保护水平，全力守护您的信息安全!',
                                ),
                              ],
                            ),
                            style: const TextStyle(
                              color: Colors.grey,
                              fontSize: 12,
                              height: 1.55,
                            ),
                          ),
                        ),
                        const SizedBox(height: 8),
                        SizedBox(
                          width: double.infinity,
                          height: 40,
                          child: FilledButton(
                            style: FilledButton.styleFrom(
                              backgroundColor: const Color(0xFFD33A2C),
                              foregroundColor: Colors.white,
                              shape: RoundedRectangleBorder(
                                borderRadius: BorderRadius.circular(10),
                              ),
                            ),
                            onPressed: widget.onAgree,
                            child: const Text(
                              '同意',
                              style: TextStyle(fontSize: 15),
                            ),
                          ),
                        ),
                        const SizedBox(height: 8),
                        TextButton(
                          style: TextButton.styleFrom(
                            foregroundColor: Colors.grey,
                            minimumSize: const Size(80, 36),
                            padding: EdgeInsets.zero,
                          ),
                          onPressed: SystemNavigator.pop,
                          child: const Text(
                            '不同意',
                            style: TextStyle(fontSize: 13),
                          ),
                        ),
                      ],
                    ),
                  );
                },
              ),
            ),
          ),
        ],
      ),
    );
  }
}
