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
