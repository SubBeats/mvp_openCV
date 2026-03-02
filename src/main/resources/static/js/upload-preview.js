/**
 * Превью загруженного кадра и отрисовка боксов детекций.
 * Классы детекции приходят с бэка в одном формате: screen, glitches, dead-pixels-block.
 */
(function (window) {
  'use strict';

  var DETECTION_CLASSES = {
    'screen':            { color: '#00ffff', labelRu: 'Экран' },
    'glitches':          { color: '#ff0000', labelRu: 'Глитчи' },
    'dead-pixels-block': { color: '#ff0000', labelRu: 'Битые блоки' }
  };
  var DEFAULT_COLOR = '#cc00ff';
  var LINE_WIDTH = 4;
  var LABEL_FONT = 'bold 18px sans-serif';

  var lastObjectUrl = null;

  function normalizeDetectionClass(apiClass) {
    return (apiClass + '').toLowerCase().trim();
  }

  function getClassLabelRu(apiClass) {
    var key = normalizeDetectionClass(apiClass);
    var def = DETECTION_CLASSES[key];
    return def ? def.labelRu : apiClass || '';
  }

  function getColorForClass(apiClass) {
    var key = normalizeDetectionClass(apiClass);
    var def = DETECTION_CLASSES[key];
    return def ? def.color : DEFAULT_COLOR;
  }

  function drawDetectionBoxes(container, file, boxes) {
    if (!container || !file) return;
    boxes = Array.isArray(boxes) ? boxes : [];

    revokeLastObjectUrl();
    container.innerHTML = '';

    var objectUrl = URL.createObjectURL(file);
    lastObjectUrl = objectUrl;

    var wrap = document.createElement('div');
    wrap.style.position = 'relative';
    wrap.style.display = 'inline-block';

    var img = document.createElement('img');
    img.style.display = 'block';
    img.style.maxWidth = '100%';
    img.style.maxHeight = '85vh';
    img.style.minHeight = '400px';
    img.style.objectFit = 'contain';
    img.alt = 'Загруженный кадр';

    img.onload = function () {
      wrap.appendChild(img);
      container.appendChild(wrap);
      if (boxes.length > 0) {
        var imgEl = img;
        var boxesCopy = boxes.slice();
        requestAnimationFrame(function () {
          var canvas = buildOverlayCanvas(imgEl, boxesCopy);
          wrap.appendChild(canvas);
        });
      }
    };

    img.onerror = function () {
      revokeLastObjectUrl();
      lastObjectUrl = null;
    };

    img.src = objectUrl;
  }

  function revokeLastObjectUrl() {
    if (lastObjectUrl) {
      URL.revokeObjectURL(lastObjectUrl);
      lastObjectUrl = null;
    }
  }

  function buildOverlayCanvas(img, boxes) {
    var w = img.offsetWidth;
    var h = img.offsetHeight;
    var nw = img.naturalWidth;
    var nh = img.naturalHeight;
    var canvas = document.createElement('canvas');
    canvas.width = w;
    canvas.height = h;
    canvas.style.cssText = 'position:absolute;left:0;top:0;width:' + w + 'px;height:' + h + 'px;pointer-events:none;box-sizing:border-box;';
    var ctx = canvas.getContext('2d');
    boxes.forEach(function (box) {
      drawBox(ctx, box, nw, nh, w, h);
    });
    return canvas;
  }

  function drawBox(ctx, box, natW, natH, dispW, dispH) {
    var x = (box.x / natW) * dispW;
    var y = (box.y / natH) * dispH;
    var w = (box.width / natW) * dispW;
    var h = (box.height / natH) * dispH;
    var color = getColorForClass(box.class || box.classLabel);
    var labelRu = getClassLabelRu(box.class || box.classLabel);
    var pct = box.confidence != null ? Math.round(box.confidence * 100) + '%' : '';
    var label = labelRu + (pct ? ' ' + pct : '');

    ctx.setLineDash([]);
    ctx.lineWidth = LINE_WIDTH + 2;
    ctx.strokeStyle = 'rgba(0,0,0,0.7)';
    ctx.strokeRect(x, y, w, h);
    ctx.lineWidth = LINE_WIDTH;
    ctx.strokeStyle = color;
    ctx.strokeRect(x, y, w, h);

    ctx.font = LABEL_FONT;
    var textY = Math.max(28, y - 12);
    var tw = ctx.measureText(label).width + 16;
    var th = 28;
    var tx = Math.max(0, x);
    var ty = Math.max(th, textY - 18);
    ctx.fillStyle = 'rgba(0,0,0,0.85)';
    ctx.fillRect(tx, ty, tw, th);
    ctx.strokeStyle = color;
    ctx.lineWidth = 2;
    ctx.strokeRect(tx, ty, tw, th);
    ctx.fillStyle = color;
    ctx.fillText(label, tx + 8, ty + 20);
  }

  function revokePreviewUrl() {
    revokeLastObjectUrl();
  }

  window.UploadPreview = {
    drawDetectionBoxes: drawDetectionBoxes,
    revokePreviewUrl: revokePreviewUrl,
    normalizeDetectionClass: normalizeDetectionClass,
    getClassLabelRu: getClassLabelRu
  };
})(window);
