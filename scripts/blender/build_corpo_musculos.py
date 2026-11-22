"""
Monta o modelo 3D anatômico do mapa do corpo (GymSync) a partir do BodyParts3D 4.0 (pacote "isa", OBJ 99%).

Fonte: BodyParts3D, © The Database Center for Life Science licensed under CC Attribution 4.0 International.

Uso (PowerShell, na raiz do projeto):
    & "C:\\Program Files\\Blender Foundation\\Blender 5.3\\blender.exe" --background --factory-startup `
      --python scripts/blender/build_corpo_musculos.py -- --obj assets-fonte/isa/isa_BP3D_4.0_obj_99

Saídas:
    src/main/resources/static/models/corpo-musculos.glb   (sem Draco: a CSP não permite WebAssembly)
    docs/preview-corpo-frente.png, docs/preview-corpo-costas.png, docs/preview-corpo-regioes.png

Nomes dos objetos no .glb:
    REG_<REGIAO>_<D|E>_<musculo>   músculos medidos (pintados pelo mapa)
    NEUTRO_<nome>                  resto do corpo (ossos e músculos sem medida)

Sistema do dataset: milímetros, Z para cima, frente em -Y, esquerda do corpo em +X.
É o mesmo do Blender; o exportador glTF passa para Y para cima e frente em +Z.
"""
import argparse
import math
import os
import re
import sys

import bmesh
import bpy
from mathutils import Matrix, Vector

RAIZ = os.path.abspath(os.path.join(os.path.dirname(__file__), '..', '..'))

# --- Regras de seleção (nome em inglês, sem "right"/"left", minúsculo) -------------------------------

REGIOES = [
    # (regex, REGIAO, musculo)
    (r'deltoid$', 'OMBRO', 'deltoide'),
    (r'pectoralis major$', 'PEITO', 'peitoral_maior'),
    (r'^serratus anterior$', 'PEITO', 'serratil_anterior'),
    (r'biceps brachii$', 'BRACO', 'biceps'),
    (r'triceps brachii$', 'BRACO', 'triceps'),
    (r'^brachioradialis$', 'ANTEBRACO', 'braquiorradial'),
    (r'^(extensor carpi radialis (longus|brevis)|extensor carpi ulnaris|extensor digitorum|anconeus'
     r'|abductor pollicis longus|extensor pollicis brevis)$', 'ANTEBRACO', 'extensores'),
    (r'(flexor carpi radialis|flexor carpi ulnaris|palmaris longus|pronator teres'
     r'|flexor digitorum superficialis)$', 'ANTEBRACO', 'flexores'),
    (r'^external oblique$', 'CINTURA', 'obliquo_externo'),
    (r'^(iliocostalis lumborum|iliocostalis thoracis|longissimus thoracis)$', 'CINTURA', 'eretor_espinha'),
    (r'^gluteus maximus$', 'QUADRIL', 'gluteo_maximo'),
    (r'^gluteus medius$', 'QUADRIL', 'gluteo_medio'),
    (r'^rectus femoris$', 'COXA', 'reto_femoral'),
    (r'^vastus lateralis$', 'COXA', 'vasto_lateral'),
    (r'^vastus medialis$', 'COXA', 'vasto_medial'),
    (r'^vastus intermedius$', 'COXA', 'vasto_intermedio'),
    (r'^sartorius$', 'COXA', 'sartorio'),
    (r'^(adductor (longus|magnus|brevis|minimus)|gracilis)$', 'COXA', 'adutores'),
    (r'biceps femoris$', 'COXA', 'biceps_femoral'),
    (r'^semitendinosus$', 'COXA', 'semitendineo'),
    (r'^semimembranosus$', 'COXA', 'semimembranaceo'),
    (r'gastrocnemius$', 'PANTURRILHA', 'gastrocnemio'),
    (r'^soleus$', 'PANTURRILHA', 'soleo'),
    (r'^tibialis anterior$', 'PANTURRILHA', 'tibial_anterior'),
]

