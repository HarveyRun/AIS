import 'package:flutter/material.dart';
import 'package:url_launcher/url_launcher.dart';
import 'package:video_player/video_player.dart';

import '../../core/config/app_config.dart';
import '../../data/models/certification_models.dart';

Future<void> openMaterial(
  BuildContext context,
  CertificationMaterial material,
) async {
  final kind = material.kind.toUpperCase();
  if (const [
    'ARCHIVE',
    'PROOF_ARCHIVE',
    'REVIEW_ORIGINAL_ARCHIVE',
  ].contains(kind)) {
    await launchUrl(
      AppConfig.resolveResource(material.url),
      mode: LaunchMode.externalApplication,
    );
    return;
  }
  if (!context.mounted) return;
  await _openMediaGallery(context, material.name, [material]);
}

Future<void> openPhotoGallery(
  BuildContext context,
  List<CertificationMaterial> photos,
) => _openMediaGallery(context, '用户照片', photos);

Future<void> openMaterialList(
  BuildContext context,
  String title,
  List<CertificationMaterial> materials,
) => _openMediaGallery(context, title, materials);

Future<void> _openMediaGallery(
  BuildContext context,
  String title,
  List<CertificationMaterial> materials,
) async {
  if (materials.isEmpty || !context.mounted) return;
  await Navigator.of(context, rootNavigator: true).push(
    MaterialPageRoute(
      builder: (_) =>
          PublicMediaGalleryPage(title: title, materials: materials),
    ),
  );
}

class PublicMediaGalleryPage extends StatefulWidget {
  const PublicMediaGalleryPage({
    super.key,
    required this.title,
    required this.materials,
  });

  final String title;
  final List<CertificationMaterial> materials;

  @override
  State<PublicMediaGalleryPage> createState() => _PublicMediaGalleryPageState();
}

class _PublicMediaGalleryPageState extends State<PublicMediaGalleryPage> {
  final TransformationController _imageTransform = TransformationController();

  int _index = 0;
  int _quarterTurns = 0;
  int _loadGeneration = 0;
  bool _loading = false;
  String? _error;
  VideoPlayerController? _video;

  CertificationMaterial get _current => widget.materials[_index];
  String get _kind => _current.kind.toUpperCase();
  bool get _isVideo => const ['VIDEO', 'DETAIL_VIDEO'].contains(_kind);

  @override
  void initState() {
    super.initState();
    _prepareCurrent();
  }

  @override
  void dispose() {
    _loadGeneration++;
    _video?.dispose();
    _imageTransform.dispose();
    super.dispose();
  }

  Future<void> _prepareCurrent() async {
    final generation = ++_loadGeneration;
    final previousVideo = _video;
    _video = null;
    await previousVideo?.dispose();
    _imageTransform.value = Matrix4.identity();

    if (!mounted || generation != _loadGeneration) return;
    setState(() {
      _quarterTurns = 0;
      _loading = _isVideo || _kind == 'AUDIO';
      _error = null;
    });

    try {
      if (_isVideo || _kind == 'AUDIO') {
        final controller = VideoPlayerController.networkUrl(
          AppConfig.resolveResource(_current.url),
        );
        _video = controller;
        await controller.initialize();
        if (!mounted || generation != _loadGeneration) {
          await controller.dispose();
          return;
        }
        await controller.setLooping(false);
      }
      if (!mounted || generation != _loadGeneration) return;
      setState(() => _loading = false);
    } catch (_) {
      if (!mounted || generation != _loadGeneration) return;
      setState(() {
        _loading = false;
        _error = _kind == 'AUDIO' ? '音频加载失败，请稍后重试' : '视频加载失败，请稍后重试';
      });
    }
  }

  Future<void> _goTo(int index) async {
    if (index < 0 || index >= widget.materials.length || index == _index) {
      return;
    }
    setState(() => _index = index);
    await _prepareCurrent();
  }

  Future<void> _toggleVideo() async {
    final video = _video;
    if (video == null || !video.value.isInitialized) return;
    if (video.value.position >= video.value.duration) {
      await video.seekTo(Duration.zero);
    }
    video.value.isPlaying ? await video.pause() : await video.play();
    if (mounted) setState(() {});
  }

