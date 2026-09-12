import 'dart:async';
import 'dart:math';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:image_picker/image_picker.dart';
import 'package:tobias/tobias.dart' as tobias;
import 'package:url_launcher/url_launcher.dart';
import '../../app/providers.dart';
import '../../core/config/app_config.dart';
import '../../core/network/realtime_service.dart';
import '../../core/widgets/app_message.dart';
import '../../data/models/curated_chat_models.dart';
import '../../data/repositories/app_repository.dart';
import 'curated_members_page.dart';
import 'curated_material_preview.dart';

class CuratedMembershipPage extends ConsumerStatefulWidget {
  const CuratedMembershipPage({super.key});
  @override
  ConsumerState<CuratedMembershipPage> createState() => _State();
}

class _State extends ConsumerState<CuratedMembershipPage>
    with WidgetsBindingObserver {
  CuratedMembership? value;
  List<CuratedCertificationMaterial> materials = const [];
  String? error;
  StreamSubscription<RealtimeEvent>? subscription;
  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
    _load();
    subscription = ref.read(realtimeProvider).events.listen((event) {
      if (event.type == 'CURATED_APPLICATION_UPDATED' ||
          event.type == 'CURATED_MEMBERSHIP_UPDATED' ||
          event.type == 'APP_GLOBAL_SETTINGS_UPDATED') {
        _load(silent: true);
      }
    });
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (state == AppLifecycleState.resumed) {
      _load(silent: true);
    }
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    subscription?.cancel();
    super.dispose();
  }

  Future<void> _load({bool silent = false}) async {
    try {
      final v = await ref
          .read(repositoryProvider)
          .curatedMembership(showLoading: !silent);
      final loadedMaterials = v.applied
          ? await ref
                .read(repositoryProvider)
                .curatedMaterials(showLoading: false)
          : const <CuratedCertificationMaterial>[];
      if (mounted) {
        setState(() {
          value = v;
          materials = loadedMaterials;
          error = null;
        });
      }
    } catch (e) {
      if (mounted) setState(() => error = '$e');
    }
  }

  @override
  Widget build(BuildContext context) {
    final v = value;
    if (v?.active == true) return const CuratedMembersPage();
    return Scaffold(
      appBar: AppBar(title: const Text('严选直聊')),
      body: v == null
          ? Center(
              child: error == null
                  ? const CircularProgressIndicator(strokeWidth: 2)
                  : Text(error!),
            )
          : ListView(
              padding: const EdgeInsets.all(16),
              children: [
                const _Intro(),
                const SizedBox(height: 20),
                _StatusCard(
                  value: v,
                  materials: materials,
                  onIdentityTap: () async {
                    await context.pushNamed('identityCertification');
                    if (mounted) await _load();
                  },
                ),
                const SizedBox(height: 24),
                if (!v.applied || v.status == 'REJECTED')
                  FilledButton(
                    onPressed: () async {
                      await context.push('/curated/apply');
                      _load();
                    },
                    child: Text(v.applied ? '重新提交未通过的认证' : '开始认证'),
                  ),
                if (!v.applied || v.status == 'REJECTED')
                  const SizedBox(height: 12),
                FilledButton(
                  onPressed: v.readyToPay ? _pay : null,
                  child: Text(
                    v.priceText,
                    style: const TextStyle(
                      fontSize: 16,
                      fontWeight: FontWeight.w700,
                    ),
                  ),
                ),
              ],
            ),
    );
  }

  Future<void> _pay() async {
    late final CuratedMembershipQuote quote;
    try {
      quote = await ref.read(repositoryProvider).curatedMembershipQuote();
    } catch (e) {
      if (mounted) AppMessage.show(context, '$e');
      return;
    }
    if (!mounted) return;
    final ok = await showDialog<bool>(
      context: context,
      builder: (c) => Dialog(
        insetPadding: const EdgeInsets.symmetric(horizontal: 28),
        child: Padding(
          padding: const EdgeInsets.fromLTRB(22, 16, 22, 22),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              Row(
                children: [
                  const Expanded(
                    child: Text(
                      '确认开通',
                      style: TextStyle(
                        fontSize: 20,
                        fontWeight: FontWeight.w700,
                      ),
                    ),
                  ),
                  IconButton(
                    onPressed: () => Navigator.pop(c, false),
                    icon: const Icon(Icons.close),
                    tooltip: '关闭',
                  ),
                ],
              ),
              const SizedBox(height: 18),
              _PayInfoRow(label: '开通方案', value: quote.priceText),
              const SizedBox(height: 14),
              _PayInfoRow(label: '有效期', value: quote.validityText),
              const SizedBox(height: 24),
              FilledButton(
                onPressed: () => Navigator.pop(c, true),
                child: const Text('确认支付'),
              ),
            ],
          ),
        ),
      ),
    );
    if (ok != true || !mounted) return;
    try {
      final id =
          'curated_${DateTime.now().millisecondsSinceEpoch}_${Random().nextInt(999999)}';
      final order = await ref
          .read(repositoryProvider)
          .createCuratedPayment(
            id,
            durationMonths: quote.durationMonths,
            price: quote.price,
          );
      if (order.status == 'PAID') {
        if (!mounted) return;
        AppMessage.show(context, '严选直聊已开通');
        await _load();
        return;
      }
      final payload = order.paymentPayload;
      if (payload.isEmpty) throw Exception('支付信息生成失败');
      if (payload.startsWith('/')) {
        await launchUrl(
          AppConfig.resolveResource(payload),
          mode: LaunchMode.externalApplication,
        );
      } else {
        final result = await tobias.Tobias().pay(payload);
        if ('${result['resultStatus']}' != '9000') throw Exception('支付未完成');
      }
      for (var i = 0; i < 6; i++) {
        await Future<void>.delayed(const Duration(seconds: 1));
        final latest = await ref
            .read(repositoryProvider)
            .curatedPayment(order.orderNo);
        if (latest.status == 'PAID') {
          if (mounted) AppMessage.show(context, '严选直聊已开通');
          await _load();
          return;
        }
      }
      throw Exception('支付结果确认中，请稍后刷新');
    } catch (e) {
      if (mounted) AppMessage.show(context, '$e');
    }
  }
}

