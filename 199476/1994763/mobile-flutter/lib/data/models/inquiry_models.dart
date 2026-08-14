class InquirySummary {
  const InquirySummary({
    required this.id,
    required this.role,
    required this.otherUserId,
    required this.otherName,
    required this.otherAvatar,
    required this.topic,
    required this.question,
    required this.amount,
    required this.status,
    required this.fundsStatus,
    required this.unreadCount,
    required this.responseDeadline,
    required this.confirmationDeadline,
    required this.createdAt,
    required this.lastMessageAt,
  });

  factory InquirySummary.fromJson(Map<String, dynamic> json) {
    return InquirySummary(
      id: _int(json['id']),
      role: json['role']?.toString() ?? '',
      otherUserId: _int(json['otherUserId']),
      otherName: json['otherName']?.toString() ?? '',
      otherAvatar: json['otherAvatar']?.toString() ?? '',
      topic: json['topic']?.toString() ?? '',
      question: json['question']?.toString() ?? '',
      amount: _double(json['amount']),
      status: json['status']?.toString() ?? '',
      fundsStatus: json['fundsStatus']?.toString() ?? '',
      unreadCount: _int(json['unreadCount']),
      responseDeadline: _date(json['responseDeadline']),
      confirmationDeadline: _date(json['confirmationDeadline']),
      createdAt: _date(json['createdAt']),
      lastMessageAt: _date(json['lastMessageAt']),
    );
  }

  final int id;
  final String role;
  final int otherUserId;
  final String otherName;
  final String otherAvatar;
  final String topic;
  final String question;
  final double amount;
  final String status;
  final String fundsStatus;
  final int unreadCount;
  final DateTime? responseDeadline;
  final DateTime? confirmationDeadline;
  final DateTime? createdAt;
  final DateTime? lastMessageAt;

  bool get isIncoming => role.toUpperCase() == 'ANSWERER';
  bool get canChat => status.toUpperCase() == 'ACTIVE';
}

class ChatMessage {
  const ChatMessage({
    required this.id,
    required this.senderId,
    required this.senderName,
    required this.senderAvatar,
    required this.type,
    required this.content,
    required this.attachmentUrl,
    required this.attachmentName,
    required this.attachmentSize,
    required this.createdAt,
    this.sending = false,
    this.failed = false,
  });

  factory ChatMessage.fromJson(Map<String, dynamic> json) {
    return ChatMessage(
      id: json['id']?.toString() ?? '',
      senderId: _int(json['senderId']),
      senderName: json['senderName']?.toString() ?? '',
      senderAvatar: json['senderAvatar']?.toString() ?? '',
      type: json['type']?.toString() ?? 'TEXT',
      content: json['content']?.toString() ?? '',
      attachmentUrl: json['attachmentUrl']?.toString() ?? '',
      attachmentName: json['attachmentName']?.toString() ?? '',
      attachmentSize: _int(json['attachmentSize']),
      createdAt: _date(json['createdAt']),
    );
  }

  final String id;
  final int senderId;
  final String senderName;
  final String senderAvatar;
  final String type;
  final String content;
  final String attachmentUrl;
  final String attachmentName;
  final int attachmentSize;
  final DateTime? createdAt;
  final bool sending;
  final bool failed;

  ChatMessage copyWith({bool? sending, bool? failed}) {
    return ChatMessage(
      id: id,
      senderId: senderId,
      senderName: senderName,
      senderAvatar: senderAvatar,
      type: type,
      content: content,
      attachmentUrl: attachmentUrl,
      attachmentName: attachmentName,
      attachmentSize: attachmentSize,
      createdAt: createdAt,
      sending: sending ?? this.sending,
      failed: failed ?? this.failed,
    );
  }
}

class InquiryDetail {
  const InquiryDetail({required this.inquiry, required this.messages});

  factory InquiryDetail.fromJson(Map<String, dynamic> json) {
    return InquiryDetail(
      inquiry: InquirySummary.fromJson(
        Map<String, dynamic>.from(json['inquiry'] as Map),
      ),
      messages: (json['messages'] as List<dynamic>? ?? const [])
          .whereType<Map>()
          .map((item) => ChatMessage.fromJson(Map<String, dynamic>.from(item)))
          .toList(growable: false),
    );
  }

  final InquirySummary inquiry;
  final List<ChatMessage> messages;
}

int _int(Object? value) =>
    value is num ? value.toInt() : int.tryParse('$value') ?? 0;
double _double(Object? value) =>
    value is num ? value.toDouble() : double.tryParse('$value') ?? 0;
DateTime? _date(Object? value) =>
    value == null ? null : DateTime.tryParse('$value')?.toLocal();
