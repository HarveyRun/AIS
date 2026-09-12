class InquirySummary {
  const InquirySummary({
    required this.id,
    required this.role,
    required this.otherUserId,
    required this.otherName,
    required this.otherAvatar,
    required this.topic,
    required this.question,
    required this.sourceExperienceCertificationId,
    required this.amount,
    required this.depositAmount,
    required this.depositStatus,
    required this.settleableAmount,
    required this.timeoutRefundedAmount,
    required this.timeoutCount,
    required this.serviceFeeRate,
    required this.serviceFeeAmount,
    required this.answererIncomeAmount,
    required this.status,
    required this.fundsStatus,
    required this.unreadCount,
    required this.responseDeadline,
    required this.conversationExpiresAt,
    required this.createdAt,
    required this.lastMessageAt,
    required this.firstAnswererReplyAt,
    required this.flowVersion,
    required this.hourlyRateSnapshot,
    required this.questionerTextCount,
    required this.answererTextCount,
    required this.textMessageLimit,
    required this.communicationBlocked,
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
      sourceExperienceCertificationId: _int(
        json['sourceExperienceCertificationId'],
      ),
      amount: _double(json['amount']),
      depositAmount: _double(json['depositAmount']),
      depositStatus: json['depositStatus']?.toString() ?? 'NONE',
      settleableAmount: _double(json['settleableAmount'] ?? json['amount']),
      timeoutRefundedAmount: _double(json['timeoutRefundedAmount']),
      timeoutCount: _int(json['timeoutCount']),
      serviceFeeRate: _double(json['serviceFeeRate']),
      serviceFeeAmount: _double(json['serviceFeeAmount']),
      answererIncomeAmount: _double(json['answererIncomeAmount']),
      status: json['status']?.toString() ?? '',
      fundsStatus: json['fundsStatus']?.toString() ?? '',
      unreadCount: _int(json['unreadCount']),
      responseDeadline: _date(json['responseDeadline']),
      conversationExpiresAt: _date(json['conversationExpiresAt']),
      createdAt: _date(json['createdAt']),
      lastMessageAt: _date(json['lastMessageAt']),
      firstAnswererReplyAt: _date(json['firstAnswererReplyAt']),
      flowVersion: _int(json['flowVersion']),
      hourlyRateSnapshot: _int(json['hourlyRateSnapshot']),
      questionerTextCount: _int(json['questionerTextCount']),
      answererTextCount: _int(json['answererTextCount']),
      textMessageLimit: _int(json['textMessageLimit']) == 0
          ? 50
          : _int(json['textMessageLimit']),
      communicationBlocked: json['communicationBlocked'] == true,
    );
  }

  final int id;
  final String role;
  final int otherUserId;
  final String otherName;
  final String otherAvatar;
  final String topic;
  final String question;
  final int sourceExperienceCertificationId;
  final double amount;
  final double depositAmount;
  final String depositStatus;
  final double settleableAmount;
  final double timeoutRefundedAmount;
  final int timeoutCount;
  final double serviceFeeRate;
  final double serviceFeeAmount;
  final double answererIncomeAmount;
  final String status;
  final String fundsStatus;
  final int unreadCount;
  final DateTime? responseDeadline;
  final DateTime? conversationExpiresAt;
  final DateTime? createdAt;
  final DateTime? lastMessageAt;
  final DateTime? firstAnswererReplyAt;
  final int flowVersion;
  final int hourlyRateSnapshot;
  final int questionerTextCount;
  final int answererTextCount;
  final int textMessageLimit;
  final bool communicationBlocked;

  bool get isIncoming => role.toUpperCase() == 'ANSWERER';
  bool get isCurrentFlow => flowVersion >= 2;
  bool get canChat => !communicationBlocked && status.toUpperCase() == 'ACTIVE';
  bool get canCreateVoiceCall =>
      isCurrentFlow &&
      !communicationBlocked &&
      !isIncoming &&
      const {'ACTIVE', 'TEXT_LIMIT_REACHED'}.contains(status.toUpperCase());
  int get ownTextCount => isIncoming ? answererTextCount : questionerTextCount;
  int get remainingTextCount =>
      (textMessageLimit - ownTextCount).clamp(0, textMessageLimit);
  double get visibleAmount =>
      isIncoming ? answererIncomeAmount : settleableAmount;
}

