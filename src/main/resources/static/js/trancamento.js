/* TRANCAMENTO: o botão "Trancar" da lista de matrículas abre o modal com os dados daquela linha.
   Nada é enviado sozinho: o formulário só vai ao servidor quando o admin confirma.
   Os elementos são procurados no clique, porque os modais ficam depois dos scripts no HTML. */
(function () {
    'use strict';

    function iso(d) {
        var m = String(d.getMonth() + 1).padStart(2, '0');
        var dia = String(d.getDate()).padStart(2, '0');
        return d.getFullYear() + '-' + m + '-' + dia;
    }

    document.addEventListener('click', function (ev) {
        var botao = ev.target.closest ? ev.target.closest('.btn-trancar') : null;
        if (!botao) return;
        var form = document.getElementById('formTrancar');
        if (!form) return;
        form.setAttribute('action', form.getAttribute('action').replace(/\/\d+$/, '/') + botao.getAttribute('data-id'));
        document.getElementById('trancarAluno').textContent = botao.getAttribute('data-aluno') || '-';
        var hoje = new Date();
        var passado = new Date(); passado.setDate(passado.getDate() - 30);
        var amanha = new Date(); amanha.setDate(amanha.getDate() + 1);
        var inicio = document.getElementById('trancarInicio');
        var retorno = document.getElementById('trancarRetorno');
        inicio.min = iso(passado); inicio.max = iso(hoje); inicio.value = iso(hoje);
        retorno.min = iso(amanha); retorno.value = '';
        document.getElementById('trancarMotivo').value = '';
    });

    // O retorno precisa ser depois do início
    document.addEventListener('change', function (ev) {
        if (!ev.target || ev.target.id !== 'trancarInicio') return;
        var retorno = document.getElementById('trancarRetorno');
        if (!ev.target.value || !retorno) return;
        var d = new Date(ev.target.value + 'T00:00:00');
        d.setDate(d.getDate() + 1);
        retorno.min = iso(d);
        if (retorno.value && retorno.value < retorno.min) retorno.value = '';
    });
})();
