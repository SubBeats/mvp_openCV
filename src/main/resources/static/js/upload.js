/**
 * Страница загрузки кадра: форма, отправка на /upload, отображение результата и превью с боксами.
 * Классы детекции (в т.ч. glitch/glitches) унифицированы через UploadPreview.normalizeDetectionClass.
 */
(function () {
  'use strict';

  var lastUploadedFile = null;
  var lastUploadedBoxes = null;

  var THRESHOLD_IDS = ['threshold-screen', 'threshold-glitches', 'threshold-dead-pixels'];
  var DEFAULT_THRESHOLDS_PERCENT = { screen: 80, glitches: 58, 'dead-pixels-block': 53 };

  var ANALYSIS_LABELS_RU = {
    freeze: 'Заморозка',
    yoloGlitches: 'Глитчи (YOLO)',
    dim: 'Тускло',
    yoloDeadPixelsBlock: 'Битые блоки (YOLO)',
    yoloScreen: 'Экран (YOLO)'
  };

  function getEl(id) { return document.getElementById(id); }

  function loadScreens() {
    fetch('/api/screens')
      .then(function (r) { return r.json(); })
      .then(function (list) {
        var sel = getEl('upload-screen');
        if (!sel) return;
        sel.innerHTML = '';
        if (!list || list.length === 0) {
          sel.innerHTML = '<option value="">— Нет экранов (POST /api/screens)</option>';
          return;
        }
        list.forEach(function (s) {
          var opt = document.createElement('option');
          opt.value = s.id;
          opt.textContent = (s.name || 'Экран ' + s.id) + ' (id: ' + s.id + ')';
          sel.appendChild(opt);
        });
      });
  }

  function setupDropZone() {
    var drop = getEl('upload-drop');
    var input = getEl('upload-file');
    var text = getEl('upload-drop-text');
    if (!drop || !input) return;

    drop.addEventListener('click', function () { input.click(); });
    input.addEventListener('change', function () {
      var file = this.files[0];
      if (text) text.textContent = file ? file.name : 'Перетащите файл сюда или нажмите для выбора';
    });
    drop.addEventListener('dragover', function (e) { e.preventDefault(); drop.classList.add('dragover'); });
    drop.addEventListener('dragleave', function () { drop.classList.remove('dragover'); });
    drop.addEventListener('drop', function (e) {
      e.preventDefault();
      drop.classList.remove('dragover');
      var f = e.dataTransfer && e.dataTransfer.files[0];
      if (f) {
        input.files = e.dataTransfer.files;
        if (text) text.textContent = f.name;
      }
    });
  }

  function setLoading(visible) {
    var el = getEl('upload-loading');
    var btn = getEl('upload-btn');
    if (el) el.style.display = visible ? 'flex' : 'none';
    if (btn) btn.disabled = !!visible;
  }

  function showError(message) {
    var el = getEl('upload-error');
    if (el) { el.textContent = message; el.style.display = 'block'; }
  }

  function hideError() {
    var el = getEl('upload-error');
    if (el) el.style.display = 'none';
  }

  function getThresholdValues() {
    var screen = getEl('threshold-screen');
    var glitches = getEl('threshold-glitches');
    var dead = getEl('threshold-dead-pixels');
    return {
      screen: screen ? parseInt(screen.value, 10) / 100 : 0.58,
      glitches: glitches ? parseInt(glitches.value, 10) / 100 : 0.58,
      'dead-pixels-block': dead ? parseInt(dead.value, 10) / 100 : 0.58
    };
  }

  function normalizeClass(apiClass) {
    var fn = window.UploadPreview && window.UploadPreview.normalizeDetectionClass;
    return fn ? fn(apiClass || '') : (apiClass || '').toLowerCase();
  }

  function filterBoxesByConfidence(boxes, thresholds) {
    if (!boxes || !thresholds) return boxes || [];
    return boxes.filter(function (box) {
      var key = normalizeClass(box.class || box.classLabel);
      var minConf = thresholds[key];
      if (minConf == null) return true;
      var conf = box.confidence != null ? Number(box.confidence) : 0;
      return conf >= minConf;
    });
  }

  function redrawPreview() {
    if (!lastUploadedFile || !lastUploadedBoxes || !window.UploadPreview) return;
    var container = getEl('upload-preview');
    if (!container) return;
    var thresholds = getThresholdValues();
    var filtered = filterBoxesByConfidence(lastUploadedBoxes, thresholds);
    container.innerHTML = '';
    window.UploadPreview.drawDetectionBoxes(container, lastUploadedFile, filtered);
  }

  function resetThresholdSliders() {
    var screen = getEl('threshold-screen');
    var screenVal = getEl('threshold-screen-value');
    var glitches = getEl('threshold-glitches');
    var glitchesVal = getEl('threshold-glitches-value');
    var dead = getEl('threshold-dead-pixels');
    var deadVal = getEl('threshold-dead-pixels-value');
    if (screen && screenVal) { screen.value = DEFAULT_THRESHOLDS_PERCENT.screen; screenVal.textContent = DEFAULT_THRESHOLDS_PERCENT.screen; }
    if (glitches && glitchesVal) { glitches.value = DEFAULT_THRESHOLDS_PERCENT.glitches; glitchesVal.textContent = DEFAULT_THRESHOLDS_PERCENT.glitches; }
    if (dead && deadVal) { dead.value = DEFAULT_THRESHOLDS_PERCENT['dead-pixels-block']; deadVal.textContent = DEFAULT_THRESHOLDS_PERCENT['dead-pixels-block']; }
    redrawPreview();
  }

  function clearResult() {
    var result = getEl('upload-result');
    var previewWrap = getEl('upload-preview-wrap');
    var preview = getEl('upload-preview');
    var thresholdsWrap = getEl('upload-thresholds-wrap');
    if (result) { result.style.display = 'none'; result.innerHTML = ''; }
    if (previewWrap) previewWrap.style.display = 'none';
    if (preview) preview.innerHTML = '';
    if (thresholdsWrap) thresholdsWrap.style.display = 'none';
    if (window.UploadPreview && window.UploadPreview.revokePreviewUrl) window.UploadPreview.revokePreviewUrl();
    lastUploadedFile = null;
    lastUploadedBoxes = null;
  }

  function escapeHtml(str) {
    var div = document.createElement('div');
    div.textContent = str;
    return div.innerHTML;
  }

  function renderResultPanel(data) {
    if (!data) return '';
    var getClassLabelRu = window.UploadPreview && window.UploadPreview.getClassLabelRu;
    var toLabel = getClassLabelRu ? function (c) { return getClassLabelRu(c || ''); } : function (c) { return c || ''; };

    var html = '<div class="result-ru">';
    html += '<h3 class="result-ru-title">Результат анализа</h3>';
    html += '<dl class="result-ru-dl">';
    if (data.id != null) html += '<dt>ID</dt><dd>' + escapeHtml(String(data.id)) + '</dd>';
    if (data.timestamp) html += '<dt>Время</dt><dd>' + escapeHtml(String(data.timestamp)) + '</dd>';
    if (data.savedPath) html += '<dt>Путь к файлу</dt><dd>' + escapeHtml(data.savedPath) + '</dd>';
    html += '</dl>';
    html += '<h4 class="result-ru-subtitle">Анализ</h4>';
    if (data.analysis && typeof data.analysis === 'object') {
      html += '<ul class="result-ru-list">';
      for (var key in data.analysis) {
        if (!Object.prototype.hasOwnProperty.call(data.analysis, key)) continue;
        var keyLower = (key + '').toLowerCase();
        if (keyLower === 'black') continue;
        var label = ANALYSIS_LABELS_RU[key] || key;
        var labelStr = (label + '').toLowerCase();
        if (labelStr.indexOf('черн') !== -1) continue;
        var val = data.analysis[key];
        html += '<li><strong>' + escapeHtml(label) + ':</strong> ' + (val ? 'да' : 'нет') + '</li>';
      }
      html += '</ul>';
    } else {
      html += '<p class="result-ru-muted">Данные анализа не получены</p>';
    }
    if (data.boxes && data.boxes.length > 0) {
      html += '<h4 class="result-ru-subtitle">Обнаруженные зоны</h4>';
      html += '<table class="result-ru-table"><thead><tr><th>Класс</th><th>Уверенность</th><th>x, y</th><th>ширина × высота</th></tr></thead><tbody>';
      data.boxes.forEach(function (box) {
        var labelRu = toLabel(box.class || box.classLabel);
        var conf = box.confidence != null ? Math.round(box.confidence * 100) + '%' : '—';
        html += '<tr><td>' + escapeHtml(labelRu) + '</td><td>' + escapeHtml(conf) + '</td><td>' + box.x + ', ' + box.y + '</td><td>' + box.width + ' × ' + box.height + '</td></tr>';
      });
      html += '</tbody></table>';
    }
    html += '</div>';
    return html;
  }

  function showSuccess(data, file) {
    var resultEl = getEl('upload-result');
    if (resultEl) {
      resultEl.style.display = 'block';
      resultEl.innerHTML = renderResultPanel(data);
    }

    var previewWrap = getEl('upload-preview-wrap');
    var thresholdsWrap = getEl('upload-thresholds-wrap');
    var boxes = (data && data.boxes) ? (data.boxes.slice ? data.boxes.slice() : data.boxes) : [];

    if (file && window.UploadPreview) {
      lastUploadedFile = file;
      lastUploadedBoxes = boxes;
      if (previewWrap) previewWrap.style.display = 'block';
      if (thresholdsWrap) thresholdsWrap.style.display = boxes.length > 0 ? 'block' : 'none';
      redrawPreview();
    } else {
      if (previewWrap) previewWrap.style.display = 'none';
      if (thresholdsWrap) thresholdsWrap.style.display = 'none';
      lastUploadedFile = null;
      lastUploadedBoxes = null;
    }
  }

  function setupThresholdSliders() {
    THRESHOLD_IDS.forEach(function (id) {
      var slider = getEl(id);
      var valueEl = getEl(id + '-value');
      if (!slider || !valueEl) return;
      function update() {
        valueEl.textContent = slider.value;
        redrawPreview();
      }
      slider.addEventListener('input', update);
    });
    var resetBtn = getEl('threshold-reset-btn');
    if (resetBtn) resetBtn.addEventListener('click', resetThresholdSliders);
  }

  function submitUpload(e) {
    e.preventDefault();
    var screenId = (getEl('upload-screen') && getEl('upload-screen').value) || '';
    var fileInput = getEl('upload-file');
    var file = fileInput && fileInput.files[0];
    if (!screenId || !file) {
      showError('Выберите экран и файл.');
      return;
    }

    hideError();
    clearResult();
    setLoading(true);

    var formData = new FormData();
    formData.append('file', file);
    formData.append('screen_id', screenId);

    fetch('/upload', { method: 'POST', body: formData })
      .then(function (r) {
        if (!r.ok) return r.json().then(function (d) { throw new Error(d.error || r.status); });
        return r.json();
      })
      .then(function (data) { showSuccess(data, file); })
      .catch(function (err) {
        showError('Ошибка: ' + (err && err.message ? err.message : 'неизвестная ошибка'));
      })
      .finally(function () { setLoading(false); });
  }

  var form = getEl('upload-form');
  if (form) form.addEventListener('submit', submitUpload);

  loadScreens();
  setupDropZone();
  setupThresholdSliders();
})();