NEUTROS = [
    # (regex, grupo, separa_lado)
    (r'trapezius$', 'trapezio', False),
    (r'^(sternocleidomastoid|levator scapulae)$', 'pescoco', False),
    (r'^(rhomboid major|rhomboid minor|infraspinatus muscle|teres major)$', 'costas', False),
    (r'^(brachialis|coracobrachialis)$', 'braco_profundo', False),
    (r'^(pectineus|iliotibial tract|fibularis (longus|brevis|tertius)|extensor digitorum longus'
     r'|extensor hallucis longus|calcaneal tendon)$', 'perna', False),
    # ossos
    (r'^(frontal bone|parietal bone|occipital bone|temporal bone|sphenoid bone|zygomatic bone|maxilla'
     r'|mandible|nasal bone|lacrimal bone)$', 'cranio', False),
    (r'(cervical|thoracic|lumbar) vertebra$|^(atlas|axis|sacrum|coccyx)$', 'coluna', False),
    (r'\brib$|costal cartilage$|^(body of sternum|manubrium|xiphoid process)$', 'torax', False),
    (r'^(clavicle|scapula)$', 'cintura_escapular', False),
    (r'^(humerus|radius|ulna)$', 'ossos_braco', False),
    (r'^(scaphoid|lunate|triquetral|pisiform|trapezium|trapezoid|capitate|hamate)$'
     r'|metacarpal bone$|phalanx of (thumb|index finger|middle finger|ring finger|little finger)$', 'maos', False),
    (r'^hip bone$', 'pelve', False),
    (r'^(femur|patella|tibia|fibula)$', 'ossos_perna', False),
    (r'^(talus|calcaneus|navicular bone of foot|cuboid bone|(medial|intermediate|lateral) cuneiform bone)$'
     r'|metatarsal bone$|phalanx of (big toe|second toe|third toe|fourth toe|little toe)$', 'pes', False),
]

EXCLUIR = re.compile(r'artery|vein|nerve|branch|tributary|ligament|bursa|membrane|gyrus')

ORCAMENTO_MUSCULOS = 56000   # triângulos
ORCAMENTO_NEUTROS = 20000
MIN_TRIS = 60
ALTURA_ALVO = 1.75

PELE = (0.70, 0.72, 0.78)          # --mapa-pele (#b4bac6) em sRGB
NEUTRO = (0.36, 0.40, 0.47)        # --mapa-neutro escuro (#475569)
CORES_REGIAO = {                  # só para o preview das regiões
    'OMBRO': (0.98, 0.62, 0.20), 'PEITO': (0.94, 0.33, 0.31), 'BRACO': (0.30, 0.66, 0.98),
    'ANTEBRACO': (0.55, 0.45, 0.95), 'CINTURA': (0.98, 0.82, 0.22), 'QUADRIL': (0.92, 0.38, 0.72),
    'COXA': (0.20, 0.80, 0.55), 'PANTURRILHA': (0.25, 0.85, 0.88),
}


def ler_args():
    argv = sys.argv[sys.argv.index('--') + 1:] if '--' in sys.argv else []
    p = argparse.ArgumentParser()
    p.add_argument('--obj', default=os.path.join(RAIZ, 'assets-fonte', 'isa', 'isa_BP3D_4.0_obj_99'))
    p.add_argument('--glb', default=os.path.join(RAIZ, 'src', 'main', 'resources', 'static', 'models',
                                                 'corpo-musculos.glb'))
    p.add_argument('--docs', default=os.path.join(RAIZ, 'docs'))
    p.add_argument('--sem-preview', action='store_true')
    return p.parse_args(argv)


def lado_de(nome):
    n = nome.lower()
    if re.search(r'\bright\b', n):
        return 'D'
    if re.search(r'\bleft\b', n):
        return 'E'
    return ''


def normalizar(nome):
    n = nome.lower()
    n = re.sub(r'\b(right|left)\b ?', '', n)
    return re.sub(r'\s+', ' ', n).strip()


def cabecalho(caminho):
    h = {}
    with open(caminho, encoding='utf-8', errors='ignore') as f:
        for linha in f:
            if not linha.startswith('#'):
                break
            m = re.match(r'# (Concept ID|English name) : (.*)', linha.strip())
            if m:
                h[m.group(1)] = m.group(2).strip()
    return h


def classificar(nome):
    if EXCLUIR.search(nome.lower()):
        return None
    base = normalizar(nome)
    lado = lado_de(nome)
    for rx, regiao, musculo in REGIOES:
        if re.search(rx, base):
            return f'REG_{regiao}_{lado or "C"}_{musculo}'
    for rx, grupo, separa in NEUTROS:
        if re.search(rx, base):
            return f'NEUTRO_{grupo}' + (f'_{lado}' if separa and lado else '')
    return None


