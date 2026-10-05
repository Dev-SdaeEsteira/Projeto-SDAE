/**
 * SAEB — Página de Cadastro de Funcionários
 * Integra o formulário HTML com o back-end Spring Boot.
 */

// Mapeamento IDs do HTML para a API
const FCAMPO = {
  nome:       'fNome',
  matricula:  'fMatricula',
  email:      'fEmail',
  telefone:   'fTelefone',
  cargo:      'fCargo',
  depto:      'fDepto',
  nivel:      'fNivel',
  obs:        'fObs',
};

// Converte nível de acesso do HTML → enum Java
const NIVEL_MAP = {
  'super':  'ADMINISTRADOR',
  'admin':  'ADMINISTRADOR',
  'oper':   'OPERADOR',
  'viewer': 'OPERADOR',
  'Engenheiro de Automação':  'ENGENHEIRO',
  'Técnico de Manutenção':    'TECNICO',
  'Operador de Esteiras':     'OPERADOR',
  'Supervisor de Produção':   'ENGENHEIRO',
  'Analista de Sistemas':     'TECNICO',
  'Gerente de Operações':     'ADMINISTRADOR',
  'ADMINISTRADOR': 'ADMINISTRADOR',
  'ENGENHEIRO':    'ENGENHEIRO',
  'TECNICO':       'TECNICO',
  'OPERADOR':      'OPERADOR',
};

// Converte cargo/departamento do HTML → enum Turno (inferido pelo cargo)
function inferirTurno(cargo) {
  const c = (cargo || '').toLowerCase();
  if (c.includes('gerente') || c.includes('supervisor')) return 'INTEGRAL';
  return 'MANHA';
}

// ── Força de senha (mantém a animação original) ───────────────────
function checkStrength(pw) {
  const fill  = document.getElementById('pwFill');
  const label = document.getElementById('pwLabel');
  if (!fill || !label) return;
  let score = 0;
  if (pw.length >= 8) score++;
  if (/[A-Z]/.test(pw)) score++;
  if (/[0-9]/.test(pw)) score++;
  if (/[^A-Za-z0-9]/.test(pw)) score++;
  const configs = [
    { w: '0%',   color: 'transparent', txt: '—' },
    { w: '25%',  color: 'var(--ruby)',   txt: 'Fraca' },
    { w: '50%',  color: 'var(--amber)',  txt: 'Média' },
    { w: '75%',  color: 'var(--blue-400)', txt: 'Boa' },
    { w: '100%', color: 'var(--emerald)', txt: 'Forte' },
  ];
  const c = configs[score];
  fill.style.width = c.w;
  fill.style.background = c.color;
  label.textContent = c.txt;
  label.style.color = c.color;
}

function togglePw(id, btn) {
  const el = document.getElementById(id);
  if (!el) return;
  const show = el.type === 'password';
  el.type  = show ? 'text' : 'password';
  btn.textContent = show ? 'OCULTAR' : 'MOSTRAR';
}

function togglePerm(el) {
  el.classList.toggle('selected');
  const check = el.querySelector('.perm-check');
  if (check) check.textContent = el.classList.contains('selected') ? '✔' : '';
}

// ── Preview ───────────────────────────────────────────────────────
function updatePreview() {
  const nome  = val(FCAMPO.nome)  || 'Nome não informado';
  const cargo = val(FCAMPO.cargo) || 'Cargo não selecionado';
  const nivel = val(FCAMPO.nivel);
  setText('prevNome',  nome);
  setText('prevCargo', cargo);

  // Badge de nível
  const nivMap = {
    'super':'ADMIN', 'admin':'ADMIN', 'oper':'OPERADOR', 'viewer':'VIEWER',
    '': '—'
  };
  const nb = document.getElementById('prevNivel');
  if (nb) {
    nb.textContent = nivMap[nivel] ?? nivel.toUpperCase() ?? '—';
    nb.className = 'chip ' + (
      nivel === 'super' || nivel === 'admin' ? 'chip-alert' :
      nivel === 'oper'  ? 'chip-ok' : ''
    );
  }
}

