/* Grade semanal de aulas.
   1) Setas e "Hoje": busca só a grade da outra semana (GET) e troca o conteúdo, sem recarregar a página (não pisca).
   2) Depois de reservar/cancelar o servidor volta com ?semana=...: abre a aba Aulas já na partida, antes de pintar. */
(function () {
    function abrirAbaAulas() {
        const botao = document.getElementById('btn-aba-aulas');
        if (botao) botao.click();
    }

    try {
        if (/[?&]semana=/.test(location.search)) {
            if (window.bootstrap) abrirAbaAulas();
            else document.addEventListener('DOMContentLoaded', abrirAbaAulas);
        }
    } catch (e) { /* sem a grade, nada a fazer */ }

    function trocarSemana(url, empilhar) {
        const atual = document.getElementById('grade-aulas');
        if (!atual) { location.href = url; return; }
        atual.classList.add('carregando');
        fetch(url, { credentials: 'same-origin', headers: { 'X-Requested-With': 'fetch' } })
            .then(function (r) { if (!r.ok) throw new Error('http ' + r.status); return r.text(); })
            .then(function (html) {
                const novo = new DOMParser().parseFromString(html, 'text/html').getElementById('grade-aulas');
                if (!novo) throw new Error('sem grade');
                atual.innerHTML = novo.innerHTML;
                atual.classList.remove('carregando');
                if (empilhar) history.pushState({ gradeAulas: true }, '', url);
            })
            .catch(function () { location.href = url; });
    }

    document.addEventListener('click', function (e) {
        const link = e.target.closest('#grade-aulas .grade-nav a');
        if (!link || e.ctrlKey || e.metaKey || e.shiftKey || e.button !== 0) return;
        e.preventDefault();
        trocarSemana(link.href, true);
    });

    window.addEventListener('popstate', function () {
        if (document.getElementById('grade-aulas')) trocarSemana(location.href, false);
    });
})();
