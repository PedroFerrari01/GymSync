/* METAS: botão "Editar" leva os valores da meta para o formulário (nada é enviado sozinho). */
(function () {
    'use strict';
    var form = document.getElementById('formMeta');
    if (!form) return;
    document.addEventListener('click', function (ev) {
        var botao = ev.target.closest ? ev.target.closest('.btn-editar-meta') : null;
        if (!botao) return;
        var tipo = form.querySelector('[name=tipo]');
        var alvo = form.querySelector('[name=alvo]');
        var prazo = form.querySelector('[name=prazo]');
        tipo.value = botao.getAttribute('data-tipo') || tipo.value;
        alvo.value = botao.getAttribute('data-alvo') || '';
        prazo.value = botao.getAttribute('data-prazo') || '';
        form.scrollIntoView({ behavior: window.matchMedia('(prefers-reduced-motion: reduce)').matches ? 'auto' : 'smooth', block: 'center' });
        alvo.focus();
    });
})();
