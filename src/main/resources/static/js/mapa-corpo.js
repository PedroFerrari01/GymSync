/* Mapa do corpo (aba Avaliações Físicas): manequim 3D que gira, com uma fita em cada região medida
   (peito, braço, cintura, quadril e coxa) e a diferença entre a primeira e a última avaliação.
   - O manequim é feito aqui mesmo, em código (nenhum modelo de terceiros). As proporções são ilustrativas:
     cada região engrossa ou afina um pouco conforme a medida do aluno.
   - Os dados vêm do HTML (data-*), montados pelo EvolucaoService; a cor segue a regra de lá.
   - No botão "2D" (o padrão), sem WebGL ou em aparelho fraco aparece o mapa dos músculos em SVG
     (frente e costas), com cada músculo na cor da medida ligada a ele.
   - O Three.js só é baixado quando a aba aparece na tela. */

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
const CHAVES_RESUMO = ['peso', 'gordura', 'imc'];
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

function lerAlunos() {
    const alunos = new Map();
    raiz.querySelectorAll('.mapa-dados > [data-aluno]').forEach(bloco => {
        const medidas = {};
        bloco.querySelectorAll('[data-chave]').forEach(el => {
            const d = el.dataset;
            medidas[d.chave] = {
                chave: d.chave, rotulo: d.rotulo, primeira: d.primeira, ultima: d.ultima,
                primeiraNumero: parseFloat(d.primeiraNumero), ultimaNumero: parseFloat(d.ultimaNumero),
                diferenca: d.diferenca || null, classe: d.classe || 'neutro',
                intensidade: parseFloat(d.intensidade) || 0
            };
        });
        alunos.set(bloco.dataset.aluno, {
            nome: bloco.dataset.nome || '', avaliacoes: parseInt(bloco.dataset.avaliacoes || '0', 10),
            dataPrimeira: bloco.dataset.primeira || null, dataUltima: bloco.dataset.ultima || null,
            altura: parseFloat(bloco.dataset.altura) || null, medidas
        });
    });
    return alunos;
}

/* ---------- Mapa dos músculos 2D (frente e costas) ----------
   Polígonos do react-body-highlighter (MIT, ver static/vendor/react-body-highlighter/LICENSE).
   Cada músculo ligado a uma medida recebe a cor dela; os outros ficam na cor base. */

const SVG_NS = 'http://www.w3.org/2000/svg';
const VISTA = { largura: 100, altura: 200 };
const DESENHOS = { frente: anteriorData, costas: posteriorData };

// Medida → músculos de cada lado do corpo (só as medidas que a avaliação física tem)
const MUSCULOS = {
    frente: { peito: ['chest'], braco: ['biceps', 'triceps'], cintura: ['abs', 'obliques'], quadril: ['abductors'], coxa: ['quadriceps'] },
    costas: { braco: ['triceps'], cintura: ['lower-back'], quadril: ['gluteal'], coxa: ['hamstring'] }
};
const CHAVE_DO_MUSCULO = Object.fromEntries(Object.entries(MUSCULOS).map(([vista, mapa]) =>
    [vista, Object.fromEntries(Object.entries(mapa).flatMap(([chave, nomes]) => nomes.map(n => [n, chave])))]));

// Ao clicar na pílula, a região que aparece melhor de um lado só leva a figura para esse lado
const VISTA_PREFERIDA = { peito: 'frente', quadril: 'costas' };

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

/** Centro de cada polígono dos músculos de uma medida, numa vista (coordenadas do desenho). */
function centrosDe(vista, chave) {
    const nomes = MUSCULOS[vista][chave] || [];
    const centros = [];
    for (const item of DESENHOS[vista]) {
        if (!nomes.includes(item.muscle)) continue;
        for (const texto of item.svgPoints) {
            const pts = pontosDe(texto);
            centros.push({ x: pts.reduce((s, p) => s + p.x, 0) / pts.length, y: pts.reduce((s, p) => s + p.y, 0) / pts.length });
        }
    }
    return centros;
}

