# Créditos dos modelos 3D

## corpo-musculos.glb

BodyParts3D, © The Database Center for Life Science licensed under CC Attribution 4.0 International.

- Fonte: BodyParts3D 4.0, pacote "isa" em OBJ 99% (https://dbarchive.biosciencedbc.jp/en/bodyparts3d/).
- Licença: há duas informações no pacote, e vale a da versão 4.0.
  - O `README_e.html` da versão 4.0 diz: Creative Commons Attribution 4.0 International (CC BY 4.0,
    https://creativecommons.org/licenses/by/4.0/), com a atribuição acima.
  - O cabeçalho de cada arquivo OBJ ainda traz o texto das versões antigas: Creative Commons
    Attribution-Share Alike 2.1 Japan (CC BY-SA 2.1 JP).
  - Usamos a licença do README, que é o documento da versão 4.0 que baixamos; o cabeçalho dos OBJ
    não foi atualizado pelo projeto quando a licença mudou.
- Alterações feitas no GymSync: seleção dos músculos superficiais medidos e de partes neutras (ossos do tronco
  e dos membros, músculos sem medida); pele do próprio dataset recortada no pescoço, no punho e no tornozelo
  (cabeça, mãos e pés) e nas costas (o dataset não tem latíssimo do dorso); união por músculo e lado; corte dos
  tendões abaixo do punho e do tornozelo; decimação (cerca de 76 mil triângulos); escala para 1,75 m;
  exportação em glTF binário.
- O dataset não tem reto do abdômen: a cintura usa o oblíquo externo (que no dataset cobre a frente do abdômen)
  e o eretor da espinha.
- Script que gera o arquivo: `scripts/blender/build_corpo_musculos.py` (Blender 5.3).
