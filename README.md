# 卡密工坊 (KamiStudio)

一款采用 iOS 液态玻璃设计风格的安卓卡密系统制作工具。

## 功能特性

- **卡密生成器**：自定义前缀、位数、数量、类型（日卡/周卡/月卡/季卡/年卡/永久卡/自定义）
- **卡密管理**：搜索、筛选、状态管理、批量导出（TXT/CSV）
- **APK卡密注入**：配置验证参数，自动生成 Smali 注入代码 + Manifest配置 + 注入指南 + 混淆规则
- **验证界面预览**：手机外框模拟，实时预览液态玻璃效果的卡密验证界面
- **防绕过安全防护**：签名校验、调试检测、模拟器检测、Root检测、强制验证、设备绑定
- **支持作者**：内置微信赞赏码
- **iOS液态玻璃UI**：全站毛玻璃透明质感、渐变背景、流畅动效

## 快速开始

### 方式一：GitHub Actions 自动打包（推荐，免费）

1. 将本项目推送到 GitHub 仓库
2. 进入仓库的 **Actions** 标签页
3. 点击 **Build APK** workflow → **Run workflow**
4. 等待构建完成（约3-5分钟）
5. 在运行结果的 **Artifacts** 中下载 APK

> 免费额度：公开仓库无限构建分钟数，无需绑信用卡。

**签名配置（可选）**：
在仓库 Settings → Secrets and variables → Actions 中添加：
- `SIGNING_KEY`：keystore文件的Base64编码（`base64 your.keystore`）
- `ALIAS`：密钥别名
- `KEY_STORE_PASSWORD`：keystore密码
- `KEY_PASSWORD`：密钥密码

不配置也能构建，会输出未签名APK，用NP Manager或其他工具签名即可安装。

### 方式二：本地编译

需要 Android Studio 或 Android SDK：
```bash
# 生成debug APK（可直接安装）
./gradlew assembleDebug

# 生成release APK（需要签名）
./gradlew assembleRelease
```

APK输出路径：`app/build/outputs/apk/`

## APK卡密注入使用流程

1. 在应用内「生成卡密」页面生成卡密
2. 在「APK注入」页面配置验证参数，点击「生成注入代码」
3. 导出生成的4个文件（Smali代码、Manifest配置、注入指南、混淆规则）
4. 使用 **NP Manager**（手机端免费）或 **apktool**（电脑端）按注入指南操作：
   - 将 `KamiVerifyActivity.smali` 放入反编译后的 `smali/com/hnl/kami/` 目录
   - 修改 `AndroidManifest.xml`，注册验证Activity并移除原启动页的LAUNCHER
   - 在原启动Activity的onCreate中添加跳转代码
   - 重新打包签名
5. 安装处理后的APK，启动时会先显示卡密验证界面

## 技术栈

- Kotlin + Jetpack Compose
- Room 数据库（本地存储卡密）
- Material 3
- Coil（图片加载）
- minSdk 26 (Android 8.0+)

## 项目结构

```
KamiStudio/
├── app/src/main/java/com/hnl/kamistudio/
│   ├── MainActivity.kt          # 应用入口，导航+底部导航栏
│   ├── KamiApp.kt               # Application类
│   ├── data/                    # Room数据库
│   │   ├── KamiDatabase.kt
│   │   ├── KamiDao.kt
│   │   └── KamiEntity.kt
│   ├── util/                    # 工具类
│   │   ├── KamiGenerator.kt     # 卡密生成器
│   │   ├── SmaliGenerator.kt    # Smali注入代码生成器
│   │   └── ExportHelper.kt      # 导出工具
│   └── ui/
│       ├── theme/               # 主题配色
│       ├── components/          # 液态玻璃组件
│       └── screens/             # 各页面
└── .github/workflows/build.yml  # GitHub Actions自动打包
```

## 注意事项

- 本工具仅用于对您拥有合法权限的APK进行卡密保护
- 加固后的APK（如360加固、腾讯乐固等）无法直接注入，需先脱壳
- Split APK（App Bundle格式）需要先合并为单APK
- 建议对注入后的smali代码进行混淆处理，增加逆向难度
