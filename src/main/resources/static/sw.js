// GymSync - service worker do "instalar no celular".
// Tudo vem sempre da internet. O cache só é usado sem conexão: arquivos visuais (css, js, ícones)
// e a página "sem conexão". Páginas com dados do aluno nunca são guardadas.
const CACHE = 'gymsync-v1';
const OFFLINE = '/offline.html';
const BASICOS = [OFFLINE, '/css/gymsync.css', '/css/gymsync-tema.css', '/img/app/icone-192.png',
    '/vendor/bootstrap/bootstrap.min.css', '/vendor/bootstrap-icons/bootstrap-icons.min.css'];

self.addEventListener('install', evento => {
    evento.waitUntil(caches.open(CACHE).then(c => c.addAll(BASICOS)).then(() => self.skipWaiting()));
});

self.addEventListener('activate', evento => {
    evento.waitUntil(
        caches.keys()
            .then(nomes => Promise.all(nomes.filter(n => n !== CACHE).map(n => caches.delete(n))))
            .then(() => self.clients.claim())
    );
});

function estatico(url) {
    return /^\/(css|js|vendor|img)\//.test(url.pathname);
}

self.addEventListener('fetch', evento => {
    const pedido = evento.request;
    const url = new URL(pedido.url);
    if (pedido.method !== 'GET' || url.origin !== self.location.origin) return;

    if (pedido.mode === 'navigate') {
        evento.respondWith(fetch(pedido).catch(() => caches.match(OFFLINE)));
        return;
    }
    if (estatico(url)) {
        evento.respondWith(
            fetch(pedido)
                .then(resposta => {
                    if (resposta.ok) {
                        const copia = resposta.clone();
                        caches.open(CACHE).then(c => c.put(pedido, copia));
                    }
                    return resposta;
                })
                .catch(() => caches.match(pedido))
        );
    }
});
