# 原始词典数据审计

本报告由 `analyze_sources.py` 只读生成，不修改原始文件。SHA-256 可用于固定当前本地快照。

## `ecdict.csv`

- 大小：66,009,548 bytes；编码：UTF-8 (optional BOM) validated；物理行：770,612；SHA-256：`d1ecbdf9ee2b920afb6f430ca6b6bb9f559588cf0dff86e04bd255f9390843cc`

- logical_csv_records: 770,611
- unique_casefold_words: 770,611
- duplicate_rows: 0
- rows_with_overflow_columns: 0
- missing_translation_rows: 373
- translation_rows_with_literal_backslash_newline: 52,999
- translation_rows_with_physical_newline: 0
- CSV columns: `word, phonetic, definition, translation, pos, collins, oxford, tag, bnc, frq, exchange, detail, audio`
- Exchange type counts: `{"0": 58625, "1": 58625, "s": 33581, "i": 12650, "p": 11711, "d": 11251, "3": 8727, "r": 713, "t": 508, "f": 5}`
- Tags: `{"gre": 7504, "toefl": 6974, "cet6": 5407, "ielts": 5040, "ky": 4801, "cet4": 3849, "gk": 3677, "zk": 1603}`

前 20 个物理行（长行截断至 350 字符）：

```text
word,phonetic,definition,translation,pos,collins,oxford,tag,bnc,frq,exchange,detail,audio
'hood,hʊd,,"n. 罩；风帽；（布质）面罩；学位连领帽（表示学位种类）\nv. 覆盖；用头巾包；使(马,鹰等)戴头罩；给…加罩\n[网络] 胡德；兜帽；引擎盖",,,,,0,0,,,
's Gravenhage,",skrɑ:vən'hɑ:ɡə",,[荷兰语]海牙(= Hague),,,,,0,0,,,
'tween,twi:n,,prep. [诗歌用语] = between [亦作 tweeny],,,,,0,0,,,
'tween decks,,,[网络] 甲板间；二层舱；双层甲板,,,,,0,0,,,
-ability,ə'biliti,,"suf. [用于构成名词, 意义相当于-able]表示“能力”, “可能性”, “倾向性”, “适宜性”",,,,,0,0,,,
-able,əbl,,"suf. [附在动词或名词后构成形容词] 表示：“能…的,会…的”：durable; “可以…的”：drinkable; “具有…特性的”：fashionable; “有助于…的”,“倾向于…的”：peaceable; “易于…的”：breakable",,,,,0,0,,,
-ably,əbli,,suf. [构成与-able结尾的形容词相应的副词],,,,,0,0,,,
-acy,əsi,,"suf. [用于构成抽象名词]表示性质, 状态, 位置, 地位, 职位",,,,,0,0,,,
-ad,'æd,," [医]〔状语性后缀〕意为向…方, 向…侧",,,,,0,0,,,
-ade,eid,,"suf. 用以构成名词表示：“行动”,“…的动作”：blockade; “行动者”：renegade; “行动者的集体”：cavalcade; “行动的产物”：arcade; “参与”,“参加”：brigade",,,,,0,0,,,
-adelphous,ə'delfəs,, 表示“[植物学]有雄蕊束的”：monadelphous,,,,,0,0,,,
-aemia,'i:miə,, [用以构成名词]表示血液,,,,,0,0,,,
-age,idʒ,,comb. 表示数量;表示动作;表示身份;表示地方,,,,,0,0,,,
-agogue,ə'ɡɒɡ,," [医]〔后缀〕意为催, 利",,,,,0,0,,,
-aholic,,, 嗜好…的；对…成瘾的,,,,,0,0,,,
-al,,," [医]〔后缀〕用于化合物名之后, 示有醛基-CHO存在, 如chloral（氯醛, 三氯乙醛）",,,,,0,0,,,
-algia,'ælɡə,, [医]〔后缀〕意为痛,,,,,0,0,,,
-alia,,, （与某活动或兴趣范围）有关的物品,,,,,0,0,,,
-ally,,, 以-al结尾的形容词加ly构成副词),,,,,0,0,,,
```

