/**
 * dashboard.js — SDAE Real-Time Telemetry & Monitoring
 * Atualização contínua via polling inteligente (2s) consumindo MySQL.
 */

let periodoAtual = '15m';
let ldrHistorico = [];

async function carregarDashboard() {
  try {
    const res = await fetch('/api/dashboard', { credentials: 'same-origin' });
    if (!res.ok) return;
    const data = await res.json();

    // 1. Atualizar KPIs (Req. 16)
    setEl('kFuncionando', data.esteirasFuncionando ?? 0);
    setEl('kParadas',     data.esteirasParadas ?? 0);
    setEl('kOffline',     data.esteirasOffline ?? 0);
    setEl('kSensores',    data.totalSensores ?? 0);
    setEl('kAlert',       data.totalAlertas ?? 0);

    // 2. Atualizar Gauge de Risco
    const risco = data.nivelRiscoAtual ?? 0;
    setGauge(risco);

    // 3. Atualizar Cards das Esteiras (Req. 17)
    if (data.esteiras) {
      atualizarCardsEsteiras(data.esteiras);
      atualizarTabelaEsteiras(data.esteiras);
      atualizarAnimacaoPrincipal(data.esteiras);
    }

    // 4. Atualizar Tabela de Eventos (Req. 20)
    if (data.ultimosEventos) {
      atualizarTabelaEventos(data.ultimosEventos);
    }

    // 5. Atualizar Alertas
    if (data.ultimosAlertas) {
      atualizarAlertas(data.ultimosAlertas);
    }

  } catch (err) {
    console.warn('Erro ao carregar dados do dashboard:', err);
  }
}

function atualizarCardsEsteiras(esteiras) {
  const container = document.getElementById('esteirasCardsContainer');
  if (!container) return;

  if (esteiras.length === 0) {
    container.innerHTML = '<div style="padding:20px;text-align:center;color:#64748B;font-size:13px;">Nenhuma esteira cadastrada.</div>';
    return;
  }

  let html = '';
  esteiras.forEach(e => {
    const estado = e.estado || 'STOPPED';
    let chipClass = 'chip-stop';
    let statusLabel = '🔴 Parada';
    let cardBorder = 'stopped';

    if (estado === 'RUNNING') {
      chipClass = 'chip-ok';
      statusLabel = '🟢 Funcionando';
      cardBorder = 'running';
    } else if (estado === 'OFFLINE') {
      chipClass = 'chip-off';
      statusLabel = '⚫ Offline';
      cardBorder = 'offline';
    } else if (estado === 'MAINTENANCE') {
      chipClass = 'chip-maint';
      statusLabel = '🟡 Manutenção';
      cardBorder = 'maintenance';
    }

    const tempoLabel = estado === 'STOPPED' ? 'Parada há:' : (estado === 'RUNNING' ? 'Operando há:' : 'Tempo:');
    const tempoVal = estado === 'STOPPED' ? (e.tempoParadoFormatado || '00:00:00') : (e.tempoFuncionandoFormatado || '00:00:00');

    html += `
      <div class="esteira-card ${cardBorder}">
        <div class="ec-head">
          <div>
            <div class="ec-title">${escapeHtml(e.nome)}</div>
            <div class="ec-loc">${escapeHtml(e.localizacao)}</div>
          </div>
          <div>
            <span class="chip ${chipClass}">${statusLabel}</span>
          </div>
        </div>
        <div class="ec-grid">
          <div class="ec-item">
            <span class="ec-item-lbl">Sensor</span>
            <span class="ec-item-val">${escapeHtml(e.sensorNome || '—')}</span>
          </div>
          <div class="ec-item">
            <span class="ec-item-lbl">LDR Atual</span>
            <span class="ec-item-val">${e.sensorValorLdr ?? '—'}</span>
          </div>
          <div class="ec-item">
            <span class="ec-item-lbl">Último Pulso</span>
            <span class="ec-item-val">${escapeHtml(e.ultimoPulsoTexto || '—')}</span>
          </div>
          <div class="ec-item">
            <span class="ec-item-lbl">Última Com.</span>
            <span class="ec-item-val">${escapeHtml(e.ultimaComunicacaoTexto || '—')}</span>
          </div>
          <div class="ec-item" style="grid-column: 1 / -1;">
            <span class="ec-item-lbl">${tempoLabel}</span>
            <span class="ec-item-val">${tempoVal}</span>
          </div>
        </div>
        <div style="margin-top:10px;text-align:right;">
          <a href="/esteiras/detalhe/${e.id}" style="font-size:12px;color:#2563EB;text-decoration:none;font-weight:600;">Ver detalhes →</a>
        </div>
      </div>
    `;
  });

  container.innerHTML = html;
}

