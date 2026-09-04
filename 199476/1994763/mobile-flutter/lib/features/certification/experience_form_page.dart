import 'dart:typed_data';
import 'dart:ui' as ui;

import 'package:file_picker/file_picker.dart';
import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:image_picker/image_picker.dart';

import '../../app/providers.dart';
import '../../core/theme/app_status_style.dart';
import '../../core/config/app_config.dart';
import '../../core/input/app_input_formatters.dart';
import '../../core/widgets/app_message.dart';
import '../../data/models/certification_models.dart';
import '../../data/repositories/app_repository.dart';
import 'certification_notice_card.dart';
import 'material_viewer.dart';

enum ExperienceBusinessType { publicWelfare, monetized }

class ExperienceFormPage extends ConsumerStatefulWidget {
  const ExperienceFormPage({
    super.key,
    this.id,
    this.upgradeSourceId,
    required this.businessType,
  });
  final int? id;
  final int? upgradeSourceId;
  final ExperienceBusinessType businessType;
  @override
  ConsumerState<ExperienceFormPage> createState() => _ExperienceFormPageState();
}

class _ExperienceFormPageState extends ConsumerState<ExperienceFormPage> {
  static const int _maxProofArchiveBytes = 2 * 1024 * 1024 * 1024;

  final _title = TextEditingController();
  final _description = TextEditingController();
  final _picker = ImagePicker();
  CertificationRecord? _record;
  CertificationRecord? _upgradeSource;
  XFile? _detailVideo;
  PlatformFile? _reviewOriginalArchive;
  PlatformFile? _proofArchive;
  bool _showVideoDetail = false;
  bool _loading = false;
  bool _submitting = false;
  bool _savingPublicMedia = false;
  ExperiencePublicMediaView? _publicMedia;
  Set<int> _selectedPublicMediaIds = <int>{};
  bool get _editable =>
      widget.id == null || _record?.status.toUpperCase() == 'REJECTED';
  bool get _isPublicWelfare =>
      widget.businessType == ExperienceBusinessType.publicWelfare;
  CertificationRecord? get _materialSource => _record ?? _upgradeSource;
  CertificationMaterial? get _existingDetailVideo => _materialSource?.materials
      .where((item) => item.kind.toUpperCase() == 'DETAIL_VIDEO')
      .firstOrNull;
  CertificationMaterial? get _existingProofArchive => _materialSource?.materials
      .where(
        (item) => const [
          'ARCHIVE',
          'PROOF_ARCHIVE',
        ].contains(item.kind.toUpperCase()),
      )
      .firstOrNull;
  CertificationMaterial? get _existingReviewOriginalArchive => _record
      ?.materials
      .where((item) => item.kind.toUpperCase() == 'REVIEW_ORIGINAL_ARCHIVE')
      .firstOrNull;

