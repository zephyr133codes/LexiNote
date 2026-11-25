PRAGMA foreign_keys = ON;
CREATE TABLE dictionary_entries (
 id INTEGER PRIMARY KEY,
 word TEXT NOT NULL COLLATE NOCASE UNIQUE,
 phonetic TEXT,
 translation_raw TEXT NOT NULL,
 exchange TEXT,
 tag TEXT,
 frq INTEGER,
 bnc INTEGER
);
CREATE TABLE dictionary_senses (
 id INTEGER PRIMARY KEY,
 entry_id INTEGER NOT NULL REFERENCES dictionary_entries(id) ON DELETE CASCADE,
 pos TEXT NOT NULL,
 meaning TEXT NOT NULL,
 sort_order INTEGER NOT NULL
);
CREATE INDEX idx_dictionary_entries_word_nocase ON dictionary_entries(word COLLATE NOCASE);
CREATE INDEX idx_dictionary_senses_entry_order ON dictionary_senses(entry_id, sort_order);
CREATE INDEX idx_dictionary_senses_entry_pos_order ON dictionary_senses(entry_id, pos, sort_order);
CREATE TABLE dictionary_word_forms (
 id INTEGER PRIMARY KEY,
 lemma TEXT NOT NULL COLLATE NOCASE REFERENCES dictionary_entries(word) ON DELETE CASCADE,
 form TEXT NOT NULL COLLATE NOCASE REFERENCES dictionary_entries(word) ON DELETE CASCADE,
 pos TEXT NOT NULL DEFAULT '',
 form_type TEXT NOT NULL,
 source TEXT NOT NULL,
 source_priority INTEGER NOT NULL,
 quality TEXT NOT NULL
);
CREATE UNIQUE INDEX idx_dictionary_word_forms_unique
 ON dictionary_word_forms(lemma COLLATE NOCASE, form COLLATE NOCASE, pos, form_type, source);
CREATE INDEX idx_dictionary_word_forms_lemma
 ON dictionary_word_forms(lemma COLLATE NOCASE, form_type, pos);
CREATE INDEX idx_dictionary_word_forms_form
 ON dictionary_word_forms(form COLLATE NOCASE, form_type, pos);
