class AppNotification {
  const AppNotification({
    required this.id,
    required this.sourceType,
    required this.title,
    required this.content,
    required this.targetPath,
    required this.read,
    required this.createdAt,
  });

  factory AppNotification.fromJson(Map<String, dynamic> json) {
    return AppNotification(
      id: _int(json['id']),
      sourceType: json['sourceType']?.toString() ?? 'PERSONAL',
      title: json['title']?.toString() ?? '',
      content: json['content']?.toString() ?? '',
      targetPath: json['targetPath']?.toString() ?? '',
      read: json['read'] == true,
      createdAt: _date(json['createdAt']),
    );
  }

  final int id;
  final String sourceType;
  final String title;
  final String content;
  final String targetPath;
  final bool read;
  final DateTime? createdAt;

  bool get isAnnouncement => sourceType.toUpperCase() == 'ANNOUNCEMENT';
}

class FeedbackRecord {
  const FeedbackRecord({
    required this.id,
    required this.type,
    required this.category,
    required this.content,
    required this.status,
    required this.createdAt,
  });

  factory FeedbackRecord.fromJson(Map<String, dynamic> json) {
    return FeedbackRecord(
      id: _int(json['id']),
      type: json['type']?.toString() ?? '',
      category: json['category']?.toString() ?? '',
      content: json['content']?.toString() ?? '',
      status: json['status']?.toString() ?? '',
      createdAt: _date(json['createdAt']),
    );
  }

  final int id;
  final String type;
  final String category;
  final String content;
  final String status;
  final DateTime? createdAt;
}

class CustomerServiceMessage {
  const CustomerServiceMessage({
    required this.id,
    required this.senderType,
    required this.messageType,
    required this.content,
    required this.attachmentUrl,
    required this.createdAt,
  });

  factory CustomerServiceMessage.fromJson(Map<String, dynamic> json) {
    return CustomerServiceMessage(
      id: _int(json['id']),
      senderType: json['senderType']?.toString() ?? '',
      messageType: json['messageType']?.toString() ?? 'TEXT',
      content: json['content']?.toString() ?? '',
      attachmentUrl: json['attachmentUrl']?.toString() ?? '',
      createdAt: _date(json['createdAt']),
    );
  }

  final int id;
  final String senderType;
  final String messageType;
  final String content;
  final String attachmentUrl;
  final DateTime? createdAt;

  bool get isMine => senderType.toUpperCase() == 'USER';
  bool get isImage => messageType.toUpperCase() == 'IMAGE';
}

int _int(Object? value) =>
    value is num ? value.toInt() : int.tryParse('$value') ?? 0;
DateTime? _date(Object? value) =>
    value == null ? null : DateTime.tryParse('$value')?.toLocal();
