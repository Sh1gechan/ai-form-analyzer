# Forma — AI フォーム分析アプリ (Android)

> **このリポジトリについて**  
> [FarzaTV の viral デモ](https://x.com/FarzaTV/status/1928484483076087922)（Python + Gemini でバスケットボールのフォームを解析するスクリプト）にインスパイアされ、**Android アプリとして独自実装**したものです。Fork ではなく、オリジナルの Android プロジェクトです。

---

## 📱 概要

Gemini API と ML Kit Pose Detection を使って、動画から **スポーツのフォームを AI 分析**するAndroid アプリ。

| 機能 | 説明 |
|------|------|
| スタンダード分析 | 動画をそのまま Gemini に送信して素早くフィードバック |
| 骨格推定分析 | ML Kit で骨格検出した後、Gemini で精密なフォーム解析 |
| 履歴管理 | 過去の分析結果を保存・閲覧・削除 |
| カスタムプロンプト | 分析の視点をカスタマイズ可能 |

---

## 🗂️ リポジトリ情報

| 項目 | 内容 |
|------|------|
| リポジトリ名 | `Sh1gechan/gemini-bball` |
| URL | https://github.com/Sh1gechan/gemini-bball |
| Fork | **No**（オリジナルリポジトリ） |
| ブランチ | `main` |
| 主要言語 | Kotlin (Jetpack Compose) |

---

## 🛠️ 開発環境セットアップ

### 必要なもの

| ツール | バージョン |
|--------|-----------|
| Java | OpenJDK 17 (Temurin 推奨) |
| Android SDK | API Level 34 (Android 14) |
| Kotlin | 1.9.x |
| Gradle | 8.x |

### Java 17 の確認・設定

```bash
# バージョン確認
java -version
# "openjdk 17.x.x" と表示されれば OK

# Temurin 17 のパスを指定する場合（.zshrc / .bashrc に追記）
export JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home
```

### プロジェクトのクローン

```bash
git clone https://github.com/Sh1gechan/gemini-bball.git
cd gemini-bball/android
```

---

## 🚀 エミュレータでの実行方法

### 1. Android エミュレータを起動する

**Android Studio 経由（推奨）**

1. Android Studio を開く
2. `Tools > Device Manager` を開く
3. `Pixel 7a` (API 34 / ARM64) を選択して ▶ ボタンで起動
4. エミュレータが起動し、ホーム画面が表示されるまで待つ

**コマンドラインで起動する場合**

```bash
# 利用可能な AVD 一覧を確認
emulator -list-avds

# AVD を起動（例: Pixel_7a API 34）
emulator -avd Pixel_7a &
```

### 2. デバイス接続確認

```bash
# エミュレータが認識されているか確認
adb devices
# 例: emulator-5554   device  と表示されれば OK
```

### 3. ビルドしてインストール

```bash
cd /path/to/gemini-bball/android

# デバッグビルド + エミュレータにインストール（1コマンドで完結）
./gradlew installDebug

# ビルドのみ（APK生成）
./gradlew assembleDebug
# → android/app/build/outputs/apk/debug/app-debug.apk
```

### 4. アプリを起動

インストール後、エミュレータのアプリ一覧から `Forma` を選んで起動、または：

```bash
adb shell am start -n com.sportanalyzer.app/.MainActivity
```

---

## 🔑 Gemini API キーの設定

1. [Google AI Studio](https://aistudio.google.com/) にアクセスし、API キーを発行する
2. アプリ起動後、ホーム画面下部の **「APIキー・モデル設定」** をタップ
3. 取得したキーを入力して保存

> 無料枠で使用できます（RPM/TPD 制限あり）。  
> モデルは `gemini-2.5-flash`（デフォルト）が高速でおすすめです。

---

## 📂 プロジェクト構成

```
gemini-bball/
├── android/                        # Android アプリ本体
│   └── app/src/main/java/com/sportanalyzer/app/
│       ├── data/
│       │   ├── api/                # Gemini REST API (Retrofit)
│       │   └── repository/         # GeminiRepository / VideoAnalysisRepository
│       ├── di/                     # Hilt DI モジュール
│       ├── domain/model/           # AnalysisResult データモデル
│       └── ui/
│           ├── components/         # 共通 UI (MarkdownContent, SimpleNavBar)
│           ├── navigation/         # Navigation グラフ
│           ├── screens/            # 各画面 (Home / Analysis / Results / Summary / History / Settings / Camera)
│           ├── theme/              # カラー・テーマ定義
│           └── MainViewModel.kt    # アプリ全体の状態管理
├── README.md
└── ball.json / ball.py             # 元ネタの Python スクリプト（参考用）
```

---

## 🏗️ 技術スタック

| カテゴリ | ライブラリ |
|---------|-----------|
| UI | Jetpack Compose |
| DI | Hilt |
| ナビゲーション | Navigation Compose |
| HTTP | Retrofit2 + OkHttp |
| 骨格推定 | ML Kit Pose Detection |
| 動画再生 | Media3 (ExoPlayer) |
| AI 分析 | Gemini API (`gemini-2.5-flash` / `gemini-2.0-flash` 等) |

---

## 🧪 よくある問題

### `JAVA_HOME` が設定されていない

```bash
export JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home
./gradlew installDebug
```

### エミュレータが `adb devices` に表示されない

```bash
adb kill-server && adb start-server
adb devices
```

### Gemini API が `503` エラーになる

- モデルを `gemini-2.5-flash` に変更するとキャパシティが取りやすい
- 1〜2 分待ってリトライ

### `installDebug` が失敗する（デバイス未接続）

エミュレータが完全に起動する前に実行している可能性があります。  
ホーム画面が表示されてから再度実行してください。

---

## 📜 ライセンス

このプロジェクトは個人開発のプロトタイプです。  
元ネタ: [FarzaTV の viral デモ](https://x.com/FarzaTV/status/1928484483076087922)
