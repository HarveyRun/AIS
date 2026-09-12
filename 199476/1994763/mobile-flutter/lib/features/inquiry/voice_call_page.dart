import 'dart:async';
import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_webrtc/flutter_webrtc.dart';
import 'package:flutter/services.dart';
import 'package:go_router/go_router.dart';

import '../../app/providers.dart';
import '../../core/network/realtime_service.dart';
import '../../core/widgets/app_avatar.dart';
import '../../core/widgets/app_message.dart';
import '../../data/models/inquiry_models.dart';

class VoiceCallPage extends ConsumerStatefulWidget {
  const VoiceCallPage({
    super.key,
    required this.inquiryId,
    required this.voiceCallId,
    required this.initiator,
  });

  final int inquiryId;
  final int voiceCallId;
  final bool initiator;

  @override
  ConsumerState<VoiceCallPage> createState() => _VoiceCallPageState();
}

class _VoiceCallPageState extends ConsumerState<VoiceCallPage> {
  static const _voiceCallChannel = MethodChannel('com.shixianwen/voice_call');
  RTCPeerConnection? _peerConnection;
  MediaStream? _localStream;
  StreamSubscription<RealtimeEvent>? _realtimeSubscription;
  Timer? _clock;
  InquirySummary? _inquiry;
  VoiceCall? _voiceCall;
  VoiceIceConfig? _iceConfig;
  final List<VoiceSignal> _earlySignals = [];
  final List<RTCIceCandidate> _pendingCandidates = [];
  final Set<int> _handledSignalIds = {};
  bool _peerReady = false;
  bool _remoteDescriptionSet = false;
  bool _muted = false;
  bool _speakerOn = false;
  String _audioOutput = '听筒';
  bool _closing = false;
  bool _connectionReported = false;
  bool _reconnectRunning = false;
  bool _incomingAwaitingAnswer = false;
  bool _mediaInitializing = false;
  String _stateText = '正在准备语音通话';

  @override
  void initState() {
    super.initState();
    _realtimeSubscription = ref
        .read(realtimeProvider)
        .events
        .listen(_handleRealtimeEvent);
    unawaited(_initialize());
  }

  Future<void> _initialize() async {
    try {
      final repository = ref.read(repositoryProvider);
      final values = await Future.wait<Object>([
        repository.inquiry(widget.inquiryId, showLoading: false),
        repository.latestVoiceCall(widget.inquiryId),
        repository.voiceIceConfig(widget.inquiryId, widget.voiceCallId),
      ]);
      final detail = values[0] as InquiryDetail;
      final voiceCall = values[1] as VoiceCall;
      final iceConfig = values[2] as VoiceIceConfig;
      if (voiceCall.id != widget.voiceCallId ||
          !const {
            'CONNECTING',
            'ACTIVE',
          }.contains(voiceCall.status.toUpperCase())) {
        throw Exception('本次语音通话已不可用');
      }
      if (!mounted) return;
      setState(() {
        _inquiry = detail.inquiry;
        _voiceCall = voiceCall;
        _iceConfig = iceConfig;
        _incomingAwaitingAnswer =
            !widget.initiator &&
            voiceCall.status.toUpperCase() == 'CONNECTING' &&
            voiceCall.acceptedAt == null;
        _stateText = _incomingAwaitingAnswer ? '语音来电' : _stateText;
      });
      if (_incomingAwaitingAnswer) return;
      await _initializeMedia();
    } catch (error) {
      if (!mounted) return;
      AppMessage.show(context, _cleanError(error));
      await _finish(remote: true);
    }
  }