def ler_obj(caminho, verts, faces):
    """Acrescenta vértices e faces (triângulos) do OBJ às listas, com deslocamento de índice."""
    base = len(verts)
    with open(caminho, encoding='utf-8', errors='ignore') as f:
        for linha in f:
            if linha.startswith('v '):
                _, x, y, z = linha.split()[:4]
                verts.append((float(x), float(y), float(z)))
            elif linha.startswith('f '):
                idx = [int(p.split('/')[0]) for p in linha.split()[1:]]
                idx = [i - 1 + base if i > 0 else len(verts) + i for i in idx]
                for k in range(1, len(idx) - 1):
                    faces.append((idx[0], idx[k], idx[k + 1]))


def compactar(verts, faces):
    """Fica só com os vértices usados pelas faces."""
    novo = {}
    vs = []
    fs = []
    for f in faces:
        t = []
        for i in f:
            if i not in novo:
                novo[i] = len(vs)
                vs.append(verts[i])
            t.append(novo[i])
        fs.append(tuple(t))
    return vs, fs


def ler_vertices(caminho):
    with open(caminho, encoding='utf-8', errors='ignore') as f:
        return [tuple(map(float, l.split()[1:4])) for l in f if l.startswith('v ')]


def material(nome, cor, alpha=1.0, emissao=None):
    m = bpy.data.materials.get(nome) or bpy.data.materials.new(nome)
    m.use_nodes = True
    bsdf = m.node_tree.nodes.get('Principled BSDF')
    bsdf.inputs['Base Color'].default_value = (*srgb_para_linear(cor), 1.0)
    bsdf.inputs['Roughness'].default_value = 0.55
    bsdf.inputs['Alpha'].default_value = alpha
    if emissao:
        bsdf.inputs['Emission Color'].default_value = (*srgb_para_linear(emissao), 1.0)
        bsdf.inputs['Emission Strength'].default_value = 0.25
    if alpha < 1.0:
        if hasattr(m, 'surface_render_method'):
            m.surface_render_method = 'BLENDED'
        if hasattr(m, 'blend_method'):
            try:
                m.blend_method = 'BLEND'
            except (TypeError, AttributeError):
                pass
        m.use_backface_culling = True
    return m


def srgb_para_linear(c):
    return tuple(((v + 0.055) / 1.055) ** 2.4 if v > 0.04045 else v / 12.92 for v in c)


def area(verts, faces):
    total = 0.0
    for a, b, c in faces:
        va, vb, vc = Vector(verts[a]), Vector(verts[b]), Vector(verts[c])
        total += (vb - va).cross(vc - va).length * 0.5
    return total


def decimar(obj, alvo):
    tris = len(obj.data.polygons)
    if tris <= alvo:
        return
    mod = obj.modifiers.new('dec', 'DECIMATE')
    mod.ratio = max(alvo / tris, 0.002)
    mod.use_collapse_triangulate = True
    with bpy.context.temp_override(object=obj, active_object=obj, selected_objects=[obj]):
        bpy.ops.object.modifier_apply(modifier=mod.name)


def limpar(me):
    """Tira faces degeneradas e vértices soltos que a decimação deixa (o exportador glTF reclama deles)."""
    bm = bmesh.new()
    bm.from_mesh(me)
    bmesh.ops.dissolve_degenerate(bm, edges=bm.edges, dist=1e-6)
    soltos = [v for v in bm.verts if not v.link_faces]
    bmesh.ops.delete(bm, geom=soltos, context='VERTS')
    bmesh.ops.triangulate(bm, faces=bm.faces)
    bm.to_mesh(me)
    bm.free()
    me.validate(clean_customdata=True)


