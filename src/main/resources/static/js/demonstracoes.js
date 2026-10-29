/* Demonstrações do "Quem somos" (fragments/demonstracoes.html).
   Simulam no navegador as regras do sistema, com dados de exemplo. Nada é enviado ao servidor. */
document.addEventListener('DOMContentLoaded', () => {
    const galeria = document.querySelector('.galeria');
    if (!galeria) return;

    // Abas da bancada: uma demonstração por vez. Setas, Home e End trocam de aba pelo teclado.
    const abas = [...galeria.querySelectorAll('.bancada-aba')];
    const paineis = abas.map(aba => document.getElementById(aba.getAttribute('aria-controls')));
    const escolher = (indice, focar) => {
        abas.forEach((aba, i) => {
            const ativa = i === indice;
            aba.setAttribute('aria-selected', ativa);
            aba.tabIndex = ativa ? 0 : -1;
            paineis[i].classList.toggle('ativo', ativa);
            paineis[i].inert = !ativa;
            paineis[i].classList.remove('entrando');
        });
        void paineis[indice].offsetWidth; // reinicia a animação de entrada
        paineis[indice].classList.add('entrando');
        if (focar) abas[indice].focus();
        abas[indice].scrollIntoView({ block: 'nearest', inline: 'nearest' });
    };
    abas.forEach((aba, i) => {
        if (i > 0) paineis[i].inert = true;
        aba.addEventListener('click', () => escolher(i, false));
        aba.addEventListener('keydown', e => {
            const ultimo = abas.length - 1;
            const destino = { ArrowRight: i === ultimo ? 0 : i + 1, ArrowLeft: i === 0 ? ultimo : i - 1, Home: 0, End: ultimo }[e.key];
            if (destino === undefined) return;
            e.preventDefault();
            escolher(destino, true);
        });
    });

    const resultado = (cartao, texto, tipo) => {
        const p = cartao.querySelector('.demo-resultado');
        p.textContent = texto;
        p.className = 'demo-resultado ' + (tipo || '');
    };

    // Catraca: mesma regra do sistema (aluno ativo, matrícula ativa e nada em atraso)
    galeria.querySelectorAll('.demo-catraca').forEach(cartao => {
        const acessos = cartao.querySelector('.demo-acessos');
        const digital = cartao.querySelector('.demo-digital');
        cartao.querySelector('.demo-liberar').addEventListener('click', () => {
            const select = cartao.querySelector('.demo-aluno');
            const nome = select.options[select.selectedIndex].text.replace(/ \(.*\)$/, '');
            digital.classList.remove('lendo');
            void digital.offsetWidth; // reinicia a animação da digital
            digital.classList.add('lendo');
            if (select.value === 'ok') {
                acessos.textContent = Number(acessos.textContent) + 1;
                resultado(cartao, `Entrada liberada para ${nome}. Bom treino!`, 'ok');
            } else if (select.value === 'atraso') {
                resultado(cartao, `Catraca bloqueada: ${nome} tem mensalidade em atraso.`, 'erro');
            } else {
                resultado(cartao, `Catraca bloqueada: ${nome} está inativa no sistema.`, 'erro');
            }
        });
    });

    // Ficha: marcar o exercício como feito hoje (clicar de novo desfaz)
    galeria.querySelectorAll('.demo-ficha').forEach(cartao => {
        const botoes = [...cartao.querySelectorAll('.demo-feito')];
        const progresso = cartao.querySelector('.demo-progresso');
        const atualizar = () => {
            const feitos = botoes.filter(b => b.classList.contains('ativo')).length;
            progresso.textContent = feitos === botoes.length ? 'Treino completo!' : `${feitos} de ${botoes.length} feitos hoje`;
            progresso.classList.toggle('status-ok', feitos === botoes.length);
        };
        botoes.forEach(botao => botao.addEventListener('click', () => {
            const linha = botao.closest('tr');
            const ultima = linha.querySelector('.demo-ultima');
            const marcado = botao.classList.toggle('ativo');
            linha.classList.toggle('feito', marcado);
            if (marcado) {
                ultima.dataset.antes = ultima.textContent;
                ultima.textContent = 'Hoje';
            } else {
                ultima.textContent = ultima.dataset.antes;
            }
            atualizar();
        }));
    });

    // Mensalidade: Pagar -> Pix simulado -> EM ANÁLISE -> administrador confirma -> PAGA
    galeria.querySelectorAll('.demo-mensalidades').forEach(cartao => {
        const lista = cartao.querySelector('.demo-lista');
        const pix = cartao.querySelector('.demo-pix');
        const situacao = cartao.querySelector('.demo-situacao');
        const pagar = cartao.querySelector('.demo-pagar');
        const confirmar = cartao.querySelector('.demo-confirmar');
        const reiniciar = cartao.querySelector('.demo-reiniciar');
        const copiar = cartao.querySelector('.demo-copiar');
        const mudarSituacao = (texto, classe) => { situacao.textContent = texto; situacao.className = classe + ' demo-situacao'; };

        pagar.addEventListener('click', () => { lista.hidden = true; pix.hidden = false; resultado(cartao, ''); });
        cartao.querySelector('.demo-voltar').addEventListener('click', () => { pix.hidden = true; lista.hidden = false; });
        copiar.addEventListener('click', () => {
            navigator.clipboard?.writeText(cartao.querySelector('.pix-codigo').value).catch(() => {});
            copiar.textContent = 'Copiado';
            setTimeout(() => { copiar.textContent = 'Copiar'; }, 1500);
        });
        cartao.querySelector('.demo-paguei').addEventListener('click', () => {
            pix.hidden = true;
            lista.hidden = false;
            pagar.hidden = true;
            mudarSituacao('EM ANÁLISE', 'status-info');
            resultado(cartao, 'Pagamento informado. Agora o administrador confere.', 'info');
            confirmar.hidden = false;
        });
        confirmar.addEventListener('click', () => {
            mudarSituacao('PAGA', 'status-ok');
            resultado(cartao, 'Pagamento confirmado pelo administrador.', 'ok');
            confirmar.hidden = true;
            reiniciar.hidden = false;
        });
        reiniciar.addEventListener('click', () => {
            mudarSituacao('PENDENTE', 'status-wait');
            pagar.hidden = false;
            reiniciar.hidden = true;
            resultado(cartao, '');
        });
    });

    // Aulas: reservar e cancelar vaga; sem vaga, a aula aparece como lotada
    galeria.querySelectorAll('.demo-aulas tbody tr').forEach(linha => {
        const total = Number(linha.dataset.total);
        let vagas = Number(linha.dataset.vagas);
        let reservada = false;
        const desenhar = () => {
            const span = linha.querySelector('.demo-vagas');
            span.textContent = `${vagas} de ${total}`;
            span.className = 'demo-vagas ' + (vagas > 0 ? 'status-ok' : 'status-err');
            const acao = linha.querySelector('.demo-acao');
            acao.innerHTML = '';
            if (reservada || vagas > 0) {
                const botao = document.createElement('button');
                botao.type = 'button';
                botao.className = reservada ? 'btn-theme btn-sm' : 'btn btn-sm btn-success fw-semibold';
                botao.textContent = reservada ? 'Cancelar' : 'Reservar';
                botao.addEventListener('click', () => {
                    reservada = !reservada;
                    vagas += reservada ? -1 : 1;
                    desenhar();
                    linha.querySelector('.demo-acao button').focus();
                });
                acao.appendChild(botao);
            } else {
                acao.innerHTML = '<span class="small demo-sub">Lotada</span>';
            }
        };
        desenhar();
    });

    // Relatório: troca o período e as barras crescem até o novo valor
    galeria.querySelectorAll('.demo-relatorio').forEach(cartao => {
        const horas = ['6h', '8h', '10h', '12h', '14h', '16h', '18h', '20h', '22h'];
        const dados = { 7: [5, 21, 9, 7, 12, 16, 34, 25, 6], 30: [22, 74, 41, 30, 47, 69, 131, 98, 27] };
        const grafico = cartao.querySelector('.demo-grafico');
        const pico = cartao.querySelector('.demo-pico');
        grafico.innerHTML = horas.map(h =>
            `<div class="vcol"><span class="vval"></span><div class="vtrack"><div class="vbar" style="height:0%"></div></div><span class="vlbl">${h}</span></div>`).join('');
        const colunas = [...grafico.querySelectorAll('.vcol')];
        const mostrar = periodo => {
            const valores = dados[periodo];
            const maior = Math.max(...valores);
            colunas.forEach((col, i) => {
                col.querySelector('.vval').textContent = valores[i];
                col.querySelector('.vbar').style.height = Math.round(valores[i] / maior * 100) + '%';
            });
            const h = valores.indexOf(maior);
            pico.textContent = `Pico: ${horas[h]} às ${parseInt(horas[h], 10) + 2}h (${maior} entradas)`;
            cartao.querySelectorAll('.demo-periodo-btn').forEach(b => {
                const ativo = b.dataset.periodo === String(periodo);
                b.classList.toggle('ativo', ativo);
                b.setAttribute('aria-pressed', ativo);
            });
        };
        cartao.querySelectorAll('.demo-periodo-btn').forEach(b => b.addEventListener('click', () => mostrar(b.dataset.periodo)));
        requestAnimationFrame(() => mostrar(7));
    });
});
