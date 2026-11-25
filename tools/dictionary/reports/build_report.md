# V0.3.5.5 词典构建报告

- 数据库：`dictionary-v0.3.5.5.db`；97,251 词条、123,184 条释义、59,362 条词形关系。
- 文件大小：37,601,280 bytes；SHA-256：`55cdc83e226107fe8b8801e83144ab2f6ef139964c1ef08a85e16d5c9c4377a5`。
- 词形来源记录：`{"ecdict": 58226, "ecdict-translation": 12493, "unimorph-eng": 19656}`（双来源关系分别计入来源覆盖统计）。
- 词形类型：`{"comparative": 664, "past": 9734, "past_participle": 9255, "plural": 24680, "present_participle": 7244, "superlative": 500, "third_person": 7285}`。
- ECDICT 明确写出‘原形的复数’且由 UniMorph N;PL 交叉确认的补充关系：12,494 条。
- UniMorph 未映射标签 427,481 行，词性或描述冲突拒绝 35,326 行；ECDICT 未知编码：`{"f": 4}`。
- 未识别词性的释义 22,525 条，原文仍然保留。
- 旧版回归：缺失词条 0，缺失释义 0，音标变化 0；旧词条新增释义 0 条。
- 旧版数据库：{'path': 'D:\\MyProject\\LexiNote\\tools\\dictionary\\output\\dictionary.db', 'present': True, 'entries': 10000}。lemma.en.txt 未并入，因为再分发授权尚不明确。

- V0.3.5.4 回归：缺失词条 0，缺失释义 0，缺失关系 4（仅 have 词族中错误的 hav 关系）。

逐词样例、冲突样例及其他详细计数见同目录 `build_report.json`。
