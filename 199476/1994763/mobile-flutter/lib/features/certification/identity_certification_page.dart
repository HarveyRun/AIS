import 'dart:io';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:image_picker/image_picker.dart';

import '../../app/providers.dart';
import '../../core/theme/app_status_style.dart';
import '../../core/widgets/app_message.dart';
import '../../data/models/certification_models.dart';
import '../../data/repositories/app_repository.dart';
import 'material_viewer.dart';

class IdentityCertificationPage extends ConsumerStatefulWidget {
  const IdentityCertificationPage({super.key});

  @override
  ConsumerState<IdentityCertificationPage> createState() => _State();
}

class _State extends ConsumerState<IdentityCertificationPage> {
  static const _requirements = <_IdentityRequirement>[
    _IdentityRequirement(
      title: '身份证正面',
      subtitle: '保持证件完整、文字清晰',
      icon: Icons.badge_outlined,
    ),
    _IdentityRequirement(
      title: '身份证反面',
      subtitle: '保持证件完整、有效期清晰',
      icon: Icons.credit_card_outlined,
    ),
    _IdentityRequirement(
      title: '本人手持身份证',
      subtitle: '本人面部与证件同时清晰入镜',
      icon: Icons.face_retouching_natural_outlined,
    ),
  ];

  final _picker = ImagePicker();
  final List<XFile?> _photos = List<XFile?>.filled(3, null);
  CertificationRecord? _record;
  bool _loading = true;
  bool _submitting = false;

  bool get _editable =>
      _record == null || _record!.status.toUpperCase() == 'REJECTED';
  int get _completedCount => _photos.whereType<XFile>().length;
  bool get _allCompleted => _completedCount == _requirements.length;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    try {
      final items = await ref.read(repositoryProvider).certifications();
      if (!mounted) return;
      final identities = items
          .where((item) => item.type == 'IDENTITY')
          .toList(growable: false);
      setState(() {
        _record = identities.firstOrNull;
        _loading = false;
      });
    } catch (error) {
      if (!mounted) return;
      setState(() => _loading = false);
      AppMessage.show(context, '$error');
    }
  }

  Future<void> _capture(int index) async {
    final photo = await _picker.pickImage(
      source: ImageSource.camera,
      imageQuality: 90,
      maxWidth: 2400,
      preferredCameraDevice: index == 2
          ? CameraDevice.front
          : CameraDevice.rear,
    );
    if (photo != null && mounted) setState(() => _photos[index] = photo);
  }

  Future<void> _submit() async {
    if (!_allCompleted || _submitting) return;
    setState(() => _submitting = true);
    try {
      await ref
          .read(repositoryProvider)
          .submitIdentityCertification(
            _photos
                .whereType<XFile>()
                .map((photo) => UploadFile(path: photo.path, name: photo.name))
                .toList(growable: false),
          );
      if (!mounted) return;
      AppMessage.show(context, '实名认证已提交审核');
      await _load();
    } catch (error) {
      if (mounted) AppMessage.show(context, '$error');
    } finally {
      if (mounted) setState(() => _submitting = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final colors = Theme.of(context).colorScheme;
    return Scaffold(
      appBar: AppBar(title: const Text('实名认证')),
      body: _loading
          ? const SizedBox.shrink()
          : ListView(
              padding: EdgeInsets.fromLTRB(10, 8, 10, _editable ? 116 : 28),
              children: [
                Container(
                  padding: const EdgeInsets.all(17),
                  decoration: BoxDecoration(
                    color: colors.primary.withValues(alpha: .07),
                    borderRadius: BorderRadius.circular(20),
                  ),
                  child: Row(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Container(
                        width: 44,
                        height: 44,
                        decoration: BoxDecoration(
                          color: colors.primary,
                          borderRadius: BorderRadius.circular(14),
                        ),
                        child: Icon(
                          Icons.verified_user_rounded,
                          color: colors.onPrimary,
                          size: 23,
                        ),
                      ),
                      const SizedBox(width: 13),
                      Expanded(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text(
                              '完成身份核验',
                              style: Theme.of(context).textTheme.titleMedium
                                  ?.copyWith(fontWeight: FontWeight.w800),
                            ),
                            const SizedBox(height: 5),
                            Text(
                              '提现和入驻严选直聊前需要完成实名认证。',
                              style: Theme.of(context).textTheme.bodySmall
                                  ?.copyWith(
                                    color: colors.onSurfaceVariant,
                                    height: 1.55,
                                  ),
                            ),
                          ],
                        ),
                      ),
                    ],
                  ),
                ),
                if (_record != null) ...[
                  const SizedBox(height: 12),
                  _StatusPanel(record: _record!),
                ],
                const SizedBox(height: 22),
                Padding(
                  padding: const EdgeInsets.symmetric(horizontal: 3),
                  child: Row(
                    children: [
                      Expanded(
                        child: Text(
                          _editable ? '请依次完成拍摄' : '已提交的认证材料',
                          style: Theme.of(context).textTheme.titleMedium
                              ?.copyWith(fontWeight: FontWeight.w800),
                        ),
                      ),
                      if (_editable)
                        Container(
                          padding: const EdgeInsets.symmetric(
                            horizontal: 9,
                            vertical: 4,
                          ),
                          decoration: BoxDecoration(
                            color: _allCompleted
                                ? const Color(0xFF27865C).withValues(alpha: .11)
                                : colors.surfaceContainerHighest,
                            borderRadius: BorderRadius.circular(10),
                          ),
                          child: Text(
                            '$_completedCount/${_requirements.length}',
                            style: Theme.of(context).textTheme.labelMedium
                                ?.copyWith(
                                  color: _allCompleted
                                      ? const Color(0xFF27865C)
                                      : colors.onSurfaceVariant,
                                  fontWeight: FontWeight.w700,
                                ),
                          ),
                        ),
                    ],
                  ),
                ),
                const SizedBox(height: 10),
                if (_editable)
                  for (var index = 0; index < _requirements.length; index++)
                    Padding(
                      padding: const EdgeInsets.only(bottom: 10),
                      child: _CaptureItem(
                        index: index,
                        requirement: _requirements[index],
                        photo: _photos[index],
                        onTap: () => _capture(index),
                      ),
                    )
                else if (_record!.materials.isNotEmpty)
                  for (final material in _record!.materials)
                    Padding(
                      padding: const EdgeInsets.only(bottom: 10),
                      child: _SubmittedItem(material: material),
                    )
                else
                  Container(
                    padding: const EdgeInsets.symmetric(vertical: 28),
                    alignment: Alignment.center,
                    decoration: BoxDecoration(
                      color: colors.surfaceContainerLow,
                      borderRadius: BorderRadius.circular(16),
                    ),
                    child: Text(
                      '暂无认证材料',
                      style: TextStyle(color: colors.onSurfaceVariant),
                    ),
                  ),
              ],
            ),
      bottomNavigationBar: !_editable
          ? null
          : Material(
              color: colors.surface,
              child: SafeArea(
                minimum: const EdgeInsets.fromLTRB(10, 10, 10, 10),
                child: SizedBox(
                  height: 48,
                  child: FilledButton(
                    onPressed: _allCompleted && !_submitting ? _submit : null,
                    child: Text(
                      _submitting
                          ? '提交中…'
                          : _allCompleted
                          ? '提交实名认证'
                          : '请完成全部拍摄',
                    ),
                  ),
                ),
              ),
            ),
    );
  }
}