class VoiceCall {
  const VoiceCall({
    required this.id,
    required this.inquiryId,
    required this.role,
    required this.status,
    required this.maxEndAt,
    required this.actualDurationSeconds,
    required this.hourlyRateSnapshot,
    required this.reservedAmount,
    required this.actualAmount,
    required this.answererIncomeAmount,
    required this.acceptedAt,
    required this.connectDeadline,
    required this.connectedAt,
    required this.reconnectDeadline,
    required this.endReason,
    required this.createdAt,
  });

  factory VoiceCall.fromJson(Map<String, dynamic> json) {
    return VoiceCall(
      id: _int(json['id']),
      inquiryId: _int(json['inquiryId']),
      role: json['role']?.toString() ?? '',
      status: json['status']?.toString() ?? 'NONE',
      maxEndAt: _date(json['maxEndAt']),
      actualDurationSeconds: _int(json['actualDurationSeconds']),
      hourlyRateSnapshot: _int(json['hourlyRateSnapshot']),
      reservedAmount: _double(json['reservedAmount']),
      actualAmount: _double(json['actualAmount']),
      answererIncomeAmount: _double(json['answererIncomeAmount']),
      acceptedAt: _date(json['acceptedAt']),
      connectDeadline: _date(json['connectDeadline']),
      connectedAt: _date(json['connectedAt']),
      reconnectDeadline: _date(json['reconnectDeadline']),
      endReason: json['endReason']?.toString() ?? '',
      createdAt: _date(json['createdAt']),
    );
  }

  final int id;
  final int inquiryId;
  final String role;
  final String status;
  final DateTime? maxEndAt;
  final int actualDurationSeconds;
  final int hourlyRateSnapshot;
  final double reservedAmount;
  final double actualAmount;
  final double answererIncomeAmount;
  final DateTime? acceptedAt;
  final DateTime? connectDeadline;
  final DateTime? connectedAt;
  final DateTime? reconnectDeadline;
  final String endReason;
  final DateTime? createdAt;

  bool get exists => id > 0;
  bool get isIncoming => role.toUpperCase() == 'ANSWERER';
  bool get isOpen =>
      const {'CONNECTING', 'ACTIVE'}.contains(status.toUpperCase());
}

class VoiceIceConfig {
  const VoiceIceConfig({
    required this.urls,
    required this.username,
    required this.credential,
  });

  factory VoiceIceConfig.fromJson(Map<String, dynamic> json) {
    return VoiceIceConfig(
      urls: (json['urls'] as List<dynamic>? ?? const [])
          .map((item) => item.toString())
          .where((item) => item.isNotEmpty)
          .toList(growable: false),
      username: json['username']?.toString() ?? '',
      credential: json['credential']?.toString() ?? '',
    );
  }

  final List<String> urls;
  final String username;
  final String credential;
}

class VoiceSignal {
  const VoiceSignal({
    required this.id,
    required this.inquiryId,
    required this.voiceCallId,
    required this.senderId,
    required this.signalType,
    required this.payload,
    required this.createdAt,
  });

  factory VoiceSignal.fromJson(Map<String, dynamic> json) {
    return VoiceSignal(
      id: _int(json['id']),
      inquiryId: _int(json['inquiryId']),
      voiceCallId: _int(json['voiceCallId']),
      senderId: _int(json['senderId']),
      signalType: json['signalType']?.toString() ?? '',
      payload: json['payload']?.toString() ?? '',
      createdAt: _date(json['createdAt']),
    );
  }

  final int id;
  final int inquiryId;
  final int voiceCallId;
  final int senderId;
  final String signalType;
  final String payload;
  final DateTime? createdAt;
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

class InquiryQualityOptions {
  const InquiryQualityOptions({
    required this.canEvaluate,
    required this.evaluated,
  });

  factory InquiryQualityOptions.fromJson(Map<String, dynamic> json) {
    return InquiryQualityOptions(
      canEvaluate: json['canEvaluate'] == true,
      evaluated: json['evaluated'] == true,
    );
  }

  final bool canEvaluate;
  final bool evaluated;
}

int _int(Object? value) =>
    value is num ? value.toInt() : int.tryParse('$value') ?? 0;
double _double(Object? value) =>
    value is num ? value.toDouble() : double.tryParse('$value') ?? 0;
DateTime? _date(Object? value) =>
    value == null ? null : DateTime.tryParse('$value')?.toLocal();
