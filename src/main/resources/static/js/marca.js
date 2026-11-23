/* Identidade da academia (cor e logo): sincroniza as cores predefinidas com a cor própria
   e mostra a prévia da cor e da logo antes de salvar. Nada vai ao servidor aqui. */
(function () {
    'use strict';
    var raiz = document.getElementById('identidade-academia');
    if (!raiz) return;

    var radios = raiz.querySelectorAll('input[name="cor"]');
    var seletor = raiz.querySelector('.id-cor-seletor');
    var texto = raiz.querySelector('.id-cor-texto');
    var previa = raiz.querySelector('.identidade-previa');
    var arquivo = raiz.querySelector('#id-logo');
    var imagem = raiz.querySelector('.id-logo-previa');
    var valida = /^#[0-9a-fA-F]{6}$/;

    function pintar(cor) {
        if (previa) previa.style.setProperty('--previa', cor);
    }
    function corAtual() {
        for (var i = 0; i < radios.length; i++) if (radios[i].checked) return radios[i].value;
        return '#f97316';
    }

    for (var i = 0; i < radios.length; i++) {
        radios[i].addEventListener('change', function () {
            texto.value = '';
            seletor.value = this.value;
            pintar(this.value);
        });
    }
    seletor.addEventListener('input', function () {
        texto.value = seletor.value;
        for (var j = 0; j < radios.length; j++) radios[j].checked = false;
        pintar(seletor.value);
    });
    texto.addEventListener('input', function () {
        var v = texto.value.trim();
        if (v && v.charAt(0) !== '#') v = '#' + v;
        if (valida.test(v)) {
            seletor.value = v.toLowerCase();
            for (var j = 0; j < radios.length; j++) radios[j].checked = false;
            pintar(v);
        }
    });
    arquivo.addEventListener('change', function () {
        var f = arquivo.files && arquivo.files[0];
        if (!f || !imagem) return;
        imagem.src = URL.createObjectURL(f);
    });

    var inicial = valida.test(texto.value) ? texto.value : corAtual();
    seletor.value = inicial;
    pintar(inicial);
})();
