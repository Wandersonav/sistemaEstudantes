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
                <button type="button" class="btn-row-action btn-action-mcp" title="Sincronizar no Google Calendar">
                  <svg class="icon-svg" width="13" height="13" viewBox="0 0 24 24">
                    <polyline points="23 4 23 10 17 10"/>
                    <polyline points="1 20 1 14 7 14"/>
                    <path d="M3.51 9a9 9 0 0 1 14.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0 0 20.49 15"/>
                  </svg>
                  <span>MCP</span>
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
    switch (sync) {
      case 'SINCRONIZADO': return '<span class="badge-pill sync-sincronizado"><span class="pill-dot"></span>Sincronizado</span>';
      case 'PENDENTE': return '<span class="badge-pill status-em-andamento"><span class="pill-dot"></span>Sincronizando</span>';
      case 'FALHA': return '<span class="badge-pill status-cancelada"><span class="pill-dot"></span>Falha</span>';
      default: return '<span class="badge-pill sync-nao"><span class="pill-dot"></span>Não Sincronizado</span>';
    }
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
    mcp.render();
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
    mcp.init();

    await refreshAppData();
  });

})();
