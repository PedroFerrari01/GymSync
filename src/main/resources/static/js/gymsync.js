const htmlEl = document.documentElement;
let temaSalvo = localStorage.getItem('gymsync_tema') || 'dark';

function aplicarTema() {
    htmlEl.setAttribute('data-theme', temaSalvo);
    const icone = document.getElementById('iconeTema') || document.getElementById('iconeTemaLogin');
    const texto = document.getElementById('textoTema') || document.getElementById('textoTemaLogin');
    if (!icone || !texto) return;
    if (temaSalvo === 'light') {
        icone.className = 'bi bi-moon-stars-fill';
        texto.textContent = 'Modo Escuro';
    } else {
        icone.className = 'bi bi-sun-fill';
        texto.textContent = 'Modo Claro';
    }
}
aplicarTema();

function alternarSenha(idCampo, btn) {
    const campo = document.getElementById(idCampo);
    const icone = btn.querySelector('i');
    if (campo.type === 'password') {
        campo.type = 'text';
        icone.className = 'bi bi-eye-slash';
    } else {
        campo.type = 'password';
        icone.className = 'bi bi-eye';
    }
}

function formatarCpf(valor) {
    let v = valor.replace(/\D/g, '').slice(0, 11);
    if (v.length > 9) v = v.replace(/(\d{3})(\d{3})(\d{3})(\d{1,2})/, '$1.$2.$3-$4');
    else if (v.length > 6) v = v.replace(/(\d{3})(\d{3})(\d{1,3})/, '$1.$2.$3');
    else if (v.length > 3) v = v.replace(/(\d{3})(\d{1,3})/, '$1.$2');
    return v;
}

document.querySelectorAll('#btnTema, #btnTemaLogin').forEach(btn => {
    btn.addEventListener('click', () => {
        temaSalvo = htmlEl.getAttribute('data-theme') === 'dark' ? 'light' : 'dark';
        localStorage.setItem('gymsync_tema', temaSalvo);
        aplicarTema();
    });
});

document.querySelectorAll('.role-btn').forEach(btn => {
    btn.addEventListener('click', function () {
        document.querySelectorAll('.role-btn').forEach(b => b.classList.remove('active'));
        this.classList.add('active');
        const perfil = this.getAttribute('data-role');
        document.getElementById('inputTipoLogin').value = perfil;

        const blocoCpf = document.getElementById('blocoCpfLogin');
        const blocoAdmin = document.getElementById('blocoAdminLogin');
        const inputIdentificador = document.getElementById('inputIdentificadorLogin');
        const inputUser = document.getElementById('inputUserAdmin');
        const linkRecuperar = document.getElementById('linkRecuperarSenha');

        if (perfil === 'ADMIN') {
            blocoCpf.style.display = 'none';
            blocoAdmin.style.display = 'block';
            inputIdentificador.removeAttribute('required');
            inputUser.setAttribute('required', 'required');
            if (linkRecuperar) linkRecuperar.style.display = 'none';
        } else {
            blocoCpf.style.display = 'block';
            blocoAdmin.style.display = 'none';
            inputIdentificador.setAttribute('required', 'required');
            inputUser.removeAttribute('required');
            if (linkRecuperar) linkRecuperar.style.display = 'inline';
            document.getElementById('labelCpfLogin').textContent =
                perfil === 'PROFESSOR' ? 'CPF ou E-mail do Professor' : 'CPF ou E-mail do Aluno';
        }
    });
});

const btnMenu = document.getElementById('btnMenu');
const sidebar = document.querySelector('.sidebar');
if (btnMenu && sidebar) {
    btnMenu.addEventListener('click', () => sidebar.classList.toggle('aberta'));
}

document.querySelectorAll('#menuAbas button').forEach(btn => {
    btn.addEventListener('shown.bs.tab', e => {
        localStorage.setItem('gymsync_aba_ativa', e.target.id);
        if (sidebar) sidebar.classList.remove('aberta');
    });
});

const abaSalva = localStorage.getItem('gymsync_aba_ativa');
if (abaSalva && document.getElementById(abaSalva)) {
    new bootstrap.Tab(document.getElementById(abaSalva)).show();
}

