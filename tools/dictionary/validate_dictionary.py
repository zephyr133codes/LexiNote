import json, sqlite3, sys
from pathlib import Path
p=Path(sys.argv[1] if len(sys.argv)>1 else Path(__file__).resolve().parent/'output'/'dictionary.db')
if not p.exists(): raise SystemExit(f'Database not found: {p}. Run build_dictionary.py after placing official CSV in raw/ecdict.csv.')
db=sqlite3.connect(p); db.execute('PRAGMA foreign_keys=ON')
checks={'integrity':db.execute('PRAGMA integrity_check').fetchone()[0], 'foreign_key_violations':db.execute('PRAGMA foreign_key_check').fetchall(), 'entries':db.execute('SELECT COUNT(*) FROM dictionary_entries').fetchone()[0], 'senses':db.execute('SELECT COUNT(*) FROM dictionary_senses').fetchone()[0], 'duplicate_casefold_keys':db.execute('SELECT COUNT(*) FROM (SELECT lower(word) FROM dictionary_entries GROUP BY lower(word) HAVING COUNT(*)>1)').fetchone()[0], 'orphan_senses':db.execute('SELECT COUNT(*) FROM dictionary_senses s LEFT JOIN dictionary_entries e ON e.id=s.entry_id WHERE e.id IS NULL').fetchone()[0]}
print(json.dumps(checks,ensure_ascii=False,indent=2))
if checks['integrity']!='ok' or checks['foreign_key_violations'] or checks['duplicate_casefold_keys'] or checks['orphan_senses']: raise SystemExit(1)
