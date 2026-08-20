import 'dart:math';

class RequestId {
  static final Random _random = Random.secure();

  static String create(String prefix) {
    final timestamp = DateTime.now().microsecondsSinceEpoch.toRadixString(36);
    final randomPart = List.generate(
      4,
      (_) => _random.nextInt(0x7fffffff).toRadixString(36),
    ).join();
    return '$prefix-$timestamp-$randomPart';
  }
}