function criarSilhueta(svg) {
    svg.setAttribute('viewBox', `0 0 ${VISTA.largura} ${VISTA.altura}`);
    let vista = 'frente';

    /** regioes = { chave: classe }; forca = { chave: intensidade de 0 a 1 } (cor mais forte quanto maior a variação). */
    function desenhar(regioes, forca = {}) {
        svg.replaceChildren();
        for (const item of DESENHOS[vista]) {
            const chave = CHAVE_DO_MUSCULO[vista][item.muscle];
            const classe = chave && regioes[chave] ? ` medido ${regioes[chave]}` : '';
            for (const texto of item.svgPoints) {
                const el = noSvg('polygon', { points: texto.trim(), class: `mapa-musculo${classe}` });
                if (classe) {
                    el.dataset.chave = chave;
                    if (regioes[chave] !== 'neutro') el.style.fillOpacity = (0.45 + 0.55 * (forca[chave] || 0)).toFixed(2); // vence o CSS (atributo não venceria)
                }
                svg.appendChild(el);
            }
        }
    }

    // Do desenho (viewBox) para pixels dentro da figura
    function emPixels(pt) {
        const caixa = svg.getBoundingClientRect();
        if (!caixa.width) return null;
        const escala = Math.min(caixa.width / VISTA.largura, caixa.height / VISTA.altura);
        return {
            x: (caixa.width - VISTA.largura * escala) / 2 + pt.x * escala,
            y: (caixa.height - VISTA.altura * escala) / 2 + pt.y * escala
        };
    }

    /** Centro do músculo da medida mais para o lado pedido; sem músculo nesta vista, null. */
    function ancora(chave, lado) {
        let melhor = null;
        for (const c of centrosDe(vista, chave)) if (!melhor || lado * c.x > lado * melhor.x) melhor = c;
        return melhor && emPixels(melhor);
    }

    /** Altura (em pixels) da medida para alinhar o rótulo; usa a frente quando a vista não tem o músculo. */
    function alturaRotulo(chave) {
        const centros = centrosDe(vista, chave).length ? centrosDe(vista, chave) : centrosDe('frente', chave);
        if (!centros.length) return null;
        const p = emPixels({ x: 0, y: centros.reduce((s, c) => s + c.y, 0) / centros.length });
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
        desenhar, ancora, alturaRotulo, destacar, aoApontar,
        vista: () => vista,
        trocarVista(nova) { vista = nova; }
    };
}

/* ---------- Cena 3D ---------- */