const buscaGlobal = document.getElementById('inputBuscaGlobal');

document.querySelectorAll('form[data-confirm]').forEach(form => {
    form.addEventListener('submit', e => {
        if (!confirm(form.dataset.confirm)) e.preventDefault();
    });
});

document.querySelectorAll('.campo-apenas-letras').forEach(input => {
    input.addEventListener('input', function () {
        this.value = this.value.replace(/[^a-zA-ZÀ-ÿ\s]/g, '');
    });
});

document.querySelectorAll('.mascara-cpf').forEach(input => {
    input.addEventListener('input', function () {
        this.value = formatarCpf(this.value);
    });
});

document.querySelectorAll('.campo-cpf-ou-email').forEach(input => {
    input.addEventListener('input', function () {
        if (/^[\d.\-]+$/.test(this.value)) this.value = formatarCpf(this.value);
    });
});

document.querySelectorAll('.mascara-telefone').forEach(input => {
    input.addEventListener('input', function () {
        let v = this.value.replace(/\D/g, '').slice(0, 11);
        if (v.length > 10) v = v.replace(/^(\d{2})(\d{5})(\d{4}).*/, '($1) $2-$3');
        else if (v.length > 6) v = v.replace(/^(\d{2})(\d{4})(\d{0,4}).*/, '($1) $2-$3');
        else if (v.length > 2) v = v.replace(/^(\d{2})(\d{0,5})/, '($1) $2');
        this.value = v;
    });
});

document.querySelectorAll('.campo-data-4digitos').forEach(input => {
    input.max = new Date().toISOString().slice(0, 10);
    input.addEventListener('input', function () {
        if (!this.value) return;
        const partes = this.value.split('-');
        if (partes[0] && partes[0].length > 4) {
            partes[0] = partes[0].slice(0, 4);
            this.value = partes.join('-');
        }
    });
});

document.querySelectorAll('.btn-fill').forEach(btn => {
    btn.addEventListener('click', () => {
        const form = document.getElementById(btn.dataset.form);
        if (!form) return;
        if (btn.dataset.action) form.action = btn.dataset.action;
        Object.keys(btn.dataset).forEach(chave => {
            if (!chave.startsWith('campo')) return;
            const nome = chave.charAt(5).toLowerCase() + chave.slice(6);
            const campo = form.elements[nome];
            if (campo) campo.value = btn.dataset[chave];
        });
    });
});

document.querySelectorAll('.campo-hoje').forEach(input => {
    if (!input.value) input.value = new Date().toISOString().slice(0, 10);
});

const selectFiltroStatus = document.getElementById('filtroStatusMensalidade');
function aplicarFiltros() {
    const termo = buscaGlobal ? buscaGlobal.value.toLowerCase() : '';
    const status = selectFiltroStatus ? selectFiltroStatus.value : '';
    document.querySelectorAll('.tabela-filtravel tbody tr').forEach(tr => {
        const passaBusca = tr.textContent.toLowerCase().includes(termo);
        const passaStatus = !status || !tr.dataset.status || tr.dataset.status === status;
        tr.style.display = passaBusca && passaStatus ? '' : 'none';
    });
}
if (buscaGlobal) buscaGlobal.addEventListener('input', aplicarFiltros);
if (selectFiltroStatus) selectFiltroStatus.addEventListener('change', aplicarFiltros);

const btnCopiarPix = document.getElementById('btnCopiarPix');
if (btnCopiarPix) {
    btnCopiarPix.addEventListener('click', () => {
        const campo = document.getElementById('pixCodigo');
        campo.select();
        if (navigator.clipboard && window.isSecureContext) {
            navigator.clipboard.writeText(campo.value);
        } else {
            document.execCommand('copy');
        }
        btnCopiarPix.textContent = 'Copiado!';
        setTimeout(() => { btnCopiarPix.textContent = 'Copiar'; }, 2000);
    });
}

document.querySelectorAll('.alert-dismissible').forEach(alerta => {
    setTimeout(() => bootstrap.Alert.getOrCreateInstance(alerta).close(), 7000);
});

