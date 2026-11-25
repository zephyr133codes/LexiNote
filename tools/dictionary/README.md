# LexiNote 离线英语词典构建

构建仅读取 `raw` 中用户已经准备的本地文件，不会联网下载、改写或删除原始数据。V0.3.5.5 保留 V0.3.5.4 的词条补充；如果 ECDICT 词条仅有缩写释义，则拒绝 UniMorph 将其标为动词原形的关系，避免把缩写 `hav` 误用为 `have` 词族的动词原形。

## 来源与许可

- `ecdict.csv`：UTF-8 CSV，770,611 条逻辑记录，13 列；释义字段使用字面 `\\n` 分隔，构建时只在释义解析层转换。ECDICT 仓库采用 MIT 许可证；其 README 说明词典长期汇集了多种来源，随 APK 发布时仍需保留声明并留意上游来源要求。
- `eng.txt`：UTF-8、三列 TSV（lemma、form、tagset），652,477 条记录。仅映射本构建脚本明确支持的名词复数、动词时态/分词和形容词比较级/最高级标签；未知标签不推测。UniMorph `eng` 仓库声明 CC BY-SA 3.0，随 APK 发布时保留署名、修改说明和相同方式共享声明。
- `lemma.en.txt`：UTF-8 的 `lemma/count -> comma-separated forms` 格式，84,497 组。文件头只授权研究和/或教育用途，未明确 APK 再分发授权，因此只审计、不合并到可分发数据库。取得明确许可后再加入。

原始快照的哈希、头部样例、字段统计、异常及样例见 `reports/source_analysis.md`。APK 随附的许可证告知文本见 Android assets 的 `THIRD_PARTY_NOTICES.md`。

## 构建和校验

使用 Python 3 标准库：

```powershell
python tools/dictionary/analyze_sources.py
python tools/dictionary/build_dictionary.py
python tools/dictionary/validate_dictionary.py
```

产物为 `output/dictionary-v0.3.5.5.db`，不会覆盖旧 `output/dictionary.db` 或 Android assets。先检查 `reports/build_report.json` 和校验结果；只有通过后，才由交付步骤将候选库复制到 `android/app/src/main/assets/dictionary.db`。

词条按真实考试标签、Oxford 标记或 BNC/FRQ 正排名筛选；再补入这些词条在 ECDICT exchange 中明确关联且自身有释义的形式，并保留所有旧版词条及释义。中文释义不生成。未识别的词性保留为 `unknown`，不丢弃其释义。

词形关系优先取 ECDICT exchange 的明确编码，其次取 UniMorph 明确标签。对 ECDICT exchange 未提供形式的少数词条，仅在释义明确写出“某原形的复数”且 UniMorph 同时存在对应 `N;PL` 关系时补入两端词条及关系。相同 lemma/form/词性/变化类型只存一条并合并来源；存在 ECDICT 相反类型时舍弃冲突的 UniMorph 关系。非法端点、未知编码和 POS 冲突计入构建报告。`past` 与 `past_participle` 分别存储，即使同一单词拼写（如 read → read）也保留。

本工具仅构建与校验词典 SQLite，不接触用户的 `lexinote.db`。
