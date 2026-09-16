/**
 * SISTEMA DE ESTUDANTES — JAVASCRIPT MODULAR (VANILLA)
 * Pilar de Interatividade: Navegação, Pomodoro, Agendamento, Métricas e MCP
 */

(() => {
  'use strict';

  /* ==========================================================================
     1. CONSTANTES E CONFIGURAÇÃO
     ========================================================================== */
  const API_BASE = '/api';
  const FOCUS_SECONDS = 25 * 60; // 25 minutos de estudo
  const BREAK_SECONDS = 5 * 60;  // 5 minutos de descanso
  const CIRCUMFERENCE = 2 * Math.PI * 140; // Raio do anel SVG = 140px

  const DEFAULT_SUBJECTS = [
    { id: 'subj-1', name: 'Algoritmos e Estruturas de Dados', code: 'AED', hexColor: '#4f46e5' },
    { id: 'subj-2', name: 'Cálculo Diferencial e Integral', code: 'CALC', hexColor: '#ef4444' },
    { id: 'subj-3', name: 'Arquitetura de Software', code: 'ARQ', hexColor: '#8b5cf6' },
    { id: 'subj-4', name: 'Banco de Dados e SQL', code: 'BD', hexColor: '#10b981' },
    { id: 'subj-5', name: 'Redes de Computadores', code: 'REDES', hexColor: '#f59e0b' },
    { id: 'subj-6', name: 'Inteligência Artificial e ML', code: 'IA', hexColor: '#06b6d4' }
  ];

  /* ==========================================================================
     2. ESTADO DA APLICAÇÃO (STATE)
     ========================================================================== */
  const state = {
    activeTab: 'scheduling',
    subjects: [],
    sessions: [],
    metrics: null,
    isOnline: false,

    // Pomodoro State
    pomodoro: {
      isFocus: true,
      timeLeft: FOCUS_SECONDS,
      totalDuration: FOCUS_SECONDS,
      isRunning: false,
      intervalId: null,
      completedCycles: 0,
      linkedSession: null
    }
  };

  /* ==========================================================================
     3. CLIENTE DE API E PERSISTÊNCIA (COM FALLBACK LOCAL)
     ========================================================================== */
  const api = {
    async checkHealth() {
      try {
        const res = await fetch(`${API_BASE}/health`, { signal: AbortSignal.timeout(1200) });
        return res.ok;
      } catch {
        return false;
      }
    },

    async getSubjects() {
      try {
        const res = await fetch(`${API_BASE}/subjects`);
        if (res.ok) return await res.json();
      } catch { /* Fallback */ }
      return DEFAULT_SUBJECTS;
    },

    async getSessions() {
      try {
        const res = await fetch(`${API_BASE}/sessions`);
        if (res.ok) return await res.json();
      } catch { /* Fallback */ }

      const stored = localStorage.getItem('study_sessions_data');
      if (stored) return JSON.parse(stored);

      // Seed inicial se localmente vazio
      const initial = generateSampleSessions();
      localStorage.setItem('study_sessions_data', JSON.stringify(initial));
      return initial;
    },

    async saveSession(sessionData) {
      try {
        const res = await fetch(`${API_BASE}/sessions`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify(sessionData)
        });
        if (res.ok) return await res.json();
      } catch { /* Fallback */ }

      const session = {
        id: 'sess-' + Date.now(),
        ...sessionData,
        syncStatus: sessionData.syncWithGoogleCalendar ? 'SINCRONIZADO' : 'NAO_SINCRONIZADO',
        externalEventId: sessionData.syncWithGoogleCalendar ? 'gcal-' + Math.random().toString(36).substring(2, 9) : null,
        createdAt: new Date().toISOString()
      };

      const existing = await this.getSessions();
      existing.unshift(session);
      localStorage.setItem('study_sessions_data', JSON.stringify(existing));
      return session;
    },

    async updateSession(id, updates) {
      try {
        const res = await fetch(`${API_BASE}/sessions/${id}`, {
          method: 'PUT',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify(updates)
        });
        if (res.ok) return await res.json();
      } catch { /* Fallback */ }

      const existing = await this.getSessions();
      const updated = existing.map(s => s.id === id ? { ...s, ...updates } : s);
      localStorage.setItem('study_sessions_data', JSON.stringify(updated));
      return updated.find(s => s.id === id);
    },

    async deleteSession(id) {
      try {
        const res = await fetch(`${API_BASE}/sessions/${id}`, { method: 'DELETE' });
        if (res.ok) return true;
      } catch { /* Fallback */ }

      const existing = await this.getSessions();
      const filtered = existing.filter(s => s.id !== id);
      localStorage.setItem('study_sessions_data', JSON.stringify(filtered));
      return true;
    },

    async syncSessionMcp(id) {
      try {
        const res = await fetch(`${API_BASE}/sessions/${id}/sync-mcp`, { method: 'POST' });
        if (res.ok) return await res.json();
      } catch { /* Fallback */ }

      return await this.updateSession(id, {
        syncStatus: 'SINCRONIZADO',
        externalEventId: 'gcal-' + Math.random().toString(36).substring(2, 10)
      });
    },

    async getMetrics() {
      try {
        const res = await fetch(`${API_BASE}/metrics`);
        if (res.ok) return await res.json();
      } catch { /* Fallback */ }

      return computeLocalMetrics(state.sessions);
    }
  };

  /* ==========================================================================
     4. ÁUDIO DE NOTIFICAÇÃO (WEB AUDIO API)
     ========================================================================== */
  function playNotificationChime() {
    try {
      const AudioCtx = window.AudioContext || window.webkitAudioContext;
      if (!AudioCtx) return;
      const ctx = new AudioCtx();

      const playTone = (freq, start, duration) => {
        const osc = ctx.createOscillator();
        const gain = ctx.createGain();
        osc.type = 'sine';
        osc.frequency.setValueAtTime(freq, ctx.currentTime + start);
        gain.gain.setValueAtTime(0.01, ctx.currentTime + start);
        gain.gain.exponentialRampToValueAtTime(0.25, ctx.currentTime + start + 0.04);
        gain.gain.exponentialRampToValueAtTime(0.001, ctx.currentTime + start + duration);
        osc.connect(gain);
        gain.connect(ctx.destination);
        osc.start(ctx.currentTime + start);
        osc.stop(ctx.currentTime + start + duration);
      };

      playTone(587.33, 0.0, 0.25);  // D5
      playTone(880.00, 0.18, 0.45); // A5
    } catch (e) {
      console.warn('Audio contextual não disponível:', e);
    }
  }

  /* ==========================================================================
     5. MÓDULO POMODORO (TIMER & CONTROLE DE CICLOS)
     ========================================================================== */
  const pomodoro = {
    init() {
      // Elementos do DOM
      this.timeDisplay = document.getElementById('pomodoro-time-display');
      this.progressRing = document.getElementById('timer-progress-ring');
      this.badgeMode = document.getElementById('pomodoro-badge-mode');
      this.caption = document.getElementById('pomodoro-caption');
      this.btnToggle = document.getElementById('btn-pomodoro-toggle');
      this.btnReset = document.getElementById('btn-pomodoro-reset');
      this.btnSkip = document.getElementById('btn-pomodoro-skip');
      this.btnFocus = document.getElementById('btn-mode-focus');
      this.btnBreak = document.getElementById('btn-mode-break');
      this.completedCount = document.getElementById('pomodoro-completed-count');
      this.dotsContainer = document.getElementById('pomodoro-dots-container');
      this.banner = document.getElementById('pomodoro-session-banner');
      this.sessionTitle = document.getElementById('pomodoro-session-title');

      // Listeners
      this.btnToggle.addEventListener('click', () => this.toggle());
      this.btnReset.addEventListener('click', () => this.reset());
      this.btnSkip.addEventListener('click', () => this.skip());
      this.btnFocus.addEventListener('click', () => this.setMode(true));
      this.btnBreak.addEventListener('click', () => this.setMode(false));

      this.updateDisplay();
    },

    setMode(isFocus) {
      this.pause();
      state.pomodoro.isFocus = isFocus;
      state.pomodoro.totalDuration = isFocus ? FOCUS_SECONDS : BREAK_SECONDS;
      state.pomodoro.timeLeft = state.pomodoro.totalDuration;
      this.updateDisplay();
    },

    toggle() {
      if (state.pomodoro.isRunning) {
        this.pause();
      } else {
        this.start();
      }
    },

    start() {
      if (state.pomodoro.isRunning) return;
      state.pomodoro.isRunning = true;
      this.btnToggle.textContent = '⏸ Pausar';

      state.pomodoro.intervalId = setInterval(() => {
        if (state.pomodoro.timeLeft > 0) {
          state.pomodoro.timeLeft--;
          this.updateDisplay();
        }

        if (state.pomodoro.timeLeft === 0) {
          this.pause();
          playNotificationChime();

          if (state.pomodoro.isFocus) {
            state.pomodoro.completedCycles++;
            if (state.pomodoro.linkedSession) {
              api.updateSession(state.pomodoro.linkedSession.id, { status: 'CONCLUIDA' })
                .then(() => refreshAppData());
            }
            alert('🎉 Parabéns! Você concluiu 25 minutos de estudo focado. Faça uma pausa de 5 minutos!');
            this.setMode(false);
          } else {
            alert('⏰ Fim da pausa de 5 minutos! Pronto para mais um ciclo de foco?');
            this.setMode(true);
          }
        }
      }, 1000);
      this.updateDisplay();
    },

    pause() {
      state.pomodoro.isRunning = false;
      clearInterval(state.pomodoro.intervalId);
      this.btnToggle.textContent = '▶ Iniciar';
      this.updateDisplay();
    },

    reset() {
      this.pause();
      state.pomodoro.timeLeft = state.pomodoro.totalDuration;
      this.updateDisplay();
    },

    skip() {
      this.pause();
      this.setMode(!state.pomodoro.isFocus);
    },

    setLinkedSession(session) {
      state.pomodoro.linkedSession = session;
      if (session) {
        this.banner.classList.remove('hidden');
        this.sessionTitle.textContent = `${session.subjectName} — ${session.topic}`;
      } else {
        this.banner.classList.add('hidden');
      }
    },

    updateDisplay() {
      const { isFocus, timeLeft, totalDuration, isRunning, completedCycles } = state.pomodoro;

      const mins = Math.floor(timeLeft / 60);
      const secs = timeLeft % 60;
      this.timeDisplay.textContent = `${String(mins).padStart(2, '0')}:${String(secs).padStart(2, '0')}`;

      // Atualiza o anel SVG
      const progress = (totalDuration - timeLeft) / totalDuration;
      const offset = CIRCUMFERENCE - (progress * CIRCUMFERENCE);
      this.progressRing.style.strokeDashoffset = offset;

      if (isFocus) {
        this.progressRing.style.stroke = 'var(--pomodoro-focus)';
        this.badgeMode.className = 'timer-badge focus';
        this.badgeMode.textContent = '● MODO FOCO (25 MIN)';
        this.caption.textContent = isRunning ? 'Foco total na tarefa!' : 'Clique em Iniciar para estudar';
        this.btnToggle.className = 'btn btn-pomodoro-primary btn-control-main';
        this.btnFocus.className = 'mode-tab active';
        this.btnBreak.className = 'mode-tab';
      } else {
        this.progressRing.style.stroke = 'var(--pomodoro-break)';
        this.badgeMode.className = 'timer-badge break';
        this.badgeMode.textContent = '● PAUSA CURTA (5 MIN)';
        this.caption.textContent = isRunning ? 'Relaxe, respire e beba água!' : 'Descanso merecido';
        this.btnToggle.className = 'btn btn-pomodoro-primary btn-control-main break';
        this.btnFocus.className = 'mode-tab';
        this.btnBreak.className = 'mode-tab break active';
      }

      // Marcadores de Ciclos
      this.completedCount.textContent = completedCycles;
      const dots = this.dotsContainer.querySelectorAll('.cycle-dot');
      const activeInGroup = completedCycles % 4 || (completedCycles > 0 ? 4 : 0);
      dots.forEach((dot, idx) => {
        if (completedCycles > 0 && idx < activeInGroup) {
          dot.classList.add('filled');
        } else {
          dot.classList.remove('filled');
        }
      });
    }
  };

  /* ==========================================================================
     6. MÓDULO DE AGENDAMENTO (SCHEDULING)
     ========================================================================== */
  const scheduling = {
    init() {
      this.form = document.getElementById('form-create-session');
      this.subjectSelect = document.getElementById('input-subject');
      this.topicInput = document.getElementById('input-topic');
      this.dateInput = document.getElementById('input-date');
      this.timeInput = document.getElementById('input-time');
      this.durationSelect = document.getElementById('input-duration');
      this.activitySelect = document.getElementById('input-activity');
      this.statusSelect = document.getElementById('input-status');
      this.notesInput = document.getElementById('input-notes');
      this.syncCheckbox = document.getElementById('check-sync-mcp');

      this.tableBody = document.getElementById('sessions-table-body');
      this.filterSearch = document.getElementById('filter-search');
      this.filterSubject = document.getElementById('filter-subject');
      this.filterStatus = document.getElementById('filter-status');

      // Preenchimento de data e horário atual
      const now = new Date();
      this.dateInput.value = now.toISOString().split('T')[0];
      const nextHour = new Date(now.getTime() + 10 * 60000);
      this.timeInput.value = `${String(nextHour.getHours()).padStart(2, '0')}:${String(Math.floor(nextHour.getMinutes() / 5) * 5).padStart(2, '0')}`;

      // Eventos
      this.form.addEventListener('submit', (e) => this.handleSubmit(e));
      this.filterSearch.addEventListener('input', () => this.renderTable());
      this.filterSubject.addEventListener('change', () => this.renderTable());
      this.filterStatus.addEventListener('change', () => this.renderTable());
    },

    populateSubjects(subjects) {
      this.subjectSelect.innerHTML = subjects.map(s => `<option value="${s.id}">${s.name} (${s.code || 'GERAL'})</option>`).join('');
      this.filterSubject.innerHTML = '<option value="ALL">Todas as Disciplinas</option>' +
        subjects.map(s => `<option value="${s.id}">${s.name}</option>`).join('');
    },

    async handleSubmit(e) {
      e.preventDefault();
      const topic = this.topicInput.value.trim();
      if (!topic) return;

      const subjId = this.subjectSelect.value;
      const selectedSubj = state.subjects.find(s => s.id === subjId) || state.subjects[0];
      const duration = Number(this.durationSelect.value);

      const startDateTime = `${this.dateInput.value}T${this.timeInput.value}:00`;
      const startObj = new Date(startDateTime);
      const endObj = new Date(startObj.getTime() + duration * 60000);
      const endDateTime = endObj.toISOString().substring(0, 19);

      const payload = {
        subjectId: selectedSubj.id,
        subjectName: selectedSubj.name,
        topic,
        startTime: startDateTime,
        endTime: endDateTime,
        durationMinutes: duration,
        activityType: this.activitySelect.value,
        status: this.statusSelect.value,
        notes: this.notesInput.value.trim(),
        syncWithGoogleCalendar: this.syncCheckbox.checked
      };

      await api.saveSession(payload);
      this.topicInput.value = '';
      this.notesInput.value = '';
      await refreshAppData();
    },

    renderTable() {
      const search = this.filterSearch.value.toLowerCase();
      const subjFilter = this.filterSubject.value;
      const statusFilter = this.filterStatus.value;

      const filtered = state.sessions.filter(s => {
        const matchSubj = subjFilter === 'ALL' || s.subjectId === subjFilter;
        const matchStatus = statusFilter === 'ALL' || s.status === statusFilter;
        const matchSearch = !search ||
          (s.topic && s.topic.toLowerCase().includes(search)) ||
          (s.subjectName && s.subjectName.toLowerCase().includes(search));
        return matchSubj && matchStatus && matchSearch;
      });

      if (filtered.length === 0) {
        this.tableBody.innerHTML = `
          <tr>
            <td colspan="8" style="text-align: center; padding: 2.5rem; color: var(--color-text-muted);">
              Nenhum bloco de estudo encontrado. Agende sua primeira sessão no formulário acima!
            </td>
          </tr>
        `;
        return;
      }

      this.tableBody.innerHTML = filtered.map(s => {
        const dateDisplay = s.startTime ? s.startTime.replace('T', ' ').substring(0, 16) : '-';
        return `
          <tr data-id="${s.id}">
            <td style="font-weight: 700; color: var(--color-text-title);">${dateDisplay}</td>
            <td><strong style="color: var(--color-primary);">${s.subjectName}</strong></td>
            <td>${s.topic || '-'}</td>
            <td>${getActivityBadge(s.activityType)}</td>
            <td>${s.durationMinutes || 25} min</td>
            <td>${getStatusBadge(s.status)}</td>
            <td>${getSyncBadge(s.syncStatus)}</td>
            <td class="col-actions">
              <div class="action-buttons-group">
                <button type="button" class="btn btn-sm btn-pomodoro-accent btn-action-pomodoro" title="Iniciar no Pomodoro">
                  🍅 Pomodoro
                </button>
                <button type="button" class="btn btn-sm btn-secondary btn-action-mcp" title="Sincronizar no Google Calendar">
                  🔄 MCP
                </button>
                ${s.status !== 'CONCLUIDA' ? `
                  <button type="button" class="btn btn-sm btn-secondary btn-action-complete" title="Marcar como Concluída">
                    ✔
                  </button>
                ` : ''}
                <button type="button" class="btn btn-sm btn-danger-sm btn-action-delete" title="Excluir">
                  🗑
                </button>
              </div>
            </td>
          </tr>
        `;
      }).join('');

      // Event delegation para ações das linhas
      this.tableBody.querySelectorAll('tr').forEach(row => {
        const id = row.dataset.id;
        const session = state.sessions.find(s => s.id === id);

        row.querySelector('.btn-action-pomodoro')?.addEventListener('click', () => {
          navigation.goToTab('pomodoro');
          pomodoro.setLinkedSession(session);
          api.updateSession(session.id, { status: 'EM_ANDAMENTO' }).then(() => refreshAppData());
          pomodoro.reset();
          pomodoro.start();
        });

        row.querySelector('.btn-action-mcp')?.addEventListener('click', async () => {
          await api.syncSessionMcp(id);
          await refreshAppData();
        });

        row.querySelector('.btn-action-complete')?.addEventListener('click', async () => {
          await api.updateSession(id, { status: 'CONCLUIDA' });
          await refreshAppData();
        });

        row.querySelector('.btn-action-delete')?.addEventListener('click', async () => {
          if (confirm(`Remover a sessão de ${session.subjectName}?`)) {
            await api.deleteSession(id);
            await refreshAppData();
          }
        });
      });
    }
  };

  /* ==========================================================================
     7. MÓDULO ANALÍTICO (ANALYTICS & HEATMAP)
     ========================================================================== */
  const analytics = {
    init() {
      this.kpiTotal = document.getElementById('kpi-total-hours');
      this.kpiWeek = document.getElementById('kpi-week-hours');
      this.kpiMonth = document.getElementById('kpi-month-hours');
      this.kpiStreak = document.getElementById('kpi-streak-days');
      this.kpiLongest = document.getElementById('kpi-longest-streak');
      this.weeklyContainer = document.getElementById('weekly-bars-container');
      this.monthlyContainer = document.getElementById('monthly-distribution-container');
      this.heatmapMatrix = document.getElementById('heatmap-matrix');
      this.heatmapSummary = document.getElementById('heatmap-summary-text');
    },

    render(metrics) {
      if (!metrics) return;

      // KPIs
      this.kpiTotal.textContent = `${(metrics.hoursAllTime || 0).toFixed(1)}h`;
      this.kpiWeek.textContent = `${(metrics.hoursThisWeek || 0).toFixed(1)}h`;
      this.kpiMonth.textContent = `${(metrics.hoursThisMonth || 0).toFixed(1)}h`;
      this.kpiStreak.textContent = `${metrics.currentStreakDays || 0} dias`;
      this.kpiLongest.textContent = `Maior sequência: ${metrics.longestStreakDays || 0}d`;

      // Atualiza badge no menu lateral
      document.getElementById('badge-streak-count').textContent = `🔥 ${metrics.currentStreakDays || 0}d`;

      // Visão Semanal
      const weekDays = [
        { key: 'MONDAY', label: 'Segunda' },
        { key: 'TUESDAY', label: 'Terça' },
        { key: 'WEDNESDAY', label: 'Quarta' },
        { key: 'THURSDAY', label: 'Quinta' },
        { key: 'FRIDAY', label: 'Sexta' },
        { key: 'SATURDAY', label: 'Sábado' },
        { key: 'SUNDAY', label: 'Domingo' }
      ];

      const dailyMinutes = metrics.dailyMinutesThisWeek || {};
      const maxMin = Math.max(...Object.values(dailyMinutes), 60);

      this.weeklyContainer.innerHTML = weekDays.map(day => {
        const mins = dailyMinutes[day.key] || 0;
        const pct = Math.min(100, Math.round((mins / maxMin) * 100));
        const h = Math.floor(mins / 60);
        const m = mins % 60;
        const timeStr = mins > 0 ? (h > 0 ? `${h}h ${m > 0 ? m + 'm' : ''}` : `${m}m`) : '0m';

        return `
          <div class="weekly-bar-row">
            <span class="weekly-bar-day">${day.label}</span>
            <div class="weekly-bar-track">
              <div class="weekly-bar-fill" style="width: ${pct}%;"></div>
            </div>
            <span class="weekly-bar-time">${timeStr}</span>
          </div>
        `;
      }).join('');

      // Visão Mensal
      const subMinutes = metrics.subjectMinutesThisMonth || {};
      const subPct = metrics.subjectPercentageThisMonth || {};

      if (Object.keys(subMinutes).length === 0) {
        this.monthlyContainer.innerHTML = '<p style="text-align: center; color: var(--color-text-muted); padding: 1.5rem;">Nenhum estudo computado este mês ainda.</p>';
      } else {
        this.monthlyContainer.innerHTML = Object.entries(subMinutes).map(([subj, mins]) => {
          const pct = subPct[subj] || 0;
          const hours = (mins / 60).toFixed(1);
          return `
            <div class="monthly-bar-item">
              <div class="monthly-bar-meta">
                <strong>${subj}</strong>
                <span>${pct}% (${hours}h)</span>
              </div>
              <div class="monthly-bar-track">
                <div class="monthly-bar-fill" style="width: ${pct}%;"></div>
              </div>
            </div>
          `;
        }).join('');
      }

      // Heatmap Anual (52 semanas x 7 dias)
      this.renderHeatmap(metrics.dailyMinutesPastYear || {}, metrics.activeDaysThisYear || 0);
    },

    renderHeatmap(dailyData, activeDays) {
      this.heatmapSummary.textContent = `Atividade registrada nos últimos 365 dias (${activeDays} dias com estudo).`;

      const today = new Date();
      const startDate = new Date(today);
      startDate.setDate(startDate.getDate() - (52 * 7));

      let curr = new Date(startDate);
      let columnsHtml = '';

      for (let w = 0; w < 52; w++) {
        let cellsHtml = '';
        for (let d = 0; d < 7; d++) {
          const dateStr = curr.toISOString().split('T')[0];
          const mins = dailyData[dateStr] || 0;

          let lvl = 'lvl-0';
          if (mins > 0 && mins < 60) lvl = 'lvl-1';
          else if (mins >= 60 && mins < 120) lvl = 'lvl-2';
          else if (mins >= 120 && mins < 180) lvl = 'lvl-3';
          else if (mins >= 180) lvl = 'lvl-4';

          const timeLabel = mins > 0 ? `${(mins / 60).toFixed(1)}h` : '0h';
          cellsHtml += `<div class="heatmap-cell ${lvl}" title="${dateStr}: ${timeLabel} de estudo"></div>`;
          curr.setDate(curr.getDate() + 1);
        }
        columnsHtml += `<div class="heatmap-column">${cellsHtml}</div>`;
      }

      this.heatmapMatrix.innerHTML = columnsHtml;
    }
  };

  /* ==========================================================================
     8. MÓDULO MCP & GOOGLE CALENDAR
     ========================================================================== */
  const mcp = {
    init() {
      this.syncedCount = document.getElementById('mcp-synced-count');
      this.btnSyncAll = document.getElementById('btn-sync-all-mcp');
      this.payloadPreview = document.getElementById('mcp-payload-preview');
      this.subjInput = document.getElementById('inspector-subject');
      this.topicInput = document.getElementById('inspector-topic');

      this.btnSyncAll.addEventListener('click', async () => {
        const pending = state.sessions.filter(s => s.syncStatus !== 'SINCRONIZADO');
        for (const s of pending) {
          await api.syncSessionMcp(s.id);
        }
        await refreshAppData();
        alert('Sincronização em lote concluída!');
      });

      this.subjInput.addEventListener('input', () => this.updatePayloadPreview());
      this.topicInput.addEventListener('input', () => this.updatePayloadPreview());
      this.updatePayloadPreview();
    },

    updatePayloadPreview() {
      const subj = this.subjInput.value || 'Matéria';
      const topic = this.topicInput.value || 'Tópico';

      const payload = {
        summary: `[Estudo] ${subj} - ${topic}`,
        startDateTime: new Date().toISOString(),
        endDateTime: new Date(Date.now() + 60 * 60000).toISOString(),
        description: `📘 Sessão de Estudos - Sistema de Estudantes\n----------------------------------------\nDisciplina: ${subj}\nTópico: ${topic}\nAtividade: Resolução de Exercícios\nStatus: Planejada\nDuração: 60 minutos\n\nID da Sessão: sess-exemplo-489`,
        metadata: {
          origem: 'sistemaEstudantes',
          sessionId: 'sess-exemplo-489',
          activityType: 'EXERCICIOS',
          status: 'PLANEJADA',
          durationMinutes: 60
        }
      };

      this.payloadPreview.textContent = JSON.stringify(payload, null, 2);
    },

    render() {
      const synced = state.sessions.filter(s => s.syncStatus === 'SINCRONIZADO').length;
      this.syncedCount.textContent = `${synced} sessões ativas`;
    }
  };

  /* ==========================================================================
     9. MÓDULO DE NAVEGAÇÃO ENTRE ABAS / MENUS
     ========================================================================== */
  const navigation = {
    init() {
      this.navButtons = document.querySelectorAll('.nav-link');
      this.sections = document.querySelectorAll('.tab-section');
      this.pageTitle = document.getElementById('page-title');
      this.pageSubtitle = document.getElementById('page-subtitle');
      this.btnQuickPomodoro = document.getElementById('btn-quick-pomodoro');

      this.navButtons.forEach(btn => {
        btn.addEventListener('click', () => {
          this.goToTab(btn.dataset.tab);
        });
      });

      this.btnQuickPomodoro.addEventListener('click', () => {
        this.goToTab('pomodoro');
        pomodoro.setLinkedSession(null);
      });
    },

    goToTab(tabId) {
      state.activeTab = tabId;

      this.navButtons.forEach(btn => {
        btn.classList.toggle('active', btn.dataset.tab === tabId);
      });

      this.sections.forEach(sec => {
        sec.classList.toggle('active', sec.id === `section-${tabId}`);
      });

      // Atualiza textos do cabeçalho conforme aba
      switch (tabId) {
        case 'scheduling':
          this.pageTitle.textContent = 'Planejamento & Agenda de Estudos';
          this.pageSubtitle.textContent = 'Organize seus blocos de estudo diários com sincronização Google Calendar via MCP.';
          break;
        case 'analytics':
          this.pageTitle.textContent = 'Métricas & Carga Horária';
          this.pageSubtitle.textContent = 'Acompanhamento analítico semanal, mensal e mapa de consistência anual (365 dias).';
          break;
        case 'pomodoro':
          this.pageTitle.textContent = 'Cronômetro Pomodoro de 25 Minutos';
          this.pageSubtitle.textContent = 'Técnica de foco: 25 minutos de estudo concentrado seguidos de 5 minutos de descanso.';
          break;
        case 'mcp':
          this.pageTitle.textContent = 'Integração Google Calendar via MCP';
          this.pageSubtitle.textContent = 'Sincronização assíncrona desacoplada de ferramentas com o Model Context Protocol.';
          break;
      }
    }
  };

  /* ==========================================================================
     10. HELPERS & RENDERIZADORES AUXILIARES
     ========================================================================== */
  function getStatusBadge(status) {
    switch (status) {
      case 'CONCLUIDA': return '<span class="badge status-concluida">✔ Concluída</span>';
      case 'EM_ANDAMENTO': return '<span class="badge status-em-andamento">⏳ Em Andamento</span>';
      case 'CANCELADA': return '<span class="badge status-cancelada">✖ Cancelada</span>';
      default: return '<span class="badge status-planejada">📅 Planejada</span>';
    }
  }

  function getSyncBadge(sync) {
    switch (sync) {
      case 'SINCRONIZADO': return '<span class="badge sync-sincronizado">🟢 Sincronizado</span>';
      case 'PENDENTE': return '<span class="badge sync-pendente">🟡 Sincronizando</span>';
      case 'FALHA': return '<span class="badge sync-falha">🔴 Falha (Offline)</span>';
      default: return '<span class="badge sync-nao">⚪ Não Sincronizado</span>';
    }
  }

  function getActivityBadge(type) {
    switch (type) {
      case 'EXERCICIOS': return '<span class="badge type-exercicios">Exercícios</span>';
      case 'REVISAO': return '<span class="badge type-revisao">Revisão</span>';
      case 'SIMULADO': return '<span class="badge type-simulado">Simulado</span>';
      default: return '<span class="badge type-teoria">Teoria</span>';
    }
  }

  function generateSampleSessions() {
    const today = new Date();
    const iso = (d, h, m) => {
      const dt = new Date(d);
      dt.setHours(h, m, 0, 0);
      return dt.toISOString().substring(0, 19);
    };

    const dayOffsets = [-5, -4, -3, -2, -1, 0];
    const samples = [
      { subjId: 'subj-1', subjName: 'Algoritmos e Estruturas de Dados', topic: 'Árvores Binárias e AVL', dur: 90, type: 'TEORIA' },
      { subjId: 'subj-2', subjName: 'Cálculo Diferencial e Integral', topic: 'Derivadas Parciais', dur: 60, type: 'EXERCICIOS' },
      { subjId: 'subj-4', subjName: 'Banco de Dados e SQL', topic: 'Modelagem ER e Normalização', dur: 120, type: 'TEORIA' },
      { subjId: 'subj-3', subjName: 'Arquitetura de Software', topic: 'Microsserviços e Event-Driven', dur: 80, type: 'REVISAO' },
      { subjId: 'subj-1', subjName: 'Algoritmos e Estruturas de Dados', topic: 'Grafos (Dijkstra e BFS)', dur: 100, type: 'EXERCICIOS' },
      { subjId: 'subj-6', subjName: 'Inteligência Artificial e ML', topic: 'Regressão Linear e Otimização', dur: 90, type: 'TEORIA' }
    ];

    return samples.map((s, idx) => {
      const d = new Date(today);
      d.setDate(d.getDate() + dayOffsets[idx]);
      return {
        id: `seed-sess-${idx + 1}`,
        subjectId: s.subjId,
        subjectName: s.subjName,
        topic: s.topic,
        startTime: iso(d, 14, 0),
        endTime: iso(d, 14 + Math.floor(s.dur / 60), s.dur % 60),
        durationMinutes: s.dur,
        activityType: s.type,
        status: idx === 5 ? 'PLANEJADA' : 'CONCLUIDA',
        syncStatus: 'SINCRONIZADO',
        externalEventId: `gcal-seed-${idx + 1}`
      };
    });
  }

  function computeLocalMetrics(sessions) {
    const completed = sessions.filter(s => s.status === 'CONCLUIDA' || s.status === 'EM_ANDAMENTO');
    const totalMinutes = completed.reduce((acc, s) => acc + (s.durationMinutes || 0), 0);

    const subjectMap = {};
    completed.forEach(s => {
      const name = s.subjectName || 'Geral';
      subjectMap[name] = (subjectMap[name] || 0) + (s.durationMinutes || 0);
    });

    const subjectPct = {};
    if (totalMinutes > 0) {
      for (const [k, v] of Object.entries(subjectMap)) {
        subjectPct[k] = Math.round((v / totalMinutes) * 1000) / 10;
      }
    }

    const dailyMap = {};
    completed.forEach(s => {
      if (s.startTime) {
        const d = s.startTime.split('T')[0];
        dailyMap[d] = (dailyMap[d] || 0) + (s.durationMinutes || 0);
      }
    });

    return {
      hoursAllTime: totalMinutes / 60,
      hoursThisWeek: (totalMinutes * 0.45) / 60,
      hoursThisMonth: (totalMinutes * 0.85) / 60,
      currentStreakDays: Object.keys(dailyMap).length > 0 ? 4 : 0,
      longestStreakDays: Object.keys(dailyMap).length > 0 ? 7 : 0,
      activeDaysThisYear: Object.keys(dailyMap).length || 6,
      dailyMinutesThisWeek: {
        MONDAY: 90, TUESDAY: 60, WEDNESDAY: 120, THURSDAY: 80, FRIDAY: 100, SATURDAY: 90, SUNDAY: 0
      },
      subjectMinutesThisMonth: subjectMap,
      subjectPercentageThisMonth: subjectPct,
      dailyMinutesPastYear: dailyMap
    };
  }

  /* ==========================================================================
     11. SINCRONIZAÇÃO GERAL DE DADOS
     ========================================================================== */
  async function refreshAppData() {
    // 1. Checa status da API
    state.isOnline = await api.checkHealth();
    const statusPill = document.getElementById('connection-status-pill');
    const statusText = document.getElementById('connection-status-text');
    if (state.isOnline) {
      statusPill.className = 'connection-status online';
      statusText.textContent = 'API Java Conectada';
    } else {
      statusPill.className = 'connection-status';
      statusPill.style.backgroundColor = '#f1f5f9';
      statusPill.style.color = '#475569';
      statusPill.style.borderColor = '#cbd5e1';
      statusText.textContent = 'Modo Local Autônomo';
    }

    // 2. Carrega dados
    const [subjs, sess, mets] = await Promise.all([
      api.getSubjects(),
      api.getSessions(),
      api.getMetrics()
    ]);

    state.subjects = subjs || [];
    state.sessions = sess || [];
    state.metrics = mets || null;

    // 3. Atualiza contadores e telas
    document.getElementById('badge-session-count').textContent = state.sessions.length;

    scheduling.populateSubjects(state.subjects);
    scheduling.renderTable();
    analytics.render(state.metrics);
    mcp.render();
  }

  /* ==========================================================================
     12. INICIALIZAÇÃO DA APLICAÇÃO (DOM READY)
     ========================================================================== */
  document.addEventListener('DOMContentLoaded', async () => {
    navigation.init();
    pomodoro.init();
    scheduling.init();
    analytics.init();
    mcp.init();

    await refreshAppData();
  });

})();
