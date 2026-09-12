class CuratedMembership {
  const CuratedMembership({
    required this.applied,
    required this.status,
    required this.identityStatus,
    required this.jobStatus,
    required this.identityRejectionReason,
    required this.jobRejectionReason,
    required this.jobTitle,
    required this.jobYears,
    required this.startedAt,
    required this.expiresAt,
    required this.price,
  });
  factory CuratedMembership.fromJson(Map<String, dynamic> j) =>
      CuratedMembership(
        applied: j['applied'] == true,
        status: '${j['status'] ?? 'NOT_APPLIED'}',
        identityStatus: '${j['identityStatus'] ?? 'NOT_APPLIED'}',
        jobStatus: '${j['jobStatus'] ?? 'NOT_APPLIED'}',
        identityRejectionReason: j['identityRejectionReason']?.toString(),
        jobRejectionReason: j['jobRejectionReason']?.toString(),
        jobTitle: j['jobTitle']?.toString(),
        jobYears: _intOrNull(j['jobYears']),
        startedAt: _date(j['startedAt']),
        expiresAt: _date(j['expiresAt']),
        price: _double(j['price']),
      );
  final bool applied;
  final String status;
  final String identityStatus;
  final String jobStatus;
  final String? identityRejectionReason;
  final String? jobRejectionReason;
  final String? jobTitle;
  final int? jobYears;
  final DateTime? startedAt;
  final DateTime? expiresAt;
  final double price;
  bool get active => status == 'ACTIVE';
  bool get readyToPay => status == 'READY_TO_PAY';
}

class CuratedPayment {
  const CuratedPayment({
    required this.orderNo,
    required this.amount,
    required this.status,
    required this.channel,
    required this.paymentPayload,
    required this.paidAt,
  });
  factory CuratedPayment.fromJson(Map<String, dynamic> j) => CuratedPayment(
    orderNo: '${j['orderNo'] ?? ''}',
    amount: _double(j['amount']),
    status: '${j['status'] ?? ''}',
    channel: '${j['channel'] ?? ''}',
    paymentPayload: j['paymentPayload']?.toString() ?? '',
    paidAt: _date(j['paidAt']),
  );
  final String orderNo;
  final double amount;
  final String status;
  final String channel;
  final String paymentPayload;
  final DateTime? paidAt;
}

class CuratedCertificationMaterial {
  const CuratedCertificationMaterial({
    required this.id,
    required this.materialType,
    required this.mediaType,
    required this.originalName,
    required this.url,
    required this.fileSize,
  });

  factory CuratedCertificationMaterial.fromJson(Map<String, dynamic> json) =>
      CuratedCertificationMaterial(
        id: _int(json['id']),
        materialType: '${json['materialType'] ?? ''}',
        mediaType: '${json['mediaType'] ?? ''}',
        originalName: '${json['originalName'] ?? ''}',
        url: '${json['url'] ?? ''}',
        fileSize: _int(json['fileSize']),
      );

  final int id;
  final String materialType;
  final String mediaType;
  final String originalName;
  final String url;
  final int fileSize;

  bool get isIdentity => materialType.startsWith('IDENTITY_');
  bool get isJob => materialType == 'JOB';
}

class CuratedMember {
  const CuratedMember({
    required this.id,
    required this.uid,
    required this.nickname,
    required this.avatarUrl,
    required this.jobTitle,
    required this.jobYears,
    required this.online,
  });
  factory CuratedMember.fromJson(Map<String, dynamic> j) => CuratedMember(
    id: _int(j['id']),
    uid: '${j['uid'] ?? ''}',
    nickname: '${j['nickname'] ?? ''}',
    avatarUrl: '${j['avatarUrl'] ?? ''}',
    jobTitle: '${j['jobTitle'] ?? ''}',
    jobYears: _int(j['jobYears']),
    online: j['online'] == true,
  );
  final int id;
  final String uid;
  final String nickname;
  final String avatarUrl;
  final String jobTitle;
  final int jobYears;
  final bool online;
}

