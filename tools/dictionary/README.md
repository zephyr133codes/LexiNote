# LexiNote 离线英语词典构建

数据来源优先为 ECDICT 官方仓库： https://github.com/skywind3000/ECDICT
官方原始 CSV 地址： https://raw.githubusercontent.com/skywind3000/ECDICT/master/ecdict.csv
本项目脚本首次运行时会尝试从该地址下载；网络不可用时，可手动将官方仓库的 `ecdict.csv` 放到 `tools/dictionary/raw/ecdict.csv`。

官方仓库标注 MIT 许可证（LICENSE），但 README 描述词典汇集自多个词库和语料来源。MIT 标注不应被视为自动清除了所有上游数据权利义务；在将数据库随 APK 分发前，应逐项核实第三方数据来源许可及归属要求。本工作区保留原始来源信息，未将数据库复制到 Android assets。

## 构建

在仓库根目录执行：

```powershell
python tools/dictionary/build_dictionary.py
python tools/dictionary/validate_dictionary.py
```

输出：`output/dictionary.db` 和 `reports/build_report.json`。脚本使用标准库、逐行读取 CSV；重复单词采用大小写折叠键合并，尽量补齐空字段，并在释义更完整时采用较长 translation。优先按真实 tag 中的 CET/考试标签，再按真实 frq、bnc 数值排序，最多收录 10,000 个有效词条，不伪造词频。

## 释义规则

translation 原文逐词条保留。只将 `n.`、`v.`、`adj.`、`adv.` 等明确词性标记作为分组边界；各标记之间文本作为该词性释义整体保存，不按逗号、分号拆义项。未能识别词性时以 `unknown` 保存整段内容。exchange 原始字符串保留，不执行词形还原。词条查询键使用 SQLite `NOCASE` 唯一约束。

实际版本/提交标识、下载时间、原始/最终行数、异常计数、查询样例、文件大小均写入构建报告。首次下载成功时报告记录下载 URL；执行后请保留 CSV 以便复现。