def montar(args):
    bpy.ops.wm.read_factory_settings(use_empty=True)
    arquivos = sorted(f for f in os.listdir(args.obj) if f.lower().endswith('.obj'))
    grupos = {}
    pele = None
    for f in arquivos:
        h = cabecalho(os.path.join(args.obj, f))
        if h.get('English name', '').lower() == 'skin':
            pele = f
        alvo = classificar(h.get('English name', ''))
        if alvo:
            grupos.setdefault(alvo, []).append((f, h.get('English name', '')))

    print(f'[corpo] {len(grupos)} objetos a partir de {sum(len(v) for v in grupos.values())} OBJ')
    with open(os.path.join(RAIZ, 'assets-fonte', 'mapeamento.tsv'), 'w', encoding='utf-8') as log:
        for alvo in sorted(grupos):
            for f, nome in grupos[alvo]:
                log.write(f'{alvo}\t{f}\t{nome}\n')

    dados = {}
    for alvo, itens in grupos.items():
        verts, faces = [], []
        for f, _ in itens:
            ler_obj(os.path.join(args.obj, f), verts, faces)
        dados[alvo] = (verts, faces, area(verts, faces))

    mat_musculo = material('musculo', PELE)
    mat_neutro = material('neutro', NEUTRO, alpha=0.6)
    mat_opaco = material('neutro_opaco', NEUTRO)
    mat_costas = material('pele_costas', NEUTRO, alpha=0.82)

    def vertices_de(grupo, nomes=r'.'):
        return [v for f, nome in grupos.get(grupo, []) if re.search(nomes, normalizar(nome))
                for v in ler_vertices(os.path.join(args.obj, f))]

    def topo(grupo, nomes):
        zs = [v[2] for v in vertices_de(grupo, nomes)]
        return max(zs) if zs else None

    # Pele (FJ2810): cabeça, mãos e pés opacos, no lugar de crânio e ossos de mãos e pés, e os vãos das costas
    # (o dataset não tem latíssimo do dorso) semitransparentes, para as costelas não aparecerem.
    punho = topo('NEUTRO_maos', r'^(scaphoid|lunate|triquetral|pisiform)$')
    tornozelo = topo('NEUTRO_pes', r'^talus$') + 30.0
    pescoco = min(v[2] for v in vertices_de('NEUTRO_cranio', r'^mandible$')) - 10  # logo abaixo do queixo
    pv, pf = [], []
    ler_obj(os.path.join(args.obj, pele), pv, pf)

    def centro(f):
        return [sum(pv[i][k] for i in f) / 3 for k in range(3)]

    def caixa(vs):
        return [min(v[k] for v in vs) for k in range(3)], [max(v[k] for v in vs) for k in range(3)]

    maos = vertices_de('NEUTRO_maos')
    caixas_maos = [caixa([v for v in maos if (v[0] > 0) == lado]) for lado in (True, False)]

    def na_mao(c):
        return any(all(a[k] - 20 <= c[k] <= b[k] + 20 for k in range(2)) for a, b in caixas_maos)

    costelas = vertices_de('NEUTRO_torax', r'rib$')
    meio_x = sum(v[0] for v in costelas) / len(costelas)
    meio_y = sum(v[1] for v in costelas) / len(costelas)
    largura = max(abs(v[0] - meio_x) for v in costelas) + 15
    eretor = sorted(abs(v[0] - meio_x) for k, (vs, _, _) in dados.items() if 'eretor' in k for v in vs)
    dentro = eretor[int(len(eretor) * 0.6)]
    crista = topo('NEUTRO_pelve', r'.') - 20
    escapula = topo('NEUTRO_cintura_escapular', r'^scapula$')

    pecas = {'NEUTRO_cabeca': [], 'NEUTRO_maos': [], 'NEUTRO_pes': [], 'NEUTRO_pele_costas': []}
    for f in pf:
        c = centro(f)
        if min(pv[i][2] for i in f) > pescoco:
            pecas['NEUTRO_cabeca'].append(f)
        elif max(pv[i][2] for i in f) < punho + 10 and na_mao(c):
            pecas['NEUTRO_maos'].append(f)
        elif max(pv[i][2] for i in f) < tornozelo:
            pecas['NEUTRO_pes'].append(f)
        elif crista < c[2] < escapula and c[1] > meio_y and dentro < abs(c[0] - meio_x) < largura:
            pecas['NEUTRO_pele_costas'].append(f)
    for nome in ('NEUTRO_cranio', 'NEUTRO_maos', 'NEUTRO_pes'):
        dados.pop(nome, None)
    for nome, faces in pecas.items():
        vs, fs = compactar(pv, faces)
        dados[nome] = (vs, fs, area(vs, fs))
    print(f'[corpo] pele: pescoço z={pescoco:.0f}, punho z={punho:.0f}, tornozelo z={tornozelo:.0f} (mm); '
          + ', '.join(f'{k}={len(v)}' for k, v in pecas.items()))

    # Escala: altura do conjunto (pele) -> 1,75 m; pés em z=0; centro em x e y
    todos = [vs[i] for vs, fs, _ in dados.values() for f in fs for i in f]
    mn = [min(v[i] for v in todos) for i in range(3)]
    mx = [max(v[i] for v in todos) for i in range(3)]
    escala = ALTURA_ALVO / (mx[2] - mn[2])
    cx, cy = (mn[0] + mx[0]) / 2, (mn[1] + mx[1]) / 2
    print(f'[corpo] altura original {mx[2] - mn[2]:.1f} mm, escala {escala:.6f}')

    # Tendões do antebraço e da perna entram nos dedos: corta o que fica abaixo do punho e do tornozelo,
    # para mãos e pés continuarem neutros (o mapa não mede mão nem pé).
    corte = {'REG_ANTEBRACO': punho, 'REG_PANTURRILHA': tornozelo}
    for alvo, (verts, faces, _) in list(dados.items()):
        z = next((v for k, v in corte.items() if alvo.startswith(k) and v is not None), None)
        if z is not None:
            faces = [f for f in faces if max(verts[i][2] for i in f) > z]
            dados[alvo] = (verts, faces, area(verts, faces))
    print(f'[corpo] corte no punho z={corte["REG_ANTEBRACO"]}, no tornozelo z={corte["REG_PANTURRILHA"]} (mm)')

    area_m = sum(a for k, (_, _, a) in dados.items() if k.startswith('REG_'))
    area_n = sum(a for k, (_, _, a) in dados.items() if k.startswith('NEUTRO_'))
    for alvo, (verts, faces, a) in sorted(dados.items()):
        vs = [((x - cx) * escala, (y - cy) * escala, (z - mn[2]) * escala) for x, y, z in verts]
        me = bpy.data.meshes.new(alvo)
        me.from_pydata(vs, [], faces)
        me.validate(clean_customdata=True)
        bm = bmesh.new()
        bm.from_mesh(me)
        bmesh.ops.remove_doubles(bm, verts=bm.verts, dist=0.00005)
        bm.to_mesh(me)
        bm.free()
        ob = bpy.data.objects.new(alvo, me)
        bpy.context.scene.collection.objects.link(ob)
        musculo = alvo.startswith('REG_')
        orc, tot = (ORCAMENTO_MUSCULOS, area_m) if musculo else (ORCAMENTO_NEUTROS, area_n)
        decimar(ob, max(MIN_TRIS, int(orc * a / tot)))
        limpar(ob.data)
        ob.data.shade_smooth()
        if musculo:
            mat = mat_musculo
        elif alvo in ('NEUTRO_cabeca', 'NEUTRO_maos', 'NEUTRO_pes'):
            mat = mat_opaco
        elif alvo == 'NEUTRO_pele_costas':
            mat = mat_costas
        else:
            mat = mat_neutro
        ob.data.materials.append(mat)
        # Origem no centro da peça: quem desenha transparência ordena os objetos pela origem
        centro_ob = sum((v.co for v in ob.data.vertices), Vector()) / max(len(ob.data.vertices), 1)
        ob.data.transform(Matrix.Translation(-centro_ob))
        ob.location = centro_ob

    tris = sum(len(o.data.polygons) for o in bpy.context.scene.objects if o.type == 'MESH')
    print(f'[corpo] {tris} triângulos no total')
    return tris