## `lemma.en.txt`

- 大小：2,318,694 bytes；编码：UTF-8 (optional BOM) validated；物理行：84,497；SHA-256：`e255b097404e3e0052060e2ddf6e15a1414f577071d63d51d2ca0ce9dacee0fc`

- parsed_groups: 84,487
- parsed_form_relations: 102,867
- malformed_data_lines: 0
- forms_with_multiple_lemmas: 828

前 20 个物理行（长行截断至 350 字符）：

```text
; En Lemma Database (version 1.0.3)
; Compiled by Lin Wei (https://github.com/skywind3000), Mar 28, 2017
; by referencing the 100M+ words in the British National Corpus (BNC), 
; NodeBox Linguistics and Yasumasa Someya's lemma list.
; This lemma list is provided "as is" and is free to use for any research
; and/or educational purposes. 
; The list currently contains 186,523 words (tokens) in 84,487 lemma groups.
; If you have any questions or comments about this lemma list, feel free 
; to contact me (skywind3000@163.com), at any time..
;
be/4109826 -> is,was,are,were,'s,been,being,'re,'m,am,m
have/1315648 -> had,has,'ve,having,'s,'d,d,ve
it/1213224 -> its,they
he/1196022 -> his,him,they
i/1133697 -> my,me,we,is
they/841960 -> their,them,'em
you/804279 -> your,ya,ye
not/767330 -> n't
she/653505 -> her
do/535646 -> did,does,done,doing,du,d'
```

## `eng.txt`

- 大小：18,022,905 bytes；编码：UTF-8 (optional BOM) validated；物理行：652,477；SHA-256：`20a191cefdc7cad6fa74b00f49d6f658684f17b14541aae372e5a3d5a8c15c67`

- forms_with_multiple_lemmas: 11,029
- records: 652,477
- malformed_records: 0
- duplicate_exact_relations: 913
- unique_relation_triples: 651,564
- distinct_tagsets: 10
- pairs_with_multiple_tagsets: 58,112
- UniMorph POS counts: `{"N": 388563, "ADJ": 136400, "V": 127514}`
- UniMorph tag components: `{"N": 388563, "SG": 287165, "ADJ": 136400, "V": 127514, "PL": 125314, "PRS": 47834, "V.PTCP": 47834, "PST": 47832, "NFIN": 31848, "IMP+SBJV": 31848, "3": 23916, "CMPR": 2008, "SPRL": 2008}`

前 20 个物理行（长行截断至 350 字符）：

```text
microtome	microtomes	N;PL
microtome	microtomes	V;PRS;3;SG
microtome	microtoming	V;V.PTCP;PRS
microtome	microtomed	V;PST
microtome	microtomed	V;V.PTCP;PST
eat	eats	V;PRS;3;SG
eat	eating	V;V.PTCP;PRS
eat	ate	V;PST
eat	eaten	V;V.PTCP;PST
eat	eats	N;PL
rastrography	rastrographies	N;PL
magnetencephalography	magnetencephalographies	N;PL
microtome	microtomes	N;PL
microtome	microtomes	V;PRS;3;SG
microtome	microtoming	V;V.PTCP;PRS
microtome	microtomed	V;PST
microtome	microtomed	V;V.PTCP;PST
rectangularize	rectangularizes	V;PRS;3;SG
rectangularize	rectangularizing	V;V.PTCP;PRS
rectangularize	rectangularized	V;PST
```

## 当前构建产物基线

