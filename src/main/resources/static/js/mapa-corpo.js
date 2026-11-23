/* Créditos: o desenho dos músculos do mapa 2D vem do react-body-highlighter (https://github.com/GV79/react-body-highlighter),
   licença MIT (texto em static/vendor/react-body-highlighter/LICENSE e static/vendor/CREDITS.md).
   Mãos, pés e o ajuste da silhueta são feitos aqui.

   Mapa do corpo (aba Avaliações Físicas): silhueta em SVG (frente e costas) com os músculos, uma medida por região
   (peito, abdômen, braço, cintura, quadril, coxa, ombro, antebraço e panturrilha) e a diferença entre a primeira e a última avaliação.
   - Os dados vêm do HTML (data-*), montados pelo EvolucaoService; a cor de cada músculo segue a regra de lá.
   - Cada músculo fica na cor da medida ligada a ele (melhorou, piorou ou estável).
*/

import { anteriorData, posteriorData } from '../vendor/react-body-highlighter/musculos.js';

const raiz = document.querySelector('[data-mapa-corpo]');

/* ---------- Forma do manequim (metros; figura de 1,78 m com os pés no chão) ---------- */

// p = altura da seção (no braço, distância a partir do ombro); rx/rz = meia largura e meia profundidade;
// cx/cz = centro; r = quanto cada medida mexe na seção
const TRONCO = [
    { p: 0.820, rx: 0.020, rz: 0.020 },
    { p: 0.850, rx: 0.090, rz: 0.072 },
    { p: 0.900, rx: 0.163, rz: 0.110, r: { quadril: 0.7 } },
    { p: 0.950, rx: 0.174, rz: 0.118, r: { quadril: 1 } },
    { p: 1.010, rx: 0.165, rz: 0.111, r: { quadril: 0.6, cintura: 0.3 } },
    { p: 1.090, rx: 0.146, rz: 0.102, r: { cintura: 0.9 } },
    { p: 1.130, rx: 0.142, rz: 0.100, r: { cintura: 1 } },
    { p: 1.200, rx: 0.151, rz: 0.106, r: { cintura: 0.5, peito: 0.4 } },
    { p: 1.290, rx: 0.170, rz: 0.118, r: { peito: 1 } },
    { p: 1.370, rx: 0.180, rz: 0.112, r: { peito: 0.6 } },
    { p: 1.425, rx: 0.176, rz: 0.096 },
    { p: 1.460, rx: 0.122, rz: 0.078 },
    { p: 1.490, rx: 0.064, rz: 0.061 },
    { p: 1.530, rx: 0.052, rz: 0.056 },
    { p: 1.570, rx: 0.050, rz: 0.055 },
    { p: 1.610, rx: 0.044, rz: 0.048 }
];
const PERNA = [
    { p: 0.020, rx: 0.026, rz: 0.030, cx: 0.088 },
    { p: 0.050, rx: 0.031, rz: 0.036, cx: 0.088 },
    { p: 0.100, rx: 0.032, rz: 0.036, cx: 0.088, cz: -0.004 },
    { p: 0.220, rx: 0.042, rz: 0.046, cx: 0.089, cz: -0.008 },
    { p: 0.350, rx: 0.055, rz: 0.060, cx: 0.090, cz: -0.012 },
    { p: 0.440, rx: 0.049, rz: 0.052, cx: 0.090, cz: -0.004 },
    { p: 0.490, rx: 0.053, rz: 0.058, cx: 0.091 },
    { p: 0.540, rx: 0.056, rz: 0.062, cx: 0.092 },
    { p: 0.640, rx: 0.070, rz: 0.076, cx: 0.094, r: { coxa: 0.6 } },
    { p: 0.740, rx: 0.082, rz: 0.088, cx: 0.095, r: { coxa: 1 } },
    { p: 0.800, rx: 0.086, rz: 0.092, cx: 0.094, r: { coxa: 1 } },
    { p: 0.880, rx: 0.087, rz: 0.098, cx: 0.089, r: { coxa: 0.6 } },
    { p: 0.960, rx: 0.060, rz: 0.070, cx: 0.085 }
];
const BRACO = [
    { p: -0.035, rx: 0.030, rz: 0.034 },
    { p: 0.000, rx: 0.054, rz: 0.058 },
    { p: 0.050, rx: 0.056, rz: 0.060, r: { braco: 0.5 } },
    { p: 0.120, rx: 0.049, rz: 0.053, r: { braco: 1 } },
    { p: 0.170, rx: 0.047, rz: 0.051, r: { braco: 1 } },
    { p: 0.250, rx: 0.040, rz: 0.042, r: { braco: 0.4 } },
    { p: 0.300, rx: 0.037, rz: 0.038 },
    { p: 0.350, rx: 0.039, rz: 0.040 },
    { p: 0.440, rx: 0.032, rz: 0.033 },
    { p: 0.540, rx: 0.024, rz: 0.021 },
    { p: 0.580, rx: 0.021, rz: 0.018 }
];
const PARTES = { tronco: TRONCO, perna: PERNA, braco: BRACO };
const OMBRO = { x: 0.198, y: 1.385, giro: 0.16 };
const CABECA = { y: 1.665, z: 0.008, rx: 0.092, ry: 0.115, rz: 0.102 };
const MAO = { p: 0.632, rx: 0.030, ry: 0.075, rz: 0.042 };
const PE = { x: 0.088, y: 0.03, z: 0.06, rx: 0.042, ry: 0.03, rz: 0.105 };
const ALTURA_BASE_CM = 175;

