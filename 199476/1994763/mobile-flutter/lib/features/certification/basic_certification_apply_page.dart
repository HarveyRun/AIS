import 'dart:io';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:image_picker/image_picker.dart';

import '../../app/providers.dart';
import '../../core/widgets/app_message.dart';
import '../../data/models/certification_models.dart';
import '../../data/repositories/app_repository.dart';
import 'material_viewer.dart';

class BasicCertificationApplyPage extends ConsumerStatefulWidget {
  const BasicCertificationApplyPage({
    super.key,
    required this.type,
    this.record,
  });
  final String type;
  final CertificationRecord? record;
  @override
  ConsumerState<BasicCertificationApplyPage> createState() =>
      _BasicCertificationApplyPageState();
}

class _BasicCertificationApplyPageState
    extends ConsumerState<BasicCertificationApplyPage> {
  final _picker = ImagePicker();
  final List<XFile> _photos = [];
  XFile? _video;
  bool _submitting = false;
  bool get _identity => widget.type == 'IDENTITY';
  bool get _editable =>
      widget.record == null ||
      widget.record!.status.toUpperCase() == 'REJECTED';

  Future<void> _capturePhoto() async {
    final image = await _picker.pickImage(
      source: ImageSource.camera,
      imageQuality: 90,
      maxWidth: 2400,
    );
    if (image != null && mounted) {
      setState(() {
        if (_identity && _photos.length >= 3) _photos.removeLast();
        _photos.add(image);
      });
    }
  }

  Future<void> _captureVideo() async {
    final video = await _picker.pickVideo(
      source: ImageSource.camera,
      maxDuration: const Duration(minutes: 10),
    );
    if (video != null) {
      final size = await video.length();
      if (size > 500 * 1024 * 1024) {
        if (mounted) AppMessage.show(context, '录像不能超过500MB');
        return;
      }
      if (mounted) setState(() => _video = video);
    }
  }

  Future<void> _submit() async {
    if (_identity && _photos.length != 3) {
      AppMessage.show(context, '请按要求拍摄3张照片');
      return;
    }
    if (!_identity && _photos.isEmpty && _video == null) {
      AppMessage.show(context, '请录制录像或拍摄照片');
      return;
    }
    setState(() => _submitting = true);
    try {
      final files = <UploadFile>[
        if (_video != null) UploadFile(path: _video!.path, name: _video!.name),
        ..._photos.map((item) => UploadFile(path: item.path, name: item.name)),
      ];
      await ref
          .read(repositoryProvider)
          .submitBasicCertification(widget.type, files);
      if (!mounted) return;
      AppMessage.show(context, '已经提交认证');
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
      appBar: AppBar(title: Text(_identity ? '实名认证' : '我的岗位')),
      body: ListView(
        padding: const EdgeInsets.fromLTRB(16, 8, 16, 110),
        children: [
          if (record != null) _Status(record: record),
          const SizedBox(height: 12),
          Card(
            child: Padding(
              padding: const EdgeInsets.all(17),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text('认证材料', style: Theme.of(context).textTheme.titleLarge),
                  const SizedBox(height: 5),
                  Text(
                    '上传材料是认证的唯一依据。',
                    style: Theme.of(context).textTheme.bodySmall,
                  ),
                  const SizedBox(height: 18),
                  if (!_editable && record != null)
                    for (final material in record.materials)
                      _ExistingMaterial(material: material)
                  else if (_identity) ...[
                    for (var index = 0; index < 3; index++)
                      _CaptureRow(
                        icon: Icons.badge_outlined,
                        title: const ['身份证正面', '身份证反面', '手持身份证'][index],
                        subtitle: index < _photos.length
                            ? _photos[index].name
                            : '现场拍摄，1张',
                        done: index < _photos.length,
                        action: index < _photos.length ? '重拍' : '拍摄',
                        onTap: () async {
                          final image = await _picker.pickImage(
                            source: ImageSource.camera,
                            imageQuality: 90,
                            maxWidth: 2400,
                            preferredCameraDevice: index == 2
                                ? CameraDevice.front
                                : CameraDevice.rear,
                          );
                          if (image == null || !mounted) return;
                          setState(() {
                            if (index < _photos.length) {
                              _photos[index] = image;
                            } else {
                              _photos.add(image);
                            }
                          });
                        },
                      ),
                  ] else ...[
                    _CaptureRow(
                      icon: Icons.videocam_outlined,
                      title: '录像（二选一）',
                      subtitle: _video?.name ?? '现场录制，最大500MB',
                      done: _video != null,
                      action: _video == null ? '录制' : '重录',
                      onTap: _captureVideo,
                    ),
                    _CaptureRow(
                      icon: Icons.photo_camera_outlined,
                      title: '拍摄（二选一）',
                      subtitle: _photos.isEmpty
                          ? '现场拍摄，最多5张'
                          : '已拍摄${_photos.length}张',
                      done: _photos.isNotEmpty,
                      action: _photos.length >= 5 ? '已满' : '拍摄',
                      onTap: _photos.length >= 5 ? null : _capturePhoto,
                    ),
                    if (_photos.isNotEmpty)
                      Wrap(
                        spacing: 8,
                        runSpacing: 8,
                        children: _photos
                            .asMap()
                            .entries
                            .map(
                              (entry) => Stack(
                                children: [
                                  ClipRRect(
                                    borderRadius: BorderRadius.circular(10),
                                    child: Image.file(
                                      File(entry.value.path),
                                      width: 76,
                                      height: 76,
                                      fit: BoxFit.cover,
                                    ),
                                  ),
                                  Positioned(
                                    right: 0,
                                    top: 0,
                                    child: IconButton.filledTonal(
                                      visualDensity: VisualDensity.compact,
                                      onPressed: () => setState(
                                        () => _photos.removeAt(entry.key),
                                      ),
                                      icon: const Icon(
                                        Icons.close_rounded,
                                        size: 16,
                                      ),
                                    ),
                                  ),
                                ],
                              ),
                            )
                            .toList(),
                      ),
                  ],
                ],
              ),
            ),
          ),
        ],
      ),
      bottomNavigationBar: !_editable
          ? null
          : SafeArea(
              minimum: const EdgeInsets.fromLTRB(16, 8, 16, 12),
              child: FilledButton(
                onPressed: _submitting ? null : _submit,
                child: _submitting
                    ? const SizedBox.square(
                        dimension: 18,
                        child: CircularProgressIndicator(
                          strokeWidth: 2,
                          color: Colors.white,
                        ),
                      )
                    : const Text('提交认证'),
              ),
            ),
    );
  }
}

