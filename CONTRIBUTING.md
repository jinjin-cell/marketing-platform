# 贡献约定（Git 提交与分支）

本项目要求 Git 历史**保留分支分叉（merge commit）**，而不是一条直线（线性 fast-forward）。
分叉能让每个功能的起止、归属和合并关系在 `git log --graph`（IDE 的 Git 日志视图）里一眼看清，
例如下面这段就是期望的效果：

```

*   617a590 merge: 合并活动权重规则与账户签到接口
|\
| * 03682cd chore: 统一业务日志和异常提示为中文
| * f861fc7 feat: 完善活动权重规则校验及账户签到查询接口
| *   9b6fc92 merge: 同步 master 最新代码
| |\
| |/
|/|
* | e5f0cb7 feat(rebate): 同步 user_behavior_rebate_order 分表 out_business_no 字段的代码支持
```

## 为什么

- 每个功能在图上都是一个独立的分支段，便于回溯「这个需求改动包含哪些提交」。
- merge commit 会保留两个父节点，合并点清晰，cherry-pick / revert 边界明确。
- 直线历史难以看出需求的聚合与边界，代码评审和 bug 定位都更费劲。

## 提交工作流（每个功能 / 每个完整改动，以 master 为中心）

**① 从 master 新建一个功能分支**：

```powershell
git checkout -b feat/<功能名> master
```

**② 在功能分支上写完业务并提交**（中文 message，见下方命名规范）：

```powershell
git add -A
git commit -m "feat: 描述这个改动"
```

**③ 把功能分支推送到远程**：

```powershell
git push -u origin feat/<功能名>
```

**④ 切换到 master 并拉到最新**：

```powershell
git checkout master
git pull
```

**⑤ 在本地把功能分支合并进 master**（关键：`--no-ff` 强制产生 merge commit，而不是 fast-forward）：

```powershell
git merge --no-ff feat/<功能名> -m "merge: 合并 <功能名>"
```

**⑥ 推送 master 到远程**：

```powershell
git push origin master
```

> `--no-ff` 是保证「永远出现分叉」的关键。即使功能分支可以直接 fast-forward，
> 也要用它保留分支拓扑，让合并点在图上呈现 `|\ /|` 的分叉形状。

## Commit Message 规范

沿用现有风格：`<type>(<scope>): <中文描述>`

| type | 用途 | 示例 |
| --- | --- | --- |
| `feat` | 新功能 | `feat: 完善用户积分发奖服务并落库积分账户` |
| `feat(scope)` | 带模块范围的新功能 | `feat(rebate): 补充注释` |
| `fix` | 修复缺陷 | `fix: 修复额度入账为空指针` |
| `chore` | 构建 / 工具 / 非业务改动 | `chore: 统一业务日志和异常提示为中文` |
| `docs` | 文档 | `docs: 补充用户行为返利入账说明` |
| `refactor` | 重构 | `refactor: 抽取活动权重计算逻辑` |
| `merge` | 合并分支 | `merge: 合并活动权重规则与账户签到接口` |

- Merge 提交统一使用 `merge: ...`，与分支拓扑对应。
- 描述用中文，简洁说明「做了什么」，不必写"优化/完善"这类无实质内容的词。

## 禁止的行为

- ❌ 一直在同一个分支上连续 commit，不切分支（会产生直线）。
- ❌ 用 `git rebase` / `git pull --rebase` 把提交「捋平」成直线；确有需要时请先与团队确认。
- ❌ 合并时用默认 fast-forward（不带 `--no-ff`），导致分叉丢失。
- ❌ 直接在 `master` 上做开发提交。
- ❌ 合并前不 `git pull` 就直接 `git push`，可能导致远程被驳回或覆盖他人提交。

## 对于 AI 代理（本仓库的协作约定）

当代理（如 DeepSeek Harness）替你落地代码并提交时，遵循本文工作流：

1. 针对每个完整功能/改动，先从 master 切分支：`git checkout -b feat/<功能名> master`。
2. 在功能分支上按规范提交中文 message，并 `git push -u origin feat/<功能名>`。
3. `git checkout master` 后先 `git pull`，再用 `git merge --no-ff feat/<功能名> -m "merge: 合并 <功能名>"` 合并。
4. 最后 `git push origin master`。

单个琐碎改动是否需要单独成支，代理会先确认；不擅自改动 `master` / git 配置 / 提交历史；若 `master` 本地落后于远程，不直接强推。
