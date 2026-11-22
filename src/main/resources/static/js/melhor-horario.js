// Cartão "Melhor horário para treinar" (aluno): troca o dia sem recarregar. Os 7 gráficos já vêm na página.
(function () {
    var cartao = document.getElementById('melhorHorario');
    if (!cartao) return;
    var chips = cartao.querySelectorAll('.melhor-chip');
    var paineis = cartao.querySelectorAll('.melhor-painel');
    chips.forEach(function (chip) {
        chip.addEventListener('click', function () {
            var dia = chip.getAttribute('data-dia');
            chips.forEach(function (c) {
                var ativo = c === chip;
                c.classList.toggle('ativo', ativo);
                c.setAttribute('aria-selected', ativo ? 'true' : 'false');
            });
            paineis.forEach(function (p) { p.hidden = p.getAttribute('data-painel') !== dia; });
        });
    });
})();