// Onde passa a fita de cada medida e de que lado da figura fica o rótulo (-1 esquerda, 1 direita)
const REGIOES = {
    peito: { parte: 'tronco', em: 1.29, lado: -1 },
    braco: { parte: 'braco', em: 0.15, lado: 1 },
    cintura: { parte: 'tronco', em: 1.13, lado: -1 },
    quadril: { parte: 'tronco', em: 0.95, lado: 1 },
    coxa: { parte: 'perna', em: 0.76, lado: -1 }
};
const CHAVES_REGIAO = Object.keys(REGIOES);
// Cartões do resumo; massa muscular e massa gorda (bioimpedância) são do corpo inteiro e não pintam região nenhuma
const CHAVES_RESUMO = ['peso', 'gordura', 'imc', 'massaMuscular', 'massaGorda'];

// Medidas que só o mapa dos músculos desenha.
// lado: de que lado fica o rótulo; em: altura aproximada (m) para alinhar o rótulo quando não há músculo para medir
const SO_2D = {
    ombro: { lado: -1, em: 1.40 },
    abdomen: { lado: 1, em: 1.12 }, // barriga: os retos do abdômen, no meio do tronco (só de frente)
    antebraco: { lado: 1, em: 0.98 },
    panturrilha: { lado: 1, em: 0.38 }
};
const CHAVES_ROTULO = [...CHAVES_REGIAO, ...Object.keys(SO_2D)];
const ladoDe = chave => (REGIOES[chave] || SO_2D[chave]).lado;
/** "(aumentou)", "(diminuiu)" ou "(igual)", a partir da diferença já formatada ("+2,0 cm", "-5,0 cm", "0,0 cm"). */
const sentido = diferenca => (/^\+/.test(diferenca) ? '(aumentou)' : /^[-−]/.test(diferenca) ? '(diminuiu)' : '(igual)');
const FATOR_NEUTRO = Object.fromEntries(CHAVES_REGIAO.map(c => [c, 1]));

const limitar = (v, min, max) => Math.max(min, Math.min(max, v));

function catmull(a, b, c, d, t) {
    return 0.5 * ((2 * b) + (-a + c) * t + (2 * a - 5 * b + 4 * c - d) * t * t + (-a + 3 * b - 3 * c + d) * t * t * t);
}

/** Aplica os fatores das medidas nas seções e devolve a lista suavizada (várias seções entre cada duas). */
function secoesDe(parte, fatores, passos = 5) {
    const base = PARTES[parte].map(s => {
        let f = 1;
        for (const chave in (s.r || {})) f += s.r[chave] * ((fatores[chave] || 1) - 1);
        return { p: s.p, rx: s.rx * f, rz: s.rz * f, cx: s.cx || 0, cz: s.cz || 0 };
    });
    const em = i => base[limitar(i, 0, base.length - 1)];
    const saida = [];
    for (let i = 0; i < base.length - 1; i++) {
        for (let k = 0; k < passos; k++) {
            const s = {};
            for (const campo of ['p', 'rx', 'rz', 'cx', 'cz']) {
                s[campo] = catmull(em(i - 1)[campo], em(i)[campo], em(i + 1)[campo], em(i + 2)[campo], k / passos);
            }
            saida.push(s);
        }
    }
    saida.push({ ...base[base.length - 1] });
    return saida;
}

function secaoEm(secoes, p) {
    for (let i = 0; i < secoes.length - 1; i++) {
        const a = secoes[i], b = secoes[i + 1];
        if (p >= a.p && p <= b.p) {
            const t = (p - a.p) / ((b.p - a.p) || 1);
            return { rx: a.rx + (b.rx - a.rx) * t, rz: a.rz + (b.rz - a.rz) * t, cx: a.cx + (b.cx - a.cx) * t, cz: a.cz + (b.cz - a.cz) * t };
        }
    }
    return secoes[secoes.length - 1];
}

/** Perímetro da elipse (Ramanujan), em centímetros: é a "medida de fita" do manequim sem ajuste. */
function perimetroCm(rx, rz) {
    return Math.PI * (3 * (rx + rz) - Math.sqrt((3 * rx + rz) * (rx + 3 * rz))) * 100;
}
const REFERENCIA_CM = Object.fromEntries(CHAVES_REGIAO.map(chave => {
    const s = secaoEm(secoesDe(REGIOES[chave].parte, FATOR_NEUTRO), REGIOES[chave].em);
    return [chave, perimetroCm(s.rx, s.rz)];
}));

/** Quanto cada região engrossa (>1) ou afina (<1): metade da diferença real, com limite, para não virar caricatura. */
function fatoresDe(aluno, momento) {
    const fatores = { ...FATOR_NEUTRO };
    const escala = (aluno.altura || ALTURA_BASE_CM) / ALTURA_BASE_CM;
    for (const chave of CHAVES_REGIAO) {
        const m = aluno.medidas[chave];
        const valor = m && (momento === 'primeira' ? m.primeiraNumero : m.ultimaNumero);
        if (!valor) continue;
        fatores[chave] = limitar(1 + (valor / (REFERENCIA_CM[chave] * escala) - 1) * 0.6, 0.88, 1.18);
    }
    return fatores;
}

/* ---------- Dados do HTML ---------- */