const chatBox = document.getElementById('chatBox');
if (chatBox) chatBox.scrollTop = chatBox.scrollHeight;

document.querySelectorAll('.btn-pagar').forEach(btn => {
    btn.addEventListener('click', () => {
        const form = document.getElementById('formPagarPix');
        // O botão do aviso de vencimento traz valor e data nele mesmo; o da tabela lê da linha
        const linha = btn.closest('tr');
        if (!form || (!linha && !btn.dataset.valor)) return;
        form.action = '/aluno/mensalidades/informar/' + btn.dataset.id;
        form.elements['valor'].value = btn.dataset.valor || linha.querySelector('.cel-valor').textContent.trim();
        form.elements['vencimento'].value = btn.dataset.vencimento || linha.querySelector('.cel-venc').textContent.trim();
        form.elements['codigo'].value = 'GYMSYNC-PIX-SIMULADO-' + btn.dataset.id;
    });
});

document.querySelectorAll('.btn-registrar').forEach(btn => {
    btn.addEventListener('click', () => {
        const form = document.getElementById('formRegistrarExercicio');
        const linha = btn.closest('tr');
        if (!form || !linha) return;
        form.elements['exercicioId'].value = btn.dataset.id;
        form.elements['nomeExercicio'].value = linha.querySelector('.cel-exercicio').textContent.trim();
    });
});

/* Entrada suave dos cards de KPI (GSAP). Só roda se o GSAP carregou e o usuário não pediu menos movimento. */
document.addEventListener('DOMContentLoaded', () => {
    if (typeof gsap === 'undefined' || window.matchMedia('(prefers-reduced-motion: reduce)').matches) return;
    gsap.from('.kpi-card', { opacity: 0, y: 20, duration: 0.6, stagger: 0.1, ease: 'power2.out' });
    document.querySelectorAll('.kpi-val').forEach(el => {
        const final = parseInt(el.textContent, 10);
        if (Number.isNaN(final)) return;
        const contador = { valor: 0 };
        gsap.to(contador, {
            valor: final, duration: 0.9, ease: 'power2.out', delay: 0.2,
            onUpdate: () => { el.textContent = Math.round(contador.valor); }
        });
    });
});

/* Saudação e data da tela inicial de aluno e professor */
document.addEventListener('DOMContentLoaded', () => {
    const saudacao = document.getElementById('boasSaudacao');
    const data = document.getElementById('boasData');
    if (!saudacao || !data) return;
    const hora = new Date().getHours();
    saudacao.textContent = hora < 12 ? 'Bom dia' : hora < 18 ? 'Boa tarde' : 'Boa noite';
    data.textContent = new Date().toLocaleDateString('pt-BR', { weekday: 'long', day: 'numeric', month: 'long' });
});

/* Entrada da tela de login (GSAP): título, lista e cartão de login aparecem em sequência */
document.addEventListener('DOMContentLoaded', () => {
    if (typeof gsap === 'undefined' || !document.querySelector('.login-wrapper')) return;
    if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) return;
    gsap.from('.login-hero > *', { opacity: 0, y: 16, duration: 0.6, stagger: 0.12, ease: 'power2.out' });
    gsap.from('.login-card', { opacity: 0, y: 24, duration: 0.7, delay: 0.25, ease: 'power2.out' });
});

/* Transição login -> sistema: a tela de login some suavemente antes do envio, e o painel entra com fade */
document.addEventListener('DOMContentLoaded', () => {
    if (typeof gsap === 'undefined' || window.matchMedia('(prefers-reduced-motion: reduce)').matches) return;
    const loginForm = document.querySelector('.login-wrapper form');
    if (loginForm) {
        loginForm.addEventListener('submit', e => {
            const botao = e.submitter;
            const semValidar = botao && botao.formNoValidate; // "Testar como visitante" não precisa de CPF e senha
            if (!semValidar && !loginForm.checkValidity()) return;
            e.preventDefault();
            if (botao && botao.hasAttribute('formaction')) loginForm.action = botao.formAction;
            let enviado = false;
            const enviar = () => { if (!enviado) { enviado = true; loginForm.submit(); } };
            gsap.to('.login-wrapper', { opacity: 0, y: -12, duration: 0.35, ease: 'power2.in', onComplete: enviar });
            setTimeout(enviar, 700); // garante o envio mesmo se a animação não rodar (aba em segundo plano)
        });
    }
    if (document.querySelector('.layout-wrapper')) {
        gsap.from('.layout-wrapper', { opacity: 0, duration: 0.45, ease: 'power2.out', clearProps: 'opacity' });
    }
});

