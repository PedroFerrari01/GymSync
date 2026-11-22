/* Dicas guiadas: tour da primeira entrada por perfil.
   Tudo no navegador: nenhuma requisição. "Já viu" fica no localStorage (chave por perfil). */
(function () {
    'use strict';

    var box = document.querySelector('.perfil-box .small.mb-2 span');
    var perfil = box ? box.textContent.trim().toUpperCase() : '';
    if (['ADMIN', 'PROFESSOR', 'ALUNO'].indexOf(perfil) < 0) return;

    var reduzir = window.matchMedia && window.matchMedia('(prefers-reduced-motion: reduce)').matches;
    var CHAVE = 'gymsync_tour_visto_' + perfil;

    function lerVisto() { try { return localStorage.getItem(CHAVE) === '1'; } catch (e) { return false; } }
    function gravarVisto() { try { localStorage.setItem(CHAVE, '1'); } catch (e) { /* sem armazenamento: o tour volta a aparecer */ } }

    var PASSOS = {
        ADMIN: [
            { alvo: '#menuLateral', titulo: 'Bem-vindo ao GymSync', texto: 'Este é o menu. Cada item abre uma área: alunos, finanças, relatórios, treinos e equipamentos.' },
            { alvo: '#btn-aba-painel', titulo: 'Painel & Catraca', texto: 'A visão do dia: avisos importantes e o botão para liberar a catraca de um aluno.' },
            { alvo: '#btn-aba-alunos', titulo: 'Alunos & Matrículas', texto: 'Cadastre alunos, professores e matrículas. Mensalidades em atraso bloqueiam a catraca.' },
            { alvo: '#btn-aba-financeiro', titulo: 'Planos & Financeiro', texto: 'Planos, mensalidades e confirmação dos pagamentos que os alunos informam por Pix.' },
            { alvo: '#btn-aba-relatorios', titulo: 'Relatórios', texto: 'Faturamento, comparação com o mês passado, exportação em planilha e histórico de ações.' }
        ],
        PROFESSOR: [
            { alvo: '#menuLateral', titulo: 'Bem-vindo ao GymSync', texto: 'Este é o seu menu. Aqui ficam os treinos dos alunos, avaliações, aulas e mensagens.' },
            { alvo: '#btn-aba-treinos', titulo: 'Treinos & Equipe', texto: 'Monte e copie fichas de treino e veja os alunos sumidos e a adesão da semana.' },
            { alvo: '#btn-aba-avaliacoes', titulo: 'Avaliações Físicas', texto: 'Registre medidas e acompanhe a evolução de cada aluno.' },
            { alvo: '#btn-aba-aulas', titulo: 'Aulas Coletivas', texto: 'Veja as aulas, horários e quem reservou vaga.' },
            { alvo: 'a[href="/mensagens"]', titulo: 'Mensagens', texto: 'Converse com os alunos. O número vermelho mostra o que ainda não foi lido.' }
        ],
        ALUNO: [
            { alvo: '#menuLateral', titulo: 'Bem-vindo ao GymSync', texto: 'Este é o seu menu: sua área, avaliações, aulas e mensagens com o professor.' },
            { alvo: '#btn-aba-portal', titulo: 'Minha Área', texto: 'Sua ficha de treino, mensalidades, calendário de frequência e avisos de vencimento.' },
            { alvo: '#btn-aba-avaliacoes', titulo: 'Avaliações Físicas', texto: 'Veja sua evolução: primeira avaliação contra a última, com gráfico.' },
            { alvo: '#btn-aba-aulas', titulo: 'Aulas Coletivas', texto: 'Escolha uma aula e reserve sua vaga.' },
            { alvo: '#btnTema', titulo: 'Modo claro ou escuro', texto: 'Troque o visual quando quiser.' }
        ]
    };

    var DICAS = {
        'aba-painel': 'Resumo do dia. Os avisos mostram o que precisa de atenção; o botão "Liberar catraca" simula a entrada de um aluno.',
        'aba-alunos': 'Cadastros de alunos, professores e matrículas. Use a busca do topo para achar alguém rápido.',
        'aba-financeiro': 'Planos e mensalidades. Pagamentos informados pelo aluno ficam "em análise" até você confirmar.',
        'aba-relatorios': 'Números do mês, comparação com o mês anterior, exportação em planilha e histórico de ações.',
        'aba-treinos': 'Fichas de treino por aluno. Use "copiar ficha" para reaproveitar um modelo e confira os alunos sumidos.',
        'aba-avaliacoes': 'Medidas e evolução física. Compare a primeira avaliação com a mais recente.',
        'aba-aulas': 'Aulas coletivas, horários e vagas. O aluno reserva por aqui.',
        'aba-equipamentos': 'Aparelhos da academia, fotos e manutenções.',
        'aba-portal': 'Sua área: ficha de treino, mensalidades, frequência no mês e avisos.'
    };

    var balao = null, passos = [], i = 0, destaque = null, anterior = null;

    function visivel(el) {
        if (!el) return false;
        var r = el.getBoundingClientRect();
        return r.width > 0 && r.height > 0 && r.right > 0 && r.bottom > 0 &&
            r.left < window.innerWidth && r.top < window.innerHeight;
    }

    function limparDestaque() {
        if (destaque) destaque.classList.remove('tour-alvo');
        destaque = null;
    }

    function fechar(marcar) {
        limparDestaque();
        if (balao) { balao.remove(); balao = null; }
        document.removeEventListener('keydown', teclas, true);
        window.removeEventListener('resize', posicionar);
        if (marcar) gravarVisto();
        if (anterior && document.contains(anterior)) { try { anterior.focus(); } catch (e) { } }
        anterior = null;
    }

    function teclas(e) {
        if (e.key === 'Escape') { e.preventDefault(); fechar(true); }
        else if (e.key === 'ArrowRight') { e.preventDefault(); ir(1); }
        else if (e.key === 'ArrowLeft') { e.preventDefault(); ir(-1); }
    }

    function posicionar() {
        if (!balao) return;
        balao.style.top = ''; balao.style.left = '';
        balao.classList.remove('tour-livre');
        if (!destaque || !visivel(destaque) || window.innerWidth < 768) {
            balao.classList.add('tour-livre'); // cartão fixo (celular ou alvo escondido)
            return;
        }
        var r = destaque.getBoundingClientRect();
        var w = balao.offsetWidth, h = balao.offsetHeight, m = 14;
        var esq = r.right + m;
        var topo = r.top + r.height / 2 - h / 2;
        if (esq + w > window.innerWidth - 12) { // não cabe à direita: abaixo do alvo
            esq = Math.min(Math.max(12, r.left), window.innerWidth - w - 12);
            topo = r.bottom + m;
            if (topo + h > window.innerHeight - 12) topo = Math.max(12, r.top - h - m);
        }
        topo = Math.min(Math.max(12, topo), window.innerHeight - h - 12);
        balao.style.left = esq + 'px';
        balao.style.top = topo + 'px';
    }

    function mostrar() {
        var p = passos[i];
        limparDestaque();
        var el = document.querySelector(p.alvo);
        if (el) { destaque = el; el.classList.add('tour-alvo'); if (visivel(el)) el.scrollIntoView({ block: 'nearest' }); }

        if (!balao) {
            balao = document.createElement('div');
            balao.className = 'tour-balao' + (reduzir ? ' sem-anim' : '');
            balao.setAttribute('role', 'dialog');
            balao.setAttribute('aria-label', 'Tour guiado do GymSync');
            balao.tabIndex = -1;
            balao.addEventListener('click', function (e) {
                var b = e.target.closest('[data-tour-acao]');
                if (!b) return;
                var a = b.getAttribute('data-tour-acao');
                if (a === 'prox') { if (i >= passos.length - 1) fechar(true); else ir(1); }
                else if (a === 'volta') ir(-1);
                else fechar(true);
            });
            document.body.appendChild(balao);
            document.addEventListener('keydown', teclas, true);
            window.addEventListener('resize', posicionar);
        }
        var ultimo = i === passos.length - 1;
        balao.innerHTML =
            '<div class="tour-topo"><span class="tour-conta">' + (i + 1) + ' de ' + passos.length + '</span>' +
            '<button type="button" class="tour-pular" data-tour-acao="pular">Pular</button></div>' +
            '<h3 class="tour-titulo"></h3><p class="tour-texto"></p>' +
            '<div class="tour-botoes">' +
            '<button type="button" class="tour-btn" data-tour-acao="volta"' + (i === 0 ? ' disabled' : '') + '>Voltar</button>' +
            '<button type="button" class="tour-btn tour-btn-prim" data-tour-acao="prox">' + (ultimo ? 'Concluir' : 'Próximo') + '</button></div>';
        balao.querySelector('.tour-titulo').textContent = p.titulo;
        balao.querySelector('.tour-texto').textContent = p.texto;
        posicionar();
        balao.focus({ preventScroll: true });
    }

    function ir(d) {
        var n = i + d;
        if (n < 0 || n >= passos.length) return;
        i = n;
        mostrar();
    }

    function iniciar() {
        fechar(false);
        passos = PASSOS[perfil];
        i = 0;
        anterior = document.activeElement;
        mostrar();
    }

    document.addEventListener('click', function (e) {
        if (e.target.closest('#btnVerTour')) { iniciar(); return; }
    });

    if (!lerVisto()) setTimeout(iniciar, 700);
})();
