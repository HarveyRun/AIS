import 'dart:io';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:image_picker/image_picker.dart';
import 'package:intl/intl.dart';

import '../../app/providers.dart';
import '../../core/theme/app_status_style.dart';
import '../../core/theme/app_theme.dart';
import '../../core/widgets/app_message.dart';
import '../../data/models/certification_models.dart';
import '../../data/repositories/app_repository.dart';
import 'job_certification_notice_dialog.dart';
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
  JobCertificationAppointment? _appointment;
  bool _loadingAppointment = false;
  bool _bookingAppointment = false;
  bool _submitting = false;
  bool get _identity => widget.type == 'IDENTITY';
  bool get _temporarilyBlocked {
    final availableAt = widget.record?.jobReapplyAvailableAt;
    return !_identity && availableAt != null && DateTime.now().isBefore(availableAt);
  }
  bool get _editable =>
      !_temporarilyBlocked &&
      (widget.record == null ||
          widget.record!.status.toUpperCase() == 'REJECTED');
  bool get _canSubmitMaterials =>
      _editable &&
      (_identity ||
          (_appointment == null && (_video != null || _photos.isNotEmpty)));

  @override
  void initState() {
    super.initState();
    if (!_identity) _loadAppointment();
  }

  Future<void> _loadAppointment() async {
    setState(() => _loadingAppointment = true);
    try {
      final result = await ref
          .read(repositoryProvider)
          .currentJobCertificationAppointment();
      if (mounted) setState(() => _appointment = result);
    } catch (error) {
      if (mounted) AppMessage.show(context, '$error');
    } finally {
      if (mounted) setState(() => _loadingAppointment = false);
    }
  }

  Future<void> _capturePhoto() async {
    final image = await _picker.pickImage(
      source: ImageSource.camera,
      imageQuality: 90,
      maxWidth: 2400,
    );
    if (image != null && mounted) {
      setState(() {
        if (_identity && _photos.length >= 3) _photos.removeLast();
        if (!_identity) _video = null;
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
      if (mounted) {
        setState(() {
          _photos.clear();
          _video = video;
        });
      }
    }
  }

  Future<void> _showOnlineCertificationNotice() async {
    await showDialog<bool>(
      context: context,
      builder: (context) => const JobCertificationNoticeDialog(),
    );
  }

  Future<void> _bookOfflineAppointment() async {
    if (_bookingAppointment) return;
    final appointmentAt = await showModalBottomSheet<DateTime>(
      context: context,
      isScrollControlled: true,
      useSafeArea: true,
      showDragHandle: true,
      builder: (context) => const _OfflineAppointmentSheet(),
    );
    if (appointmentAt == null || !mounted) return;

    setState(() => _bookingAppointment = true);
    try {
      final result = await ref
          .read(repositoryProvider)
          .bookJobCertificationAppointment(appointmentAt);
      if (!mounted) return;
      setState(() {
        _appointment = result;
        _photos.clear();
        _video = null;
      });
      AppMessage.show(context, '预约成功');
    } catch (error) {
      if (mounted) AppMessage.show(context, '$error');
    } finally {
      if (mounted) setState(() => _bookingAppointment = false);
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
        padding: const EdgeInsets.fromLTRB(10, 8, 10, 110),
        children: [
          if (record != null) ...[
            _Status(record: record),
            const SizedBox(height: 12),
          ],
          if (!_identity) ...[
            if (_loadingAppointment)
              Card(
                child: Padding(
                  padding: const EdgeInsets.symmetric(vertical: 30),
                  child: Center(
                    child: CircularProgressIndicator(strokeWidth: 2),
                  ),
                ),
              )
            else if (_appointment != null)
              Card(
                child: Padding(
                  padding: const EdgeInsets.all(16),
                  child: _OfflineAppointmentCard(appointment: _appointment!),
                ),
              )
            else if (_editable)
              Card(
                child: Padding(
                  padding: const EdgeInsets.all(16),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        '线下认证',
                        style: Theme.of(context).textTheme.titleLarge,
                      ),
                      const SizedBox(height: 5),
                      Text(
                        '本人到场，与工作人员面对面完成认证。',
                        style: Theme.of(context).textTheme.bodySmall,
                      ),
                      const SizedBox(height: 16),
                      _CaptureRow(
                        icon: Icons.location_on_outlined,
                        title: '线下认证',
                        subtitle: '现场以即兴问答为主',
                        done: false,
                        action: _bookingAppointment ? '预约中' : '预约',
                        onTap: _bookingAppointment
                            ? null
                            : _bookOfflineAppointment,
                      ),
                    ],
                  ),
                ),
              ),
            if (_loadingAppointment || _appointment != null || _editable)
              const SizedBox(height: 12),
          ],
          if (_identity || (!_loadingAppointment && _appointment == null))
            Card(
              child: Padding(
                padding: const EdgeInsets.all(16),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    if (!_identity && _editable)
                      Row(
                        children: [
                          Text(
                            '线上认证',
                            style: Theme.of(context).textTheme.titleLarge,
                          ),
                          const SizedBox(width: 0),
                          IconButton(
                            onPressed: _showOnlineCertificationNotice,
                            tooltip: '查看认证说明',
                            visualDensity: VisualDensity.compact,
                            constraints: const BoxConstraints(
                              minWidth: 25,
                              minHeight: 28,
                            ),
                            padding: EdgeInsets.zero,
                            icon: const Icon(
                              Icons.info_rounded,
                              size: 19,
                              color: AppColors.success,
                            ),
                          ),
                        ],
                      )
                    else
                      Text(
                        '认证材料',
                        style: Theme.of(context).textTheme.titleLarge,
                      ),
                    const SizedBox(height: 5),
                    Text(
                      _identity
                          ? '上传材料'
                          : _editable
                          ? '录像或拍照，任选一种即可。'
                          : '以下是您提交的岗位认证材料。',
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
                        title: '现场录像',
                        subtitle: _video?.name ?? '现场录制，最大500MB',
                        done: _video != null,
                        action: _video == null ? '录制' : '重录',
                        onTap: _captureVideo,
                      ),
                      _CaptureRow(
                        icon: Icons.photo_camera_outlined,
                        title: '现场拍照',
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
      bottomNavigationBar: !_canSubmitMaterials || _loadingAppointment
          ? null
          : SafeArea(
              minimum: const EdgeInsets.fromLTRB(10, 8, 10, 12),
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
    final style = appStatusStyle(context, status);
    final availableAt = record.jobReapplyAvailableAt;
    final blocked = availableAt != null && DateTime.now().isBefore(availableAt);
    final statusText = status == 'APPROVED'
        ? '已经通过认证'
        : status == 'REJECTED'
        ? (record.rejectionReason.isEmpty ? '认证未通过' : record.rejectionReason)
        : '正在审核';
    return Container(
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: style.background,
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
            color: style.foreground,
          ),
          const SizedBox(width: 10),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  statusText,
                  style: TextStyle(
                    color: style.foreground,
                    fontWeight: FontWeight.w600,
                  ),
                ),
                if (blocked) ...[
                  const SizedBox(height: 4),
                  Text(
                    '${DateFormat('yyyy年M月d日').format(availableAt)}后可再次申请岗位认证',
                    style: TextStyle(
                      color: style.foreground.withValues(alpha: 0.82),
                      fontSize: 12,
                    ),
                  ),
                ],
              ],
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
          : material.kind.toUpperCase() == 'AUDIO'
          ? Icons.mic_none_rounded
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

class _OfflineAppointmentCard extends StatelessWidget {
  const _OfflineAppointmentCard({required this.appointment});

  final JobCertificationAppointment appointment;

  @override
  Widget build(BuildContext context) {
    final awaitingResult = appointment.appointmentAt.isBefore(DateTime.now());
    final weekday = appointment.appointmentAt.weekday == DateTime.saturday
        ? '周六'
        : '周日';
    final time =
        '${DateFormat('M月d日').format(appointment.appointmentAt)} '
        '$weekday ${DateFormat('HH:mm').format(appointment.appointmentAt)}';
    return Container(
      padding: const EdgeInsets.all(15),
      decoration: BoxDecoration(
        color: Theme.of(context).colorScheme.primaryContainer,
        borderRadius: BorderRadius.circular(14),
      ),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Icon(
            Icons.event_available_outlined,
            color: Theme.of(context).colorScheme.primary,
          ),
          const SizedBox(width: 11),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  awaitingResult ? '线下认证待确认' : '已预约线下认证',
                  style: Theme.of(context).textTheme.titleMedium,
                ),
                const SizedBox(height: 4),
                Text('$time · ${appointment.city}'),
                const SizedBox(height: 5),
                Text(
                  awaitingResult
                      ? '工作人员确认结果后，您会收到认证通知。'
                      : '平台将在预约日前与您再次确认，请务必携带身份证原件。',
                  style: Theme.of(context).textTheme.bodySmall,
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

class _OfflineAppointmentSheet extends ConsumerStatefulWidget {
  const _OfflineAppointmentSheet();

  @override
  ConsumerState<_OfflineAppointmentSheet> createState() =>
      _OfflineAppointmentSheetState();
}

class _OfflineAppointmentSheetState
    extends ConsumerState<_OfflineAppointmentSheet> {
  late final List<DateTime> _dates = _upcomingWeekendDates();
  final List<int> _hours = const [11, 15];
  DateTime? _selectedDate;
  int? _selectedHour;
  bool _checkingAvailability = false;
  bool? _available;
  int _availabilityRequest = 0;

  DateTime? get _selectedSlot {
    if (_selectedDate == null || _selectedHour == null) return null;
    return DateTime(
      _selectedDate!.year,
      _selectedDate!.month,
      _selectedDate!.day,
      _selectedHour!,
    );
  }

  void _selectDate(DateTime date) {
    setState(() {
      _selectedDate = date;
      _available = null;
    });
    _checkAvailability();
  }

  void _selectHour(int hour) {
    setState(() {
      _selectedHour = hour;
      _available = null;
    });
    _checkAvailability();
  }

  Future<void> _checkAvailability() async {
    final slot = _selectedSlot;
    if (slot == null) return;
    final request = ++_availabilityRequest;
    setState(() => _checkingAvailability = true);
    try {
      final available = await ref
          .read(repositoryProvider)
          .jobCertificationAppointmentAvailable(slot);
      if (!mounted || request != _availabilityRequest) return;
      setState(() => _available = available);
    } catch (error) {
      if (!mounted || request != _availabilityRequest) return;
      setState(() => _available = null);
      AppMessage.show(context, '$error');
    } finally {
      if (mounted && request == _availabilityRequest) {
        setState(() => _checkingAvailability = false);
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Padding(
      padding: EdgeInsets.fromLTRB(
        16,
        0,
        16,
        16 + MediaQuery.viewInsetsOf(context).bottom,
      ),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text('预约线下认证', style: theme.textTheme.titleLarge),
          const SizedBox(height: 14),
          Container(
            width: double.infinity,
            padding: const EdgeInsets.all(14),
            decoration: BoxDecoration(
              color: theme.colorScheme.surfaceContainerHighest,
              borderRadius: BorderRadius.circular(14),
            ),
            child: const Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                _AppointmentNotice(text: '认证仅限北京地区，预约后由平台主动联系您'),
                _AppointmentNotice(text: '现场仅需携带本人身份证原件'),
                _AppointmentNotice(text: '现场以自然交流为主，并结合您的岗位进行即兴问答'),
              ],
            ),
          ),
          const SizedBox(height: 18),
          Text('选择日期', style: theme.textTheme.titleMedium),
          const SizedBox(height: 10),
          SizedBox(
            height: 70,
            child: ListView.separated(
              scrollDirection: Axis.horizontal,
              itemCount: _dates.length,
              separatorBuilder: (_, _) => const SizedBox(width: 8),
              itemBuilder: (context, index) {
                final date = _dates[index];
                final selected = _selectedDate == date;
                return _DateChoice(
                  date: date,
                  selected: selected,
                  onTap: () => _selectDate(date),
                );
              },
            ),
          ),
          const SizedBox(height: 18),
          Text('选择时间', style: theme.textTheme.titleMedium),
          const SizedBox(height: 10),
          Wrap(
            spacing: 8,
            runSpacing: 8,
            children: _hours
                .map((hour) {
                  final selected = _selectedHour == hour;
                  return ChoiceChip(
                    label: Text(hour == 11 ? '上午 11:00' : '下午 15:00'),
                    selected: selected,
                    showCheckmark: false,
                    onSelected: (_) => _selectHour(hour),
                  );
                })
                .toList(growable: false),
          ),
          const SizedBox(height: 14),
          if (_checkingAvailability)
            Text('正在确认该时间是否可预约…', style: theme.textTheme.bodySmall)
          else if (_available == false)
            Text(
              '该时间已被预约，请选择其他时间',
              style: theme.textTheme.bodySmall?.copyWith(
                color: theme.colorScheme.error,
                fontWeight: FontWeight.w600,
              ),
            )
          else if (_available == true)
            Text(
              '该时间可以预约',
              style: theme.textTheme.bodySmall?.copyWith(
                color: const Color(0xFF43855A),
                fontWeight: FontWeight.w600,
              ),
            )
          else
            const SizedBox(height: 16),
          const SizedBox(height: 14),
          SizedBox(
            width: double.infinity,
            child: FilledButton(
              onPressed: _available == true && !_checkingAvailability
                  ? () => Navigator.pop(context, _selectedSlot)
                  : null,
              child: _checkingAvailability
                  ? const SizedBox.square(
                      dimension: 18,
                      child: CircularProgressIndicator(
                        strokeWidth: 2,
                        color: Colors.white,
                      ),
                    )
                  : const Text('确认预约'),
            ),
          ),
        ],
      ),
    );
  }

  static List<DateTime> _upcomingWeekendDates() {
    final today = DateUtils.dateOnly(DateTime.now());
    final result = <DateTime>[];
    for (var offset = 1; result.length < 12; offset++) {
      final date = today.add(Duration(days: offset));
      if (date.weekday == DateTime.saturday ||
          date.weekday == DateTime.sunday) {
        result.add(date);
      }
    }
    return result;
  }
}

class _AppointmentNotice extends StatelessWidget {
  const _AppointmentNotice({required this.text});

  final String text;

  @override
  Widget build(BuildContext context) => Padding(
    padding: const EdgeInsets.symmetric(vertical: 4),
    child: Row(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Padding(
          padding: const EdgeInsets.only(top: 6),
          child: Container(
            width: 4,
            height: 4,
            decoration: BoxDecoration(
              color: Theme.of(context).colorScheme.primary,
              shape: BoxShape.circle,
            ),
          ),
        ),
        const SizedBox(width: 8),
        Expanded(child: Text(text)),
      ],
    ),
  );
}

class _DateChoice extends StatelessWidget {
  const _DateChoice({
    required this.date,
    required this.selected,
    required this.onTap,
  });

  final DateTime date;
  final bool selected;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final colors = Theme.of(context).colorScheme;
    return Material(
      color: selected ? colors.primary : colors.surfaceContainerHighest,
      borderRadius: BorderRadius.circular(13),
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(13),
        child: SizedBox(
          width: 76,
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Text(
                DateFormat('M月d日').format(date),
                style: TextStyle(
                  color: selected ? colors.onPrimary : colors.onSurface,
                  fontWeight: FontWeight.w700,
                ),
              ),
              const SizedBox(height: 3),
              Text(
                date.weekday == DateTime.saturday ? '周六' : '周日',
                style: TextStyle(
                  color: selected
                      ? colors.onPrimary.withValues(alpha: 0.78)
                      : colors.onSurfaceVariant,
                  fontSize: 11,
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
