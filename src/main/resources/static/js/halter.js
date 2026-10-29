/* Abertura do "Quem somos": halter 3D (Three.js) que cai girando na frente do título conforme a rolagem.
   Em tela estreita ou com "reduzir movimento", o halter aparece parado. Sem WebGL, o canvas some. */
import * as THREE from '../vendor/three/three.module.min.js';

const secao = document.querySelector('.abertura');
const canvas = document.querySelector('.halter-canvas');
if (secao && canvas) iniciar();

// Halter ajustável cromado: barra rosqueada, anilhas com miolo rebaixado em laranja e porca sextavada na ponta
function montarHalter() {
    const halter = new THREE.Group();
    const cromo = new THREE.MeshStandardMaterial({ color: 0xeef0f4, metalness: 1, roughness: 0.16 });
    const laranja = new THREE.MeshStandardMaterial({ color: 0xf97316, metalness: 0.25, roughness: 0.4, emissive: 0x3a1400, side: THREE.DoubleSide });

    // Peças do Three nascem em pé (eixo Y); cada uma é deitada no eixo X do halter
    const deitar = (peca, x) => {
        peca.rotation.z = Math.PI / 2;
        peca.position.x = x;
        halter.add(peca);
    };
    const cilindro = (raio, comprimento, x, lados = 48) =>
        deitar(new THREE.Mesh(new THREE.CylinderGeometry(raio, raio, comprimento, lados), cromo), x);
    const rosca = (raio, x) => {
        const filete = new THREE.Mesh(new THREE.TorusGeometry(raio, 0.014, 8, 40), cromo);
        filete.rotation.y = Math.PI / 2;
        filete.position.x = x;
        halter.add(filete);
    };

    // Anilha: perfil girado (borda chanfrada, miolo rebaixado e cubo central) + disco laranja no rebaixo dos dois lados
    const anilha = (raio, espessura, x) => {
        const h = espessura / 2;
        const perfil = [
            [0.12, -h * 0.7], [0.3, -h * 0.7], [0.32, -h * 0.4], [raio * 0.7, -h * 0.4], [raio * 0.75, -h],
            [raio * 0.93, -h], [raio, -h * 0.55], [raio, h * 0.55], [raio * 0.93, h], [raio * 0.75, h],
            [raio * 0.7, h * 0.4], [0.32, h * 0.4], [0.3, h * 0.7], [0.12, h * 0.7]
        ].map(([r, y]) => new THREE.Vector2(r, y));
        const peca = new THREE.Group();
        peca.add(new THREE.Mesh(new THREE.LatheGeometry(perfil, 72), cromo));
        for (const face of [-1, 1]) {
            const miolo = new THREE.Mesh(new THREE.RingGeometry(0.33, raio * 0.69, 72), laranja);
            miolo.rotation.x = -face * Math.PI / 2;
            miolo.position.y = face * (h * 0.4 + 0.003);
            peca.add(miolo);
        }
        deitar(peca, x);
    };

    cilindro(0.1, 3.3, 0);                                            // barra
    for (let i = -6; i <= 6; i++) rosca(0.103, i * 0.07);            // pegada recartilhada
    for (const lado of [-1, 1]) {
        for (let i = 0; i < 9; i++) rosca(0.115, lado * (0.62 + i * 0.1)); // rosca das pontas
        cilindro(0.2, 0.08, lado * 0.66);                             // batente interno
        anilha(0.95, 0.2, lado * 0.82);
        anilha(0.95, 0.2, lado * 1.03);
        anilha(0.8, 0.18, lado * 1.23);
        cilindro(0.26, 0.16, lado * 1.42, 6);                         // porca sextavada
        cilindro(0.12, 0.1, lado * 1.55);                             // ponta da barra
    }
    return halter;
}

// Ambiente simples com faixas de luz, para o metal ter reflexo sem baixar imagem HDR
function ambiente(renderer) {
    const cena = new THREE.Scene();
    cena.background = new THREE.Color(0x4a4c55); // fundo médio + faixas muito fortes: contraste claro/escuro do cromo
    const luz = new THREE.MeshBasicMaterial({ color: new THREE.Color(3.5, 3.5, 3.5) });
    const quente = new THREE.MeshBasicMaterial({ color: 0xf97316 });
    [[0, 6, 0, 12, 1.2, luz], [-7, 1, 2, 1.2, 8, luz], [0, -6, 2, 10, 2, luz], [7, -1, 3, 1, 6, quente]].forEach(([x, y, z, w, h, m]) => {
        const faixa = new THREE.Mesh(new THREE.PlaneGeometry(w, h), m);
        faixa.position.set(x, y, z);
        faixa.lookAt(0, 0, 0);
        cena.add(faixa);
    });
    const pmrem = new THREE.PMREMGenerator(renderer);
    const textura = pmrem.fromScene(cena, 0.04).texture;
    pmrem.dispose();
    return textura;
}

