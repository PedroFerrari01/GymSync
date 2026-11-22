/*
 * Mapa do corpo em 3D, visual "body scan": modelo anatômico (BodyParts3D, CC BY 4.0) em grafite fosco com
 * contorno frio; as regiões medidas acendem na cor da situação (brilho pela intensidade, bloom no tema escuro).
 * Carregado sob demanda pelo mapa-corpo.js (import dinâmico). Tudo local: Three.js r169 em /vendor/three.
 *
 * Contrato igual ao da cena 3D antiga: mostrar, cores, destacar, ancora, redimensionar, aoPerder;
 * mais alturaRotulo, aoApontar, girarPara, mostrarLado e descartar.
 *
 * Objetos do modelo: REG_<REGIAO>_<D|E>_<musculo> (pintados) e NEUTRO_<nome> (resto do corpo).
 */
import * as THREE from '../vendor/three/three.module.min.js';
import { GLTFLoader } from '../vendor/three/GLTFLoader.js';
import { OrbitControls } from '../vendor/three/OrbitControls.js';
import { EffectComposer } from '../vendor/three/EffectComposer.js';
import { RenderPass } from '../vendor/three/RenderPass.js';
import { UnrealBloomPass } from '../vendor/three/UnrealBloomPass.js';
import { OutputPass } from '../vendor/three/OutputPass.js';

const URL_MODELO = new URL('../models/corpo-musculos.glb', import.meta.url).href;
const REGIAO_PARA_CHAVE = {
    OMBRO: 'ombro', PEITO: 'peito', BRACO: 'braco', ANTEBRACO: 'antebraco',
    CINTURA: 'cintura', QUADRIL: 'quadril', COXA: 'coxa', PANTURRILHA: 'panturrilha'
};
const CHAVES = Object.values(REGIAO_PARA_CHAVE);
// De que lado a região aparece melhor (para girar quando o usuário aponta a pílula)
const LADO_PREFERIDO = {
    ombro: 'frente', peito: 'frente', braco: 'frente', antebraco: 'frente',
    cintura: 'frente', quadril: 'costas', coxa: 'frente', panturrilha: 'costas'
};
const ACESAS = ['melhorou', 'piorou', 'estavel'];
const POSE = 0.38;            // 3/4 de frente
const POLAR = 1.64;           // câmera um pouco abaixo do centro, olhando de baixo para cima
const ALVO = new THREE.Vector3(0, 0.875, 0);
const OCUPA = 0.85;           // o corpo ocupa ~85% da altura do canvas
const TRANSICAO_MS = 250;
const GIRO_MS = 600;
const VARREDURA_MS = 1200;

let carga = null; // o .glb é baixado uma vez por página; voltar do 2D não baixa de novo

function carregarModelo(aoProgresso) {
    if (!carga) {
        carga = new Promise((ok, erro) => {
            new GLTFLoader().load(URL_MODELO, ok, ev => {
                if (aoProgresso && ev.total) aoProgresso(ev.loaded / ev.total);
            }, erro);
        });
        carga.catch(() => { carga = null; });
    }
    return carga;
}

/**
 * Bloom com fundo transparente: no r169 o desfoque do UnrealBloomPass grava alfa 1 e o canvas ficaria opaco.
 * Esta subclasse só troca o shader de desfoque por um que desfoca também o alfa (o resto é o original).
 */
class BrilhoComAlfa extends UnrealBloomPass {
    getSeperableBlurMaterial(raio) {
        const m = super.getSeperableBlurMaterial(raio);
        m.fragmentShader = m.fragmentShader
            .replace(/vec3 (diffuseSum|sample1|sample2)/g, 'vec4 $1')
            .replace(/\)\.rgb/g, ')')
            .replace('vec4(diffuseSum/weightSum, 1.0)', 'diffuseSum/weightSum');
        return m;
    }
}

const suave = t => 1 - Math.pow(1 - t, 3);
const angulo = a => Math.atan2(Math.sin(a), Math.cos(a));

