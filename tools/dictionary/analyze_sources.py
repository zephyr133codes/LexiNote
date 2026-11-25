"""Read-only, reproducible audit of the local dictionary source snapshots."""

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
OUTPUT = ROOT / "output" / "dictionary.db"
REPORT = ROOT / "reports" / "source_analysis.json"
MARKDOWN = ROOT / "reports" / "source_analysis.md"


def file_facts(name: str) -> tuple[Path, str, dict]:
    path = RAW / name
    data = path.read_bytes()
    text = data.decode("utf-8-sig")
    facts = {
        "bytes": len(data),
        "sha256": hashlib.sha256(data).hexdigest(),
        "encoding": "UTF-8 (optional BOM) validated",
        "physical_lines": len(text.splitlines()),
        "first_20_lines": [line[:350] for line in text.splitlines()[:20]],
    }
    return path, text, facts


def analyze() -> dict:
    result: dict = {}
    for name in ("ecdict.csv", "lemma.en.txt", "eng.txt"):
        _, _, facts = file_facts(name)
        result[name] = facts

    ecdict = result["ecdict.csv"]
    ecdict_path = RAW / "ecdict.csv"
    word_rows: dict[str, dict] = {}
    row_count = duplicate_rows = malformed_rows = missing_translation = 0
    literal_newline_rows = physical_newline_rows = 0
    duplicate_conflicts = Counter()
    tags = Counter()
    exchange_codes = Counter()
    exchange_parse_errors = 0
    exchange_examples: dict[str, list[dict]] = defaultdict(list)
    nonempty = Counter()
    with ecdict_path.open("r", encoding="utf-8-sig", newline="") as stream:
        reader = csv.DictReader(stream)
        columns = reader.fieldnames or []
        for row in reader:
            row_count += 1
            if None in row:
                malformed_rows += 1
            for key, value in row.items():
                if value:
                    nonempty[key] += 1
            word = (row.get("word") or "").strip()
            key = word.casefold()
            if not key:
                continue
            tags.update((row.get("tag") or "").split())
            trans = (row.get("translation") or "").strip()
            if not trans:
                missing_translation += 1
            if "\\n" in trans or "\\r" in trans:
                literal_newline_rows += 1
            if "\n" in trans or "\r" in trans:
                physical_newline_rows += 1
            exchange = (row.get("exchange") or "").strip()
            if exchange:
                for item in exchange.split("/"):
                    if ":" not in item:
                        exchange_parse_errors += 1
                        continue
                    code, form = item.split(":", 1)
                    code, form = code.strip(), form.strip()
                    if not code or not form:
                        exchange_parse_errors += 1
                        continue
                    exchange_codes[code] += 1
                    if len(exchange_examples[code]) < 6:
                        exchange_examples[code].append({"word": word, "form": form})
            old = word_rows.get(key)
            if old is None:
                word_rows[key] = {"word": word, "translation": trans, "phonetic": row.get("phonetic") or "", "exchange": exchange, "tag": row.get("tag") or "", "rows": 1}
            else:
                duplicate_rows += 1
                old["rows"] += 1
                for field, label in (("translation", "translation"), ("phonetic", "phonetic"), ("exchange", "exchange")):
                    value = (row.get(field) or "").strip()
                    if value and old[field] and value != old[field]:
                        duplicate_conflicts[label] += 1
                if trans and not old["translation"]:
                    old["translation"] = trans
    ecdict.update({
        "columns": columns,
        "logical_csv_records": row_count,
        "unique_casefold_words": len(word_rows),
        "duplicate_rows": duplicate_rows,
        "rows_with_overflow_columns": malformed_rows,
        "missing_translation_rows": missing_translation,
        "translation_rows_with_literal_backslash_newline": literal_newline_rows,
        "translation_rows_with_physical_newline": physical_newline_rows,
        "nonempty_fields": dict(nonempty),
        "tag_frequency": dict(tags.most_common()),
        "exchange_code_frequency": dict(exchange_codes.most_common()),
        "exchange_parse_errors": exchange_parse_errors,
        "exchange_examples": dict(exchange_examples),
        "duplicate_conflict_counts": dict(duplicate_conflicts),
        "required_samples": {word: word_rows.get(word.casefold()) for word in ("go", "went", "gone", "going", "goes", "study", "studied", "studying", "run", "ran", "running", "child", "children", "mouse", "mice", "see", "saw", "seen", "good", "better", "best", "book", "books", "read", "important", "introduction", "factory", "practice")},
    })

    lemma = result["lemma.en.txt"]
    lemma_groups = 0
    lemma_relations = 0
    lemma_malformed = 0
    lemma_form_to_lemmas: dict[str, set[str]] = defaultdict(set)
    lemma_examples = []
    for line in (RAW / "lemma.en.txt").read_text(encoding="utf-8-sig").splitlines():
        if not line or line.lstrip().startswith(";"):
            continue
        if "->" not in line:
            lemma_malformed += 1
            continue
        left, right = line.split("->", 1)
        left = left.strip()
        lemma_word = re.sub(r"/\d+\s*$", "", left).strip()
        if not lemma_word or not right.strip():
            lemma_malformed += 1
            continue
        lemma_groups += 1
        forms = [form.strip() for form in right.split(",") if form.strip()]
        lemma_relations += len(forms)
        for form in forms:
            lemma_form_to_lemmas[form.casefold()].add(lemma_word.casefold())
        if len(lemma_examples) < 12:
            lemma_examples.append({"lemma": lemma_word, "forms": forms})
    lemma.update({
        "parsed_groups": lemma_groups,
        "parsed_form_relations": lemma_relations,
        "malformed_data_lines": lemma_malformed,
        "forms_with_multiple_lemmas": sum(len(values) > 1 for values in lemma_form_to_lemmas.values()),
        "distinct_forms": len(lemma_form_to_lemmas),
        "required_samples": {word: sorted(lemma_form_to_lemmas.get(word, set())) for word in ("went", "gone", "going", "goes", "studied", "studying", "ran", "running", "children", "mice", "saw", "seen", "better", "best", "books", "read")},
        "examples": lemma_examples,
    })

    unimorph = result["eng.txt"]
    um_rows = duplicate_um = malformed_um = 0
    tagsets = Counter()
    tag_parts = Counter()
    relation_keys = set()
    conflicts: dict[tuple[str, str], set[str]] = defaultdict(set)
    form_lemmas: dict[str, set[str]] = defaultdict(set)
    known_pos = Counter()
    sample_relations = defaultdict(list)
    with (RAW / "eng.txt").open("r", encoding="utf-8-sig", newline="") as stream:
        for line in stream:
            line = line.rstrip("\r\n")
            if not line:
                continue
            fields = line.split("\t")
            if len(fields) != 3:
                malformed_um += 1
                continue
            lemma_word, form, tagset = (field.strip() for field in fields)
            um_rows += 1
            tags_in_set = tagset.split(";")
            tagsets[tagset] += 1
            tag_parts.update(tags_in_set)
            known_pos[tags_in_set[0] if tags_in_set else ""] += 1
            key = (lemma_word.casefold(), form.casefold(), tagset)
            if key in relation_keys:
                duplicate_um += 1
            relation_keys.add(key)
            conflicts[(lemma_word.casefold(), form.casefold())].add(tagset)
            form_lemmas[form.casefold()].add(lemma_word.casefold())
            if len(sample_relations[lemma_word.casefold()]) < 12:
                sample_relations[lemma_word.casefold()].append({"form": form, "tag": tagset})
    unimorph.update({
        "records": um_rows,
        "malformed_records": malformed_um,
        "duplicate_exact_relations": duplicate_um,
        "unique_relation_triples": len(relation_keys),
        "distinct_tagsets": len(tagsets),
        "tagset_frequency": dict(tagsets.most_common(80)),
        "tag_component_frequency": dict(tag_parts.most_common()),
        "upos_frequency": dict(known_pos.most_common()),
        "pairs_with_multiple_tagsets": sum(len(values) > 1 for values in conflicts.values()),
        "forms_with_multiple_lemmas": sum(len(values) > 1 for values in form_lemmas.values()),
        "required_samples": {word: sample_relations.get(word.casefold(), []) for word in ("go", "went", "gone", "going", "goes", "study", "studied", "studying", "run", "ran", "running", "child", "children", "mouse", "mice", "see", "saw", "seen", "good", "better", "best", "book", "books", "read")},
    })

    if OUTPUT.exists():
        db = sqlite3.connect(f"file:{OUTPUT.as_posix()}?mode=ro", uri=True)
        old = {"bytes": OUTPUT.stat().st_size, "entries": db.execute("SELECT COUNT(*) FROM dictionary_entries").fetchone()[0], "senses": db.execute("SELECT COUNT(*) FROM dictionary_senses").fetchone()[0]}
        old["sample_words"] = {}
        for word in ("Introduction", "important", "factory", "practice", "book", "go", "read"):
            row = db.execute("SELECT id, word, phonetic, translation_raw, exchange FROM dictionary_entries WHERE word=? COLLATE NOCASE", (word,)).fetchone()
            old["sample_words"][word] = None if row is None else {"word": row[1], "phonetic": row[2], "translation_raw": row[3], "exchange": row[4], "senses": db.execute("SELECT pos, meaning, sort_order FROM dictionary_senses WHERE entry_id=? ORDER BY sort_order", (row[0],)).fetchall()}
        db.close()
        result["existing_output_dictionary"] = old

    return result