// Sem cor só para peso e IMC (subir ou descer não é bom nem ruim por si). As outras medidas comparadas
// que não mudaram além do limite do servidor aparecem como "estável" (laranja), nos dois modos.
const SEMPRE_SEM_COR = ['peso', 'imc'];
function classeVisual(chave, classe, diferenca) {
    return classe === 'neutro' && diferenca && !SEMPRE_SEM_COR.includes(chave) ? 'estavel' : classe;
}

function lerAlunos() {
    const alunos = new Map();
    raiz.querySelectorAll('.mapa-dados > [data-aluno]').forEach(bloco => {
        const medidas = {};
        bloco.querySelectorAll('[data-chave]').forEach(el => {
            const d = el.dataset;
            medidas[d.chave] = {
                chave: d.chave, rotulo: d.rotulo, primeira: d.primeira, ultima: d.ultima,
                primeiraNumero: parseFloat(d.primeiraNumero), ultimaNumero: parseFloat(d.ultimaNumero),
                diferenca: d.diferenca || null, classe: classeVisual(d.chave, d.classe || 'neutro', d.diferenca),
                intensidade: parseFloat(d.intensidade) || 0
            };
        });
        alunos.set(bloco.dataset.aluno, {
            nome: bloco.dataset.nome || '', avaliacoes: parseInt(bloco.dataset.avaliacoes || '0', 10),
            dataPrimeira: bloco.dataset.primeira || null, dataUltima: bloco.dataset.ultima || null,
            altura: parseFloat(bloco.dataset.altura) || null, silhueta: bloco.dataset.silhueta || 'NAO_INFORMADA', medidas
        });
    });
    return alunos;
}

/* ---------- Mapa dos músculos 2D (frente e costas) ----------
   Polígonos do react-body-highlighter (MIT, ver static/vendor/react-body-highlighter/LICENSE).
   Cada músculo ligado a uma medida recebe a cor dela; os outros ficam na cor base. */

const SVG_NS = 'http://www.w3.org/2000/svg';
// Área de cada vista (viewBox): maior que a do pacote para caber mãos, pés e o ajuste da silhueta
const VISTAS = {
    frente: { x: -8, y: -6, largura: 116, altura: 216 },
    costas: { x: -8, y: -6, largura: 116, altura: 236 }
};
const DESENHOS = { frente: anteriorData, costas: posteriorData };
const ALTURA_PADRAO_CM = 170;

/* ---------- Silhueta ajustada a cada aluno (só o 2D) ----------
   O desenho-base é sempre o mesmo; a largura de cada faixa do corpo muda conforme a silhueta escolhida no cadastro
   (masculina, feminina ou não informada), a altura, o peso, a cintura e o quadril do aluno. Proporções ilustrativas. */

// Desenho-base de cada silhueta: largura relativa dos ombros, peito, cintura, quadril e coxas
const BASES_SILHUETA = {
    MASCULINA: { ombro: 1.16, peito: 1.10, cintura: 1.00, quadril: 0.90, coxa: 0.94 },
    FEMININA: { ombro: 0.84, peito: 0.90, cintura: 0.80, quadril: 1.14, coxa: 1.10 },
    NAO_INFORMADA: { ombro: 1, peito: 1, cintura: 1, quadril: 1, coxa: 1 }
};
// Perímetro de referência dividido pela altura (o "normal" de cada silhueta, que não mexe no desenho)
const REFERENCIA_SILHUETA = {
    MASCULINA: { cintura: 0.48, quadril: 0.55 },
    FEMININA: { cintura: 0.43, quadril: 0.60 },
    NAO_INFORMADA: { cintura: 0.455, quadril: 0.575 }
};
// Altura (y do desenho) de cada faixa do corpo em cada vista; a largura é interpolada entre elas
const FAIXAS = {
    frente: [[0, 'cabeca'], [25, 'cabeca'], [38, 'ombro'], [50, 'peito'], [80, 'cintura'], [100, 'quadril'],
        [120, 'coxa'], [148, 'joelho'], [175, 'panturrilha'], [215, 'panturrilha']],
    costas: [[0, 'cabeca'], [22, 'cabeca'], [40, 'ombro'], [55, 'peito'], [88, 'cintura'], [112, 'quadril'],
        [135, 'coxa'], [160, 'joelho'], [185, 'panturrilha'], [235, 'panturrilha']]
};
const BASE_DOS_PES = { frente: 196, costas: 220 };
const MUSCULOS_DO_BRACO = ['biceps', 'triceps', 'forearm', 'front-deltoids', 'back-deltoids'];

/** Larguras de cada faixa para um aluno (1 = desenho original). */
function perfilDe(aluno, momento) {
    const tipo = BASES_SILHUETA[aluno && aluno.silhueta] ? aluno.silhueta : 'NAO_INFORMADA';
    const base = BASES_SILHUETA[tipo];
    const ref = REFERENCIA_SILHUETA[tipo];
    const altura = (aluno && aluno.altura) || ALTURA_PADRAO_CM;
    const valor = chave => {
        const m = aluno && aluno.medidas[chave];
        const v = m && (momento === 'primeira' ? m.primeiraNumero : m.ultimaNumero);
        return Number.isFinite(v) && v > 0 ? v : null;
    };
    const peso = valor('peso');
    const imc = peso ? peso / ((altura / 100) ** 2) : 23;
    // Sensibilidade maior que a proporção real, para que poucos centímetros de diferença apareçam no desenho
    const geral = limitar(1 + (imc - 23) * 0.035, 0.84, 1.25);
    const razao = (chave, referencia) => {
        const v = valor(chave);
        return v ? limitar(1 + (v / altura / referencia - 1) * 1.8, 0.8, 1.3) : geral;
    };
    const cintura = razao('cintura', ref.cintura);
    const quadril = razao('quadril', ref.quadril);
    const faixa = (v, min = 0.68, max = 1.35) => limitar(v, min, max);
    return {
        cabeca: 1,
        ombro: faixa(base.ombro * Math.sqrt(geral)),
        peito: faixa(base.peito * geral),
        cintura: faixa(base.cintura * cintura),
        quadril: faixa(base.quadril * quadril),
        coxa: faixa(base.coxa * (quadril + geral) / 2),
        joelho: faixa(geral ** 0.4),
        panturrilha: faixa(geral ** 0.6),
        escala: limitar(altura / 172, 0.92, 1.06),
        alturaPadrao: !(aluno && aluno.altura)
    };
}