// ── Validação ─────────────────────────────────────────────────────
function validate() {
  const campos = [
    { id: FCAMPO.nome,      err: 'err-nome'  },
    { id: FCAMPO.matricula, err: 'err-mat'   },
    { id: FCAMPO.cargo,     err: 'err-cargo' },
    { id: FCAMPO.nivel,     err: 'err-nivel' },
  ];
  let ok = true;
  campos.forEach(({ id, err }) => {
    const el    = document.getElementById(id);
    const errEl = document.getElementById(err);
    const valid = el && el.value.trim() !== '';
    if (el) el.className = (el.tagName === 'SELECT' ? 'form-select' : 'form-input') + (valid ? ' ok' : ' error');
    if (errEl) errEl.className = 'field-error' + (valid ? '' : ' show');
    if (!valid) ok = false;
  });
  return ok;
}

// ── Submissão ─────────────────────────────────────────────────────
async function submitForm() {
  if (!validate()) {
    showToast('Erro de validação', 'Preencha os campos obrigatórios.', true);
    return;
  }

  const editId = document.getElementById('funcIdOculto')?.value;
  const cargo  = val(FCAMPO.cargo);
  const nivel  = val(FCAMPO.nivel);

  const dados = {
    nome:        val(FCAMPO.nome),
    matricula:   val(FCAMPO.matricula).toUpperCase(),
    cargo:       cargo,
    departamento:val(FCAMPO.depto) || 'Geral',
    email:       val(FCAMPO.email)    || null,
    telefone:    val(FCAMPO.telefone) || null,
    turno:       inferirTurno(cargo),
    nivelAcesso: NIVEL_MAP[nivel] ?? NIVEL_MAP[cargo] ?? 'OPERADOR',
  };
  const senha = val('fSenha');
  if (senha) dados.senha = senha;

  try {
    if (editId) {
      await Funcionarios.atualizar(editId, dados);
      showToast('Atualizado ✔', `Funcionário ${dados.nome} atualizado com sucesso.`);
    } else {
      if (!dados.senha) {
        showToast('Validação', 'Informe uma senha para o novo usuário.', true);
        return;
      }
      await Funcionarios.criar(dados);
      showToast('Cadastrado ✔', `Funcionário ${dados.nome} cadastrado no SDAE.`);
    }
    clearForm();
    await carregarTabela();
  } catch (e) {
    showToast('Erro', e.message, true);
  }
}

// ── Limpar ────────────────────────────────────────────────────────
function clearForm() {
  Object.values(FCAMPO).forEach(id => {
    const el = document.getElementById(id);
    if (!el) return;
    el.value = '';
    el.className = el.tagName === 'SELECT' ? 'form-select' : 'form-input';
  });
  ['fUser','fSenha','fSenha2'].forEach(id => {
    const el = document.getElementById(id);
    if (el) { el.value = ''; el.className = 'form-input'; }
  });
  document.querySelectorAll('.field-error').forEach(e => e.className = 'field-error');
  const oculto = document.getElementById('funcIdOculto');
  if (oculto) oculto.value = '';
  const fs = document.getElementById('formStatus');
  if (fs) { fs.textContent = 'Preenchendo'; fs.className = 'chip chip-pend'; }
  const titulo = document.querySelector('.card-title');
  if (titulo) titulo.textContent = 'Dados Pessoais';
  checkStrength('');
  updatePreview();
}

