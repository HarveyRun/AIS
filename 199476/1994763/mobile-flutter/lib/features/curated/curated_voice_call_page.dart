import 'dart:async';
import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_webrtc/flutter_webrtc.dart';
import 'package:go_router/go_router.dart';

import '../../app/providers.dart';
import '../../core/network/realtime_service.dart';
import '../../core/widgets/app_avatar.dart';
import '../../core/widgets/app_message.dart';
import '../../data/models/curated_chat_models.dart';
import '../../data/models/inquiry_models.dart';

class CuratedVoiceCallPage extends ConsumerStatefulWidget {
  const CuratedVoiceCallPage({
    super.key,
    required this.callId,
    required this.initiator,
  });

  final int callId;
  final bool initiator;

  @override
  ConsumerState<CuratedVoiceCallPage> createState() =>
      _CuratedVoiceCallPageState();
}

class _CuratedVoiceCallPageState extends ConsumerState<CuratedVoiceCallPage> {
  static const _native = MethodChannel('com.shixianwen/voice_call');
  RTCPeerConnection? _peer;
  MediaStream? _localStream;
  StreamSubscription<RealtimeEvent>? _events;
  Timer? _signalTimer;
  Timer? _clockTimer;
  CuratedVoiceCall? _call;
  CuratedConversation? _conversation;
  VoiceIceConfig? _ice;
  final List<RTCIceCandidate> _pendingCandidates = [];
  final Set<int> _handledSignals = {};
  int _lastSignalId = 0;
  bool _peerReady = false;
  bool _remoteDescriptionSet = false;
  bool _waitingForAnswer = false;
  bool _busy = false;
  bool _closing = false;
  bool _muted = false;
  bool _speaker = false;
  String _stateText = '正在准备语音通话';

  @override
  void initState() {
    super.initState();
    _events = ref.read(realtimeProvider).events.listen(_handleRealtime);
    unawaited(_initialize());
  }

  Future<void> _initialize() async {
    try {
      final repository = ref.read(repositoryProvider);
      final call = await repository.curatedVoiceCall(widget.callId);
      if (!const {'RINGING', 'ANSWERED', 'CONNECTED'}.contains(call.status)) {
        throw Exception('本次语音通话已经结束');
      }
      final values = await Future.wait<Object>([
        repository.curatedConversation(call.conversationId, showLoading: false),
        repository.curatedVoiceIceConfig(widget.callId),
      ]);
      if (!mounted) return;
      setState(() {
        _call = call;
        _conversation = values[0] as CuratedConversation;
        _ice = values[1] as VoiceIceConfig;
        _waitingForAnswer = !widget.initiator && call.status == 'RINGING';
        _stateText = _waitingForAnswer ? '语音来电' : '正在连接';
      });
      _signalTimer = Timer.periodic(
        const Duration(milliseconds: 1200),
        (_) => unawaited(_pullSignals()),
      );
      _clockTimer = Timer.periodic(const Duration(seconds: 1), (_) {
        if (mounted) setState(() {});
      });
      if (!_waitingForAnswer) await _initializeMedia();
    } catch (error) {
      if (mounted) AppMessage.show(context, _cleanError(error));
      await _closeLocal();
    }
  }

