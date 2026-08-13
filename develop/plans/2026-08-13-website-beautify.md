# 官网 + 附属页「玻璃拟态打磨」美化方案 — 设计文档

> **状态:** 方案设计已确认（2026-08-13，逐点选择），**代码暂不实施**，待用户明确指示后按本方案分阶段执行。
>
> 背景：官网已完成 HTML 屎山重构（样式/脚本外置 + 共用 JS + onerror 抽离）。下一步是视觉美化。用户通过逐点多选确定最终方案，本文件记录全部选择与实施细节。

---

## 背景与目标

**品牌一致性断点（本次美化的出发点）**：

- 官网现状：玻璃拟态（大圆角 40–56px、半透明白卡片、blur、柔和阴影）+ 青蓝色系（`#2f6b8f`/`#1d4d6b`/`#0b3b4e`）。
- 游戏内 UI 偏好（记忆）：新粗野主义/线框/磨砂质感，明确排除玻璃拟态。
- 品牌色（PROJECT_THIRDPARTY）：`#3F48CC` 蓝紫 + `#FDA1FF` 粉。

**用户决策**：保留玻璃拟态 + 青蓝为基调（不动风格骨架），只做细节打磨；品牌色暂不迁移到官网（若未来需要可再议）。本次只做"在现有玻璃拟态框架内的精致化"，附属页轻量换皮、不重构结构。

---

## 逐点选择结果汇总

| # | 可美化点 | 选择 |
|---|---------|------|
| 1 | 视觉方向 | 保留玻璃拟态 + 青蓝（打磨） |
| 2 | Hero 区 | 居中大 logo + 新增品牌标语 |
| 3 | 顶部导航 | 吸顶导航条（锚点），移动端汉堡菜单 |
| 4 | 卡片风格 | 悬停动效 + 圆角 56→32px + 标题左侧渐变竖条 + 阴影统一细腻 |
| 5 | 按钮风格 | 悬停发光 + 次级钮描边化 + 主按钮青蓝渐变 |
| 6 | 字体排版 | 正文阅读优化 + 版本数字特化 + 标题层级增强 + Noto Sans SC（OFL 1.1） |
| 7 | 背景 | 细腻流动渐变（尊重 reduced-motion） |
| 8 | 弹窗 Modal | 过渡动画 + 圆角统一 32px + 标题徽章分隔线 + 平台钮反馈 |
| 9 | 页脚 Footer | 毛玻璃背景 + 三栏分区 + 链接 hover 动画 |
| 10 | 动效微交互 | 滚动 reveal 强化 + 导航滚动毛玻璃 + 锚点平滑滚动 + 尊重降级偏好 |
| 11 | 附属页统一 | 轻量同步首页风格（圆角/毛玻璃/按钮 hover/页脚/元信息），结构不重构 |
| 12 | 响应式 | 触摸目标 44px + 字号断点细化 + 下载网格单列 |

---

## 方案一 — Hero 区 + 品牌标语

**改动：**
- `index.html`：`.hero-card` 内 logo 下方新增 `<p class="hero-tagline" data-i18n="hero_tagline">Roblox 式叙事创作平台</p>`。
- `index.css`：`.hero-tagline` — 青蓝渐变文字（对齐 `.app-title` 技法）、`1.4rem`、`letter-spacing: 2px`、`margin-top: 4px`、柔光 `text-shadow`。
- i18n：新增 key `hero_tagline`，同步 9 语言 locale（zh/zh-TW/en/ja/ko/fr/de/pt/ru）。

## 方案二 — 吸顶导航条（锚点）+ 汉堡菜单

**HTML（`index.html`）：**
- `<body>` 顶部新增 `<nav class="top-nav">`：
  - 左侧：小号品牌文字 `◈ 氢风`（锚点回顶部）。
  - 右侧：`游戏介绍 → #intro`、`资源下载 → #download`、`社区分享 → #community`、`历史版本 → html/history_versions.html`。
  - 移动端：`.top-nav` 内加 `<button class="nav-toggle">☰</button>`，点开下拉展开锚点链接。
- 给对应 section 加锚点 id：`.card-intro` → `id="intro"`、下载 section → `id="download"`、社区 section → `id="community"`。

**CSS（`index.css`）：**
- `.top-nav`：`position: fixed; top:0; width:100%`，初始半透明，滚动后加 `.scrolled` 类 → 毛玻璃背景（`rgba(255,255,255,0.75)` + `backdrop-filter: blur(12px)` + 细下描边）。
- `.nav-toggle` 与下拉面板：移动端 `display` 切换；汉堡菜单下拉用玻璃卡片样式。
- 锚点 `scroll-margin-top` 补偿固定导航高度。

**JS（`main.js`）：**
- 滚动监听：`scrollY > 10` → `.top-nav` 加 `.scrolled`。
- 汉堡切换：点击 `.nav-toggle` → `.top-nav` 加 `.open` 展开链接；点击链接后收起。
- 平滑滚动由 CSS `html { scroll-behavior: smooth }` 实现（见方案六），无需 JS。