  Future<void> _seekVideo(Duration delta) async {
    final video = _video;
    if (video == null || !video.value.isInitialized) return;
    final target = video.value.position + delta;
    final milliseconds = target.inMilliseconds.clamp(
      0,
      video.value.duration.inMilliseconds,
    );
    await video.seekTo(Duration(milliseconds: milliseconds));
  }

  Future<void> _toggleAudio() async {
    await _toggleVideo();
  }

  Future<void> _seekAudio(double milliseconds) async {
    await _video?.seekTo(Duration(milliseconds: milliseconds.round()));
  }

  void _rotateImage(int amount) {
    setState(() {
      _quarterTurns = (_quarterTurns + amount) % 4;
      if (_quarterTurns < 0) _quarterTurns += 4;
      _imageTransform.value = Matrix4.identity();
    });
  }

  String _formatDuration(Duration value) {
    final totalSeconds = value.inSeconds.clamp(0, 359999);
    final hours = totalSeconds ~/ 3600;
    final minutes = (totalSeconds % 3600) ~/ 60;
    final seconds = totalSeconds % 60;
    if (hours > 0) {
      return '${hours.toString().padLeft(2, '0')}:'
          '${minutes.toString().padLeft(2, '0')}:'
          '${seconds.toString().padLeft(2, '0')}';
    }
    return '${minutes.toString().padLeft(2, '0')}:'
        '${seconds.toString().padLeft(2, '0')}';
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.black,
      appBar: AppBar(
        backgroundColor: Colors.black,
        foregroundColor: Colors.white,
        title: Text(widget.title),
        actions: [
          if (_kind == 'IMAGE') ...[
            IconButton(
              onPressed: () => _rotateImage(-1),
              tooltip: '向左旋转',
              icon: const Icon(Icons.rotate_left_rounded),
            ),
            IconButton(
              onPressed: () => _rotateImage(1),
              tooltip: '向右旋转',
              icon: const Icon(Icons.rotate_right_rounded),
            ),
          ],
          if (widget.materials.length > 1)
            Center(
              child: Padding(
                padding: const EdgeInsets.only(left: 6, right: 16),
                child: Text(
                  '${_index + 1}/${widget.materials.length}',
                  style: const TextStyle(color: Colors.white70),
                ),
              ),
            ),
        ],
      ),
      body: Column(
        children: [
          Expanded(child: _preview()),
          _navigationBar(),
        ],
      ),
    );
  }

  Widget _preview() {
    if (_error != null) {
      return _LoadFailure(message: _error!, onRetry: _prepareCurrent);
    }
    if (_loading) {
      return const Center(
        child: CircularProgressIndicator(color: Colors.white),
      );
    }
    return switch (_kind) {
      'IMAGE' => _imagePreview(),
      'VIDEO' || 'DETAIL_VIDEO' => _videoPreview(),
      'AUDIO' => _audioPreview(),
      _ => const Center(
        child: Icon(
          Icons.insert_drive_file_outlined,
          color: Colors.white70,
          size: 48,
        ),
      ),
    };
  }

  Widget _imagePreview() {
    return Stack(
      children: [
        Positioned.fill(
          child: InteractiveViewer(
            transformationController: _imageTransform,
            minScale: 0.7,
            maxScale: 5,
            child: Center(
              child: RotatedBox(
                quarterTurns: _quarterTurns,
                child: Image.network(
                  AppConfig.resolveImage(_current.url).toString(),
                  fit: BoxFit.contain,
                  loadingBuilder: (context, child, progress) {
                    if (progress == null) return child;
                    return const Center(
                      child: CircularProgressIndicator(color: Colors.white),
                    );
                  },
                  errorBuilder: (_, _, _) => const _MediaError(
                    icon: Icons.broken_image_outlined,
                    text: '图片加载失败',
                  ),
                ),
              ),
            ),
          ),
        ),
        Positioned(
          right: 12,
          bottom: 12,
          child: DecoratedBox(
            decoration: BoxDecoration(
              color: Colors.black.withValues(alpha: 0.55),
              borderRadius: BorderRadius.circular(18),
            ),
            child: const Padding(
              padding: EdgeInsets.symmetric(horizontal: 12, vertical: 7),
              child: Text(
                '双指缩放',
                style: TextStyle(color: Colors.white70, fontSize: 12),
              ),
            ),
          ),
        ),
      ],
    );
  }

  Widget _videoPreview() {
    final video = _video;
    if (video == null || !video.value.isInitialized) {
      return _LoadFailure(message: '视频加载失败，请稍后重试', onRetry: _prepareCurrent);
    }
    return AnimatedBuilder(
      animation: video,
      builder: (context, _) {
        final value = video.value;
        final max = value.duration.inMilliseconds
            .toDouble()
            .clamp(1, double.infinity)
            .toDouble();
        final position = value.position.inMilliseconds
            .toDouble()
            .clamp(0, max)
            .toDouble();
        return Column(
          children: [
            Expanded(
              child: GestureDetector(
                behavior: HitTestBehavior.opaque,
                onTap: _toggleVideo,
                child: Stack(
                  alignment: Alignment.center,
                  children: [
                    Center(
                      child: AspectRatio(
                        aspectRatio: value.aspectRatio > 0
                            ? value.aspectRatio
                            : 16 / 9,
                        child: VideoPlayer(video),
                      ),
                    ),
                    if (value.isBuffering)
                      const CircularProgressIndicator(color: Colors.white),
                    if (!value.isPlaying && !value.isBuffering)
                      Container(
                        width: 68,
                        height: 68,
                        decoration: const BoxDecoration(
                          color: Color(0xB3000000),
                          shape: BoxShape.circle,
                        ),
                        child: const Icon(
                          Icons.play_arrow_rounded,
                          color: Colors.white,
                          size: 42,
                        ),
                      ),
                  ],
                ),
              ),
            ),
            Container(
              color: const Color(0xFF111111),
              padding: const EdgeInsets.fromLTRB(12, 8, 12, 10),
              child: Row(
                children: [
                  IconButton(
                    onPressed: _toggleVideo,
                    color: Colors.white,
                    icon: Icon(
                      value.isPlaying
                          ? Icons.pause_rounded
                          : Icons.play_arrow_rounded,
                    ),
                  ),
                  IconButton(
                    onPressed: () => _seekVideo(const Duration(seconds: -10)),
                    color: Colors.white70,
                    tooltip: '后退10秒',
                    icon: const Icon(Icons.replay_10_rounded),
                  ),
                  Expanded(
                    child: SliderTheme(
                      data: SliderTheme.of(context).copyWith(
                        trackHeight: 2,
                        thumbShape: const RoundSliderThumbShape(
                          enabledThumbRadius: 6,
                        ),
                        overlayShape: const RoundSliderOverlayShape(
                          overlayRadius: 14,
                        ),
                      ),
                      child: Slider(
                        value: position,
                        max: max,
                        activeColor: Colors.white,
                        inactiveColor: Colors.white24,
                        onChanged: (next) =>
                            video.seekTo(Duration(milliseconds: next.round())),
                      ),
                    ),
                  ),
                  Text(
                    '${_formatDuration(value.position)} / '
                    '${_formatDuration(value.duration)}',
                    style: const TextStyle(color: Colors.white70, fontSize: 12),
                  ),
                  IconButton(
                    onPressed: () => _seekVideo(const Duration(seconds: 10)),
                    color: Colors.white70,
                    tooltip: '快进10秒',
                    icon: const Icon(Icons.forward_10_rounded),
                  ),
                ],
              ),
            ),
          ],
        );
      },
    );
  }

  Widget _audioPreview() {
    final audio = _video;
    if (audio == null || !audio.value.isInitialized) {
      return _LoadFailure(message: '音频加载失败，请稍后重试', onRetry: _prepareCurrent);
    }
    return AnimatedBuilder(
      animation: audio,
      builder: (context, _) {
        final value = audio.value;
        final max = value.duration.inMilliseconds
            .toDouble()
            .clamp(1, double.infinity)
            .toDouble();
        final position = value.position.inMilliseconds
            .toDouble()
            .clamp(0, max)
            .toDouble();
        return Center(
          child: Container(
            width: double.infinity,
            margin: const EdgeInsets.symmetric(horizontal: 24),
            padding: const EdgeInsets.fromLTRB(22, 28, 22, 22),
            decoration: BoxDecoration(
              color: const Color(0xFF171717),
              borderRadius: BorderRadius.circular(24),
            ),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                Container(
                  width: 84,
                  height: 84,
                  decoration: const BoxDecoration(
                    color: Color(0xFF292929),
                    shape: BoxShape.circle,
                  ),
                  child: Icon(
                    value.isPlaying
                        ? Icons.graphic_eq_rounded
                        : Icons.multitrack_audio_rounded,
                    color: Colors.white,
                    size: 42,
                  ),
                ),
                const SizedBox(height: 18),
                Text(
                  _current.name,
                  maxLines: 2,
                  overflow: TextOverflow.ellipsis,
                  textAlign: TextAlign.center,
                  style: const TextStyle(
                    color: Colors.white,
                    fontSize: 16,
                    fontWeight: FontWeight.w600,
                  ),
                ),
                const SizedBox(height: 20),
                SliderTheme(
                  data: SliderTheme.of(context).copyWith(
                    trackHeight: 3,
                    thumbShape: const RoundSliderThumbShape(
                      enabledThumbRadius: 7,
                    ),
                    overlayShape: const RoundSliderOverlayShape(
                      overlayRadius: 16,
                    ),
                  ),
                  child: Slider(
                    value: position,
                    max: max,
                    activeColor: Colors.white,
                    inactiveColor: Colors.white24,
                    onChanged: _seekAudio,
                  ),
                ),
                Padding(
                  padding: const EdgeInsets.symmetric(horizontal: 8),
                  child: Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      Text(
                        _formatDuration(value.position),
                        style: const TextStyle(
                          color: Colors.white54,
                          fontSize: 12,
                        ),
                      ),
                      Text(
                        _formatDuration(value.duration),
                        style: const TextStyle(
                          color: Colors.white54,
                          fontSize: 12,
                        ),
                      ),
                    ],
                  ),
                ),
                const SizedBox(height: 10),
                IconButton.filled(
                  onPressed: _toggleAudio,
                  style: IconButton.styleFrom(
                    backgroundColor: Colors.white,
                    foregroundColor: Colors.black,
                    minimumSize: const Size(58, 58),
                  ),
                  icon: Icon(
                    value.isPlaying
                        ? Icons.pause_rounded
                        : Icons.play_arrow_rounded,
                    size: 34,
                  ),
                ),
              ],
            ),
          ),
        );
      },
    );
  }

  Widget _navigationBar() {
    if (widget.materials.length <= 1) return const SizedBox.shrink();
    return Container(
      color: const Color(0xFF111111),
      padding: const EdgeInsets.fromLTRB(8, 6, 8, 8),
      child: SafeArea(
        top: false,
        child: Row(
          children: [
            Expanded(
              child: TextButton.icon(
                onPressed: _index == 0 ? null : () => _goTo(_index - 1),
                icon: const Icon(Icons.chevron_left_rounded),
                label: const Text('上一个'),
                style: TextButton.styleFrom(
                  foregroundColor: Colors.white,
                  disabledForegroundColor: Colors.white24,
                ),
              ),
            ),
            Expanded(
              child: Text(
                _current.name,
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
                textAlign: TextAlign.center,
                style: const TextStyle(color: Colors.white60, fontSize: 12),
              ),
            ),
            Expanded(
              child: TextButton.icon(
                onPressed: _index == widget.materials.length - 1
                    ? null
                    : () => _goTo(_index + 1),
                label: const Text('下一个'),
                icon: const Icon(Icons.chevron_right_rounded),
                iconAlignment: IconAlignment.end,
                style: TextButton.styleFrom(
                  foregroundColor: Colors.white,
                  disabledForegroundColor: Colors.white24,
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _LoadFailure extends StatelessWidget {
  const _LoadFailure({required this.message, required this.onRetry});

  final String message;
  final VoidCallback onRetry;

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          const Icon(
            Icons.error_outline_rounded,
            color: Colors.white54,
            size: 42,
          ),
          const SizedBox(height: 12),
          Text(message, style: const TextStyle(color: Colors.white70)),
          const SizedBox(height: 8),
          TextButton(onPressed: onRetry, child: const Text('重新加载')),
        ],
      ),
    );
  }
}

class _MediaError extends StatelessWidget {
  const _MediaError({required this.icon, required this.text});

  final IconData icon;
  final String text;

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          Icon(icon, color: Colors.white54, size: 44),
          const SizedBox(height: 10),
          Text(text, style: const TextStyle(color: Colors.white70)),
        ],
      ),
    );
  }
}
