/**
 * Отрисовка боксов детекций поверх загруженного изображения.
 * Используется на странице «Загрузка кадра» после успешного ответа от /upload.
 */

(function (window) {
  'use strict';

  var BOX_COLORS = {
    'screen': '#0066ff',
    'glitches': '#ff6600',
    'glitch': '#ff6600',
    'dead-pixels-block': '#ff0000'
  };
  var DEFAULT_COLOR = '#9933ff';
  var LINE_WIDTH = 8;
  var LABEL_FONT = '17px sans-serif';

  var lastObjectUrl = null;

  /**
   * Показывает изображение из файла и рисует поверх него боксы.
   * Клик по картинке открывает её в полном размере в новой вкладке.
   */
  function drawDetectionBoxes(container, file, boxes) {
    if (!container || !file) return;
    boxes = boxes && boxes.length ? boxes : [];

    if (lastObjectUrl) {
      URL.revokeObjectURL(lastObjectUrl);
      lastObjectUrl = null;
    }
    container.innerHTML = '';
    var objectUrl = URL.createObjectURL(file);
    lastObjectUrl = objectUrl;

    var wrap = document.createElement('div');
    wrap.style.position = 'relative';
    wrap.style.display = 'inline-block';
    wrap.style.cursor = 'pointer';
    wrap.title = 'Клик — открыть в полном размере';
    wrap.addEventListener('click', function () {
      if (lastObjectUrl) window.open(lastObjectUrl, '_blank', 'noopener,noreferrer');
    });

    var img = document.createElement('img');
    img.style.display = 'block';
    img.style.maxWidth = '100%';
    img.style.maxHeight = '70vh';
    img.style.minHeight = '320px';
    img.style.objectFit = 'contain';
    img.alt = 'Загруженный кадр (клик — открыть в полном размере)';

    img.onload = function () {
      wrap.appendChild(img);

      if (boxes.length > 0) {
        var naturalWidth = img.naturalWidth;
        var naturalHeight = img.naturalHeight;
        var displayWidth = img.offsetWidth;
        var displayHeight = img.offsetHeight;
        var canvas = createOverlayCanvas(displayWidth, displayHeight);
        var ctx = canvas.getContext('2d');
        boxes.forEach(function (box) {
          drawOneBox(ctx, box, naturalWidth, naturalHeight, displayWidth, displayHeight);
        });
        wrap.appendChild(canvas);
      }
      container.appendChild(wrap);

      var openBtn = document.createElement('button');
      openBtn.type = 'button';
      openBtn.className = 'btn btn-secondary';
      openBtn.textContent = 'Открыть в полном размере';
      openBtn.style.marginTop = '0.5rem';
      openBtn.addEventListener('click', function () {
        if (lastObjectUrl) window.open(lastObjectUrl, '_blank', 'noopener,noreferrer');
      });
      container.appendChild(openBtn);
    };

    img.onerror = function () {
      URL.revokeObjectURL(objectUrl);
      lastObjectUrl = null;
    };

    img.src = objectUrl;
  }

  /**
   * Создаёт canvas поверх изображения (тот же размер, абсолютное позиционирование).
   */
  function createOverlayCanvas(width, height) {
    var canvas = document.createElement('canvas');
    canvas.width = width;
    canvas.height = height;
    canvas.style.position = 'absolute';
    canvas.style.left = '0';
    canvas.style.top = '0';
    canvas.style.width = width + 'px';
    canvas.style.height = height + 'px';
    canvas.style.pointerEvents = 'none';
    canvas.style.boxSizing = 'border-box';
    return canvas;
  }

  /**
   * Рисует один бокс и подпись (класс + уверенность в %).
   * Координаты бокса приходят в пикселях исходного изображения — пересчитываем в размер превью.
   */
  function drawOneBox(ctx, box, natW, natH, dispW, dispH) {
    var x = (box.x / natW) * dispW;
    var y = (box.y / natH) * dispH;
    var w = (box.width / natW) * dispW;
    var h = (box.height / natH) * dispH;

    var color = BOX_COLORS[box.class] || DEFAULT_COLOR;
    var cls = (box.class || '').toLowerCase();

    ctx.setLineDash([]);
    ctx.lineWidth = LINE_WIDTH + 6;
    ctx.strokeStyle = 'rgba(0,0,0,0.95)';
    ctx.strokeRect(x, y, w, h);
    ctx.lineWidth = LINE_WIDTH + 3;
    ctx.strokeStyle = 'rgba(255,255,255,1)';
    ctx.strokeRect(x, y, w, h);
    ctx.lineWidth = LINE_WIDTH;
    ctx.strokeStyle = color;
    ctx.strokeRect(x, y, w, h);

    var confidencePercent = box.confidence != null ? Math.round(box.confidence * 100) + '%' : '';
    var label = (cls ? cls + ' ' : '') + (confidencePercent || '');
    ctx.font = LABEL_FONT;
    var textY = Math.max(20, y - 10);
    ctx.fillStyle = 'rgba(0,0,0,0.9)';
    ctx.fillText(label, x + 4, textY + 4);
    ctx.fillStyle = '#fff';
    ctx.fillText(label, x + 2, textY + 2);
    ctx.fillStyle = color;
    ctx.fillText(label, x, textY);
  }

  function revokePreviewUrl() {
    if (lastObjectUrl) {
      URL.revokeObjectURL(lastObjectUrl);
      lastObjectUrl = null;
    }
  }

  window.UploadPreview = {
    drawDetectionBoxes: drawDetectionBoxes,
    revokePreviewUrl: revokePreviewUrl
  };
})(window);