  @override
  void initState() {
    super.initState();
    if (widget.id != null || widget.upgradeSourceId != null) _load();
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
      final upgradeSource = records
          .where((item) => item.id == widget.upgradeSourceId)
          .firstOrNull;
      final contentSource = record ?? upgradeSource;
      if (contentSource != null && mounted) {
        ExperiencePublicMediaView? publicMedia;
        if (record?.approved == true) {
          publicMedia = await ref
              .read(repositoryProvider)
              .experiencePublicMedia(record!.id);
        }
        if (!mounted) return;
        setState(() {
          _record = record;
          _upgradeSource = upgradeSource;
          _title.text = contentSource.title;
          _description.text = contentSource.description;
          _showVideoDetail =
              contentSource.description.trim().isEmpty &&
              contentSource.materials.any(
                (item) => item.kind.toUpperCase() == 'DETAIL_VIDEO',
              );
          _publicMedia = publicMedia;
          _selectedPublicMediaIds =
              publicMedia?.items
                  .where((item) => item.selected)
                  .map((item) => item.id)
                  .toSet() ??
              <int>{};
        });
      }
    } catch (error) {
      if (mounted) AppMessage.show(context, '$error');
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  Future<void> _savePublicMedia() async {
    final record = _record;
    if (record == null || _savingPublicMedia) return;
    setState(() => _savingPublicMedia = true);
    try {
      final updated = await ref
          .read(repositoryProvider)
          .updateExperiencePublicMedia(record.id, _selectedPublicMediaIds);
      if (!mounted) return;
      setState(() {
        _publicMedia = updated;
        _selectedPublicMediaIds = updated.items
            .where((item) => item.selected)
            .map((item) => item.id)
            .toSet();
      });
      AppMessage.show(context, '公开内容已保存');
    } catch (error) {
      if (mounted) AppMessage.show(context, '$error');
    } finally {
      if (mounted) setState(() => _savingPublicMedia = false);
    }
  }

  Future<XFile?> _pickDetailVideo() async {
    final file = await _picker.pickVideo(source: ImageSource.gallery);
    if (file == null) return null;
    if (await file.length() > 1024 * 1024 * 1024) {
      if (mounted) AppMessage.show(context, '视频不能超过1GB');
      return null;
    }
    return file;
  }

  Future<void> _selectDetailVideo() async {
    final file = await _pickDetailVideo();
    if (file != null && mounted) setState(() => _detailVideo = file);
  }

  void _selectDetailMode(bool video) {
    if (_showVideoDetail == video) return;
    setState(() => _showVideoDetail = video);
  }

  Future<PlatformFile?> _pickArchive() async {
    final result = await FilePicker.platform.pickFiles(
      type: FileType.custom,
      allowedExtensions: const ['zip', 'rar'],
      allowMultiple: false,
      withData: false,
    );
    if (result == null || result.files.isEmpty) return null;
    final file = result.files.single;
    if (file.path == null) {
      if (mounted) AppMessage.show(context, '无法读取该压缩包');
      return null;
    }
    if (file.size > _maxProofArchiveBytes) {
      if (mounted) AppMessage.show(context, '每个压缩包不能超过2GB');
      return null;
    }
    return file;
  }

  Future<void> _pickReviewOriginalArchive() async {
    final file = await _pickArchive();
    if (file != null && mounted) {
      setState(() => _reviewOriginalArchive = file);
    }
  }

  Future<void> _pickProofArchive() async {
    final file = await _pickArchive();
    if (file != null && mounted) setState(() => _proofArchive = file);
  }

  Future<void> _submit() async {
    if (_title.text.trim().isEmpty) {
      AppMessage.show(context, _isPublicWelfare ? '请填写经历标题' : '请填写经历标题');
      return;
    }
    if (_title.text.trim().length > 20) {
      AppMessage.show(context, _isPublicWelfare ? '经历标题最多20个字' : '经历标题最多20个字');
      return;
    }
    final description = _description.text.trim();
    if (description.length > 5000) {
      AppMessage.show(
        context,
        _isPublicWelfare ? '经历叙述最多5000个字' : '经历叙述最多5000个字',
      );
      return;
    }
    final hasDetailVideo = _detailVideo != null || _existingDetailVideo != null;
    if (description.isEmpty && !hasDetailVideo) {
      AppMessage.show(
        context,
        _isPublicWelfare ? '请填写文字叙述或选择叙述视频' : '请填写文字叙述或选择叙述视频',
      );
      return;
    }
    if (!_isPublicWelfare &&
        _proofArchive == null &&
        _existingProofArchive == null) {
      AppMessage.show(context, '请上传已处理证明资料压缩包');
      return;
    }
    if (!_isPublicWelfare &&
        _reviewOriginalArchive == null &&
        _existingReviewOriginalArchive == null) {
      AppMessage.show(context, '请上传未处理证明资料压缩包');
      return;
    }
    Uint8List? confirmation;
    if (!_isPublicWelfare) {
      confirmation = await _confirmSubmission();
      if (confirmation == null || !mounted) return;
    }
    setState(() => _submitting = true);
    try {
      final repository = ref.read(repositoryProvider);
      if (_isPublicWelfare) {
        await repository.submitPublicWelfareExperience(
          existingId: widget.id,
          title: _title.text.trim(),
          description: description,
          detailMode: description.isNotEmpty && hasDetailVideo
              ? 'BOTH'
              : hasDetailVideo
              ? 'VIDEO'
              : 'TEXT',
          proofArchive: _proofArchive == null
              ? null
              : UploadFile(
                  path: _proofArchive!.path!,
                  name: _proofArchive!.name,
                ),
          detailVideo: _detailVideo == null
              ? null
              : UploadFile(path: _detailVideo!.path, name: _detailVideo!.name),
        );
      } else {
        await repository.submitMonetizedExperience(
          existingId: widget.id,
          upgradeSourceId: widget.upgradeSourceId,
          title: _title.text.trim(),
          description: description,
          signatureBytes: confirmation!,
          detailMode: description.isNotEmpty && hasDetailVideo
              ? 'BOTH'
              : hasDetailVideo
              ? 'VIDEO'
              : 'TEXT',
          reviewOriginal: _reviewOriginalArchive == null
              ? null
              : UploadFile(
                  path: _reviewOriginalArchive!.path!,
                  name: _reviewOriginalArchive!.name,
                ),
          proofArchive: _proofArchive == null
              ? null
              : UploadFile(
                  path: _proofArchive!.path!,
                  name: _proofArchive!.name,
                ),
          detailVideo: _detailVideo == null
              ? null
              : UploadFile(path: _detailVideo!.path, name: _detailVideo!.name),
        );
      }
      if (!mounted) return;
      AppMessage.show(context, _isPublicWelfare ? '公益分享已提交审核' : '干货变现已提交审核');
      Navigator.pop(context);
    } catch (error) {
      if (mounted) AppMessage.show(context, '$error');
    } finally {
      if (mounted) setState(() => _submitting = false);
    }
  }

  Future<Uint8List?> _confirmSubmission() => showModalBottomSheet<Uint8List>(
    context: context,
    isScrollControlled: true,
    isDismissible: false,
    enableDrag: false,
    backgroundColor: Colors.transparent,
    builder: (context) => const _SubmissionConfirmationDialog(),
  );

  Future<void> _showPrivacyInformationDialog() => showDialog<void>(
    context: context,
    builder: (context) => const _PrivacyInformationDialog(),
  );

  Future<void> _showReviewStandard() => showDialog<void>(
    context: context,
    builder: (dialogContext) => Dialog(
      backgroundColor: Colors.transparent,
      insetPadding: const EdgeInsets.symmetric(horizontal: 20),
      child: SingleChildScrollView(
        child: CertificationNoticeCard(
          tone: CertificationNoticeTone.warning,
          label: '审核标准',
          prominentLabel: true,
          onClose: () => Navigator.pop(dialogContext),
          paragraphs: _isPublicWelfare
              ? const [
                  CertificationNoticeParagraph(text: '不需要完成实名认证', marker: '1'),
                  CertificationNoticeParagraph(
                    text: '无明显矛盾、无不符合常识或疑似虚构的内容',
                    marker: '2',
                  ),
                  CertificationNoticeParagraph(
                    text: '上传证明资料会大幅提高审核通过率',
                    marker: '3',
                  ),
                ]
              : const [
                  CertificationNoticeParagraph(text: '须完成实名认证', marker: '1'),
                  CertificationNoticeParagraph(
                    text: '除敏感信息外，整个事情的时间、人物、经过及前后逻辑必须合理，无明显矛盾、无不符合常识或疑似虚构的内容',
                    marker: '2',
                  ),
                  CertificationNoticeParagraph(
                    text: '必须是自己经历的事情，家人、亲戚、朋友以及听说、转述、道听途说的内容，均不作为有效经历采纳',
                    marker: '3',
                  ),
                  CertificationNoticeParagraph(
                    text: '必须提交能证明该经历与本人直接相关的证明材料',
                    marker: '4',
                  ),
                  CertificationNoticeParagraph(
                    text:
                        '已审核通过的可变现经历，为确保信息真实，平台将视情况安排线下二次审核，多次拒审者视为放弃账户；线上审核通过后即可享受变现以及其它完整功能。感谢您的理解与配合！',
                    marker: '5',
                  ),
                ],
        ),
      ),
    ),
  );

  @override
  Widget build(BuildContext context) => Scaffold(
    appBar: AppBar(
      title: Text(widget.id == null ? '发布经历' : '经历详情'),
      actions: [
        IconButton(
          onPressed: _showReviewStandard,
          tooltip: '审核标准',
          color: Theme.of(context).brightness == Brightness.dark
              ? const Color(0xFFE0A24A)
              : const Color(0xFFB36B18),
          icon: const Icon(Icons.warning_amber_rounded),
        ),
      ],
    ),
    body: _loading
        ? const SizedBox.shrink()
        : ListView(
            padding: const EdgeInsets.fromLTRB(10, 8, 10, 110),
            children: [
              if (_record != null) ...[
                _ExperienceStatus(record: _record!),
                const SizedBox(height: 12),
              ],
              if (!_editable && _record?.approved == true) ...[
                _PublicMediaSection(
                  isPublicWelfare: _isPublicWelfare,
                  data: _publicMedia,
                  selectedIds: _selectedPublicMediaIds,
                  saving: _savingPublicMedia,
                  onToggle: (id) {
                    setState(() {
                      if (!_selectedPublicMediaIds.add(id)) {
                        _selectedPublicMediaIds.remove(id);
                      }
                    });
                  },
                  onSave: _savePublicMedia,
                ),
                const SizedBox(height: 12),
              ],
              Container(
                padding: const EdgeInsets.all(16),
                decoration: BoxDecoration(
                  color: Theme.of(context).colorScheme.surface,
                  borderRadius: BorderRadius.circular(16),
                ),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      _isPublicWelfare ? '经历标题' : '经历标题',
                      style: Theme.of(context).textTheme.titleMedium,
                    ),
                    const SizedBox(height: 7),
                    TextField(
                      controller: _title,
                      enabled: _editable,
                      maxLength: 20,
                      inputFormatters: AppInputFormatters.description(20),
                      decoration: InputDecoration(
                        hintText: _isPublicWelfare
                            ? '例如：我的房屋装修经历'
                            : '例如：我的房屋装修经历',
                      ),
                    ),
                    const SizedBox(height: 14),
                    Row(
                      children: [
                        Text(
                          _isPublicWelfare ? '叙述' : '叙述',
                          style: Theme.of(context).textTheme.titleMedium,
                        ),
                        const Spacer(),
                        Text(
                          '文字和录像可同时提供',
                          style: Theme.of(context).textTheme.bodySmall
                              ?.copyWith(
                                color: Theme.of(
                                  context,
                                ).colorScheme.onSurfaceVariant,
                              ),
                        ),
                      ],
                    ),
                    const SizedBox(height: 7),
                    Row(
                      children: [
                        Expanded(
                          child: _DetailModeButton(
                            label: _isPublicWelfare ? '文字叙述' : '文字叙述',
                            selected: !_showVideoDetail,
                            enabled:
                                _editable ||
                                _description.text.trim().isNotEmpty,
                            onTap: () => _selectDetailMode(false),
                          ),
                        ),
                        const SizedBox(width: 8),
                        Expanded(
                          child: _DetailModeButton(
                            label: _isPublicWelfare ? '录像叙述' : '录像叙述',
                            selected: _showVideoDetail,
                            enabled: _editable || _existingDetailVideo != null,
                            onTap: () => _selectDetailMode(true),
                          ),
                        ),
                      ],
                    ),
                    const SizedBox(height: 12),
                    if (_showVideoDetail)
                      if (_detailVideo == null && _existingDetailVideo != null)
                        Column(
                          children: [
                            _ExistingMaterialRow(
                              material: _existingDetailVideo!,
                            ),
                            if (_editable)
                              Align(
                                alignment: Alignment.centerRight,
                                child: TextButton(
                                  onPressed: _selectDetailVideo,
                                  child: const Text('更换叙述录像'),
                                ),
                              ),
                          ],
                        )
                      else if (!_editable && _record != null)
                        for (final material in _record!.materials.where(
                          (item) => item.kind.toUpperCase() == 'DETAIL_VIDEO',
                        ))
                          _ExistingMaterialRow(
                            material: material,
                            label:
                                material.kind.toUpperCase() ==
                                    'REVIEW_ORIGINAL_ARCHIVE'
                                ? '原件'
                                : '副件（需处理）',
                          )
                      else
                        _MaterialRow(
                          icon: Icons.video_camera_back_outlined,
                          title: _isPublicWelfare ? '选择叙述录像' : '选择叙述录像',
                          subtitle: _detailVideo?.name ?? '从相册选择，最大1GB',
                          action: _detailVideo == null ? '选择' : '重选',
                          onTap: _selectDetailVideo,
                        )
                    else
                      TextField(
                        controller: _description,
                        enabled: _editable,
                        maxLength: 5000,
                        inputFormatters: AppInputFormatters.description(5000),
                        minLines: 8,
                        maxLines: 16,
                        decoration: InputDecoration(
                          hintText: _isPublicWelfare ? '请叙述经历内容' : '请叙述经历内容',
                        ),
                      ),
                  ],
                ),
              ),
              const SizedBox(height: 12),
              Card(
                child: Padding(
                  padding: const EdgeInsets.fromLTRB(16, 18, 16, 14),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        '证明资料',
                        style: Theme.of(context).textTheme.titleLarge,
                      ),
                      if (_editable) ...[
                        const SizedBox(height: 5),
                        Text(
                          _isPublicWelfare
                              ? '选填，上传证明资料可大幅提高审核通过率'
                              : '请上传两份 ZIP 或 RAR 压缩包，每份最大 2GB',
                          style: Theme.of(context).textTheme.bodySmall
                              ?.copyWith(
                                color: _isPublicWelfare
                                    ? Theme.of(context).colorScheme.primary
                                    : Theme.of(
                                        context,
                                      ).colorScheme.onSurfaceVariant,
                                fontWeight: _isPublicWelfare
                                    ? FontWeight.w600
                                    : null,
                              ),
                        ),
                        if (_isPublicWelfare) ...[
                          const SizedBox(height: 3),
                          Text(
                            '支持 ZIP 或 RAR 压缩包，最大 2GB',
                            style: Theme.of(context).textTheme.bodySmall
                                ?.copyWith(
                                  color: Theme.of(
                                    context,
                                  ).colorScheme.onSurfaceVariant,
                                ),
                          ),
                        ],
                      ],
                      const SizedBox(height: 12),
                      if (!_editable && _record != null)
                        for (final material in _record!.materials.where(
                          (item) => const [
                            'REVIEW_ORIGINAL_ARCHIVE',
                            'PROOF_ARCHIVE',
                            'ARCHIVE',
                          ].contains(item.kind.toUpperCase()),
                        ))
                          _ExistingMaterialRow(material: material)
                      else ...[
                        if (!_isPublicWelfare) ...[
                          _MaterialRow(
                            icon: Icons.lock_outline_rounded,
                            title: '原件',
                            subtitle:
                                _reviewOriginalArchive?.name ??
                                _existingReviewOriginalArchive?.name ??
                                '完整版本，仅供平台审核，永不公开',
                            action:
                                _reviewOriginalArchive == null &&
                                    _existingReviewOriginalArchive == null
                                ? '上传'
                                : '重选',
                            onTap: _pickReviewOriginalArchive,
                          ),
                          const SizedBox(height: 4),
                        ],
                        _MaterialRow(
                          icon: Icons.folder_zip_outlined,
                          title: _isPublicWelfare ? '证明资料' : '副件（需处理）',
                          subtitle:
                              _proofArchive?.name ??
                              _existingProofArchive?.name ??
                              (_isPublicWelfare
                                  ? '请先处理其中的个人隐私'
                                  : '需处理隐私的版本，可自行公开'),
                          notice: '不知道如何处理？',
                          onNoticeTap: _showPrivacyInformationDialog,
                          action:
                              _proofArchive == null &&
                                  _existingProofArchive == null
                              ? '上传'
                              : '重选',
                          onTap: _pickProofArchive,
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
            minimum: const EdgeInsets.fromLTRB(10, 8, 10, 12),
            child: FilledButton(
              onPressed: _submitting ? null : _submit,
              child: Text(_isPublicWelfare ? '提交审核' : '提交审核'),
            ),
          ),
  );
}

class _PublicMediaSection extends StatelessWidget {
  const _PublicMediaSection({
    required this.isPublicWelfare,
    required this.data,
    required this.selectedIds,
    required this.saving,
    required this.onToggle,
    required this.onSave,
  });

  final bool isPublicWelfare;
  final ExperiencePublicMediaView? data;
  final Set<int> selectedIds;
  final bool saving;
  final ValueChanged<int> onToggle;
  final VoidCallback onSave;

  Future<void> _showExplanation(BuildContext context) => showDialog<void>(
    context: context,
    builder: (dialogContext) {
      final theme = Theme.of(dialogContext);
      final colorScheme = theme.colorScheme;
      return Dialog(
        backgroundColor: Colors.transparent,
        insetPadding: const EdgeInsets.symmetric(horizontal: 24),
        child: ClipRRect(
          borderRadius: BorderRadius.circular(24),
          child: ColoredBox(
            color: colorScheme.surface,
            child: Column(
              mainAxisSize: MainAxisSize.min,
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Container(
                  padding: const EdgeInsets.fromLTRB(20, 16, 10, 16),
                  decoration: BoxDecoration(
                    gradient: LinearGradient(
                      colors: [
                        colorScheme.primary.withValues(alpha: 0.13),
                        colorScheme.primary.withValues(alpha: 0.035),
                      ],
                    ),
                  ),
                  child: Row(
                    children: [
                      Container(
                        width: 38,
                        height: 38,
                        decoration: BoxDecoration(
                          color: colorScheme.primary,
                          borderRadius: BorderRadius.circular(12),
                        ),
                        child: Icon(
                          Icons.verified_user_outlined,
                          color: colorScheme.onPrimary,
                          size: 21,
                        ),
                      ),
                      const SizedBox(width: 12),
                      Expanded(
                        child: Text(
                          '公开内容',
                          style: theme.textTheme.titleLarge?.copyWith(
                            fontWeight: FontWeight.w800,
                            color: colorScheme.onSurface,
                          ),
                        ),
                      ),
                      IconButton(
                        onPressed: () => Navigator.pop(dialogContext),
                        tooltip: '关闭',
                        icon: const Icon(Icons.close_rounded),
                      ),
                    ],
                  ),
                ),
                Padding(
                  padding: const EdgeInsets.fromLTRB(22, 22, 22, 24),
                  child: DecoratedBox(
                    decoration: BoxDecoration(
                      color: colorScheme.surfaceContainerLow,
                      borderRadius: BorderRadius.circular(16),
                    ),
                    child: Padding(
                      padding: const EdgeInsets.fromLTRB(18, 17, 18, 18),
                      child: RichText(
                        text: TextSpan(
                          style: theme.textTheme.bodyMedium?.copyWith(
                            height: 1.75,
                            color: colorScheme.onSurfaceVariant,
                          ),
                          children: [
                            if (isPublicWelfare) ...[
                              const TextSpan(
                                text: '公开证明材料，可以让其他用户更直观地了解内容来源，未勾选的内容不会对外展示。',
                              ),
                              const TextSpan(text: ''),
                            ] else ...[
                              const TextSpan(
                                text:
                                    '在信息泛滥的当下，仅仅叙述事情经过，很难完全消除他人对“胡编乱造”的疑虑。\n\n',
                              ),
                              TextSpan(
                                text: '选择公开证明材料',
                                style: TextStyle(
                                  color: colorScheme.primary,
                                  fontWeight: FontWeight.w700,
                                ),
                              ),
                              const TextSpan(
                                text: '，是用最直观的真实背书，是向其他用户展示诚意与真实性的最佳方式。\n\n',
                              ),
                              const TextSpan(
                                text: '当你的干货有了确凿的证据支撑，不仅能大幅提升内容的说服力，更能有效增强他人的',
                              ),
                              TextSpan(
                                text: '付费意愿',
                                style: TextStyle(
                                  color: colorScheme.primary,
                                  fontWeight: FontWeight.w700,
                                ),
                              ),
                              const TextSpan(text: '，为你的干货变现创造更大的可能性。'),
                            ],
                          ],
                        ),
                      ),
                    ),
                  ),
                ),
              ],
            ),
          ),
        ),
      );
    },
  );

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final status = data?.processingStatus.toUpperCase() ?? 'PENDING';
    return Card(
      child: Padding(
        padding: const EdgeInsets.fromLTRB(16, 18, 16, 16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              mainAxisSize: MainAxisSize.min,
              children: [
                Text('选择公开内容', style: theme.textTheme.titleLarge),
                const SizedBox(width: 0),
                IconButton(
                  onPressed: () => _showExplanation(context),
                  tooltip: '公开内容说明',
                  visualDensity: VisualDensity.compact,
                  constraints: const BoxConstraints.tightFor(
                    width: 34,
                    height: 34,
                  ),
                  padding: EdgeInsets.zero,
                  icon: Icon(
                    Icons.help_outline_rounded,
                    size: 20,
                    color: theme.colorScheme.primary,
                  ),
                ),
              ],
            ),
            const SizedBox(height: 5),
            Text(
              isPublicWelfare ? '只会公开您勾选的证明资料内容' : '只会公开您勾选的副件内容',
              style: theme.textTheme.bodySmall?.copyWith(
                color: theme.colorScheme.onSurfaceVariant,
              ),
            ),
            const SizedBox(height: 14),
            if (status == 'PENDING' || status == 'PROCESSING')
              const _MediaProcessingNotice(text: '正在整理副件中的图片、视频和音频')
            else if (status == 'FAILED')
              _MediaProcessingNotice(
                text: data?.processingError.isNotEmpty == true
                    ? data!.processingError
                    : '副件整理失败，请联系平台处理',
                error: true,
              )
            else if (data == null || data!.items.isEmpty)
              const _MediaProcessingNotice(text: '副件中没有可供选择的图片、视频或音频')
            else ...[
              for (final group in const [
                ('IMAGE', '图片'),
                ('VIDEO', '视频'),
                ('AUDIO', '音频'),
              ])
                if (data!.items.any(
                  (item) => item.kind.toUpperCase() == group.$1,
                )) ...[
                  Padding(
                    padding: const EdgeInsets.only(top: 4, bottom: 8),
                    child: Text(
                      group.$2,
                      style: theme.textTheme.titleSmall?.copyWith(
                        fontWeight: FontWeight.w800,
                      ),
                    ),
                  ),
                  ...data!.items
                      .where((item) => item.kind.toUpperCase() == group.$1)
                      .map(
                        (item) => _PublicMediaItem(
                          material: item,
                          selected: selectedIds.contains(item.id),
                          onTap: () => onToggle(item.id),
                        ),
                      ),
                ],
              const SizedBox(height: 12),
              SizedBox(
                width: double.infinity,
                child: FilledButton(
                  onPressed: saving ? null : onSave,
                  child: Text(saving ? '保存中' : '保存公开设置'),
                ),
              ),
            ],
          ],
        ),
      ),
    );
  }
}

