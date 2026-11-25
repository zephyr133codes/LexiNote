"""Validate structural integrity, provenance, and morphology regressions."""
import json
import sqlite3
import sys
from pathlib import Path

path = Path(sys.argv[1] if len(sys.argv) > 1 else Path(__file__).resolve().parent / "output" / "dictionary-v0.3.5.5.db")
if not path.exists():
    raise SystemExit(f"Database not found: {path}. Run build_dictionary.py using the local raw files.")
db = sqlite3.connect(f"file:{path.as_posix()}?mode=ro", uri=True)
db.execute("PRAGMA foreign_keys=ON")
tables = {row[0] for row in db.execute("SELECT name FROM sqlite_master WHERE type='table'")}
required = {"dictionary_entries", "dictionary_senses", "dictionary_word_forms"}
if not required.issubset(tables):
    raise SystemExit(f"Missing tables: {sorted(required - tables)}")

result = {
    "path": str(path),
    "file_bytes": path.stat().st_size,
    "integrity": db.execute("PRAGMA integrity_check").fetchone()[0],
    "foreign_key_violations": db.execute("PRAGMA foreign_key_check").fetchall(),
    "entries": db.execute("SELECT COUNT(*) FROM dictionary_entries").fetchone()[0],
    "senses": db.execute("SELECT COUNT(*) FROM dictionary_senses").fetchone()[0],
    "word_forms": db.execute("SELECT COUNT(*) FROM dictionary_word_forms").fetchone()[0],
    "entries_without_senses": db.execute("SELECT COUNT(*) FROM dictionary_entries e WHERE NOT EXISTS (SELECT 1 FROM dictionary_senses s WHERE s.entry_id=e.id)").fetchone()[0],
    "duplicate_casefold_words": db.execute("SELECT COUNT(*) FROM (SELECT lower(word) FROM dictionary_entries GROUP BY lower(word) HAVING COUNT(*)>1)").fetchone()[0],
    "duplicate_word_form_relations": db.execute("SELECT COUNT(*) FROM (SELECT lower(lemma),lower(form),pos,form_type,COUNT(*) c FROM dictionary_word_forms GROUP BY lower(lemma),lower(form),pos,form_type HAVING c>1)").fetchone()[0],
    "word_forms_without_lemma_entry": db.execute("SELECT COUNT(*) FROM dictionary_word_forms f WHERE NOT EXISTS (SELECT 1 FROM dictionary_entries e WHERE e.word=f.lemma COLLATE NOCASE)").fetchone()[0],
    "word_forms_without_form_entry": db.execute("SELECT COUNT(*) FROM dictionary_word_forms f WHERE NOT EXISTS (SELECT 1 FROM dictionary_entries e WHERE e.word=f.form COLLATE NOCASE)").fetchone()[0],
    "senses_by_pos": db.execute("SELECT pos,COUNT(*) FROM dictionary_senses GROUP BY pos ORDER BY COUNT(*) DESC").fetchall(),
    "forms_by_type": db.execute("SELECT form_type,COUNT(*) FROM dictionary_word_forms GROUP BY form_type ORDER BY form_type").fetchall(),
    "forms_by_source": db.execute("SELECT source,COUNT(*) FROM dictionary_word_forms GROUP BY source ORDER BY source").fetchall(),
}

def has(lemma, form, kind):
    return db.execute("SELECT 1 FROM dictionary_word_forms WHERE lemma=? COLLATE NOCASE AND form=? COLLATE NOCASE AND form_type=? LIMIT 1", (lemma, form, kind)).fetchone() is not None

samples = {
    "go_went_past": has("go", "went", "past"),
    "go_gone_past_participle": has("go", "gone", "past_participle"),
    "go_going_present_participle": has("go", "going", "present_participle"),
    "go_goes_third_person": has("go", "goes", "third_person"),
    "study_studied_past": has("study", "studied", "past"),
    "study_studied_past_participle": has("study", "studied", "past_participle"),
    "study_studying": has("study", "studying", "present_participle"),
    "run_ran": has("run", "ran", "past"),
    "run_running": has("run", "running", "present_participle"),
    "child_children": has("child", "children", "plural"),
    "mouse_mice": has("mouse", "mice", "plural"),
    "see_saw": has("see", "saw", "past"),
    "see_seen": has("see", "seen", "past_participle"),
    "good_better": has("good", "better", "comparative"),
    "good_best": has("good", "best", "superlative"),
    "book_books_noun": db.execute("SELECT 1 FROM dictionary_word_forms WHERE lemma='book' COLLATE NOCASE AND form='books' COLLATE NOCASE AND form_type='plural' AND pos='n'").fetchone() is not None,
    "book_books_verb": db.execute("SELECT 1 FROM dictionary_word_forms WHERE lemma='book' COLLATE NOCASE AND form='books' COLLATE NOCASE AND form_type='third_person' AND pos='v'").fetchone() is not None,
    "read_same_spelling_past": has("read", "read", "past"),
    "read_same_spelling_past_participle": has("read", "read", "past_participle"),
    "important_entry": db.execute("SELECT 1 FROM dictionary_entries WHERE word='important' COLLATE NOCASE").fetchone() is not None,
    "introduction_entry": db.execute("SELECT 1 FROM dictionary_entries WHERE word='introduction' COLLATE NOCASE").fetchone() is not None,
    "factory_entry": db.execute("SELECT 1 FROM dictionary_entries WHERE word='factory' COLLATE NOCASE").fetchone() is not None,
    "practice_entry": db.execute("SELECT 1 FROM dictionary_entries WHERE word='practice' COLLATE NOCASE").fetchone() is not None,
    "smartphone_entry": db.execute("SELECT 1 FROM dictionary_entries WHERE word='smartphone' COLLATE NOCASE").fetchone() is not None,
    "smartphones_entry": db.execute("SELECT 1 FROM dictionary_entries WHERE word='smartphones' COLLATE NOCASE").fetchone() is not None,
    "smartphones_to_smartphone_plural": has("smartphone", "smartphones", "plural"),
    "have_has_third_person": has("have", "has", "third_person"),
    "have_had_past": has("have", "had", "past"),
    "have_had_past_participle": has("have", "had", "past_participle"),
    "have_having_present_participle": has("have", "having", "present_participle"),
    "hav_not_linked_to_have_forms": db.execute(
        "SELECT 1 FROM dictionary_word_forms WHERE (lemma='hav' COLLATE NOCASE AND form IN ('had','has','having')) OR (lemma='have' COLLATE NOCASE AND form='hav') LIMIT 1"
    ).fetchone() is None,
}
result["required_samples"] = samples
result["all_required_samples_pass"] = all(samples.values())
db.close()
print(json.dumps(result, ensure_ascii=False, indent=2))
failed = (result["integrity"] != "ok" or result["foreign_key_violations"] or result["duplicate_casefold_words"] or result["duplicate_word_form_relations"] or result["word_forms_without_lemma_entry"] or result["word_forms_without_form_entry"] or not result["all_required_samples_pass"])
if failed:
    raise SystemExit(1)