function atualizarTabelaEsteiras(esteiras) {
  const tbody = document.getElementById('tbEsteirasBody');
  if (!tbody) return;

  if (esteiras.length === 0) {
    tbody.innerHTML = '<tr><td colspan="7" style="text-align:center;padding:16px;color:#64748B;">Nenhuma esteira monitorada.</td></tr>';
    return;
  }

  let html = '';
  esteiras.forEach(e => {
    let chip = '<span class="chip chip-stop">🔴 Parada</span>';
    if (e.estado === 'RUNNING') chip = '<span class="chip chip-ok">🟢 Funcionando</span>';
    else if (e.estado === 'OFFLINE') chip = '<span class="chip chip-off">⚫ Offline</span>';
    else if (e.estado === 'MAINTENANCE') chip = '<span class="chip chip-maint">🟡 Manutenção</span>';

    const laserTxt = e.sensorLaser
      ? '<span style="color:#059669;font-weight:600;">Detectado</span>'
      : '<span style="color:#DC2626;font-weight:600;">Interrompido</span>';

    html += `
      <tr>
        <td style="font-weight:600;">${escapeHtml(e.nome)}</td>
        <td>${escapeHtml(e.sensorNome || '—')}</td>
        <td>${chip}</td>
        <td style="text-align:right;font-weight:700;">${e.sensorValorLdr ?? '—'}</td>
        <td>${laserTxt}</td>
        <td>${escapeHtml(e.ultimoPulsoTexto || '—')}</td>
        <td>${escapeHtml(e.ultimaComunicacaoTexto || '—')}</td>
      </tr>
    `;
  });

  tbody.innerHTML = html;
}

function atualizarAnimacaoPrincipal(esteiras) {
  const btrack = document.getElementById('mainBeltTrack');
  const chip   = document.getElementById('mainBeltStatusChip');
  const sub    = document.getElementById('beltSubText');
  const beam   = document.getElementById('laserBeamStatus');

  const algumRodando = esteiras.some(e => e.estado === 'RUNNING');
  const algumComLaser = esteiras.some(e => e.sensorLaser);

  if (btrack) {
    if (algumRodando) btrack.classList.remove('halt');
    else btrack.classList.add('halt');
  }

  if (chip) {
    if (algumRodando) {
      chip.className = 'chip chip-ok';
      chip.textContent = '🟢 Operando';
    } else {
      chip.className = 'chip chip-stop';
      chip.textContent = '🔴 Parada';
    }
  }

  if (sub && esteiras.length > 0) {
    const e = esteiras[0];
    sub.textContent = `${e.nome} (${e.estadoDescricao || e.estado})`;
  }

  if (beam) {
    beam.textContent = algumComLaser ? 'Laser: Feixe Detectado' : 'Laser: Feixe Interrompido';
  }
}

function atualizarTabelaEventos(eventos) {
  const tbody = document.getElementById('tbEventosBody');
  const count = document.getElementById('eventosCountBadge');
  if (count) count.textContent = `${eventos.length} registros`;
  if (!tbody) return;

  if (eventos.length === 0) {
    tbody.innerHTML = '<tr><td colspan="6" style="text-align:center;padding:16px;color:#64748B;">Nenhum evento registrado no banco.</td></tr>';
    return;
  }

  let html = '';
  eventos.forEach(ev => {
    const dt = ev.dataHoraInicio ? new Date(ev.dataHoraInicio).toLocaleTimeString('pt-BR') : '—';
    const duracao = ev.duracaoFormatada || '—';

    html += `
      <tr>
        <td>${dt}</td>
        <td style="font-weight:600;">${escapeHtml(ev.esteiraNome || '—')}</td>
        <td><span style="font-weight:600;color:#2563EB;">${escapeHtml(ev.tipoEvento || '—')}</span></td>
        <td>${escapeHtml(ev.estadoAnterior || '—')}</td>
        <td>${escapeHtml(ev.novoEstado || '—')}</td>
        <td>${escapeHtml(duracao)}</td>
      </tr>
    `;
  });

  tbody.innerHTML = html;
}

function atualizarAlertas(alertas) {
  const tbody = document.getElementById('tbAlertasBody');
  const ribbon = document.getElementById('ribbon');
  const ribbonMsg = document.getElementById('ribbonMsg');
  if (!tbody) return;

  if (alertas.length === 0) {
    tbody.innerHTML = '<tr><td style="text-align:center;padding:16px;color:#64748B;">Nenhum alerta ativo.</td></tr>';
    if (ribbon) ribbon.classList.remove('on');
    return;
  }

  // Verifica se o mais recente é crítico
  const maisRecente = alertas[0];
  if (ribbon && ribbonMsg && maisRecente.nivelRisco === 'CRITICO') {
    ribbonMsg.textContent = maisRecente.mensagem;
    ribbon.classList.add('on');
  } else if (ribbon) {
    ribbon.classList.remove('on');
  }

  let html = '';
  alertas.forEach(a => {
    const hora = a.dataHora ? new Date(a.dataHora).toLocaleTimeString('pt-BR') : '—';
    const cor = a.nivelRisco === 'CRITICO' ? '#DC2626' : (a.nivelRisco === 'ATENCAO' ? '#D97706' : '#059669');
    html += `
      <tr>
        <td style="white-space:nowrap;color:#64748B;">${hora}</td>
        <td style="color:${cor};font-weight:500;">${escapeHtml(a.mensagem)}</td>
      </tr>
    `;
  });

  tbody.innerHTML = html;
}