**i18n：** 新增 key `nav_intro` / `nav_download` / `nav_community`、汉堡 `nav_menu_label`，同步 9 语言。

## 方案三 — 卡片风格（悬停动效 + 圆角 + 竖条 + 阴影统一）

**CSS 变量（`common.css` `:root`）：**
- 新增 `--radius-card: 32px`、`--shadow-soft`、`--shadow-hover`、`--border-highlight`（青蓝渐变描边色）。
- 各页面卡片统一引用变量，消除阴影大小不一致。

**`.card`（`index.css` + 附属页）：**
- `border-radius: 56px → var(--radius-card)`（32px）。
- hover：`transform: translateY(-6px)` + `box-shadow: var(--shadow-hover)` + 描边高亮（伪元素叠一层青蓝渐变描边，`opacity` 过渡）。

**`.card-header h2`（`index.css`）：**
- 新增 `::before` 左侧青蓝渐变竖条（宽 6px、高 0.7em、圆角）作为主视觉；现有 `::after` 下划线**保留但变浅变细**（改 `#b1d0e8`、高 2px），作辅助元素，二者并存不冲突。

**阴影统一：** `.card` / `.platform-box` / `.changelog-box` / `.repair-steps` / `.download-item` 统一改用 `--shadow-soft`/`--shadow-hover`。

## 方案四 — 按钮风格（悬停发光 + 次级钮描边化 + 主钮渐变）

- **主按钮**（`.tip-btn` 及全站下载主动作）：青蓝渐变 + 高光描边，hover 上浮 + 光晕 `box-shadow: 0 0 0 4px rgba(47,107,143,0.15)`。
- **次级钮描边化**（`.history-link` 等次级入口）：透明底 + 青蓝描边，hover 填充浅青蓝底。
- **弹窗按钮**（`.platform-select-btn` / `.download-option`）：hover 上浮 + 描边高亮 + 光晕（方案八）。
- 圆角：未勾选"统一胶囊"，维持各按钮现状圆角。

## 方案五 — 字体排版（正文优化 + 数字特化 + 层级 + Noto Sans SC）

- **字体引入**：`common.css` 头部 `@import` Google Fonts `Noto Sans SC`（SIL OFL 1.1，无版权争议），`font-display: swap`；`:root` 定义 `--font-body: 'Noto Sans SC', 'Segoe UI', Roboto, 'Helvetica Neue', 'PingFang SC', 'Microsoft YaHei', sans-serif`，`*` 引用。离线时完全回退到系统字体栈，不影响布局。
  - 注意：单个中文子集请求较大，用 `preconnect` + 限制加载字重（400/600）控制体积。
- **标题层级**：`.card-header h2` `2.2rem → 2.6rem`；hero 标语 1.4rem；`.version-badge` 2rem 保持。
- **正文阅读优化**：`.intro-body` `1.1rem → 1.15rem`、`line-height: 1.8`、`letter-spacing: 0.01em`；`.tip-text`、`.log-content` 同步微调。
- **版本数字特化**：`.version-badge` 加 `font-variant-numeric: tabular-nums`，版本号数字等宽对齐，无需额外字体。

## 方案六 — 背景（细腻流动渐变）

- `index.css` `body`：保留 base 渐变底色 `linear-gradient(145deg, #eef2f6, #d9e2ec)`，叠加一层慢速流动层：
  - 用伪元素或第二背景层，`background-position` 缓慢平移（周期 ~20s，`ease-in-out` 往返）。
  - 仅 opacity/transform 相关属性动画，避免重排；`prefers-reduced-motion` 下由 common.css 全局 reduce 关停。

## 方案七 — 弹窗 Modal（过渡 + 圆角 + 标题 + 平台钮反馈）

- **`common.css` `.modal-content`**：`border-radius: 48px → 32px`；过渡动画强化（遮罩淡入 + 弹窗上浮 + 轻微 scale 到位，现有 `translateY(16px) scale(0.97)` 已具备，微调曲线）。
- **标题强化**：`.modal-title` 文字前加图标徽章（`.modal-title::before` 或 common-modal.js 渲染时加 `<span>` 前缀）；分隔线由现有 `border-bottom: 2px dashed` 改为更精致的细分隔线（可选青蓝渐变）。
- **平台按钮反馈**：`.platform-select-btn` / `.download-option` hover 上浮 + 描边高亮 + 光晕。
- `common-modal.js`：如标题渲染需加前缀，做最小改动；交互逻辑与两阶段流程不变。

## 方案八 — 页脚 Footer（毛玻璃 + 三栏分区 + 链接动画）

- **HTML**：`index.html` footer 改为三栏布局 — 品牌区（`⚡ 氢风 · 官方站` + 版权）、快速链接（所有版本/社区资源）、版权与许可（许可证/两个第三方声明）。
- **CSS**：`.footnote` 加毛玻璃背景（`rgba(255,255,255,0.6)` + `backdrop-filter: blur(8px)` + 顶部细描边）；三栏用 flex/grid，移动端单列。
- **链接 hover 动画**：下划线滑入（`background-size` 过渡）或箭头位移，全站 footer 链接统一。

