/* =========================================================
   SDAE — sdae-forms.js
   =========================================================
   Comportamentos compartilhados por todos os formulários:

   1) Loading no botão de submit (Melhoria 4)
      -> Assim que qualquer <form> é enviado, o botão de
         submit ganha a classe "btn-loading" automaticamente
         (spinner + texto ocultado via CSS). Isso evita duplo
         clique enquanto o servidor processa o cadastro/edição.
         Não precisa fazer nada no HTML, é automático.

   2) Modal de confirmação (Melhoria 5)
      -> Substitui o confirm() nativo do navegador (feio e
         bloqueável) por um modal customizado no estilo do
         sistema. Para usar, troque:

            onclick="return confirm('Excluir X?')"

         por:

            class="sdae-confirm-delete"
            data-confirm-titulo="Excluir X?"
            data-confirm-mensagem="Esta ação não pode ser desfeita."

         O botão precisa estar dentro de um <form> (o form é
         o que será enviado depois que o usuário confirmar).

   Como usar em cada página:
       <script src="/js/sdae-forms.js" th:src="@{/js/sdae-forms.js}"></script>
   (colocar no final do <body>, depois dos outros scripts)
   ========================================================= */
(function () {

    /* ---------- Modal de confirmação ---------- */
    function criarModal() {
        if (document.getElementById('sdaeModalOverlay')) return;
        const overlay = document.createElement('div');
        overlay.id = 'sdaeModalOverlay';
        overlay.className = 'sdae-modal-overlay';
        overlay.innerHTML =
            '<div class="sdae-modal">' +
                '<h3 id="sdaeModalTitulo">Confirmar ação</h3>' +
                '<p id="sdaeModalMensagem"></p>' +
                '<div class="sdae-modal-actions">' +
                    '<button type="button" class="sdae-modal-btn-cancelar">Cancelar</button>' +
                    '<button type="button" class="sdae-modal-btn-confirmar">Excluir</button>' +
                '</div>' +
            '</div>';
        document.body.appendChild(overlay);
    }

    function confirmarAcao(titulo, mensagem) {
        criarModal();
        const overlay = document.getElementById('sdaeModalOverlay');
        document.getElementById('sdaeModalTitulo').textContent = titulo;
        document.getElementById('sdaeModalMensagem').textContent = mensagem;
        overlay.classList.add('aberto');

        return new Promise(function (resolve) {
            const btnCancelar = overlay.querySelector('.sdae-modal-btn-cancelar');
            const btnConfirmar = overlay.querySelector('.sdae-modal-btn-confirmar');

            function limpar(resultado) {
                overlay.classList.remove('aberto');
                btnCancelar.removeEventListener('click', onCancelar);
                btnConfirmar.removeEventListener('click', onConfirmar);
                overlay.removeEventListener('click', onOverlayClick);
                resolve(resultado);
            }
            function onCancelar() { limpar(false); }
            function onConfirmar() { limpar(true); }
            function onOverlayClick(e) { if (e.target === overlay) limpar(false); }

            btnCancelar.addEventListener('click', onCancelar);
            btnConfirmar.addEventListener('click', onConfirmar);
            overlay.addEventListener('click', onOverlayClick);
        });
    }

    document.addEventListener('click', function (e) {
        const btn = e.target.closest('.sdae-confirm-delete');
        if (!btn) return;

        e.preventDefault();
        const form = btn.closest('form');
        const titulo = btn.dataset.confirmTitulo || 'Confirmar exclusão';
        const mensagem = btn.dataset.confirmMensagem || 'Esta ação não pode ser desfeita.';

        confirmarAcao(titulo, mensagem).then(function (confirmado) {
            if (confirmado && form) {
                btn.classList.add('btn-loading');
                btn.disabled = true;
                if (form.requestSubmit) form.requestSubmit();
                else form.submit();
            }
        });
    });

    /* ---------- Loading automático no submit ---------- */
    document.addEventListener('submit', function (e) {
        const form = e.target;
        // Ignora forms que já têm seu próprio botão de confirmação
        // (esse fluxo já adiciona o loading manualmente acima).
        if (form.dataset.sdaeSkipLoading) return;

        const btn = form.querySelector('button[type="submit"]');
        if (btn && !btn.disabled) {
            btn.classList.add('btn-loading');
            btn.disabled = true;
        }
    });

})();
