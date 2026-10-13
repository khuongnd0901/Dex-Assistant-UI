#!/usr/bin/env python3
"""Check repository Markdown links and the current workspace authority offline."""
import json
from pathlib import Path
import re
import sys
from urllib.parse import unquote, urlsplit

ROOT = Path(__file__).resolve().parents[2]
LINK = re.compile(r'(?<!!)\[[^\]]*\]\((<[^>]+>|[^\s)]+)(?:\s+"[^"]*")?\)')
CURRENT = ('AGENTS.md', '.agent/STATUS.md', '.agent/README.md', '.agent/CONTEXT.md',
           '.agent/roles/main-agent.md', '.agent/roles/android-agent.md',
           'scripts/android/README.md')


def validate(root=ROOT):
    errors = []
    count = 0
    files = sorted(set(root.glob('*.md')) | set((root / '.agent').rglob('*.md')) |
                   set((root / 'docs').rglob('*.md')) | set((root / 'scripts').rglob('*.md')))
    for file in files:
        text = file.read_text(encoding='utf-8')
        for match in LINK.finditer(text):
            target = unquote(match.group(1).strip('<>'))
            if urlsplit(target).scheme or target.startswith('#'):
                continue
            local = target.split('#', 1)[0]
            if not local:
                continue
            dest = (file.parent / local).resolve()
            count += 1
            if not dest.is_relative_to(root):
                errors.append(f'{file.relative_to(root)}: required local link escapes repository: {target}')
            elif not dest.exists():
                errors.append(f'{file.relative_to(root)}: missing local link: {target}')
    for name in CURRENT:
        file = root / name
        if not file.exists():
            errors.append(f'missing current entry point: {name}')
            continue
        text = file.read_text(encoding='utf-8')
        if re.search(r'(?:/mnt/d|/home/[^/]+)/DeX-Assistant(?:/|$)|\.\./DeX-Assistant/|canonical backend|backend repository remains', text, re.I):
            errors.append(f'{name}: backend authority or local checkout dependency remains')
    try:
        folders = json.loads((root / 'Dex-Assistant.code-workspace').read_text())['folders']
        if len(folders) != 1 or (root / folders[0]['path']).resolve() != root:
            errors.append('workspace must open this Android root only')
    except (OSError, ValueError, KeyError) as exc:
        errors.append(f'invalid workspace: {exc}')
    if 'D-037' not in (root / '.agent/DECISIONS.md').read_text():
        errors.append('D-037 authority decision missing')
    return files, count, errors


if __name__ == '__main__':
    files, count, errors = validate()
    if errors:
        print('\n'.join(errors), file=sys.stderr)
        sys.exit(1)
    print(f'PASS: {len(files)} Markdown files, {count} local links, Android authority/workspace')
