/**
 * Страница «Демо-анализ» (demo.html).
 * Выбор файла (drag-n-drop), отправка POST /api/demo/analyze, отображение meanY, var, isBlack и сырого JSON.
 */
(function () {
  'use strict';

  var demoFile = null;

  function setupDropZone() {
    var dropEl = document.getElementById('demo-drop');
    var fileInput = document.getElementById('demo-file');
    var analyzeBtn = document.getElementById('demo-analyze-btn');
    var previewEl = document.getElementById('demo-preview');

    dropEl.addEventListener('click', function () { fileInput.click(); });
    fileInput.addEventListener('change', function () {
      demoFile = this.files[0];
      analyzeBtn.disabled = !demoFile;
      if (demoFile) {
        previewEl.innerHTML = '<img src="' + URL.createObjectURL(demoFile) + '" alt="Preview">';
      } else {
        previewEl.innerHTML = 'Выберите изображение';
      }
    });
    dropEl.addEventListener('dragover', function (e) { e.preventDefault(); dropEl.classList.add('dragover'); });
    dropEl.addEventListener('dragleave', function () { dropEl.classList.remove('dragover'); });
    dropEl.addEventListener('drop', function (e) {
      e.preventDefault();
      dropEl.classList.remove('dragover');
      var f = e.dataTransfer && e.dataTransfer.files[0];
      if (f) {
        fileInput.files = e.dataTransfer.files;
        demoFile = f;
        analyzeBtn.disabled = false;
        previewEl.innerHTML = '<img src="' + URL.createObjectURL(f) + '" alt="Preview">';
      }
    });
  }

  function showResult(data) {
    document.getElementById('demo-result-empty').style.display = 'none';
    document.getElementById('demo-result-content').style.display = 'block';

    var metricsHtml = '';
    if (data.meanY != null) {
      metricsHtml += '<div class="metric"><div class="metric-label">Яркость (meanY)</div><div class="metric-value">' + data.meanY.toFixed(2) + '</div><div class="metric-bar"><div class="metric-bar-fill" style="width:' + Math.min(100, data.meanY) + '%"></div></div></div>';
    }
    if (data.var != null) {
      metricsHtml += '<div class="metric"><div class="metric-label">Дисперсия (var)</div><div class="metric-value">' + data.var.toFixed(2) + '</div><div class="metric-bar"><div class="metric-bar-fill" style="width:' + Math.min(100, (data.var || 0) * 2) + '%"></div></div></div>';
    }
    document.getElementById('demo-metrics').innerHTML = metricsHtml;

    var flagsHtml = '';
    if (data.isBlack != null) {
      var css = data.isBlack ? 'badge-defect' : 'badge-ok';
      flagsHtml += '<span class="badge ' + css + '">Чёрный экран: ' + (data.isBlack ? 'да' : 'нет') + '</span>';
    }
    if (data.error) {
      flagsHtml += '<span class="badge badge-defect">' + data.error + '</span>';
    }
    document.getElementById('demo-flags').innerHTML = flagsHtml;
    document.getElementById('demo-raw').textContent = JSON.stringify(data, null, 2);
  }

  document.getElementById('demo-analyze-btn').addEventListener('click', function () {
    if (!demoFile) return;
    var btn = this;
    btn.disabled = true;
    var fd = new FormData();
    fd.append('file', demoFile);
    fetch('/api/demo/analyze', { method: 'POST', body: fd })
      .then(function (r) { return r.json(); })
      .then(showResult)
      .catch(function (err) {
        showResult({ error: err.message });
      })
      .finally(function () { btn.disabled = false; });
  });

  setupDropZone();
})();