def exportar(args):
    os.makedirs(os.path.dirname(args.glb), exist_ok=True)
    bpy.ops.export_scene.gltf(
        filepath=args.glb, export_format='GLB', export_yup=True, export_apply=True,
        export_normals=True, export_texcoords=False, export_materials='EXPORT',
        export_draco_mesh_compression_enable=False, export_cameras=False, export_lights=False,
        export_extras=False, export_animations=False)
    print(f'[corpo] {args.glb}: {os.path.getsize(args.glb) / 1e6:.2f} MB')


# --- Previews ------------------------------------------------------------------------------------------

def preparar_render():
    cena = bpy.context.scene
    for motor in ('BLENDER_EEVEE', 'BLENDER_EEVEE_NEXT'):
        try:
            cena.render.engine = motor
            break
        except TypeError:
            continue
    cena.render.resolution_x, cena.render.resolution_y = 900, 1400
    cena.render.film_transparent = False
    cena.view_settings.view_transform = 'AgX' if 'AgX' in [i.identifier for i in
                                                           cena.view_settings.bl_rna.properties['view_transform'].enum_items] else 'Filmic'
    mundo = bpy.data.worlds.new('fundo')
    mundo.use_nodes = True
    mundo.node_tree.nodes['Background'].inputs['Color'].default_value = (*srgb_para_linear((0.043, 0.043, 0.055)), 1)
    mundo.node_tree.nodes['Background'].inputs['Strength'].default_value = 1.0
    cena.world = mundo

    def luz(nome, tipo, energia, pos, cor=(1, 1, 1), tamanho=1.5):
        dados = bpy.data.lights.new(nome, tipo)
        dados.energy = energia
        dados.color = cor
        if tipo == 'AREA':
            dados.size = tamanho
        ob = bpy.data.objects.new(nome, dados)
        ob.location = pos
        cena.collection.objects.link(ob)
        direcao = Vector((0, 0, 1.0)) - Vector(pos)
        ob.rotation_euler = direcao.to_track_quat('-Z', 'Y').to_euler()
        return ob

    luz('chave', 'AREA', 160, (1.8, -3.0, 2.6), tamanho=2.0)
    luz('preenche', 'AREA', 45, (-2.5, -2.0, 1.4), tamanho=2.0)
    luz('recorte', 'AREA', 260, (-0.6, 2.6, 2.4), cor=(1.0, 0.55, 0.2))
    luz('recorte2', 'AREA', 140, (1.2, 2.6, 1.0), cor=(1.0, 0.55, 0.2))
    luz('frente_baixa', 'AREA', 40, (0.0, -3.0, 0.3), cor=(0.75, 0.82, 1.0))

    cam_d = bpy.data.cameras.new('cam')
    cam_d.lens = 70
    cam = bpy.data.objects.new('cam', cam_d)
    cena.collection.objects.link(cam)
    cena.camera = cam
    return cam