function criarCena3D(THREE, canvas, aoDesenhar) {
    const renderer = new THREE.WebGLRenderer({ canvas, alpha: true, antialias: true });
    renderer.setPixelRatio(Math.min(window.devicePixelRatio || 1, 2));

    const cena = new THREE.Scene();
    const camera = new THREE.PerspectiveCamera(28, 1, 0.1, 30);
    const centro = new THREE.Vector3(0, 0.9, 0);
    cena.add(new THREE.HemisphereLight(0xffffff, 0x30323c, 1.5));
    const principal = new THREE.DirectionalLight(0xffffff, 2.1);
    principal.position.set(1.6, 2.6, 3.2);
    cena.add(principal);
    const contraLuz = new THREE.DirectionalLight(0xf97316, 2.4); // recorte laranja por trás, como no halter
    contraLuz.position.set(-2.4, 1.2, -3);
    cena.add(contraLuz);

    const corpo = new THREE.Group();
    cena.add(corpo);
    const pele = new THREE.MeshStandardMaterial({ color: 0xc7cbd6, roughness: 0.58, metalness: 0.06 });
    const materialFita = {
        melhorou: new THREE.MeshBasicMaterial({ toneMapped: false }),
        piorou: new THREE.MeshBasicMaterial({ toneMapped: false }),
        neutro: new THREE.MeshBasicMaterial({ toneMapped: false })
    };
    const materialChao = new THREE.MeshBasicMaterial({ transparent: true, opacity: 0.07, depthWrite: false });
    const materialBorda = new THREE.MeshBasicMaterial({ transparent: true, opacity: 0.55 });

    // Malha "torneada" pelas seções: uma volta de vértices por seção, fechada em cima e embaixo
    const LADOS = 40;
    function criarMalha(nSecoes, invertida) {
        const geometria = new THREE.BufferGeometry();
        geometria.setAttribute('position', new THREE.BufferAttribute(new Float32Array((nSecoes * LADOS + 2) * 3), 3));
        const indices = [];
        const tri = (a, b, c) => (invertida ? indices.push(a, c, b) : indices.push(a, b, c));
        for (let i = 0; i < nSecoes - 1; i++) {
            for (let j = 0; j < LADOS; j++) {
                const a = i * LADOS + j, b = (i + 1) * LADOS + j;
                const c = i * LADOS + (j + 1) % LADOS, d = (i + 1) * LADOS + (j + 1) % LADOS;
                tri(a, b, c);
                tri(c, b, d);
            }
        }
        const fundo = nSecoes * LADOS, topo = fundo + 1, ultima = (nSecoes - 1) * LADOS;
        for (let j = 0; j < LADOS; j++) {
            tri((j + 1) % LADOS, fundo, j);
            tri(ultima + j, topo, ultima + (j + 1) % LADOS);
        }
        geometria.setIndex(indices);
        return geometria;
    }
    function moldar(geometria, secoes, ladoX, noBraco) {
        const pos = geometria.attributes.position;
        const y = s => (noBraco ? -s.p : s.p);
        secoes.forEach((s, i) => {
            for (let j = 0; j < LADOS; j++) {
                const ang = (j / LADOS) * Math.PI * 2;
                pos.setXYZ(i * LADOS + j, ladoX * s.cx + s.rx * Math.cos(ang), y(s), s.cz + s.rz * Math.sin(ang));
            }
        });
        const primeira = secoes[0], ultima = secoes[secoes.length - 1];
        pos.setXYZ(secoes.length * LADOS, ladoX * primeira.cx, y(primeira), primeira.cz);
        pos.setXYZ(secoes.length * LADOS + 1, ladoX * ultima.cx, y(ultima), ultima.cz);
        pos.needsUpdate = true;
        geometria.computeVertexNormals();
        geometria.computeBoundingSphere();
    }

    const nSecoes = parte => secoesDe(parte, FATOR_NEUTRO).length;
    const pecas = []; // { geometria, parte, ladoX, noBraco }
    const malha = (parte, ladoX, noBraco, pai) => {
        const geometria = criarMalha(nSecoes(parte), noBraco);
        pecas.push({ geometria, parte, ladoX, noBraco });
        const m = new THREE.Mesh(geometria, pele);
        pai.add(m);
        return m;
    };
    const elipsoide = (rx, ry, rz, pai) => {
        const m = new THREE.Mesh(new THREE.SphereGeometry(1, 40, 28), pele);
        m.scale.set(rx, ry, rz);
        pai.add(m);
        return m;
    };

    malha('tronco', 1, false, corpo);
    elipsoide(CABECA.rx, CABECA.ry, CABECA.rz, corpo).position.set(0, CABECA.y, CABECA.z);
    const bracos = {};
    for (const lado of [-1, 1]) {
        malha('perna', lado, false, corpo);
        elipsoide(PE.rx, PE.ry, PE.rz, corpo).position.set(lado * PE.x, PE.y, PE.z);
        const braco = new THREE.Group();
        braco.position.set(lado * OMBRO.x, OMBRO.y, 0);
        braco.rotation.z = lado * OMBRO.giro;
        corpo.add(braco);
        malha('braco', 1, true, braco);
        elipsoide(MAO.rx, MAO.ry, MAO.rz, braco).position.y = -MAO.p;
        elipsoide(0.056, 0.054, 0.06, braco).position.y = 0.002; // ombro
        bracos[lado] = braco;
    }

    // Chão: disco quase invisível com uma borda fina, para o corpo não parecer solto no ar
    const chao = new THREE.Mesh(new THREE.CircleGeometry(0.42, 64), materialChao);
    chao.rotation.x = -Math.PI / 2;
    const borda = new THREE.Mesh(new THREE.RingGeometry(0.414, 0.42, 96), materialBorda);
    borda.rotation.x = -Math.PI / 2;
    borda.position.y = 0.001;
    cena.add(chao, borda);

    // Fitas: um tubo fino em volta de cada região (elipse do manequim sem ajuste; a escala acompanha o fator)
    class Elipse extends THREE.Curve {
        constructor(a, b) { super(); this.a = a; this.b = b; }
        getPoint(t, alvo = new THREE.Vector3()) {
            const ang = t * Math.PI * 2;
            return alvo.set(this.a * Math.cos(ang), 0, this.b * Math.sin(ang));
        }
    }
    const aneis = {};
    for (const chave of CHAVES_REGIAO) {
        const regiao = REGIOES[chave];
        const s = secaoEm(secoesDe(regiao.parte, FATOR_NEUTRO), regiao.em);
        const a = s.rx * 1.02 + 0.004, b = s.rz * 1.02 + 0.004;
        aneis[chave] = [];
        const lados = regiao.parte === 'tronco' ? [0] : [-1, 1];
        for (const lado of lados) {
            const fita = new THREE.Mesh(new THREE.TubeGeometry(new Elipse(a, b), 96, 0.0062, 10, true), materialFita.neutro);
            const brilho = new THREE.Mesh(new THREE.TubeGeometry(new Elipse(a, b), 96, 0.016, 10, true),
                new THREE.MeshBasicMaterial({ transparent: true, opacity: 0, depthWrite: false, toneMapped: false }));
            const grupo = new THREE.Group();
            grupo.add(fita, brilho);
            if (regiao.parte === 'braco') {
                grupo.position.y = -regiao.em;
                bracos[lado].add(grupo);
            } else {
                grupo.position.set(lado * s.cx, regiao.em, s.cz);
                corpo.add(grupo);
            }
            grupo.visible = false;
            aneis[chave].push({ grupo, fita, brilho, a, b });
        }
    }

    /* Estado e animação: só desenha quando algo muda (arrasto, inércia, troca de medida) */
    const reduzido = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
    const POSE = 0.34; // leve giro para a figura não ficar chapada de frente
    let giro = reduzido ? POSE : POSE - 0.56;
    let velocidade = reduzido ? 0 : 0.045;
    let giroAlvo = null;
    let inclinacao = 0.1;
    let arrastando = false;
    let destaque = null;
    let largura = 1, altura = 1;
    let quadroPedido = 0;
    let perdido = false;
    let desenhado = false;
    const fatores = { ...FATOR_NEUTRO };
    const fatoresAlvo = { ...FATOR_NEUTRO };

    function remodelar() {
        for (const peca of pecas) moldar(peca.geometria, secoesDe(peca.parte, fatores), peca.ladoX, peca.noBraco);
        for (const chave of CHAVES_REGIAO) aneis[chave].forEach(anel => anel.grupo.scale.set(fatores[chave], 1, fatores[chave]));
    }

    function posicionarCamera() {
        const metade = Math.tan(THREE.MathUtils.degToRad(camera.fov / 2));
        const distancia = Math.max(1.02 / metade, 0.46 / (metade * camera.aspect));
        camera.position.set(0, centro.y + Math.sin(inclinacao) * distancia, Math.cos(inclinacao) * distancia);
        camera.lookAt(centro);
    }

    function quadro() {
        quadroPedido = 0;
        if (perdido) return;
        let continuar = false;
        if (!arrastando) {
            if (giroAlvo !== null) {
                const falta = giroAlvo - giro;
                if (Math.abs(falta) > 0.002) { giro += falta * 0.14; continuar = true; } else { giro = giroAlvo; giroAlvo = null; }
            } else if (Math.abs(velocidade) > 0.0005) {
                giro += velocidade;
                velocidade *= 0.92;
                continuar = true;
            }
        }
        let mudou = false;
        for (const chave of CHAVES_REGIAO) {
            const falta = fatoresAlvo[chave] - fatores[chave];
            if (falta === 0) continue;
            if (Math.abs(falta) > 0.0008 && !reduzido) { fatores[chave] += falta * 0.16; continuar = true; } else fatores[chave] = fatoresAlvo[chave];
            mudou = true;
        }
        if (mudou) remodelar();
        for (const chave of CHAVES_REGIAO) {
            for (const anel of aneis[chave]) {
                const alvo = chave === destaque ? 0.32 : 0;
                const falta = alvo - anel.brilho.material.opacity;
                if (Math.abs(falta) > 0.01 && !reduzido) { anel.brilho.material.opacity += falta * 0.25; continuar = true; } else anel.brilho.material.opacity = alvo;
            }
        }
        corpo.rotation.y = giro;
        posicionarCamera();
        renderer.render(cena, camera);
        desenhado = true;
        aoDesenhar();
        if (continuar) pedirQuadro();
    }
    function pedirQuadro() {
        if (!quadroPedido) quadroPedido = requestAnimationFrame(quadro);
    }

    /* Girar: arrastar (mouse ou dedo), setas do teclado; dois cliques voltam à pose inicial */
    let ultimoX = 0, ultimoY = 0;
    canvas.addEventListener('pointerdown', e => {
        arrastando = true;
        giroAlvo = null;
        velocidade = 0;
        ultimoX = e.clientX;
        ultimoY = e.clientY;
        canvas.setPointerCapture(e.pointerId);
        canvas.classList.add('arrastando');
    });
    canvas.addEventListener('pointermove', e => {
        if (!arrastando) return;
        const dx = (e.clientX - ultimoX) * 0.011;
        giro += dx;
        velocidade = dx;
        if (e.pointerType === 'mouse') inclinacao = limitar(inclinacao + (e.clientY - ultimoY) * 0.004, -0.12, 0.42);
        ultimoX = e.clientX;
        ultimoY = e.clientY;
        pedirQuadro();
    });
    const soltar = () => {
        if (!arrastando) return;
        arrastando = false;
        canvas.classList.remove('arrastando');
        if (reduzido) velocidade = 0;
        pedirQuadro();
    };
    canvas.addEventListener('pointerup', soltar);
    canvas.addEventListener('pointercancel', soltar);
    canvas.addEventListener('keydown', e => {
        if (e.key !== 'ArrowLeft' && e.key !== 'ArrowRight') return;
        e.preventDefault();
        giroAlvo = (giroAlvo === null ? giro : giroAlvo) + (e.key === 'ArrowLeft' ? -0.4 : 0.4);
        pedirQuadro();
    });
    canvas.addEventListener('dblclick', () => {
        // volta pela volta mais curta
        giro = Math.atan2(Math.sin(giro - POSE), Math.cos(giro - POSE)) + POSE;
        giroAlvo = POSE;
        inclinacao = 0.1;
        pedirQuadro();
    });

    const quat = new THREE.Quaternion();
    const direcao = new THREE.Vector3();
    const ponto = new THREE.Vector3();

    return {
        canvas,
        aoPerder(f) {
            canvas.addEventListener('webglcontextlost', e => { e.preventDefault(); perdido = true; f(); });
        },
        redimensionar() {
            const w = canvas.clientWidth, h = canvas.clientHeight;
            if (!w || !h) return;
            largura = w;
            altura = h;
            renderer.setSize(w, h, false);
            camera.aspect = w / h;
            camera.updateProjectionMatrix();
            pedirQuadro();
        },
        /** fatores = quanto cada região engrossa; regioes = { chave: classe } das que têm medida. */
        mostrar(novosFatores, regioes, semAnimar) {
            Object.assign(fatoresAlvo, novosFatores);
            if (semAnimar) { Object.assign(fatores, novosFatores); remodelar(); }
            for (const chave of CHAVES_REGIAO) {
                aneis[chave].forEach(anel => {
                    anel.grupo.visible = !!regioes[chave];
                    if (regioes[chave]) {
                        anel.fita.material = materialFita[regioes[chave]] || materialFita.neutro;
                        anel.brilho.material.color.copy(anel.fita.material.color);
                    }
                });
            }
            pedirQuadro();
        },
        cores(c) {
            pele.color.set(c.pele);
            for (const classe in materialFita) materialFita[classe].color.set(c[classe]);
            materialChao.color.set(c.texto);
            materialBorda.color.set(c.borda);
            for (const chave of CHAVES_REGIAO) aneis[chave].forEach(anel => anel.brilho.material.color.copy(anel.fita.material.color));
            pedirQuadro();
        },
        destacar(chave) {
            destaque = chave;
            pedirQuadro();
        },
        /** Ponto da fita mais para o lado pedido da tela, em pixels dentro do canvas. */
        ancora(chave, lado) {
            let melhor = null;
            if (!desenhado) return null; // antes do primeiro quadro a câmera ainda não está no lugar
            for (const anel of aneis[chave] || []) {
                if (!anel.grupo.visible) continue;
                anel.grupo.getWorldQuaternion(quat).invert();
                direcao.set(lado, 0, 0).applyQuaternion(quat);
                const n = Math.hypot(anel.a * direcao.x, anel.b * direcao.z) || 1;
                ponto.set(anel.a * anel.a * direcao.x / n, 0, anel.b * anel.b * direcao.z / n);
                anel.grupo.localToWorld(ponto).project(camera);
                const x = (ponto.x + 1) / 2 * largura, y = (1 - ponto.y) / 2 * altura;
                if (!melhor || lado * x > lado * melhor.x) melhor = { x, y };
            }
            return melhor;
        }
    };
}