function larguraEm(vista, y, perfil) {
    const faixas = FAIXAS[vista];
    if (y <= faixas[0][0]) return perfil[faixas[0][1]];
    for (let i = 1; i < faixas.length; i++) {
        const [y1, f1] = faixas[i];
        if (y <= y1) {
            const [y0, f0] = faixas[i - 1];
            const t = (y - y0) / (y1 - y0);
            return perfil[f0] + (perfil[f1] - perfil[f0]) * t;
        }
    }
    return perfil[faixas[faixas.length - 1][1]];
}

/** Desenho de uma vista com a silhueta do aluno, mais mãos e pés (em tom neutro, sem medida). */
function formaDe(vista, perfil) {
    const baseY = BASE_DOS_PES[vista];
    // Os braços não afinam com a cintura: andam junto com a borda do peito
    const desvioBraco = 23 * (larguraEm(vista, 48, perfil) - 1);
    const ponto = (x, y, braco) => {
        let nx = braco ? x + Math.sign(x - 50) * desvioBraco : 50 + (x - 50) * larguraEm(vista, y, perfil);
        nx = 50 + (nx - 50) * perfil.escala;
        return { x: nx, y: baseY - (baseY - y) * perfil.escala };
    };
    const forma = DESENHOS[vista].map(item => {
        const braco = MUSCULOS_DO_BRACO.includes(item.muscle);
        return {
            muscle: item.muscle,
            pontos: item.svgPoints.map(texto => pontosDe(texto).map(p => ponto(p.x, p.y, braco)))
        };
    });
    return forma.concat(extremidades(vista, forma));
}

// Formas da mão e do pé (origem no meio da ponta do antebraço ou da perna; x espelhado para o lado esquerdo)
const FORMA_MAO = [[-3.2, 0], [3.2, 0], [4.3, 4.5], [3.8, 9.5], [1.2, 12.8], [-1.8, 12.4], [-3.9, 8.6], [-4.3, 3.6]];
const PE_FRENTE = [[-3.6, 0], [3.6, 0], [4.4, 3.8], [6.6, 7.2], [5.8, 9], [-4.2, 9], [-5, 7], [-4.4, 3.6]];
const PE_COSTAS = [[-3.4, 0], [3.4, 0], [4, 2.8], [3, 5.6], [-3, 5.6], [-4, 2.8]];

function extremidades(vista, forma) {
    const pontasDe = (musculos, lado) => {
        const pts = forma.filter(f => musculos.includes(f.muscle)).flatMap(f => f.pontos.flat())
            .filter(p => Math.sign(p.x - 50) === lado);
        if (!pts.length) return null;
        const fundo = Math.max(...pts.map(p => p.y));
        const embaixo = pts.filter(p => p.y > fundo - 2.5);
        return { x: embaixo.reduce((s, p) => s + p.x, 0) / embaixo.length, y: fundo - 1.2 };
    };
    const pecas = [];
    for (const lado of [-1, 1]) {
        const mao = pontasDe(['forearm'], lado);
        if (mao) pecas.push({ muscle: "mao", pontos: [FORMA_MAO.map(([x, y]) => ({ x: mao.x + lado * (x + 0.22 * y), y: mao.y + y }))] });
        const pe = pontasDe(vista === 'frente' ? ['calves'] : ['left-soleus', 'right-soleus', 'calves'], lado);
        const formaPe = vista === 'frente' ? PE_FRENTE : PE_COSTAS;
        if (pe) pecas.push({ muscle: 'pe', pontos: [formaPe.map(([x, y]) => ({ x: pe.x + lado * x, y: pe.y + y }))] });
    }
    return pecas;
}

// Medida → músculos de cada lado do corpo (só as medidas que a avaliação física tem)
const MUSCULOS = {
    frente: {
        peito: ['chest'], braco: ['biceps', 'triceps'], cintura: ['obliques'], abdomen: ['abs'], quadril: ['abductors'], coxa: ['quadriceps'],
        ombro: ['front-deltoids'], antebraco: ['forearm'], panturrilha: ['calves'] // na frente, "calves" é a perna abaixo do joelho (tibial)
    },
    costas: {
        braco: ['triceps'], cintura: ['lower-back'], quadril: ['gluteal'], coxa: ['hamstring'],
        ombro: ['back-deltoids'], antebraco: ['forearm'], panturrilha: ['calves'] // gastrocnêmio
    }
};
const CHAVE_DO_MUSCULO = Object.fromEntries(Object.entries(MUSCULOS).map(([vista, mapa]) =>
    [vista, Object.fromEntries(Object.entries(mapa).flatMap(([chave, nomes]) => nomes.map(n => [n, chave])))]));

