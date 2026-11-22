/* Visão 360° do aluno: abre o retrato do aluno num modal (conteúdo carregado do servidor, só leitura). */
(function () {
    document.addEventListener('click', function (e) {
        const botao = e.target.closest('.btn-visao360');
        if (!botao) return;
        const alvo = document.getElementById('visao360Conteudo');
        const modalEl = document.getElementById('modalVisao360');
        if (!alvo || !modalEl || !window.bootstrap) return;
        alvo.innerHTML = '<div class="vazio">Carregando...</div>';
        bootstrap.Modal.getOrCreateInstance(modalEl).show();
        fetch(botao.dataset.url, { credentials: 'same-origin' })
            .then(function (r) {
                if (r.status === 403) return Promise.reject('negado');
                return r.ok ? r.text() : Promise.reject(r.status);
            })
            .then(function (html) { alvo.innerHTML = html; })
            .catch(function (erro) {
                alvo.innerHTML = '<div class="vazio">' + (erro === 'negado'
                    ? 'Você não tem acesso a este aluno.'
                    : 'Não foi possível carregar a visão do aluno.') + '</div>';
            });
    });
})();
