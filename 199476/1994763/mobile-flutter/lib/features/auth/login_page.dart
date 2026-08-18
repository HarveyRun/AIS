import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../app/providers.dart';
import '../../core/widgets/app_message.dart';

class LoginPage extends ConsumerStatefulWidget {
  const LoginPage({super.key});

  @override
  ConsumerState<LoginPage> createState() => _LoginPageState();
}

class _LoginPageState extends ConsumerState<LoginPage> {
  final _phoneController = TextEditingController();
  final _codeController = TextEditingController();
  final _phoneFocus = FocusNode();
  final _codeFocus = FocusNode();
  bool _codeStep = false;
  bool _agreed = false;
  bool _sending = false;
  int _countdown = 0;
  Timer? _timer;

  String get _phone => _phoneController.text.replaceAll(' ', '');

  @override
  void dispose() {
    _timer?.cancel();
    _phoneController.dispose();
    _codeController.dispose();
    _phoneFocus.dispose();
    _codeFocus.dispose();
    super.dispose();
  }

  void _startCountdown() {
    _timer?.cancel();
    setState(() => _countdown = 60);
    _timer = Timer.periodic(const Duration(seconds: 1), (timer) {
      if (!mounted) return;
      if (_countdown <= 1) {
        timer.cancel();
        setState(() => _countdown = 0);
      } else {
        setState(() => _countdown--);
      }
    });
  }

  Future<void> _sendCode() async {
    if (!RegExp(r'^1\d{10}$').hasMatch(_phone)) {
      AppMessage.show(context, '请输入正确的手机号');
      return;
    }
    if (!_agreed) {
      AppMessage.show(context, '请先阅读并同意服务协议和隐私政策');
      return;
    }
    setState(() => _sending = true);
    try {
      await ref.read(authControllerProvider).sendCode(_phone);
      if (!mounted) return;
      setState(() => _codeStep = true);
      _startCountdown();
      WidgetsBinding.instance.addPostFrameCallback(
        (_) => _codeFocus.requestFocus(),
      );
    } catch (error) {
      if (mounted) AppMessage.show(context, '$error');
    } finally {
      if (mounted) setState(() => _sending = false);
    }
  }

  Future<void> _login() async {
    if (_codeController.text.length != 4) return;
    if (!_agreed) {
      AppMessage.show(context, '请先阅读并同意服务协议和隐私政策');
      return;
    }
    try {
      await ref
          .read(authControllerProvider)
          .login(_phone, _codeController.text);
    } catch (error) {
      if (mounted) AppMessage.show(context, '$error');
    }
  }

