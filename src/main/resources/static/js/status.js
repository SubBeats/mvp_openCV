/**
 * Страница «Статус сети» (index.html).
 * Загружает GET /api/status и заполняет таблицу экранов с последним результатом анализа.
 */
(function () {
  'use strict';

  function formatStatusBadges(analysis) {
    if (!analysis) return '<span class="badge badge-none">нет данных</span>';
    var defects = [];
    if (analysis.black) defects.push({ text: 'чёрный', css: 'badge-defect' });
    if (analysis.freeze) defects.push({ text: 'фриз', css: 'badge-defect' });
    if (analysis.yoloGlitches) defects.push({ text: 'глитчи', css: 'badge-defect' });
    if (analysis.yoloDeadPixelsBlock) defects.push({ text: 'битые блоки', css: 'badge-defect' });
    if (analysis.dim) defects.push({ text: 'пониженная яркость', css: 'badge-defect' });
    if (defects.length === 0) return '<span class="badge badge-ok">OK</span>';
    return defects.map(function (d) { return '<span class="badge ' + d.css + '">' + d.text + '</span>'; }).join('');
  }

  function loadStatus() {
    fetch('/api/status')
      .then(function (r) { return r.json(); })
      .then(function (list) {
        var tbody = document.getElementById('status-tbody');
        var emptyEl = document.getElementById('status-empty');
        tbody.innerHTML = '';
        if (!list || list.length === 0) {
          emptyEl.style.display = 'block';
          return;
        }
        emptyEl.style.display = 'none';
        list.forEach(function (row) {
          var badges = formatStatusBadges(row.analysis);
          var tr = document.createElement('tr');
          tr.innerHTML = '<td>' + row.id + '</td><td>' + (row.name || '') + '</td><td>' + (row.lastTs || '—') + '</td><td>' + badges + '</td>';
          tbody.appendChild(tr);
        });
      })
      .catch(function () {
        var emptyEl = document.getElementById('status-empty');
        emptyEl.textContent = 'Ошибка загрузки. Проверьте, что сервер запущен.';
        emptyEl.style.display = 'block';
      });
  }

  loadStatus();
})();
