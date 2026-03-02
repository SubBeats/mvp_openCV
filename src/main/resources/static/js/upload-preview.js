/**
 * Отрисовка боксов детекций поверх загруженного изображения.
 * Используется на странице «Загрузка кадра» после успешного ответа от /upload.
 */

(function (window) {
  'use strict';

  // Цвета рамок по классу детекции (экран, глитчи, битые блоки)
  var BOX_COLORS = {
    'screen': '#3b82f6',
    'glitches': '#f59e0b',
    'glitch': '#f59e0b',
    'dead-pixels-block': '#ef4444'
  };

  var DEFAULT_COLOR = '#6366f1';

  /**
   * Показывает изображение из файла и рисует поверх него боксы из ответа API.
   *
   * @param {HTMLElement} container - контейнер (в него будут добавлены img и canvas)
   * @param {File} file - файл изображения, который загружали
   * @param {Array} boxes - массив боксов из ответа: [{ class, x, y, width, height, confidence }]
   */
  function drawDetectionBoxes(container, file, boxes) {
    if (!container || !file) return;
    boxes = boxes && boxes.length ? boxes : [];

    container.innerHTML = '';
    var img = document.createElement('img');
    img.style.display = 'block';
    img.style.maxWidth = '100%';
    img.style.maxHeight = '400px';
    img.style.objectFit = 'contain';
    img.alt = 'Загруженный кадр';

    var objectUrl = URL.createObjectURL(file);

    img.onload = function () {
      container.appendChild(img);

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
        container.appendChild(canvas);
      }
      URL.revokeObjectURL(objectUrl);
    };

    img.onerror = function () {
      URL.revokeObjectURL(objectUrl);
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
    return canvas;
  }

  /**
   * Рисует один бокс и подпись (класс + уверенность в %).
   * Координаты бокса приходят в пикселях исходного изображения — пересчитываем в размер превью.
   */
  function drawOneBox(ctx, box, natW, natH, dispW, dispH) {
    var scaleX = dispW / natW;
    var scaleY = dispH / natH;

    var x = (box.x / natW) * dispW;
    var y = (box.y / natH) * dispH;
    var w = (box.width / natW) * dispW;
    var h = (box.height / natH) * dispH;

    var color = BOX_COLORS[box.class] || DEFAULT_COLOR;

    ctx.strokeStyle = color;
    ctx.lineWidth = 2;
    ctx.strokeRect(x, y, w, h);

    var confidencePercent = box.confidence != null ? Math.round(box.confidence * 100) + '%' : '';
    var label = (box.class || '') + (confidencePercent ? ' ' + confidencePercent : '');
    ctx.fillStyle = color;
    ctx.font = '12px sans-serif';
    ctx.fillText(label, x, Math.max(12, y - 4));
  }

  window.UploadPreview = {
    drawDetectionBoxes: drawDetectionBoxes
  };
})(window);