/* Troca de abas: os cartões da aba aparecem em sequência (fade + subida leve). Também roda na aba aberta ao carregar. */
document.addEventListener('DOMContentLoaded', () => {
    if (typeof gsap === 'undefined' || window.matchMedia('(prefers-reduced-motion: reduce)').matches) return;
    const animarAba = aba => {
        const itens = aba.querySelectorAll('.panel-card, .mini-kpi');
        if (!itens.length) return;
        // Abas com poucos cartões usam intervalo maior, para durarem quase o mesmo que as cheias (ex.: Relatórios)
        const intervalo = itens.length <= 4 ? 0.3 : 0.12;
        gsap.from(itens, { opacity: 0, y: 28, duration: 0.9, delay: 0.12, stagger: intervalo, ease: 'power2.out', clearProps: 'opacity,transform' });
    };
    document.querySelectorAll('[data-bs-toggle="pill"]').forEach(btn => {
        btn.addEventListener('shown.bs.tab', e => {
            const alvo = document.querySelector(e.target.dataset.bsTarget);
            if (alvo) animarAba(alvo);
        });
    });
    const aberta = document.querySelector('.tab-pane.active');
    if (aberta) animarAba(aberta);
});

/* Quem somos (abaixo do login): frase que acende com a rolagem, jornada presa na tela e perfis */
document.addEventListener('DOMContentLoaded', () => {
    const sobre = document.querySelector('.sobre');
    if (!sobre) return;

    // Volta ao login de uma vez (rolar a página inteira passando pela jornada presa travava) e o cartão entra com fade
    document.querySelectorAll('.sobre-voltar').forEach(link => link.addEventListener('click', e => {
        e.preventDefault();
        window.scrollTo({ top: 0, behavior: 'instant' });
        if (typeof gsap !== 'undefined' && !window.matchMedia('(prefers-reduced-motion: reduce)').matches) {
            gsap.from('.login-card', { opacity: 0, y: 24, duration: 0.6, ease: 'power2.out', clearProps: 'opacity,transform' });
        }
        document.getElementById('inputIdentificadorLogin')?.focus({ preventScroll: true });
    }));

    // Perfis: o menu de exemplo mostra só o que cada perfil vê no sistema (mesmas regras do menu real)
    const palcoPerfis = sobre.querySelector('.perfis-palco');
    if (palcoPerfis) {
        const botoes = sobre.querySelectorAll('.perfil-btn');
        const itens = palcoPerfis.querySelectorAll('[data-perfis]');
        const escolher = perfil => {
            botoes.forEach(b => {
                const ativo = b.dataset.perfil === perfil;
                b.classList.toggle('active', ativo);
                b.setAttribute('aria-pressed', ativo);
            });
            let primeiro = true;
            itens.forEach(item => {
                const ve = item.dataset.perfis.split(' ').includes(perfil);
                item.classList.toggle('oculto', !ve);
                const destaque = ve && primeiro && item.classList.contains('nav-link');
                item.classList.toggle('active', destaque);
                if (destaque) primeiro = false;
            });
            palcoPerfis.querySelectorAll('.perfil-info').forEach(info => info.classList.toggle('ativo', info.dataset.perfil === perfil));
        };
        botoes.forEach(b => b.addEventListener('click', () => escolher(b.dataset.perfil)));
        escolher('ALUNO');
    }

    // Jornada: numera os elementos de cada cena (entram em sequência) e desenha o QR code do Pix
    sobre.querySelectorAll('.visual-item').forEach(cena => {
        cena.querySelectorAll('.cena-anima').forEach((el, i) => el.style.setProperty('--i', i));
    });
    sobre.querySelectorAll('.cena-qr').forEach(qr => {
        let semente = 11;
        qr.innerHTML = Array.from({ length: 81 }, (_, n) => {
            const linha = Math.floor(n / 9), coluna = n % 9;
            const canto = (linha < 3 || linha > 5) && (coluna < 3 || coluna > 5) && !(linha > 5 && coluna > 5);
            semente = (semente * 9301 + 49297) % 233280;
            return canto || semente / 233280 > 0.5 ? '<span></span>' : '<span class="vazio"></span>';
        }).join('');
    });

    // Jornada no celular: cada cena vai para baixo do texto do seu passo e se monta ao chegar na tela.
    // Em tela larga as cenas voltam para o quadro ao lado (que fica preso na tela, mais abaixo).
    const jornada = sobre.querySelector('.jornada');
    if (jornada) {
        const passosJ = [...jornada.querySelectorAll('.passo')];
        const cenas = [...jornada.querySelectorAll('.visual-item')];
        const quadro = jornada.querySelector('.jornada-visual');
        const celular = window.matchMedia('(max-width: 991px)');
        const semMovimento = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
        const observador = 'IntersectionObserver' in window ? new IntersectionObserver(entradas => {
            entradas.forEach(e => { if (e.isIntersecting) { e.target.classList.add('ativo'); observador.unobserve(e.target); } });
        }, { threshold: 0.45 }) : null;
        const arrumar = () => {
            cenas.forEach((cena, i) => {
                if (celular.matches) {
                    passosJ[i]?.querySelector(':scope > div')?.appendChild(cena);
                    cena.classList.add('cena-no-passo');
                    if (semMovimento || !observador) cena.classList.add('ativo');
                    else observador.observe(cena);
                } else if (cena.classList.contains('cena-no-passo')) {
                    cena.classList.remove('cena-no-passo', 'ativo');
                    observador?.unobserve(cena);
                    quadro.appendChild(cena);
                }
            });
        };
        arrumar();
        celular.addEventListener('change', arrumar);
    }

    if (typeof gsap === 'undefined' || typeof ScrollTrigger === 'undefined') return;
    gsap.registerPlugin(ScrollTrigger);

    // A abertura com o halter fica em js/halter.js
    const mm = gsap.matchMedia();
    mm.add('(prefers-reduced-motion: no-preference)', () => {
        gsap.from('.perfis-palco > *', {
            opacity: 0, y: 40, duration: 0.8, stagger: 0.15, ease: 'power3.out',
            scrollTrigger: { trigger: '.perfis-palco', start: 'top 80%' }
        });
    });

    // A jornada só fica presa na tela em telas largas; no celular ela é uma lista normal
    mm.add('(min-width: 992px) and (prefers-reduced-motion: no-preference)', () => {
        const secao = sobre.querySelector('.jornada');
        const passos = [...secao.querySelectorAll('.passo')];
        const visuais = [...secao.querySelectorAll('.visual-item')];
        const barra = secao.querySelector('.jornada-barra span');
        const marcar = atual => {
            passos.forEach((p, i) => p.classList.toggle('ativo', i === atual));
            visuais.forEach((v, i) => v.classList.toggle('ativo', i === atual));
        };
        secao.classList.add('jornada-animada');
        marcar(0);
        ScrollTrigger.create({
            trigger: secao,
            start: 'top top',
            end: () => '+=' + window.innerHeight * passos.length * 0.7,
            pin: true,
            onUpdate: self => {
                marcar(Math.min(passos.length - 1, Math.floor(self.progress * passos.length)));
                barra.style.transform = 'scaleY(' + self.progress + ')';
            }
        });
        return () => {
            secao.classList.remove('jornada-animada');
            passos.forEach(p => p.classList.remove('ativo'));
            visuais.forEach(v => v.classList.remove('ativo'));
        };
    });

    // Galeria: os prints dão a volta numa órbita enquanto a seção está presa na tela.
    // Cada cartão continua virado para a frente (não fica espelhado); os de trás ficam menores e mais apagados.
    mm.add('(min-width: 992px) and (prefers-reduced-motion: no-preference)', () => {
        const galeria = sobre.querySelector('.galeria');
        const itens = [...galeria.querySelectorAll('.galeria-item')];
        const volta = Math.PI * 2;
        const orbita = { angulo: 0 };
        const legenda = galeria.querySelector('.galeria-legenda');
        const pontos = galeria.querySelector('.galeria-pontos');
        let frenteAtual = -1;
        galeria.classList.add('galeria-animada');

        // Um ponto por cartão: clicar rola até a posição em que ele fica de frente
        pontos.innerHTML = '';
        const botoes = itens.map((item, i) => {
            const b = document.createElement('button');
            b.type = 'button';
            b.className = 'galeria-ponto';
            b.setAttribute('aria-label', item.dataset.nome);
            b.addEventListener('click', () => {
                const st = animacao.scrollTrigger;
                window.scrollTo({ top: st.start + (st.end - st.start) * i / (itens.length - 1), behavior: 'smooth' });
            });
            pontos.appendChild(b);
            return b;
        });

        const mostrarLegenda = indice => {
            if (indice === frenteAtual) return;
            frenteAtual = indice;
            const item = itens[indice];
            legenda.querySelector('.galeria-quem span').textContent = item.dataset.quem;
            legenda.querySelector('.galeria-nome').textContent = item.dataset.nome;
            legenda.querySelector('.galeria-dica').textContent = item.dataset.dica;
            botoes.forEach((b, i) => b.setAttribute('aria-current', i === indice));
            legenda.classList.remove('trocando');
            void legenda.offsetWidth; // reinicia a animação da legenda
            legenda.classList.add('trocando');
        };

        const posicionar = () => {
            const largura = Math.min(window.innerWidth * 0.36, 600);
            let maisNaFrente = 0;
            let melhor = -1;
            itens.forEach((item, i) => {
                const a = orbita.angulo + (i / itens.length) * volta;
                const frente = (Math.cos(a) + 1) / 2; // 1 = na frente, 0 = atrás
                item.style.transform = `translate3d(${Math.sin(a) * largura}px, ${(1 - frente) * -120}px, ${(frente - 1) * 900}px) rotateY(${Math.sin(a) * -22}deg)`;
                // Os de trás quase somem; os dos lados ficam meio apagados
                item.style.setProperty('--apagado', Math.min(0.92, (1 - frente) * 1.4).toFixed(3));
                item.style.setProperty('--destaque', Math.max(0, (frente - 0.85) / 0.15).toFixed(3));
                item.style.zIndex = Math.round(frente * 100);
                item.inert = frente < 0.9; // só o cartão da frente pode ser clicado
                if (frente > melhor) { melhor = frente; maisNaFrente = i; }
            });
            mostrarLegenda(maisNaFrente);
        };
        posicionar();
        const animacao = gsap.to(orbita, {
            angulo: -volta * (itens.length - 1) / itens.length,
            ease: 'none',
            onUpdate: posicionar,
            scrollTrigger: {
                trigger: galeria, start: 'top top', end: '+=200%', pin: true, scrub: 0.6,
                // Ao parar de rolar, a órbita encaixa com um cartão de frente, pronto para clicar
                snap: { snapTo: 1 / (itens.length - 1), duration: { min: 0.2, max: 0.6 }, ease: 'power1.inOut' }
            }
        });
        window.addEventListener('resize', posicionar);
        return () => {
            window.removeEventListener('resize', posicionar);
            galeria.classList.remove('galeria-animada');
            pontos.innerHTML = '';
            itens.forEach(item => {
                item.style.transform = '';
                item.style.removeProperty('--apagado');
                item.style.removeProperty('--destaque');
                item.style.zIndex = '';
                item.inert = false;
            });
        };
    });
});

