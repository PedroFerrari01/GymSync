// Leitor do QR da carteirinha pela webcam (simulação de catraca).
// A imagem da câmera é lida só aqui no navegador (jsQR); o que vai ao servidor é apenas o código do QR.
(function () {
    const video = document.getElementById('leitorVideo');
    if (!video) return;
    const canvas = document.getElementById('leitorCanvas');
    const ctx = canvas.getContext('2d', { willReadFrequently: true });
    const status = document.getElementById('leitorStatus');
    const btnLigar = document.getElementById('leitorLigar');
    const btnDesligar = document.getElementById('leitorDesligar');
    const form = document.getElementById('leitorForm');
    const campo = document.getElementById('leitorCodigo');

    let fluxo = null;
    let lendo = false;

    function avisar(texto) { status.textContent = texto; }

    function desligar() {
        lendo = false;
        if (fluxo) {
            fluxo.getTracks().forEach(function (t) { t.stop(); });
            fluxo = null;
        }
        video.srcObject = null;
        btnLigar.hidden = false;
        btnDesligar.hidden = true;
    }

    function ler() {
        if (!lendo) return;
        if (video.readyState >= video.HAVE_ENOUGH_DATA && video.videoWidth) {
            canvas.width = video.videoWidth;
            canvas.height = video.videoHeight;
            ctx.drawImage(video, 0, 0, canvas.width, canvas.height);
            const img = ctx.getImageData(0, 0, canvas.width, canvas.height);
            const qr = window.jsQR ? window.jsQR(img.data, img.width, img.height, { inversionAttempts: 'dontInvert' }) : null;
            if (qr && /^GS1\.[0-9]{1,18}\.[A-Za-z0-9_-]{22}$/.test(qr.data.trim())) {
                campo.value = qr.data.trim();
                avisar('Carteirinha lida. Validando...');
                desligar();
                form.submit();
                return;
            }
            if (qr) avisar('Esse QR não é de uma carteirinha GymSync.');
        }
        requestAnimationFrame(ler);
    }

    btnLigar.addEventListener('click', async function () {
        if (!navigator.mediaDevices || !navigator.mediaDevices.getUserMedia) {
            avisar('Este navegador não dá acesso à câmera. Digite o código abaixo.');
            return;
        }
        try {
            fluxo = await navigator.mediaDevices.getUserMedia({ video: { facingMode: 'environment' }, audio: false });
            video.srcObject = fluxo;
            await video.play();
            lendo = true;
            btnLigar.hidden = true;
            btnDesligar.hidden = false;
            avisar('Câmera ligada. Aponte o QR da carteirinha para a câmera.');
            requestAnimationFrame(ler);
        } catch (e) {
            desligar();
            avisar(e && e.name === 'NotAllowedError'
                ? 'O acesso à câmera foi negado. Libere a câmera para este site ou digite o código abaixo.'
                : 'Não foi possível ligar a câmera. Digite o código abaixo.');
        }
    });

    btnDesligar.addEventListener('click', function () {
        desligar();
        avisar('Câmera desligada.');
    });

    // Sair da página sempre solta a câmera
    window.addEventListener('pagehide', desligar);
})();
