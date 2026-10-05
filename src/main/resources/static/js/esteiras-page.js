/**
 * SDAE — Página de esteiras (REST: nome, localizacao, ativa).
 */

const CAMPO = {
  nome:        'fNome',
  localizacao: 'fPatio',
};

let esteiraAtiva = true;

function selectStatus(ativa) {
  esteiraAtiva = ativa;
  document.querySelectorAll('.status-opt').forEach(el => el.classList.remove('selected'));
  const key = ativa ? 'ok' : 'maint';
  document.getElementById('st-' + key)?.classList.add(ativa ? 'sel-ok' : 'sel-maint');
  const prev = document.getElementById('prevStatus');
  if (prev) {
    prev.className = 'chip ' + chipEsteiraAtiva(ativa);
    prev.textContent = labelEsteiraAtiva(ativa);
  }
}

function updatePreview() {
  const nome = val(CAMPO.nome) || 'Nome não definido';
  const loc  = val(CAMPO.localizacao) || 'Localização não informada';
  setText('prevCodigo', '#' + (document.getElementById('esteiraIdOculto')?.value || 'novo'));
  setText('prevNome', nome);
  setText('prevPatio', loc);
}

function validate() {
  const campos = [
    { id: CAMPO.nome,        err: 'err-nome'  },
    { id: CAMPO.localizacao, err: 'err-patio' },
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

async function submitForm() {
  if (!validate()) {
    showToast('Erro de validação', 'Informe nome e localização.', true);
    return;
  }

  const editId = document.getElementById('esteiraIdOculto')?.value;
  const dados = {
    nome:        val(CAMPO.nome).trim(),
    localizacao: val(CAMPO.localizacao).trim(),
    ativa:       esteiraAtiva,
  };

  try {
    if (editId) {
      await Esteiras.atualizar(editId, dados);
      showToast('Atualizado ✔', `Esteira "${dados.nome}" atualizada.`);
    } else {
      await Esteiras.criar(dados);
      showToast('Cadastrado ✔', `Esteira "${dados.nome}" cadastrada.`);
    }
    clearForm();
    await carregarTabela();
    await carregarMiniLista();
  } catch (e) {
    showToast('Erro', e.message, true);
  }
}

function clearForm() {
  [CAMPO.nome, CAMPO.localizacao].forEach(id => {
    const el = document.getElementById(id);
    if (!el) return;
    el.value = '';
    el.className = el.tagName === 'SELECT' ? 'form-select' : 'form-input';
  });
  document.querySelectorAll('.field-error').forEach(e => e.className = 'field-error');
  const oculto = document.getElementById('esteiraIdOculto');
  if (oculto) oculto.value = '';
  selectStatus(true);
  updatePreview();
}

async function carregarTabela() {
  const tbody = document.getElementById('beltBody');
  if (!tbody) return;
  try {
    const lista = await Esteiras.listar();
    tbody.innerHTML = '';
    lista.forEach(e => {
      const tr = document.createElement('tr');
      tr.className = 'new';
      const nomeEsc = String(e.nome).replace(/\\/g, '\\\\').replace(/'/g, "\\'");
      tr.innerHTML = `
        <td class="mono" style="color:var(--blue-300)">#${escapeHtml(e.id)}</td>
        <td>${escapeHtml(e.nome)}</td>
        <td style="font-size:11px;color:var(--text-400)">${escapeHtml(e.localizacao ?? '—')}</td>
        <td><span class="chip ${chipEsteiraAtiva(e.ativa)}">${escapeHtml(labelEsteiraAtiva(e.ativa))}</span></td>
        <td>
          <button class="act-icon" onclick="editarEsteira(${e.id})">✎</button>
          <button class="act-icon danger" onclick="deletarEsteira(${e.id},'${nomeEsc}')">✕</button>
        </td>`;
      tbody.appendChild(tr);
    });
    updateTableCount(lista.length);
  } catch (e) {
    console.warn('Tabela esteiras:', e.message);
  }
}

async function editarEsteira(id) {
  try {
    const e = await Esteiras.buscar(id);
    const oculto = document.getElementById('esteiraIdOculto');
    if (oculto) oculto.value = e.id;
    setVal(CAMPO.nome, e.nome);
    setVal(CAMPO.localizacao, e.localizacao ?? '');
    selectStatus(e.ativa !== false);
    updatePreview();
    window.scrollTo({ top: 0, behavior: 'smooth' });
  } catch (err) {
    showToast('Erro', err.message, true);
  }
}

async function deletarEsteira(id, nome) {
  if (!confirm(`Excluir a esteira "${nome}"?`)) return;
  try {
    await Esteiras.deletar(id);
    showToast('Removido', `Esteira "${nome}" excluída.`);
    await carregarTabela();
    await carregarMiniLista();
  } catch (e) {
    showToast('Erro', e.message, true);
  }
}

async function carregarMiniLista() {
  const ml = document.getElementById('beltMiniList');
  if (!ml) return;
  try {
    const lista = await Esteiras.listar();
    ml.innerHTML = '';
    lista.forEach(e => {
      const div = document.createElement('div');
      div.className = 'bm-row';
      div.onclick = () => showToast(e.nome, e.localizacao ?? '');
      div.innerHTML = `
        <div class="bm-dot" style="background:${e.ativa ? 'var(--emerald)' : 'var(--amber)'}"></div>
        <div class="bm-code">#${escapeHtml(e.id)}</div>
        <div class="bm-name">${escapeHtml((e.nome || '').split(' ')[0])}</div>
        <span class="chip ${chipEsteiraAtiva(e.ativa)}" style="font-size:8px;padding:2px 6px">${escapeHtml(labelEsteiraAtiva(e.ativa))}</span>`;
      ml.appendChild(div);
    });
    updateTableCount(lista.length);
  } catch (_) {}
}

function updateTableCount(n) {
  setText('tableCount', n + ' esteira' + (n !== 1 ? 's' : ''));
  setText('kTotal', n);
}

function val(id)       { return document.getElementById(id)?.value ?? ''; }
function setVal(id, v) { const el = document.getElementById(id); if (el) el.value = v; }
function setText(id, v) { const el = document.getElementById(id); if (el) el.textContent = v; }

document.addEventListener('DOMContentLoaded', async () => {
  await window.sdaeSessaoPronta;
  const formCard = document.querySelector('.card-body');
  if (formCard && !document.getElementById('esteiraIdOculto')) {
    const h = document.createElement('input');
    h.type = 'hidden';
    h.id = 'esteiraIdOculto';
    formCard.prepend(h);
  }
  document.getElementById(CAMPO.nome)?.addEventListener('input', updatePreview);
  document.getElementById(CAMPO.localizacao)?.addEventListener('change', updatePreview);
  selectStatus(true);
  await carregarTabela();
  await carregarMiniLista();
});