/* ---------- Tela ---------- */

function iniciar() {
    const alunos = lerAlunos();
    const palco = raiz.querySelector('.mapa-palco');
    const figura = raiz.querySelector('.mapa-figura');
    const canvas = raiz.querySelector('.mapa-canvas');
    const linhas = raiz.querySelector('.mapa-linhas');
    const colunas = { '-1': raiz.querySelector('.mapa-esq'), '1': raiz.querySelector('.mapa-dir') };
    const resumo = raiz.querySelector('.mapa-resumo');
    const periodo = raiz.querySelector('.mapa-periodo');
    const semMedidas = raiz.querySelector('.mapa-sem-medidas');
    const seletor = raiz.querySelector('.mapa-aluno');
    const botoesMomento = raiz.querySelectorAll('[data-momento]');
    const botoesModo = raiz.querySelectorAll('[data-modo]');
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
    let cena3d = null;
    let carregando3d = false;
    let visivel = false;
    let fixado = null;
    let rotulos = []; // { chave, lado, li, traco, bolinha }

    const guardado = (() => { try { return localStorage.getItem('gymsync-mapa-modo'); } catch (e) { return null; } })();
    const aparelhoFraco = (navigator.deviceMemory && navigator.deviceMemory <= 2)
        || (navigator.hardwareConcurrency && navigator.hardwareConcurrency <= 2);
    // O mapa dos músculos (2D) é o padrão; o 3D fica no botão
    let modo = guardado === '2d' || guardado === '3d' ? guardado : '2d';
    if (aparelhoFraco) modo = '2d';
    let sem3d = false;

    const aluno = () => alunos.get(alunoAtual);
    const usando3d = () => modo === '3d' && cena3d && !sem3d;

    function coresDoTema() {
        const estilo = getComputedStyle(raiz);
        const cor = nome => estilo.getPropertyValue(nome).trim();
        return {
            pele: cor('--mapa-pele'), melhorou: cor('--mapa-melhorou'), piorou: cor('--mapa-piorou'),
            neutro: cor('--mapa-neutro'), texto: cor('--mapa-chao'), borda: cor('--mapa-neutro')
        };
    }

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
        if (cena3d) cena3d.destacar(ativa);
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
        for (const chave of CHAVES_REGIAO) {
            const m = a.medidas[chave];
            if (!m) continue;
            const lado = REGIOES[chave].lado;
            const li = elemento('li', `mapa-rotulo ${m.classe}`);
            li.tabIndex = 0;
            li.append(elemento('span', 'mapa-rotulo-nome', m.rotulo), elemento('span', 'mapa-rotulo-valor', valor(m)));
            if (m.diferenca) li.append(elemento('span', `evo-dif ${m.classe}`, m.diferenca));
            li.addEventListener('mouseenter', () => destacar(chave));
            li.addEventListener('mouseleave', () => destacar(null));
            li.addEventListener('focus', () => destacar(chave));
            li.addEventListener('blur', () => destacar(null));
            li.addEventListener('click', () => {
                fixado = fixado === chave ? null : chave;
                destacar(null);
                const preferida = VISTA_PREFERIDA[chave];
                if (fixado && preferida && !usando3d()) mudarVista(preferida);
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
        const grupos = { melhorou: 'Melhorou', piorou: 'Piorou', neutro: 'Sem cor' };
        for (const classe in grupos) {
            const doGrupo = Object.values(a.medidas).filter(m => (m.classe || 'neutro') === classe);
            if (!doGrupo.length) continue;
            const li = elemento('li');
            const texto = doGrupo.map(m => (m.diferenca && classe !== 'neutro' ? `${m.rotulo} (${m.diferenca})` : m.rotulo)).join(', ');
            li.append(elemento('span', `mapa-ponto ${classe}`), elemento('span', null, `${grupos[classe]}: ${texto}`));
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
        // Altura do rótulo no palco: no 3D pela forma do manequim, no 2D pelo centro do músculo
        const meioDe = chave => {
            const y = usando3d() ? null : silhueta.alturaRotulo(chave);
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
        return regiao.parte === 'braco' ? OMBRO.y - regiao.em : regiao.em;
    }

    function atualizarTracos() {
        if (!rotulos.length || !comTracos()) return;
        const base = palco.getBoundingClientRect();
        const caixaFigura = figura.getBoundingClientRect();
        const fonte = usando3d() ? cena3d : silhueta;
        linhas.setAttribute('viewBox', `0 0 ${base.width} ${base.height}`);
        for (const r of rotulos) {
            const ancora = fonte.ancora(r.chave, r.lado);
            // No mapa dos músculos, medida sem músculo nesta vista (o peito nas costas) fica apagada
            r.li.classList.toggle('fora-da-vista', !ancora && !usando3d());
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
        if (a) for (const chave of CHAVES_REGIAO) if (a.medidas[chave]) forca[chave] = a.medidas[chave].intensidade;
        return forca;
    }

    function desenhar2d() {
        silhueta.trocarVista(vistaEscolhida);
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
        dica.textContent = [m.rotulo, valorAtual, m.diferenca].filter(Boolean).join(' · ');
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
        if (a) for (const chave of CHAVES_REGIAO) if (a.medidas[chave]) regioes[chave] = a.medidas[chave].classe;
        return regioes;
    }

    function atualizarFigura(semAnimar) {
        const a = aluno();
        const fatores = a ? fatoresDe(a, momento) : { ...FATOR_NEUTRO };
        desenhar2d();
        if (cena3d) cena3d.mostrar(fatores, regioesComClasse(), semAnimar);
        destacar(null);
        atualizarTracos();
    }

    function atualizarTudo(semAnimar) {
        montarRotulos();
        posicionarRotulos();
        atualizarFigura(semAnimar);
    }

    function aplicarModo() {
        raiz.classList.toggle('mapa-em-3d', usando3d());
        botoesModo.forEach(b => {
            const ativo = b.dataset.modo === (usando3d() || (modo === '3d' && !sem3d) ? '3d' : '2d');
            b.classList.toggle('active', ativo);
            b.setAttribute('aria-pressed', ativo);
            if (b.dataset.modo === '3d') {
                b.disabled = sem3d;
                if (sem3d) b.title = 'Este aparelho não conseguiu abrir o 3D';
            }
        });
        grupoVista.hidden = usando3d(); // Frente/Costas é do mapa dos músculos; o 3D gira
        if (usando3d()) cena3d.redimensionar();
        posicionarRotulos();
        atualizarTracos();
    }

    function desistirDo3d() {
        sem3d = true;
        cena3d = null;
        aplicarModo();
    }

    async function carregar3d() {
        if (cena3d || carregando3d || sem3d || modo !== '3d' || !visivel) return;
        carregando3d = true;
        try {
            // Sem WebGL nem adianta baixar o Three.js: fica a silhueta
            if (!document.createElement('canvas').getContext('webgl2')) throw new Error('sem WebGL');
            const THREE = await import('../vendor/three/three.module.min.js');
            cena3d = criarCena3D(THREE, canvas, atualizarTracos);
            cena3d.aoPerder(desistirDo3d);
            cena3d.cores(coresDoTema());
            const a = aluno();
            cena3d.mostrar(a ? fatoresDe(a, momento) : { ...FATOR_NEUTRO }, regioesComClasse(), true);
            aplicarModo();
        } catch (erro) {
            desistirDo3d();
        }
        carregando3d = false;
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
    botoesModo.forEach(botao => botao.addEventListener('click', () => {
        modo = botao.dataset.modo;
        try { localStorage.setItem('gymsync-mapa-modo', modo); } catch (e) { /* sem armazenamento: vale só nesta visita */ }
        aplicarModo();
        carregar3d();
    }));
    botoesVista.forEach(botao => botao.addEventListener('click', () => {
        mudarVista(botao.dataset.vista);
    }));
    // Músculos interativos nas duas vistas: passar o mouse destaca a pílula e mostra a dica; clicar fixa, como na pílula
    silhueta.aoApontar((chave, e) => {
        mostrarDica(chave, e);
        destacar(chave);
    }, chave => {
        fixado = fixado === chave ? null : chave;
        destacar(null);
    });
    canvas.addEventListener('pointerdown', () => raiz.classList.add('mapa-ja-girou'), { once: true });

    // Tema claro/escuro: as cores do 3D vêm das variáveis CSS
    new MutationObserver(() => { if (cena3d) cena3d.cores(coresDoTema()); })
        .observe(document.documentElement, { attributes: true, attributeFilter: ['data-theme'] });

    // A aba começa escondida: só mede e carrega o 3D quando a figura aparece
    new ResizeObserver(() => {
        if (!figura.clientWidth) return;
        posicionarRotulos();
        if (usando3d()) cena3d.redimensionar();
        atualizarTracos();
    }).observe(palco);
    new IntersectionObserver(entradas => {
        visivel = entradas.some(e => e.isIntersecting);
        if (visivel) carregar3d();
    }).observe(figura);

    atualizarTudo(true);
    aplicarModo();
}

if (raiz && raiz.querySelector('.mapa-palco')) iniciar();
