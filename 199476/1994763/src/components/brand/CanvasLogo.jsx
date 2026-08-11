import { useEffect, useRef } from 'react';

const drawRoundedSquare = (context) => {
  context.fillStyle = '#c23b32';
  context.beginPath();
  context.roundRect(5, 5, 134, 134, 34);
  context.fill();
};

const drawQuestionWord = (context) => {
  context.fillStyle = '#fffdf9';
  context.font = '800 80px "Microsoft YaHei", "PingFang SC", sans-serif';
  context.textAlign = 'center';
  context.textBaseline = 'middle';
  context.fillText('问', 73, 75);

  context.fillStyle = '#c23b32';
  context.beginPath();
  context.roundRect(31, 24, 34, 31, 8);
  context.fill();
};

const drawFootprint = (context) => {
  context.save();
  context.translate(47, 38);
  context.rotate(-0.43);
  context.fillStyle = '#fffdf9';

  context.beginPath();
  context.moveTo(-1, 11);
  context.bezierCurveTo(-5.5, 9, -6.3, 3.5, -5.3, -1.5);
  context.bezierCurveTo(-4.4, -6.2, -1.2, -8.2, 2.2, -6.5);
  context.bezierCurveTo(5.6, -4.7, 6.1, 0.7, 4.7, 5.7);
  context.bezierCurveTo(3.7, 9.2, 1.8, 11.8, -1, 11);
  context.fill();

  const toes = [
    [-5.2, -7.3, 1.55],
    [-2.1, -10, 1.8],
    [1.5, -10.7, 1.95],
    [5.2, -9.2, 1.8],
    [7.8, -6.5, 1.45],
  ];

  toes.forEach(([x, y, radius]) => {
    context.beginPath();
    context.arc(x, y, radius, 0, Math.PI * 2);
    context.fill();
  });

  context.restore();
};

export default function CanvasLogo({ size = 40, className = '' }) {
  const canvasRef = useRef(null);

  useEffect(() => {
    const canvas = canvasRef.current;
    const pixelRatio = window.devicePixelRatio || 1;
    canvas.width = Math.round(size * pixelRatio);
    canvas.height = Math.round(size * pixelRatio);
    canvas.style.width = `${size}px`;
    canvas.style.height = `${size}px`;

    const context = canvas.getContext('2d');
    context.setTransform(pixelRatio, 0, 0, pixelRatio, 0, 0);
    context.clearRect(0, 0, size, size);
    context.save();
    context.scale(size / 144, size / 144);

    drawRoundedSquare(context);
    drawQuestionWord(context);
    drawFootprint(context);

    context.restore();
  }, [size]);

  return (
    <canvas
      ref={canvasRef}
      className={`canvas-logo ${className}`.trim()}
      role="img"
      aria-label="事先问"
    />
  );
}
