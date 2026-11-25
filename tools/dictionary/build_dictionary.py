import csv, json, re, sqlite3, urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parent
RAW = ROOT / 'raw' / 'ecdict.csv'
OUT = ROOT / 'output' / 'dictionary.db'
REPORT = ROOT / 'reports' / 'build_report.json'
URL = 'https://raw.githubusercontent.com/skywind3000/ECDICT/master/ecdict.csv'
TARGET = 10000
POS_RE = re.compile(r'(?<![A-Za-z])(?:vt|vi|v|n|adj|a|adv|pron|prep|conj|art|int|num|abbr|aux|det|interj)\.', re.I)

def download():
    if RAW.exists() and RAW.stat().st_size:
        return 'existing local file'
    RAW.parent.mkdir(parents=True, exist_ok=True)
    req = urllib.request.Request(URL, headers={'User-Agent':'LexiNote-dictionary-builder'})
    with urllib.request.urlopen(req, timeout=30) as response, RAW.open('wb') as out:
        while True:
            chunk = response.read(1024 * 1024)
            if not chunk: break
            out.write(chunk)
    return URL

def parse_senses(text):
    text = (text or '').strip()
    if not text: return []
    # ECDICT CSV snapshots may store line breaks as the two literal characters \\n+    # instead of physical newlines; normalize those separators before scanning.
    text = text.replace('\\r\\n', '\n').replace('\\n', '\n').replace('\\r', '\n')
    matches = list(POS_RE.finditer(text))
    if not matches: return [('unknown', text)]
    senses=[]
    prefix=text[:matches[0].start()].strip(' ;；\n')
    if prefix: senses.append(('unknown', prefix))
    for i,m in enumerate(matches):
        end = matches[i+1].start() if i+1<len(matches) else len(text)
        pos=m.group(0)[:-1].lower()
        if pos == 'a': pos = 'adj'
        meaning=text[m.end():end].strip(' ;；\n')
        if meaning: senses.append((pos, meaning))
    return senses or [('unknown', text)]

def main():
    source=download()
    counts={'raw_rows':0,'valid_rows':0,'inserted_entries':0,'duplicate_words':0,'missing_translation':0,'invalid_rows':0,'missing_phonetic':0,'unknown_senses':0}
    candidates={}
    with RAW.open('r',encoding='utf-8-sig',newline='') as f:
        reader=csv.DictReader(f)
        fields=reader.fieldnames or []
        required={'word','translation'}
        if not required.issubset(fields): raise ValueError(f'CSV columns missing {required-set(fields)}; actual={fields}')
        for row in reader:
            counts['raw_rows']+=1
            word=(row.get('word') or '').strip()
            key=word.casefold()
            trans=(row.get('translation') or '').strip()
            if not key or not re.fullmatch(r"[A-Za-z][A-Za-z'’-]*(?: [A-Za-z][A-Za-z'’-]*)?",word):
                counts['invalid_rows']+=1; continue
            if not trans:
                counts['missing_translation']+=1; continue
            counts['valid_rows']+=1
            rec={'word':word,'phonetic':re.sub(r'\s+',' ',(row.get('phonetic') or '').strip()) or None,'translation':trans,'exchange':(row.get('exchange') or '').strip() or None,'tag':(row.get('tag') or '').strip() or None,'frq':row.get('frq') or None,'bnc':row.get('bnc') or None,'senses':parse_senses(trans)}
            if key in candidates:
                counts['duplicate_words']+=1
                old=candidates[key]
                # Merge useful fields conservatively; preserve first word/translation order.
                for k in ('phonetic','exchange','tag','frq','bnc'):
                    if not old[k] and rec[k]: old[k]=rec[k]
                if len(rec['translation'])>len(old['translation']): old['translation']=rec['translation']; old['senses']=rec['senses']
            else: candidates[key]=rec
    def score(r):
        tag=set((r['tag'] or '').lower().split())
        exam=sum(t in tag for t in ('cet4','cet6','gk','zk','toefl','ielts','oxford3000'))
        freq=min([int(x) for x in (r['frq'],r['bnc']) if x and str(x).isdigit()] or [10**9])
        return (-exam, freq, r['word'].casefold())
    chosen=sorted(candidates.values(),key=score)[:TARGET]
    OUT.parent.mkdir(parents=True,exist_ok=True)
    if OUT.exists(): OUT.unlink()
    db=sqlite3.connect(OUT)
    db.execute('PRAGMA foreign_keys=ON')
    db.executescript((ROOT/'schema.sql').read_text(encoding='utf-8'))
    multi=0; senses_total=0
    with db:
        for r in chosen:
            cur=db.execute('INSERT INTO dictionary_entries(word,phonetic,translation_raw,exchange,tag,frq,bnc) VALUES(?,?,?,?,?,?,?)',(r['word'],r['phonetic'],r['translation'],r['exchange'],r['tag'],int(r['frq']) if str(r['frq'] or '').isdigit() else None,int(r['bnc']) if str(r['bnc'] or '').isdigit() else None))
            poses=set()
            for order,(pos,meaning) in enumerate(r['senses']):
                db.execute('INSERT INTO dictionary_senses(entry_id,pos,meaning,sort_order) VALUES(?,?,?,?)',(cur.lastrowid,pos,meaning,order))
                poses.add(pos); senses_total+=1
                if pos=='unknown': counts['unknown_senses']+=1
            if len(poses-{ 'unknown' })>1: multi+=1
            if not r['phonetic']: counts['missing_phonetic']+=1
    counts['inserted_entries']=len(chosen); counts['sense_rows']=senses_total; counts['multi_pos_entries']=multi
    counts['unique_valid_words']=len(candidates); counts['filtered_out_by_target']=max(0,len(candidates)-len(chosen))
    counts['database_bytes']=OUT.stat().st_size
    counts['source']=source; counts['csv_columns']=fields
    counts['sqlite_integrity']=db.execute('PRAGMA integrity_check').fetchone()[0]
    counts['foreign_key_violations']=len(db.execute('PRAGMA foreign_key_check').fetchall())
    counts['duplicate_keys']=db.execute('SELECT COUNT(*) FROM (SELECT lower(word) FROM dictionary_entries GROUP BY lower(word) HAVING COUNT(*)>1)').fetchone()[0]
    counts['orphan_senses']=db.execute('SELECT COUNT(*) FROM dictionary_senses s LEFT JOIN dictionary_entries e ON e.id=s.entry_id WHERE e.id IS NULL').fetchone()[0]
    counts['query_plan']=db.execute("EXPLAIN QUERY PLAN SELECT * FROM dictionary_entries WHERE word = 'book' COLLATE NOCASE").fetchall()
    sample={}
    for w in ['book','run','work','play','study','light','right','important','computer','reading']:
        e=db.execute('SELECT id,word,phonetic,translation_raw,exchange FROM dictionary_entries WHERE word=? COLLATE NOCASE',(w,)).fetchone()
        sample[w]=None if not e else {'word':e[1],'phonetic':e[2],'exchange':e[4],'senses':[{'pos':p,'meaning':m} for p,m in db.execute('SELECT pos,meaning FROM dictionary_senses WHERE entry_id=? ORDER BY sort_order',(e[0],))]}
    counts['sample_queries']=sample
    db.close()
    REPORT.write_text(json.dumps(counts,ensure_ascii=False,indent=2),encoding='utf-8')
    print(json.dumps(counts,ensure_ascii=False,indent=2))
if __name__=='__main__': main()