  Future<void> _initializeMedia() async {
    if (_peerReady || _busy || _closing) return;
    setState(() => _busy = true);
    try {
      final ice = _ice;
      if (ice == null) throw Exception('无法读取语音连接配置');
      final servers = <Map<String, dynamic>>[];
      if (ice.urls.isNotEmpty) {
        servers.add({
          'urls': ice.urls,
          if (ice.username.isNotEmpty) 'username': ice.username,
          if (ice.credential.isNotEmpty) 'credential': ice.credential,
        });
      }
      _peer = await createPeerConnection({
        'iceServers': servers,
        'sdpSemantics': 'unified-plan',
      });
      _localStream = await navigator.mediaDevices.getUserMedia({
        'audio': {
          'echoCancellation': true,
          'noiseSuppression': true,
          'autoGainControl': true,
        },
        'video': false,
      });
      for (final track in _localStream!.getAudioTracks()) {
        await _peer!.addTrack(track, _localStream!);
      }
      _peer!.onIceCandidate = (candidate) {
        if (candidate.candidate == null) return;
        unawaited(
          _sendSignal(
            'ICE',
            jsonEncode({
              'candidate': candidate.candidate,
              'sdpMid': candidate.sdpMid,
              'sdpMLineIndex': candidate.sdpMLineIndex,
            }),
          ),
        );
      };
      _peer!.onTrack = (event) => event.track.enabled = true;
      _peer!.onConnectionState = (state) {
        if (!mounted || _closing) return;
        setState(() {
          _stateText = switch (state) {
            RTCPeerConnectionState.RTCPeerConnectionStateConnected => '通话中',
            RTCPeerConnectionState.RTCPeerConnectionStateConnecting => '正在连接',
            RTCPeerConnectionState.RTCPeerConnectionStateDisconnected =>
              '连接已中断，正在恢复',
            RTCPeerConnectionState.RTCPeerConnectionStateFailed => '连接失败',
            RTCPeerConnectionState.RTCPeerConnectionStateClosed => '通话已结束',
            _ => _stateText,
          };
        });
        if (state == RTCPeerConnectionState.RTCPeerConnectionStateConnected) {
          unawaited(_reportConnected());
        } else if (widget.initiator &&
            (state ==
                    RTCPeerConnectionState.RTCPeerConnectionStateDisconnected ||
                state == RTCPeerConnectionState.RTCPeerConnectionStateFailed)) {
          unawaited(_restartIce());
        }
      };
      await Helper.setSpeakerphoneOn(false);
      await _startForegroundCall();
      _peerReady = true;
      await _pullSignals();
      if (widget.initiator) await _createOffer();
    } catch (error) {
      if (mounted) AppMessage.show(context, _cleanError(error));
      await _endRemote();
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  Future<void> _answer() async {
    if (!_waitingForAnswer || _busy) return;
    setState(() {
      _busy = true;
      _stateText = '正在接通';
    });
    try {
      final call = await ref
          .read(repositoryProvider)
          .answerCuratedVoiceCall(widget.callId);
      if (!mounted) return;
      setState(() {
        _call = call;
        _waitingForAnswer = false;
        _busy = false;
      });
      await _initializeMedia();
    } catch (error) {
      if (!mounted) return;
      setState(() {
        _busy = false;
        _stateText = '语音来电';
      });
      AppMessage.show(context, _cleanError(error));
    }
  }

  Future<void> _reject() async {
    if (_closing) return;
    try {
      await ref.read(repositoryProvider).rejectCuratedVoiceCall(widget.callId);
    } catch (_) {}
    await _closeLocal();
  }

  void _handleRealtime(RealtimeEvent event) {
    final callId = int.tryParse('${event.payload['callId'] ?? ''}');
    if (callId != widget.callId) return;
    if (event.type == 'CURATED_VOICE_SIGNAL') {
      unawaited(_pullSignals());
    } else if (event.type == 'CURATED_VOICE_ANSWERED') {
      if (mounted) setState(() => _stateText = '对方已接听，正在连接');
    } else if (const {
      'CURATED_VOICE_REJECTED',
      'CURATED_VOICE_MISSED',
      'CURATED_VOICE_ENDED',
    }.contains(event.type)) {
      unawaited(_closeLocal());
    }
  }

  Future<void> _pullSignals() async {
    if (_closing || !_peerReady) return;
    try {
      final signals = await ref
          .read(repositoryProvider)
          .curatedVoiceSignals(widget.callId, afterId: _lastSignalId);
      for (final signal in signals) {
        _lastSignalId = signal.id > _lastSignalId ? signal.id : _lastSignalId;
        await _handleSignal(signal);
      }
    } catch (_) {
      // WebSocket 会同时触发拉取；短暂断网时由下一轮轮询恢复。
    }
  }

  Future<void> _handleSignal(CuratedVoiceSignal signal) async {
    if (_handledSignals.contains(signal.id) || _closing || _peer == null) {
      return;
    }
    _handledSignals.add(signal.id);
    final decoded = jsonDecode(signal.payload);
    if (decoded is! Map) return;
    final data = Map<String, dynamic>.from(decoded);
    switch (signal.type) {
      case 'OFFER':
        if (widget.initiator) return;
        await _peer!.setRemoteDescription(
          RTCSessionDescription(data['sdp']?.toString(), 'offer'),
        );
        _remoteDescriptionSet = true;
        await _flushCandidates();
        final answer = await _peer!.createAnswer({
          'offerToReceiveAudio': true,
          'offerToReceiveVideo': false,
        });
        await _peer!.setLocalDescription(answer);
        await _sendSignal(
          'ANSWER',
          jsonEncode({'sdp': answer.sdp, 'type': answer.type}),
        );
      case 'ANSWER':
        if (!widget.initiator) return;
        await _peer!.setRemoteDescription(
          RTCSessionDescription(data['sdp']?.toString(), 'answer'),
        );
        _remoteDescriptionSet = true;
        await _flushCandidates();
      case 'ICE':
        final candidate = RTCIceCandidate(
          data['candidate']?.toString(),
          data['sdpMid']?.toString(),
          data['sdpMLineIndex'] is num
              ? (data['sdpMLineIndex'] as num).toInt()
              : int.tryParse('${data['sdpMLineIndex']}'),
        );
        if (_remoteDescriptionSet) {
          await _peer!.addCandidate(candidate);
        } else {
          _pendingCandidates.add(candidate);
        }
    }
  }

  Future<void> _createOffer({bool restart = false}) async {
    if (_peer == null || _closing) return;
    if (mounted) setState(() => _stateText = '正在呼叫对方');
    final offer = await _peer!.createOffer({
      'offerToReceiveAudio': true,
      'offerToReceiveVideo': false,
      if (restart) 'iceRestart': true,
    });
    await _peer!.setLocalDescription(offer);
    await _sendSignal(
      'OFFER',
      jsonEncode({
        'sdp': offer.sdp,
        'type': offer.type,
        if (restart) 'iceRestart': true,
      }),
    );
  }

  Future<void> _restartIce() async {
    await Future<void>.delayed(const Duration(seconds: 2));
    if (!_closing && _peer != null) {
      try {
        await _createOffer(restart: true);
      } catch (_) {}
    }
  }

  Future<void> _flushCandidates() async {
    final items = List<RTCIceCandidate>.from(_pendingCandidates);
    _pendingCandidates.clear();
    for (final candidate in items) {
      await _peer?.addCandidate(candidate);
    }
  }

  Future<void> _sendSignal(String type, String payload) async {
    await ref
        .read(repositoryProvider)
        .sendCuratedVoiceSignal(widget.callId, type, payload);
  }

  Future<void> _reportConnected() async {
    try {
      final call = await ref
          .read(repositoryProvider)
          .connectCuratedVoiceCall(widget.callId);
      if (mounted) setState(() => _call = call);
    } catch (_) {}
  }

  Future<void> _hangUp() async {
    if (_waitingForAnswer) {
      await _reject();
    } else {
      await _endRemote();
    }
  }

  Future<void> _endRemote() async {
    if (_closing) return;
    try {
      await ref.read(repositoryProvider).endCuratedVoiceCall(widget.callId);
    } catch (_) {}
    await _closeLocal();
  }

  Future<void> _closeLocal() async {
    if (_closing) return;
    _closing = true;
    _signalTimer?.cancel();
    _clockTimer?.cancel();
    await _events?.cancel();
    for (final track in _localStream?.getTracks() ?? const []) {
      track.stop();
    }
    await _localStream?.dispose();
    await _peer?.close();
    await _peer?.dispose();
    await Helper.setSpeakerphoneOn(false);
    await _stopForegroundCall();
    if (mounted && context.canPop()) context.pop();
  }

  Future<void> _toggleMute() async {
    _muted = !_muted;
    for (final track in _localStream?.getAudioTracks() ?? const []) {
      track.enabled = !_muted;
    }
    if (mounted) setState(() {});
  }

  Future<void> _toggleSpeaker() async {
    _speaker = !_speaker;
    await Helper.setSpeakerphoneOn(_speaker);
    if (mounted) setState(() {});
  }

  Future<void> _startForegroundCall() async {
    try {
      await _native.invokeMethod<void>('startForegroundCall');
    } catch (_) {}
  }

  Future<void> _stopForegroundCall() async {
    try {
      await _native.invokeMethod<void>('stopForegroundCall');
    } catch (_) {}
  }

  String get _elapsed {
    final start = _call?.connectedAt;
    if (start == null) return '00:00';
    final total = DateTime.now().difference(start).inSeconds.clamp(0, 864000);
    final hours = total ~/ 3600;
    final minutes = (total % 3600) ~/ 60;
    final seconds = total % 60;
    if (hours > 0) {
      return '${hours.toString().padLeft(2, '0')}:${minutes.toString().padLeft(2, '0')}:${seconds.toString().padLeft(2, '0')}';
    }
    return '${minutes.toString().padLeft(2, '0')}:${seconds.toString().padLeft(2, '0')}';
  }

  @override
  void dispose() {
    if (!_closing) {
      _signalTimer?.cancel();
      _clockTimer?.cancel();
      _events?.cancel();
      for (final track in _localStream?.getTracks() ?? const []) {
        track.stop();
      }
      _localStream?.dispose();
      _peer?.close();
      _peer?.dispose();
      Helper.setSpeakerphoneOn(false);
      _stopForegroundCall();
    }
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final other = _conversation?.otherUser;
    return PopScope(
      canPop: false,
      onPopInvokedWithResult: (didPop, _) {
        if (!didPop) unawaited(_hangUp());
      },
      child: Scaffold(
        backgroundColor: const Color(0xFF171719),
        body: SafeArea(
          child: Padding(
            padding: const EdgeInsets.fromLTRB(24, 22, 24, 34),
            child: Column(
              children: [
                Align(
                  alignment: Alignment.centerLeft,
                  child: IconButton(
                    onPressed: _hangUp,
                    color: Colors.white,
                    iconSize: 32,
                    icon: const Icon(Icons.keyboard_arrow_down_rounded),
                  ),
                ),
                const Spacer(flex: 2),
                AppAvatar(
                  url: other?.avatarUrl ?? '',
                  name: other?.nickname ?? '',
                  radius: 48,
                ),
                const SizedBox(height: 20),
                Text(
                  other?.nickname.isNotEmpty == true ? other!.nickname : '对方',
                  style: const TextStyle(
                    color: Colors.white,
                    fontSize: 24,
                    fontWeight: FontWeight.w700,
                  ),
                ),
                const SizedBox(height: 10),
                Text(
                  '$_stateText · $_elapsed',
                  style: const TextStyle(
                    color: Color(0xFFB8B8BD),
                    fontSize: 15,
                  ),
                ),
                const Spacer(flex: 3),
                if (_waitingForAnswer)
                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceEvenly,
                    children: [
                      _CallAction(
                        icon: Icons.call_end_rounded,
                        label: '拒绝',
                        color: const Color(0xFFD83A32),
                        onTap: _reject,
                      ),
                      _CallAction(
                        icon: Icons.call_rounded,
                        label: '接听',
                        color: const Color(0xFF28A745),
                        onTap: _busy ? () {} : _answer,
                      ),
                    ],
                  )
                else
                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceEvenly,
                    children: [
                      _CallAction(
                        icon: _muted
                            ? Icons.mic_off_rounded
                            : Icons.mic_rounded,
                        label: _muted ? '已静音' : '静音',
                        selected: _muted,
                        onTap: _toggleMute,
                      ),
                      _CallAction(
                        icon: _speaker
                            ? Icons.volume_up_rounded
                            : Icons.hearing_rounded,
                        label: _speaker ? '免提' : '听筒',
                        selected: _speaker,
                        onTap: _toggleSpeaker,
                      ),
                      _CallAction(
                        icon: Icons.call_end_rounded,
                        label: '挂断',
                        color: const Color(0xFFD83A32),
                        onTap: _hangUp,
                      ),
                    ],
                  ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}

class _CallAction extends StatelessWidget {
  const _CallAction({
    required this.icon,
    required this.label,
    required this.onTap,
    this.selected = false,
    this.color,
  });

  final IconData icon;
  final String label;
  final VoidCallback onTap;
  final bool selected;
  final Color? color;

  @override
  Widget build(BuildContext context) {
    final background =
        color ?? (selected ? Colors.white : const Color(0xFF303034));
    final foreground = color != null
        ? Colors.white
        : selected
        ? const Color(0xFF171719)
        : Colors.white;
    return Column(
      children: [
        Material(
          color: background,
          shape: const CircleBorder(),
          child: InkWell(
            customBorder: const CircleBorder(),
            onTap: onTap,
            child: SizedBox(
              width: 62,
              height: 62,
              child: Icon(icon, color: foreground, size: 27),
            ),
          ),
        ),
        const SizedBox(height: 9),
        Text(label, style: const TextStyle(color: Colors.white70)),
      ],
    );
  }
}

String _cleanError(Object error) {
  final value = '$error';
  return value.startsWith('Exception: ') ? value.substring(11) : value;
}
