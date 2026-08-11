import { useEffect, useRef, useState } from 'react';
import { Camera, CircleStop, RotateCcw, Video, X } from 'lucide-react';

export default function CameraCapture({ mode, facingMode = 'environment', onCapture, onClose }) {
  const videoRef = useRef(null);
  const streamRef = useRef(null);
  const recorderRef = useRef(null);
  const chunksRef = useRef([]);
  const [ready, setReady] = useState(false);
  const [recording, setRecording] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    let active = true;

    const startCamera = async () => {
      try {
        const stream = await navigator.mediaDevices.getUserMedia({
          video: { facingMode },
          audio: mode === 'video',
        });

        if (!active) {
          stream.getTracks().forEach((track) => track.stop());
          return;
        }

        streamRef.current = stream;
        videoRef.current.srcObject = stream;
        await videoRef.current.play();
        setReady(true);
      } catch {
        setError('无法使用摄像头，请允许相机权限后重试');
      }
    };

    startCamera();

    return () => {
      active = false;
      recorderRef.current?.state === 'recording' && recorderRef.current.stop();
      streamRef.current?.getTracks().forEach((track) => track.stop());
    };
  }, [facingMode, mode]);

  const takePhoto = () => {
    const video = videoRef.current;
    const canvas = document.createElement('canvas');
    canvas.width = video.videoWidth;
    canvas.height = video.videoHeight;
    canvas.getContext('2d').drawImage(video, 0, 0, canvas.width, canvas.height);
    canvas.toBlob(
      (blob) => {
        if (blob) onCapture(blob);
      },
      'image/jpeg',
      0.9,
    );
  };

  const startRecording = () => {
    try {
      chunksRef.current = [];
      const recorder = new MediaRecorder(streamRef.current);
      recorderRef.current = recorder;
      recorder.ondataavailable = (event) => {
        if (event.data.size) chunksRef.current.push(event.data);
      };
      recorder.onstop = () => {
        const blob = new Blob(chunksRef.current, { type: recorder.mimeType || 'video/webm' });
        onCapture(blob);
      };
      recorder.onerror = () => {
        setRecording(false);
        setError('录像失败，请关闭后重新录制');
      };
      recorder.start();
      setRecording(true);
    } catch {
      setError('当前浏览器不支持录像，请更换浏览器后重试');
    }
  };

  const stopRecording = () => {
    recorderRef.current?.stop();
    setRecording(false);
  };

  return (
    <div className="camera-capture" role="dialog" aria-modal="true">
      <header>
        <div>
          {mode === 'photo' ? <Camera /> : <Video />}
          <b>{mode === 'photo' ? '拍摄照片' : '录制录像'}</b>
        </div>
        <button type="button" aria-label="关闭相机" onClick={onClose}>
          <X />
        </button>
      </header>

      <div className="camera-preview">
        <video ref={videoRef} muted playsInline />
        {!ready && !error && <span>正在打开摄像头…</span>}
        {error && (
          <div className="camera-error">
            <RotateCcw />
            <p>{error}</p>
          </div>
        )}
        {recording && <em>录制中</em>}
      </div>

      {!error && (
        <footer>
          {mode === 'photo' && (
            <button className="camera-shutter" type="button" disabled={!ready} onClick={takePhoto}>
              <i />
            </button>
          )}
          {mode === 'video' && !recording && (
            <button
              className="camera-record"
              type="button"
              disabled={!ready}
              onClick={startRecording}
            >
              <i />
              开始录制
            </button>
          )}
          {mode === 'video' && recording && (
            <button className="camera-record stop" type="button" onClick={stopRecording}>
              <CircleStop />
              结束录制
            </button>
          )}
        </footer>
      )}
    </div>
  );
}