## 方案九 — 动效微交互（reveal 强化 + 导航毛玻璃 + 平滑滚动 + 降级）

- **锚点平滑滚动**：`common.css` 加 `html { scroll-behavior: smooth; }`（`prefers-reduced-motion` 全局降级已覆盖）。
- **滚动 reveal 强化**：现有 `initScrollReveal`（main.js）保留，节奏微调（`idx * 0.05s` 上限 `0.25s`）；新元素（`.hero-tagline`、`.top-nav`）纳入选择器。
- **导航滚动毛玻璃**：见方案二。
- **降级偏好**：common.css 全局 `prefers-reduced-motion` 规则已存在，保持不变并确保新增动画被覆盖。

## 方案十 — 附属页轻量换皮（history/community/license）

原则：**结构不重构，只换皮**。同步首页新的打磨要素：
- **history 页**（`history_versions.html` + `history.css`）：版本卡片圆角 32px、毛玻璃卡片、按钮 hover 动效、分页按钮描边化、链接 hover 动画、页脚与 favicon 统一。
- **community 页**（`community_share.html` + `community.css`）：讨论卡片容器套玻璃卡片风格、圆角 32px、加载骨架保留、页脚统一。
- **license 三页**（`LICENSE.html`/`THIRDPARTY_LICENSES.html`/`PROJECT_THIRDPARTY.html` + `license.css`/`license-tabs.css`）：统一顶部返回 + 页脚样式、圆角/边框微调、毛玻璃容器。
- **全站元信息**：各页 `<title>`、favicon 路径（`html/` 下已用 `../favicon.ico`）、页脚版权信息统一为 `© HujiuGame`。

## 方案十一 — 响应式细节（44px + 断点 + 单列）

- **触摸目标**：全站按钮/链接 `min-height: 44px`（小屏）。
- **字号断点细化**：新增 `320 / 375 / 414` 断点，标题/正文逐级缩放。
- **下载网格单列**：已有 `700px` 断点单列，补充 `400px` 细化（`.platform-grid` 单列、`.download-panels` 单列）。
- **汉堡菜单**：见方案二（移动端导航）。

---

## i18n 新增 key 清单（9 语言同步）

| key | 示例文案（zh） |
|-----|---------------|
| `hero_tagline` | Roblox 式叙事创作平台 |
| `nav_intro` | 游戏介绍 |
| `nav_download` | 资源下载 |
| `nav_community` | 社区分享 |
| `nav_menu_label` | 展开导航菜单（汉堡 aria-label） |

文件：`docs/data/locales/{zh,zh-TW,en,ja,ko,fr,de,pt,ru}.json`

## 不改

- 视觉基调：仍是玻璃拟态 + 青蓝，品牌色 `#3F48CC/#FDA1FF` 暂不迁移。
- 页面 HTML 语义结构与弹窗两阶段交互逻辑、数据格式。
- `docs/data/` 下版本/图片/主题配置 JSON 与 9 语言既有文案（只新增 key，不改旧 key）。
- 附属页页面结构（只换皮）。

## 验证

1. 每个改动 `.js` 文件 `node --check`。
2. DOM shim 测试：common.js → common-modal.js → main.js 加载进沙箱，跑弹窗全流程 + reveal 类名 + 汉堡菜单 toggle。
3. `python -m http.server` 浏览器逐页确认：index / history / community / LICENSE / 两个 THIRDPARTY 加载正常；导航吸顶与毛玻璃、锚点平滑滚动、汉堡菜单、滚动 reveal、弹窗过渡、页脚三栏与改前一致；图片兜底仍生效。
4. 9 语言 i18n：grep 确认新增 key 已同步，缺 key 时 `loadMessages` 回退到 zh 逻辑不受影响。
5. 系统开启"减少动态效果"验证动效降级；移动端视口（375/414）验证汉堡菜单、44px 触控、单列网格。

## 提交策略（按项目规范逐笔拆分）

每笔独立提交 = 改动文件 + 对应 CHANGELOG 条目（`develop/CHANGELOG.md`「网站」节），完成后 `temp/CLAUDE_MEMORY.md` 追加设计决策记录。

1. **Hero 标语 + i18n key**：index.html + index.css + 9 locale 文件 + CHANGELOG。
2. **吸顶导航 + 锚点 + 汉堡菜单**：index.html + index.css + main.js + 9 locale + CHANGELOG。
3. **卡片/按钮/字体/背景打磨**：common.css + index.css + CHANGELOG。
4. **弹窗打磨**：common.css + common-modal.js（如改）+ CHANGELOG。
5. **页脚 + 全站元信息**：index.html + index.css + 附属页 html/css + CHANGELOG。
6. **动效强化**：main.js + common.css + CHANGELOG。
7. **附属页换皮**：history/community/license 各 css + html + js（如改）+ CHANGELOG。
8. **响应式细化**：common.css + index.css + 附属页 css + CHANGELOG。

> CSS 文件跨多笔提交时按模块分段 `git add -p`，确保每笔只含该笔改动。
