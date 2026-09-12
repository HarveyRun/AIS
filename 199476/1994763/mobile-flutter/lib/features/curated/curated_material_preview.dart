import 'dart:io';

import 'package:flutter/material.dart';
import 'package:video_player/video_player.dart';

import '../../data/models/certification_models.dart';
import '../../data/models/curated_chat_models.dart';
import '../certification/material_viewer.dart';

Future<void> openSubmittedCuratedMaterials(
  BuildContext context,
  String title,
  List<CuratedCertificationMaterial> materials,
) {
  return openMaterialList(
    context,
    title,
    materials
        .map(
          (item) => CertificationMaterial(
            id: item.id,
            kind: item.mediaType,
            name: item.originalName,
            url: item.url,
            size: item.fileSize,
            contentType: '',
          ),
        )
        .toList(growable: false),
  );
}

Future<void> openLocalPhotoPreview(
  BuildContext context,
  String path, {
  required String title,
}) => _openLocalPreview(context, path, title: title, video: false);

Future<void> openLocalVideoPreview(
  BuildContext context,
  String path, {
  required String title,
}) => _openLocalPreview(context, path, title: title, video: true);

Future<void> _openLocalPreview(
  BuildContext context,
  String path, {
  required String title,
  required bool video,
}) async {
  if (!context.mounted) return;
  await Navigator.of(context, rootNavigator: true).push(
    MaterialPageRoute(
      builder: (_) =>
          _LocalMaterialPreviewPage(path: path, title: title, video: video),
    ),
  );
}

class _LocalMaterialPreviewPage extends StatefulWidget {
  const _LocalMaterialPreviewPage({
    required this.path,
    required this.title,
    required this.video,
  });

  final String path;
  final String title;
  final bool video;

  @override
  State<_LocalMaterialPreviewPage> createState() =>
      _LocalMaterialPreviewPageState();
}

class _LocalMaterialPreviewPageState extends State<_LocalMaterialPreviewPage> {
  VideoPlayerController? _controller;
  String? _error;

  @override
  void initState() {
    super.initState();
    if (widget.video) _prepareVideo();
  }

  Future<void> _prepareVideo() async {
    try {
      final controller = VideoPlayerController.file(File(widget.path));
      _controller = controller;
      await controller.initialize();
      if (!mounted) {
        await controller.dispose();
        return;
      }
      setState(() {});
    } catch (_) {
      if (mounted) setState(() => _error = '录像加载失败');
    }
  }

  @override
  void dispose() {
    _controller?.dispose();
    super.dispose();
  }

  Future<void> _toggleVideo() async {
    final controller = _controller;
    if (controller == null || !controller.value.isInitialized) return;
    if (controller.value.position >= controller.value.duration) {
      await controller.seekTo(Duration.zero);
    }
    controller.value.isPlaying
        ? await controller.pause()
        : await controller.play();
    if (mounted) setState(() {});
  }

  @override
  Widget build(BuildContext context) => Scaffold(
    backgroundColor: Colors.black,
    appBar: AppBar(
      backgroundColor: Colors.black,
      foregroundColor: Colors.white,
      title: Text(widget.title),
    ),
    body: Center(child: widget.video ? _videoPreview() : _photoPreview()),
  );

  Widget _photoPreview() => InteractiveViewer(
    minScale: 0.7,
    maxScale: 5,
    child: Image.file(
      File(widget.path),
      fit: BoxFit.contain,
      errorBuilder: (_, _, _) => const _PreviewError(text: '图片加载失败'),
    ),
  );

  Widget _videoPreview() {
    if (_error != null) return _PreviewError(text: _error!);
    final controller = _controller;
    if (controller == null || !controller.value.isInitialized) {
      return const CircularProgressIndicator(color: Colors.white);
    }
    return GestureDetector(
      behavior: HitTestBehavior.opaque,
      onTap: _toggleVideo,
      child: Stack(
        alignment: Alignment.center,
        children: [
          AspectRatio(
            aspectRatio: controller.value.aspectRatio > 0
                ? controller.value.aspectRatio
                : 16 / 9,
            child: VideoPlayer(controller),
          ),
          if (!controller.value.isPlaying)
            Container(
              width: 64,
              height: 64,
              decoration: const BoxDecoration(
                color: Color(0xA6000000),
                shape: BoxShape.circle,
              ),
              child: const Icon(
                Icons.play_arrow_rounded,
                color: Colors.white,
                size: 40,
              ),
            ),
        ],
      ),
    );
  }
}

class _PreviewError extends StatelessWidget {
  const _PreviewError({required this.text});
  final String text;

  @override
  Widget build(BuildContext context) => Column(
    mainAxisSize: MainAxisSize.min,
    children: [
      const Icon(Icons.broken_image_outlined, color: Colors.white70, size: 44),
      const SizedBox(height: 10),
      Text(text, style: const TextStyle(color: Colors.white70)),
    ],
  );
}
