#!/usr/bin/env python3
"""Mescla bancos de cheats (contents_complete.zip) na base offline do SwitchForge.

Uso: merge_cheat_dbs.py BASE.zip SAIDA.zip FONTE1.zip [FONTE2.zip ...]

Regras (aditivas, nunca removem nada):
- chave = (TitleID, BuildID); um cheat só é adicionado se a assinatura do código
  (linhas sem espaços, maiúsculas) ainda não existir naquele arquivo;
- blocos de pasta de menu (20000000/20000001) das fontes são descartados;
- arquivos novos são gravados em contents/<TID>/cheats/<BID>.txt;
- entradas que não são cheats (mods, nomes) passam intactas.
"""
import re, sys, zipfile

PAT = re.compile(r'(?:^|/)\[?([0-9A-Fa-f]{16})\]?/(?:cheats/)?([0-9A-Fa-f]{16})\.txt$')
HDR = re.compile(r'^\s*[\[{].*[\]}]\s*$')


def parse(text):
    """-> lista de (cabeçalho, [linhas de código])"""
    out, cur = [], None
    for l in text.replace('\r\n', '\n').split('\n'):
        if HDR.match(l):
            cur = (l.strip(), [])
            out.append(cur)
        elif cur is not None and l.strip():
            cur[1].append(l.strip())
    return out


def sig(b):
    code = ';'.join(re.sub(r'\s+', '', x).upper() for x in b[1] if x.strip())
    return code or 'EMPTY_' + b[0].lower()


def is_menu(b):
    """Bloco de 'pasta' de menu do Cheat-Engine (só 20000000/20000001): não é um cheat de verdade."""
    return bool(b[1]) and all(re.match(r'^2000000[01]\b', x) for x in b[1])


def build(blocks):
    return '\n\n'.join(h + '\n' + '\n'.join(c) for h, c in blocks) + '\n'


def main(base, outp, sources):
    entries = {}   # (tid,bid) -> nome da entrada na base
    with zipfile.ZipFile(base) as z:
        for n in z.namelist():
            m = PAT.search(n)
            if m and not n.endswith('/'):
                entries[(m.group(1).upper(), m.group(2).upper())] = n
    zb = zipfile.ZipFile(base)
    blocks = {k: parse(zb.read(n).decode('utf8', 'ignore')) for k, n in entries.items()}
    sigs = {k: {sig(b) for b in v} for k, v in blocks.items()}
    changed, added, newfiles = set(), 0, 0
    for s in sources:
        with zipfile.ZipFile(s) as z:
            for n in sorted(z.namelist()):
                m = PAT.search(n)
                if not m or n.endswith('/'):
                    continue
                k = (m.group(1).upper(), m.group(2).upper())
                if k not in blocks:
                    blocks[k], sigs[k] = [], set()
                    entries[k] = 'contents/%s/cheats/%s.txt' % k
                    newfiles += 1
                for b in parse(z.read(n).decode('utf8', 'ignore')):
                    if is_menu(b):
                        continue
                    g = sig(b)
                    if g not in sigs[k]:
                        sigs[k].add(g)
                        blocks[k].append(b)
                        changed.add(k)
                        added += 1
    done = set()
    with zipfile.ZipFile(outp, 'w', zipfile.ZIP_DEFLATED, compresslevel=9) as zo:
        for info in zb.infolist():
            m = PAT.search(info.filename)
            if m and not info.filename.endswith("/"):
                k = (m.group(1).upper(), m.group(2).upper())
                done.add(k)
                if k in changed:
                    zo.writestr(info.filename, build(blocks[k]).encode('utf8'))
                    continue
            zo.writestr(info, zb.read(info.filename))
        for k in sorted(changed - done):
            zo.writestr(entries[k], build(blocks[k]).encode('utf8'))
    print('arquivos novos: %d | cheats novos: %d | total: %d cheats em %d arquivos' % (
        newfiles, added, sum(len(v) for v in blocks.values()), len(blocks)))


if __name__ == '__main__':
    if len(sys.argv) < 4:
        sys.exit(__doc__)
    main(sys.argv[1], sys.argv[2], sys.argv[3:])