  Future<void> _initializeMedia() async {
    if (_peerReady || _mediaInitializing || _closing) return;
    _mediaInitializing = true;
    try {
      final repository = ref.read(repositoryProvider);
      final iceConfig = _iceConfig;
      if (iceConfig == null) throw Exception('无法读取语音连接配置');
      final servers = <Map<String, dynamic>>[];
      if (iceConfig.urls.isNotEmpty) {
        final server = <String, dynamic>{'urls': iceConfig.urls};
        if (iceConfig.username.isNotEmpty) {
          server['username'] = iceConfig.username;
        }
        if (iceConfig.credential.isNotEmpty) {
          server['credential'] = iceConfig.credential;
        }
        servers.add(server);
      }
      _peerConnection = await createPeerConnection({
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
      await _startForegroundCall();
      await repository.joinVoiceCall(widget.inquiryId, widget.voiceCallId);
      for (final track in _localStream!.getAudioTracks()) {
        await _peerConnection!.addTrack(track, _localStream!);
      }
      _peerConnection!.onIceCandidate = (candidate) {
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
      _peerConnection!.onTrack = (event) {
        event.track.enabled = true;
      };
      _peerConnection!.onConnectionState = (state) {
        if (!mounted) return;
        setState(() {
          _stateText = switch (state) {
            RTCPeerConnectionState.RTCPeerConnectionStateConnected => '通话中',
            RTCPeerConnectionState.RTCPeerConnectionStateConnecting => '正在连接',
            RTCPeerConnectionState.RTCPeerConnectionStateDisconnected =>
              '连接已中断',
            RTCPeerConnectionState.RTCPeerConnectionStateFailed => '连接失败',
            RTCPeerConnectionState.RTCPeerConnectionStateClosed => '通话已结束',
            _ => _stateText,
          };
        });
        if (state == RTCPeerConnectionState.RTCPeerConnectionStateConnected) {
          _connectionReported = true;
          _reconnectRunning = false;
          unawaited(_reportConnected());
        } else if (state ==
                RTCPeerConnectionState.RTCPeerConnectionStateDisconnected ||
            state == RTCPeerConnectionState.RTCPeerConnectionStateFailed) {
          unawaited(_handleDisconnected(state.name));
        }
      };
      await Helper.setSpeakerphoneOn(false);
      _peerReady = true;

      final queued = List<VoiceSignal>.from(_earlySignals);
      _earlySignals.clear();
      for (final signal in queued) {
        await _handleSignal(signal);
      }
      final pending = await repository.voiceSignals(
        widget.inquiryId,
        widget.voiceCallId,
      );
      for (final signal in pending) {
        await _handleSignal(signal);
      }
      if (widget.initiator) await _createOffer();
      _clock = Timer.periodic(const Duration(seconds: 1), (_) {
        if (!mounted) return;
        final endAt = _voiceCall?.maxEndAt;
        if (endAt != null && !DateTime.now().isBefore(endAt)) {
          unawaited(_finishAtTimeLimit());
        } else {
          setState(() {});
        }
      });
    } catch (error) {
      try {
        await ref
            .read(repositoryProvider)
            .markVoiceCallDisconnected(
              widget.inquiryId,
              widget.voiceCallId,
              'INITIALIZE_FAILED:${_cleanError(error)}',
            );
      } catch (_) {}
      if (!mounted) return;
      AppMessage.show(context, _cleanError(error));
      await _finish(remote: true);
    } finally {
      _mediaInitializing = false;
    }
  }

  Future<void> _answerIncoming() async {
    if (!_incomingAwaitingAnswer || _mediaInitializing || _closing) return;
    setState(() {
      _mediaInitializing = true;
      _stateText = '正在接通';
    });
    try {
      final answered = await ref
          .read(repositoryProvider)
          .answerVoiceCall(widget.inquiryId, widget.voiceCallId);
      if (!mounted) return;
      setState(() {
        _voiceCall = answered;
        _incomingAwaitingAnswer = false;
        _mediaInitializing = false;
      });
      await _initializeMedia();
    } catch (error) {
      if (!mounted) return;
      setState(() {
        _mediaInitializing = false;
        _stateText = '语音来电';
      });
      AppMessage.show(context, _cleanError(error));
    }
  }

  Future<void> _rejectIncoming() async {
    if (_closing) return;
    try {
      await ref
          .read(repositoryProvider)
          .rejectVoiceCall(widget.inquiryId, widget.voiceCallId);
    } catch (_) {
      // 对方已取消或呼叫已超时，直接关闭来电页。
    }
    await _finish(remote: true);
  }

  void _handleRealtimeEvent(RealtimeEvent event) {
    if (event.type == 'VOICE_CALL_ENDED') {
      final voiceCallId = int.tryParse('${event.payload['voiceCallId'] ?? ''}');
      if (voiceCallId == widget.voiceCallId) {
        unawaited(_finish(remote: true));
      }
      return;
    }
    if (event.type != 'VOICE_SIGNAL') return;
    final signal = VoiceSignal.fromJson(event.payload);
    if (signal.inquiryId != widget.inquiryId ||
        signal.voiceCallId != widget.voiceCallId) {
      return;
    }
    if (!_peerReady) {
      _earlySignals.add(signal);
      return;
    }
    unawaited(_handleSignal(signal));
  }

  Future<void> _createOffer() async {
    if (_peerConnection == null) return;
    if (mounted) setState(() => _stateText = '正在呼叫对方');
    final offer = await _peerConnection!.createOffer({
      'offerToReceiveAudio': true,
      'offerToReceiveVideo': false,
    });
    await _peerConnection!.setLocalDescription(offer);
    await _sendSignal(
      'OFFER',
      jsonEncode({'sdp': offer.sdp, 'type': offer.type}),
    );
  }

  Future<void> _reportConnected() async {
    try {
      final updated = await ref
          .read(repositoryProvider)
          .markVoiceCallConnected(widget.inquiryId, widget.voiceCallId);
      if (!mounted) return;
      setState(() {
        _voiceCall = updated;
        _stateText = updated.status.toUpperCase() == 'ACTIVE'
            ? '通话中'
            : '正在确认双方连接';
      });
    } catch (error) {
      if (mounted) setState(() => _stateText = '正在确认连接');
    }
  }

  Future<void> _handleDisconnected(String reason) async {
    if (_closing) return;
    _connectionReported = false;
    try {
      await ref
          .read(repositoryProvider)
          .markVoiceCallDisconnected(
            widget.inquiryId,
            widget.voiceCallId,
            reason,
          );
    } catch (_) {
      // 断网时上报可能失败，本地仍继续尝试恢复连接。
    }
    if (!widget.initiator || _reconnectRunning || _closing) return;
    _reconnectRunning = true;
    for (var attempt = 1; attempt <= 3 && !_closing; attempt++) {
      await Future<void>.delayed(Duration(seconds: attempt * 2));
      if (_connectionReported || _peerConnection == null) break;
      if (mounted) setState(() => _stateText = '正在重新连接（$attempt/3）');
      try {
        final offer = await _peerConnection!.createOffer({
          'offerToReceiveAudio': true,
          'offerToReceiveVideo': false,
          'iceRestart': true,
        });
        await _peerConnection!.setLocalDescription(offer);
        await _sendSignal(
          'OFFER',
          jsonEncode({
            'sdp': offer.sdp,
            'type': offer.type,
            'iceRestart': true,
          }),
        );
      } catch (_) {
        // 继续下一次弱网重连。
      }
    }
    _reconnectRunning = false;
  }

  Future<void> _handleSignal(VoiceSignal signal) async {
    if (_handledSignalIds.contains(signal.id) || _closing) return;
    _handledSignalIds.add(signal.id);
    final payload = jsonDecode(signal.payload);
    if (payload is! Map) return;
    final data = Map<String, dynamic>.from(payload);
    switch (signal.signalType.toUpperCase()) {
      case 'OFFER':
        if (widget.initiator || _peerConnection == null) return;
        await _peerConnection!.setRemoteDescription(
          RTCSessionDescription(data['sdp']?.toString(), 'offer'),
        );
        _remoteDescriptionSet = true;
        await _flushCandidates();
        final answer = await _peerConnection!.createAnswer({
          'offerToReceiveAudio': true,
          'offerToReceiveVideo': false,
        });
        await _peerConnection!.setLocalDescription(answer);
        await _sendSignal(
          'ANSWER',
          jsonEncode({'sdp': answer.sdp, 'type': answer.type}),
        );
        if (mounted) setState(() => _stateText = '正在连接');
      case 'ANSWER':
        if (!widget.initiator || _peerConnection == null) return;
        await _peerConnection!.setRemoteDescription(
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
          await _peerConnection?.addCandidate(candidate);
        } else {
          _pendingCandidates.add(candidate);
        }
      case 'HANGUP':
        await _finish(remote: true);
    }
  }

  Future<void> _flushCandidates() async {
    final queued = List<RTCIceCandidate>.from(_pendingCandidates);
    _pendingCandidates.clear();
    for (final candidate in queued) {
      await _peerConnection?.addCandidate(candidate);
    }
  }

  Future<void> _sendSignal(String type, String payload) async {
    try {
      await ref
          .read(repositoryProvider)
          .sendVoiceSignal(
            inquiryId: widget.inquiryId,
            voiceCallId: widget.voiceCallId,
            signalType: type,
            payload: payload,
          );
    } catch (error) {
      if (mounted && type != 'HANGUP') {
        AppMessage.show(context, _cleanError(error));
      }
    }
  }

  Future<void> _toggleMute() async {
    _muted = !_muted;
    for (final track in _localStream?.getAudioTracks() ?? const []) {
      track.enabled = !_muted;
    }
    if (mounted) setState(() {});
  }

  Future<void> _chooseAudioOutput() async {
    final output = await showModalBottomSheet<String>(
      context: context,
      builder: (context) => SafeArea(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            ListTile(
              leading: const Icon(Icons.hearing_rounded),
              title: const Text('听筒'),
              onTap: () => Navigator.pop(context, '听筒'),
            ),
            ListTile(
              leading: const Icon(Icons.bluetooth_audio_rounded),
              title: const Text('蓝牙耳机优先'),
              onTap: () => Navigator.pop(context, '蓝牙耳机'),
            ),
            ListTile(
              leading: const Icon(Icons.volume_up_rounded),
              title: const Text('免提'),
              onTap: () => Navigator.pop(context, '免提'),
            ),
          ],
        ),
      ),
    );
    if (output == null) return;
    if (output == '免提') {
      await Helper.setSpeakerphoneOn(true);
      _speakerOn = true;
    } else if (output == '蓝牙耳机') {
      await Helper.setSpeakerphoneOnButPreferBluetooth();
      _speakerOn = false;
    } else {
      await Helper.setSpeakerphoneOn(false);
      _speakerOn = false;
    }
    if (mounted) setState(() => _audioOutput = output);
  }

  Future<void> _startForegroundCall() async {
    try {
      await _voiceCallChannel.invokeMethod<void>('startForegroundCall');
    } catch (_) {
      // 非 Android 平台或系统不支持时不阻断通话。
    }
  }

  Future<void> _stopForegroundCall() async {
    try {
      await _voiceCallChannel.invokeMethod<void>('stopForegroundCall');
    } catch (_) {}
  }

  Future<void> _hangUp() async {
    if (_closing) return;
    if (_incomingAwaitingAnswer) {
      await _rejectIncoming();
      return;
    }
    await _sendSignal('HANGUP', jsonEncode({'reason': 'USER_HANGUP'}));
    try {
      await ref
          .read(repositoryProvider)
          .finishVoiceCall(widget.inquiryId, widget.voiceCallId);
    } catch (_) {
      // 对端已经结束或计时任务已完成时，直接关闭本地通话页。
    }
    await _finish(remote: true);
  }

  Future<void> _finishAtTimeLimit() async {
    if (_closing) return;
    try {
      await ref
          .read(repositoryProvider)
          .finishVoiceCall(widget.inquiryId, widget.voiceCallId);
    } catch (_) {
      // 服务端定时任务可能已经先一步完成通话。
    }
    await _finish(remote: true);
  }

  Future<void> _finish({required bool remote}) async {
    if (_closing) return;
    _closing = true;
    _clock?.cancel();
    await _realtimeSubscription?.cancel();
    for (final track in _localStream?.getTracks() ?? const []) {
      track.stop();
    }
    await _localStream?.dispose();
    await _peerConnection?.close();
    await _peerConnection?.dispose();
    await Helper.setSpeakerphoneOn(false);
    await _stopForegroundCall();
    if (mounted && context.canPop()) context.pop();
  }

  String get _elapsedText {
    final connectedAt = _voiceCall?.connectedAt;
    if (connectedAt == null) return '00:00';
    final seconds = DateTime.now()
        .difference(connectedAt)
        .inSeconds
        .clamp(0, 1728000);
    final hours = seconds ~/ 3600;
    final minutes = (seconds % 3600) ~/ 60;
    final rest = seconds % 60;
    if (hours > 0) {
      return '${hours.toString().padLeft(2, '0')}:${minutes.toString().padLeft(2, '0')}:${rest.toString().padLeft(2, '0')}';
    }
    return '${minutes.toString().padLeft(2, '0')}:${rest.toString().padLeft(2, '0')}';
  }

  @override
  void dispose() {
    if (!_closing) {
      _clock?.cancel();
      _realtimeSubscription?.cancel();
      for (final track in _localStream?.getTracks() ?? const []) {
        track.stop();
      }
      _localStream?.dispose();
      _peerConnection?.close();
      _peerConnection?.dispose();
      Helper.setSpeakerphoneOn(false);
      _stopForegroundCall();
    }
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final colorScheme = Theme.of(context).colorScheme;
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
                    icon: const Icon(Icons.keyboard_arrow_down_rounded),
                    color: Colors.white,
                    iconSize: 32,
                  ),
                ),
                const Spacer(flex: 2),
                AppAvatar(
                  url: _inquiry?.otherAvatar,
                  name: _inquiry?.otherName ?? '',
                  radius: 48,
                ),
                const SizedBox(height: 20),
                Text(
                  _inquiry?.otherName.isNotEmpty == true
                      ? _inquiry!.otherName
                      : '对方',
                  style: const TextStyle(
                    color: Colors.white,
                    fontSize: 24,
                    fontWeight: FontWeight.w700,
                  ),
                ),
                const SizedBox(height: 10),
                Text(
                  '$_stateText · $_elapsedText',
                  style: const TextStyle(
                    color: Color(0xFFB8B8BD),
                    fontSize: 15,
                  ),
                ),
                const Spacer(flex: 3),
                if (_incomingAwaitingAnswer)
                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceEvenly,
                    children: [
                      _CallAction(
                        icon: Icons.call_end_rounded,
                        label: '拒绝',
                        color: colorScheme.error,
                        onTap: _rejectIncoming,
                      ),
                      _CallAction(
                        icon: Icons.call_rounded,
                        label: '接听',
                        color: const Color(0xFF28A745),
                        onTap: _mediaInitializing ? () {} : _answerIncoming,
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
                        icon: _speakerOn
                            ? Icons.volume_up_rounded
                            : Icons.hearing_rounded,
                        label: _audioOutput,
                        selected: _speakerOn,
                        onTap: _chooseAudioOutput,
                      ),
                      _CallAction(
                        icon: Icons.call_end_rounded,
                        label: '挂断',
                        color: colorScheme.error,
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
  final text = '$error';
  return text.startsWith('Exception: ') ? text.substring(11) : text;
}