/** Brilho radial laranja do pedestal, feito num canvas (nada externo). */
function texturaChao() {
    const c = document.createElement('canvas');
    c.width = c.height = 256;
    const g = c.getContext('2d');
    const grad = g.createRadialGradient(128, 128, 0, 128, 128, 128);
    grad.addColorStop(0, 'rgba(255,107,0,0.15)');
    grad.addColorStop(0.6, 'rgba(255,107,0,0.06)');
    grad.addColorStop(1, 'rgba(255,107,0,0)');
    g.fillStyle = grad;
    g.fillRect(0, 0, 256, 256);
    const t = new THREE.CanvasTexture(c);
    t.colorSpace = THREE.SRGBColorSpace;
    return t;
}

export async function criarCenaAnatomica(canvas, aoDesenhar, { aoProgresso } = {}) {
    const gltf = await carregarModelo(aoProgresso);

    const celular = window.matchMedia('(pointer: coarse)').matches || window.innerWidth < 768;
    const reduzido = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
    const razaoPixels = Math.min(window.devicePixelRatio || 1, celular ? 1.5 : 2);
    const renderer = new THREE.WebGLRenderer({ canvas, alpha: true, antialias: true });
    renderer.setPixelRatio(razaoPixels);
    renderer.toneMapping = THREE.ACESFilmicToneMapping;
    renderer.toneMappingExposure = 1.0;
    renderer.outputColorSpace = THREE.SRGBColorSpace;
    renderer.setClearColor(0x000000, 0);

    const cena = new THREE.Scene();
    const camera = new THREE.PerspectiveCamera(28, 1, 0.1, 30);

    /* Luz: ambiente baixo, principal suave de cima/frente e recorte frio por trás (presas à câmera) */
    const ambiente = new THREE.HemisphereLight(0xffffff, 0x0b0d12, 0.35);
    cena.add(ambiente);
    const mira = new THREE.Object3D();
    mira.position.set(0, 0, -4);
    camera.add(mira);
    const luz = (cor, forca, x, y, z) => {
        const l = new THREE.DirectionalLight(cor, forca);
        l.position.set(x, y, z);
        l.target = mira;
        camera.add(l);
        return l;
    };
    const principal = luz(0xffffff, 1.3, 0.8, 2.6, 1.2);
    const recorte = luz(0x8fb4ff, 3.0, -1.4, 1.6, -8.5);
    const recorte2 = luz(0x8fb4ff, 2.0, 1.5, 0.6, -8.5);
    cena.add(camera);

    /* Pedestal: anel fino laranja e brilho radial suave, sem grade */
    const brilhoChao = new THREE.Mesh(new THREE.CircleGeometry(0.62, 64),
        new THREE.MeshBasicMaterial({ map: texturaChao(), transparent: true, depthWrite: false, toneMapped: false }));
    brilhoChao.rotation.x = -Math.PI / 2;
    brilhoChao.position.y = 0.001;
    brilhoChao.renderOrder = -2;
    const anel = new THREE.Mesh(new THREE.RingGeometry(0.355, 0.372, 160),
        new THREE.MeshBasicMaterial({ transparent: true, opacity: 0.9, depthWrite: false, toneMapped: false }));
    anel.rotation.x = -Math.PI / 2;
    anel.position.y = 0.002;
    anel.renderOrder = -1;
    cena.add(brilhoChao, anel);

    /* Contorno (fresnel) e faixa de varredura: um trecho de shader em todos os materiais do corpo */
    const uniformes = {
        uBorda: { value: new THREE.Color(0x7da2d8) },
        uBordaForca: { value: 0.55 },
        uFaixaY: { value: -1 },
        uFaixaForca: { value: 0 }
    };
    function comBorda(mat) {
        mat.onBeforeCompile = sh => {
            Object.assign(sh.uniforms, uniformes);
            sh.vertexShader = 'varying float vMundoY;\n' + sh.vertexShader.replace('#include <project_vertex>',
                '#include <project_vertex>\n\tvMundoY = ( modelMatrix * vec4( transformed, 1.0 ) ).y;');
            sh.fragmentShader = 'uniform vec3 uBorda;\nuniform float uBordaForca;\nuniform float uFaixaY;\n'
                + 'uniform float uFaixaForca;\nvarying float vMundoY;\n'
                + sh.fragmentShader.replace('#include <emissivemap_fragment>', `#include <emissivemap_fragment>
	float bordaFres = pow( 1.0 - saturate( dot( normal, normalize( vViewPosition ) ) ), 2.5 );
	totalEmissiveRadiance += uBorda * bordaFres * uBordaForca;
	float faixa = exp( - pow( ( vMundoY - uFaixaY ) / 0.045, 2.0 ) );
	totalEmissiveRadiance += uBorda * faixa * uFaixaForca;`);
        };
        mat.customProgramCacheKey = () => 'gymsync-borda';
        return mat;
    }

    /* Materiais: grafite fosco para o corpo todo; um material por região medida */
    const fosco = { roughness: 0.8, metalness: 0 };
    const matBase = comBorda(new THREE.MeshStandardMaterial(fosco));
    const matCostas = comBorda(new THREE.MeshStandardMaterial({ ...fosco, transparent: true, opacity: 0.85, depthWrite: false }));
    const matRegiao = Object.fromEntries(CHAVES.map(c => [c, comBorda(new THREE.MeshStandardMaterial(fosco))]));

    const modelo = gltf.scene;
    const malhasRegiao = [];          // para o raio do mouse
    const malhasPorChave = Object.fromEntries(CHAVES.map(c => [c, []]));
    modelo.traverse(o => {
        if (!o.isMesh) return;
        const nome = o.name || '';
        const m = /^REG_([A-Z]+)_/.exec(nome);
        if (m && REGIAO_PARA_CHAVE[m[1]]) {
            const c = REGIAO_PARA_CHAVE[m[1]];
            o.material = matRegiao[c];
            o.userData.chave = c;
            malhasRegiao.push(o);
            malhasPorChave[c].push(o);
        } else if (nome === 'NEUTRO_pele_costas') {
            o.material = matCostas;   // cobre o vão das costas (o dataset não tem latíssimo do dorso)
            o.renderOrder = 3;
        } else {
            o.material = matBase;     // cabeça, mãos, pés, ossos e músculos sem medida: tudo igual à base
        }
    });
    modelo.rotation.set(0, 0, 0);
    cena.add(modelo);
    modelo.updateMatrixWorld(true);

    /* Pontos de cada região (faixa do meio da altura): altura dos rótulos e âncora */
    const amostras = {};
    const v = new THREE.Vector3(), n = new THREE.Vector3(), mn = new THREE.Matrix3();
    for (const c of CHAVES) {
        const pts = [];
        for (const malha of malhasPorChave[c]) {
            const pos = malha.geometry.attributes.position, nor = malha.geometry.attributes.normal;
            mn.getNormalMatrix(malha.matrixWorld);
            const passo = Math.max(1, Math.floor(pos.count / 160));
            for (let i = 0; i < pos.count; i += passo) {
                pts.push({
                    p: v.fromBufferAttribute(pos, i).applyMatrix4(malha.matrixWorld).clone(),
                    n: n.fromBufferAttribute(nor, i).applyMatrix3(mn).normalize().clone()
                });
            }
        }
        if (!pts.length) { amostras[c] = []; continue; }
        const ys = pts.map(q => q.p.y).sort((a, b) => a - b);
        const baixo = ys[Math.floor(ys.length * 0.3)], alto = ys[Math.floor(ys.length * 0.7)];
        amostras[c] = pts.filter(q => q.p.y >= baixo && q.p.y <= alto);
    }

    /* Pós-processamento: bloom só no que passa do limite de brilho (as regiões acesas) */
    const composer = new EffectComposer(renderer);
    composer.setPixelRatio(razaoPixels);
    composer.addPass(new RenderPass(cena, camera));
    const bloom = new BrilhoComAlfa(new THREE.Vector2(256, 256), 0.7, 0.5, 0.2);
    composer.addPass(bloom);
    composer.addPass(new OutputPass());
    let brilhoDoTema = true, brilhoCaiu = false;
    const usarBrilho = () => brilhoDoTema && !brilhoCaiu;

    /* Controles: só giro em volta do corpo (ângulo vertical travado), zoom curto, sem arrastar o quadro */
    const controles = new OrbitControls(camera, canvas);
    controles.target.copy(ALVO);
    controles.enablePan = false;
    controles.enableDamping = !reduzido;
    controles.dampingFactor = 0.08;
    controles.minPolarAngle = controles.maxPolarAngle = POLAR;
    controles.rotateSpeed = 0.7;
    controles.autoRotate = !reduzido;
    controles.autoRotateSpeed = 0.55;
    canvas.style.touchAction = 'pan-y'; // no celular, arrastar na vertical continua rolando a página

    let distancia = 4;
    function colocarCamera(az, dist = camera.position.distanceTo(ALVO) || distancia) {
        camera.position.set(
            ALVO.x + dist * Math.sin(POLAR) * Math.sin(az),
            ALVO.y + dist * Math.cos(POLAR),
            ALVO.z + dist * Math.sin(POLAR) * Math.cos(az));
        camera.lookAt(ALVO);
        controles.update();
    }

    /* Estado: cor de cada região (com transição), destaque, pulsação, giro animado, varredura */
    const cores = {
        base3d: '#151922', borda3d: '#7da2d8', anel3d: '#f97316',
        melhorou3d: '#10b981', piorou3d: '#ef4444', estavel3d: '#f97316', brilho3d: true
    };
    const corBase = new THREE.Color();
    let regioes = {}, forca = {};
    let destaque = null;
    const estado = Object.fromEntries(CHAVES.map(c => [c, {
        de: { cor: new THREE.Color(), emi: new THREE.Color(), int: 0 },
        para: { cor: new THREE.Color(), emi: new THREE.Color(), int: 0 },
        inicio: 0
    }]));
    let giro = null; // { de, para, inicio }
    let largura = 1, altura = 1, desenhado = false, perdido = false, descartado = false;
    let visivelNaTela = true, quadroPedido = 0, ultimoQuadro = 0;
    const tempos = [];
    let inicioVarredura = reduzido ? null : performance.now();
    const relogio = performance.now();

    function alvoDe(c) {
        const classe = regioes[c];
        const alvo = { cor: corBase.clone(), emi: new THREE.Color(0x000000), int: 0 };
        if (!ACESAS.includes(classe)) return alvo;   // sem medida, ou medida sem comparação: igual à base
        const status = new THREE.Color(cores[classe + '3d']);
        const forte = Math.max(0, Math.min(1, forca[c] || 0));
        alvo.emi.copy(status);
        if (usarBrilho()) {
            alvo.cor.lerp(status, 0.4);
            alvo.int = 0.4 + 1.0 * forte * forte;   // mínimo visível 0,4; a variação grande brilha bem mais
        } else {
            alvo.cor.lerp(status, 0.8 + 0.2 * forte);  // tema claro, sem bloom: cor saturada; a variação maior fica mais cheia
            alvo.int = 0.12 * forte;                  // brilho quase nenhum (clarearia e lavaria a cor)
        }
        return alvo;
    }

    function aplicarAlvos(semAnimar) {
        const agora = performance.now();
        for (const c of CHAVES) {
            const e = estado[c], mat = matRegiao[c];
            e.de.cor.copy(mat.color);
            e.de.emi.copy(mat.emissive);
            e.de.int = mat.userData.intBase ?? 0;
            e.para = alvoDe(c);
            e.inicio = semAnimar || reduzido ? agora - TRANSICAO_MS : agora;
        }
        pedirQuadro();
    }

    function atualizarMateriais(agora) {
        let mexendo = false;
        const pulso = reduzido ? 0 : Math.sin((agora - relogio) / 2400 * Math.PI * 2);
        for (const c of CHAVES) {
            const e = estado[c], mat = matRegiao[c];
            const t = Math.min(1, (agora - e.inicio) / TRANSICAO_MS);
            if (t < 1) mexendo = true;
            const k = suave(t);
            mat.color.copy(e.de.cor).lerp(e.para.cor, k);
            mat.emissive.copy(e.de.emi).lerp(e.para.emi, k);
            let intensidade = e.de.int + (e.para.int - e.de.int) * k;
            mat.userData.intBase = intensidade;
            if (regioes[c] === 'melhorou') intensidade *= 1 + 0.12 * pulso; // pulsa de leve só o que melhorou
            if (destaque) {
                if (c === destaque) {
                    // a região apontada acende mais; sem cor de medida, clareia com o tom do contorno
                    if (!ACESAS.includes(regioes[c])) { mat.emissive.set(cores.borda3d); intensidade = usarBrilho() ? 0.35 : 0.2; }
                    else intensidade = intensidade * 1.35 + (usarBrilho() ? 0.6 : 0.2);
                } else {
                    intensidade *= 0.25;                                       // as outras escurecem
                    mat.color.lerp(corBase, 0.6);
                }
            }
            mat.emissiveIntensity = intensidade;
        }
        return mexendo;
    }

    function atualizarVarredura(agora) {
        if (inicioVarredura === null) return false;
        const t = (agora - inicioVarredura) / VARREDURA_MS;
        if (t >= 1) { uniformes.uFaixaForca.value = 0; inicioVarredura = null; return false; }
        uniformes.uFaixaY.value = -0.05 + 1.85 * suave(t);
        uniformes.uFaixaForca.value = 1.6 * Math.min(1, (1 - t) * 4);
        return true;
    }

    const pulsando = () => !reduzido && Object.values(regioes).includes('melhorou');

    function desenhar() {
        if (usarBrilho()) composer.render();
        else renderer.render(cena, camera);
    }

    function quadro(agora) {
        quadroPedido = 0;
        if (perdido || descartado || !visivelNaTela || document.hidden) return;
        // Se o bloom deixar a animação lenta (celular fraco), desliga
        if (ultimoQuadro && usarBrilho()) {
            tempos.push(agora - ultimoQuadro);
            if (tempos.length > 60) tempos.shift();
            if (tempos.length === 60 && tempos.reduce((s, x) => s + x, 0) / 60 > 40) { brilhoCaiu = true; aplicarAlvos(true); }
        }
        let continuar = false;
        if (giro) {
            const t = Math.min(1, (agora - giro.inicio) / GIRO_MS);
            colocarCamera(giro.de + (giro.para - giro.de) * suave(t));
            if (t >= 1) giro = null; else continuar = true;
        } else if (controles.update()) {
            continuar = true;
        }
        if (controles.autoRotate) continuar = true;
        if (atualizarMateriais(agora)) continuar = true;
        if (atualizarVarredura(agora)) continuar = true;
        if (pulsando()) continuar = true;
        desenhar();
        desenhado = true;
        aoDesenhar();
        ultimoQuadro = continuar ? agora : 0;
        if (continuar) pedirQuadro();
    }
    function pedirQuadro() {
        if (!quadroPedido && !descartado) quadroPedido = requestAnimationFrame(quadro);
    }

    function pararGiroAutomatico() {
        controles.autoRotate = false;
    }
    function girarPara(az) {
        pararGiroAutomatico();
        const atual = controles.getAzimuthalAngle();
        const para = atual + angulo(az - atual);
        if (reduzido) { colocarCamera(para); pedirQuadro(); return; }
        giro = { de: atual, para, inicio: performance.now() };
        pedirQuadro();
    }

    controles.addEventListener('start', pararGiroAutomatico);
    controles.addEventListener('change', pedirQuadro);

    /* Teclado: setas giram; dois cliques voltam à pose inicial */
    const aoTeclar = e => {
        if (e.key !== 'ArrowLeft' && e.key !== 'ArrowRight') return;
        e.preventDefault();
        girarPara((giro ? giro.para : controles.getAzimuthalAngle()) + (e.key === 'ArrowLeft' ? -0.4 : 0.4));
    };
    const aoDuploClique = () => girarPara(POSE);
    canvas.addEventListener('keydown', aoTeclar);
    canvas.addEventListener('dblclick', aoDuploClique);

    /* Mouse sobre o músculo: raio só nas regiões medidas */
    const raio = new THREE.Raycaster();
    const ponteiro = new THREE.Vector2();
    let aoPassar = null, aoClicar = null, ultimaChave = null, inicioToque = null, pedidoRaio = 0, ultimoEvento = null;
    function chaveEm(e) {
        const caixa = canvas.getBoundingClientRect();
        ponteiro.set((e.clientX - caixa.left) / caixa.width * 2 - 1, -((e.clientY - caixa.top) / caixa.height) * 2 + 1);
        raio.setFromCamera(ponteiro, camera);
        const hit = raio.intersectObjects(malhasRegiao, false)[0];
        return hit ? hit.object.userData.chave : null;
    }
    const aoMover = e => {
        if (!aoPassar || e.buttons) return;
        ultimoEvento = e;
        if (pedidoRaio) return;
        pedidoRaio = requestAnimationFrame(() => {
            pedidoRaio = 0;
            const c = chaveEm(ultimoEvento);
            if (c !== ultimaChave || c) aoPassar(c, ultimoEvento);
            ultimaChave = c;
        });
    };
    const aoSair = e => { if (aoPassar) { ultimaChave = null; aoPassar(null, e); } };
    const aoApertar = e => { inicioToque = { x: e.clientX, y: e.clientY }; };
    const aoSoltar = e => {
        if (!inicioToque || !aoClicar) return;
        const andou = Math.hypot(e.clientX - inicioToque.x, e.clientY - inicioToque.y);
        inicioToque = null;
        if (andou > 5) return;
        const c = chaveEm(e);
        if (c) aoClicar(c);
    };
    canvas.addEventListener('pointermove', aoMover);
    canvas.addEventListener('pointerleave', aoSair);
    canvas.addEventListener('pointerdown', aoApertar);
    canvas.addEventListener('pointerup', aoSoltar);

    /* Só desenha com o canvas na tela e a aba visível */
    const observador = new IntersectionObserver(entradas => {
        visivelNaTela = entradas.some(e => e.isIntersecting);
        ultimoQuadro = 0;
        if (visivelNaTela) pedirQuadro();
    });
    observador.observe(canvas);
    const aoMudarAba = () => { ultimoQuadro = 0; if (!document.hidden) pedirQuadro(); };
    document.addEventListener('visibilitychange', aoMudarAba);

    let aoPerderCb = null;
    const aoPerderContexto = e => { e.preventDefault(); perdido = true; if (aoPerderCb) aoPerderCb(); };
    canvas.addEventListener('webglcontextlost', aoPerderContexto);

    colocarCamera(POSE, distancia);
    const projetado = new THREE.Vector3(), olhar = new THREE.Vector3();
    const emPixels = p => {
        projetado.copy(p).project(camera);
        return { x: (projetado.x + 1) / 2 * largura, y: (1 - projetado.y) / 2 * altura };
    };

    return {
        canvas,
        aoPerder(f) { aoPerderCb = f; },
        redimensionar() {
            const w = canvas.clientWidth, h = canvas.clientHeight;
            if (!w || !h) return;
            largura = w;
            altura = h;
            renderer.setSize(w, h, false);
            composer.setSize(w, h);
            camera.aspect = w / h;
            camera.updateProjectionMatrix();
            // Distância para o corpo (1,75 m) ocupar ~85% da altura; braços abertos (~1,1 m) cabem na largura
            const metade = Math.tan(THREE.MathUtils.degToRad(camera.fov / 2));
            const antes = distancia;
            distancia = Math.max(1.75 / OCUPA / 2 / metade, 0.56 / (metade * camera.aspect));
            controles.minDistance = distancia * 0.7;
            controles.maxDistance = distancia * 1.1;
            const atual = camera.position.distanceTo(ALVO);
            colocarCamera(controles.getAzimuthalAngle(), Math.min(controles.maxDistance,
                Math.max(controles.minDistance, atual * distancia / antes)));
            pedirQuadro();
        },
        /** fatores: não usados no modelo anatômico (a forma é fixa); regioes = { chave: classe }; forca = { chave: 0 a 1 }. */
        mostrar(_fatores, novasRegioes, semAnimar, novaForca) {
            regioes = { ...(novasRegioes || {}) };
            forca = { ...(novaForca || {}) };
            aplicarAlvos(semAnimar);
        },
        cores(c) {
            for (const k of Object.keys(cores)) if (c[k] !== undefined && c[k] !== '') cores[k] = c[k];
            brilhoDoTema = !!cores.brilho3d;
            corBase.set(cores.base3d);
            matBase.color.copy(corBase);
            matCostas.color.copy(corBase);
            uniformes.uBorda.value.set(cores.borda3d);
            uniformes.uBordaForca.value = brilhoDoTema ? 0.55 : 0.1;
            anel.material.color.set(cores.anel3d);
            // Escuro: ambiente baixo e recorte frio forte; claro: mais luz e recorte discreto
            ambiente.intensity = brilhoDoTema ? 0.35 : 0.75;
            principal.intensity = brilhoDoTema ? 1.3 : 1.5;
            renderer.toneMappingExposure = brilhoDoTema ? 1.0 : 0.9;
            recorte.intensity = brilhoDoTema ? 3.0 : 0.8;
            recorte2.intensity = brilhoDoTema ? 2.0 : 0.5;
            aplicarAlvos(true);
        },
        destacar(c) {
            destaque = c;
            pedirQuadro();
        },
        /** Gira até o lado em que a região aparece melhor (só se ela estiver do outro lado). */
        mostrarLado(c) {
            const lado = LADO_PREFERIDO[c];
            if (!lado) return;
            const az = giro ? giro.para : controles.getAzimuthalAngle();
            const deFrente = Math.cos(az) > 0.2, deCostas = Math.cos(az) < -0.2;
            if ((lado === 'frente' && deFrente) || (lado === 'costas' && deCostas)) { pararGiroAutomatico(); return; }
            girarPara(lado === 'costas' ? Math.PI + POSE : POSE);
        },
        girarPara(vista) {
            girarPara(vista === 'costas' ? Math.PI : 0);
        },
        /** Ponto da região mais para o lado pedido da tela (só a parte virada para a câmera), em pixels do canvas. */
        ancora(c, lado) {
            if (!desenhado) return null;
            let melhor = null;
            for (const q of amostras[c] || []) {
                olhar.copy(camera.position).sub(q.p);
                if (olhar.dot(q.n) <= 0) continue;
                const pt = emPixels(q.p);
                if (!melhor || lado * pt.x > lado * melhor.x) melhor = pt;
            }
            return melhor;
        },
        /** Altura (pixels) do meio da região, para alinhar o rótulo. */
        alturaRotulo(c) {
            const lista = amostras[c] || [];
            if (!lista.length || !largura) return null;
            let soma = 0;
            for (const q of lista) soma += emPixels(q.p).y;
            return soma / lista.length;
        },
        /** Ponteiro sobre um músculo medido: f(chave ou null, evento). Clique: g(chave). */
        aoApontar(f, g) { aoPassar = f; aoClicar = g; },
        /** Libera a GPU (ao voltar para o 2D). O .glb continua guardado para não baixar de novo. */
        descartar() {
            descartado = true;
            if (quadroPedido) cancelAnimationFrame(quadroPedido);
            observador.disconnect();
            document.removeEventListener('visibilitychange', aoMudarAba);
            canvas.removeEventListener('keydown', aoTeclar);
            canvas.removeEventListener('dblclick', aoDuploClique);
            canvas.removeEventListener('pointermove', aoMover);
            canvas.removeEventListener('pointerleave', aoSair);
            canvas.removeEventListener('pointerdown', aoApertar);
            canvas.removeEventListener('pointerup', aoSoltar);
            canvas.removeEventListener('webglcontextlost', aoPerderContexto);
            controles.dispose();
            cena.remove(modelo);
            modelo.traverse(o => { if (o.isMesh) o.geometry.dispose(); });
            [...Object.values(matRegiao), matBase, matCostas, brilhoChao.material, anel.material].forEach(m => m.dispose());
            brilhoChao.material.map.dispose();
            brilhoChao.geometry.dispose();
            anel.geometry.dispose();
            composer.dispose();
            renderer.dispose();
        }
    };
}
