import 'package:file_picker/file_picker.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../app/providers.dart';
import '../../core/theme/app_status_style.dart';
import '../../core/config/app_config.dart';
import '../../core/input/app_input_formatters.dart';
import '../../core/widgets/app_message.dart';
import '../../data/models/certification_models.dart';
import '../../data/repositories/app_repository.dart';
import '../../data/models/app_global_settings.dart';
import 'material_viewer.dart';
import 'experience_additional_info_page.dart';
import 'experience_category_picker.dart';

class ExperienceFormPage extends ConsumerStatefulWidget {
  const ExperienceFormPage({super.key, this.id});

  final int? id;

  @override
  ConsumerState<ExperienceFormPage> createState() => _ExperienceFormPageState();
}

class _ExperienceFormPageState extends ConsumerState<ExperienceFormPage> {
  final _title = TextEditingController();
  final _description = TextEditingController();
  ExperienceAdditionalInfo _additionalInfo = const ExperienceAdditionalInfo();
  List<ExperienceCategoryOption> _categories = const [];
  Future<List<ExperienceCategoryOption>>? _categoryRequest;
  CertificationRecord? _record;
  PlatformFile? _proofArchive;
  String? _completedProofUploadId;
  double? _proofUploadProgress;
  bool _removeExistingProofArchive = false;
  bool _loading = false;
  bool _submitting = false;
  bool _savingPublicMedia = false;
  ExperiencePublicMediaView? _publicMedia;
  Set<int> _selectedPublicMediaIds = <int>{};
  int _titleMaxLength = 18;
  int _descriptionMaxLength = 400;
  int _maxProofArchiveBytes = 2 * 1024 * 1024 * 1024;
  bool get _editable =>
      widget.id == null || _record?.status.toUpperCase() == 'REJECTED';
  bool get _hasPublicMediaConfiguration {
    final publicMedia = _publicMedia;
    if (publicMedia == null) return false;
    return publicMedia.processingStatus.toUpperCase() != 'NOT_REQUIRED' ||
        publicMedia.items.isNotEmpty;
  }

  CertificationMaterial? get _existingProofArchive => _record?.materials
      .where(
        (item) => const [
          'ARCHIVE',
          'PROOF_ARCHIVE',
        ].contains(item.kind.toUpperCase()),
      )
      .firstOrNull;
  bool get _hasSelectedProofArchive =>
      _proofArchive != null ||
      (!_removeExistingProofArchive && _existingProofArchive != null);
  @override
  void initState() {
    super.initState();
    _initialize();
  }

  Future<void> _initialize() async {
    try {
      final AppGlobalSettings settings = await ref
          .read(repositoryProvider)
          .appGlobalSettings();
      _titleMaxLength = settings.experienceTitleMaxLength;
      _descriptionMaxLength = settings.experienceDescriptionMaxLength;
      _maxProofArchiveBytes = settings.proofArchiveMaxBytes;
    } catch (_) {}
    try { await _fetchCategories(); } catch (_) {}
    if (widget.id != null) await _load();
  }

  Future<List<ExperienceCategoryOption>> _fetchCategories() async {
    final request = _categoryRequest ??=
        ref.read(repositoryProvider).experienceCategories();
    try {
      final items = await request;
      if (mounted) setState(() => _categories = items);
      return items;
    } finally {
      _categoryRequest = null;
    }
  }

