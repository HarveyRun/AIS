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
  final uri = AppConfig.resolveResource(material.url);
  if (kind == 'ARCHIVE') {
    await launchUrl(uri, mode: LaunchMode.externalApplication);
    return;
  }
  if (!context.mounted) return;
  await Navigator.push(
    context,
    MaterialPageRoute(builder: (_) => MaterialViewerPage(material: material)),
  );
}

class MaterialViewerPage extends StatefulWidget {
  const MaterialViewerPage({super.key, required this.material});
  final CertificationMaterial material;
  @override
  State<MaterialViewerPage> createState() => _MaterialViewerPageState();
}

class _MaterialViewerPageState extends State<MaterialViewerPage> {
  VideoPlayerController? _video;
  Future<void>? _initializing;
  @override
  void initState() {
    super.initState();
    if (widget.material.kind.toUpperCase() == 'VIDEO') {
      _video = VideoPlayerController.networkUrl(
        AppConfig.resolveResource(widget.material.url),
      );
      _initializing = _video!.initialize();
    }
  }

  @override
  void dispose() {
    _video?.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) => Scaffold(
    backgroundColor: Colors.black,
    appBar: AppBar(
      backgroundColor: Colors.black,
      foregroundColor: Colors.white,
      title: Text(widget.material.name),
    ),
    body: Center(
      child: _video == null
          ? InteractiveViewer(
              child: Image.network(
                AppConfig.resolveImage(widget.material.url).toString(),
                fit: BoxFit.contain,
              ),
            )
          : FutureBuilder<void>(
              future: _initializing,
              builder: (context, snapshot) {
                if (snapshot.connectionState != ConnectionState.done) {
                  return const CircularProgressIndicator();
                }
                return GestureDetector(
                  onTap: () => setState(
                    () => _video!.value.isPlaying
                        ? _video!.pause()
                        : _video!.play(),
                  ),
                  child: AspectRatio(
                    aspectRatio: _video!.value.aspectRatio,
                    child: VideoPlayer(_video!),
                  ),
                );
              },
            ),
    ),
    floatingActionButton: _video == null
        ? null
        : FloatingActionButton(
            onPressed: () => setState(
              () => _video!.value.isPlaying ? _video!.pause() : _video!.play(),
            ),
            child: Icon(
              _video!.value.isPlaying
                  ? Icons.pause_rounded
                  : Icons.play_arrow_rounded,
            ),
          ),
  );
}