class _PayInfoRow extends StatelessWidget {
  const _PayInfoRow({required this.label, required this.value});

  final String label;
  final String value;

  @override
  Widget build(BuildContext context) => Row(
    crossAxisAlignment: CrossAxisAlignment.start,
    children: [
      SizedBox(
        width: 70,
        child: Text(
          label,
          style: TextStyle(
            color: Theme.of(context).colorScheme.onSurfaceVariant,
          ),
        ),
      ),
      Expanded(
        child: Text(
          value,
          textAlign: TextAlign.right,
          style: const TextStyle(fontWeight: FontWeight.w600, height: 1.45),
        ),
      ),
    ],
  );
}

class _Intro extends StatelessWidget {
  const _Intro();
  @override
  Widget build(BuildContext context) => Container(
    padding: const EdgeInsets.fromLTRB(20, 19, 20, 20),
    decoration: BoxDecoration(
      color: Theme.of(context).colorScheme.primaryContainer,
      borderRadius: BorderRadius.circular(20),
    ),
    child: const Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          '申请资格',
          style: TextStyle(fontSize: 22, fontWeight: FontWeight.w800),
        ),
        SizedBox(height: 10),
        Text('请完成实名及岗位认证（岗位需具备10年以上从业经验）', style: TextStyle(height: 1.65)),
      ],
    ),
  );
}

