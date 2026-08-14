import 'dart:io';

import 'package:file_picker/file_picker.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:image_picker/image_picker.dart';

import '../../app/providers.dart';
import '../../core/widgets/app_message.dart';
import '../../data/models/certification_models.dart';
import '../../data/repositories/app_repository.dart';
import 'material_viewer.dart';

class ExperienceFormPage extends ConsumerStatefulWidget {
  const ExperienceFormPage({super.key, this.id});
  final int? id;
  @override
  ConsumerState<ExperienceFormPage> createState() => _ExperienceFormPageState();
}

class _ExperienceFormPageState extends ConsumerState<ExperienceFormPage> {
  final _title = TextEditingController();
  final _description = TextEditingController();
  final _picker = ImagePicker();
  CertificationRecord? _record;
  PlatformFile? _archive;
  XFile? _video;
  final List<XFile> _photos = [];
  bool _loading = false;
  bool _submitting = false;
  bool get _editable =>
      widget.id == null || _record?.status.toUpperCase() == 'REJECTED';

  @override
  void initState() {
    super.initState();
    if (widget.id != null) _load();
  }

  @override
  void dispose() {
    _title.dispose();
    _description.dispose();
    super.dispose();
  }

  Future<void> _load() async {
    setState(() => _loading = true);
    try {
      final records = await ref.read(repositoryProvider).certifications();
      final record = records.where((item) => item.id == widget.id).firstOrNull;
      if (record != null && mounted) {
        setState(() {
          _record = record;
          _title.text = record.title;
          _description.text = record.description;
        });
      }
    } catch (error) {
      if (mounted) AppMessage.show(context, '$error');
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  Future<void> _selectArchive() async {
    final result = await FilePicker.platform.pickFiles(
      type: FileType.custom,
      allowedExtensions: ['rar', 'zip'],
      allowMultiple: false,
    );
    final file = result?.files.single;
    if (file == null) return;
    if (file.path == null) {
      if (mounted) AppMessage.show(context, '无法读取这个文件');
      return;
    }
    if (file.size > 1024 * 1024 * 1024) {
      if (mounted) AppMessage.show(context, '压缩包不能超过1GB');
      return;
    }
    setState(() => _archive = file);
  }

  Future<void> _captureVideo() async {
    final file = await _picker.pickVideo(
      source: ImageSource.camera,
      maxDuration: const Duration(minutes: 10),
    );
    if (file == null) return;
    if (await file.length() > 500 * 1024 * 1024) {
      if (mounted) AppMessage.show(context, '录像不能超过500MB');
      return;
    }
    if (mounted) setState(() => _video = file);
  }

  Future<void> _capturePhoto() async {
    if (_photos.length >= 5) return;
    final file = await _picker.pickImage(
      source: ImageSource.camera,
      imageQuality: 90,
      maxWidth: 2400,
    );
    if (file != null && mounted) setState(() => _photos.add(file));
  }

  Future<void> _submit() async {
    if (_title.text.trim().isEmpty) {
      AppMessage.show(context, '请填写经历标题');
      return;
    }
    if (_description.text.trim().isEmpty) {
      AppMessage.show(context, '请填写经历简述');
      return;
    }
    if (_archive?.path == null) {
      AppMessage.show(context, '请上传一个 RAR 或 ZIP 压缩包');
      return;
    }
    setState(() => _submitting = true);
    try {
      await ref
          .read(repositoryProvider)
          .submitExperienceCertification(
            existingId: widget.id,
            title: _title.text.trim(),
            description: _description.text.trim(),
            files: [
              UploadFile(path: _archive!.path!, name: _archive!.name),
              if (_video != null)
                UploadFile(path: _video!.path, name: _video!.name),
              ..._photos.map(
                (file) => UploadFile(path: file.path, name: file.name),
              ),
            ],
          );
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
  Widget build(BuildContext context) => Scaffold(
    appBar: AppBar(title: Text(widget.id == null ? '添加经历' : '经历详情')),
    body: _loading
        ? const Center(child: CircularProgressIndicator(strokeWidth: 2))
        : ListView(
            padding: const EdgeInsets.fromLTRB(16, 8, 16, 110),
            children: [
              if (_record != null) _ExperienceStatus(record: _record!),
              const SizedBox(height: 12),
              TextField(
                controller: _title,
                enabled: _editable,
                maxLength: 50,
                decoration: const InputDecoration(
                  labelText: '经历标题',
                  hintText: '例如：经历过劳动仲裁',
                ),
              ),
              const SizedBox(height: 10),
              TextField(
                controller: _description,
                enabled: _editable,
                maxLength: 300,
                minLines: 4,
                maxLines: 7,
                decoration: const InputDecoration(
                  labelText: '简述',
                  hintText: '简单说明事情发生和处理的经过',
                ),
              ),
              const SizedBox(height: 14),
              Card(
                child: Padding(
                  padding: const EdgeInsets.all(17),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        '证明材料',
                        style: Theme.of(context).textTheme.titleLarge,
                      ),
                      const SizedBox(height: 5),
                      Text(
                        '上传材料是认证的唯一依据。',
                        style: Theme.of(context).textTheme.bodySmall,
                      ),
                      const SizedBox(height: 16),
                      if (!_editable && _record != null)
                        for (final material in _record!.materials)
                          ListTile(
                            contentPadding: EdgeInsets.zero,
                            leading: Icon(_materialIcon(material.kind)),
                            title: Text(
                              material.name,
                              maxLines: 1,
                              overflow: TextOverflow.ellipsis,
                            ),
                            trailing: TextButton(
                              onPressed: () => openMaterial(context, material),
                              child: Text(
                                material.kind.toUpperCase() == 'ARCHIVE'
                                    ? '下载'
                                    : '预览',
                              ),
                            ),
                          )
                      else ...[
                        _MaterialRow(
                          icon: Icons.archive_outlined,
                          title: '压缩包（必填）',
                          subtitle: _archive?.name ?? 'RAR 或 ZIP，最大1GB，最多1个',
                          action: '选择',
                          onTap: _selectArchive,
                        ),
                        _MaterialRow(
                          icon: Icons.videocam_outlined,
                          title: '录制录像（选填）',
                          subtitle: _video?.name ?? '现场录制，最大500MB',
                          action: _video == null ? '录制' : '重录',
                          onTap: _captureVideo,
                        ),
                        _MaterialRow(
                          icon: Icons.photo_camera_outlined,
                          title: '拍摄照片（选填）',
                          subtitle: _photos.isEmpty
                              ? '现场拍摄，最多5张'
                              : '已拍摄${_photos.length}张',
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

  IconData _materialIcon(String kind) => switch (kind.toUpperCase()) {
    'VIDEO' => Icons.videocam_outlined,
    'IMAGE' => Icons.image_outlined,
    _ => Icons.archive_outlined,
  };
}

class _ExperienceStatus extends StatelessWidget {
  const _ExperienceStatus({required this.record});
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
      child: Text(
        status == 'APPROVED'
            ? '已经通过认证'
            : status == 'REJECTED'
            ? (record.rejectionReason.isEmpty
                  ? '请修改后重新提交'
                  : record.rejectionReason)
            : '正在审核',
      ),
    );
  }
}

class _MaterialRow extends StatelessWidget {
  const _MaterialRow({
    required this.icon,
    required this.title,
    required this.subtitle,
    required this.action,
    required this.onTap,
  });
  final IconData icon;
  final String title;
  final String subtitle;
  final String action;
  final VoidCallback? onTap;
  @override
  Widget build(BuildContext context) => ListTile(
    contentPadding: EdgeInsets.zero,
    leading: Icon(icon),
    title: Text(title),
    subtitle: Text(subtitle, maxLines: 1, overflow: TextOverflow.ellipsis),
    trailing: TextButton(onPressed: onTap, child: Text(action)),
  );
}
