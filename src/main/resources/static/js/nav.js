/**
 * Reforça cabeçalho: nome do usuário (sessão) e logout integrado ao Spring Security.
 * Não substitui os links de navegação já definidos no Thymeleaf.
 */
(function () {
  function wireLogout() {
    document.querySelectorAll('[data-sdae-logout], .nav-logout').forEach(el => {
      if (el.dataset.sdaeWired) return;
      el.dataset.sdaeWired = '1';
      el.addEventListener('click', e => {
        if (el.tagName === 'A') e.preventDefault();
        if (typeof Sessao !== 'undefined') Sessao.sair();
      });
    });
    document.querySelectorAll('a[href="/logout"]').forEach(el => {
      if (el.dataset.sdaeWired) return;
      el.dataset.sdaeWired = '1';
      el.addEventListener('click', e => {
        e.preventDefault();
        if (typeof Sessao !== 'undefined') Sessao.sair();
      });
    });
  }

  function enhance() {
    if (typeof verificarSessao === 'function') verificarSessao();
    wireLogout();
  }

  function onReady() {
    const run = () => enhance();
    if (window.sdaeSessaoPronta) {
      window.sdaeSessaoPronta.then(run);
    } else {
      run();
    }
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', onReady);
  } else {
    onReady();
  }
})();
