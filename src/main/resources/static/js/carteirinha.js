// Carteirinha digital do aluno: o código do QR vale poucos minutos, então a tela mostra o tempo
// que falta e pede um código novo antes de vencer (GET /aluno/carteirinha/codigo, só do próprio aluno).
// O QR continua sendo desenhado no servidor; aqui só trocamos o desenho e o texto.
(function () {
    const card = document.getElementById('carteirinha');
    if (!card) return;

    const url = card.dataset.url;
    const areaQr = document.getElementById('carteirinhaQr');
    const areaCodigo = document.getElementById('carteirinhaCodigo');
    const areaValidade = document.getElementById('carteirinhaValidade');
    const areaSituacao = card.querySelector('.carteirinha-situacao');

    const RENOVAR_COM_SEGUNDOS = 60;   // pede o código novo quando faltar 1 minuto
    const NOVA_TENTATIVA_MS = 5000;    // se falhar, tenta de novo daqui a 5 s

    let restante = parseInt(card.dataset.restante, 10) || 300;
    let marco = performance.now();
    let buscando = false;
    let proximaTentativa = 0;

    function segundosRestantes() {
        return Math.ceil(restante - (performance.now() - marco) / 1000);
    }

    function formatar(s) {
        const m = Math.floor(s / 60);
        const r = s % 60;
        return m + ':' + (r < 10 ? '0' : '') + r;
    }

    function desenharQr(svgTexto) {
        const doc = new DOMParser().parseFromString(svgTexto, 'image/svg+xml');
        const svg = doc.documentElement;
        if (!svg || svg.nodeName.toLowerCase() !== 'svg') return;
        areaQr.replaceChildren(document.importNode(svg, true));
    }

    function mostrarSituacao(motivo) {
        if (!areaSituacao) return;
        const span = document.createElement('span');
        if (motivo) {
            span.className = 'status-err';
            span.textContent = 'Bloqueada: ' + motivo;
        } else {
            span.className = 'status-ok';
            span.textContent = 'Acesso liberado';
        }
        areaSituacao.replaceChildren(span);
    }

    async function renovar() {
        if (buscando || performance.now() < proximaTentativa) return;
        buscando = true;
        try {
            const resp = await fetch(url, {
                credentials: 'same-origin',
                cache: 'no-store',
                headers: { 'Accept': 'application/json' }
            });
            if (!resp.ok) throw new Error('HTTP ' + resp.status);
            const d = await resp.json();
            if (!d || !d.codigo || !d.qr) throw new Error('resposta inesperada');
            desenharQr(d.qr);
            areaCodigo.textContent = d.codigo;
            mostrarSituacao(d.motivo);
            restante = Number(d.restante) || 300;
            marco = performance.now();
        } catch (e) {
            proximaTentativa = performance.now() + NOVA_TENTATIVA_MS;
        } finally {
            buscando = false;
        }
    }

    function atualizar() {
        const s = segundosRestantes();
        if (s > 0) {
            areaValidade.textContent = 'Válido por ' + formatar(s);
            card.classList.remove('carteirinha-vencida');
        } else {
            areaValidade.textContent = 'Código vencido. Atualizando...';
            card.classList.add('carteirinha-vencida');
        }
        if (s <= RENOVAR_COM_SEGUNDOS) renovar();
    }

    setInterval(atualizar, 1000);
    // Aba em segundo plano para os contadores: ao voltar, confere na hora
    document.addEventListener('visibilitychange', function () {
        if (!document.hidden) atualizar();
    });
    atualizar();
})();