class _StatusCard extends StatelessWidget {
  const _StatusCard({
    required this.value,
    required this.materials,
    required this.onIdentityTap,
  });
  final CuratedMembership value;
  final List<CuratedCertificationMaterial> materials;
  final VoidCallback onIdentityTap;
  @override
  Widget build(BuildContext context) {
    String text = switch (value.status) {
      'NOT_APPLIED' => '尚未提交认证',
      'UNDER_REVIEW' => '资料正在审核',
      'REJECTED' => '认证未通过',
      'READY_TO_PAY' => '认证已通过',
      'SUSPENDED' => '会员资格已暂停',
      _ => value.status,
    };
    return Container(
      padding: const EdgeInsets.all(18),
      decoration: BoxDecoration(
        color: Theme.of(context).colorScheme.surfaceContainerLow,
        borderRadius: BorderRadius.circular(18),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            text,
            style: const TextStyle(fontSize: 17, fontWeight: FontWeight.w700),
          ),
          const SizedBox(height: 14),
          _row('实名认证', value.identityStatus, value.identityRejectionReason),
          const SizedBox(height: 10),
          _row('岗位认证', value.jobStatus, value.jobRejectionReason),
          const SizedBox(height: 16),
          Row(
            children: [
              Expanded(
                child: OutlinedButton(
                  onPressed: onIdentityTap,
                  child: const Text('实名认证'),
                ),
              ),
              const SizedBox(width: 10),
              Expanded(
                child: OutlinedButton(
                  onPressed: materials.isEmpty
                      ? null
                      : () {
                          openSubmittedCuratedMaterials(
                            context,
                            '岗位资料',
                            materials,
                          );
                        },
                  child: const Text('查看岗位资料'),
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }

  Widget _row(String name, String status, String? reason) => Row(
    crossAxisAlignment: CrossAxisAlignment.start,
    children: [
      Expanded(child: Text(name)),
      Text(switch (status) {
        'APPROVED' => '已通过',
        'REJECTED' => '已驳回',
        'PENDING' => '审核中',
        _ => '未提交',
      }),
      if (reason?.isNotEmpty == true)
        Expanded(
          child: Padding(
            padding: const EdgeInsets.only(left: 8),
            child: Text(reason!, textAlign: TextAlign.right),
          ),
        ),
    ],
  );
}

class CuratedApplicationPage extends ConsumerStatefulWidget {
  const CuratedApplicationPage({super.key});
  @override
  ConsumerState<CuratedApplicationPage> createState() => _ApplyState();
}

class _ApplyState extends ConsumerState<CuratedApplicationPage> {
  final picker = ImagePicker();
  final List<XFile> jobPhotos = [];
  XFile? jobVideo;
  CuratedMembership? membership;
  List<CuratedCertificationMaterial> submittedMaterials = const [];
  bool loading = true;
  bool submitting = false;

  bool get jobEditable =>
      membership?.applied != true || membership?.jobStatus == 'REJECTED';

  @override
  void initState() {
    super.initState();
    loadApplication();
  }

  Future<void> loadApplication() async {
    try {
      final current = await ref.read(repositoryProvider).curatedMembership();
      final materials = current.applied
          ? await ref
                .read(repositoryProvider)
                .curatedMaterials(showLoading: false)
          : const <CuratedCertificationMaterial>[];
      if (!mounted) return;
      setState(() {
        membership = current;
        submittedMaterials = materials;
        loading = false;
      });
    } catch (error) {
      if (!mounted) return;
      setState(() => loading = false);
      AppMessage.show(context, '$error');
    }
  }

  Future<void> openIdentity() async {
    await context.pushNamed('identityCertification');
    if (mounted) await loadApplication();
  }

  Future<void> jobPhoto() async {
    if (!jobEditable) return;
    if (jobPhotos.length >= 10) {
      AppMessage.show(context, '岗位认证图片最多10张');
      return;
    }
    final x = await picker.pickImage(
      source: ImageSource.camera,
      imageQuality: 88,
    );
    if (x != null && mounted) {
      setState(() => jobPhotos.add(x));
    }
  }

  Future<void> video() async {
    if (!jobEditable) return;
    final x = await picker.pickVideo(
      source: ImageSource.camera,
      maxDuration: const Duration(minutes: 10),
    );
    if (x != null && mounted) {
      setState(() => jobVideo = x);
    }
  }

  Future<void> submit() async {
    if (jobEditable && jobPhotos.isEmpty && jobVideo == null) {
      AppMessage.show(context, '请先拍摄或录制岗位证明资料');
      return;
    }
    try {
      setState(() => submitting = true);
      await ref
          .read(repositoryProvider)
          .submitCuratedApplication(
            jobFiles: jobEditable
                ? <XFile>[
                    ...jobPhotos,
                    if (jobVideo != null) jobVideo!,
                  ].map((x) => UploadFile(path: x.path, name: x.name)).toList()
                : const [],
          );
      if (mounted) {
        AppMessage.show(context, '认证资料已提交');
        context.pop();
      }
    } catch (e) {
      if (mounted) AppMessage.show(context, '$e');
    } finally {
      if (mounted) setState(() => submitting = false);
    }
  }

  @override
  Widget build(BuildContext context) => Scaffold(
    appBar: AppBar(title: const Text('严选直聊认证')),
    body: loading || membership == null
        ? const SizedBox.shrink()
        : ListView(
            padding: const EdgeInsets.all(16),
            children: [
              _GlobalIdentitySection(
                status: membership!.identityStatus,
                reason: membership!.identityRejectionReason,
                onTap: openIdentity,
              ),
              const SizedBox(height: 16),
              if (jobEditable)
                _JobCaptureSection(
                  title: '岗位认证',
                  subtitle: membership!.jobStatus == 'REJECTED'
                      ? '已被驳回，请重新拍摄或录制岗位资料'
                      : '仅支持拍照或录制；图片最多10张，录像最长10分钟',
                  photos: jobPhotos,
                  videoFile: jobVideo,
                  onPhoto: jobPhoto,
                  onVideo: video,
                  onDeletePhoto: (i) => setState(() => jobPhotos.removeAt(i)),
                  onDeleteVideo: () => setState(() => jobVideo = null),
                  onPreviewPhoto: (file, index) => openLocalPhotoPreview(
                    context,
                    file.path,
                    title: '岗位图片 ${index + 1}',
                  ),
                  onPreviewVideo: (file) =>
                      openLocalVideoPreview(context, file.path, title: '岗位录像'),
                )
              else
                _ReadOnlyCertificationSection(
                  title: '岗位认证',
                  status: membership!.jobStatus,
                  materials: submittedMaterials
                      .where((item) => item.isJob)
                      .toList(growable: false),
                ),
              if (jobEditable) ...[
                const SizedBox(height: 26),
                FilledButton(
                  onPressed: submitting ? null : submit,
                  child: Text(submitting ? '正在提交' : '提交审核'),
                ),
              ],
            ],
          ),
  );
}

class _GlobalIdentitySection extends StatelessWidget {
  const _GlobalIdentitySection({
    required this.status,
    required this.reason,
    required this.onTap,
  });

  final String status;
  final String? reason;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final colors = Theme.of(context).colorScheme;
    final approved = status == 'APPROVED';
    final pending = status == 'PENDING';
    final rejected = status == 'REJECTED';
    final color = approved
        ? const Color(0xFF27865C)
        : rejected
        ? colors.error
        : colors.tertiary;
    final stateText = approved
        ? '已认证通过，仅可查看'
        : pending
        ? '审核中，仅可查看'
        : rejected
        ? (reason?.isNotEmpty == true ? reason! : '认证未通过，请重新提交')
        : '尚未完成实名认证';
    return Container(
      padding: const EdgeInsets.all(18),
      decoration: BoxDecoration(
        color: colors.surfaceContainerLow,
        borderRadius: BorderRadius.circular(18),
      ),
      child: Row(
        children: [
          Icon(
            approved
                ? Icons.check_circle_rounded
                : rejected
                ? Icons.info_rounded
                : pending
                ? Icons.schedule_rounded
                : Icons.verified_user_outlined,
            color: color,
          ),
          const SizedBox(width: 11),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Text(
                  '实名认证',
                  style: TextStyle(fontSize: 18, fontWeight: FontWeight.w700),
                ),
                const SizedBox(height: 4),
                Text(stateText, maxLines: 2, overflow: TextOverflow.ellipsis),
              ],
            ),
          ),
          TextButton(
            onPressed: onTap,
            child: Text(
              approved || pending
                  ? '查看'
                  : rejected
                  ? '重新认证'
                  : '去认证',
            ),
          ),
        ],
      ),
    );
  }
}

class _ReadOnlyCertificationSection extends StatelessWidget {
  const _ReadOnlyCertificationSection({
    required this.title,
    required this.status,
    required this.materials,
  });