class _IdentityRequirement {
  const _IdentityRequirement({
    required this.title,
    required this.subtitle,
    required this.icon,
  });

  final String title;
  final String subtitle;
  final IconData icon;
}

class _StatusPanel extends StatelessWidget {
  const _StatusPanel({required this.record});

  final CertificationRecord record;

  @override
  Widget build(BuildContext context) {
    final style = appStatusStyle(context, record.status);
    final status = record.status.toUpperCase();
    final label = switch (status) {
      'APPROVED' => '实名认证已通过',
      'REJECTED' => '认证未通过，可重新提交',
      _ => '资料审核中',
    };
    final detail = switch (status) {
      'APPROVED' => '身份信息已完成平台核验',
      'REJECTED' =>
        record.rejectionReason.isEmpty
            ? '请重新拍摄清晰、完整的认证照片'
            : record.rejectionReason,
      _ => '资料已提交，审核结果会通过通知告知',
    };
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 13),
      decoration: BoxDecoration(
        color: style.background,
        borderRadius: BorderRadius.circular(16),
      ),
      child: Row(
        children: [
          Icon(
            status == 'APPROVED'
                ? Icons.check_circle_rounded
                : status == 'REJECTED'
                ? Icons.info_rounded
                : Icons.schedule_rounded,
            color: style.foreground,
            size: 22,
          ),
          const SizedBox(width: 10),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  label,
                  style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                    color: style.foreground,
                    fontWeight: FontWeight.w800,
                  ),
                ),
                const SizedBox(height: 3),
                Text(
                  detail,
                  style: Theme.of(context).textTheme.bodySmall?.copyWith(
                    color: style.foreground.withValues(alpha: .82),
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

class _CaptureItem extends StatelessWidget {
  const _CaptureItem({
    required this.index,
    required this.requirement,
    required this.photo,
    required this.onTap,
  });

  final int index;
  final _IdentityRequirement requirement;
  final XFile? photo;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final colors = Theme.of(context).colorScheme;
    final completed = photo != null;
    return Material(
      color: colors.surfaceContainerLow,
      borderRadius: BorderRadius.circular(17),
      clipBehavior: Clip.antiAlias,
      child: InkWell(
        onTap: onTap,
        child: Padding(
          padding: const EdgeInsets.all(13),
          child: Row(
            children: [
              Container(
                width: 34,
                height: 34,
                alignment: Alignment.center,
                decoration: BoxDecoration(
                  color: completed
                      ? const Color(0xFF27865C).withValues(alpha: .12)
                      : colors.primary.withValues(alpha: .09),
                  borderRadius: BorderRadius.circular(11),
                ),
                child: completed
                    ? const Icon(
                        Icons.check_rounded,
                        size: 19,
                        color: Color(0xFF27865C),
                      )
                    : Text(
                        '${index + 1}',
                        style: TextStyle(
                          color: colors.primary,
                          fontWeight: FontWeight.w800,
                        ),
                      ),
              ),
              const SizedBox(width: 11),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      requirement.title,
                      style: Theme.of(context).textTheme.bodyLarge?.copyWith(
                        fontWeight: FontWeight.w700,
                      ),
                    ),
                    const SizedBox(height: 4),
                    Text(
                      completed ? '已拍摄，点击可重新拍摄' : requirement.subtitle,
                      maxLines: 2,
                      overflow: TextOverflow.ellipsis,
                      style: Theme.of(context).textTheme.bodySmall?.copyWith(
                        color: colors.onSurfaceVariant,
                      ),
                    ),
                  ],
                ),
              ),
              const SizedBox(width: 10),
              ClipRRect(
                borderRadius: BorderRadius.circular(11),
                child: Container(
                  width: 76,
                  height: 54,
                  color: colors.surfaceContainerHighest,
                  child: completed
                      ? Image.file(File(photo!.path), fit: BoxFit.cover)
                      : Icon(
                          requirement.icon,
                          color: colors.onSurfaceVariant,
                          size: 25,
                        ),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _SubmittedItem extends StatelessWidget {
  const _SubmittedItem({required this.material});

  final CertificationMaterial material;

  @override
  Widget build(BuildContext context) {
    final colors = Theme.of(context).colorScheme;
    return Material(
      color: colors.surfaceContainerLow,
      borderRadius: BorderRadius.circular(16),
      clipBehavior: Clip.antiAlias,
      child: InkWell(
        onTap: material.url.isEmpty
            ? null
            : () => openMaterial(context, material),
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 13),
          child: Row(
            children: [
              Container(
                width: 40,
                height: 40,
                decoration: BoxDecoration(
                  color: colors.primary.withValues(alpha: .08),
                  borderRadius: BorderRadius.circular(12),
                ),
                child: Icon(
                  _materialIcon(material.kind),
                  color: colors.primary,
                  size: 21,
                ),
              ),
              const SizedBox(width: 11),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      _materialName(material.kind),
                      style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                        fontWeight: FontWeight.w700,
                      ),
                    ),
                    const SizedBox(height: 3),
                    Text(
                      '点击查看已提交照片',
                      style: Theme.of(context).textTheme.bodySmall?.copyWith(
                        color: colors.onSurfaceVariant,
                      ),
                    ),
                  ],
                ),
              ),
              Icon(Icons.chevron_right_rounded, color: colors.onSurfaceVariant),
            ],
          ),
        ),
      ),
    );
  }

  String _materialName(String kind) => switch (kind) {
    'IDENTITY_FRONT' => '身份证正面',
    'IDENTITY_BACK' => '身份证反面',
    'IDENTITY_HANDHELD' => '本人手持身份证',
    _ => '认证照片',
  };

  IconData _materialIcon(String kind) => switch (kind) {
    'IDENTITY_FRONT' => Icons.badge_outlined,
    'IDENTITY_BACK' => Icons.credit_card_outlined,
    'IDENTITY_HANDHELD' => Icons.face_retouching_natural_outlined,
    _ => Icons.image_outlined,
  };
}
