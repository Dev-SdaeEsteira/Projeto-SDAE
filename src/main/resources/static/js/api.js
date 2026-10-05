/**
 * SDAE — Módulo central de API e sessão (alinhado ao back-end Spring Boot).
 */

const API_BASE = `${window.location.origin}/api`;

function escapeHtml(value) {
  return String(value ?? '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}

const ROTAS_PUBLICAS = ['/login', '/cadastro', '/welcome', '/'];

function caminhoPublico(path) {
  if (!path) return true;
  if (ROTAS_PUBLICAS.includes(path)) return true;
  return ROTAS_PUBLICAS.some(p => p !== '/' && path.startsWith(p + '/'));
}

// ─── Sessão (espelha o usuário autenticado no Spring Security) ────────────
const Sessao = {
  salvar(nome, nivel, login) {
    sessionStorage.setItem('sdae_nome', nome || 'Usuário');
    sessionStorage.setItem('sdae_nivel', nivel || 'OPERADOR');
    sessionStorage.setItem('sdae_login', login || '');
  },
  nome()   { return sessionStorage.getItem('sdae_nome')  || 'Usuário'; },
  nivel()  { return sessionStorage.getItem('sdae_nivel') || 'OPERADOR'; },
  login()  { return sessionStorage.getItem('sdae_login') || ''; },
  logado() { return !!sessionStorage.getItem('sdae_login'); },
  limpar() {
    ['sdae_nome', 'sdae_nivel', 'sdae_login', 'saeb_nome', 'saeb_nivel', 'saeb_login'].forEach(k =>
      sessionStorage.removeItem(k)
    );
  },
  sair() {
    this.limpar();
    const form = document.createElement('form');
    form.method = 'POST';
    form.action = '/logout';
    const csrfToken = (document.cookie.match(/XSRF-TOKEN=([^;]+)/)?.[1])
      || document.querySelector('meta[name="_csrf"]')?.content;
    const csrfParam = document.querySelector('meta[name="_csrf_parameter"]')?.content || '_csrf';
    if (csrfToken) {
      const input = document.createElement('input');
      input.type = 'hidden';
      input.name = csrfParam;
      input.value = decodeURIComponent(csrfToken);
      form.appendChild(input);
    }
    document.body.appendChild(form);
    form.submit();
  }
};

// Compatibilidade com chaves antigas (saeb_*)
(function migrarChavesAntigas() {
  if (!sessionStorage.getItem('sdae_login') && sessionStorage.getItem('saeb_login')) {
    Sessao.salvar(
      sessionStorage.getItem('saeb_nome'),
      sessionStorage.getItem('saeb_nivel'),
      sessionStorage.getItem('saeb_login')
    );
  }
})();

async function sincronizarSessao() {
  const path = window.location.pathname || '';
  if (caminhoPublico(path)) return true;

  try {
    const data = await apiFetch('/auth/me');
    Sessao.salvar(data.nome, data.nivel, data.login);
    return true;
  } catch {
    Sessao.limpar();
    const destino = encodeURIComponent(path + window.location.search);
    window.location.replace('/login?next=' + destino);
    return false;
  }
}

window.sdaeSessaoPronta = sincronizarSessao();

function verificarSessao() {
  const nome = Sessao.nome();
  document.querySelectorAll('#adminName, .admin-badge span, .header-user-name').forEach(el => {
    if (el) el.textContent = nome;
  });
  const loginEl = document.querySelector('[data-sdae-login]');
  if (loginEl) loginEl.textContent = Sessao.login();
  return true;
}

function getCsrfToken() {
  const match = document.cookie.match(new RegExp('(^| )XSRF-TOKEN=([^;]+)'));
  if (match) return decodeURIComponent(match[2]);
  const meta = document.querySelector('meta[name="_csrf"]');
  if (meta && meta.content) return meta.content;
  const input = document.querySelector('input[name="_csrf"]');
  if (input && input.value) return input.value;
  return null;
}

// ─── Fetch central (envia cookie de sessão e token CSRF) ───────────────────
async function apiFetch(url, options = {}) {
  const method = (options.method || 'GET').toUpperCase();
  const headers = { 'Content-Type': 'application/json', ...options.headers };
  if (method !== 'GET') {
    const csrf = getCsrfToken();
    if (csrf) {
      headers['X-XSRF-TOKEN'] = csrf;
      headers['X-CSRF-TOKEN'] = csrf;
    }
  }

    const resp = await fetch(API_BASE + url, {
    credentials: 'same-origin',
    ...options,
    headers,
  });

  if (resp.status === 401) {
    const err = await resp.json().catch(() => ({ erro: 'Não autenticado' }));
    if (!url.includes('/auth/me')) {
      Sessao.limpar();
      const path = window.location.pathname || '/dashboard';
      window.location.replace('/login?next=' + encodeURIComponent(path + window.location.search));
    }
    throw new Error(err.erro || 'Não autenticado');
  }

  if (!resp.ok) {
    const err = await resp.json().catch(() => ({ erro: resp.statusText }));
    throw new Error(err.erro || 'Erro na requisição');
  }

  if (resp.status === 204) return null;
  const ct = resp.headers.get('content-type') || '';
  if (ct.includes('application/json')) return resp.json();
  return null;
}

// ─── Autenticação ─────────────────────────────────────────────────────────
const Auth = {
  /** Login REST com sessão e suporte a CSRF. */
  async login(login, senha) {
    const headers = { 'Content-Type': 'application/json' };
    const csrf = getCsrfToken();
    if (csrf) headers['X-XSRF-TOKEN'] = csrf;

    const resp = await fetch(API_BASE + '/auth/login', {
      method: 'POST',
      credentials: 'same-origin',
      headers,
      body: JSON.stringify({ login, senha })
    });
    const data = await resp.json();
    if (!resp.ok) throw new Error(data.erro || 'Falha no login');
    Sessao.salvar(data.nome, data.nivel, data.login);
    return data;
  },
  async me() {
    return apiFetch('/auth/me');
  },
  sair() { Sessao.sair(); }
};

// ─── Esteiras (modelo: nome, localizacao, ativa) ──────────────────────────
const Esteiras = {
  listar:    ()           => apiFetch('/esteiras'),
  buscar:    (id)         => apiFetch(`/esteiras/${id}`),
  criar:    (d)           => apiFetch('/esteiras', { method: 'POST', body: JSON.stringify(d) }),
  atualizar:(id, d)       => apiFetch(`/esteiras/${id}`, { method: 'PUT', body: JSON.stringify(d) }),
  /** status: 'ATIVA' | 'INATIVA' (PATCH /status) */
  status:   (id, status) => apiFetch(`/esteiras/${id}/status`, {
    method: 'PATCH',
    body: JSON.stringify({ status })
  }),
  deletar:  (id)         => apiFetch(`/esteiras/${id}`, { method: 'DELETE' }),
  ativar:   (id)         => Esteiras.status(id, 'ATIVA'),
  desativar:(id)         => Esteiras.status(id, 'INATIVA'),
};

// ─── Sensores (modelo: modelo, leituraAtual, nivelRisco, esteiraId) ───────
const Sensores = {
  listar:        ()              => apiFetch('/sensores'),
  buscar:        (id)            => apiFetch(`/sensores/${id}`),
  porEsteira:    (eId)           => apiFetch(`/sensores/esteira/${eId}`),
  criar:         (d, eId)        => apiFetch(`/sensores?esteiraId=${eId}`, {
    method: 'POST',
    body: JSON.stringify(d)
  }),
  atualizar:     (id, d, eId)    => apiFetch(`/sensores/${id}?esteiraId=${eId}`, {
    method: 'PUT',
    body: JSON.stringify(d)
  }),
  deletar:       (id)            => apiFetch(`/sensores/${id}`, { method: 'DELETE' }),
  enviarLeitura: (id, payload)   => apiFetch(`/sensores/${id}/leitura`, {
    method: 'POST',
    body: JSON.stringify(payload)
  }),
  historico:     (id, qtd = 30)  => apiFetch(`/sensores/${id}/historico?qtd=${qtd}`),
};

// ─── Funcionários ─────────────────────────────────────────────────────────
const Funcionarios = {
  listar:   ()       => apiFetch('/funcionarios'),
  buscar:   (id)     => apiFetch(`/funcionarios/${id}`),
  criar:    (d)      => apiFetch('/funcionarios', { method: 'POST', body: JSON.stringify(d) }),
  atualizar:(id, d)  => apiFetch(`/funcionarios/${id}`, { method: 'PUT', body: JSON.stringify(d) }),
  deletar:  (id)     => apiFetch(`/funcionarios/${id}`, { method: 'DELETE' }),
};

// ─── Dashboard ────────────────────────────────────────────────────────────
const Dashboard = { stats: () => apiFetch('/dashboard') };

// ─── Controles de esteira (mapeados para PATCH /esteiras/{id}/status) ─────
const Sistema = {
  paradaEmergencia: (eId) => Esteiras.desativar(eId),
  reset:            (eId) => Esteiras.ativar(eId),
  pistao:           async () => null,
};

// ─── Eventos (ainda sem REST no servidor) ─────────────────────────────────
const Eventos = {
  async listar() {
    console.warn('[SDAE] GET /api/eventos não implementado no servidor.');
    return [];
  },
  baixarCsv() {
    showToast('Indisponível', 'Exportação CSV de eventos ainda não existe no back-end.', true);
  }
};

// ─── Utilitários de domínio ───────────────────────────────────────────────
function labelEsteiraAtiva(ativa) {
  return ativa === true || ativa === 'true' ? 'Ativa' : 'Inativa';
}

function chipEsteiraAtiva(ativa) {
  return ativa === true || ativa === 'true' ? 'chip-ok' : 'chip-maint';
}

function chipNivelRisco(nivel) {
  const n = (nivel || '').toUpperCase();
  if (n === 'CRITICO') return 'chip-alert';
  if (n === 'ATENCAO') return 'chip-warn';
  return 'chip-ok';
}

function labelNivelRisco(nivel) {
  const n = (nivel || '').toUpperCase();
  if (n === 'CRITICO') return 'Crítico';
  if (n === 'ATENCAO') return 'Atenção';
  return 'Normal';
}

// ─── Toast ────────────────────────────────────────────────────────────────
function showToast(titulo, msg, danger = false) {
  const t = document.getElementById('toast');
  if (!t) return;
  const titleEl = document.getElementById('tTitle');
  const msgEl   = document.getElementById('tMsg');
  if (titleEl) titleEl.textContent = titulo;
  if (msgEl)   msgEl.textContent   = msg;
  const ts = document.getElementById('tTs');
  if (ts) ts.textContent = new Date().toTimeString().slice(0, 8);
  t.className = 'toast on' + (danger ? ' danger' : '');
  setTimeout(() => { t.className = 'toast' + (danger ? ' danger' : ''); }, 5500);
}

// ─── Relógio ──────────────────────────────────────────────────────────────
function initClock() {
  function tick() {
    const t = new Date().toTimeString().slice(0, 8);
    document.querySelectorAll('#clk,#rdTime').forEach(el => el && (el.textContent = t));
    const fd = document.getElementById('fdate');
    if (fd) {
      fd.textContent = new Date().toLocaleDateString('pt-BR', {
        weekday: 'long', day: '2-digit', month: 'long', year: 'numeric'
      });
    }
  }
  setInterval(tick, 1000);
  tick();
}

// ─── Init ─────────────────────────────────────────────────────────────────
document.addEventListener('DOMContentLoaded', async () => {
  sessionStorage.removeItem('sdae_token');
  sessionStorage.removeItem('saeb_token');
  await window.sdaeSessaoPronta;
  initClock();
  if (!window.location.pathname.includes('login')) {
    verificarSessao();
  }
});
