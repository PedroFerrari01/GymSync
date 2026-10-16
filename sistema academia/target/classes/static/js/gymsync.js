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
if (buscaGlobal) {
    buscaGlobal.addEventListener('input', function () {
        const termo = this.value.toLowerCase();
        document.querySelectorAll('.tabela-filtravel tbody tr').forEach(tr => {
            tr.style.display = tr.textContent.toLowerCase().includes(termo) ? '' : 'none';
        });
    });
}

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

document.querySelectorAll('.btn-editar-ficha').forEach(btn => {
    btn.addEventListener('click', () => {
        document.getElementById('editFichaId').value = btn.dataset.id;
        document.getElementById('editFichaDescricao').value = btn.dataset.descricao;
    });
});

document.querySelectorAll('.btn-editar-exercicio').forEach(btn => {
    btn.addEventListener('click', () => {
        document.getElementById('editExercicioId').value = btn.dataset.id;
        document.getElementById('editExercicioNome').value = btn.dataset.exercicio;
        document.getElementById('editExercicioSeries').value = btn.dataset.series;
        document.getElementById('editExercicioReps').value = btn.dataset.repeticoes;
    });
});