// Ao clicar na pílula, a região que aparece melhor de um lado só leva a figura para esse lado
const VISTA_PREFERIDA = { peito: 'frente', quadril: 'costas' };
// Músculo que fica no meio do corpo nesta vista: a linha-guia teria de atravessar outro músculo para chegar,
// então ali a pílula fica apagada (como o peito nas costas). O quadril, de frente, é o abdutor (parte de dentro da coxa).
const SEM_LINHA = { frente: ['quadril'], costas: [] };

function noSvg(nome, atributos) {
    const el = document.createElementNS(SVG_NS, nome);
    for (const a in atributos) el.setAttribute(a, atributos[a]);
    return el;
}

function pontosDe(texto) {
    const n = texto.trim().split(/\s+/).map(Number);
    const pontos = [];
    for (let i = 0; i + 1 < n.length; i += 2) pontos.push({ x: n[i], y: n[i + 1] });
    return pontos;
}

/** Centro de cada polígono dos músculos de uma medida, numa forma já ajustada (coordenadas do desenho). */
function centrosDe(vista, chave, forma) {
    const nomes = MUSCULOS[vista][chave] || [];
    const centros = [];
    for (const item of forma) {
        if (!nomes.includes(item.muscle)) continue;
        for (const pts of item.pontos) {
            centros.push({ x: pts.reduce((s, p) => s + p.x, 0) / pts.length, y: pts.reduce((s, p) => s + p.y, 0) / pts.length });
        }
    }
    return centros;
}

const PERFIL_NEUTRO = perfilDe(null, 'ultima');

function criarSilhueta(svg) {
    let vista = 'frente';
    let formas = { frente: formaDe('frente', PERFIL_NEUTRO), costas: formaDe('costas', PERFIL_NEUTRO) };
    const area = () => VISTAS[vista];
    const usarArea = () => { const v = area(); svg.setAttribute('viewBox', `${v.x} ${v.y} ${v.largura} ${v.altura}`); };
    usarArea();
    const centros = (v, chave) => centrosDe(v, chave, formas[v]);

    /** Ajusta as duas vistas à silhueta do aluno (perfil vindo de perfilDe). */
    function ajustar(perfil) {
        formas = { frente: formaDe('frente', perfil), costas: formaDe('costas', perfil) };
    }

    /** regioes = { chave: classe }; forca = { chave: intensidade de 0 a 1 } (cor mais forte quanto maior a variação). */
    function desenhar(regioes, forca = {}) {
        svg.replaceChildren();
        usarArea();
        for (const item of formas[vista]) {
            const chave = CHAVE_DO_MUSCULO[vista][item.muscle];
            const classe = chave && regioes[chave] ? ` medido ${regioes[chave]}` : '';
            const extra = item.muscle === 'mao' || item.muscle === 'pe' ? ' mapa-extremidade' : '';
            for (const pts of item.pontos) {
                const texto = pts.map(p => `${p.x.toFixed(2)} ${p.y.toFixed(2)}`).join(' ');
                const el = noSvg('polygon', { points: texto, class: `mapa-musculo${extra}${classe}` });
                if (classe) {
                    el.dataset.chave = chave;
                    // vence o CSS (atributo não venceria); estável tem variação ~0, então usa opacidade fixa para o laranja aparecer
                    if (regioes[chave] === 'estavel') el.style.fillOpacity = '0.75';
                    else if (regioes[chave] !== 'neutro') el.style.fillOpacity = (0.45 + 0.55 * (forca[chave] || 0)).toFixed(2);
                }
                svg.appendChild(el);
            }
        }
    }

    // Do desenho (viewBox) para pixels dentro da figura
    function emPixels(pt) {
        const caixa = svg.getBoundingClientRect();
        if (!caixa.width) return null;
        const v = area();
        const escala = Math.min(caixa.width / v.largura, caixa.height / v.altura);
        return {
            x: (caixa.width - v.largura * escala) / 2 + (pt.x - v.x) * escala,
            y: (caixa.height - v.altura * escala) / 2 + (pt.y - v.y) * escala
        };
    }

    /**
     * Ponto do músculo da medida para a linha-guia: só músculos do mesmo lado do rótulo (ou um que fica no meio
     * do corpo), na borda virada para o rótulo, na altura do meio do músculo. Assim a linha não atravessa o corpo.
     * Sem músculo nesta vista, null.
     */
    function ancora(chave, lado) {
        if (SEM_LINHA[vista].includes(chave)) return null;
        const nomes = MUSCULOS[vista][chave] || [];
        let minX = Infinity, maxX = -Infinity;
        for (const item of formas[vista]) for (const pts of item.pontos) for (const p of pts) { minX = Math.min(minX, p.x); maxX = Math.max(maxX, p.x); }
        const meio = (minX + maxX) / 2, folga = (maxX - minX) * 0.03;
        let melhor = null;
        for (const item of formas[vista]) {
            if (!nomes.includes(item.muscle)) continue;
            for (const pts of item.pontos) {
                const cx = pts.reduce((s, p) => s + p.x, 0) / pts.length;
                if (lado * (cx - meio) < -folga) continue; // músculo do outro lado do corpo
                let y0 = Infinity, y1 = -Infinity;
                for (const p of pts) { y0 = Math.min(y0, p.y); y1 = Math.max(y1, p.y); }
                // O abdômen sai pela parte de baixo dos retos, onde não há oblíquo ao lado: a linha não passa sobre a cintura
                const y = y0 + (y1 - y0) * (chave === 'abdomen' ? 0.7 : 0.5);
                // Borda do polígono na altura y, a mais para o lado do rótulo
                for (let i = 0; i < pts.length; i++) {
                    const a = pts[i], b = pts[(i + 1) % pts.length];
                    if ((a.y - y) * (b.y - y) > 0 || a.y === b.y) continue;
                    const x = a.x + (y - a.y) * (b.x - a.x) / (b.y - a.y);
                    if (!melhor || lado * x > lado * melhor.x) melhor = { x, y };
                }
            }
        }
        if (!melhor) return null;
        return emPixels({ x: melhor.x - lado * 1.2, y: melhor.y }); // um pouco para dentro: a bolinha fica sobre o músculo
    }

    /** Altura (em pixels) da medida para alinhar o rótulo; usa a frente quando a vista não tem o músculo. */
    function alturaRotulo(chave) {
        const lista = centros(vista, chave).length ? centros(vista, chave) : centros('frente', chave);
        if (!lista.length) return null;
        const p = emPixels({ x: 0, y: lista.reduce((s, c) => s + c.y, 0) / lista.length });
        return p && p.y;
    }

    function destacar(chave) {
        svg.querySelectorAll('.mapa-musculo.medido').forEach(el => el.classList.toggle('ativo', el.dataset.chave === chave));
    }

    /** Ponteiro sobre um músculo medido: f(chave ou null, evento). Clique: g(chave). Vale nas duas vistas. */
    function aoApontar(f, g) {
        const chaveDe = e => (e.target.closest && e.target.closest('.mapa-musculo.medido') || {}).dataset?.chave || null;
        svg.addEventListener('pointermove', e => f(chaveDe(e), e));
        svg.addEventListener('pointerleave', e => f(null, e));
        svg.addEventListener('click', e => { const c = chaveDe(e); if (c) g(c); });
    }

    return {
        desenhar, ajustar, ancora, alturaRotulo, destacar, aoApontar,
        vista: () => vista,
        trocarVista(nova) { vista = nova; }
    };
}