  final String title;
  final String status;
  final List<CuratedCertificationMaterial> materials;

  @override
  Widget build(BuildContext context) {
    final colors = Theme.of(context).colorScheme;
    final approved = status == 'APPROVED';
    return Container(
      padding: const EdgeInsets.all(18),
      decoration: BoxDecoration(
        color: colors.surfaceContainerLow,
        borderRadius: BorderRadius.circular(18),
      ),
      child: Row(
        children: [
          Icon(
            approved ? Icons.check_circle_rounded : Icons.schedule_rounded,
            color: approved ? const Color(0xFF27865C) : colors.tertiary,
          ),
          const SizedBox(width: 11),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  title,
                  style: const TextStyle(
                    fontSize: 18,
                    fontWeight: FontWeight.w700,
                  ),
                ),
                const SizedBox(height: 4),
                Text(approved ? '已认证通过，仅可查看' : '审核中，仅可查看'),
              ],
            ),
          ),
          TextButton(
            onPressed: materials.isEmpty
                ? null
                : () => openSubmittedCuratedMaterials(
                    context,
                    '$title资料',
                    materials,
                  ),
            child: const Text('查看'),
          ),
        ],
      ),
    );
  }
}

class _JobCaptureSection extends StatelessWidget {
  const _JobCaptureSection({
    required this.title,
    required this.subtitle,
    required this.photos,
    required this.videoFile,
    required this.onPhoto,
    required this.onVideo,
    required this.onDeletePhoto,
    required this.onDeleteVideo,
    required this.onPreviewPhoto,
    required this.onPreviewVideo,
  });
  final String title, subtitle;
  final List<XFile> photos;
  final XFile? videoFile;
  final VoidCallback onPhoto;
  final VoidCallback onVideo;
  final void Function(int) onDeletePhoto;
  final VoidCallback onDeleteVideo;
  final void Function(XFile, int) onPreviewPhoto;
  final void Function(XFile) onPreviewVideo;
  @override
  Widget build(BuildContext context) => Container(
    padding: const EdgeInsets.all(18),
    decoration: BoxDecoration(
      color: Theme.of(context).colorScheme.surfaceContainerLow,
      borderRadius: BorderRadius.circular(18),
    ),
    child: Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          title,
          style: const TextStyle(fontSize: 18, fontWeight: FontWeight.w700),
        ),
        const SizedBox(height: 5),
        Text(subtitle, style: Theme.of(context).textTheme.bodySmall),
        const SizedBox(height: 14),
        Wrap(
          spacing: 10,
          children: [
            OutlinedButton.icon(
              onPressed: photos.length < 10 ? onPhoto : null,
              icon: const Icon(Icons.photo_camera_outlined),
              label: Text('拍照 ${photos.length}/10'),
            ),
            OutlinedButton.icon(
              onPressed: onVideo,
              icon: const Icon(Icons.videocam_outlined),
              label: Text(videoFile == null ? '录像' : '重新录制'),
            ),
          ],
        ),
        if (photos.isNotEmpty || videoFile != null) ...[
          const SizedBox(height: 12),
          ...photos.indexed.map(
            (e) => ListTile(
              onTap: () => onPreviewPhoto(e.$2, e.$1),
              contentPadding: EdgeInsets.zero,
              leading: const Icon(Icons.image_outlined),
              title: Text(
                '岗位图片 ${e.$1 + 1}',
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
              ),
              trailing: IconButton(
                onPressed: () => onDeletePhoto(e.$1),
                icon: const Icon(Icons.close),
              ),
            ),
          ),
          if (videoFile != null)
            ListTile(
              onTap: () => onPreviewVideo(videoFile!),
              contentPadding: EdgeInsets.zero,
              leading: const Icon(Icons.videocam_outlined),
              title: const Text('岗位录像（最长10分钟）'),
              trailing: IconButton(
                onPressed: onDeleteVideo,
                icon: const Icon(Icons.close),
              ),
            ),
        ],
      ],
    ),
  );
}
