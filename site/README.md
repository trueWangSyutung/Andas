# Andas GitBook 文档站

本目录使用 [dunwu/gitbook-templates](https://github.com/dunwu/gitbook-templates) 的
`doc-template` 结构与主题，构建 Andas 的 GitHub Pages 文档站。

## 内容来源

| 站点页面 | 源文件 |
| --- | --- |
| 项目简介 | `site/docs/README.md`（本目录维护） |
| 用户指南 | `doc/charter*/**.md` |
| 快速开始 | `QUICK_START.md` |
| API 参考 | `API_REFERENCE.md` |
| 版本更新 | `CHANGELOG_0.1.5.md` |
| 基准测试报告 | `BENCHMARK_REPORT.md` |
| 实验复现 | `bench/README.md` |
| 英文技术报告 | `report/` + `site/docs/benchmark/english-report.md` |

`site/docs/guide/`、`site/docs/benchmark/report.md`、`site/docs/assets/` 等由
`scripts/sync_docs.py` 生成，不提交到仓库。

## 本地构建

GitBook 3 需要 Node.js 10：

```bash
cd site
python3 scripts/sync_docs.py
npm install -g gitbook-cli@2.3.2
gitbook install
gitbook build . _book
python3 -m http.server 8000 -d _book
```

打开 <http://127.0.0.1:8000/>。

## 部署

`.github/workflows/pages.yml` 在 `main` 分支变更文档时自动执行：

1. 同步 `doc/`、`bench/`、`report/` 到 `site/docs/`；
2. `gitbook install && gitbook build`；
3. 通过 GitHub Pages Actions 发布 `site/_book`。

站点地址：<https://truewangsyutung.github.io/Andas/>。