  Future<void> _openCategoryPicker() async {
    List<ExperienceCategoryOption> categories = _categories;
    if (categories.isEmpty) {
      try {
        categories = await _fetchCategories();
      } catch (_) {
        if (mounted) AppMessage.show(context, '分类加载失败，请重试');
        return;
      }
    }
    if (!mounted) return;
    if (categories.isEmpty) {
      AppMessage.show(context, '暂无可选分类');
      return;
    }
    final selected = await showExperienceCategoryPicker(
      context,
      categories: categories,
      selectedParentId: _additionalInfo.categoryParentId,
      selectedChildId: _additionalInfo.categoryId,
    );
    if (selected != null && mounted) {
      setState(() => _additionalInfo = _additionalInfo.withCategory(
        parent: selected.parent,
        child: selected.child,
      ));
    }
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
        ExperiencePublicMediaView? publicMedia;
        if (record.approved) {
          publicMedia = await ref
              .read(repositoryProvider)
              .experiencePublicMedia(record.id);
        }
        if (!mounted) return;
        setState(() {
          _record = record;
          _title.text = record.title;
          _description.text = record.description;
          _additionalInfo = record.additionalInfo;
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
      final gigabytes = _maxProofArchiveBytes / 1024 / 1024 / 1024;
      final label = gigabytes >= 1
          ? '${gigabytes.toStringAsFixed(gigabytes % 1 == 0 ? 0 : 1)}GB'
          : '${(_maxProofArchiveBytes / 1024 / 1024).round()}MB';
      if (mounted) AppMessage.show(context, '每个压缩包不能超过$label');
      return null;
    }
    return file;
  }

  Future<void> _pickProofArchive() async {
    final file = await _pickArchive();
    if (file != null && mounted) {
      setState(() {
        _proofArchive = file;
        _completedProofUploadId = null;
        _proofUploadProgress = null;
        _removeExistingProofArchive = false;
      });
    }
  }

  void _removeProofArchive() {
    setState(() {
      _proofArchive = null;
      _completedProofUploadId = null;
      _proofUploadProgress = null;
      _removeExistingProofArchive = true;
    });
  }

  Future<void> _openAdditionalInfo() async {
    final result = await context.push<ExperienceAdditionalInfo>(
      '/profile/certifications/experiences/additional-info',
      extra: _additionalInfo,
    );
    if (result != null && mounted) {
      setState(() => _additionalInfo = result);
    }
  }

  Future<void> _submit() async {
    if (_title.text.trim().isEmpty) {
      AppMessage.show(context, '请填写经历标题');
      return;
    }
    if (_title.text.trim().length > _titleMaxLength) {
      AppMessage.show(context, '经历标题最多$_titleMaxLength个字');
      return;
    }
    if (_additionalInfo.categoryId == null) {
      AppMessage.show(context, '请选择经历分类');
      return;
    }
    final description = _description.text.trim();
    if (description.isEmpty) {
      AppMessage.show(context, '请填写经历叙述');
      return;
    }
    if (description.length > _descriptionMaxLength) {
      AppMessage.show(context, '经历叙述最多$_descriptionMaxLength个字');
      return;
    }
    final confirmed = await _confirmSubmission();
    if (confirmed != true || !mounted) return;
    setState(() => _submitting = true);
    try {
      final repository = ref.read(repositoryProvider);
      if (_proofArchive != null && _completedProofUploadId == null) {
        _completedProofUploadId = await repository.uploadExperienceProof(
          UploadFile(path: _proofArchive!.path!, name: _proofArchive!.name),
          onProgress: (progress) {
            if (mounted) setState(() => _proofUploadProgress = progress);
          },
        );
      }
      await repository.submitExperience(
        existingId: widget.id,
        title: _title.text.trim(),
        description: description,
        additionalInfo: _additionalInfo,
        proofArchive: _proofArchive != null && _completedProofUploadId == null
            ? UploadFile(path: _proofArchive!.path!, name: _proofArchive!.name)
            : null,
        proofUploadId: _completedProofUploadId,
        removeProofArchive: _removeExistingProofArchive,
      );
      if (!mounted) return;
      AppMessage.show(context, '经历已提交审核');
      Navigator.pop(context);
    } catch (error) {
      if (mounted) AppMessage.show(context, '$error');
    } finally {
      if (mounted) setState(() => _submitting = false);
    }
  }

  Future<bool?> _confirmSubmission() => showDialog<bool>(
    context: context,
    builder: (dialogContext) => AlertDialog(
      titlePadding: const EdgeInsets.fromLTRB(24, 14, 8, 0),
      title: Row(
        children: [
          const Expanded(child: Text('确认提交？')),
          IconButton(
            onPressed: () => Navigator.pop(dialogContext, false),
            tooltip: '关闭',
            icon: const Icon(Icons.close_rounded),
          ),
        ],
      ),
      content: const Text('提交后将进入审核，是否继续？'),
      actions: [
        FilledButton(
          onPressed: () => Navigator.pop(dialogContext, true),
          child: const Text('确认提交'),
        ),
      ],
    ),
  );

  Future<void> _showPrivacyInformationDialog() => showDialog<void>(
    context: context,
    builder: (context) => const _PrivacyInformationDialog(),
  );

  @override
  Widget build(BuildContext context) => PopScope(
    canPop: !_submitting,
    child: Scaffold(
      appBar: AppBar(title: Text(widget.id == null ? '发布经历' : '经历详情')),
      body: AbsorbPointer(
        absorbing: _submitting,
        child: _loading
            ? const SizedBox.shrink()
            : ListView(
                padding: const EdgeInsets.fromLTRB(10, 8, 10, 110),
                children: [
                  if (_record != null) ...[
                    _ExperienceStatus(record: _record!),
                    const SizedBox(height: 12),
                  ],
                  if (!_editable &&
                      _record?.approved == true &&
                      _hasPublicMediaConfiguration) ...[
                    _PublicMediaSection(
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
                          '经历标题',
                          style: Theme.of(context).textTheme.titleMedium,
                        ),
                        const SizedBox(height: 7),
                        TextField(
                          controller: _title,
                          enabled: _editable,
                          maxLength: _titleMaxLength,
                          inputFormatters: AppInputFormatters.description(
                            _titleMaxLength,
                          ),
                          decoration: const InputDecoration(
                            hintText: '例如：我的房屋装修经历',
                          ),
                        ),
                        const SizedBox(height: 14),
                        Row(children: [
                          Text('经历分类', style: Theme.of(context).textTheme.titleMedium),
                          if (_editable) Text(' *', style: Theme.of(context).textTheme.titleMedium?.copyWith(
                            color: Theme.of(context).colorScheme.primary,
                          )),
                        ]),
                        const SizedBox(height: 7),
                        InkWell(
                          borderRadius: BorderRadius.circular(12),
                          onTap: _editable ? _openCategoryPicker : null,
                          child: InputDecorator(
                            decoration: InputDecoration(
                              contentPadding: const EdgeInsets.symmetric(horizontal: 12, vertical: 14),
                              suffixIcon: _editable ? const Icon(Icons.chevron_right_rounded) : null,
                            ),
                            child: Text(
                              _additionalInfo.categoryName.isEmpty
                                  ? '请选择分类'
                                  : '${_additionalInfo.categoryParentName} / ${_additionalInfo.categoryName}',
                              maxLines: 1,
                              overflow: TextOverflow.ellipsis,
                              style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                                color: _additionalInfo.categoryName.isEmpty
                                    ? Theme.of(context).hintColor : null,
                              ),
                            ),
                          ),
                        ),
                        const SizedBox(height: 14),
                        Row(
                          children: [
                            Expanded(
                              child: Text(
                                '经历叙述',
                                style: Theme.of(context).textTheme.titleMedium,
                              ),
                            ),
                            if (_editable)
                              TextButton(
                                onPressed: _openAdditionalInfo,
                                child: Text(
                                  _additionalInfo.isEmpty
                                      ? '补充更多信息'
                                      : '已填写${_additionalInfo.completedCount}项',
                                ),
                              ),
                          ],
                        ),
                        const SizedBox(height: 7),
                        TextField(
                          controller: _description,
                          enabled: _editable,
                          maxLength: _descriptionMaxLength,
                          inputFormatters: AppInputFormatters.description(
                            _descriptionMaxLength,
                          ),
                          minLines: 6,
                          maxLines: 10,
                          decoration: const InputDecoration(
                            hintText:
                                '请叙述您经历了什么、当时是什么身份、亲自做过哪些事，以及最后怎么样。\n\n例如：2023年我第一次装修自己的房子，当时什么都不懂。我自己找了装修公司，选的是半包，主材自己买。中间遇到过报价漏项、工期拖延，也跟着做了水电和完工验收，最后多花了两万多，延期一个月才住进去。',
                          ),
                        ),
                      ],
                    ),
                  ),
                  if (!_editable && !_additionalInfo.isEmpty) ...[
                    const SizedBox(height: 12),
                    ExperienceAdditionalInfoCard(value: _additionalInfo, showCategory: false),
                  ],
                  const SizedBox(height: 12),
                  if (_editable || _existingProofArchive != null)
                    Card(
                      child: Padding(
                        padding: const EdgeInsets.fromLTRB(16, 18, 16, 14),
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Row(
                              children: [
                                Text(
                                  '证明资料',
                                  style: Theme.of(context).textTheme.titleLarge,
                                ),
                                if (_editable) ...[
                                  const SizedBox(width: 8),
                                  Container(
                                    padding: const EdgeInsets.symmetric(
                                      horizontal: 9,
                                      vertical: 3,
                                    ),
                                    decoration: BoxDecoration(
                                      color: const Color(0xFFFFF0D8),
                                      borderRadius: BorderRadius.circular(20),
                                    ),
                                    child: const Text(
                                      '选填',
                                      style: TextStyle(
                                        color: Color(0xFF9A5B0B),
                                        fontSize: 12,
                                        fontWeight: FontWeight.w700,
                                      ),
                                    ),
                                  ),
                                ],
                              ],
                            ),
                            if (_editable) ...[
                              const SizedBox(height: 9),
                              Container(
                                width: double.infinity,
                                padding: const EdgeInsets.symmetric(
                                  horizontal: 12,
                                  vertical: 10,
                                ),
                                decoration: BoxDecoration(
                                  color: Theme.of(
                                    context,
                                  ).colorScheme.primary.withValues(alpha: .07),
                                  borderRadius: BorderRadius.circular(10),
                                ),
                                child: Text(
                                  '资料越完整，越能获得信任，收到更多付费询问。',
                                  style: Theme.of(context).textTheme.bodyMedium
                                      ?.copyWith(height: 1.45),
                                ),
                              ),
                              const SizedBox(height: 7),
                              Text(
                                '支持 ZIP 或 RAR 压缩包，最大 2GB',
                                style: Theme.of(context).textTheme.bodySmall,
                              ),
                            ],
                            const SizedBox(height: 12),
                            if (!_editable && _record != null)
                              for (final material in _record!.materials.where(
                                (item) => const [
                                  'PROOF_ARCHIVE',
                                  'ARCHIVE',
                                ].contains(item.kind.toUpperCase()),
                              ))
                                _ExistingMaterialRow(material: material)
                            else ...[
                              _MaterialRow(
                                icon: Icons.folder_zip_outlined,
                                title: '证明资料',
                                subtitle:
                                    _proofArchive?.name ??
                                    (!_removeExistingProofArchive
                                        ? _existingProofArchive?.name
                                        : null) ??
                                    '请先处理其中的个人隐私',
                                notice: '不知道如何处理？',
                                onNoticeTap: _showPrivacyInformationDialog,
                                action: _hasSelectedProofArchive ? '重选' : '上传',
                                onTap: _pickProofArchive,
                                onRemove: _hasSelectedProofArchive
                                    ? _removeProofArchive
                                    : null,
                              ),
                            ],
                          ],
                        ),
                      ),
                    ),
                ],
              ),
      ),
      bottomNavigationBar: !_editable
          ? null
          : SafeArea(
              minimum: const EdgeInsets.fromLTRB(10, 8, 10, 12),
              child: Column(
                mainAxisSize: MainAxisSize.min,
                children: [
                  if (_submitting && _proofArchive != null) ...[
                    LinearProgressIndicator(value: _proofUploadProgress),
                    const SizedBox(height: 6),
                    Text(
                      _proofUploadProgress == null
                          ? '准备上传证明资料…'
                          : '上传证明资料 ${(_proofUploadProgress! * 100).round()}%',
                    ),
                    const SizedBox(height: 8),
                  ],
                  SizedBox(
                    width: double.infinity,
                    child: FilledButton(
                      onPressed: _submitting ? null : _submit,
                      child: const Text('提交审核'),
                    ),
                  ),
                ],
              ),
            ),
    ),
  );
}

