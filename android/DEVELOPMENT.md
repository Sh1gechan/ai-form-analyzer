# Sport Analyzer - Development Guide

## 🛠️ 開発環境セットアップ

### 必要な環境
- **Java**: OpenJDK 17 (Temurin推奨)
- **Android SDK**: API Level 34
- **Kotlin**: 1.9.10

### ビルド手順

#### Cursor/VS Code での開発
```bash
# Java 17を使用
export JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home

# クリーンビルド
./gradlew clean
./gradlew assembleDebug

# エミュレータにインストール
./gradlew installDebug
```

#### Android Studio での開発
1. **Gradle Sync**: `Tools > Sync Project with Gradle Files`
2. **Clean Project**: `Build > Clean Project`
3. **Rebuild Project**: `Build > Rebuild Project`

### よくある問題と解決方法

#### 1. アイコンエラー
```
Unresolved reference: Circle
Unresolved reference: Download
```
**解決**: Gradle Sync後にRebuild Project

#### 2. Java バージョンエラー
```
jlink with arguments ... error
```
**解決**: Java 17を使用していることを確認

#### 3. Hilt/DIエラー
```
error.NonExistentClass could not be resolved
```
**解決**: Clean Project → Rebuild Project

## 🏗️ アーキテクチャ

### 主要コンポーネント
- **UI**: Jetpack Compose
- **DI**: Hilt
- **API**: Retrofit2 + OkHttp
- **画像処理**: ML Kit Pose Detection
- **動画処理**: MediaMetadataRetriever
- **AI分析**: Gemini API

### データフロー
1. **動画選択** → 動画URI取得
2. **フレーム抽出** → 1FPSで最大30フレーム
3. **骨格推定** → ML Kitで人体骨格検出
4. **AI分析** → Gemini APIでフォーム分析
5. **結果表示** → 構造化された分析結果

## 🔑 設定

### Gemini API Key
1. [Google AI Studio](https://aistudio.google.com/)でAPIキーを取得
2. アプリの設定画面でAPIキーを入力
3. カスタムプロンプトも設定可能

## 🧪 テスト

### エミュレータ設定
- **推奨**: Pixel 7a API 34 (ARM64)
- **システムイメージ**: Google APIs ARM64

### テストファイル
- `final_ball.mov`: バスケットボールのテスト動画
- エミュレータの `/sdcard/Movies/` にコピー済み

## 📱 機能一覧

- ✅ 動画選択・撮影
- ✅ リアルタイム進捗表示
- ✅ ML Kit骨格推定
- ✅ Gemini AI分析
- ✅ 詳細サマリー表示
- ✅ 設定管理（API Key、プロンプト）

## 🚀 リリース準備

### Proguard設定 (本番用)
```gradle
buildTypes {
    release {
        isMinifyEnabled = true
        proguardFiles(...)
    }
}
```

### 署名設定
- KeystoreファイルとSign設定が必要
- Google Play Console での配布準備


