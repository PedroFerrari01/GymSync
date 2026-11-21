/* Sininho de notificações: contagem, painel e "lida" guardada no navegador (localStorage, opcional). */
(function () {
    const raiz = document.getElementById('sininho');
    if (!raiz) return;
    const botao = document.getElementById('sininhoBotao');
    const painel = document.getElementById('sininhoPainel');
    const contagem = document.getElementById('sininhoContagem');
    const vazio = document.getElementById('sininhoVazio');
    const links = Array.from(raiz.querySelectorAll('.sininho-link'));
    const chaveArmazem = 'gymsync.sininho.lidas.' + (raiz.dataset.usuario || '');

    function lerLidas() {
        try { return JSON.parse(localStorage.getItem(chaveArmazem) || '[]'); } catch (e) { return []; }
    }
    function gravarLidas(lista) {
        try { localStorage.setItem(chaveArmazem, JSON.stringify(lista)); } catch (e) { /* sem armazenamento: reaparece */ }
    }
    const lidas = new Set(lerLidas());

    function atualizar() {
        let naoLidas = 0;
        links.forEach(function (a) {
            const lida = lidas.has(a.dataset.chave);
            a.closest('.sininho-item').classList.toggle('lida', lida);
            if (!lida) naoLidas++;
        });
        contagem.textContent = naoLidas > 99 ? '99+' : String(naoLidas);
        contagem.hidden = naoLidas === 0;
        vazio.hidden = links.length > 0;
        botao.setAttribute('aria-label', naoLidas ? 'Notificações: ' + naoLidas + ' novas' : 'Notificações');
    }
    function abrir(aberto) {
        painel.hidden = !aberto;
        botao.setAttribute('aria-expanded', String(aberto));
    }
    function marcar(chave) {
        lidas.add(chave);
        gravarLidas(Array.from(lidas));
        atualizar();
    }
    function piscar(id) {
        const alvo = id && document.getElementById(id);
        if (!alvo) return;
        alvo.classList.remove('destaque-piscar');
        void alvo.offsetWidth;
        alvo.classList.add('destaque-piscar');
        alvo.scrollIntoView({ behavior: 'smooth', block: 'center' });
        setTimeout(function () { alvo.classList.remove('destaque-piscar'); }, 2000);
    }

    botao.addEventListener('click', function (e) { e.stopPropagation(); abrir(painel.hidden); });
    document.addEventListener('click', function (e) { if (!painel.hidden && !raiz.contains(e.target)) abrir(false); });
    document.addEventListener('keydown', function (e) {
        if (e.key === 'Escape' && !painel.hidden) { abrir(false); botao.focus(); }
    });
    document.getElementById('sininhoLimpar').addEventListener('click', function () {
        links.forEach(function (a) { lidas.add(a.dataset.chave); });
        gravarLidas(Array.from(lidas));
        atualizar();
    });
    links.forEach(function (a) {
        a.addEventListener('click', function (e) {
            marcar(a.dataset.chave);
            const destino = a.getAttribute('href');
            if (destino && destino !== '#') return; // página (ex.: /mensagens)
            e.preventDefault();
            abrir(false);
            const aba = a.dataset.aba && document.getElementById(a.dataset.aba);
            if (aba) aba.click();
            setTimeout(function () { piscar(a.dataset.alvo); }, 350);
        });
    });
    atualizar();
})();