class _PublicMediaSection extends StatelessWidget {
  const _PublicMediaSection({
    required this.data,
    required this.selectedIds,
    required this.saving,
    required this.onToggle,
    required this.onSave,
  });

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
                            const TextSpan(
                              text: '仅靠文字叙述，很难完全消除他人对内容真实性的疑虑。\n\n',
                            ),
                            TextSpan(
                              text: '选择公开证明材料',
                              style: TextStyle(
                                color: colorScheme.primary,
                                fontWeight: FontWeight.w700,
                              ),
                            ),
                            const TextSpan(
                              text: '，可以为这段经历提供更直观的真实依据，也能帮助其他用户判断是否需要进一步了解。',
                            ),
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
              '只会公开您勾选的证明资料内容',
              style: theme.textTheme.bodySmall?.copyWith(
                color: theme.colorScheme.onSurfaceVariant,
              ),
            ),
            const SizedBox(height: 14),
            if (status == 'PENDING' || status == 'PROCESSING')
              const _MediaProcessingNotice(text: '正在整理证明资料中的图片、视频和音频')
            else if (status == 'FAILED')
              _MediaProcessingNotice(
                text: data?.processingError.isNotEmpty == true
                    ? data!.processingError
                    : '证明资料整理失败，请联系平台处理',
                error: true,
              )
            else if (data == null || data!.items.isEmpty)
              const _MediaProcessingNotice(text: '证明资料中没有可供选择的图片、视频或音频')
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

class _ExistingMaterialRow extends StatelessWidget {
  const _ExistingMaterialRow({required this.material});

  final CertificationMaterial material;

  @override
  Widget build(BuildContext context) => ListTile(
    contentPadding: EdgeInsets.zero,
    leading: Icon(switch (material.kind.toUpperCase()) {
      'VIDEO' || 'DETAIL_VIDEO' => Icons.videocam_outlined,
      'IMAGE' => Icons.image_outlined,
      _ => Icons.archive_outlined,
    }),
    title: Text(material.name, maxLines: 1, overflow: TextOverflow.ellipsis),
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
            ? '已通过审核'
            : status == 'REJECTED'
            ? (record.rejectionReason.isEmpty
                  ? '审核未通过'
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
    this.onRemove,
  });
  final IconData icon;
  final String title;
  final String subtitle;
  final String action;
  final VoidCallback? onTap;
  final String? notice;
  final VoidCallback? onNoticeTap;
  final VoidCallback? onRemove;
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
    trailing: Row(
      mainAxisSize: MainAxisSize.min,
      children: [
        if (onRemove != null)
          TextButton(onPressed: onRemove, child: const Text('移除')),
        TextButton(onPressed: onTap, child: Text(action)),
      ],
    ),
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