// ── Tabela ────────────────────────────────────────────────────────
async function carregarTabela() {
  try {
    const lista = await Funcionarios.listar();
    const tbody = document.getElementById('funcBody') ?? document.querySelector('tbody');
    if (!tbody) return;
    tbody.innerHTML = '';
    lista.forEach(f => {
      const nivelCls = {
        ADMINISTRADOR: 'chip-alert',
        ENGENHEIRO:    'chip-ok',
        TECNICO:       'chip-maint',
        OPERADOR:      '',
      }[f.nivelAcesso] ?? '';
      const tr = document.createElement('tr');
      tr.className = 'new';
      const nomeEsc = String(f.nome ?? '').replace(/\\/g, '\\\\').replace(/'/g, "\\'");
      tr.innerHTML = `
        <td class="mono" style="color:var(--blue-300)">${escapeHtml(f.matricula)}</td>
        <td>${escapeHtml(f.nome)}</td>
        <td>${escapeHtml(f.cargo)}</td>
        <td style="font-size:11px;color:var(--text-400)">${escapeHtml(f.departamento)}</td>
        <td style="font-size:11px;color:var(--text-400)">${escapeHtml(f.email ?? '—')}</td>
        <td>${escapeHtml(f.turno)}</td>
        <td><span class="chip ${nivelCls}" style="font-size:10px">${escapeHtml(f.nivelAcesso)}</span></td>
        <td>
          <button class="act-icon" onclick="editarFuncionario(${f.id})">✎</button>
          <button class="act-icon danger" onclick="deletarFuncionario(${f.id},'${nomeEsc}')">✕</button>
        </td>`;
      tbody.appendChild(tr);
    });
    setText('kTotal', lista.length);
  } catch (e) {
    console.warn('Tabela funcionários: API offline.');
  }
}

// ── Editar ────────────────────────────────────────────────────────
async function editarFuncionario(id) {
  try {
    const f = await Funcionarios.buscar(id);
    document.getElementById('funcIdOculto').value = f.id;
    setVal(FCAMPO.nome,      f.nome);
    setVal(FCAMPO.matricula, f.matricula);
    setVal(FCAMPO.email,     f.email      ?? '');
    setVal(FCAMPO.telefone,  f.telefone   ?? '');
    setVal(FCAMPO.cargo,     f.cargo      ?? '');
    setVal(FCAMPO.depto,     f.departamento ?? '');
    // Nível: converte API → valor do select HTML
    const nivelInv = {
      ADMINISTRADOR: 'super',
      ENGENHEIRO:    'admin',
      TECNICO:       'oper',
      OPERADOR:      'oper',
    };
    setVal(FCAMPO.nivel, nivelInv[f.nivelAcesso] ?? 'oper');
    const titulo = document.querySelector('.card-title');
    if (titulo) titulo.textContent = 'Dados Pessoais';
    const fs = document.getElementById('formStatus');
    if (fs) { fs.textContent = 'Editando'; fs.className = 'chip chip-warn'; }
    updatePreview();
    window.scrollTo({ top: 0, behavior: 'smooth' });
  } catch (err) {
    showToast('Erro', err.message, true);
  }
}

async function deletarFuncionario(id, nome) {
  if (!confirm(`Deseja excluir o funcionário "${nome}"?`)) return;
  try {
    await Funcionarios.deletar(id);
    showToast('Removido', `Funcionário "${nome}" removido do sistema.`);
    await carregarTabela();
  } catch (e) {
    showToast('Erro', e.message, true);
  }
}

// ── Helpers ───────────────────────────────────────────────────────
function val(id)       { return document.getElementById(id)?.value ?? ''; }
function setVal(id, v) { const el = document.getElementById(id); if (el) el.value = v; }
function setText(id,v) { const el = document.getElementById(id); if (el) el.textContent = v; }

// ── Inicialização ─────────────────────────────────────────────────
document.addEventListener('DOMContentLoaded', async () => {
  if (window.sdaeSessaoPronta) await window.sdaeSessaoPronta;
  // Campo oculto para ID de edição
  const body = document.querySelector('.card-body');
  if (body && !document.getElementById('funcIdOculto')) {
    const h = document.createElement('input');
    h.type = 'hidden'; h.id = 'funcIdOculto';
    body.prepend(h);
  }
  // Preview ao digitar
  document.getElementById(FCAMPO.nome)?.addEventListener('input', updatePreview);
  document.getElementById(FCAMPO.cargo)?.addEventListener('change', updatePreview);
  document.getElementById(FCAMPO.nivel)?.addEventListener('change', updatePreview);
  // Força de senha
  document.getElementById('fSenha')?.addEventListener('input', e => checkStrength(e.target.value));

  await carregarTabela();
});
