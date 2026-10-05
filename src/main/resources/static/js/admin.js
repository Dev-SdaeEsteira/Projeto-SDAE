/* ═══════════════════════════════════════════════════════════
   SDAE — Administração · admin.js

// ── BUSCA DE FUNCIONÁRIOS (filtra a tabela renderizada pelo Thymeleaf) ──
document.addEventListener('DOMContentLoaded', async () => {
  if (window.sdaeSessaoPronta) await window.sdaeSessaoPronta;

  const input     = document.getElementById('buscaFuncionario');
  const semResult = document.getElementById('buscaSemResultado');
  const tabela    = document.querySelector('table');
  if (!input || !tabela) return;

  const linhas = Array.from(tabela.querySelectorAll('tbody tr'));

  input.addEventListener('input', () => {
    const termo = input.value.trim().toLowerCase();
    let visiveis = 0;

    linhas.forEach(linha => {
      const nome      = linha.dataset.nome      || '';
      const matricula = linha.dataset.matricula || '';
      const bateu = !termo || nome.includes(termo) || matricula.includes(termo);
      linha.style.display = bateu ? '' : 'none';
      if (bateu) visiveis++;
    });

    if (semResult) semResult.style.display = visiveis === 0 ? 'block' : 'none';
    tabela.style.display = visiveis === 0 ? 'none' : '';
  });
});