"""Build a reproducible, local-only LexiNote dictionary database.

No source files are downloaded or modified. See README.md and
reports/source_analysis.md for source formats and redistribution notes.
"""
from __future__ import annotations

import csv
import hashlib
import json
import re
import sqlite3
from collections import Counter, defaultdict
from pathlib import Path

ROOT = Path(__file__).resolve().parent
RAW = ROOT / "raw"
OUTPUT = ROOT / "output" / "dictionary-v0.3.5.5.db"
REPORT = ROOT / "reports" / "build_report.json"
SCHEMA = ROOT / "schema.sql"
EXAM_TAGS = {"zk", "gk", "cet4", "cet6", "ky", "ielts", "toefl", "gre", "oxford3000", "oxford5000"}
WORD_RE = re.compile(r"[A-Za-z][A-Za-z'’-]{0,39}(?: [A-Za-z][A-Za-z'’-]{0,39})?")
POS_RE = re.compile(r"(?<![A-Za-z])(?:vt|vi|v|n|adj|a|adv|pron|prep|conj|art|int|num|abbr|aux|det|interj|phr|modal)\.", re.I)
EXPLICIT_PLURAL_NOTE = re.compile(r"[\(（\[]?\s*([A-Za-z][A-Za-z'’-]*)\s*的复数")
EXCHANGE_TYPE = {"s": "plural", "i": "present_participle", "p": "past", "d": "past_participle", "3": "third_person", "r": "comparative", "t": "superlative"}
UNIMORPH_TAG = {
    "N;PL": ("plural", "n"),
    "V;PST": ("past", "v"),
    "V;V.PTCP;PST": ("past_participle", "v"),
    "V;V.PTCP;PRS": ("present_participle", "v"),
    "V;PRS;3;SG": ("third_person", "v"),
    "ADJ;CMPR": ("comparative", "adj"),
    "ADJ;SPRL": ("superlative", "adj"),
}


def normalize_literal_newlines(value: str) -> str:
    return value.replace("\\r\\n", "\n").replace("\\n", "\n").replace("\\r", "\n")


def parse_senses(text: str) -> list[tuple[str, str]]:
    text = normalize_literal_newlines((text or "").strip())
    if not text:
        return []
    matches = list(POS_RE.finditer(text))
    if not matches:
        return [("unknown", text)]
    senses: list[tuple[str, str]] = []
    prefix = text[:matches[0].start()].strip(" ;；\n")
    if prefix:
        senses.append(("unknown", prefix))
    for index, match in enumerate(matches):
        end = matches[index + 1].start() if index + 1 < len(matches) else len(text)
        pos = match.group(0)[:-1].lower()
        if pos == "a":
            pos = "adj"
        meaning = text[match.end():end].strip(" ;；\n")
        if meaning:
            senses.append((pos, meaning))
    return senses or [("unknown", text)]


def parse_num(value: str | None) -> int | None:
    try:
        number = int(value or "")
        return number if number > 0 else None
    except (TypeError, ValueError):
        return None


def clean_row(row: dict[str, str | None]) -> dict | None:
    word = (row.get("word") or "").strip()
    translation = (row.get("translation") or "").strip()
    if not word or not WORD_RE.fullmatch(word) or not translation:
        return None
    return {
        "word": word,
        "phonetic": re.sub(r"\s+", " ", (row.get("phonetic") or "").strip()) or None,
        "translation": translation,
        "exchange": (row.get("exchange") or "").strip() or None,
        "tag": (row.get("tag") or "").strip() or None,
        "frq": parse_num(row.get("frq")),
        "bnc": parse_num(row.get("bnc")),
        "oxford": (row.get("oxford") or "").strip(),
        "senses": parse_senses(translation),
    }


def exchange_items(raw: str | None):
    for item in (raw or "").split("/"):
        if ":" not in item:
            if item.strip():
                yield None, None
            continue
        code, form = item.split(":", 1)
        code, form = code.strip().lower(), form.strip()
        yield (code or None), (form or None)


def is_core(row: dict) -> bool:
    tags = {part.lower() for part in (row["tag"] or "").split()}
    exam = bool(tags & EXAM_TAGS)
    oxford = row["oxford"] in {"1", "true", "True"}
    rank = min([n for n in (row["frq"], row["bnc"]) if n is not None] or [10**9])
    return exam or oxford or rank <= 200_000