/* Botão de tema do login: aparece só com o login ou o final do "Quem somos" na tela, entrando com fade */
(() => {
    const botao = document.querySelector('.tema-login');
    const login = document.querySelector('.login-wrapper');
    const fim = document.querySelector('.sobre-fim');
    if (!botao || !login || !('IntersectionObserver' in window)) return;
    const visiveis = new Map();
    const observador = new IntersectionObserver(entradas => {
        entradas.forEach(e => visiveis.set(e.target, e.target === login ? e.intersectionRatio > 0.35 : e.isIntersecting));
        botao.classList.toggle('escondido', ![...visiveis.values()].some(Boolean));
    }, { threshold: [0, 0.35] });
    observador.observe(login);
    if (fim) observador.observe(fim);
})();

// Senha provisória (modo de senhas fortes): abre o modal de troca e não deixa fechar
document.addEventListener('DOMContentLoaded', () => {
    const modal = document.getElementById('modalMinhaSenha');
    if (modal && modal.dataset.obrigatorio === 'true' && window.bootstrap) {
        bootstrap.Modal.getOrCreateInstance(modal).show();
    }
});

// Sem onclick no HTML (a política de segurança CSP bloqueia scripts escritos dentro do HTML)
document.addEventListener('click', (e) => {
    const olho = e.target.closest('.pass-toggle');
    if (olho) {
        const campo = olho.closest('.pass-group')?.querySelector('input');
        if (campo) alternarSenha(campo.id, olho);
        return;
    }
    if (e.target.closest('[data-imprimir]')) window.print();
});

