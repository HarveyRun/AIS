import 'package:flutter/material.dart';

class AsyncContent<T> extends StatelessWidget {
  const AsyncContent({
    super.key,
    required this.value,
    required this.builder,
    required this.retry,
    this.empty,
  });

  final AsyncSnapshot<T> value;
  final Widget Function(T data) builder;
  final VoidCallback retry;
  final Widget? empty;

  @override
  Widget build(BuildContext context) {
    if (value.connectionState == ConnectionState.waiting) {
      return const Center(child: CircularProgressIndicator(strokeWidth: 2));
    }
    if (value.hasError) {
      return Center(
        child: Padding(
          padding: const EdgeInsets.all(32),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              Text('${value.error}', textAlign: TextAlign.center),
              const SizedBox(height: 16),
              OutlinedButton(onPressed: retry, child: const Text('重新加载')),
            ],
          ),
        ),
      );
    }
    final data = value.data;
    if (data == null) return empty ?? const SizedBox.shrink();
    return builder(data);
  }
}