def load_old_dictionary() -> tuple[dict, dict]:
    old_path = ROOT / "output" / "dictionary.db"
    entries: dict[str, dict] = {}
    if not old_path.exists():
        return entries, {"path": str(old_path), "present": False}
    db = sqlite3.connect(f"file:{old_path.as_posix()}?mode=ro", uri=True)
    try:
        for row in db.execute("SELECT id,word,phonetic,translation_raw,exchange,tag,frq,bnc FROM dictionary_entries"):
            entry_id, word, phonetic, raw, exchange, tag, frq, bnc = row
            senses = db.execute("SELECT pos,meaning,sort_order FROM dictionary_senses WHERE entry_id=? ORDER BY sort_order", (entry_id,)).fetchall()
            entries[word.casefold()] = {"word": word, "phonetic": phonetic, "translation": raw, "exchange": exchange, "tag": tag, "frq": frq, "bnc": bnc, "senses": [(p, m) for p, m, _ in senses], "old": True}
    finally:
        db.close()
    return entries, {"path": str(old_path), "present": True, "entries": len(entries)}


def main() -> None:
    source = RAW / "ecdict.csv"
    unimorph_path = RAW / "eng.txt"
    for path in (source, unimorph_path):
        if not path.is_file():
            raise SystemExit(f"Required local source is missing: {path}; this script never downloads sources.")

    old_entries, old_info = load_old_dictionary()
    core: dict[str, dict] = {}
    csv_records = rejected = missing_translation = 0
    with source.open("r", encoding="utf-8-sig", newline="") as stream:
        reader = csv.DictReader(stream)
        required = {"word", "translation", "exchange", "tag", "frq", "bnc", "oxford"}
        if not required.issubset(reader.fieldnames or []):
            raise ValueError(f"ECDICT columns missing: {sorted(required-set(reader.fieldnames or []))}")
        for row in reader:
            csv_records += 1
            record = clean_row(row)
            if record is None:
                rejected += 1
                if not (row.get("translation") or "").strip():
                    missing_translation += 1
                continue
            key = record["word"].casefold()
            if is_core(record) or key in old_entries:
                core[key] = record
    core_keys = set(core)

    # Add narrowly evidenced out-of-core forms only when ECDICT's Chinese note
    # explicitly names the lemma and UniMorph independently confirms N;PL.
    explicit_pairs: set[tuple[str, str]] = set()
    with source.open("r", encoding="utf-8-sig", newline="") as stream:
        for row in csv.DictReader(stream):
            form = (row.get("word") or "").strip()
            translation = row.get("translation") or ""
            if not form or not WORD_RE.fullmatch(form):
                continue
            for match in EXPLICIT_PLURAL_NOTE.finditer(translation):
                lemma = match.group(1).strip()
                if lemma.casefold() != form.casefold() and WORD_RE.fullmatch(lemma):
                    explicit_pairs.add((lemma.casefold(), form.casefold()))
    confirmed_pairs: set[tuple[str, str]] = set()
    with unimorph_path.open("r", encoding="utf-8-sig", newline="") as stream:
        for line in stream:
            fields = line.rstrip("\r\n").split("\t")
            if len(fields) == 3 and fields[2].strip() == "N;PL":
                pair = (fields[0].strip().casefold(), fields[1].strip().casefold())
                if pair in explicit_pairs:
                    confirmed_pairs.add(pair)
    supplementary_pairs = sorted(confirmed_pairs)
    supplementary_words = {word for pair in supplementary_pairs for word in pair} - core_keys

    # Keep translated inflected forms explicitly attached to selected ECDICT lemmas.
    rows: dict[str, dict] = dict(core)
    if supplementary_words:
        with source.open("r", encoding="utf-8-sig", newline="") as stream:
            for row in csv.DictReader(stream):
                record = clean_row(row)
                if record and record["word"].casefold() in supplementary_words:
                    rows[record["word"].casefold()] = record
    related_forms: set[str] = set()
    with source.open("r", encoding="utf-8-sig", newline="") as stream:
        for row in csv.DictReader(stream):
            word = (row.get("word") or "").strip()
            if not word:
                continue
            for code, form in exchange_items(row.get("exchange")):
                if code == "0" and form and form.casefold() in core_keys:
                    related_forms.add(word.casefold())
                elif word.casefold() in core_keys and code not in {"0", "1", None} and form:
                    related_forms.add(form.casefold())
    needed = related_forms - rows.keys()
    if needed:
        with source.open("r", encoding="utf-8-sig", newline="") as stream:
            for row in csv.DictReader(stream):
                record = clean_row(row)
                if record and record["word"].casefold() in needed:
                    rows[record["word"].casefold()] = record

    # Old database-only entries are retained (including legacy multiword entries).
    for key, old in old_entries.items():
        if key not in rows:
            rows[key] = old
    for key, old in old_entries.items():
        current = rows[key]
        current["old"] = True
        current["senses"] = list(old["senses"]) + [sense for sense in current.get("senses", []) if sense not in old["senses"]]
        # Preserve original phonetic and raw ECDICT translation for existing entries.
        current["phonetic"] = old["phonetic"] or current.get("phonetic")
        current["translation"] = old["translation"] or current.get("translation") or ""
        current["exchange"] = old["exchange"] or current.get("exchange")
        current["tag"] = old["tag"] or current.get("tag")
        current["frq"] = old["frq"] or current.get("frq")
        current["bnc"] = old["bnc"] or current.get("bnc")

    key_to_word = {key: record["word"] for key, record in rows.items()}
    ecdict_senses_pos: dict[str, set[str]] = {}
    for key, record in rows.items():
        ecdict_senses_pos[key] = {pos for pos, _ in record.get("senses", []) if pos not in {"unknown", "abbr", "interj", "phr"}}
    ecdict_abbreviation_only = {
        key for key, record in rows.items()
        if record.get("senses") and all(pos == "abbr" for pos, _ in record["senses"])
    }

    relations: dict[tuple[str, str, str, str, str], dict] = {}
    relation_stats = Counter()
    conflicts: list[dict] = []
    conflict_count = 0
    unknown_exchange = Counter()

    def add_relation(lemma: str, form: str, pos: str, kind: str, source_name: str, priority: int, quality: str) -> None:
        lemma_key, form_key = lemma.casefold(), form.casefold()
        if (lemma_key == form_key and kind not in {"past", "past_participle"}) or lemma_key not in rows or form_key not in rows:
            relation_stats["missing_endpoint_or_identity"] += 1
            return
        key = (lemma_key, form_key, pos, kind, source_name)
        relations.setdefault(key, {"lemma": key_to_word[lemma_key], "form": key_to_word[form_key], "pos": pos, "form_type": kind, "source": source_name, "source_priority": priority, "quality": quality})

    # ECDICT explicit forward forms and reverse lemma/type declarations.
    for key, record in rows.items():
        items = list(exchange_items(record.get("exchange")))
        reverse_lemma = next((value for code, value in items if code == "0" and value), None)
        reverse_types = next((value for code, value in items if code == "1" and value), None)
        if reverse_lemma and key_to_word.get(reverse_lemma.casefold()):
            codes = list(reverse_types or "")
            for code in codes:
                kind = EXCHANGE_TYPE.get(code)
                if kind:
                    pos = "n" if kind == "plural" else "adj" if kind in {"comparative", "superlative"} else "v"
                    add_relation(reverse_lemma, record["word"], pos, kind, "ecdict", 1, "explicit_exchange_reverse")
                elif code not in {"0", "1"}:
                    unknown_exchange[code] += 1
        for code, value in items:
            if code in {"0", "1", None} or not value:
                continue
            kind = EXCHANGE_TYPE.get(code)
            if kind:
                pos = "n" if kind == "plural" else "adj" if kind in {"comparative", "superlative"} else "v"
                add_relation(record["word"], value, pos, kind, "ecdict", 1, "explicit_exchange_forward")
            else:
                unknown_exchange[code] += 1

    for lemma_key, form_key in supplementary_pairs:
        lemma = key_to_word.get(lemma_key)
        form = key_to_word.get(form_key)
        if lemma and form:
            add_relation(lemma, form, "n", "plural", "ecdict-translation", 1, "explicit_plural_note_confirmed_by_unimorph")

    # UniMorph: retain only exact documented tagsets; use ECDICT POS as a cross-check.
    um_rows = um_malformed = um_unknown = um_rejected_pos = 0
    um_seen: set[tuple[str, str, str]] = set()
    explicit_ecdict_form_lemmas: dict[tuple[str, str, str], set[str]] = defaultdict(set)
    for rel in relations.values():
        if rel["source"] == "ecdict":
            explicit_ecdict_form_lemmas[(rel["form"].casefold(), rel["pos"], rel["form_type"])].add(rel["lemma"].casefold())
    with unimorph_path.open("r", encoding="utf-8-sig", newline="") as stream:
        for line_no, line in enumerate(stream, 1):
            line = line.rstrip("\r\n")
            if not line:
                continue
            parts = line.split("\t")
            if len(parts) != 3:
                um_malformed += 1
                continue
            lemma, form, tagset = (part.strip() for part in parts)
            um_rows += 1
            mapped = UNIMORPH_TAG.get(tagset)
            if not mapped:
                um_unknown += 1
                continue
            kind, expected_pos = mapped
            known = ecdict_senses_pos.get(lemma.casefold(), set())
            lemma_key = lemma.casefold()
            competing_explicit_form = any(
                explicit_lemma != lemma_key
                for explicit_lemma in explicit_ecdict_form_lemmas.get((form.casefold(), expected_pos, kind), ())
            )
            # ECDICT's "hav" headword is abbreviation-only, while its had/has/having
            # records explicitly point to "have". Reject only this evidenced conflict.
            if expected_pos == "v" and lemma_key == "hav" and lemma_key in ecdict_abbreviation_only and competing_explicit_form:
                um_rejected_pos += 1
                conflict_count += 1
                if len(conflicts) < 1000:
                    conflicts.append({"line": line_no, "lemma": lemma, "form": form, "tagset": tagset, "reason": "abbreviation_lemma_conflicts_with_explicit_ecdict_form_relation"})
                continue
            if form.casefold() in {"countable", "uncountable"}:
                um_rejected_pos += 1
                conflict_count += 1
                if len(conflicts) < 1000:
                    conflicts.append({"line": line_no, "lemma": lemma, "form": form, "tagset": tagset, "reason": "non_inflected_descriptor_used_as_form"})
                continue
            if known and expected_pos not in known:
                um_rejected_pos += 1
                conflict_count += 1
                if len(conflicts) < 1000:
                    conflicts.append({"line": line_no, "lemma": lemma, "form": form, "tagset": tagset, "reason": "pos_conflicts_with_ecdict", "ecdict_pos": sorted(known)})
                continue
            triple = (lemma.casefold(), form.casefold(), tagset)
            if triple in um_seen:
                continue
            um_seen.add(triple)
            add_relation(lemma, form, expected_pos, kind, "unimorph-eng", 3, "exact_tagset_pos_checked")

    # Resolve source conflicts conservatively: if ECDICT provides a type for a pair,
    # do not add a different UniMorph type for that same lemma/form pair.
    ecdict_pairs: dict[tuple[str, str, str], set[str]] = defaultdict(set)
    for rel in relations.values():
        if rel["source"] == "ecdict":
            ecdict_pairs[(rel["lemma"].casefold(), rel["form"].casefold(), rel["pos"])].add(rel["form_type"])
    before = len(relations)
    filtered = {}
    for key, rel in relations.items():
        if rel["source"] == "unimorph-eng":
            explicit_types = ecdict_pairs.get((rel["lemma"].casefold(), rel["form"].casefold(), rel["pos"]), set())
            if explicit_types and rel["form_type"] not in explicit_types:
                relation_stats["source_conflict_unimorph_rejected"] += 1
                conflict_count += 1
                if len(conflicts) < 1000:
                    conflicts.append({"lemma": rel["lemma"], "form": rel["form"], "type": rel["form_type"], "reason": "ecdict_type_preferred", "ecdict_types": sorted(explicit_types)})
                continue
        filtered[key] = rel
    filtered_count = len(filtered)
    # Identical relations from multiple sources are stored once, with provenance merged.
    merged_relations: dict[tuple[str, str, str, str], dict] = {}
    for rel in filtered.values():
        key = (rel["lemma"].casefold(), rel["form"].casefold(), rel["pos"], rel["form_type"])
        current = merged_relations.get(key)
        if current is None:
            current = dict(rel)
            current["_sources"] = {rel["source"]}
            current["_qualities"] = {rel["quality"]}
            merged_relations[key] = current
        else:
            current["_sources"].add(rel["source"])
            current["_qualities"].add(rel["quality"])
            current["source_priority"] = min(current["source_priority"], rel["source_priority"])
    relations = {}
    for rel in merged_relations.values():
        rel["source"] = "+".join(sorted(rel.pop("_sources")))
        rel["quality"] = "+".join(sorted(rel.pop("_qualities")))
        relations[(rel["lemma"].casefold(), rel["form"].casefold(), rel["pos"], rel["form_type"], rel["source"])] = rel
    invalid_have_hav = [
        rel for rel in relations.values()
        if (rel["lemma"].casefold() == "hav" and rel["form"].casefold() in {"had", "has", "having"})
        or (rel["lemma"].casefold() == "have" and rel["form"].casefold() == "hav")
    ]
    if invalid_have_hav:
        raise ValueError(f"Rejected erroneous hav/have-family relations: {invalid_have_hav[:5]}")
    relation_stats["relations_removed_by_conflict_filter"] = before - filtered_count
    relation_stats["identical_cross_source_relations_collapsed"] = filtered_count - len(relations)
    relation_stats["unimorph_relations_retained_with_provenance"] = sum("unimorph-eng" in rel["source"] for rel in relations.values())

    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    temp = OUTPUT.with_suffix(OUTPUT.suffix + ".tmp")
    if temp.exists():
        temp.unlink()
    db = sqlite3.connect(temp)
    try:
        db.execute("PRAGMA foreign_keys=ON")
        db.executescript(SCHEMA.read_text(encoding="utf-8"))
        with db:
            for key in sorted(rows):
                record = rows[key]
                cur = db.execute("INSERT INTO dictionary_entries(word,phonetic,translation_raw,exchange,tag,frq,bnc) VALUES(?,?,?,?,?,?,?)", (record["word"], record.get("phonetic"), record.get("translation") or "", record.get("exchange"), record.get("tag"), record.get("frq"), record.get("bnc")))
                for order, (pos, meaning) in enumerate(record.get("senses", [])):
                    if meaning.strip():
                        db.execute("INSERT INTO dictionary_senses(entry_id,pos,meaning,sort_order) VALUES(?,?,?,?)", (cur.lastrowid, pos or "unknown", meaning.strip(), order))
            for rel in sorted(relations.values(), key=lambda item: (item["lemma"].casefold(), item["form"].casefold(), item["pos"], item["form_type"], item["source"])):
                db.execute("INSERT OR IGNORE INTO dictionary_word_forms(lemma,form,pos,form_type,source,source_priority,quality) VALUES(?,?,?,?,?,?,?)", (rel["lemma"], rel["form"], rel["pos"], rel["form_type"], rel["source"], rel["source_priority"], rel["quality"]))
        db.execute("PRAGMA optimize")
        db.commit()
    finally:
        db.close()
    temp.replace(OUTPUT)

    # Compare all previously shipped rows/senses after writing the candidate.
    db = sqlite3.connect(OUTPUT)
    old_missing = []
    old_sense_missing = []
    old_sense_additions = 0
    old_phonetic_changes = []
    for key, old in old_entries.items():
        row = db.execute("SELECT id,phonetic FROM dictionary_entries WHERE word=? COLLATE NOCASE", (old["word"],)).fetchone()
        if row is None:
            old_missing.append(old["word"])
            continue
        if old["phonetic"] != row[1]:
            old_phonetic_changes.append(old["word"])
        present = set(db.execute("SELECT pos,meaning FROM dictionary_senses WHERE entry_id=?", (row[0],)).fetchall())
        for sense in old["senses"]:
            if sense not in present:
                old_sense_missing.append({"word": old["word"], "pos": sense[0], "meaning": sense[1]})
        old_set = set(old["senses"])
        old_sense_additions += sum(1 for sense in present if sense not in old_set)

    previous_path = ROOT / "output" / "dictionary-v0.3.5.4.db"
    if not previous_path.exists():
        previous_path = ROOT.parent.parent / "android" / "app" / "src" / "main" / "assets" / "dictionary.db"
    previous_diff = {"path": str(previous_path), "present": previous_path.is_file()}
    if previous_path.is_file():
        previous = sqlite3.connect(f"file:{previous_path.as_posix()}?mode=ro", uri=True)
        previous_entries_missing = []
        previous_senses_missing = []
        previous_phonetic_changes = []
        previous_translation_changes = []
        for prev_id, word, phonetic, translation in previous.execute("SELECT id,word,phonetic,translation_raw FROM dictionary_entries"):
            current = db.execute("SELECT id,phonetic,translation_raw FROM dictionary_entries WHERE word=? COLLATE NOCASE", (word,)).fetchone()
            if current is None:
                previous_entries_missing.append(word)
                continue
            if phonetic != current[1]:
                previous_phonetic_changes.append(word)
            if translation != current[2]:
                previous_translation_changes.append(word)
            current_senses = set(db.execute("SELECT pos,meaning FROM dictionary_senses WHERE entry_id=?", (current[0],)).fetchall())
            for sense in previous.execute("SELECT pos,meaning FROM dictionary_senses WHERE entry_id=?", (prev_id,)).fetchall():
                if sense not in current_senses:
                    previous_senses_missing.append({"word": word, "pos": sense[0], "meaning": sense[1]})
        previous_relations = set(previous.execute("SELECT DISTINCT lower(lemma),lower(form),pos,form_type FROM dictionary_word_forms").fetchall())
        current_relations = set(db.execute("SELECT DISTINCT lower(lemma),lower(form),pos,form_type FROM dictionary_word_forms").fetchall())
        previous_diff.update({
            "entries": previous.execute("SELECT COUNT(*) FROM dictionary_entries").fetchone()[0],
            "senses": previous.execute("SELECT COUNT(*) FROM dictionary_senses").fetchone()[0],
            "relations": len(previous_relations),
            "entries_missing": previous_entries_missing,
            "senses_missing_count": len(previous_senses_missing),
            "senses_missing_sample": previous_senses_missing[:100],
            "phonetic_changes": previous_phonetic_changes,
            "translation_changes": previous_translation_changes,
            "relations_missing_count": len(previous_relations - current_relations),
            "relations_missing_sample": sorted(previous_relations - current_relations)[:100],
        })
        previous.close()
    entry_count = db.execute("SELECT COUNT(*) FROM dictionary_entries").fetchone()[0]
    sense_count = db.execute("SELECT COUNT(*) FROM dictionary_senses").fetchone()[0]
    relation_count = db.execute("SELECT COUNT(*) FROM dictionary_word_forms").fetchone()[0]
    unknown_sense_count = db.execute("SELECT COUNT(*) FROM dictionary_senses WHERE pos='unknown'").fetchone()[0]
    by_source = dict(db.execute("SELECT source,COUNT(*) FROM dictionary_word_forms GROUP BY source ORDER BY source").fetchall())
    provenance_counts = Counter()
    for source_value, count in by_source.items():
        for source_name in source_value.split("+"):
            provenance_counts[source_name] += count
    by_type = dict(db.execute("SELECT form_type,COUNT(*) FROM dictionary_word_forms GROUP BY form_type ORDER BY form_type").fetchall())
    required_samples = {}
    for word in ("go", "went", "gone", "going", "goes", "study", "studied", "studying", "run", "ran", "running", "child", "children", "mouse", "mice", "see", "saw", "seen", "good", "better", "best", "book", "books", "read", "have", "has", "had", "having", "hav", "important", "introduction", "factory", "practice", "smartphone", "smartphones"):
        required_samples[word] = db.execute("SELECT lemma,form,pos,form_type,source FROM dictionary_word_forms WHERE lemma=? COLLATE NOCASE OR form=? COLLATE NOCASE ORDER BY source_priority,lemma,form,form_type", (word, word)).fetchall()
    required_samples["have_family_irregular_forms"] = all(
        db.execute("SELECT 1 FROM dictionary_word_forms WHERE lemma='have' COLLATE NOCASE AND form=? COLLATE NOCASE AND form_type=? AND pos='v'", (form, kind)).fetchone()
        for form, kind in (("has", "third_person"), ("had", "past"), ("had", "past_participle"), ("having", "present_participle"))
    )
    required_samples["have_family_has_no_spurious_hav"] = db.execute(
        "SELECT 1 FROM dictionary_word_forms WHERE (lemma='hav' COLLATE NOCASE AND form IN ('had','has','having')) OR (lemma='have' COLLATE NOCASE AND form='hav') LIMIT 1"
    ).fetchone() is None
    db.close()
    digest = hashlib.sha256(OUTPUT.read_bytes()).hexdigest()
    report = {
        "version": "0.3.5.5", "source": "local files only", "ecdict_csv_records": csv_records,
        "rejected_ecdict_rows": rejected, "missing_translation_rows_among_rejected": missing_translation,
        "core_selected_count": len(core), "expanded_entry_count": entry_count, "sense_count": sense_count,
        "explicit_plural_note_pairs_confirmed_by_unimorph": len(supplementary_pairs),
        "word_form_count": relation_count, "word_forms_by_source": dict(provenance_counts), "word_forms_by_type": by_type,
        "word_form_source_combinations": by_source,
        "unimorph_rows": um_rows, "unimorph_malformed_rows": um_malformed, "unimorph_unmapped_tagsets": um_unknown,
        "unimorph_pos_rejected": um_rejected_pos, "unknown_or_undocumented_ecdict_exchange_codes": dict(unknown_exchange),
        "relation_stats": dict(relation_stats), "conflict_report_count": conflict_count, "conflicts_sample": conflicts,
        "old_dictionary": old_info, "old_entries_missing": old_missing, "old_senses_missing_count": len(old_sense_missing),
        "previous_dictionary_regression": previous_diff,
        "additional_senses_on_old_entries": old_sense_additions,
        "old_senses_missing_sample": old_sense_missing[:100], "old_phonetic_change_count": len(old_phonetic_changes),
        "old_phonetic_changes_sample": old_phonetic_changes[:100], "database_bytes": OUTPUT.stat().st_size,
        "sha256": digest, "required_relation_samples": required_samples,
        "lemma_en_included": False, "lemma_en_exclusion_reason": "Header grants research/educational use only; redistribution rights for bundled APK are not clear.",
        "license_notes": {"ecdict": "MIT notice retained; upstream ECDICT README describes data assembled from multiple sources.", "unimorph-eng": "CC BY-SA 3.0; attribution, modification notice, and share-alike notice required.", "lemma.en.txt": "Excluded from distributable DB pending clear redistribution permission."}
    }
    REPORT.parent.mkdir(parents=True, exist_ok=True)
    REPORT.write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8")
    markdown = [
        "# V0.3.5.5 词典构建报告", "",
        f"- 数据库：`{OUTPUT.name}`；{entry_count:,} 词条、{sense_count:,} 条释义、{relation_count:,} 条词形关系。",
        f"- 文件大小：{OUTPUT.stat().st_size:,} bytes；SHA-256：`{digest}`。",
        f"- 词形来源记录：`{json.dumps(dict(provenance_counts), ensure_ascii=False)}`（双来源关系分别计入来源覆盖统计）。",
        f"- 词形类型：`{json.dumps(by_type, ensure_ascii=False)}`。",
        f"- ECDICT 明确写出‘原形的复数’且由 UniMorph N;PL 交叉确认的补充关系：{len(supplementary_pairs):,} 条。",
        f"- UniMorph 未映射标签 {um_unknown:,} 行，词性或描述冲突拒绝 {um_rejected_pos:,} 行；ECDICT 未知编码：`{json.dumps(dict(unknown_exchange), ensure_ascii=False)}`。",
        f"- 未识别词性的释义 {unknown_sense_count:,} 条，原文仍然保留。",
        f"- 旧版回归：缺失词条 {len(old_missing)}，缺失释义 {len(old_sense_missing)}，音标变化 {len(old_phonetic_changes)}；旧词条新增释义 {old_sense_additions:,} 条。",
        f"- 旧版数据库：{old_info}。lemma.en.txt 未并入，因为再分发授权尚不明确。", "",
        f"- V0.3.5.4 回归：缺失词条 {len(previous_diff.get('entries_missing', []))}，缺失释义 {previous_diff.get('senses_missing_count', '未比对')}，缺失关系 {previous_diff.get('relations_missing_count', '未比对')}。", "",
        "逐词样例、冲突样例及其他详细计数见同目录 `build_report.json`。"
    ]
    (REPORT.parent / "build_report.md").write_text("\n".join(markdown) + "\n", encoding="utf-8")
    print(json.dumps({key: value for key, value in report.items() if key not in {"conflicts_sample", "required_relation_samples", "old_senses_missing_sample", "old_phonetic_changes_sample"}}, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