class _Status extends StatelessWidget {
  const _Status({required this.record});
  final CertificationRecord record;
  @override
  Widget build(BuildContext context) {
    final status = record.status.toUpperCase();
    return Container(
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: Theme.of(context).colorScheme.surfaceContainerHighest,
        borderRadius: BorderRadius.circular(14),
      ),
      child: Row(
        children: [
          Icon(
            status == 'APPROVED'
                ? Icons.verified_outlined
                : status == 'REJECTED'
                ? Icons.error_outline_rounded
                : Icons.schedule_rounded,
          ),
          const SizedBox(width: 10),
          Expanded(
            child: Text(
              status == 'APPROVED'
                  ? '已经通过认证'
                  : status == 'REJECTED'
                  ? (record.rejectionReason.isEmpty
                        ? '请修改后重新提交'
                        : record.rejectionReason)
                  : '正在审核',
            ),
          ),
        ],
      ),
    );
  }
}

class _ExistingMaterial extends StatelessWidget {
  const _ExistingMaterial({required this.material});
  final CertificationMaterial material;
  @override
  Widget build(BuildContext context) => ListTile(
    contentPadding: EdgeInsets.zero,
    leading: Icon(
      material.kind.toUpperCase() == 'VIDEO'
          ? Icons.videocam_outlined
          : Icons.image_outlined,
    ),
    title: Text(material.name, maxLines: 1, overflow: TextOverflow.ellipsis),
    trailing: TextButton(
      onPressed: () => openMaterial(context, material),
      child: const Text('预览'),
    ),
  );
}

class _CaptureRow extends StatelessWidget {
  const _CaptureRow({
    required this.icon,
    required this.title,
    required this.subtitle,
    required this.done,
    required this.action,
    required this.onTap,
  });
  final IconData icon;
  final String title;
  final String subtitle;
  final bool done;
  final String action;
  final VoidCallback? onTap;
  @override
  Widget build(BuildContext context) => Padding(
    padding: const EdgeInsets.only(bottom: 12),
    child: Row(
      children: [
        Container(
          width: 42,
          height: 42,
          decoration: BoxDecoration(
            color: Theme.of(context).colorScheme.surfaceContainerHighest,
            borderRadius: BorderRadius.circular(12),
          ),
          child: Icon(
            done ? Icons.check_rounded : icon,
            color: Theme.of(context).colorScheme.primary,
          ),
        ),
        const SizedBox(width: 11),
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(title, style: Theme.of(context).textTheme.titleMedium),
              Text(
                subtitle,
                style: Theme.of(context).textTheme.bodySmall,
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
              ),
            ],
          ),
        ),
        TextButton(onPressed: onTap, child: Text(action)),
      ],
    ),
  );
}
