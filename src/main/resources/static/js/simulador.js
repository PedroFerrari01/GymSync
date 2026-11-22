/* Simulador de faturamento (só admin, aba Relatórios). Tudo no navegador: nada é enviado nem salvo. */
(function () {
    'use strict';

    /* ---------- Funções puras ---------- */
    function numero(v, padrao) {
        var n = parseFloat(v);
        return isFinite(n) ? n : (padrao || 0);
    }

    /** Receita de um mês futuro k (1 = próximo mês). Sem valores negativos. */
    function receitaDoMes(base, ctrl, k) {
        var ativos = Math.max(0, numero(base.ativos) + k * (numero(ctrl.novas) - numero(ctrl.cancelamentos)));
        var ticket = Math.max(0, numero(base.ticket)) * (1 + numero(ctrl.reajuste) / 100);
        var pago = 1 - Math.min(100, Math.max(0, numero(ctrl.inadimplencia))) / 100;
        return { ativos: ativos, ticket: ticket, receita: ativos * ticket * pago };
    }

    /** Projeção dos próximos n meses. */
    function projetar(base, ctrl, n) {
        var lista = [];
        for (var k = 1; k <= n; k++) lista.push(receitaDoMes(base, ctrl, k));
        return lista;
    }

    /** Receita de hoje, já descontando a inadimplência de hoje (ponto de comparação). */
    function receitaAtual(base) {
        return numero(base.receita) * (1 - Math.min(100, Math.max(0, numero(base.inadimplencia))) / 100);
    }

    /** Diferença entre dois valores: { reais, pct } (pct nulo se a base é zero). */
    function variacao(novo, antigo) {
        var reais = novo - antigo;
        return { reais: reais, pct: antigo > 0 ? reais / antigo * 100 : null };
    }

    /** Quantas matrículas novas faltam no 1º mês para a meta. 0 = já atingida; null = sem ticket. */
    function faltamMatriculas(meta, base, ctrl) {
        var alvo = numero(meta);
        if (alvo <= 0) return null;
        var mes = receitaDoMes(base, ctrl, 1);
        var porAluno = mes.ticket * (1 - Math.min(100, Math.max(0, numero(ctrl.inadimplencia))) / 100);
        if (mes.receita >= alvo) return 0;
        if (porAluno <= 0) return null;
        return Math.ceil((alvo - mes.receita) / porAluno);
    }

    var api = { receitaDoMes: receitaDoMes, projetar: projetar, receitaAtual: receitaAtual,
                variacao: variacao, faltamMatriculas: faltamMatriculas };
    if (typeof module !== 'undefined' && module.exports) module.exports = api;
    window.GymSyncSimulador = api;

    /* ---------- Tela ---------- */
    var raiz = document.getElementById('simulador');
    if (!raiz) return;

    var moeda = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' });
    var inteiro = new Intl.NumberFormat('pt-BR', { maximumFractionDigits: 0 });
    var decimal = new Intl.NumberFormat('pt-BR', { maximumFractionDigits: 1 });
    var base = {
        ativos: numero(raiz.dataset.ativos),
        ticket: numero(raiz.dataset.ticket),
        receita: numero(raiz.dataset.receita),
        inadimplencia: numero(raiz.dataset.inad)
    };
    var padrao = { novas: 0, cancelamentos: 0, reajuste: 0, inadimplencia: base.inadimplencia };

    function el(nome) { return raiz.querySelector('[data-sim="' + nome + '"]'); }
    var campos = ['novas', 'cancelamentos', 'reajuste', 'inadimplencia'];

    function ler() {
        var c = {};
        campos.forEach(function (n) { c[n] = numero(el(n).value, padrao[n]); });
        return c;
    }

    function atualizar() {
        var c = ler();
        campos.forEach(function (n) { el(n + '-valor').textContent = (n === 'inadimplencia' ? decimal : inteiro).format(c[n]) + (n === 'novas' || n === 'cancelamentos' ? '' : '%'); });

        var proj = projetar(base, c, 6);
        var atual = receitaAtual(base);
        var prox = proj[0].receita;
        var v = variacao(prox, atual);

        el('atual').textContent = moeda.format(atual);
        el('prox').textContent = moeda.format(prox);
        var dif = el('dif');
        var sinal = v.reais > 0 ? '+' : '';
        dif.textContent = sinal + moeda.format(v.reais) + (v.pct === null ? '' : ' (' + sinal + v.pct.toFixed(1).replace('.', ',') + '%)');
        dif.className = 'sim-dif ' + (v.reais > 0 ? 'sim-sobe' : v.reais < 0 ? 'sim-desce' : '');
        el('ativos6').textContent = inteiro.format(proj[5].ativos) + ' alunos ativos em 6 meses';

        var max = Math.max(atual, 1);
        proj.forEach(function (p) { max = Math.max(max, p.receita); });
        var barras = el('barras');
        barras.textContent = '';
        [{ rot: 'Hoje', val: atual, hoje: true }].concat(proj.map(function (p, i) { return { rot: '+' + (i + 1) + 'm', val: p.receita }; }))
            .forEach(function (b) {
                var col = document.createElement('div');
                col.className = 'sim-col';
                var valor = document.createElement('span');
                valor.className = 'sim-val';
                valor.textContent = moeda.format(b.val);
                var trilho = document.createElement('div');
                trilho.className = 'sim-trilho';
                var barra = document.createElement('div');
                barra.className = 'sim-barra' + (b.hoje ? ' sim-barra-hoje' : '');
                barra.style.height = Math.round(b.val / max * 100) + '%';
                trilho.appendChild(barra);
                var rot = document.createElement('span');
                rot.className = 'sim-rot';
                rot.textContent = b.rot;
                col.appendChild(valor);
                col.appendChild(trilho);
                col.appendChild(rot);
                barras.appendChild(col);
            });

        var meta = el('meta').value.replace(/\./g, '').replace(',', '.');
        var f = faltamMatriculas(meta, base, c);
        var saida = el('meta-resultado');
        if (numero(meta) <= 0) saida.textContent = 'Digite uma meta mensal em R$ para ver quantas matrículas faltam.';
        else if (f === 0) saida.textContent = 'Meta já atingida com este cenário.';
        else if (f === null) saida.textContent = 'Sem mensalidade média para calcular.';
        else saida.textContent = 'Faltam ' + inteiro.format(f) + (f === 1 ? ' matrícula nova' : ' matrículas novas') + ' por mês (além das já simuladas).';
    }

    campos.concat(['meta']).forEach(function (n) { el(n).addEventListener('input', atualizar); });
    el('reset').addEventListener('click', function () {
        campos.forEach(function (n) { el(n).value = padrao[n]; });
        el('meta').value = '';
        atualizar();
    });
    campos.forEach(function (n) { el(n).value = padrao[n]; });
    atualizar();
})();