class _MediaProcessingNotice extends StatelessWidget {
  const _MediaProcessingNotice({required this.text, this.error = false});
  final String text;
  final bool error;

  @override
  Widget build(BuildContext context) => Container(
    width: double.infinity,
    padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 11),
    decoration: BoxDecoration(
      color: error
          ? Theme.of(context).colorScheme.errorContainer
          : Theme.of(context).colorScheme.surfaceContainerHighest,
      borderRadius: BorderRadius.circular(12),
    ),
    child: Text(text),
  );
}

class _PublicMediaItem extends StatelessWidget {
  const _PublicMediaItem({
    required this.material,
    required this.selected,
    required this.onTap,
  });

  final CertificationMaterial material;
  final bool selected;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final isImage = material.kind.toUpperCase() == 'IMAGE';
    return Padding(
      padding: const EdgeInsets.only(bottom: 8),
      child: Material(
        color: selected
            ? theme.colorScheme.primaryContainer
            : theme.colorScheme.surfaceContainerLow,
        borderRadius: BorderRadius.circular(12),
        clipBehavior: Clip.antiAlias,
        child: InkWell(
          onTap: onTap,
          child: Padding(
            padding: const EdgeInsets.all(10),
            child: Row(
              children: [
                ClipRRect(
                  borderRadius: BorderRadius.circular(8),
                  child: SizedBox(
                    width: 52,
                    height: 52,
                    child: isImage
                        ? Image.network(
                            AppConfig.resolveImage(material.url).toString(),
                            fit: BoxFit.cover,
                            errorBuilder: (_, _, _) =>
                                const Icon(Icons.broken_image_outlined),
                          )
                        : ColoredBox(
                            color: theme.colorScheme.surfaceContainerHighest,
                            child: Icon(
                              material.kind.toUpperCase() == 'VIDEO'
                                  ? Icons.play_circle_outline_rounded
                                  : Icons.graphic_eq_rounded,
                            ),
                          ),
                  ),
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: Text(
                    material.name,
                    style: theme.textTheme.bodyMedium?.copyWith(
                      fontWeight: FontWeight.w700,
                      color: selected
                          ? theme.colorScheme.onPrimaryContainer
                          : null,
                    ),
                  ),
                ),
                TextButton(
                  onPressed: () => openMaterial(context, material),
                  child: const Text('预览'),
                ),
                Checkbox(value: selected, onChanged: (_) => onTap()),
              ],
            ),
          ),
        ),
      ),
    );
  }
}

