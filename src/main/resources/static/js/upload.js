(function () {
  'use strict';

  function loadScreens() {
    fetch('/api/screens')
      .then(function (r) { return r.json(); })
      .then(function (list) {
        var sel = document.getElementById('upload-screen');
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
    var dropEl = document.getElementById('upload-drop');
    var fileInput = document.getElementById('upload-file');
    var dropText = document.getElementById('upload-drop-text');
    if (!dropEl || !fileInput) return;

    dropEl.addEventListener('click', function () { fileInput.click(); });
    fileInput.addEventListener('change', function () {
      var file = this.files[0];
      if (dropText) dropText.textContent = file ? file.name : 'Перетащите файл сюда или нажмите для выбора';
    });
    dropEl.addEventListener('dragover', function (e) { e.preventDefault(); dropEl.classList.add('dragover'); });
    dropEl.addEventListener('dragleave', function () { dropEl.classList.remove('dragover'); });
    dropEl.addEventListener('drop', function (e) {
      e.preventDefault();
      dropEl.classList.remove('dragover');
      var f = e.dataTransfer && e.dataTransfer.files[0];
      if (f) {
        fileInput.files = e.dataTransfer.files;
        if (dropText) dropText.textContent = f.name;
      }
    });
  }

  var lastFile = null;
  var lastBoxes = null;

  var DEFAULT_THRESHOLDS_PERCENT = { screen: 80, glitches: 58, 'dead-pixels-block': 56 };

  function setThresholdSlidersToDefault() {
    var screenSlider = document.getElementById('threshold-screen');
    var screenVal = document.getElementById('threshold-screen-value');
    var glitchesSlider = document.getElementById('threshold-glitches');
    var glitchesVal = document.getElementById('threshold-glitches-value');
    var deadSlider = document.getElementById('threshold-dead-pixels');
    var deadVal = document.getElementById('threshold-dead-pixels-value');
    if (screenSlider && screenVal) { screenSlider.value = DEFAULT_THRESHOLDS_PERCENT.screen; screenVal.textContent = DEFAULT_THRESHOLDS_PERCENT.screen; }
    if (glitchesSlider && glitchesVal) { glitchesSlider.value = DEFAULT_THRESHOLDS_PERCENT.glitches; glitchesVal.textContent = DEFAULT_THRESHOLDS_PERCENT.glitches; }
    if (deadSlider && deadVal) { deadSlider.value = DEFAULT_THRESHOLDS_PERCENT['dead-pixels-block']; deadVal.textContent = DEFAULT_THRESHOLDS_PERCENT['dead-pixels-block']; }
    redrawPreview();
  }

  function setLoading(show) {
    var el = document.getElementById('upload-loading');
    var btn = document.getElementById('upload-btn');
    if (el) el.style.display = show ? 'flex' : 'none';
    if (btn) btn.disabled = !!show;
  }

  function clearResult() {
    var resultEl = document.getElementById('upload-result');
    var previewWrap = document.getElementById('upload-preview-wrap');
    var previewEl = document.getElementById('upload-preview');
    var thresholdsWrap = document.getElementById('upload-thresholds-wrap');
    if (resultEl) { resultEl.style.display = 'none'; resultEl.innerHTML = ''; }
    if (previewWrap) previewWrap.style.display = 'none';
    if (previewEl) previewEl.innerHTML = '';
    if (thresholdsWrap) thresholdsWrap.style.display = 'none';
    lastFile = null;
    lastBoxes = null;
  }

  function getThresholds() {
    var screen = document.getElementById('threshold-screen');
    var glitches = document.getElementById('threshold-glitches');
    var dead = document.getElementById('threshold-dead-pixels');
    return {
      screen: screen ? parseInt(screen.value, 10) / 100 : 0.58,
      glitches: glitches ? parseInt(glitches.value, 10) / 100 : 0.58,
      'dead-pixels-block': dead ? parseInt(dead.value, 10) / 100 : 0.58
    };
  }

  function classToKey(cls) {
    if (cls === 'screen') return 'screen';
    if (cls === 'glitch' || cls === 'glitches') return 'glitches';
    if (cls === 'dead-pixels-block') return 'dead-pixels-block';
    return cls;
  }

  function filterBoxesByThresholds(boxes, thresholds) {
    if (!boxes || !thresholds) return boxes || [];
    return boxes.filter(function (box) {
      var key = classToKey(box.class || box.classLabel || '');
      var threshold = thresholds[key];
      if (threshold == null) return true;
      var conf = box.confidence != null ? Number(box.confidence) : 0;
      return conf >= threshold;
    });
  }

  function redrawPreview() {
    if (!lastFile || !lastBoxes || !window.UploadPreview) return;
    var container = document.getElementById('upload-preview');
    if (!container) return;
    var thresholds = getThresholds();
    var filtered = filterBoxesByThresholds(lastBoxes, thresholds);
    container.innerHTML = '';
    window.UploadPreview.drawDetectionBoxes(container, lastFile, filtered);
  }

  function showError(message) {
    var el = document.getElementById('upload-error');
    if (el) { el.textContent = message; el.style.display = 'block'; }
  }

  function hideError() {
    var el = document.getElementById('upload-error');
    if (el) el.style.display = 'none';
  }

  function showSuccess(data, file) {
    var resultEl = document.getElementById('upload-result');
    if (resultEl) {
      resultEl.style.display = 'block';
      resultEl.innerHTML = '<pre>' + JSON.stringify(data, null, 2) + '</pre>';
    }

    var previewWrap = document.getElementById('upload-preview-wrap');
    var thresholdsWrap = document.getElementById('upload-thresholds-wrap');
    if (data.boxes && data.boxes.length > 0 && file && window.UploadPreview) {
      lastFile = file;
      lastBoxes = data.boxes.slice ? data.boxes.slice() : data.boxes;
      if (previewWrap) previewWrap.style.display = 'block';
      if (thresholdsWrap) thresholdsWrap.style.display = 'block';
      redrawPreview();
    } else {
      if (previewWrap) previewWrap.style.display = 'none';
      if (thresholdsWrap) thresholdsWrap.style.display = 'none';
      lastFile = null;
      lastBoxes = null;
    }
  }

  function setupThresholdSliders() {
    var ids = ['threshold-screen', 'threshold-glitches', 'threshold-dead-pixels'];
    ids.forEach(function (id) {
      var slider = document.getElementById(id);
      var valueEl = document.getElementById(id + '-value');
      if (!slider || !valueEl) return;
      function update() {
        valueEl.textContent = slider.value;
        redrawPreview();
      }
      slider.addEventListener('input', update);
    });
    var resetBtn = document.getElementById('threshold-reset-btn');
    if (resetBtn) resetBtn.addEventListener('click', setThresholdSlidersToDefault);
  }

  var form = document.getElementById('upload-form');
  if (form) {
    form.addEventListener('submit', function (e) {
      e.preventDefault();
      var screenId = document.getElementById('upload-screen') && document.getElementById('upload-screen').value;
      var fileInput = document.getElementById('upload-file');
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
        .then(function (data) {
          showSuccess(data, file);
        })
        .catch(function (err) {
          showError('Ошибка: ' + (err && err.message ? err.message : 'неизвестная ошибка'));
        })
        .finally(function () {
          setLoading(false);
        });
    });
  }

  loadScreens();
  setupDropZone();
  setupThresholdSliders();
})();
