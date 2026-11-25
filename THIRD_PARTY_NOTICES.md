# 离线词典第三方数据声明

LexiNote 随应用分发的 `dictionary.db` 包含以下第三方数据的整理和修改版本：

## ECDICT

来源：ECDICT（`https://github.com/skywind3000/ECDICT`），MIT License，Copyright (c) 2025 Linwei。ECDICT 的 README 说明其数据在长期维护过程中汇集自多个来源；ECDICT 的 MIT 声明不应被理解为替代或消除可能适用于上游数据的额外义务。LexiNote 对词条进行筛选、释义分组并提取 exchange 词形关系。完整 MIT 许可文本可在上述仓库的 `LICENSE` 文件查看。

MIT License

Copyright (c) 2025 Linwei

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

## UniMorph English

来源：UniMorph `eng`，来源说明为 Wikipedia，`https://github.com/unimorph/eng`，许可为 Creative Commons Attribution-ShareAlike 3.0（CC BY-SA 3.0，`https://creativecommons.org/licenses/by-sa/3.0/`）。LexiNote 仅保留可识别的名词复数、动词时态/分词及形容词比较级/最高级标签，经过词性检查、冲突过滤、去重并合并来源信息。该整理关系数据依 CC BY-SA 3.0 提供；再分发时须提供署名、许可链接和修改说明，并遵循相同方式共享条款。

## 排除的数据

`lemma.en.txt` 文件头标明仅供研究和/或教育用途；目前没有确认其允许随一般 Android APK 再分发，因此未将其内容合并到 `dictionary.db`。
