/* MODO TREINO (aluno, celular): um exercício por vez, séries, descanso e resumo.
   Usa o registro que já existe (POST /aluno/exercicios/registrar), então alimenta a "Adesão ao treino" do professor.
   Visitante: roda inteiro, mas não envia nada. */
(function () {
    'use strict';
    /* Imprimir a ficha: leva a folha limpa (#fichaImpressao) para o body e esconde o resto só durante a impressão */
    const imprimir = document.querySelector('[data-imprimir-ficha]');
    const folha = document.getElementById('fichaImpressao');
    if (imprimir && folha) {
        imprimir.addEventListener('click', () => {
            document.body.appendChild(folha);
            document.body.classList.add('imprimindo-ficha');
            window.print();
        });
        window.addEventListener('afterprint', () => document.body.classList.remove('imprimindo-ficha'));
    }
    const botao = document.getElementById('btnModoTreino');
    const overlay = document.getElementById('modoTreino');
    if (!botao || !overlay) return;

    const CHAVE = 'gymsync-modo-treino';
    const DESCANSO_PADRAO = 60;
    const visitante = botao.dataset.visitante === 'true';
    const $ = id => document.getElementById(id);
    const el = {
        contador: $('mtContador'), tempo: $('mtTempo'), barra: $('mtBarra'), nome: $('mtNome'), meta: $('mtMeta'),
        carga: $('mtCarga'), series: $('mtSeries'), dica: $('mtDica'), corpo: $('mtExercicio'), rodape: $('mtRodape'),
        proximo: $('mtProximo'), descanso: $('mtDescanso'), descansoTempo: $('mtDescansoTempo'), resumo: $('mtResumo'),
        form: $('formModoTreino'), visitante: $('mtVisitante')
    };

    let lista = [];       // exercícios da ficha
    let estado = null;    // { dia, assinatura, idx, feitas:{id:n}, cargas:{id:kg}, enviados:{id:true}, inicio }
    let descansoFim = 0, intervalo = null, relogio = null, wake = null, encerrado = false;

    const seguro = fn => { try { return fn(); } catch (e) { return null; } };
    const hoje = () => new Date().toISOString().slice(0, 10);
    const fmt = s => { s = Math.max(0, Math.round(s)); return Math.floor(s / 60) + ':' + String(s % 60).padStart(2, '0'); };

    function lerFicha() {
        return Array.from(document.querySelectorAll('tr.linha-exercicio')).map(tr => ({
            id: tr.dataset.id, nome: tr.dataset.nome,
            series: parseInt(tr.dataset.series, 10) || 1, reps: tr.dataset.reps || '',
            carga: tr.dataset.carga || ''
        }));
    }

    function salvar() { if (estado) seguro(() => localStorage.setItem(CHAVE, JSON.stringify(estado))); }
    function limpar() { seguro(() => localStorage.removeItem(CHAVE)); }

    function carregarOuCriar() {
        const assinatura = lista.map(e => e.id).join(',');
        const salvo = seguro(() => JSON.parse(localStorage.getItem(CHAVE) || 'null'));
        if (salvo && salvo.dia === hoje() && salvo.assinatura === assinatura && !salvo.concluido) return salvo;
        return { dia: hoje(), assinatura, idx: 0, feitas: {}, cargas: {}, enviados: {}, inicio: Date.now() };
    }

    /* ---- tela sempre acesa ---- */
    async function pedirWake() {
        try {
            if (!('wakeLock' in navigator) || wake || document.visibilityState !== 'visible') return;
            wake = await navigator.wakeLock.request('screen');
            wake.addEventListener('release', () => { wake = null; });
        } catch (e) { wake = null; }
    }
    function soltarWake() { try { if (wake) wake.release(); } catch (e) { /* nada */ } wake = null; }
    document.addEventListener('visibilitychange', () => {
        if (document.visibilityState === 'visible' && !overlay.hidden && !encerrado) pedirWake();
    });

    /* ---- desenho ---- */
    function totalSeries() { return lista.reduce((t, e) => t + e.series, 0); }
    function seriesFeitas() { return lista.reduce((t, e) => t + Math.min(estado.feitas[e.id] || 0, e.series), 0); }

    function desenhar() {
        const e = lista[estado.idx];
        const feitas = Math.min(estado.feitas[e.id] || 0, e.series);
        el.contador.textContent = (estado.idx + 1) + ' / ' + lista.length;
        el.barra.style.width = Math.round(100 * seriesFeitas() / Math.max(1, totalSeries())) + '%';
        el.nome.textContent = e.nome;
        el.meta.textContent = e.series + ' x ' + e.reps;
        el.carga.value = estado.cargas[e.id] != null ? estado.cargas[e.id] : e.carga;
        el.series.textContent = '';
        for (let i = 0; i < e.series; i++) {
            const b = document.createElement('button');
            b.type = 'button';
            b.className = 'mt-serie' + (i < feitas ? ' feita' : '') + (i === feitas ? ' proxima' : '');
            b.dataset.serie = i;
            b.setAttribute('aria-pressed', i < feitas ? 'true' : 'false');
            b.innerHTML = '<span class="mt-serie-n">Série ' + (i + 1) + '</span><span class="mt-serie-r">' +
                (i < feitas ? '<i class="ph ph-check-circle"></i> feita' : e.reps + ' reps') + '</span>';
            el.series.appendChild(b);
        }
        el.dica.textContent = feitas >= e.series ? 'Exercício concluído!' : 'Toque numa série ao terminá-la.';
        const ultimo = estado.idx === lista.length - 1;
        el.proximo.innerHTML = ultimo ? 'Concluir <i class="ph ph-flag-checkered"></i>' : 'Próximo <i class="ph ph-caret-right"></i>';
        overlay.querySelector('[data-mt="anterior"]').disabled = estado.idx === 0;
    }

    function relogioTotal() { el.tempo.textContent = fmt((Date.now() - estado.inicio) / 1000); }

    /* ---- descanso ---- */
    function iniciarDescanso(segundos) {
        descansoFim = Date.now() + segundos * 1000;
        el.descanso.hidden = false;
        atualizarDescanso();
        clearInterval(intervalo);
        intervalo = setInterval(atualizarDescanso, 250);
    }
    function pararDescanso() { clearInterval(intervalo); intervalo = null; el.descanso.hidden = true; }
    function atualizarDescanso() {
        const resta = (descansoFim - Date.now()) / 1000;
        if (resta <= 0) {
            pararDescanso();
            seguro(() => { if (navigator.vibrate) navigator.vibrate([200, 100, 200]); });
            return;
        }
        el.descansoTempo.textContent = fmt(Math.ceil(resta));
        el.descansoTempo.classList.toggle('acabando', resta <= 5);
    }

    /* ---- envio do registro (mesmo formulário e token CSRF da tela) ---- */
    function enviar(e) {
        if (estado.enviados[e.id] || visitante) return;
        estado.enviados[e.id] = true;
        salvar();
        const dados = new URLSearchParams();
        const token = el.form.querySelector('input[name="_csrf"]');
        if (token) dados.append('_csrf', token.value);
        dados.append('exercicioId', e.id);
        const kg = estado.cargas[e.id];
        if (kg !== undefined && kg !== '' && kg !== null) dados.append('carga', kg);
        dados.append('observacao', 'Modo treino');
        fetch(el.form.action, { method: 'POST', body: dados, credentials: 'same-origin' })
            .catch(() => { delete estado.enviados[e.id]; salvar(); });
    }

    /* ---- ações ---- */
    function marcarSerie(i) {
        const e = lista[estado.idx];
        const feitas = estado.feitas[e.id] || 0;
        if (i === feitas - 1) {              // toque na última feita desfaz
            estado.feitas[e.id] = feitas - 1;
            pararDescanso();
        } else if (i === feitas) {            // só a próxima série é marcada, em ordem
            estado.feitas[e.id] = feitas + 1;
            seguro(() => { if (navigator.vibrate) navigator.vibrate(30); });
            if (estado.feitas[e.id] >= e.series) enviar(e);
            iniciarDescanso(DESCANSO_PADRAO);
        } else { return; }
        salvar();
        desenhar();
    }

    function guardarCarga() {
        const e = lista[estado.idx];
        estado.cargas[e.id] = el.carga.value;
        salvar();
    }

    function ir(delta) {
        guardarCarga();
        const novo = estado.idx + delta;
        if (novo < 0) return;
        if (novo >= lista.length) { concluir(); return; }
        estado.idx = novo;
        pararDescanso();
        salvar();
        desenhar();
    }

    function concluir() {
        lista.forEach(e => { if ((estado.feitas[e.id] || 0) > 0) enviar(e); });
        encerrado = true;
        pararDescanso();
        clearInterval(relogio);
        soltarWake();
        const exFeitos = lista.filter(e => (estado.feitas[e.id] || 0) >= e.series).length;
        $('mtResExercicios').textContent = exFeitos + ' de ' + lista.length;
        $('mtResSeries').textContent = seriesFeitas();
        $('mtResTempo').textContent = fmt((Date.now() - estado.inicio) / 1000);
        $('mtResMsg').textContent = visitante
            ? 'Modo visitante: o treino não é salvo.'
            : 'Registrado na sua ficha. Seu professor vê a sua adesão.';
        el.corpo.hidden = true; el.rodape.hidden = true; el.resumo.hidden = false;
        estado.concluido = true;
        limpar();
        seguro(() => { if (navigator.vibrate) navigator.vibrate([100, 60, 100, 60, 250]); });
    }

    function abrir() {
        lista = lerFicha();
        if (!lista.length) return;
        estado = carregarOuCriar();
        if (estado.idx >= lista.length) estado.idx = 0;
        encerrado = false;
        if (overlay.parentNode !== document.body) document.body.appendChild(overlay);
        overlay.hidden = false;
        document.body.classList.add('mt-aberto');
        el.corpo.hidden = false; el.rodape.hidden = false; el.resumo.hidden = true; el.descanso.hidden = true;
        el.visitante.hidden = !visitante;
        salvar();
        desenhar();
        relogioTotal();
        clearInterval(relogio);
        relogio = setInterval(relogioTotal, 1000);
        pedirWake();
    }

    function sair() {
        if (!encerrado) guardarCarga();
        pararDescanso();
        clearInterval(relogio);
        soltarWake();
        overlay.hidden = true;
        document.body.classList.remove('mt-aberto');
    }

    botao.addEventListener('click', abrir);
    overlay.addEventListener('click', ev => {
        const serie = ev.target.closest('.mt-serie');
        if (serie) { marcarSerie(parseInt(serie.dataset.serie, 10)); return; }
        const acao = ev.target.closest('[data-mt]');
        if (!acao) return;
        switch (acao.dataset.mt) {
            case 'anterior': ir(-1); break;
            case 'proximo': ir(1); break;
            case 'pular': pararDescanso(); break;
            case 'mais15': descansoFim += 15000; atualizarDescanso(); break;
            case 'sair': sair(); break;
            case 'fechar': sair(); break;
        }
    });
    el.carga.addEventListener('change', guardarCarga);
    document.addEventListener('keydown', ev => { if (ev.key === 'Escape' && !overlay.hidden) sair(); });
})();
