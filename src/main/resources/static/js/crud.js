/**
 * SDAE — CRUD via REST (modelo alinhado às entidades Java).
 */

function jsEscAttr(s) {
  if (s == null || s === undefined) return '';
  return String(s)
    .replace(/\\/g, '\\\\')
    .replace(/'/g, "\\'")
    .replace(/"/g, '\\"')
    .replace(/</g, '\\x3C')
    .replace(/>/g, '\\x3E')
    .replace(/&/g, '\\x26');
}

function payloadEsteiraFromForm(prefix = 'e') {
  const ativaEl = document.getElementById(prefix + 'Ativa') || document.getElementById('eAtiva');
  const ativa = ativaEl ? ativaEl.checked : true;
  return {
    nome:        document.getElementById(prefix + 'Nome')?.value?.trim()
              || document.getElementById('eNome')?.value?.trim(),
    localizacao: document.getElementById(prefix + 'Localizacao')?.value?.trim()
              || document.getElementById('eLocalizacao')?.value?.trim(),
    ativa
  };
}

function payloadSensorFromForm() {
  const leitura = parseFloat(document.getElementById('sLeitura')?.value
    || document.getElementById('leituraAtual')?.value);
  return {
    modelo: document.getElementById('sModelo')?.value?.trim(),
    leituraAtual: Number.isFinite(leitura) ? leitura : 0
  };
}

const CrudEsteiras = {
  async carregar() {
    try {
      this.renderizar(await Esteiras.listar());
    } catch (e) {
      showToast('Erro', 'Não foi possível carregar as esteiras.', true);
    }
  },

  renderizar(lista) {
    const tbody = document.getElementById('tabelaEsteiras');
    if (!tbody) return;
    tbody.innerHTML = '';
    if (!lista.length) {
      tbody.innerHTML = '<tr><td colspan="5" style="text-align:center;color:var(--text-400)">Nenhuma esteira cadastrada</td></tr>';
      return;
    }
    lista.forEach(e => {
      const tr = document.createElement('tr');
      tr.innerHTML = `
        <td><span class="font-mono">#${escapeHtml(e.id)}</span></td>
        <td>${escapeHtml(e.nome)}</td>
        <td>${escapeHtml(e.localizacao ?? '—')}</td>
        <td><span class="chip ${chipEsteiraAtiva(e.ativa)}">${escapeHtml(labelEsteiraAtiva(e.ativa))}</span></td>
        <td class="td-actions">
          <button class="btn btn-sm btn-ghost" onclick="CrudEsteiras.editar(${e.id})">✏ Editar</button>
          <button class="btn btn-sm btn-danger" onclick="CrudEsteiras.confirmarDeletar(${e.id}, '${jsEscAttr(e.nome)}')">✕</button>
        </td>`;
      tbody.appendChild(tr);
    });
  },

  async salvar(evento) {
    evento?.preventDefault();
    const id = document.getElementById('esteiraId')?.value;
    const dados = payloadEsteiraFromForm();
    if (!dados.nome || !dados.localizacao) {
      showToast('Validação', 'Informe nome e localização.', true);
      return;
    }
    try {
      if (id) {
        await Esteiras.atualizar(id, dados);
        showToast('Atualizado', `Esteira "${dados.nome}" atualizada.`);
      } else {
        await Esteiras.criar(dados);
        showToast('Cadastrado', `Esteira "${dados.nome}" cadastrada.`);
      }
      fecharModal('modalEsteira');
      await this.carregar();
    } catch (e) {
      showToast('Erro', e.message, true);
    }
  },

  async editar(id) {
    try {
      const e = await Esteiras.buscar(id);
      document.getElementById('esteiraId').value = e.id;
      const nome = document.getElementById('eNome');
      const loc  = document.getElementById('eLocalizacao');
      const ativa = document.getElementById('eAtiva');
      if (nome)  nome.value  = e.nome ?? '';
      if (loc)   loc.value   = e.localizacao ?? '';
      if (ativa) ativa.checked = e.ativa !== false;
      abrirModal('modalEsteira', 'Editar Esteira');
    } catch (err) {
      showToast('Erro', err.message, true);
    }
  },

  novoRegistro() {
    document.getElementById('esteiraId').value = '';
    document.getElementById('formEsteira')?.reset();
    const ativa = document.getElementById('eAtiva');
    if (ativa) ativa.checked = true;
    abrirModal('modalEsteira', 'Nova Esteira');
  },

  confirmarDeletar(id, nome) {
    if (!confirm(`Excluir a esteira "${nome}"?\nSensores vinculados também serão removidos.`)) return;
    Esteiras.deletar(id)
      .then(() => { showToast('Removido', `Esteira "${nome}" excluída.`); this.carregar(); })
      .catch(e => showToast('Erro', e.message, true));
  }
};

const CrudSensores = {
  async carregar() {
    try {
      const lista = await Sensores.listar();
      this.renderizar(lista);
      await this.carregarSelectEsteiras();
    } catch (e) {
      showToast('Erro', 'Não foi possível carregar os sensores.', true);
    }
  },

  async carregarSelectEsteiras() {
    try {
      const esteiras = await Esteiras.listar();
      document.querySelectorAll('.select-esteira, #sEsteiraId').forEach(sel => {
        const atual = sel.value;
        sel.replaceChildren();
        const placeholder = document.createElement('option');
        placeholder.value = '';
        placeholder.textContent = 'Selecione a esteira';
        sel.appendChild(placeholder);
        esteiras.forEach(e => {
          const opt = document.createElement('option');
          opt.value = e.id;
          opt.textContent = `${e.nome} — ${e.localizacao ?? ''}`;
          if (String(atual) === String(e.id)) opt.selected = true;
          sel.appendChild(opt);
        });
      });
    } catch (_) { /* noop */ }
  },

  renderizar(lista) {
    const tbody = document.getElementById('tabelaSensores');
    if (!tbody) return;
    tbody.innerHTML = '';
    if (!lista.length) {
      tbody.innerHTML = '<tr><td colspan="6" style="text-align:center;color:var(--text-400)">Nenhum sensor cadastrado</td></tr>';
      return;
    }
    lista.forEach(s => {
      const tr = document.createElement('tr');
      tr.innerHTML = `
        <td><span class="font-mono">#${escapeHtml(s.id)}</span></td>
        <td>${escapeHtml(s.modelo)}</td>
        <td>${escapeHtml(s.esteiraNome ?? '—')}</td>
        <td class="mono">${escapeHtml(s.leituraAtual ?? 0)}%</td>
        <td><span class="chip ${chipNivelRisco(s.nivelRisco)}">${escapeHtml(labelNivelRisco(s.nivelRisco))}</span></td>
        <td class="td-actions">
          <button class="btn btn-sm btn-ghost" onclick="CrudSensores.editar(${s.id})">✏ Editar</button>
          <button class="btn btn-sm btn-danger" onclick="CrudSensores.confirmarDeletar(${s.id}, '${jsEscAttr(s.modelo)}')">✕</button>
        </td>`;
      tbody.appendChild(tr);
    });
  },

  async salvar(evento) {
    evento?.preventDefault();
    const id = document.getElementById('sensorId')?.value;
    const esteiraId = document.getElementById('sEsteiraId')?.value;
    const dados = payloadSensorFromForm();
    if (!dados.modelo) {
      showToast('Validação', 'Informe o modelo do sensor.', true);
      return;
    }
    if (!esteiraId) {
      showToast('Validação', 'Selecione a esteira vinculada.', true);
      return;
    }
    try {
      if (id) {
        await Sensores.atualizar(id, dados, esteiraId);
        showToast('Atualizado', `Sensor ${dados.modelo} atualizado.`);
      } else {
        await Sensores.criar(dados, esteiraId);
        showToast('Cadastrado', `Sensor ${dados.modelo} cadastrado.`);
      }
      fecharModal('modalSensor');
      await this.carregar();
    } catch (e) {
      showToast('Erro', e.message, true);
    }
  },

  async editar(id) {
    try {
      const s = await Sensores.buscar(id);
      document.getElementById('sensorId').value = s.id;
      const modelo = document.getElementById('sModelo');
      const leitura = document.getElementById('sLeitura');
      if (modelo)  modelo.value  = s.modelo ?? '';
      if (leitura) leitura.value = s.leituraAtual ?? 0;
      await this.carregarSelectEsteiras();
      const esteiraSel = document.getElementById('sEsteiraId');
      if (esteiraSel) esteiraSel.value = s.esteiraId ?? '';
      abrirModal('modalSensor', 'Editar Sensor');
    } catch (err) {
      showToast('Erro', err.message, true);
    }
  },

  novoRegistro() {
    document.getElementById('sensorId').value = '';
    document.getElementById('formSensor')?.reset();
    this.carregarSelectEsteiras();
    abrirModal('modalSensor', 'Novo Sensor');
  },

  confirmarDeletar(id, modelo) {
    if (!confirm(`Excluir o sensor "${modelo}"?`)) return;
    Sensores.deletar(id)
      .then(() => { showToast('Removido', `Sensor "${modelo}" excluído.`); this.carregar(); })
      .catch(e => showToast('Erro', e.message, true));
  }
};

const CrudFuncionarios = {
  async carregar() {
    try {
      this.renderizar(await Funcionarios.listar());
    } catch (e) {
      showToast('Erro', 'Não foi possível carregar os funcionários.', true);
    }
  },

  renderizar(lista) {
    const tbody = document.getElementById('tabelaFuncionarios');
    if (!tbody) return;
    tbody.innerHTML = '';
    if (!lista.length) {
      tbody.innerHTML = '<tr><td colspan="7" style="text-align:center;color:var(--text-400)">Nenhum funcionário cadastrado</td></tr>';
      return;
    }
    lista.forEach(f => {
      const nivelCls = f.nivelAcesso === 'ADMINISTRADOR' ? 'chip-alert'
        : f.nivelAcesso === 'ENGENHEIRO' ? 'chip-ok'
        : f.nivelAcesso === 'TECNICO' ? 'chip-maint' : '';
      const tr = document.createElement('tr');
      tr.innerHTML = `
        <td><span class="font-mono">${escapeHtml(f.matricula ?? '—')}</span></td>
        <td>${escapeHtml(f.nome)}</td>
        <td>${escapeHtml(f.cargo ?? '—')}</td>
        <td>${escapeHtml(f.departamento ?? '—')}</td>
        <td>${escapeHtml(f.turno ?? '—')}</td>
        <td><span class="chip ${nivelCls}" style="font-size:10px">${escapeHtml(f.nivelAcesso ?? '—')}</span></td>
        <td class="td-actions">
          <button class="btn btn-sm btn-ghost" onclick="CrudFuncionarios.editar(${f.id})">✏ Editar</button>
          <button class="btn btn-sm btn-danger" onclick="CrudFuncionarios.confirmarDeletar(${f.id}, '${jsEscAttr(f.nome)}')">✕</button>
        </td>`;
      tbody.appendChild(tr);
    });
  },

  async salvar(evento) {
    evento?.preventDefault();
    const id = document.getElementById('funcId')?.value;
    const dados = {
      nome:         document.getElementById('fNome')?.value?.trim(),
      matricula:    document.getElementById('fMatricula')?.value?.trim(),
      cargo:        document.getElementById('fCargo')?.value,
      departamento: document.getElementById('fDept')?.value,
      email:        document.getElementById('fEmail')?.value?.trim(),
      telefone:     document.getElementById('fTelefone')?.value?.trim(),
      turno:        document.getElementById('fTurno')?.value ?? 'MANHA',
      nivelAcesso:  document.getElementById('fNivel')?.value ?? 'OPERADOR',
    };
    const senha = document.getElementById('fSenha')?.value;
    if (senha) dados.senha = senha;
    try {
      if (id) {
        await Funcionarios.atualizar(id, dados);
        showToast('Atualizado', `Funcionário ${dados.nome} atualizado.`);
      } else {
        if (!dados.senha) {
          showToast('Validação', 'Informe uma senha para o novo usuário.', true);
          return;
        }
        await Funcionarios.criar(dados);
        showToast('Cadastrado', `Funcionário ${dados.nome} cadastrado.`);
      }
      fecharModal('modalFuncionario');
      await this.carregar();
    } catch (e) {
      showToast('Erro', e.message, true);
    }
  },

  async editar(id) {
    try {
      const f = await Funcionarios.buscar(id);
      document.getElementById('funcId').value = f.id;
      document.getElementById('fNome').value = f.nome ?? '';
      document.getElementById('fMatricula').value = f.matricula ?? '';
      document.getElementById('fCargo').value = f.cargo ?? '';
      document.getElementById('fDept').value = f.departamento ?? '';
      document.getElementById('fEmail').value = f.email ?? '';
      document.getElementById('fTelefone').value = f.telefone ?? '';
      document.getElementById('fTurno').value = f.turno ?? 'MANHA';
      document.getElementById('fNivel').value = f.nivelAcesso ?? 'OPERADOR';
      abrirModal('modalFuncionario', 'Editar Funcionário');
    } catch (err) {
      showToast('Erro', err.message, true);
    }
  },

  novoRegistro() {
    document.getElementById('funcId').value = '';
    document.getElementById('formFuncionario')?.reset();
    abrirModal('modalFuncionario', 'Novo Funcionário');
  },

  confirmarDeletar(id, nome) {
    if (!confirm(`Excluir o funcionário "${nome}"?`)) return;
    Funcionarios.deletar(id)
      .then(() => { showToast('Removido', `Funcionário "${nome}" excluído.`); this.carregar(); })
      .catch(e => showToast('Erro', e.message, true));
  }
};

function abrirModal(id, titulo) {
  const modal = document.getElementById(id);
  if (!modal) return;
  const h = modal.querySelector('.modal-title');
  if (h && titulo) h.textContent = titulo;
  modal.style.display = 'flex';
  requestAnimationFrame(() => modal.classList.add('open'));
}

function fecharModal(id) {
  const modal = document.getElementById(id);
  if (!modal) return;
  modal.classList.remove('open');
  setTimeout(() => { modal.style.display = 'none'; }, 250);
}

document.addEventListener('click', e => {
  if (e.target.classList.contains('modal-overlay')) fecharModal(e.target.id);
});
