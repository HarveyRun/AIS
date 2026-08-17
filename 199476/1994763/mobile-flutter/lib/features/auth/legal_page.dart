import 'package:flutter/material.dart';

enum LegalType { terms, privacy }

class LegalPage extends StatelessWidget {
  const LegalPage({super.key, required this.type});

  final LegalType type;

  @override
  Widget build(BuildContext context) {
    final terms = type == LegalType.terms;
    return Scaffold(
      appBar: AppBar(title: Text(terms ? '服务协议' : '隐私政策')),
      body: ListView(
        padding: const EdgeInsets.fromLTRB(17, 8, 17, 80),
        children: [
          Text(
            terms ? '事先问服务协议' : '事先问隐私政策',
            style: Theme.of(context).textTheme.headlineMedium,
          ),
          const SizedBox(height: 18),
          Container(
            padding: const EdgeInsets.all(16),
            decoration: BoxDecoration(
              color: Theme.of(context).colorScheme.surfaceContainer,
              borderRadius: BorderRadius.circular(16),
            ),
            child: Text(
              terms
                  ? '欢迎使用事先问。平台帮助用户联系经过认证的从业者和亲历者进行一对一交流。你应如实提供账户和认证材料，文明交流，并遵守平台规则。发起询问后，费用按页面所示规则冻结、退回或结算。平台提供信息与交流条件，不对交流结果作绝对保证。'
                  : '我们仅在提供账户、认证、询问、聊天、支付和安全服务所必需的范围内处理你的信息。手机号用于登录，认证材料用于审核，设备相机仅在你主动拍摄认证材料或发送照片时使用。未经你的授权，我们不会将个人信息用于无关用途。',
              style: Theme.of(context).textTheme.bodyMedium,
            ),
          ),
        ],
      ),
    );
  }
}