class _SubmissionConfirmationDialog extends StatefulWidget {
  const _SubmissionConfirmationDialog();

  @override
  State<_SubmissionConfirmationDialog> createState() =>
      _SubmissionConfirmationDialogState();
}

class _SubmissionConfirmationDialogState
    extends State<_SubmissionConfirmationDialog> {
  final GlobalKey _signatureBoundaryKey = GlobalKey();
  final List<Offset?> _points = [];
  bool _privacyConfirmed = false;
  bool _saving = false;

  bool get _hasSignature => _points.whereType<Offset>().length >= 8;

  void _addPoint(Offset? point) {
    setState(() => _points.add(point));
  }

  void _clearSignature() {
    setState(_points.clear);
  }

  Future<void> _confirm() async {
    if (!_privacyConfirmed || !_hasSignature || _saving) return;
    setState(() => _saving = true);
    try {
      await WidgetsBinding.instance.endOfFrame;
      final boundary =
          _signatureBoundaryKey.currentContext?.findRenderObject()
              as RenderRepaintBoundary?;
      if (boundary == null) return;
      final image = await boundary.toImage(pixelRatio: 2.5);
      final data = await image.toByteData(format: ui.ImageByteFormat.png);
      image.dispose();
      if (!mounted || data == null) return;
      Navigator.pop(context, data.buffer.asUint8List());
    } finally {
      if (mounted) setState(() => _saving = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final scheme = theme.colorScheme;
    final warningStyle = appStatusStyle(context, 'PENDING');
    final canSubmit = _privacyConfirmed && _hasSignature && !_saving;
    return Container(
      decoration: BoxDecoration(
        color: scheme.surface,
        borderRadius: const BorderRadius.vertical(top: Radius.circular(26)),
      ),
      child: SafeArea(
        top: false,
        child: SingleChildScrollView(
          padding: const EdgeInsets.fromLTRB(20, 18, 20, 16),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          '确认提交',
                          style: theme.textTheme.titleLarge?.copyWith(
                            fontWeight: FontWeight.w800,
                          ),
                        ),
                        const SizedBox(height: 4),
                        Text(
                          '二次确认，并完成本人签字',
                          style: theme.textTheme.bodyMedium?.copyWith(
                            color: scheme.onSurfaceVariant,
                          ),
                        ),
                      ],
                    ),
                  ),
                  IconButton(
                    onPressed: _saving ? null : () => Navigator.pop(context),
                    tooltip: '关闭',
                    visualDensity: VisualDensity.compact,
                    icon: const Icon(Icons.close_rounded),
                  ),
                ],
              ),
              const SizedBox(height: 20),
              Material(
                color: warningStyle.background,
                borderRadius: BorderRadius.circular(16),
                clipBehavior: Clip.antiAlias,
                child: InkWell(
                  onTap: _saving
                      ? null
                      : () => setState(
                          () => _privacyConfirmed = !_privacyConfirmed,
                        ),
                  child: Padding(
                    padding: const EdgeInsets.all(15),
                    child: Row(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Icon(
                          _privacyConfirmed
                              ? Icons.check_circle_rounded
                              : Icons.radio_button_unchecked_rounded,
                          size: 24,
                          color: _privacyConfirmed
                              ? warningStyle.foreground
                              : warningStyle.foreground.withValues(alpha: .72),
                        ),
                        const SizedBox(width: 12),
                        Expanded(
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Text(
                                '本人已对副件中的隐私信息完成脱敏处理，并自愿承担产生的一切风险与责任。',
                                style: theme.textTheme.bodyLarge?.copyWith(
                                  color: warningStyle.foreground,
                                  fontWeight: FontWeight.w700,
                                ),
                              ),
                              const SizedBox(height: 5),
                              Text(
                                '提交前，请务必自行核查并脱敏副件中的敏感隐私。',
                                style: theme.textTheme.bodySmall?.copyWith(
                                  color: scheme.onSurfaceVariant,
                                  height: 1.45,
                                ),
                              ),
                            ],
                          ),
                        ),
                      ],
                    ),
                  ),
                ),
              ),
              const SizedBox(height: 22),
              Row(
                children: [
                  Expanded(
                    child: Text(
                      '本人签字',
                      style: theme.textTheme.titleMedium?.copyWith(
                        fontWeight: FontWeight.w700,
                      ),
                    ),
                  ),
                  if (_points.isNotEmpty)
                    TextButton(
                      onPressed: _saving ? null : _clearSignature,
                      child: const Text('重新签写'),
                    ),
                ],
              ),
              const SizedBox(height: 8),
              ClipRRect(
                borderRadius: BorderRadius.circular(16),
                child: RepaintBoundary(
                  key: _signatureBoundaryKey,
                  child: ColoredBox(
                    color: Colors.white,
                    child: GestureDetector(
                      behavior: HitTestBehavior.opaque,
                      onPanStart: (details) => _addPoint(details.localPosition),
                      onPanUpdate: (details) =>
                          _addPoint(details.localPosition),
                      onPanEnd: (_) => _addPoint(null),
                      child: SizedBox(
                        width: double.infinity,
                        height: 160,
                        child: Stack(
                          alignment: Alignment.center,
                          children: [
                            if (_points.isEmpty)
                              const Text(
                                '在此处手写签字',
                                style: TextStyle(
                                  color: Color(0xFFB8B8B8),
                                  fontSize: 14,
                                ),
                              ),
                            Positioned.fill(
                              child: CustomPaint(
                                painter: _SignaturePainter(_points),
                              ),
                            ),
                          ],
                        ),
                      ),
                    ),
                  ),
                ),
              ),
              const SizedBox(height: 12),
              Row(
                crossAxisAlignment: CrossAxisAlignment.center,
                children: [
                  Icon(
                    Icons.info_outline_rounded,
                    size: 16,
                    color: scheme.onSurfaceVariant,
                  ),
                  const SizedBox(width: 7),
                  Expanded(
                    child: Text(
                      '审核通过后，经历内容及两份证明资料均不支持修改',
                      style: theme.textTheme.bodySmall?.copyWith(
                        color: scheme.onSurfaceVariant,
                        height: 1.2,
                      ),
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 20),
              SizedBox(
                width: double.infinity,
                child: FilledButton(
                  onPressed: canSubmit ? _confirm : null,
                  child: Text(_saving ? '正在提交…' : '确认并提交'),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _SignaturePainter extends CustomPainter {
  const _SignaturePainter(this.points);

  final List<Offset?> points;

  @override
  void paint(Canvas canvas, Size size) {
    final paint = Paint()
      ..color = const Color(0xFF222222)
      ..strokeWidth = 3
      ..strokeCap = StrokeCap.round
      ..strokeJoin = StrokeJoin.round;
    for (var index = 0; index < points.length - 1; index++) {
      final current = points[index];
      final next = points[index + 1];
      if (current != null && next != null) {
        canvas.drawLine(current, next, paint);
      }
    }
  }

  @override
  bool shouldRepaint(covariant _SignaturePainter oldDelegate) => true;
}

class _DetailModeButton extends StatelessWidget {
  const _DetailModeButton({
    required this.label,
    required this.selected,
    required this.enabled,
    required this.onTap,
  });

  final String label;
  final bool selected;
  final bool enabled;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Material(
      color: selected
          ? theme.colorScheme.primaryContainer
          : theme.colorScheme.surfaceContainerHighest,
      borderRadius: BorderRadius.circular(10),
      clipBehavior: Clip.antiAlias,
      child: InkWell(
        onTap: enabled ? onTap : null,
        child: Padding(
          padding: const EdgeInsets.symmetric(vertical: 11),
          child: Text(
            label,
            textAlign: TextAlign.center,
            style: theme.textTheme.bodyMedium?.copyWith(
              color: selected
                  ? theme.colorScheme.onPrimaryContainer
                  : theme.colorScheme.onSurfaceVariant,
              fontWeight: selected ? FontWeight.w700 : FontWeight.w500,
            ),
          ),
        ),
      ),
    );
  }
}

class _ExistingMaterialRow extends StatelessWidget {
  const _ExistingMaterialRow({required this.material, this.label});

  final CertificationMaterial material;
  final String? label;

  @override
  Widget build(BuildContext context) => ListTile(
    contentPadding: EdgeInsets.zero,
    leading: Icon(switch (material.kind.toUpperCase()) {
      'VIDEO' || 'DETAIL_VIDEO' => Icons.videocam_outlined,
      'IMAGE' => Icons.image_outlined,
      _ => Icons.archive_outlined,
    }),
    title: Text(
      label ?? material.name,
      maxLines: 1,
      overflow: TextOverflow.ellipsis,
    ),
    subtitle: label == null
        ? null
        : Text(material.name, maxLines: 1, overflow: TextOverflow.ellipsis),
    trailing: material.url.trim().isEmpty
        ? null
        : TextButton(
            onPressed: () => openMaterial(context, material),
            child: Text(
              const {
                    'ARCHIVE',
                    'PROOF_ARCHIVE',
                    'REVIEW_ORIGINAL_ARCHIVE',
                  }.contains(material.kind.toUpperCase())
                  ? '下载'
                  : '预览',
            ),
          ),
  );
}

class _ExperienceStatus extends StatelessWidget {
  const _ExperienceStatus({required this.record});
  final CertificationRecord record;
  @override
  Widget build(BuildContext context) {
    final status = record.status.toUpperCase();
    final style = appStatusStyle(context, status);
    return Container(
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: style.background,
        borderRadius: BorderRadius.circular(14),
      ),
      child: Text(
        status == 'APPROVED'
            ? record.isPublicWelfare
                  ? '已通过审核'
                  : '已经通过认证'
            : status == 'REJECTED'
            ? (record.rejectionReason.isEmpty
                  ? '认证未通过'
                  : record.rejectionReason)
            : '正在审核',
        style: TextStyle(color: style.foreground, fontWeight: FontWeight.w600),
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
    this.notice,
    this.onNoticeTap,
  });
  final IconData icon;
  final String title;
  final String subtitle;
  final String action;
  final VoidCallback? onTap;
  final String? notice;
  final VoidCallback? onNoticeTap;
  @override
  Widget build(BuildContext context) => ListTile(
    contentPadding: EdgeInsets.zero,
    leading: Icon(icon),
    title: Text(title),
    subtitle: Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(subtitle, maxLines: 1, overflow: TextOverflow.ellipsis),
        if (notice != null) ...[
          const SizedBox(height: 2),
          Align(
            alignment: Alignment.centerLeft,
            child: Material(
              color: Colors.transparent,
              child: InkWell(
                onTap: onNoticeTap,
                borderRadius: BorderRadius.circular(4),
                child: Padding(
                  padding: const EdgeInsets.symmetric(vertical: 2),
                  child: Text(
                    notice!,
                    style: Theme.of(context).textTheme.bodySmall?.copyWith(
                      color: Theme.of(context).colorScheme.primary,
                      fontWeight: FontWeight.w600,
                      decoration: TextDecoration.underline,
                      decorationColor: Theme.of(context).colorScheme.primary,
                    ),
                  ),
                ),
              ),
            ),
          ),
        ],
      ],
    ),
    trailing: TextButton(onPressed: onTap, child: Text(action)),
  );
}

class _PrivacyInformationDialog extends StatelessWidget {
  const _PrivacyInformationDialog();

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final scheme = theme.colorScheme;
    return Dialog(
      insetPadding: const EdgeInsets.symmetric(horizontal: 18, vertical: 24),
      backgroundColor: scheme.surface,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(24)),
      clipBehavior: Clip.antiAlias,
      child: ConstrainedBox(
        constraints: BoxConstraints(
          maxHeight: MediaQuery.sizeOf(context).height * .82,
        ),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Padding(
              padding: const EdgeInsets.fromLTRB(20, 16, 10, 8),
              child: Row(
                children: [
                  Expanded(
                    child: Text(
                      '隐私处理指南',
                      style: theme.textTheme.titleLarge?.copyWith(
                        fontWeight: FontWeight.w800,
                      ),
                    ),
                  ),
                  IconButton(
                    onPressed: () => Navigator.pop(context),
                    tooltip: '关闭',
                    icon: const Icon(Icons.close_rounded),
                  ),
                ],
              ),
            ),
            Flexible(
              child: SingleChildScrollView(
                padding: const EdgeInsets.fromLTRB(20, 4, 20, 16),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Container(
                      width: double.infinity,
                      padding: const EdgeInsets.all(14),
                      decoration: BoxDecoration(
                        color: scheme.primary.withValues(alpha: .08),
                        borderRadius: BorderRadius.circular(14),
                      ),
                      child: Text(
                        '请对不愿公开的隐私信息进行脱敏处理，同时务必保留能够印证经历的关键要素，如时间日期、事件经过及核心事项节点。',
                        style: theme.textTheme.bodyMedium?.copyWith(
                          color: scheme.onSurface,
                          fontWeight: FontWeight.w600,
                          height: 1.55,
                        ),
                      ),
                    ),
                    const SizedBox(height: 16),
                    _PrivacyGuideSection(
                      icon: Icons.visibility_off_outlined,
                      title: '建议遮挡',
                      color: scheme.primary,
                      background: scheme.surfaceContainerLow,
                      items: const [
                        ('身份信息', '姓名、身份证号、证件住址、证件有效期'),
                        ('联系方式', '手机号、微信号、邮箱及其他社交账号'),
                        ('住址与位置', '家庭住址、门牌号、车牌号及精确工作地点'),
                        ('资金信息', '银行卡号、支付账号、工资流水账号及余额'),
                        ('专属标识', '合同编号、员工编号、客户编号及业务单号'),
                        ('图像信息', '本人或他人的签名、印章、人脸及证件照片'),
                        ('他人隐私', '材料中其他人的姓名、联系方式和个人资料'),
                      ],
                    ),
                    const SizedBox(height: 12),
                    _PrivacyGuideSection(
                      icon: Icons.fact_check_outlined,
                      title: '处理后仍应保留',
                      color: scheme.tertiary,
                      background: scheme.tertiaryContainer.withValues(
                        alpha: .42,
                      ),
                      items: const [
                        ('关键信息', '与经历有关的时间、事件和必要内容'),
                        ('材料用途', '让人能够看出材料是什么、能够证明什么'),
                      ],
                    ),
                    const SizedBox(height: 12),
                    _PrivacyGuideSection(
                      icon: Icons.gpp_bad_outlined,
                      title: '不要上传',
                      color: scheme.error,
                      background: scheme.errorContainer,
                      foreground: scheme.onErrorContainer,
                      items: const [
                        ('账号凭证', '密码、短信验证码、支付密码、密保答案'),
                        ('安全密钥', '银行卡密码、私钥、助记词及恢复码'),
                      ],
                    ),
                    const SizedBox(height: 10),
                    Text(
                      '以上为常见情况。无法确定的信息，请按照“是否愿意向其他用户公开”自行判断。',
                      style: theme.textTheme.bodySmall?.copyWith(
                        color: scheme.onSurfaceVariant,
                        height: 1.5,
                      ),
                    ),
                  ],
                ),
              ),
            ),
            SafeArea(
              top: false,
              minimum: const EdgeInsets.fromLTRB(20, 0, 20, 18),
              child: SizedBox(
                width: double.infinity,
                child: FilledButton(
                  onPressed: () => Navigator.pop(context),
                  child: const Text('知道了'),
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _PrivacyGuideSection extends StatelessWidget {
  const _PrivacyGuideSection({
    required this.icon,
    required this.title,
    required this.color,
    required this.background,
    required this.items,
    this.foreground,
  });

  final IconData icon;
  final String title;
  final Color color;
  final Color background;
  final Color? foreground;
  final List<(String, String)> items;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final textColor = foreground ?? theme.colorScheme.onSurface;
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.fromLTRB(14, 13, 14, 12),
      decoration: BoxDecoration(
        color: background,
        borderRadius: BorderRadius.circular(14),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Icon(icon, size: 19, color: color),
              const SizedBox(width: 7),
              Text(
                title,
                style: theme.textTheme.titleSmall?.copyWith(
                  color: textColor,
                  fontWeight: FontWeight.w800,
                ),
              ),
            ],
          ),
          const SizedBox(height: 10),
          for (final item in items) ...[
            Text.rich(
              TextSpan(
                style: theme.textTheme.bodySmall?.copyWith(
                  color: textColor,
                  height: 1.55,
                ),
                children: [
                  TextSpan(
                    text: '${item.$1}：',
                    style: const TextStyle(fontWeight: FontWeight.w700),
                  ),
                  TextSpan(text: item.$2),
                ],
              ),
            ),
            if (item != items.last) const SizedBox(height: 7),
          ],
        ],
      ),
    );
  }
}