def write_report(data: dict) -> None:
    REPORT.parent.mkdir(parents=True, exist_ok=True)
    REPORT.write_text(json.dumps(data, ensure_ascii=False, indent=2), encoding="utf-8")
    lines = ["# 原始词典数据审计", "", "本报告由 `analyze_sources.py` 只读生成，不修改原始文件。SHA-256 可用于固定当前本地快照。", ""]
    for name in ("ecdict.csv", "lemma.en.txt", "eng.txt"):
        facts = data[name]
        lines.extend([f"## `{name}`", "", f"- 大小：{facts['bytes']:,} bytes；编码：{facts['encoding']}；物理行：{facts['physical_lines']:,}；SHA-256：`{facts['sha256']}`", ""])
        for key in ("logical_csv_records", "unique_casefold_words", "duplicate_rows", "rows_with_overflow_columns", "missing_translation_rows", "translation_rows_with_literal_backslash_newline", "translation_rows_with_physical_newline", "parsed_groups", "parsed_form_relations", "malformed_data_lines", "forms_with_multiple_lemmas", "records", "malformed_records", "duplicate_exact_relations", "unique_relation_triples", "distinct_tagsets", "pairs_with_multiple_tagsets"):
            if key in facts:
                lines.append(f"- {key}: {facts[key]:,}")
        if "columns" in facts:
            lines.append(f"- CSV columns: `{', '.join(facts['columns'])}`")
            lines.append(f"- Exchange type counts: `{json.dumps(facts['exchange_code_frequency'], ensure_ascii=False)}`")
            lines.append(f"- Tags: `{json.dumps(facts['tag_frequency'], ensure_ascii=False)}`")
        if name == "eng.txt":
            lines.append(f"- UniMorph POS counts: `{json.dumps(facts['upos_frequency'], ensure_ascii=False)}`")
            lines.append(f"- UniMorph tag components: `{json.dumps(facts['tag_component_frequency'], ensure_ascii=False)}`")
        lines.extend(["", "前 20 个物理行（长行截断至 350 字符）：", "", "```text"])
        lines.extend(facts["first_20_lines"])
        lines.extend(["```", ""])
    if "existing_output_dictionary" in data:
        lines.extend(["## 当前构建产物基线", "", "```json", json.dumps(data["existing_output_dictionary"], ensure_ascii=False, indent=2), "```", ""])
    lines.extend(["## 许可与合并门槛", "", "- ECDICT 上游仓库标注 MIT；合并其词条/关系时保留版权声明和许可证副本，并注意 README 所述多个上游数据来源。", "- `lemma.en.txt` 文件头只明确允许研究/教育用途，没有明确说明随商业或一般 APK 再分发；本轮构建器不把它作为可随 APK 分发的数据来源，除非后续获得明确许可。", "- UniMorph `eng` 仓库 README 标注来源 Wikipedia、CC BY-SA 3.0。若纳入，保留来源、版本、修改说明及 ShareAlike 许可声明；相关关系来源单独标记为 `unimorph`。", ""])
    MARKDOWN.write_text("\n".join(lines), encoding="utf-8")


if __name__ == "__main__":
    data = analyze()
    write_report(data)
    print(json.dumps({name: {key: value for key, value in facts.items() if key not in {"first_20_lines", "required_samples", "examples", "exchange_examples", "sample_words"}} for name, facts in data.items()}, ensure_ascii=False, indent=2))