def mirar(cam, angulo_graus, distancia=4.1, altura=1.25, alvo_z=0.88):
    a = math.radians(angulo_graus)
    cam.location = (math.sin(a) * distancia, -math.cos(a) * distancia, altura)
    cam.rotation_euler = (Vector((0, 0, alvo_z)) - cam.location).to_track_quat('-Z', 'Y').to_euler()


def render(caminho):
    bpy.context.scene.render.filepath = caminho
    bpy.ops.render.render(write_still=True)
    print(f'[corpo] preview {caminho}')


def previews(args):
    os.makedirs(args.docs, exist_ok=True)
    cam = preparar_render()
    mirar(cam, 0)
    render(os.path.join(args.docs, 'preview-corpo-frente.png'))
    mirar(cam, 180)
    render(os.path.join(args.docs, 'preview-corpo-costas.png'))

    # Regiões coloridas (conferência do mapeamento): frente e costas lado a lado
    mats = {r: material(f'regiao_{r}', c) for r, c in CORES_REGIAO.items()}
    originais = {}
    for ob in bpy.context.scene.objects:
        if ob.name.startswith('REG_'):
            originais[ob.name] = ob.data.materials[0]
            ob.data.materials[0] = mats[ob.name.split('_')[1]]
    cena = bpy.context.scene
    cena.render.resolution_x = 1600
    cam.data.lens = 50
    mirar(cam, 0, distancia=3.6)
    copia = []
    for ob in list(cena.objects):
        if ob.type == 'MESH':
            dup = ob.copy()
            x, y, z = ob.location
            dup.rotation_euler = (0, 0, math.pi)
            dup.location = (0.75 - x, -y, z)
            cena.collection.objects.link(dup)
            ob.location = (x - 0.75, y, z)
            copia.append(dup)
    render(os.path.join(args.docs, 'preview-corpo-regioes.png'))


def main():
    args = ler_args()
    montar(args)
    exportar(args)
    if not args.sem_preview:
        previews(args)


if __name__ == '__main__':
    main()