  @override
  Widget build(BuildContext context) {
    final readyPhone = RegExp(r'^1\d{10}$').hasMatch(_phone);
    final dark = Theme.of(context).brightness == Brightness.dark;
    return Scaffold(
      backgroundColor: Theme.of(context).brightness == Brightness.dark
          ? const Color(0xFF111214)
          : Colors.white,
      resizeToAvoidBottomInset: true,
      body: SafeArea(
        child: _codeStep
            ? _buildCodeStep(context)
            : LayoutBuilder(
                builder: (context, constraints) => SingleChildScrollView(
                  padding: const EdgeInsets.symmetric(horizontal: 10),
                  child: ConstrainedBox(
                    constraints: BoxConstraints(
                      minHeight: constraints.maxHeight,
                    ),
                    child: IntrinsicHeight(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          const SizedBox(height: 110),
                          Center(
                            child: Text(
                              '事先问',
                              style: Theme.of(context).textTheme.headlineLarge
                                  ?.copyWith(
                                    color: Theme.of(
                                      context,
                                    ).colorScheme.primary,
                                    fontSize: 36,
                                    fontWeight: FontWeight.w800,
                                    letterSpacing: 3,
                                  ),
                            ),
                          ),
                          const SizedBox(height: 52),
                          SizedBox(
                            height: 58,
                            child: TextField(
                              controller: _phoneController,
                              focusNode: _phoneFocus,
                              keyboardType: TextInputType.phone,
                              textInputAction: TextInputAction.done,
                              inputFormatters: const [_PhoneFormatter()],
                              onChanged: (_) => setState(() {}),
                              onSubmitted: (_) =>
                                  readyPhone ? _sendCode() : null,
                              style: const TextStyle(fontSize: 16),
                              decoration: InputDecoration(
                                filled: true,
                                fillColor: dark
                                    ? Theme.of(
                                        context,
                                      ).colorScheme.surfaceContainerHigh
                                    : const Color(0xFFF6F6F6),
                                contentPadding: const EdgeInsets.symmetric(
                                  horizontal: 22,
                                ),
                                prefixIcon: Padding(
                                  padding: const EdgeInsets.only(
                                    left: 22,
                                    right: 12,
                                  ),
                                  child: Center(
                                    widthFactor: 1,
                                    child: Text(
                                      '+86',
                                      style: TextStyle(
                                        fontSize: 14,
                                        color: Theme.of(
                                          context,
                                        ).colorScheme.onSurfaceVariant,
                                      ),
                                    ),
                                  ),
                                ),
                                prefixIconConstraints: const BoxConstraints(
                                  minWidth: 0,
                                  minHeight: 0,
                                ),
                                hintText: '请输入手机号',
                                hintStyle: TextStyle(
                                  color: Theme.of(
                                    context,
                                  ).colorScheme.onSurfaceVariant,
                                ),
                                border: OutlineInputBorder(
                                  borderRadius: BorderRadius.circular(29),
                                  borderSide: BorderSide.none,
                                ),
                                enabledBorder: OutlineInputBorder(
                                  borderRadius: BorderRadius.circular(29),
                                  borderSide: BorderSide.none,
                                ),
                                focusedBorder: OutlineInputBorder(
                                  borderRadius: BorderRadius.circular(29),
                                  borderSide: BorderSide.none,
                                ),
                              ),
                            ),
                          ),
                          const SizedBox(height: 13),
                          Text(
                            '未注册的手机号验证后自动创建账户',
                            style: Theme.of(context).textTheme.bodyMedium
                                ?.copyWith(
                                  fontSize: 12,
                                  color: Theme.of(
                                    context,
                                  ).colorScheme.onSurfaceVariant,
                                ),
                          ),
                          const SizedBox(height: 40),
                          Center(
                            child: SizedBox(
                              width: 64,
                              height: 64,
                              child: FilledButton(
                                style: FilledButton.styleFrom(
                                  padding: EdgeInsets.zero,
                                  shape: const CircleBorder(),
                                ),
                                onPressed: readyPhone && !_sending
                                    ? _sendCode
                                    : null,
                                child: _sending
                                    ? const SizedBox.square(
                                        dimension: 17,
                                        child: CircularProgressIndicator(
                                          strokeWidth: 2,
                                          color: Colors.white,
                                        ),
                                      )
                                    : const Icon(
                                        Icons.arrow_forward_rounded,
                                        size: 27,
                                      ),
                              ),
                            ),
                          ),
                          const Spacer(),
                          _Agreement(
                            checked: _agreed,
                            onChanged: (value) =>
                                setState(() => _agreed = value),
                          ),
                          const SizedBox(height: 28),
                        ],
                      ),
                    ),
                  ),
                ),
              ),
      ),
    );
  }

  Widget _buildCodeStep(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 10),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          IconButton(
            padding: EdgeInsets.zero,
            alignment: Alignment.centerLeft,
            onPressed: () => setState(() => _codeStep = false),
            icon: const Icon(Icons.arrow_back_rounded),
          ),
          const SizedBox(height: 52),
          Text(
            '输入验证码',
            style: Theme.of(context).textTheme.headlineLarge?.copyWith(
              fontSize: 26,
              fontWeight: FontWeight.w500,
            ),
          ),
          const SizedBox(height: 18),
          Text(
            '验证码已发送至 +86 ${_phoneController.text}',
            style: Theme.of(context).textTheme.bodyLarge,
          ),
          const SizedBox(height: 52),
          Stack(
            children: [
              Positioned.fill(
                child: Opacity(
                  opacity: 0,
                  child: TextField(
                    controller: _codeController,
                    focusNode: _codeFocus,
                    keyboardType: TextInputType.number,
                    enableInteractiveSelection: false,
                    cursorColor: Colors.transparent,
                    decoration: const InputDecoration.collapsed(hintText: ''),
                    inputFormatters: [
                      FilteringTextInputFormatter.digitsOnly,
                      LengthLimitingTextInputFormatter(4),
                    ],
                    onChanged: (_) {
                      setState(() {});
                      if (_codeController.text.length == 4) _login();
                    },
                  ),
                ),
              ),
              GestureDetector(
                behavior: HitTestBehavior.opaque,
                onTap: _codeFocus.requestFocus,
                child: Row(
                  children: List.generate(4, (index) {
                    final value = index < _codeController.text.length
                        ? _codeController.text[index]
                        : '';
                    return Expanded(
                      child: Container(
                        height: 44,
                        margin: EdgeInsets.only(right: index == 3 ? 0 : 28),
                        alignment: Alignment.center,
                        decoration: BoxDecoration(
                          border: Border(
                            bottom: BorderSide(
                              color: index == _codeController.text.length
                                  ? Theme.of(context).colorScheme.primary
                                  : Theme.of(
                                      context,
                                    ).colorScheme.outlineVariant,
                              width: 2,
                            ),
                          ),
                        ),
                        child: Text(
                          value,
                          style: Theme.of(context).textTheme.headlineMedium,
                        ),
                      ),
                    );
                  }),
                ),
              ),
            ],
          ),
          const SizedBox(height: 38),
          TextButton(
            style: TextButton.styleFrom(
              padding: EdgeInsets.zero,
              alignment: Alignment.centerLeft,
              minimumSize: const Size(88, 40),
            ),
            onPressed: _countdown == 0 ? _sendCode : null,
            child: Text(_countdown == 0 ? '重新获取' : '重新获取（${_countdown}s）'),
          ),
        ],
      ),
    );
  }
}

