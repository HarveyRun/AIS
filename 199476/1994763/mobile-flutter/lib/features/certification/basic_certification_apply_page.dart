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

class BasicCertificationApplyPage extends ConsumerStatefulWidget {
  const BasicCertificationApplyPage({super.key, this.record});

  final CertificationRecord? record;

  @override
  ConsumerState<BasicCertificationApplyPage> createState() =>
      _BasicCertificationApplyPageState();
}

class _BasicCertificationApplyPageState
    extends ConsumerState<BasicCertificationApplyPage> {
  static const _requirements = ['身份证正面', '身份证反面', '手持身份证'];

  final _picker = ImagePicker();
  final List<XFile?> _photos = List<XFile?>.filled(3, null);
  bool _submitting = false;

  bool get _editable =>
      widget.record == null ||
      widget.record!.status.toUpperCase() == 'REJECTED';

  Future<void> _capture(int index) async {
    final image = await _picker.pickImage(
      source: ImageSource.camera,
      imageQuality: 90,
      maxWidth: 2400,
      preferredCameraDevice: index == 2
          ? CameraDevice.front
          : CameraDevice.rear,
    );
    if (image == null || !mounted) return;
    setState(() => _photos[index] = image);
  }

  Future<void> _submit() async {
    if (_photos.any((item) => item == null)) {
      AppMessage.show(context, '请按要求拍摄3张照片');
      return;
    }
    setState(() => _submitting = true);
    try {
      await ref.read(repositoryProvider).submitIdentityCertification(
            _photos
                .whereType<XFile>()
                .map((item) => UploadFile(path: item.path, name: item.name))
                .toList(growable: false),
          );
      if (!mounted) return;
      AppMessage.show(context, '实名认证已提交');
      Navigator.pop(context);
    } catch (error) {
      if (mounted) AppMessage.show(context, '$error');
    } finally {
      if (mounted) setState(() => _submitting = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final record = widget.record;
    return Scaffold(
      appBar: AppBar(title: const Text('实名认证')),
      body: ListView(
        padding: const EdgeInsets.fromLTRB(10, 8, 10, 110),
        children: [
          if (record != null) ...[
            _StatusPanel(record: record),
            const SizedBox(height: 12),
          ],
          Container(
            padding: const EdgeInsets.all(16),
            decoration: BoxDecoration(
              color: Theme.of(context).colorScheme.surface,
              borderRadius: BorderRadius.circular(20),
            ),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text('认证材料', style: Theme.of(context).textTheme.titleLarge),
                const SizedBox(height: 5),
                Text(
                  _editable ? '请现场拍摄以下3张照片' : '以下是您提交的认证材料',
                  style: Theme.of(context).textTheme.bodySmall,
                ),
                const SizedBox(height: 18),
                if (_editable)
                  for (var index = 0; index < _requirements.length; index++)
                    _PhotoRequirement(
                      title: _requirements[index],
                      photo: _photos[index],
                      onTap: () => _capture(index),
                    )
                else if (record != null && record.materials.isNotEmpty)
                  for (final material in record.materials)
                    _ExistingMaterial(material: material)
                else
                  Text('暂无认证材料', style: Theme.of(context).textTheme.bodyMedium),
              ],
            ),
          ),
        ],
      ),
      bottomNavigationBar: !_editable
          ? null
          : SafeArea(
              minimum: const EdgeInsets.fromLTRB(10, 8, 10, 10),
              child: FilledButton(
                onPressed: _submitting ? null : _submit,
                child: Text(_submitting ? '提交中' : '提交认证'),
              ),
            ),
    );
  }
}

class _StatusPanel extends StatelessWidget {
  const _StatusPanel({required this.record});

  final CertificationRecord record;

  @override
  Widget build(BuildContext context) {
    final style = appStatusStyle(context, record.status);
    final status = record.status.toUpperCase();
    final approved = status == 'APPROVED' || record.status == '已认证';
    final rejected = status == 'REJECTED' || record.status == '已拒绝';
    final label = approved ? '认证通过' : rejected ? '认证未通过' : '审核中';
    final icon = approved
        ? Icons.check_circle_rounded
        : rejected
            ? Icons.error_outline_rounded
            : Icons.schedule_rounded;
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: Theme.of(context).colorScheme.surface,
        borderRadius: BorderRadius.circular(20),
      ),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Icon(icon, color: style.foreground),
          const SizedBox(width: 10),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  label,
                  style: Theme.of(context).textTheme.titleMedium?.copyWith(
                        color: style.foreground,
                        fontWeight: FontWeight.w700,
                      ),
                ),
                if (record.rejectionReason.isNotEmpty) ...[
                  const SizedBox(height: 5),
                  Text(record.rejectionReason),
                ],
              ],
            ),
          ),
        ],
      ),
    );
  }
}

class _PhotoRequirement extends StatelessWidget {
  const _PhotoRequirement({
    required this.title,
    required this.photo,
    required this.onTap,
  });

  final String title;
  final XFile? photo;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 12),
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(14),
        child: Container(
          padding: const EdgeInsets.all(12),
          decoration: BoxDecoration(
            color: Theme.of(context).colorScheme.surfaceContainerLow,
            borderRadius: BorderRadius.circular(14),
          ),
          child: Row(
            children: [
              ClipRRect(
                borderRadius: BorderRadius.circular(10),
                child: SizedBox(
                  width: 54,
                  height: 54,
                  child: photo == null
                      ? Icon(
                          Icons.photo_camera_outlined,
                          color: Theme.of(context).colorScheme.primary,
                        )
                      : Image.file(File(photo!.path), fit: BoxFit.cover),
                ),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(title, style: Theme.of(context).textTheme.titleMedium),
                    const SizedBox(height: 3),
                    Text(
                      photo == null ? '现场拍摄1张' : '已拍摄',
                      style: Theme.of(context).textTheme.bodySmall,
                    ),
                  ],
                ),
              ),
              Text(
                photo == null ? '拍摄' : '重拍',
                style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                      color: Theme.of(context).colorScheme.primary,
                      fontWeight: FontWeight.w700,
                    ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _ExistingMaterial extends StatelessWidget {
  const _ExistingMaterial({required this.material});

  final CertificationMaterial material;

  @override
  Widget build(BuildContext context) {
    return ListTile(
      contentPadding: EdgeInsets.zero,
      leading: const Icon(Icons.image_outlined),
      title: Text(material.name),
      trailing: const Icon(Icons.chevron_right_rounded),
      onTap: () => openMaterial(context, material),
    );
  }
}
