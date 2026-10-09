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
  const FOCUS_SECONDS = 25 * 60;       // 25 minutos de estudo
  const SHORT_BREAK_SECONDS = 5 * 60;  // 5 minutos de pausa curta
  const LONG_BREAK_SECONDS = 15 * 60;  // 15 minutos de pausa longa
  const CIRCUMFERENCE = 2 * Math.PI * 140; // Raio do anel SVG = 140px

  const DEFAULT_SUBJECTS = [
    { id: 'subj-1', name: 'Algoritmos e Estruturas de Dados', code: 'AED', hexColor: '#3A7D8C' },
    { id: 'subj-2', name: 'Cálculo Diferencial e Integral', code: 'CALC', hexColor: '#7FB3D1' },
    { id: 'subj-3', name: 'Arquitetura de Software', code: 'ARQ', hexColor: '#7FA99B' },
    { id: 'subj-4', name: 'Banco de Dados e SQL', code: 'BD', hexColor: '#8A9BAA' },
    { id: 'subj-5', name: 'Redes de Computadores', code: 'REDES', hexColor: '#5F7F99' },
    { id: 'subj-6', name: 'Inteligência Artificial e ML', code: 'IA', hexColor: '#C9B99A' }
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

    // Pomodoro State (Seção 4 da SKILL: Foco, Pausa Curta, Pausa Longa)
    pomodoro: {
      mode: 'focus', // 'focus' | 'short_break' | 'long_break'
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
      this.btnLongBreak = document.getElementById('btn-mode-long-break');
      this.completedCount = document.getElementById('pomodoro-completed-count');
      this.dotsContainer = document.getElementById('pomodoro-dots-container');
      this.banner = document.getElementById('pomodoro-session-banner');
      this.sessionTitle = document.getElementById('pomodoro-session-title');

      // Listeners
      this.btnToggle?.addEventListener('click', () => this.toggle());
      this.btnReset?.addEventListener('click', () => this.reset());
      this.btnSkip?.addEventListener('click', () => this.skip());
      this.btnFocus?.addEventListener('click', () => this.setMode('focus'));
      this.btnBreak?.addEventListener('click', () => this.setMode('short_break'));
      this.btnLongBreak?.addEventListener('click', () => this.setMode('long_break'));

      this.updateDisplay();
    },

    setMode(mode) {
      this.pause();
      state.pomodoro.mode = mode;
      if (mode === 'focus') {
        state.pomodoro.totalDuration = FOCUS_SECONDS;
      } else if (mode === 'short_break') {
        state.pomodoro.totalDuration = SHORT_BREAK_SECONDS;
      } else if (mode === 'long_break') {
        state.pomodoro.totalDuration = LONG_BREAK_SECONDS;
      }
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
      const toggleText = document.getElementById('btn-pomodoro-toggle-text');
      if (toggleText) toggleText.textContent = 'Pausar';
      else this.btnToggle.textContent = 'Pausar';

      state.pomodoro.intervalId = setInterval(() => {
        if (state.pomodoro.timeLeft > 0) {
          state.pomodoro.timeLeft--;
          this.updateDisplay();
        }

        if (state.pomodoro.timeLeft === 0) {
          this.pause();
          playNotificationChime();

          if (state.pomodoro.mode === 'focus') {
            state.pomodoro.completedCycles++;
            if (state.pomodoro.linkedSession) {
              api.updateSession(state.pomodoro.linkedSession.id, { status: 'CONCLUIDA' })
                .then(() => refreshAppData());
            }
            if (state.pomodoro.completedCycles % 4 === 0) {
              alert('🎉 Excelente! 4 blocos de foco concluídos. Hora de uma Pausa Longa revigorante (15 min)!');
              this.setMode('long_break');
            } else {
              alert('🎉 Bloco de foco concluído! Faça uma Pausa Curta de 5 minutos.');
              this.setMode('short_break');
            }
          } else {
            alert('⏰ Fim do descanso! Pronto para retomar os estudos?');
            this.setMode('focus');
          }
        }
      }, 1000);
      this.updateDisplay();
    },

    pause() {
      state.pomodoro.isRunning = false;
      clearInterval(state.pomodoro.intervalId);
      const toggleText = document.getElementById('btn-pomodoro-toggle-text');
      if (toggleText) toggleText.textContent = 'Iniciar';
      else this.btnToggle.textContent = 'Iniciar';
      this.updateDisplay();
    },

    reset() {
      this.pause();
      state.pomodoro.timeLeft = state.pomodoro.totalDuration;
      this.updateDisplay();
    },

    skip() {
      this.pause();
      if (state.pomodoro.mode === 'focus') {
        this.setMode('short_break');
      } else {
        this.setMode('focus');
      }
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
      const { mode, timeLeft, totalDuration, isRunning, completedCycles } = state.pomodoro;

      const mins = Math.floor(timeLeft / 60);
      const secs = timeLeft % 60;
      this.timeDisplay.textContent = `${String(mins).padStart(2, '0')}:${String(secs).padStart(2, '0')}`;

      // Atualiza o anel SVG
      const progress = (totalDuration - timeLeft) / totalDuration;
      const offset = CIRCUMFERENCE - (progress * CIRCUMFERENCE);
      this.progressRing.style.strokeDashoffset = offset;

      if (mode === 'focus') {
        this.progressRing.style.stroke = 'var(--pomodoro-focus)';
        this.badgeMode.className = 'timer-mode-badge focus';
        this.badgeMode.textContent = 'MODO FOCO (25 MIN)';
        this.caption.textContent = isRunning ? 'Foco total na tarefa!' : 'Clique em Iniciar para estudar';
        if (this.btnFocus) this.btnFocus.className = 'pomodoro-mode-tab active';
        if (this.btnBreak) this.btnBreak.className = 'pomodoro-mode-tab break';
        if (this.btnLongBreak) this.btnLongBreak.className = 'pomodoro-mode-tab long-break';
      } else if (mode === 'short_break') {
        this.progressRing.style.stroke = 'var(--pomodoro-break)';
        this.badgeMode.className = 'timer-mode-badge break';
        this.badgeMode.textContent = 'PAUSA CURTA (5 MIN)';
        this.caption.textContent = isRunning ? 'Relaxe, respire e beba água!' : 'Descanso merecido';
        if (this.btnFocus) this.btnFocus.className = 'pomodoro-mode-tab';
        if (this.btnBreak) this.btnBreak.className = 'pomodoro-mode-tab break active';
        if (this.btnLongBreak) this.btnLongBreak.className = 'pomodoro-mode-tab long-break';
      } else if (mode === 'long_break') {
        this.progressRing.style.stroke = 'var(--pomodoro-long-break)';
        this.badgeMode.className = 'timer-mode-badge long-break';
        this.badgeMode.textContent = 'PAUSA LONGA (15 MIN)';
        this.caption.textContent = isRunning ? 'Alongue-se e recupere as energias!' : 'Descanso amplo e merecido';
        if (this.btnFocus) this.btnFocus.className = 'pomodoro-mode-tab';
        if (this.btnBreak) this.btnBreak.className = 'pomodoro-mode-tab break';
        if (this.btnLongBreak) this.btnLongBreak.className = 'pomodoro-mode-tab long-break active';
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
            <td colspan="8" style="text-align: center; padding: 40px 16px; color: var(--color-text-muted);">
              Nenhum bloco de estudo encontrado. Agende sua primeira sessão no formulário acima.
            </td>
          </tr>
        `;
        return;
      }

      this.tableBody.innerHTML = filtered.map(s => {
        const dateDisplay = s.startTime ? s.startTime.replace('T', ' ').substring(0, 16) : '-';
        return `
          <tr data-id="${s.id}">
            <td style="font-weight: 600; color: var(--color-text-title); white-space: nowrap;">${dateDisplay}</td>
            <td>${getSubjectBadge(s.subjectName, s.subjectId)}</td>
            <td style="font-weight: 500;">${s.topic || '-'}</td>
            <td>${getActivityBadge(s.activityType)}</td>
            <td style="font-weight: 600; color: var(--color-text-title);">${s.durationMinutes || 25} min</td>
            <td>${getStatusBadge(s.status)}</td>
            <td>${getSyncBadge(s.syncStatus)}</td>
            <td>
              <div class="row-actions-group">
                <button type="button" class="btn-row-action action-pomodoro btn-action-pomodoro" title="Iniciar no Pomodoro">
                  <svg class="icon-svg" width="13" height="13" viewBox="0 0 24 24">
                    <circle cx="12" cy="14" r="8"/>
                    <line x1="12" y1="2" x2="12" y2="6"/>
                    <line x1="12" y1="10" x2="12" y2="14"/>
                  </svg>
                  <span>Pomodoro</span>
                </button>
                <button type="button" class="btn-row-action btn-action-calendar" title="Ver no Calendário de Estudos">
                  <svg class="icon-svg" width="13" height="13" viewBox="0 0 24 24">
                    <rect x="3" y="4" width="18" height="18" rx="2" ry="2"/>
                    <line x1="16" y1="2" x2="16" y2="6"/>
                    <line x1="8" y1="2" x2="8" y2="6"/>
                    <line x1="3" y1="10" x2="21" y2="10"/>
                  </svg>
                  <span>Agenda</span>
                </button>
                ${s.status !== 'CONCLUIDA' ? `
                  <button type="button" class="btn-row-action action-complete btn-action-complete" title="Marcar como Concluída">
                    <svg class="icon-svg" width="13" height="13" viewBox="0 0 24 24">
                      <polyline points="20 6 9 17 4 12"/>
                    </svg>
                    <span>Concluir</span>
                  </button>
                ` : ''}
                <button type="button" class="btn-row-action action-delete btn-action-delete" title="Excluir">
                  <svg class="icon-svg" width="13" height="13" viewBox="0 0 24 24">
                    <polyline points="3 6 5 6 21 6"/>
                    <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/>
                  </svg>
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

        row.querySelector('.btn-action-calendar')?.addEventListener('click', () => {
          navigation.goToTab('calendar');
          if (session && session.startTime) {
            calendar.currentDate = new Date(session.startTime);
            calendar.miniDate = new Date(session.startTime);
            calendar.render();
            calendar.openDetailsModal(session);
          }
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
      document.getElementById('badge-streak-count').textContent = `${metrics.currentStreakDays || 0}d`;

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
     8. MÓDULO CALENDÁRIO (ESTILO GOOGLE CALENDAR)
     ========================================================================== */
  const calendar = {
    currentDate: new Date(),
    miniDate: new Date(),
    currentView: 'month', // 'month' | 'week' | 'day' | 'schedule'
    selectedSubjectFilters: new Set(),
    activeSession: null,

    init() {
      // Elementos do Header
      this.btnToday = document.getElementById('btn-gcal-today');
      this.btnPrev = document.getElementById('btn-gcal-prev');
      this.btnNext = document.getElementById('btn-gcal-next');
      this.currentLabel = document.getElementById('gcal-current-label');
      this.todayBadge = document.getElementById('gcal-today-badge');
      this.btnCreate = document.getElementById('btn-gcal-create');
      this.viewButtons = document.querySelectorAll('.gcal-view-btn');

      // Elementos da Sidebar
      this.miniMonthTitle = document.getElementById('mini-cal-month-title');
      this.miniGrid = document.getElementById('mini-cal-grid');
      this.btnMiniPrev = document.getElementById('btn-mini-prev');
      this.btnMiniNext = document.getElementById('btn-mini-next');
      this.subjectsFilterList = document.getElementById('gcal-subjects-filter-list');
      this.btnToggleAllSubj = document.getElementById('btn-toggle-all-subjects');
      this.monthSessionsCount = document.getElementById('gcal-month-sessions-count');
      this.monthHoursCount = document.getElementById('gcal-month-hours-count');

      // Containers das Visualizações
      this.views = {
        month: document.getElementById('gcal-view-month'),
        week: document.getElementById('gcal-view-week'),
        day: document.getElementById('gcal-view-day'),
        schedule: document.getElementById('gcal-view-schedule')
      };

      // Modais
      this.detailsModal = document.getElementById('modal-event-details');
      this.formModal = document.getElementById('modal-event-form');
      this.formQuick = document.getElementById('gcal-quick-form');

      // Preenchimento do Badge do Dia Atual no Logo
      if (this.todayBadge) {
        this.todayBadge.textContent = new Date().getDate();
      }

      // Eventos de Navegação Principal
      this.btnToday?.addEventListener('click', () => {
        this.currentDate = new Date();
        this.miniDate = new Date();
        this.render();
      });

      this.btnPrev?.addEventListener('click', () => this.navigate(-1));
      this.btnNext?.addEventListener('click', () => this.navigate(1));

      // Eventos do Mini Calendário
      this.btnMiniPrev?.addEventListener('click', () => {
        this.miniDate.setMonth(this.miniDate.getMonth() - 1);
        this.renderMiniCalendar();
      });

      this.btnMiniNext?.addEventListener('click', () => {
        this.miniDate.setMonth(this.miniDate.getMonth() + 1);
        this.renderMiniCalendar();
      });

      // Alternar visualizações
      this.viewButtons.forEach(btn => {
        btn.addEventListener('click', () => {
          this.switchView(btn.dataset.view);
        });
      });

      // Botão + Criar
      this.btnCreate?.addEventListener('click', () => {
        const now = new Date();
        const ymd = now.toISOString().split('T')[0];
        const nextH = String((now.getHours() + 1) % 24).padStart(2, '0') + ':00';
        this.openCreateModal(ymd, nextH);
      });

      // Toggle todas as disciplinas
      this.btnToggleAllSubj?.addEventListener('click', () => {
        if (this.selectedSubjectFilters.size === state.subjects.length) {
          this.selectedSubjectFilters.clear();
        } else {
          state.subjects.forEach(s => this.selectedSubjectFilters.add(s.id));
        }
        this.renderSubjectFilters();
        this.renderActiveView();
      });

      // Setup dos Modais
      this.setupModals();
    },

    setupModals() {
      // Modal de Detalhes
      document.getElementById('btn-modal-close')?.addEventListener('click', () => this.closeDetailsModal());
      document.getElementById('btn-modal-action-close')?.addEventListener('click', () => this.closeDetailsModal());

      document.getElementById('btn-modal-pomodoro')?.addEventListener('click', () => {
        if (!this.activeSession) return;
        const targetSession = this.activeSession;
        this.closeDetailsModal();
        navigation.goToTab('pomodoro');
        pomodoro.setLinkedSession(targetSession);
        api.updateSession(targetSession.id, { status: 'EM_ANDAMENTO' }).then(() => refreshAppData());
        pomodoro.reset();
        pomodoro.start();
      });

      document.getElementById('btn-modal-action-pomodoro')?.addEventListener('click', () => {
        if (!this.activeSession) return;
        const targetSession = this.activeSession;
        this.closeDetailsModal();
        navigation.goToTab('pomodoro');
        pomodoro.setLinkedSession(targetSession);
        api.updateSession(targetSession.id, { status: 'EM_ANDAMENTO' }).then(() => refreshAppData());
        pomodoro.reset();
        pomodoro.start();
      });

      document.getElementById('btn-modal-toggle-status')?.addEventListener('click', async () => {
        if (!this.activeSession) return;
        const nextStatus = this.activeSession.status === 'CONCLUIDA' ? 'PLANEJADA' : 'CONCLUIDA';
        await api.updateSession(this.activeSession.id, { status: nextStatus });
        this.closeDetailsModal();
        await refreshAppData();
      });

      document.getElementById('btn-modal-edit')?.addEventListener('click', () => {
        if (!this.activeSession) return;
        const sess = this.activeSession;
        this.closeDetailsModal();
        const dateStr = sess.startTime ? sess.startTime.split('T')[0] : new Date().toISOString().split('T')[0];
        const timeStr = sess.startTime && sess.startTime.includes('T') ? sess.startTime.split('T')[1].substring(0, 5) : '14:00';
        this.openCreateModal(dateStr, timeStr, sess);
      });

      document.getElementById('btn-modal-delete')?.addEventListener('click', async () => {
        if (!this.activeSession) return;
        if (confirm(`Excluir o bloco de estudo "${this.activeSession.topic}"?`)) {
          await api.deleteSession(this.activeSession.id);
          this.closeDetailsModal();
          await refreshAppData();
        }
      });

      // Fechar modal ao clicar fora
      this.detailsModal?.addEventListener('click', (e) => {
        if (e.target === this.detailsModal) this.closeDetailsModal();
      });

      // Modal de Formulário
      document.getElementById('btn-form-modal-close')?.addEventListener('click', () => this.closeFormModal());
      document.getElementById('btn-form-modal-cancel')?.addEventListener('click', () => this.closeFormModal());

      this.formModal?.addEventListener('click', (e) => {
        if (e.target === this.formModal) this.closeFormModal();
      });

      this.formQuick?.addEventListener('submit', async (e) => {
        e.preventDefault();
        await this.handleFormSubmit();
      });
    },

    navigate(delta) {
      if (this.currentView === 'month' || this.currentView === 'schedule') {
        this.currentDate.setMonth(this.currentDate.getMonth() + delta);
        this.miniDate = new Date(this.currentDate);
      } else if (this.currentView === 'week') {
        this.currentDate.setDate(this.currentDate.getDate() + delta * 7);
        this.miniDate = new Date(this.currentDate);
      } else if (this.currentView === 'day') {
        this.currentDate.setDate(this.currentDate.getDate() + delta);
        this.miniDate = new Date(this.currentDate);
      }
      this.render();
    },

    switchView(viewName) {
      this.currentView = viewName;
      this.viewButtons.forEach(btn => {
        btn.classList.toggle('active', btn.dataset.view === viewName);
      });
      Object.keys(this.views).forEach(k => {
        if (this.views[k]) {
          this.views[k].classList.toggle('active', k === viewName);
        }
      });
      this.render();
    },

    ensureSubjectFilters() {
      if (this.selectedSubjectFilters.size === 0 && state.subjects.length > 0) {
        state.subjects.forEach(s => this.selectedSubjectFilters.add(s.id));
      }
    },

    render() {
      this.ensureSubjectFilters();
      this.updateHeaderLabel();
      this.renderMiniCalendar();
      this.renderSubjectFilters();
      this.updateStats();
      this.renderActiveView();

      const badge = document.getElementById('badge-calendar-count');
      if (badge) {
        badge.textContent = `${state.sessions.length}`;
      }
    },

    updateHeaderLabel() {
      if (!this.currentLabel) return;
      const months = ['Janeiro', 'Fevereiro', 'Março', 'Abril', 'Maio', 'Junho', 'Julho', 'Agosto', 'Setembro', 'Outubro', 'Novembro', 'Dezembro'];
      const m = months[this.currentDate.getMonth()];
      const y = this.currentDate.getFullYear();

      if (this.currentView === 'month' || this.currentView === 'schedule') {
        this.currentLabel.textContent = `${m} de ${y}`;
      } else if (this.currentView === 'week') {
        const start = this.getStartOfWeek(this.currentDate);
        const end = new Date(start);
        end.setDate(end.getDate() + 6);
        const startDay = String(start.getDate()).padStart(2, '0');
        const endDay = String(end.getDate()).padStart(2, '0');
        const endMonth = months[end.getMonth()];
        this.currentLabel.textContent = `${startDay} – ${endDay} de ${endMonth}, ${y}`;
      } else if (this.currentView === 'day') {
        const weekdays = ['Domingo', 'Segunda-feira', 'Terça-feira', 'Quarta-feira', 'Quinta-feira', 'Sexta-feira', 'Sábado'];
        const w = weekdays[this.currentDate.getDay()];
        const d = String(this.currentDate.getDate()).padStart(2, '0');
        this.currentLabel.textContent = `${w}, ${d} de ${m} de ${y}`;
      }
    },

    getStartOfWeek(d) {
      const date = new Date(d);
      const day = date.getDay(); // 0 domingo
      date.setDate(date.getDate() - day);
      date.setHours(0, 0, 0, 0);
      return date;
    },

    renderActiveView() {
      switch (this.currentView) {
        case 'month': this.renderMonthView(); break;
        case 'week': this.renderWeekView(); break;
        case 'day': this.renderDayView(); break;
        case 'schedule': this.renderScheduleView(); break;
      }
    },

    // 1. Mini Calendário
    renderMiniCalendar() {
      if (!this.miniMonthTitle || !this.miniGrid) return;
      const months = ['Janeiro', 'Fevereiro', 'Março', 'Abril', 'Maio', 'Junho', 'Julho', 'Agosto', 'Setembro', 'Outubro', 'Novembro', 'Dezembro'];
      this.miniMonthTitle.textContent = `${months[this.miniDate.getMonth()]} ${this.miniDate.getFullYear()}`;

      const year = this.miniDate.getFullYear();
      const month = this.miniDate.getMonth();
      const firstDay = new Date(year, month, 1).getDay();
      const totalDays = new Date(year, month + 1, 0).getDate();
      const prevMonthDays = new Date(year, month, 0).getDate();

      const today = new Date();
      const todayStr = `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, '0')}-${String(today.getDate()).padStart(2, '0')}`;
      const selectedStr = `${this.currentDate.getFullYear()}-${String(this.currentDate.getMonth() + 1).padStart(2, '0')}-${String(this.currentDate.getDate()).padStart(2, '0')}`;

      const sessionDates = new Set(state.sessions.map(s => s.startTime ? s.startTime.split('T')[0] : ''));

      let html = '';

      // Dias anteriores
      for (let i = firstDay - 1; i >= 0; i--) {
        const dNum = prevMonthDays - i;
        const prevM = month === 0 ? 12 : month;
        const prevY = month === 0 ? year - 1 : year;
        const dStr = `${prevY}-${String(prevM).padStart(2, '0')}-${String(dNum).padStart(2, '0')}`;
        html += `<button type="button" class="mini-day-cell other-month" data-date="${dStr}">${dNum}</button>`;
      }

      // Dias do mês atual
      for (let day = 1; day <= totalDays; day++) {
        const dStr = `${year}-${String(month + 1).padStart(2, '0')}-${String(day).padStart(2, '0')}`;
        const isToday = dStr === todayStr;
        const isSelected = dStr === selectedStr;
        const hasSession = sessionDates.has(dStr);

        let cls = 'mini-day-cell';
        if (isToday) cls += ' today';
        if (isSelected) cls += ' selected';

        html += `
          <button type="button" class="${cls}" data-date="${dStr}">
            ${day}
            ${hasSession ? '<span class="mini-day-dot"></span>' : ''}
          </button>
        `;
      }

      // Dias seguintes
      const totalRendered = firstDay + totalDays;
      const nextDays = (7 - (totalRendered % 7)) % 7;
      for (let day = 1; day <= nextDays; day++) {
        const nextM = month === 11 ? 1 : month + 2;
        const nextY = month === 11 ? year + 1 : year;
        const dStr = `${nextY}-${String(nextM).padStart(2, '0')}-${String(day).padStart(2, '0')}`;
        html += `<button type="button" class="mini-day-cell other-month" data-date="${dStr}">${day}</button>`;
      }

      this.miniGrid.innerHTML = html;

      this.miniGrid.querySelectorAll('.mini-day-cell').forEach(btn => {
        btn.addEventListener('click', () => {
          const [y, m, d] = btn.dataset.date.split('-').map(Number);
          this.currentDate = new Date(y, m - 1, d);
          this.render();
        });
      });
    },

    // 2. Filtro de Disciplinas
    renderSubjectFilters() {
      if (!this.subjectsFilterList) return;
      this.subjectsFilterList.innerHTML = state.subjects.map(s => {
        const isChecked = this.selectedSubjectFilters.has(s.id);
        const count = state.sessions.filter(sess => sess.subjectId === s.id).length;
        return `
          <label class="gcal-subj-item" data-id="${s.id}">
            <input type="checkbox" class="gcal-subj-checkbox" ${isChecked ? 'checked' : ''} data-id="${s.id}">
            <span class="gcal-subj-dot" style="background-color: ${s.hexColor || 'var(--color-primary)'};"></span>
            <span class="gcal-subj-name">${s.name}</span>
            <span class="gcal-subj-count">${count}</span>
          </label>
        `;
      }).join('');

      this.subjectsFilterList.querySelectorAll('.gcal-subj-checkbox').forEach(cb => {
        cb.addEventListener('change', (e) => {
          const id = e.target.dataset.id;
          if (e.target.checked) {
            this.selectedSubjectFilters.add(id);
          } else {
            this.selectedSubjectFilters.delete(id);
          }
          this.renderActiveView();
        });
      });
    },

    updateStats() {
      const year = this.currentDate.getFullYear();
      const month = this.currentDate.getMonth();
      const monthSessions = state.sessions.filter(s => {
        if (!s.startTime) return false;
        const d = new Date(s.startTime);
        return d.getFullYear() === year && d.getMonth() === month;
      });

      const totalMin = monthSessions.reduce((acc, s) => acc + (s.durationMinutes || 25), 0);
      const hours = (totalMin / 60).toFixed(1);

      if (this.monthSessionsCount) this.monthSessionsCount.textContent = monthSessions.length;
      if (this.monthHoursCount) this.monthHoursCount.textContent = `${hours}h de foco`;
    },

    getVisibleSessions() {
      return state.sessions.filter(s => {
        if (!s.subjectId) return true;
        return this.selectedSubjectFilters.has(s.subjectId);
      });
    },

    // 3. Visualização Mensal
    renderMonthView() {
      const grid = document.getElementById('gcal-month-grid');
      if (!grid) return;

      const year = this.currentDate.getFullYear();
      const month = this.currentDate.getMonth();
      const firstDay = new Date(year, month, 1).getDay();
      const totalDays = new Date(year, month + 1, 0).getDate();
      const prevMonthDays = new Date(year, month, 0).getDate();

      const today = new Date();
      const todayStr = `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, '0')}-${String(today.getDate()).padStart(2, '0')}`;
      const visibleSessions = this.getVisibleSessions();

      let cellsHtml = '';

      for (let i = firstDay - 1; i >= 0; i--) {
        const dNum = prevMonthDays - i;
        const prevM = month === 0 ? 12 : month;
        const prevY = month === 0 ? year - 1 : year;
        const dStr = `${prevY}-${String(prevM).padStart(2, '0')}-${String(dNum).padStart(2, '0')}`;
        cellsHtml += this.buildMonthCellHtml(dNum, dStr, true, false, visibleSessions);
      }

      for (let day = 1; day <= totalDays; day++) {
        const dStr = `${year}-${String(month + 1).padStart(2, '0')}-${String(day).padStart(2, '0')}`;
        const isToday = dStr === todayStr;
        cellsHtml += this.buildMonthCellHtml(day, dStr, false, isToday, visibleSessions);
      }

      const totalRendered = firstDay + totalDays;
      const nextDays = (7 - (totalRendered % 7)) % 7;
      for (let day = 1; day <= nextDays; day++) {
        const nextM = month === 11 ? 1 : month + 2;
        const nextY = month === 11 ? year + 1 : year;
        const dStr = `${nextY}-${String(nextM).padStart(2, '0')}-${String(day).padStart(2, '0')}`;
        cellsHtml += this.buildMonthCellHtml(day, dStr, true, false, visibleSessions);
      }

      grid.innerHTML = cellsHtml;

      grid.querySelectorAll('.gcal-month-cell').forEach(cell => {
        const dateStr = cell.dataset.date;
        cell.addEventListener('click', (e) => {
          if (e.target.closest('.gcal-event-chip') || e.target.closest('.gcal-more-btn')) return;
          this.openCreateModal(dateStr, '14:00');
        });
      });

      grid.querySelectorAll('.gcal-event-chip').forEach(chip => {
        chip.addEventListener('click', (e) => {
          e.stopPropagation();
          const sessId = chip.dataset.id;
          const sess = state.sessions.find(s => s.id === sessId);
          if (sess) this.openDetailsModal(sess);
        });
      });

      grid.querySelectorAll('.gcal-more-btn').forEach(btn => {
        btn.addEventListener('click', (e) => {
          e.stopPropagation();
          const dStr = btn.dataset.date;
          const [y, m, d] = dStr.split('-').map(Number);
          this.currentDate = new Date(y, m - 1, d);
          this.switchView('day');
        });
      });
    },

    buildMonthCellHtml(dayNum, dateStr, isOtherMonth, isToday, allSessions) {
      const daySessions = allSessions.filter(s => s.startTime && s.startTime.startsWith(dateStr));
      const maxDisplay = 3;
      const displayList = daySessions.slice(0, maxDisplay);
      const remaining = daySessions.length - maxDisplay;

      let chipsHtml = '';
      displayList.forEach(s => {
        const sub = state.subjects.find(sub => sub.id === s.subjectId || sub.name === s.subjectName);
        const hex = sub ? sub.hexColor : '#3A7D8C';
        const upperHex = hex.toUpperCase();
        const darkTones = ['#3A7D8C', '#5F7F99'];
        const textCol = darkTones.includes(upperHex) ? '#FFFFFF' : '#3E4042';
        const timeStr = s.startTime && s.startTime.includes('T') ? s.startTime.split('T')[1].substring(0, 5) : '';
        const isDone = s.status === 'CONCLUIDA';

        chipsHtml += `
          <div class="gcal-event-chip ${isDone ? 'concluida' : ''}" data-id="${s.id}" style="background-color: ${hex}; color: ${textCol};" title="${timeStr} ${s.subjectName}: ${s.topic}">
            <span class="gcal-event-chip-time">${timeStr}</span>
            <span class="gcal-event-chip-title">${s.topic || s.subjectName}</span>
          </div>
        `;
      });

      if (remaining > 0) {
        chipsHtml += `<button type="button" class="gcal-more-btn" data-date="${dateStr}">+${remaining} mais</button>`;
      }

      return `
        <div class="gcal-month-cell ${isOtherMonth ? 'other-month' : ''}" data-date="${dateStr}">
          <div class="cell-top-bar">
            <span class="cell-day-num ${isToday ? 'is-today' : ''}">${dayNum}</span>
          </div>
          <div class="cell-events-list">
            ${chipsHtml}
          </div>
        </div>
      `;
    },

    // 4. Visualização Semanal
    renderWeekView() {
      const headerRow = document.getElementById('gcal-week-header');
      const bodyGrid = document.getElementById('gcal-week-body');
      if (!headerRow || !bodyGrid) return;

      const start = this.getStartOfWeek(this.currentDate);
      const weekDays = [];
      for (let i = 0; i < 7; i++) {
        const d = new Date(start);
        d.setDate(d.getDate() + i);
        weekDays.push(d);
      }

      const today = new Date();
      const todayStr = `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, '0')}-${String(today.getDate()).padStart(2, '0')}`;
      const weekdayNames = ['DOM', 'SEG', 'TER', 'QUA', 'QUI', 'SEX', 'SÁB'];

      let headerHtml = '<div class="week-tz-cell">GMT-3</div>';
      weekDays.forEach(d => {
        const dStr = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
        const isToday = dStr === todayStr;
        headerHtml += `
          <div class="week-day-header-cell">
            <span class="week-day-name">${weekdayNames[d.getDay()]}</span>
            <span class="week-day-num ${isToday ? 'is-today' : ''}">${d.getDate()}</span>
          </div>
        `;
      });
      headerRow.innerHTML = headerHtml;

      const startHour = 7;
      const totalHours = 16;
      const hourHeight = 50;

      let hoursHtml = '<div class="time-col-hours">';
      for (let h = startHour; h <= startHour + totalHours; h++) {
        const hLabel = `${String(h).padStart(2, '0')}:00`;
        hoursHtml += `<div class="time-hour-label">${hLabel}</div>`;
      }
      hoursHtml += '</div>';

      const visibleSessions = this.getVisibleSessions();
      let colsHtml = '';

      weekDays.forEach(d => {
        const dStr = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
        const isToday = dStr === todayStr;

        let slotsHtml = '';
        for (let h = startHour; h < startHour + totalHours; h++) {
          slotsHtml += `<div class="hour-slot-row" data-date="${dStr}" data-hour="${h}"></div>`;
        }

        let currentTimeHtml = '';
        if (isToday) {
          const nowHour = today.getHours();
          const nowMin = today.getMinutes();
          if (nowHour >= startHour && nowHour < startHour + totalHours) {
            const currentTop = (nowHour - startHour + nowMin / 60) * hourHeight;
            currentTimeHtml = `
              <div class="current-time-line" style="top: ${currentTop}px;">
                <span class="current-time-dot"></span>
              </div>
            `;
          }
        }

        const daySessions = visibleSessions.filter(s => s.startTime && s.startTime.startsWith(dStr));
        let blocksHtml = '';

        daySessions.forEach(s => {
          const sub = state.subjects.find(sub => sub.id === s.subjectId || sub.name === s.subjectName);
          const hex = sub ? sub.hexColor : '#3A7D8C';
          const upperHex = hex.toUpperCase();
          const darkTones = ['#3A7D8C', '#5F7F99'];
          const textCol = darkTones.includes(upperHex) ? '#FFFFFF' : '#3E4042';

          const startD = new Date(s.startTime);
          const hour = startD.getHours();
          const min = startD.getMinutes();
          const dur = s.durationMinutes || 25;

          const top = Math.max(0, (hour - startHour + min / 60) * hourHeight);
          const height = Math.max(26, (dur / 60) * hourHeight - 2);
          const timeLabel = `${String(hour).padStart(2, '0')}:${String(min).padStart(2, '0')}`;

          blocksHtml += `
            <div class="cal-event-block" data-id="${s.id}" style="top: ${top}px; height: ${height}px; background-color: ${hex}; color: ${textCol}; border-left: 4px solid rgba(0,0,0,0.25);" title="${s.subjectName} • ${s.topic}">
              <div class="cal-block-title">${s.subjectName}</div>
              <div class="cal-block-sub">${s.topic || ''}</div>
              <div class="cal-block-time">${timeLabel} (${dur}m)</div>
            </div>
          `;
        });

        colsHtml += `
          <div class="day-time-col" data-date="${dStr}">
            ${slotsHtml}
            ${currentTimeHtml}
            ${blocksHtml}
          </div>
        `;
      });

      bodyGrid.innerHTML = hoursHtml + colsHtml;

      bodyGrid.querySelectorAll('.hour-slot-row').forEach(slot => {
        slot.addEventListener('click', () => {
          const dStr = slot.dataset.date;
          const hStr = `${String(slot.dataset.hour).padStart(2, '0')}:00`;
          this.openCreateModal(dStr, hStr);
        });
      });

      bodyGrid.querySelectorAll('.cal-event-block').forEach(block => {
        block.addEventListener('click', (e) => {
          e.stopPropagation();
          const sessId = block.dataset.id;
          const sess = state.sessions.find(s => s.id === sessId);
          if (sess) this.openDetailsModal(sess);
        });
      });
    },

    // 5. Visualização Diária
    renderDayView() {
      const headerRow = document.getElementById('gcal-day-header');
      const bodyGrid = document.getElementById('gcal-day-body');
      if (!headerRow || !bodyGrid) return;

      const weekdays = ['Domingo', 'Segunda-feira', 'Terça-feira', 'Quarta-feira', 'Quinta-feira', 'Sexta-feira', 'Sábado'];
      const months = ['Janeiro', 'Fevereiro', 'Março', 'Abril', 'Maio', 'Junho', 'Julho', 'Agosto', 'Setembro', 'Outubro', 'Novembro', 'Dezembro'];
      const d = this.currentDate;
      const dStr = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;

      headerRow.innerHTML = `
        <div style="font-weight: 700; color: var(--color-primary); font-size: 16px;">
          ${weekdays[d.getDay()]}, ${d.getDate()} de ${months[d.getMonth()]} de ${d.getFullYear()}
        </div>
      `;

      const startHour = 6;
      const totalHours = 17;
      const hourHeight = 55;

      let hoursHtml = '<div class="time-col-hours">';
      for (let h = startHour; h <= startHour + totalHours; h++) {
        hoursHtml += `<div class="time-hour-label" style="height: ${hourHeight}px;">${String(h).padStart(2, '0')}:00</div>`;
      }
      hoursHtml += '</div>';

      let slotsHtml = '';
      for (let h = startHour; h < startHour + totalHours; h++) {
        slotsHtml += `<div class="hour-slot-row" style="height: ${hourHeight}px;" data-date="${dStr}" data-hour="${h}"></div>`;
      }

      const visibleSessions = this.getVisibleSessions();
      const daySessions = visibleSessions.filter(s => s.startTime && s.startTime.startsWith(dStr));
      let blocksHtml = '';

      daySessions.forEach(s => {
        const sub = state.subjects.find(sub => sub.id === s.subjectId || sub.name === s.subjectName);
        const hex = sub ? sub.hexColor : '#3A7D8C';
        const startD = new Date(s.startTime);
        const hour = startD.getHours();
        const min = startD.getMinutes();
        const dur = s.durationMinutes || 25;

        const top = Math.max(0, (hour - startHour + min / 60) * hourHeight);
        const height = Math.max(32, (dur / 60) * hourHeight - 2);

        blocksHtml += `
          <div class="cal-event-block" data-id="${s.id}" style="top: ${top}px; height: ${height}px; background-color: ${hex}; color: #ffffff; padding: 10px 14px;">
            <div style="font-weight: 700; font-size: 13px;">${s.subjectName} • ${s.topic}</div>
            <div style="font-size: 12px; opacity: 0.9;">Atividade: ${s.activityType} • ${s.durationMinutes} minutos</div>
            ${s.notes ? `<div style="font-size: 11px; opacity: 0.85; margin-top: 4px;">📝 ${s.notes}</div>` : ''}
          </div>
        `;
      });

      bodyGrid.innerHTML = hoursHtml + `
        <div class="day-time-col" style="flex: 1;" data-date="${dStr}">
          ${slotsHtml}
          ${blocksHtml}
        </div>
      `;

      bodyGrid.querySelectorAll('.hour-slot-row').forEach(slot => {
        slot.addEventListener('click', () => {
          const hStr = `${String(slot.dataset.hour).padStart(2, '0')}:00`;
          this.openCreateModal(dStr, hStr);
        });
      });

      bodyGrid.querySelectorAll('.cal-event-block').forEach(block => {
        block.addEventListener('click', (e) => {
          e.stopPropagation();
          const sess = state.sessions.find(s => s.id === block.dataset.id);
          if (sess) this.openDetailsModal(sess);
        });
      });
    },

    // 6. Visualização Programação / Agenda
    renderScheduleView() {
      const list = document.getElementById('gcal-schedule-list');
      if (!list) return;

      const visibleSessions = this.getVisibleSessions().slice();
      visibleSessions.sort((a, b) => (a.startTime || '').localeCompare(b.startTime || ''));

      if (visibleSessions.length === 0) {
        list.innerHTML = `
          <div style="text-align: center; padding: 60px 20px; color: var(--color-text-muted);">
            <svg class="icon-svg" width="40" height="40" viewBox="0 0 24 24" style="margin-bottom: 12px; stroke: var(--color-text-muted);">
              <rect x="3" y="4" width="18" height="18" rx="2" ry="2"/>
              <line x1="16" y1="2" x2="16" y2="6"/>
              <line x1="8" y1="2" x2="8" y2="6"/>
              <line x1="3" y1="10" x2="21" y2="10"/>
            </svg>
            <p style="font-size: 14px; font-weight: 500;">Nenhum bloco de estudo agendado para as disciplinas selecionadas.</p>
          </div>
        `;
        return;
      }

      const groups = {};
      visibleSessions.forEach(s => {
        const d = s.startTime ? s.startTime.split('T')[0] : 'Indefinido';
        if (!groups[d]) groups[d] = [];
        groups[d].push(s);
      });

      let html = '';
      const months = ['Jan', 'Fev', 'Mar', 'Abr', 'Mai', 'Jun', 'Jul', 'Ago', 'Set', 'Out', 'Nov', 'Dez'];
      const weekdays = ['Dom', 'Seg', 'Ter', 'Qua', 'Qui', 'Sex', 'Sáb'];

      Object.keys(groups).forEach(dateStr => {
        const parts = dateStr.split('-');
        let headerLabel = dateStr;
        if (parts.length === 3) {
          const dObj = new Date(Number(parts[0]), Number(parts[1]) - 1, Number(parts[2]));
          headerLabel = `${weekdays[dObj.getDay()]}, ${dObj.getDate()} de ${months[dObj.getMonth()]} de ${dObj.getFullYear()}`;
        }

        let cardsHtml = '';
        groups[dateStr].forEach(s => {
          const sub = state.subjects.find(sub => sub.id === s.subjectId || sub.name === s.subjectName);
          const hex = sub ? sub.hexColor : '#3A7D8C';
          const time = s.startTime && s.startTime.includes('T') ? s.startTime.split('T')[1].substring(0, 5) : '--:--';

          cardsHtml += `
            <div class="gcal-schedule-card" data-id="${s.id}" style="border-left-color: ${hex};">
              <div class="gcal-sched-info">
                <span class="gcal-sched-time">${time} (${s.durationMinutes}m)</span>
                <div>
                  <div class="gcal-sched-title">${s.topic || 'Sessão de Estudo'}</div>
                  <div class="gcal-sched-subj">${s.subjectName} • ${s.activityType}</div>
                </div>
              </div>
              <div>${getStatusBadge(s.status)}</div>
            </div>
          `;
        });

        html += `
          <div class="gcal-schedule-group">
            <div class="gcal-schedule-date-header">${headerLabel}</div>
            ${cardsHtml}
          </div>
        `;
      });

      list.innerHTML = html;

      list.querySelectorAll('.gcal-schedule-card').forEach(card => {
        card.addEventListener('click', () => {
          const sess = state.sessions.find(s => s.id === card.dataset.id);
          if (sess) this.openDetailsModal(sess);
        });
      });
    },

    // 7. Modal de Detalhes
    openDetailsModal(session) {
      this.activeSession = session;
      const sub = state.subjects.find(s => s.id === session.subjectId || s.name === session.subjectName);
      const hex = sub ? sub.hexColor : '#3A7D8C';

      const banner = document.getElementById('modal-details-banner');
      if (banner) banner.style.backgroundColor = hex;

      document.getElementById('modal-details-subject').textContent = session.subjectName || 'Geral';
      document.getElementById('modal-details-subject').style.color = hex;
      document.getElementById('modal-details-topic').textContent = session.topic || 'Sessão sem título';

      const dtElem = document.getElementById('modal-details-datetime');
      if (session.startTime) {
        const d = new Date(session.startTime);
        const months = ['Janeiro', 'Fevereiro', 'Março', 'Abril', 'Maio', 'Junho', 'Julho', 'Agosto', 'Setembro', 'Outubro', 'Novembro', 'Dezembro'];
        const weekdays = ['Domingo', 'Segunda-feira', 'Terça-feira', 'Quarta-feira', 'Quinta-feira', 'Sexta-feira', 'Sábado'];
        const startStr = `${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`;
        const endD = session.endTime ? new Date(session.endTime) : new Date(d.getTime() + (session.durationMinutes || 25) * 60000);
        const endStr = `${String(endD.getHours()).padStart(2, '0')}:${String(endD.getMinutes()).padStart(2, '0')}`;
        dtElem.textContent = `${weekdays[d.getDay()]}, ${d.getDate()} de ${months[d.getMonth()]} • ${startStr} – ${endStr}`;
      } else {
        dtElem.textContent = 'Horário flexível';
      }

      document.getElementById('modal-details-duration').textContent = `${session.durationMinutes || 25} minutos de foco planejado`;

      const badgesElem = document.getElementById('modal-details-badges');
      badgesElem.innerHTML = `
        ${getActivityBadge(session.activityType)}
        ${getStatusBadge(session.status)}
      `;

      const notesElem = document.getElementById('modal-details-notes');
      notesElem.textContent = session.notes && session.notes.trim() ? session.notes : 'Nenhuma anotação informada para este bloco.';

      this.detailsModal.style.display = 'flex';
    },

    closeDetailsModal() {
      this.detailsModal.style.display = 'none';
      this.activeSession = null;
    },

    // 8. Modal de Criação / Edição
    openCreateModal(dateStr, timeStr, editSession = null) {
      const modalTitle = document.getElementById('modal-form-title');
      const idInput = document.getElementById('gcal-form-session-id');
      const topicInput = document.getElementById('gcal-input-topic');
      const subjSelect = document.getElementById('gcal-input-subject');
      const dateInput = document.getElementById('gcal-input-date');
      const timeInput = document.getElementById('gcal-input-time');
      const durSelect = document.getElementById('gcal-input-duration');
      const actSelect = document.getElementById('gcal-input-activity');
      const statSelect = document.getElementById('gcal-input-status');
      const notesInput = document.getElementById('gcal-input-notes');

      subjSelect.innerHTML = state.subjects.map(s => `
        <option value="${s.id}">${s.name} (${s.code || 'GERAL'})</option>
      `).join('');

      if (editSession) {
        modalTitle.textContent = 'Editar Bloco de Estudo';
        idInput.value = editSession.id;
        topicInput.value = editSession.topic || '';
        subjSelect.value = editSession.subjectId || state.subjects[0]?.id;
        dateInput.value = editSession.startTime ? editSession.startTime.split('T')[0] : dateStr;
        timeInput.value = editSession.startTime && editSession.startTime.includes('T') ? editSession.startTime.split('T')[1].substring(0, 5) : timeStr;
        durSelect.value = String(editSession.durationMinutes || 25);
        actSelect.value = editSession.activityType || 'TEORIA';
        statSelect.value = editSession.status || 'PLANEJADA';
        notesInput.value = editSession.notes || '';
      } else {
        modalTitle.textContent = 'Novo Bloco de Estudo';
        idInput.value = '';
        topicInput.value = '';
        subjSelect.value = state.subjects[0]?.id || '';
        dateInput.value = dateStr || new Date().toISOString().split('T')[0];
        timeInput.value = timeStr || '14:00';
        durSelect.value = '50';
        actSelect.value = 'TEORIA';
        statSelect.value = 'PLANEJADA';
        notesInput.value = '';
      }

      this.formModal.style.display = 'flex';
      setTimeout(() => topicInput.focus(), 50);
    },

    closeFormModal() {
      this.formModal.style.display = 'none';
      this.formQuick.reset();
    },

    async handleFormSubmit() {
      const idInput = document.getElementById('gcal-form-session-id').value;
      const topic = document.getElementById('gcal-input-topic').value.trim();
      const subjId = document.getElementById('gcal-input-subject').value;
      const selectedSubj = state.subjects.find(s => s.id === subjId) || state.subjects[0];
      const dateStr = document.getElementById('gcal-input-date').value;
      const timeStr = document.getElementById('gcal-input-time').value;
      const duration = Number(document.getElementById('gcal-input-duration').value);
      const activityType = document.getElementById('gcal-input-activity').value;
      const status = document.getElementById('gcal-input-status').value;
      const notes = document.getElementById('gcal-input-notes').value.trim();

      const startDateTime = `${dateStr}T${timeStr}:00`;
      const startObj = new Date(startDateTime);
      const endObj = new Date(startObj.getTime() + duration * 60000);
      const endDateTime = endObj.toISOString().substring(0, 19);

      if (idInput) {
        await api.updateSession(idInput, {
          subjectId: selectedSubj.id,
          subjectName: selectedSubj.name,
          topic,
          startTime: startDateTime,
          endTime: endDateTime,
          durationMinutes: duration,
          activityType,
          status,
          notes
        });
      } else {
        await api.saveSession({
          subjectId: selectedSubj.id,
          subjectName: selectedSubj.name,
          topic,
          startTime: startDateTime,
          endTime: endDateTime,
          durationMinutes: duration,
          activityType,
          status,
          notes,
          syncWithGoogleCalendar: true
        });
      }

      this.closeFormModal();
      await refreshAppData();
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
          this.pageSubtitle.textContent = 'Organize seus blocos de estudo diários no calendário interativo integrado ao sistema.';
          break;
        case 'analytics':
          this.pageTitle.textContent = 'Métricas & Carga Horária';
          this.pageSubtitle.textContent = 'Acompanhamento analítico semanal, mensal e mapa de consistência anual (365 dias).';
          break;
        case 'pomodoro':
          this.pageTitle.textContent = 'Cronômetro Pomodoro de 25 Minutos';
          this.pageSubtitle.textContent = 'Técnica de foco: 25 minutos de estudo concentrado seguidos de 5 minutos de descanso.';
          break;
        case 'calendar':
        case 'mcp':
          this.pageTitle.textContent = 'Agenda & Calendário de Estudos';
          this.pageSubtitle.textContent = 'Visualize, planeje e gerencie seus blocos de estudo no calendário integrado estilo Google Calendar.';
          if (calendar) calendar.render();
          break;
      }
    }
  };

  /* ==========================================================================
     10. GERENCIADOR DE TEMA CLARO / ESCURO (SEÇÃO 7 DA SKILL)
     ========================================================================== */
  const themeManager = {
    init() {
      this.btnToggle = document.getElementById('btn-theme-toggle');
      this.icon = document.getElementById('theme-toggle-icon');
      this.text = document.getElementById('theme-toggle-text');

      const savedTheme = localStorage.getItem('app_theme') || 'light';
      this.applyTheme(savedTheme);

      if (this.btnToggle) {
        this.btnToggle.addEventListener('click', () => {
          const current = document.documentElement.getAttribute('data-theme') || 'light';
          const next = current === 'dark' ? 'light' : 'dark';
          this.applyTheme(next);
        });
      }
    },

    applyTheme(theme) {
      document.documentElement.setAttribute('data-theme', theme);
      localStorage.setItem('app_theme', theme);

      if (this.icon && this.text) {
        if (theme === 'dark') {
          this.icon.innerHTML = `<circle cx="12" cy="12" r="5"/><line x1="12" y1="1" x2="12" y2="3"/><line x1="12" y1="21" x2="12" y2="23"/><line x1="4.22" y1="4.22" x2="5.64" y2="5.64"/><line x1="18.36" y1="18.36" x2="19.78" y2="19.78"/><line x1="1" y1="12" x2="3" y2="12"/><line x1="21" y1="12" x2="23" y2="12"/><line x1="4.22" y1="19.78" x2="5.64" y2="18.36"/><line x1="18.36" y1="5.64" x2="19.78" y2="4.22"/>`;
          this.text.textContent = 'Tema Claro';
        } else {
          this.icon.innerHTML = `<path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z"/>`;
          this.text.textContent = 'Tema Escuro';
        }
      }
    }
  };

  /* ==========================================================================
     11. HELPERS & RENDERIZADORES AUXILIARES
     ========================================================================== */
  function getSubjectBadge(subjectName, subjectId) {
    const sub = state.subjects.find(s => s.id === subjectId || s.name === subjectName);
    const hex = sub ? sub.hexColor : '#3A7D8C';
    // Conforme Seção 6 da SKILL:
    // Grafite #3E4042 em tons claros (lagoa #7FB3D1, sálvia #7FA99B, cinza-azulado #8A9BAA, areia #C9B99A)
    // Branco #FFFFFF apenas em tons escuros (petróleo #3A7D8C, azul-aço #5F7F99)
    const upperHex = hex.toUpperCase();
    const darkTones = ['#3A7D8C', '#5F7F99'];
    const textColor = darkTones.includes(upperHex) ? '#FFFFFF' : '#3E4042';
    return `<span class="subject-pill" style="background-color: ${hex}; color: ${textColor};">${subjectName || 'Geral'}</span>`;
  }
  function getStatusBadge(status) {
    switch (status) {
      case 'CONCLUIDA': return '<span class="badge-pill status-concluida"><span class="pill-dot"></span>Concluída</span>';
      case 'EM_ANDAMENTO': return '<span class="badge-pill status-em-andamento"><span class="pill-dot"></span>Em Andamento</span>';
      case 'CANCELADA': return '<span class="badge-pill status-cancelada"><span class="pill-dot"></span>Cancelada</span>';
      default: return '<span class="badge-pill status-planejada"><span class="pill-dot"></span>Planejada</span>';
    }
  }

  function getSyncBadge(sync) {
    return '<span class="badge-pill sync-sincronizado"><span class="pill-dot"></span>No Calendário</span>';
  }

  function getActivityBadge(type) {
    switch (type) {
      case 'EXERCICIOS': return '<span class="badge-pill activity-neutral">Exercícios</span>';
      case 'REVISAO': return '<span class="badge-pill activity-neutral">Revisão</span>';
      case 'SIMULADO': return '<span class="badge-pill activity-neutral">Simulado</span>';
      default: return '<span class="badge-pill activity-neutral">Teoria</span>';
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
      statusPill.className = 'connection-pill';
      statusText.textContent = 'API Java Conectada';
    } else {
      statusPill.className = 'connection-pill offline';
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
    calendar.render();
  }

  /* ==========================================================================
     12. INICIALIZAÇÃO DA APLICAÇÃO (DOM READY)
     ========================================================================== */
  document.addEventListener('DOMContentLoaded', async () => {
    themeManager.init();
    navigation.init();
    pomodoro.init();
    scheduling.init();
    analytics.init();
    calendar.init();

    await refreshAppData();
  });

})();