// Vídeo do "Quem somos": o YouTube só é carregado quando a pessoa clica em assistir
document.addEventListener('click', (e) => {
    const play = e.target.closest('.video-play');
    if (!play) return;
    const quadro = play.closest('.video-quadro');
    const id = quadro.dataset.video || '';
    if (!/^[A-Za-z0-9_-]{11}$/.test(id)) return;
    const frame = document.createElement('iframe');
    frame.src = 'https://www.youtube-nocookie.com/embed/' + id + '?autoplay=1&rel=0';
    frame.title = 'Vídeo de apresentação do GymSync';
    frame.allow = 'autoplay; encrypted-media; picture-in-picture; fullscreen';
    frame.allowFullscreen = true;
    frame.referrerPolicy = 'strict-origin-when-cross-origin'; // o YouTube exige saber de qual site vem (sem isso: erro 153)
    quadro.replaceChildren(frame);
});

// Instalar no celular (PWA). O service worker só guarda o visual para a página "sem conexão".
if ('serviceWorker' in navigator && window.isSecureContext) {
    navigator.serviceWorker.register('/sw.js').catch(() => {});
}

(() => {
    const cartao = document.querySelector('.instalar-app');
    if (!cartao) return;
    const botao = cartao.querySelector('.instalar-botao');
    const jaInstalado = window.matchMedia('(display-mode: standalone)').matches || navigator.standalone;
    let dispensado = false;
    try { dispensado = localStorage.getItem('gymsync_instalar_dispensado') === 'sim'; } catch (e) {}
    if (jaInstalado || dispensado) return;

    // Android e computador: o navegador avisa quando dá para instalar
    let pedido = null;
    window.addEventListener('beforeinstallprompt', (e) => {
        e.preventDefault();
        pedido = e;
        botao.hidden = false;
        cartao.hidden = false;
    });
    botao.addEventListener('click', async () => {
        if (!pedido) return;
        pedido.prompt();
        await pedido.userChoice;
        pedido = null;
        cartao.hidden = true;
    });
    window.addEventListener('appinstalled', () => { cartao.hidden = true; });

    // iPhone: não existe botão de instalar, só o caminho pelo Safari
    if (/iphone|ipad|ipod/i.test(navigator.userAgent)) {
        cartao.querySelector('.instalar-texto').textContent =
            'No Safari, toque em Compartilhar e depois em "Adicionar à Tela de Início".';
        cartao.hidden = false;
    }

    cartao.querySelector('.instalar-fechar').addEventListener('click', () => {
        cartao.hidden = true;
        try { localStorage.setItem('gymsync_instalar_dispensado', 'sim'); } catch (e) {}
    });
})();