// ── GAUGE ─────────────────────────────────────────────────────────
function setGauge(v) {
  const circ = 2 * Math.PI * 60;
  const arc  = document.getElementById('garc');
  if (!arc) return;
  arc.style.strokeDashoffset = circ - (v / 100) * circ;
  setEl('gnum', Math.round(v));
  if (v >= 70) {
    arc.style.stroke = '#EF4444';
  } else if (v >= 35) {
    arc.style.stroke = '#F59E0B';
  } else {
    arc.style.stroke = '#2563EB';
  }
}

// ── GRÁFICO HISTÓRICO LDR × TEMPO (Req. 21) ───────────────────────
async function carregarGraficoLdr() {
  try {
    const res = await fetch(`/api/dashboard/grafico-ldr?periodo=${periodoAtual}`, { credentials: 'same-origin' });
    if (!res.ok) return;
    ldrHistorico = await res.json();
    desenharGraficoLdr();
  } catch (err) {
    console.warn('Erro ao buscar dados do gráfico LDR:', err);
  }
}

function mudarPeriodoGrafico(p, btn) {
  periodoAtual = p;
  document.querySelectorAll('.btn-period').forEach(b => b.classList.remove('active'));
  if (btn) btn.classList.add('active');
  carregarGraficoLdr();
}

function desenharGraficoLdr() {
  const cvs = document.getElementById('mc');
  if (!cvs) return;
  const ctx = cvs.getContext('2d');
  const w = cvs.offsetWidth || 260;
  const h = 100;
  cvs.width = w;
  cvs.height = h;
  ctx.clearRect(0, 0, w, h);

  if (!ldrHistorico || ldrHistorico.length === 0) {
    ctx.fillStyle = '#64748B';
    ctx.font = '12px IBM Plex Mono, monospace';
    ctx.textAlign = 'center';
    ctx.fillText('Aguardando telemetria...', w / 2, h / 2);
    return;
  }

  const valores = ldrHistorico.map(d => d.valorLdr ?? 0);
  const min = Math.min(...valores);
  const max = Math.max(...valores);
  const cur = valores[valores.length - 1];

  setEl('ldrMin', min);
  setEl('ldrMax', max);
  setEl('ldrCur', cur);

  // Grade de fundo
  [0.25, 0.5, 0.75].forEach(y => {
    ctx.beginPath();
    ctx.strokeStyle = '#E8EDF5';
    ctx.lineWidth = 1;
    ctx.moveTo(0, y * h);
    ctx.lineTo(w, y * h);
    ctx.stroke();
  });

  const range = (max - min) > 0 ? (max - min) : 4095;
  const escalaMin = min;

  // Curva de LDR
  ctx.beginPath();
  valores.forEach((v, i) => {
    const x = (i / (valores.length - 1 || 1)) * w;
    const y = h - ((v - escalaMin) / range) * (h - 20) - 10;
    if (i === 0) ctx.moveTo(x, y);
    else ctx.lineTo(x, y);
  });

  ctx.strokeStyle = '#2563EB';
  ctx.lineWidth = 2;
  ctx.stroke();

  // Preenchimento gradiente
  ctx.lineTo(w, h);
  ctx.lineTo(0, h);
  ctx.closePath();
  const grad = ctx.createLinearGradient(0, 0, 0, h);
  grad.addColorStop(0, 'rgba(37,99,235,0.2)');
  grad.addColorStop(1, 'rgba(37,99,235,0.0)');
  ctx.fillStyle = grad;
  ctx.fill();
}

function setEl(id, val) {
  const el = document.getElementById(id);
  if (el) el.textContent = val;
}

function escapeHtml(text) {
  if (text === null || text === undefined) return '';
  return String(text)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#039;');
}

// ── INICIALIZAÇÃO E POLLING (Req. 22) ─────────────────────────────
document.addEventListener('DOMContentLoaded', async () => {
  await carregarDashboard();
  await carregarGraficoLdr();

  // Polling em tempo real a cada 2 segundos
  setInterval(async () => {
    await carregarDashboard();
    await carregarGraficoLdr();
  }, 2000);
});