class _Agreement extends StatelessWidget {
  const _Agreement({required this.checked, required this.onChanged});

  final bool checked;
  final ValueChanged<bool> onChanged;

  @override
  Widget build(BuildContext context) {
    return Row(
      mainAxisAlignment: MainAxisAlignment.center,
      children: [
        SizedBox(
          width: 20,
          height: 20,
          child: Checkbox(
            value: checked,
            onChanged: (value) => onChanged(value ?? false),
            shape: const CircleBorder(),
            side: BorderSide(
              color: Theme.of(context).colorScheme.onSurfaceVariant,
            ),
          ),
        ),
        const SizedBox(width: 8),
        Flexible(
          child: Wrap(
            crossAxisAlignment: WrapCrossAlignment.center,
            alignment: WrapAlignment.center,
            children: [
              Text('我已阅读并同意', style: Theme.of(context).textTheme.bodySmall),
              TextButton(
                style: TextButton.styleFrom(
                  padding: const EdgeInsets.symmetric(horizontal: 2),
                  minimumSize: Size.zero,
                  tapTargetSize: MaterialTapTargetSize.shrinkWrap,
                ),
                onPressed: () => context.push('/terms'),
                child: const Text('《服务协议》'),
              ),
              Text('和', style: Theme.of(context).textTheme.bodySmall),
              TextButton(
                style: TextButton.styleFrom(
                  padding: const EdgeInsets.symmetric(horizontal: 2),
                  minimumSize: Size.zero,
                  tapTargetSize: MaterialTapTargetSize.shrinkWrap,
                ),
                onPressed: () => context.push('/privacy'),
                child: const Text('《隐私政策》'),
              ),
            ],
          ),
        ),
      ],
    );
  }
}

class _PhoneFormatter extends TextInputFormatter {
  const _PhoneFormatter();

  @override
  TextEditingValue formatEditUpdate(
    TextEditingValue oldValue,
    TextEditingValue newValue,
  ) {
    final raw = newValue.text.replaceAll(RegExp(r'\D'), '');
    final digits = raw.length > 11 ? raw.substring(0, 11) : raw;
    final buffer = StringBuffer();
    for (var index = 0; index < digits.length; index++) {
      if (index == 3 || index == 7) buffer.write(' ');
      buffer.write(digits[index]);
    }
    final text = buffer.toString();
    return TextEditingValue(
      text: text,
      selection: TextSelection.collapsed(offset: text.length),
    );
  }
}