/* ---------- Tela ---------- */

function iniciar() {
    const alunos = lerAlunos();
    const palco = raiz.querySelector('.mapa-palco');
    const figura = raiz.querySelector('.mapa-figura');
    const linhas = raiz.querySelector('.mapa-linhas');
    const colunas = { '-1': raiz.querySelector('.mapa-esq'), '1': raiz.querySelector('.mapa-dir') };
    const resumo = raiz.querySelector('.mapa-resumo');
    const periodo = raiz.querySelector('.mapa-periodo');
    const semMedidas = raiz.querySelector('.mapa-sem-medidas');
    const avisoAltura = raiz.querySelector('.mapa-aviso-altura');
    const seletor = raiz.querySelector('.mapa-aluno');
    const botoesMomento = raiz.querySelectorAll('[data-momento]');
    const botoesVista = raiz.querySelectorAll('[data-vista]');
    const grupoVista = raiz.querySelector('.mapa-vista');
    const comparando = raiz.querySelector('.mapa-comparando');
    const legenda = raiz.querySelector('.mapa-legenda');
    const silhueta = criarSilhueta(raiz.querySelector('.mapa-silhueta'));
    const dica = raiz.querySelector('.mapa-dica-musculo');
    let vistaEscolhida = 'frente';
    let trocaVista = 0;

    let alunoAtual = seletor ? seletor.value : alunos.keys().next().value;
    let momento = 'ultima';
    let fixado = null;
    let rotulos = []; // { chave, lado, li, traco, bolinha }

    const aluno = () => alunos.get(alunoAtual);

    function elemento(tag, classe, texto) {
        const el = document.createElement(tag);
        if (classe) el.className = classe;
        if (texto != null) el.textContent = texto;
        return el;
    }

    function destacar(chave) {
        const ativa = chave || fixado;
        rotulos.forEach(r => {
            const ligado = r.chave === ativa;
            r.li.classList.toggle('ativo', ligado);
            r.traco.classList.toggle('ativo', ligado);
            r.bolinha.classList.toggle('ativo', ligado);
        });
        silhueta.destacar(ativa);
    }

    function montarRotulos() {
        const a = aluno();
        Object.values(colunas).forEach(c => c.replaceChildren());
        linhas.replaceChildren();
        resumo.replaceChildren();
        rotulos = [];
        fixado = null;
        if (!a) return;

        const valor = m => (momento === 'primeira' ? m.primeira : m.ultima);
        for (const chave of CHAVES_ROTULO) {
            const m = a.medidas[chave];
            if (!m) continue;
            const lado = ladoDe(chave);
            const li = elemento('li', `mapa-rotulo ${m.classe}`);
            li.tabIndex = 0;
            // Fita métrica mede o perímetro da região: o texto diz isso, sem falar em ganho ou perda de músculo
            const nome = elemento('span', 'mapa-rotulo-nome', m.rotulo);
            nome.append(' ', elemento('span', 'mapa-rotulo-tipo', '(perímetro)')); // o espaço deixa quebrar a linha em tela estreita
            li.append(nome, elemento('span', 'mapa-rotulo-valor', valor(m)));
            if (m.diferenca) li.append(elemento('span', `evo-dif ${m.classe}`, `${m.diferenca} ${sentido(m.diferenca)}`));
            const apontarPilula = () => destacar(chave);
            li.addEventListener('mouseenter', apontarPilula);
            li.addEventListener('mouseleave', () => destacar(null));
            li.addEventListener('focus', apontarPilula);
            li.addEventListener('blur', () => destacar(null));
            li.addEventListener('click', () => {
                fixado = fixado === chave ? null : chave;
                destacar(null);
                const preferida = VISTA_PREFERIDA[chave];
                if (fixado && preferida) mudarVista(preferida);
            });
            colunas[lado].append(li);
            const traco = noSvg('path', { class: `mapa-traco ${m.classe}` });
            const bolinha = noSvg('circle', { r: 3.5, class: `mapa-bolinha ${m.classe}` });
            linhas.append(traco, bolinha);
            rotulos.push({ chave, lado, li, traco, bolinha });
        }
        semMedidas.hidden = rotulos.length > 0;

        for (const chave of CHAVES_RESUMO) {
            const m = a.medidas[chave];
            if (!m) continue;
            const cartao = elemento('div', 'mapa-resumo-item');
            cartao.append(elemento('span', 'mapa-rotulo-nome', m.rotulo), elemento('span', 'mapa-resumo-valor', valor(m)));
            if (m.diferenca) cartao.append(elemento('span', `evo-dif ${m.classe}`, m.diferenca));
            resumo.append(cartao);
        }

        const comEvolucao = Object.values(a.medidas).some(m => m.diferenca);
        raiz.querySelector('.mapa-momento').hidden = !comEvolucao;
        if (a.avaliacoes <= 1) {
            periodo.textContent = `1 avaliação${a.dataUltima ? ' em ' + a.dataUltima : ''}. Com a segunda, a evolução aparece aqui.`;
        } else {
            periodo.textContent = `${a.avaliacoes} avaliações, de ${a.dataPrimeira} a ${a.dataUltima}.`;
        }

        // O que está sendo comparado, ao lado do Primeira/Última
        if (comEvolucao && a.dataPrimeira && a.dataUltima) {
            comparando.textContent = `Comparando ${a.dataPrimeira} → ${a.dataUltima}`;
            botoesMomento.forEach(b => {
                b.title = `Mostrar os valores de ${b.dataset.momento === 'primeira' ? a.dataPrimeira : a.dataUltima}`;
            });
        } else {
            comparando.textContent = a.dataUltima ? `Avaliação de ${a.dataUltima}` : '';
        }

        // Legenda a partir dos dados: cada medida entra num status só
        legenda.replaceChildren();
        const grupos = { melhorou: 'Melhorou', piorou: 'Piorou', estavel: 'Estável', neutro: 'Sem cor' };
        for (const classe in grupos) {
            const doGrupo = Object.values(a.medidas).filter(m => (m.classe || 'neutro') === classe);
            if (!doGrupo.length) continue;
            // Um grupo por linha: bolinha, título e os itens; cada item não quebra no meio (nome e número juntos)
            const li = elemento('li', `mapa-legenda-grupo ${classe}`);
            const corpo = elemento('div', 'mapa-legenda-corpo');
            corpo.append(elemento('span', 'mapa-legenda-titulo', `${grupos[classe]}:`));
            const itens = elemento('span', 'mapa-legenda-itens');
            doGrupo.forEach((m, i) => {
                const texto = m.diferenca && classe !== 'neutro' && classe !== 'estavel' ? `${m.rotulo} (${m.diferenca})` : m.rotulo;
                itens.append(elemento('span', 'mapa-legenda-item', texto + (i < doGrupo.length - 1 ? ',' : '')));
                if (i < doGrupo.length - 1) itens.append(' ');
            });
            corpo.append(' ', itens);
            li.append(elemento('span', `mapa-ponto ${classe}`), corpo);
            legenda.append(li);
        }
    }

    // Em tela larga os rótulos ficam ao lado da figura, na altura da região, ligados por um traço
    function comTracos() {
        return getComputedStyle(linhas).display !== 'none';
    }
    function posicionarRotulos() {
        const largo = comTracos();
        const alturaPalco = palco.clientHeight;
        const topoFigura = figura.getBoundingClientRect().top - palco.getBoundingClientRect().top;
        // Altura do rótulo no palco: pelo centro do músculo
        const meioDe = chave => {
            const y = silhueta.alturaRotulo(chave);
            return y != null ? topoFigura + y : (1 - (alturaDe(chave) + 0.06) / 1.92) * alturaPalco;
        };
        for (const lado of [-1, 1]) {
            const doLado = rotulos.filter(r => r.lado === lado)
                .sort((x, y) => meioDe(x.chave) - meioDe(y.chave));
            let minimo = 0;
            doLado.forEach(r => {
                if (!largo) { r.li.style.top = ''; return; }
                const meio = meioDe(r.chave);
                const topo = Math.max(minimo, meio - r.li.offsetHeight / 2);
                r.li.style.top = `${Math.round(topo)}px`;
                minimo = topo + r.li.offsetHeight + 8;
            });
        }
    }
    function alturaDe(chave) {
        const regiao = REGIOES[chave];
        if (!regiao) return SO_2D[chave].em;
        return regiao.parte === 'braco' ? OMBRO.y - regiao.em : regiao.em;
    }

    function atualizarTracos() {
        if (!rotulos.length || !comTracos()) return;
        const base = palco.getBoundingClientRect();
        const caixaFigura = figura.getBoundingClientRect();
        const fonte = silhueta;
        linhas.setAttribute('viewBox', `0 0 ${base.width} ${base.height}`);
        for (const r of rotulos) {
            const ancora = fonte.ancora(r.chave, r.lado);
            // No mapa dos músculos, medida sem músculo nesta vista (o peito nas costas) fica apagada
            r.li.classList.toggle('fora-da-vista', !ancora);
            if (!ancora) { r.traco.setAttribute('d', ''); r.bolinha.setAttribute('visibility', 'hidden'); continue; }
            const ax = caixaFigura.left - base.left + ancora.x;
            const ay = caixaFigura.top - base.top + ancora.y;
            const caixa = r.li.getBoundingClientRect();
            const lx = (r.lado < 0 ? caixa.right : caixa.left) - base.left;
            const ly = caixa.top - base.top + caixa.height / 2;
            const dobra = lx - r.lado * 18;
            r.traco.setAttribute('d', `M${lx.toFixed(1)},${ly.toFixed(1)}H${dobra.toFixed(1)}L${ax.toFixed(1)},${ay.toFixed(1)}`);
            r.bolinha.setAttribute('cx', ax.toFixed(1));
            r.bolinha.setAttribute('cy', ay.toFixed(1));
            r.bolinha.setAttribute('visibility', 'visible');
        }
    }

    function intensidades() {
        const a = aluno();
        const forca = {};
        if (a) for (const chave of CHAVES_ROTULO) if (a.medidas[chave]) forca[chave] = a.medidas[chave].intensidade;
        return forca;
    }

    function desenhar2d() {
        const a = aluno();
        const perfil = perfilDe(a, momento);
        avisoAltura.hidden = !a || !perfil.alturaPadrao;
        silhueta.trocarVista(vistaEscolhida);
        silhueta.ajustar(perfil);
        silhueta.desenhar(regioesComClasse(), intensidades());
    }

    /** Troca Frente/Costas com um fade curto (100 ms some, 100 ms volta; sem animação se o sistema pedir). */
    function mudarVista(vista) {
        botoesVista.forEach(b => {
            b.classList.toggle('active', b.dataset.vista === vista);
            b.setAttribute('aria-pressed', b.dataset.vista === vista);
        });
        if (vista === vistaEscolhida) return;
        vistaEscolhida = vista;
        mostrarDica(null);
        const aplicar = () => {
            desenhar2d();
            posicionarRotulos();
            destacar(null);
            atualizarTracos();
            raiz.classList.remove('mapa-trocando');
        };
        clearTimeout(trocaVista);
        if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) { aplicar(); return; }
        raiz.classList.add('mapa-trocando');
        trocaVista = setTimeout(aplicar, 100);
    }

    /** Dica sobre o músculo: nome, valor e diferença da medida, perto do ponteiro. */
    function mostrarDica(chave, e) {
        const a = aluno();
        const m = chave && a && a.medidas[chave];
        if (!m || !e) { dica.hidden = true; return; }
        const valorAtual = momento === 'primeira' ? m.primeira : m.ultima;
        const regiao = CHAVES_ROTULO.includes(chave) ? `${m.rotulo} (perímetro)` : m.rotulo;
        dica.textContent = [regiao, valorAtual, m.diferenca && `${m.diferenca} ${sentido(m.diferenca)}`].filter(Boolean).join(' · ');
        dica.className = `mapa-dica-musculo ${m.classe}`;
        dica.hidden = false;
        const caixa = figura.getBoundingClientRect();
        const x = limitar(e.clientX - caixa.left, 8, caixa.width - dica.offsetWidth - 8);
        dica.style.left = `${Math.round(x)}px`;
        dica.style.top = `${Math.round(Math.max(0, e.clientY - caixa.top - dica.offsetHeight - 14))}px`;
    }

    function regioesComClasse() {
        const a = aluno();
        const regioes = {};
        if (a) for (const chave of CHAVES_ROTULO) if (a.medidas[chave]) regioes[chave] = a.medidas[chave].classe;
        return regioes;
    }

    function atualizarFigura(semAnimar) {
        const a = aluno();
        const fatores = a ? fatoresDe(a, momento) : { ...FATOR_NEUTRO };
        desenhar2d();
        destacar(null);
        atualizarTracos();
    }

    function atualizarTudo(semAnimar) {
        montarRotulos();
        posicionarRotulos();
        atualizarFigura(semAnimar);
    }

    /* Controles */
    if (seletor) {
        seletor.addEventListener('change', () => {
            alunoAtual = seletor.value;
            atualizarTudo(false);
        });
    }
    botoesMomento.forEach(botao => botao.addEventListener('click', () => {
        momento = botao.dataset.momento;
        botoesMomento.forEach(b => {
            b.classList.toggle('active', b === botao);
            b.setAttribute('aria-pressed', b === botao);
        });
        const fixadoAntes = fixado;
        montarRotulos();
        posicionarRotulos();
        fixado = fixadoAntes;
        atualizarFigura(false);
    }));
    botoesVista.forEach(botao => botao.addEventListener('click', () => {
        mudarVista(botao.dataset.vista);
    }));
    // Músculos interativos nas duas vistas: passar o mouse destaca a pílula e mostra a dica; clicar fixa, como na pílula
    function apontarMusculo(chave, e) {
        mostrarDica(chave, e);
        destacar(chave);
    }
    function clicarMusculo(chave) {
        fixado = fixado === chave ? null : chave;
        destacar(null);
    }
    silhueta.aoApontar(apontarMusculo, clicarMusculo);

    // A aba começa escondida: só mede quando a figura aparece
    new ResizeObserver(() => {
        if (!figura.clientWidth) return;
        posicionarRotulos();
        atualizarTracos();
    }).observe(palco);

    atualizarTudo(true);
}

if (raiz && raiz.querySelector('.mapa-palco')) iniciar();