```json
{
  "bytes": 2891776,
  "entries": 10000,
  "senses": 15730,
  "sample_words": {
    "Introduction": {
      "word": "introduction",
      "phonetic": ".intrә'dʌkʃәn",
      "translation_raw": "n. 介绍, 传入, 采用, 初步",
      "exchange": "s:introductions",
      "senses": [
        [
          "n",
          "介绍, 传入, 采用, 初步",
          0
        ]
      ]
    },
    "important": {
      "word": "important",
      "phonetic": "im'pɒ:tәnt",
      "translation_raw": "a. 重要的, 有地位的, 大量的, 显要的, 自负的\\n[计] 要点",
      "exchange": null,
      "senses": [
        [
          "adj",
          "重要的, 有地位的, 大量的, 显要的, 自负的\n[计] 要点",
          0
        ]
      ]
    },
    "factory": {
      "word": "factory",
      "phonetic": "'fæktәri",
      "translation_raw": "n. 工厂, 产生地, 代理店\\n[经] 工厂, 代理店, 商行在国外的代理处",
      "exchange": "s:factories",
      "senses": [
        [
          "n",
          "工厂, 产生地, 代理店\n[经] 工厂, 代理店, 商行在国外的代理处",
          0
        ]
      ]
    },
    "practice": {
      "word": "practice",
      "phonetic": "'præktis",
      "translation_raw": "n. 实践, 练习, 实行, 惯例, 习惯, 开业\\nv. 实践, 实行, 练习, 实习, 业务",
      "exchange": "s:practices/3:practices/i:practicing/p:practiced/d:practiced",
      "senses": [
        [
          "n",
          "实践, 练习, 实行, 惯例, 习惯, 开业",
          0
        ],
        [
          "v",
          "实践, 实行, 练习, 实习, 业务",
          1
        ]
      ]
    },
    "book": {
      "word": "book",
      "phonetic": "buk",
      "translation_raw": "n. 书, 书籍, 帐簿, 名册, 工作簿\\nv. 登记, 预订\\n[计] 工作簿",
      "exchange": "s:books/d:booked/p:booked/i:booking/3:books",
      "senses": [
        [
          "n",
          "书, 书籍, 帐簿, 名册, 工作簿",
          0
        ],
        [
          "v",
          "登记, 预订\n[计] 工作簿",
          1
        ]
      ]
    },
    "go": {
      "word": "go",
      "phonetic": "gou",
      "translation_raw": "vi. 去, 走, 达到, 运转, 查阅, 消失, 结束, 放弃, 花费, 流传, 趋于, 打算, 剩下\\nvt. 以...打赌, 对付, 忍受, 出产, 为被捕者出(保释金)\\nn. 去, 尝试, 进行",
      "exchange": "i:going/p:went/d:gone/3:goes",
      "senses": [
        [
          "vi",
          "去, 走, 达到, 运转, 查阅, 消失, 结束, 放弃, 花费, 流传, 趋于, 打算, 剩下",
          0
        ],
        [
          "vt",
          "以...打赌, 对付, 忍受, 出产, 为被捕者出(保释金)",
          1
        ],
        [
          "n",
          "去, 尝试, 进行",
          2
        ]
      ]
    },
    "read": {
      "word": "read",
      "phonetic": "ri:d",
      "translation_raw": "v. 读, 阅读, 理解\\na. 有学问的\\nn. 读取, 阅读\\n[计] 读取",
      "exchange": "d:read/0:read/1:dp/i:reading/p:read/3:reads",
      "senses": [
        [
          "v",
          "读, 阅读, 理解",
          0
        ],
        [
          "adj",
          "有学问的",
          1
        ],
        [
          "n",
          "读取, 阅读\n[计] 读取",
          2
        ]
      ]
    }
  }
}
```

## 许可与合并门槛

- ECDICT 上游仓库标注 MIT；合并其词条/关系时保留版权声明和许可证副本，并注意 README 所述多个上游数据来源。
- `lemma.en.txt` 文件头只明确允许研究/教育用途，没有明确说明随商业或一般 APK 再分发；本轮构建器不把它作为可随 APK 分发的数据来源，除非后续获得明确许可。
- UniMorph `eng` 仓库 README 标注来源 Wikipedia、CC BY-SA 3.0。若纳入，保留来源、版本、修改说明及 ShareAlike 许可声明；相关关系来源单独标记为 `unimorph`。
