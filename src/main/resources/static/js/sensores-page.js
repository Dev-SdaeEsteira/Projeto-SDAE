/**
 * SDAE — Página de sensores (REST: modelo, leituraAtual, esteiraId).
 */

const SCAMPO = {
  modelo:  'fModelo',
  esteira: 'fEsteira',
  leitura: 'fDistNom',
};

function updatePreview() {
  const modelo = val(SCAMPO.modelo) || 'Modelo não informado';
  const sel = document.getElementById(SCAMPO.esteira);
  const est = sel?.options[sel.selectedIndex]?.text ?? 'Esteira não selecionada';
  setText('prevCodigo', modelo);
  setText('prevEsteira', est === 'Selecione a esteira...' ? 'Esteira não selecionada' : est);
}

async function carregarSelectEsteiras() {
  const sel = document.getElementById(SCAMPO.esteira);
  if (!sel) return;
  try {
    const lista = await Esteiras.listar();
    const atual = sel.value;
    sel.replaceChildren();
    const placeholder = document.createElement('option');
    placeholder.value = '';
    placeholder.textContent = 'Selecione a esteira...';
    sel.appendChild(placeholder);
    lista.forEach(e => {
      const opt = document.createElement('option');
      opt.value = e.id;
      opt.textContent = `${e.nome} — ${e.localizacao ?? ''}`;
      if (String(atual) === String(e.id)) opt.selected = true;
      sel.appendChild(opt);
    });
  } catch (e) {
    console.warn('Select esteiras:', e.message);
  }
}

function validate() {
  const campos = [
    { id: SCAMPO.modelo,  err: 'err-modelo'  },
    { id: SCAMPO.esteira, err: 'err-esteira' },
    { id: SCAMPO.leitura, err: 'err-dist'    },
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
    showToast('Erro de validação', 'Preencha modelo e esteira.', true);
    return;
  }

  const editId    = document.getElementById('sensorIdOculto')?.value;
  const esteiraId = val(SCAMPO.esteira);
  const leitura   = parseFloat(val(SCAMPO.leitura));
  const dados = {
    modelo:       val(SCAMPO.modelo).trim(),
    leituraAtual: Number.isFinite(leitura) ? leitura : 0,
  };

  try {
    if (editId) {
      await Sensores.atualizar(editId, dados, esteiraId);
      showToast('Atualizado ✔', `Sensor ${dados.modelo} atualizado.`);
    } else {
      await Sensores.criar(dados, esteiraId);
      showToast('Cadastrado ✔', `Sensor ${dados.modelo} cadastrado.`);
    }
    clearForm();
    await carregarTabela();
    await atualizarKPIs();
  } catch (e) {
    showToast('Erro', e.message, true);
  }
}

function clearForm() {
  Object.values(SCAMPO).forEach(id => {
    const el = document.getElementById(id);
    if (!el) return;
    el.value = '';
    el.className = el.tagName === 'SELECT' ? 'form-select' : 'form-input';
  });
  document.querySelectorAll('.field-error').forEach(e => e.className = 'field-error');
  const oculto = document.getElementById('sensorIdOculto');
  if (oculto) oculto.value = '';
  updatePreview();
}

async function carregarTabela() {
  const tbody = document.getElementById('sensorBody') ?? document.querySelector('tbody');
  if (!tbody) return;
  try {
    const lista = await Sensores.listar();
    tbody.innerHTML = '';
    lista.forEach(s => {
      const tr = document.createElement('tr');
      tr.className = 'new';
      const modeloEsc = String(s.modelo).replace(/\\/g, '\\\\').replace(/'/g, "\\'");
      tr.innerHTML = `
        <td class="mono" style="color:var(--blue-300)">#${escapeHtml(s.id)}</td>
        <td>${escapeHtml(s.modelo)}</td>
        <td class="mono">${escapeHtml(s.esteiraNome ?? '—')}</td>
        <td style="font-size:11px;color:var(--text-400)">${escapeHtml(s.esteiraLocalizacao ?? '—')}</td>
        <td class="mono">${escapeHtml(s.leituraAtual ?? 0)}%</td>
        <td><span class="chip ${chipNivelRisco(s.nivelRisco)}">${escapeHtml(labelNivelRisco(s.nivelRisco))}</span></td>
        <td>
          <button class="act-icon" onclick="editarSensor(${s.id})">✎</button>
          <button class="act-icon danger" onclick="deletarSensor(${s.id},'${modeloEsc}')">✕</button>
        </td>`;
      tbody.appendChild(tr);
    });
    setText('kTotal', lista.length);
  } catch (e) {
    console.warn('Tabela sensores:', e.message);
  }
}

async function atualizarKPIs() {
  try {
    const lista = await Sensores.listar();
    const normal  = lista.filter(s => (s.nivelRisco || '').toUpperCase() === 'NORMAL').length;
    const critico = lista.filter(s => (s.nivelRisco || '').toUpperCase() === 'CRITICO').length;
    const atencao = lista.filter(s => (s.nivelRisco || '').toUpperCase() === 'ATENCAO').length;
    setText('kTotal',   lista.length);
    setText('kOnline',  normal);
    setText('kOffline', critico + atencao);
  } catch (_) {}
}

async function editarSensor(id) {
  try {
    const s = await Sensores.buscar(id);
    document.getElementById('sensorIdOculto').value = s.id;
    setVal(SCAMPO.modelo, s.modelo ?? '');
    setVal(SCAMPO.leitura, s.leituraAtual ?? 0);
    await carregarSelectEsteiras();
    setVal(SCAMPO.esteira, s.esteiraId ?? '');
    updatePreview();
    window.scrollTo({ top: 0, behavior: 'smooth' });
  } catch (err) {
    showToast('Erro', err.message, true);
  }
}

async function deletarSensor(id, modelo) {
  if (!confirm(`Excluir o sensor "${modelo}"?`)) return;
  try {
    await Sensores.deletar(id);
    showToast('Removido', `Sensor "${modelo}" excluído.`);
    await carregarTabela();
    await atualizarKPIs();
  } catch (e) {
    showToast('Erro', e.message, true);
  }
}

function val(id)       { return document.getElementById(id)?.value ?? ''; }
function setVal(id, v) { const el = document.getElementById(id); if (el) el.value = v; }
function setText(id, v) { const el = document.getElementById(id); if (el) el.textContent = v; }

document.addEventListener('DOMContentLoaded', async () => {
  await window.sdaeSessaoPronta;
  const body = document.querySelector('.card-body');
  if (body && !document.getElementById('sensorIdOculto')) {
    const h = document.createElement('input');
    h.type = 'hidden';
    h.id = 'sensorIdOculto';
    body.prepend(h);
  }
  document.getElementById(SCAMPO.modelo)?.addEventListener('input', updatePreview);
  document.getElementById(SCAMPO.esteira)?.addEventListener('change', updatePreview);
  await carregarSelectEsteiras();
  await carregarTabela();
  await atualizarKPIs();
});