function iniciar() {
    let renderer;
    try {
        renderer = new THREE.WebGLRenderer({ canvas, alpha: true, antialias: true });
    } catch (e) {
        canvas.remove();
        return;
    }
    // Desenha com pelo menos o dobro de pixels e reduz: bordas e reflexos nítidos mesmo em monitor comum
    renderer.setPixelRatio(Math.min(Math.max(window.devicePixelRatio * 2, 2), 3));
    renderer.toneMapping = THREE.ACESFilmicToneMapping;

    const cena = new THREE.Scene();
    cena.environment = ambiente(renderer);
    cena.add(new THREE.HemisphereLight(0xffffff, 0x1a1a22, 0.8));
    const principal = new THREE.DirectionalLight(0xffffff, 2.2);
    principal.position.set(4, 6, 6);
    cena.add(principal);
    const contraLuz = new THREE.DirectionalLight(0xf97316, 1.8);
    contraLuz.position.set(-6, -2, -4);
    cena.add(contraLuz);

    const camera = new THREE.PerspectiveCamera(35, 1, 0.1, 100);
    camera.position.z = 10;
    const halter = montarHalter();
    cena.add(halter);

    const largaEAnimada = () => window.matchMedia('(min-width: 992px) and (prefers-reduced-motion: no-preference)').matches;
    let alturaVisivel = 1;
    let deslocamentoX = 0; // na abertura animada o halter fica à direita, longe do texto
    let animado = false;
    // y em frações da altura visível; rotações em radianos
    const inicio = { y: 0.8, rx: 2.6, ry: 0.9, rz: -1.1 };
    const fim = { y: -0.27, rx: 0.32, ry: -0.35, rz: 0.06 };
    const estado = { ...fim };
    let giroLivre = 0;

    const desenhar = () => {
        halter.position.y = (animado ? estado.y : 0) * alturaVisivel;
        halter.position.x = deslocamentoX;
        halter.rotation.set(estado.rx, estado.ry + giroLivre, estado.rz);
        renderer.render(cena, camera);
    };

    const ajustarTamanho = () => {
        const { clientWidth: w, clientHeight: h } = canvas;
        if (!w || !h) return;
        renderer.setSize(w, h, false);
        camera.aspect = w / h;
        camera.updateProjectionMatrix();
        alturaVisivel = 2 * Math.tan(THREE.MathUtils.degToRad(camera.fov / 2)) * camera.position.z;
        const larguraVisivel = alturaVisivel * camera.aspect;
        deslocamentoX = animado ? larguraVisivel * 0.28 : 0;
        // O halter mede 3,2 de ponta a ponta: 40% da largura na abertura animada, 75% no celular, sem passar da altura
        const fracao = animado ? 0.34 : 0.75;
        halter.scale.setScalar(Math.min((larguraVisivel * fracao) / 3.2, (alturaVisivel * 0.55) / 2));
        desenhar();
    };
    window.addEventListener('resize', ajustarTamanho);

    animado = largaEAnimada() && typeof gsap !== 'undefined' && typeof ScrollTrigger !== 'undefined';
    ajustarTamanho();
    if (!animado) return;

    gsap.registerPlugin(ScrollTrigger);
    secao.classList.add('abertura-animada');
    ajustarTamanho();
    Object.assign(estado, inicio);
    let visivel = false;

    const linha = gsap.timeline({
        defaults: { ease: 'none' },
        scrollTrigger: {
            trigger: secao,
            start: 'top top',
            end: '+=130%',
            pin: true,
            scrub: 0.6,
            onToggle: self => { visivel = self.isActive; }
        },
        onUpdate: desenhar
    });
    linha.to(estado, { y: fim.y, duration: 0.7, ease: 'bounce.out' }, 0)
        .to(estado, { rx: fim.rx, ry: fim.ry, rz: fim.rz, duration: 0.7, ease: 'power2.out' }, 0)
        .fromTo('.halter-sombra', { opacity: 0, scaleX: 0.3 }, { opacity: 1, scaleX: 1, duration: 0.7, ease: 'power2.in' }, 0)
        .to('.abertura-texto', { scale: 0.96, duration: 1 }, 0);
    secao.querySelectorAll('.cartao-flutuante').forEach(cartao => {
        const profundidade = parseFloat(cartao.dataset.profundidade || '0.5');
        linha.fromTo(cartao, { y: 160 * profundidade }, { y: -220 * profundidade, duration: 1 }, 0);
    });

    // Depois de pousar, o halter gira bem devagar enquanto a abertura está na tela
    gsap.ticker.add(() => {
        if (!visivel || linha.progress() < 0.7) return;
        giroLivre += 0.003;
        desenhar();
    });
    desenhar();
}