class CuratedMemberPage {
  const CuratedMemberPage({
    required this.content,
    required this.total,
    required this.hasNext,
  });
  factory CuratedMemberPage.fromJson(Map<String, dynamic> j) =>
      CuratedMemberPage(
        content: (j['content'] as List? ?? const [])
            .whereType<Map>()
            .map((x) => CuratedMember.fromJson(Map<String, dynamic>.from(x)))
            .toList(),
        total: _int(j['totalElements']),
        hasNext: j['hasNext'] == true,
      );
  final List<CuratedMember> content;
  final int total;
  final bool hasNext;
}

class CuratedMessage {
  const CuratedMessage({
    required this.id,
    required this.senderId,
    required this.type,
    required this.content,
    required this.attachmentUrl,
    required this.attachmentName,
    required this.createdAt,
  });
  factory CuratedMessage.fromJson(Map<String, dynamic> j) => CuratedMessage(
    id: _int(j['id']),
    senderId: _int(j['senderId']),
    type: '${j['type'] ?? ''}',
    content: '${j['content'] ?? ''}',
    attachmentUrl: '${j['attachmentUrl'] ?? ''}',
    attachmentName: '${j['attachmentName'] ?? ''}',
    createdAt: _date(j['createdAt']),
  );
  final int id;
  final int senderId;
  final String type;
  final String content;
  final String attachmentUrl;
  final String attachmentName;
  final DateTime? createdAt;
  bool get system => type == 'SYSTEM';
}

class CuratedConversation {
  const CuratedConversation({
    required this.id,
    required this.otherUser,
    required this.unreadCount,
    required this.lastMessage,
    required this.messages,
    required this.writable,
    required this.lastMessageAt,
  });
  factory CuratedConversation.fromJson(Map<String, dynamic> j) =>
      CuratedConversation(
        id: _int(j['id']),
        otherUser: CuratedMember.fromJson(
          Map<String, dynamic>.from(j['otherUser'] as Map? ?? const {}),
        ),
        unreadCount: _int(j['unreadCount']),
        lastMessage: j['lastMessage'] is Map
            ? CuratedMessage.fromJson(
                Map<String, dynamic>.from(j['lastMessage'] as Map),
              )
            : null,
        messages: (j['messages'] as List? ?? const [])
            .whereType<Map>()
            .map((x) => CuratedMessage.fromJson(Map<String, dynamic>.from(x)))
            .toList(),
        writable: j['writable'] == true,
        lastMessageAt: _date(j['lastMessageAt']),
      );
  final int id;
  final CuratedMember otherUser;
  final int unreadCount;
  final CuratedMessage? lastMessage;
  final List<CuratedMessage> messages;
  final bool writable;
  final DateTime? lastMessageAt;
}

class CuratedVoiceCall {
  const CuratedVoiceCall({
    required this.id,
    required this.conversationId,
    required this.callerId,
    required this.calleeId,
    required this.status,
    required this.connectDeadline,
    required this.connectedAt,
    required this.endedAt,
  });
  factory CuratedVoiceCall.fromJson(Map<String, dynamic> j) => CuratedVoiceCall(
    id: _int(j['id']),
    conversationId: _int(j['conversationId']),
    callerId: _int(j['callerId']),
    calleeId: _int(j['calleeId']),
    status: '${j['status'] ?? ''}',
    connectDeadline: _date(j['connectDeadline']),
    connectedAt: _date(j['connectedAt']),
    endedAt: _date(j['endedAt']),
  );
  final int id;
  final int conversationId;
  final int callerId;
  final int calleeId;
  final String status;
  final DateTime? connectDeadline;
  final DateTime? connectedAt;
  final DateTime? endedAt;
}

class CuratedVoiceSignal {
  const CuratedVoiceSignal({
    required this.id,
    required this.type,
    required this.payload,
  });
  factory CuratedVoiceSignal.fromJson(Map<String, dynamic> j) =>
      CuratedVoiceSignal(
        id: _int(j['id']),
        type: '${j['type'] ?? ''}',
        payload: '${j['payload'] ?? ''}',
      );
  final int id;
  final String type;
  final String payload;
}

int _int(Object? v) => v is num ? v.toInt() : int.tryParse('$v') ?? 0;
int? _intOrNull(Object? v) => v == null ? null : _int(v);
double _double(Object? v) =>
    v is num ? v.toDouble() : double.tryParse('$v') ?? 0;
DateTime? _date(Object? v) => v == null ? null : DateTime.tryParse('$v');
