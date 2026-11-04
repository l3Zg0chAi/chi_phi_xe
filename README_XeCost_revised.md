# XeCost — Ứng dụng quản lý chi phí xe Android

> Tài liệu dành cho người **mới bắt đầu với Android**, đặc biệt là lập trình viên đã quen C/C++, CMake, Ninja/Make hoặc Yocto/BitBake.
>
> Mục tiêu: hiểu các thành phần của môi trường Android, tự thiết lập Windows, build APK, chạy trên điện thoại thật và phân biệt các loại kiểm thử. **Không cần chạy test trước mỗi lần mở ứng dụng.**

**Repository:** <https://github.com/l3Zg0chAi/chi_phi_xe>

> **Phạm vi phiên bản:** Các hướng dẫn dưới đây dựa trên cấu hình `app/build.gradle.kts` và quá trình thiết lập đã kiểm chứng ngày 04/11/2026: `compileSdk = 35`, `targetSdk = 35`, `minSdk = 26`, Gradle Wrapper **8.9**, Android Gradle Plugin **8.7.3**, JDK **21** đã chạy được. Nếu source thay đổi, ưu tiên cấu hình thực tế trong repository.
>
> **Lưu ý:** Đây là tài liệu phát triển và kiểm thử, không phải hướng dẫn phát hành APK lên Google Play.

## Mục lục

- [A. Bắt đầu nhanh: chỉ muốn chạy app](#a-bắt-đầu-nhanh-chỉ-muốn-chạy-app)
- [B. Kiến thức nền: Android dành cho người biết C++](#b-kiến-thức-nền-android-dành-cho-người-biết-c)
  - [B1. Android Studio, JDK, SDK, Gradle, AGP, ADB](#b1-android-studio-jdk-sdk-gradle-agp-adb)
  - [B2. So sánh Gradle với CMake và BitBake](#b2-so-sánh-gradle-với-cmake-và-bitbake)
  - [B3. Quy trình từ source code đến APK](#b3-quy-trình-từ-source-code-đến-apk)
  - [B4. Các khái niệm Android và thư viện của XeCost](#b4-các-khái-niệm-android-và-thư-viện-của-xecost)
- [C. Cấu trúc project và các file quan trọng](#c-cấu-trúc-project-và-các-file-quan-trọng)
- [D. Thiết lập môi trường Windows từ đầu](#d-thiết-lập-môi-trường-windows-từ-đầu)
  - [D1. Cài Android Studio và SDK API 35](#d1-cài-android-studio-và-sdk-api-35)
  - [D2. Kiểm tra/cấu hình Java](#d2-kiểm-tracấu-hình-java)
  - [D3. Kiểm tra/cấu hình ADB](#d3-kiểm-tracấu-hình-adb)
  - [D4. Kiểm tra/cấu hình sdkmanager](#d4-kiểm-tracấu-hình-sdkmanager)
  - [D5. Tạo Gradle Wrapper nếu project thiếu](#d5-tạo-gradle-wrapper-nếu-project-thiếu)
  - [D6. Mở project và Gradle Sync](#d6-mở-project-và-gradle-sync)
- [E. Build, cài và chạy app trên điện thoại](#e-build-cài-và-chạy-app-trên-điện-thoại)
- [F. Kiểm thử: Unit, Instrumented, UI và Manual](#f-kiểm-thử-unit-instrumented-ui-và-manual)
  - [F1. Khi nào cần test?](#f1-khi-nào-cần-test)
  - [F2. Unit Test](#f2-unit-test)
  - [F3. Instrumented Test trên Android thật](#f3-instrumented-test-trên-android-thật)
  - [F4. UI Test và Manual Test](#f4-ui-test-và-manual-test)
- [G. Xử lý các lỗi đã gặp](#g-xử-lý-các-lỗi-đã-gặp)
- [H. Lệnh thường dùng và quy trình phát triển hằng ngày](#h-lệnh-thường-dùng-và-quy-trình-phát-triển-hằng-ngày)
- [I. Thuật ngữ và tài liệu chính thức](#i-thuật-ngữ-và-tài-liệu-chính-thức)

---

# A. Bắt đầu nhanh: chỉ muốn chạy app

Nếu máy đã cài Android Studio, mở được XeCost và điện thoại đã nhận ADB:

1. Mở **Android Studio → Open** → chọn thư mục gốc của repo (ví dụ `D:\chi_phi_xe`).
2. Chờ **Gradle Sync** hoàn tất.
3. Kết nối điện thoại bằng USB; bật **USB debugging** và chấp nhận xác thực máy tính.
4. Trên thanh công cụ, chọn **thiết bị Android** và cấu hình chạy **`app`** (không chọn `FuelStatsTest`).
5. Nhấn **Run ▶**. Android Studio build/cài/khởi chạy ứng dụng trên điện thoại.

Kiểm tra ADB bằng PowerShell:

```powershell
adb devices
```

Phải có một dòng dạng:

```text
List of devices attached
<serial-cua-dien-thoai>    device
```

Có thể **bỏ qua Unit Test và Instrumented Test để chạy app trực tiếp**. Chỉ cần Gradle build thành công và điện thoại cho phép cài APK. Test tự động giúp phát hiện lỗi và chống lỗi tái xuất hiện, **không phải điều kiện tiên quyết** để chạy thử thủ công.

> Nếu app đã được cài, có thể rút cáp và mở XeCost từ biểu tượng trên điện thoại. Cần kết nối lại nếu muốn cài bản code mới hoặc debug qua ADB.

---

# B. Kiến thức nền: Android dành cho người biết C++

## B1. Android Studio, JDK, SDK, Gradle, AGP, ADB

Các công cụ có vai trò khác nhau, **không đồng nghĩa với nhau**:

| Thành phần | Vai trò | Liên tưởng khi làm C/C++ |
|---|---|---|
| **Android Studio** | IDE: sửa code, xem project, chạy và debug | CLion / Visual Studio |
| **JDK** | Bộ công cụ Java và JVM dùng để chạy Gradle, một phần toolchain | Toolchain và runtime trên host |
| **Android SDK** | API và công cụ để xây dựng app Android | Platform headers, libraries, toolchain |
| **SDK Platform 35** | API Android 15 dùng để **biên dịch** khi `compileSdk = 35` | Target platform API |
| **Build-Tools** | Các công cụ build Android như AAPT2, D8, zipalign, apksigner (tùy thành phần) | Binutils, linker, packager |
| **Gradle** | Build automation/task engine; tổ chức công việc và dependencies | Một phần vai trò CMake + Ninja/Make |
| **Android Gradle Plugin (AGP)** | Dạy Gradle cách build ứng dụng Android | Bộ quy tắc/toolchain chuyên biệt |
| **Gradle Wrapper** | Script + JAR để project gọi đúng phiên bản Gradle | Wrapper để ghim build tool |
| **ADB** | Kênh PC ↔ thiết bị: cài APK, chạy shell, xem log, debug | Công cụ quản trị target qua host |
| **Emulator** | Thiết bị Android ảo | Máy ảo/QEMU dùng để thử nghiệm |

**Tách rõ hai việc:**

- **Build APK:** Gradle + AGP + SDK, không bắt buộc phải có điện thoại.
- **Cài/chạy/debug APK trên điện thoại:** ADB, không cần Gradle nếu bạn đã có APK.

Ví dụ:

```powershell
# Build mà không cần kết nối điện thoại
.\gradlew.bat :app:assembleDebug

# Cài APK có sẵn lên thiết bị đã kết nối
adb install -r .\app\build\outputs\apk\debug\app-debug.apk
```

### `compileSdk`, `targetSdk`, `minSdk` là gì?

Trong `app/build.gradle.kts`:

```kotlin
android {
    namespace = "com.hailn.xecost"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.hailn.xecost"
        minSdk = 26
        targetSdk = 35
    }
}
```

- `compileSdk = 35`: sử dụng API Android 15 để **biên dịch**. Máy build phải có SDK Platform 35.
- `minSdk = 26`: thiết bị chạy app phải có Android API **26 trở lên**.
- `targetSdk = 35`: mức API ứng dụng tuyên bố đã nhắm tới; ảnh hưởng đến các quy định/hành vi tương thích Android.
- `applicationId`: ID ứng dụng được cài lên điện thoại; khác với tên hiển thị của app.

**Không nhầm `compileSdk = 35` với yêu cầu điện thoại phải là Android 15.** Điện thoại có thể chạy phiên bản Android khác miễn đáp ứng `minSdk` và tính tương thích của app.

## B2. So sánh Gradle với CMake và BitBake

Bạn đã quen quy trình:

```bash
cmake -S . -B build -G Ninja
cmake --build build
```

Trong C++, CMake thường **sinh build system**, Ninja/Make **thực thi** và GCC/Clang **biên dịch**.

Với Android:

```powershell
.\gradlew.bat :app:assembleDebug
```

Gradle có **task engine** riêng; AGP đăng ký các task Android, gọi Kotlin/Java compiler, công cụ xử lý tài nguyên, DEX, đóng gói và ký APK debug.

Với Yocto:

```bash
bitbake core-image-minimal
```

BitBake điều phối recipe/task để tạo package, rootfs và image cho toàn hệ thống Linux. BitBake thậm chí có thể gọi **CMake + Ninja** trong một recipe.

| Câu hỏi | CMake | Gradle | BitBake |
|---|---|---|---|
| Mô tả build ở đâu? | `CMakeLists.txt` | `build.gradle.kts` | `.bb`, `.bbclass`, metadata |
| Thực thi task ở đâu? | Thường do Ninja/Make | Task engine của Gradle | Task engine của BitBake |
| Gọi compiler thật? | Có, gián tiếp | Có, gián tiếp | Có, gián tiếp |
| Sản phẩm điển hình | `.exe`, `.so`, `.a` | `.apk`, `.aab`, `.jar` | Package/rootfs/image |
| Build theo dependencies? | Có | Có | Có |
| Incremental build/cache? | Có | Có | Có |

**Kết luận:** Gradle gần với CMake/BitBake ở tư tưởng *khai báo quy trình + dependency graph + tự động thực thi*. Nhưng chúng khác nhau về phạm vi và cách triển khai. Cả ba **không phải compiler**.

### `gradle` khác `gradlew.bat` thế nào?

- `gradle`: chạy bản Gradle được cài hoặc gọi trực tiếp ở một đường dẫn trên máy.
- `gradlew.bat`: script **trong repository** dùng Gradle Wrapper để tải/chạy đúng phiên bản ghi trong `gradle/wrapper/gradle-wrapper.properties`.
- `gradlew`: script tương đương trên Linux/macOS.
- `gradle-wrapper.jar`: mã bootstrap của Wrapper.

Ví dụ:

```properties
distributionUrl=https\://services.gradle.org/distributions/gradle-8.9-bin.zip
```

Khi chạy `.\gradlew.bat ...`, Wrapper sẽ dùng Gradle 8.9 (tải vào `%USERPROFILE%\.gradle\wrapper\dists` nếu chưa có). **Wrapper không chứa toàn bộ Gradle trong repository.**

Thông thường **nên commit cả 4 file Wrapper** (`gradlew`, `gradlew.bat`, `gradle-wrapper.jar`, `gradle-wrapper.properties`) để người khác clone về là chạy được. Nói rằng không nên commit `gradle-wrapper.jar` chỉ vì là file nhị phân là **không đúng với thực hành Gradle Wrapper thông thường**. Khi thêm Wrapper mới nên xác minh nguồn và tính toàn vẹn của JAR.

## B3. Quy trình từ source code đến APK

Sơ đồ rút gọn:

```text
Source Kotlin/Java + XML + ảnh + Manifest + dependencies
                       |
                       v
               Gradle + AGP tasks
                       |
        +--------------+--------------+
        |                             |
   Kotlin/Java compile          Xử lý resources
        |                             |
        +--------------+--------------+
                       |
                 DEX + packaging
                       |
                 APK debug được ký
                       |
                (ADB nếu muốn)
                       |
              Điện thoại Android
```

Chạy `:app:assembleDebug` chỉ **build APK**. Chạy cấu hình `app` bằng nút Run trong Android Studio còn có bước **cài và khởi chạy** ứng dụng trên thiết bị.

APK là gói cài đặt Android, **không phải file `.exe`**. Android chạy code ứng dụng qua Android Runtime (ART) và thực thi các thành phần do hệ điều hành quản lý.

### Gradle Sync có phải build không?

**Không.** Gradle Sync là bước IDE đọc cấu hình Gradle, resolve dependencies và nhận biết module/source sets/task để hỗ trợ code editor. Nó **không đồng nghĩa** ứng dụng đã được build thành công hay các test đã passed.

Thông báo như `Gradle sync finished` chỉ xác nhận việc **đồng bộ dự án**.

## B4. Các khái niệm Android và thư viện của XeCost

Các khái niệm quan trọng khi lần đầu đọc source Android:

| Thuật ngữ | Hiểu ngắn gọn |
|---|---|
| **Activity** | Thành phần Android thường đại diện cho điểm vào màn hình/giao diện |
| **AndroidManifest.xml** | Khai báo thành phần, quyền và thông tin ứng dụng |
| **Context** | Điểm truy cập tài nguyên và dịch vụ Android trong ngữ cảnh app |
| **Jetpack Compose** | Framework khai báo giao diện bằng Kotlin, UI cập nhật theo state |
| **Composable** | Hàm dựng phần giao diện trong Compose |
| **ViewModel** | Giữ/xử lý state và logic phục vụ UI, tách khỏi vòng đời UI |
| **Room** | Lớp truy cập database SQLite thông qua Entity, DAO, Database |
| **Hilt** | Dependency injection; giúp cung cấp dependency giữa các class |
| **KSP** | Kotlin Symbol Processing; sinh code khi build |
| **Coroutine / Flow** | Xử lý bất đồng bộ và luồng dữ liệu trong Kotlin |
| **WorkManager** | Lập lịch công việc nền có yêu cầu đáng tin cậy |
| **DataStore** | Lưu dữ liệu cấu hình/preferences hoặc dữ liệu có cấu trúc |

Trong `app/build.gradle.kts` được cung cấp có dependencies cho Compose, Room, Hilt, KSP, Coroutines, WorkManager và DataStore. **Việc có dependency không chứng minh mọi tính năng đó đã được dùng ở mọi màn hình**; phải đọc code cụ thể mới kết luận được.

Một luồng xử lý thường gặp (mô hình tham khảo, không khẳng định 100% code XeCost hiện tại):

```text
Người dùng bấm "Lưu" trong Compose UI
               |
               v
         Event / ViewModel
               |
               v
       Logic nghiệp vụ / repository
               |
               v
             Room DAO
               |
               v
           SQLite database
               |
               v
       Flow / State cập nhật UI
```

Khác với ứng dụng C++ thông thường có `main()` và `while` tự điều phối, app Android do hệ điều hành quản lý **lifecycle** của Activity/process. Tìm hiểu `MainActivity`, `onCreate`, `ViewModel`, `remember`, `State` sẽ giúp bạn đọc code Android nhanh hơn.

---

# C. Cấu trúc project và các file quan trọng

Sơ đồ theo quy ước Android/Gradle và những thư mục đã thao tác trong quá trình thiết lập:

```text
chi_phi_xe/
├── app/
│   ├── src/
│   │   ├── main/                     # Code/tài nguyên ứng dụng thật
│   │   │   ├── java/                 # Kotlin/Java (có thể là java/ dù đuôi .kt)
│   │   │   ├── res/
│   │   │   └── AndroidManifest.xml
│   │   ├── test/                     # Unit Test chạy trên JVM máy tính
│   │   └── androidTest/              # Test chạy trên Android thật/emulator
│   │       └── java/com/hailn/xecost/
│   │           └── ExampleInstrumentedTest.kt
│   └── build.gradle.kts             # Cấu hình module app
├── gradle/
│   ├── libs.versions.toml           # Version catalog (nếu project dùng)
│   └── wrapper/
│       ├── gradle-wrapper.jar
│       └── gradle-wrapper.properties
├── gradlew                          # Wrapper Linux/macOS
├── gradlew.bat                      # Wrapper Windows
├── settings.gradle.kts              # Khai báo module/repositories
└── build.gradle.kts                 # Cấu hình chung
```

> Sơ đồ trên mô tả những đường dẫn quan trọng, không phải khẳng định repository chỉ có các file này.

### Vì sao Android Studio hiển thị cây thư mục khác?

Góc trên trái Project tool window có menu chọn **Android** / **Project**:

- **Android view:** gom `AndroidManifest.xml` thành `manifests`, nguồn thành `kotlin+java`, tài nguyên thành `res`, hiển thị các nguồn `(test)`/`(androidTest)`.
- **Project view:** hiển thị cấu trúc file/thư mục gần với File Explorer.

Nếu sau Gradle Sync bạn thấy source "biến thành" `manifests`, `kotlin+java`, `Gradle Scripts`, đó **chỉ là cách hiển thị**, không phải code đã bị sửa.

### Vai trò của các file Gradle

- `settings.gradle.kts`: xác định module tham gia (ví dụ `:app`).
- `build.gradle.kts` tại root: cấu hình plugin/cấu hình chung.
- `app/build.gradle.kts`: plugin Android/Kotlin, `compileSdk`, `minSdk`, dependencies, test runner, build types.
- `gradle/libs.versions.toml`: tập trung version và alias thư viện/plugin.
- `gradle-wrapper.properties`: ghim bản Gradle (XeCost: **8.9**).

---

# D. Thiết lập môi trường Windows từ đầu

> Hướng dẫn dùng **Windows PowerShell**. Mở bằng `Win + X → Terminal/PowerShell`, hoặc tìm "PowerShell" trong Start. Các lệnh kiểm tra có thể chạy từ bất kỳ thư mục nào; lệnh `.\gradlew.bat` phải chạy tại **root project**.
>
> **Nguyên tắc xử lý:** Làm một bước → kiểm tra đầu ra → chỉ sang bước kế tiếp khi bước trước đã đúng. Đừng vội cài lại công cụ khi chỉ thiếu `PATH`.

## D1. Cài Android Studio và SDK API 35

Cài Android Studio từ <https://developer.android.com/studio>. Khi mở lần đầu, làm theo Setup Wizard.

Mở **Tools → SDK Manager** (ở màn hình welcome có thể vào **More Actions → SDK Manager**).

**Tab SDK Platforms:**

- Kiểm tra **Android 15 (API Level 35)** đã cài. Đây là SDK Platform cần cho `compileSdk = 35`.
- Không bắt buộc cài mọi bản Android SDK cao hơn.
- Cài emulator/system image nếu muốn giả lập; **không bắt buộc** nếu dùng điện thoại thật.

**Tab SDK Tools:**

- **Android SDK Platform-Tools** — chứa `adb`.
- **Android SDK Command-line Tools (latest)** — chứa `sdkmanager`/các CLI.
- **Android SDK Build-Tools** — phiên bản do AGP sử dụng.
- **Android Emulator** — chỉ cần nếu chạy emulator.

Nhấn **Apply → OK** để cài các gói đã chọn.

**Quan trọng về Build-Tools 35:** Trong phiên thiết lập này máy có Build-Tools **34.0.0 và 36.0.0**, vẫn **Gradle Sync, build và chạy test thành công**. Vì vậy **không nên kết luận Build-Tools 35.0.0 luôn là điều kiện bắt buộc** chỉ vì `compileSdk = 35`; AGP chọn công cụ theo cấu hình/khả năng hỗ trợ. Chỉ cài thêm đúng phiên bản nếu cấu hình project, tài liệu yêu cầu cứng hoặc thông báo build báo thiếu.

Thư mục SDK mặc định trên máy Windows thường là:

```text
C:\Users\<user>\AppData\Local\Android\Sdk
```

Để xem đường dẫn thực tế: **Tools → SDK Manager → Android SDK Location**.

## D2. Kiểm tra/cấu hình Java

Kiểm tra:

```powershell
java -version
```

Nếu nhận `java : The term 'java' is not recognized...`, hãy **kiểm tra bản JDK/JBR có sẵn** trước, không vội tải bản khác:

```powershell
Test-Path "C:\Program Files\Android\Android Studio\jbr\bin\java.exe"
```

Nếu `True`:

```powershell
& "C:\Program Files\Android\Android Studio\jbr\bin\java.exe" -version
```

Để JDK được sử dụng bởi Gradle, `JAVA_HOME` cần trỏ đến **thư mục JDK**, **không phải** `bin` hay `java.exe`.

Ở máy đã thiết lập, Android Studio cung cấp JBR 21 tại `C:\Users\Admin\.jdks\jbr-21.0.11`. Trong **cửa sổ PowerShell hiện tại**:

```powershell
$env:JAVA_HOME = "C:\Users\Admin\.jdks\jbr-21.0.11"
& "$env:JAVA_HOME\bin\java.exe" -version
```

Nếu đúng sẽ báo Java **21.0.11**. Đường dẫn ví dụ là đường dẫn đã xác minh trên một máy; **máy khác cần dùng đường dẫn JDK thực tế**.

**Vì sao dùng JDK 21 thay vì 25?** Gradle **8.9 chưa hỗ trợ chính thức chạy trên Java 25**. Trong quá trình thiết lập, `java -version` từng trả Java 25.0.4 nhưng khi dùng JDK 21, Gradle 8.9 hoạt động ổn định. AGP 8.7.x yêu cầu JDK tối thiểu 17; JDK 21 là lựa chọn phù hợp cho cấu hình này.

Để dùng JDK 21 cho Android Studio: **File → Settings → Build, Execution, Deployment → Build Tools → Gradle → Gradle JDK**. Chọn JDK phù hợp.

**Không cần đổi toàn bộ Java của Windows** nếu chỉ muốn thử trong một PowerShell: gán `$env:JAVA_HOME` chỉ ảnh hưởng phiên hiện tại.

Nếu **muốn lưu `JAVA_HOME` theo user** (chỉ sau khi đã xác minh đường dẫn):

```powershell
[Environment]::SetEnvironmentVariable(
    "JAVA_HOME",
    "C:\Users\Admin\.jdks\jbr-21.0.11",
    "User"
)
```

Đóng/mở lại terminal để biến môi trường được nhận. Tránh thêm nhiều bản `java.exe` khác nhau vào PATH không có chủ đích.

## D3. Kiểm tra/cấu hình ADB

Kiểm tra:

```powershell
adb --version
```

Nếu không nhận, kiểm tra `adb.exe` trong SDK:

```powershell
Test-Path "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
```

Nếu `True`, gọi trực tiếp:

```powershell
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" --version
```

Nếu chạy được nhưng `adb` không nhận từ mọi thư mục, thêm `platform-tools` vào **User PATH** (tránh thêm trùng):

```powershell
$adbPath = "$env:LOCALAPPDATA\Android\Sdk\platform-tools"
$userPath = [Environment]::GetEnvironmentVariable("Path", "User")
$entries = $userPath -split ';'
if ($adbPath -notin $entries) {
    [Environment]::SetEnvironmentVariable("Path", "$userPath;$adbPath", "User")
}
```

Đóng PowerShell, mở lại và chạy `adb --version`.

## D4. Kiểm tra/cấu hình sdkmanager

Kiểm tra đường dẫn:

```powershell
Test-Path "$env:LOCALAPPDATA\Android\Sdk\cmdline-tools\latest\bin\sdkmanager.bat"
```

- `False`: vào **Android Studio → Tools → SDK Manager → SDK Tools** và cài **Android SDK Command-line Tools (latest)**. Sau đó kiểm tra lại.
- `True`: thử gọi bằng đường dẫn đầy đủ:

```powershell
& "$env:LOCALAPPDATA\Android\Sdk\cmdline-tools\latest\bin\sdkmanager.bat" --list_installed
```

Nếu gọi trực tiếp được nhưng lệnh ngắn `sdkmanager` chưa nhận, thêm vào User PATH (tránh thêm trùng):

```powershell
$sdkManagerPath = "$env:LOCALAPPDATA\Android\Sdk\cmdline-tools\latest\bin"
$userPath = [Environment]::GetEnvironmentVariable("Path", "User")
if ($sdkManagerPath -notin ($userPath -split ';')) {
    [Environment]::SetEnvironmentVariable("Path", "$userPath;$sdkManagerPath", "User")
}
```

Đóng/mở lại PowerShell, kiểm tra:

```powershell
sdkmanager --list_installed
```

**Thông báo đã gặp:**

```text
WARNING: The SDK Manager CLI tool (sdkmanager) is deprecated.
Android CLI will be used instead.
Downloading Android CLI...
```

Đây là thông báo chuyển đổi công cụ CLI, **không tự động có nghĩa cài SDK thất bại**. Trong lần thiết lập này, sau khi CLI tải xong, lệnh vẫn in được danh sách packages.

Cần thấy ít nhất các gói liên quan như:

```text
platforms/android-35
platform-tools
cmdline-tools/latest
```

Tên gói trong bản CLI mới có thể dùng dấu `/` thay vì `;`. Dòng `platforms/android-35` tương ứng Android SDK Platform API 35.

## D5. Tạo Gradle Wrapper nếu project thiếu

**Trước hết kiểm tra, không tạo lại nếu đã có:**

```powershell
cd D:\chi_phi_xe
Get-Item .\gradlew, .\gradlew.bat, .\gradle\wrapper\gradle-wrapper.jar, .\gradle\wrapper\gradle-wrapper.properties
```

Nếu **đủ 4 file**, bỏ qua phần bootstrap, dùng trực tiếp:

```powershell
.\gradlew.bat --version
```

### Trường hợp project skeleton thiếu Wrapper

Lúc thiết lập ban đầu, `gradlew`/`gradlew.bat`/`gradle-wrapper.jar` chưa có. Thay vì tải Gradle mới, đã tìm được bản **8.9** do Android Studio tải vào Gradle cache:

```powershell
Get-ChildItem "$env:USERPROFILE\.gradle\wrapper\dists" `
    -Recurse -Filter gradle.bat -ErrorAction SilentlyContinue |
    Select-Object -ExpandProperty FullName
```

Ví dụ kết quả thực tế:

```text
C:\Users\Admin\.gradle\wrapper\dists\gradle-8.9-bin\90cnw93cvbtalezasaz0blq0a\gradle-8.9\bin\gradle.bat
```

**Chỉ sau khi xác nhận file đó tồn tại**, tại root `D:\chi_phi_xe`, với `JAVA_HOME` đã trỏ JDK 21:

```powershell
& "C:\Users\Admin\.gradle\wrapper\dists\gradle-8.9-bin\90cnw93cvbtalezasaz0blq0a\gradle-8.9\bin\gradle.bat" --version
```

Nếu thấy `Gradle 8.9`, tạo Wrapper:

```powershell
& "C:\Users\Admin\.gradle\wrapper\dists\gradle-8.9-bin\90cnw93cvbtalezasaz0blq0a\gradle-8.9\bin\gradle.bat" wrapper --gradle-version 8.9
```

Kết quả đã ghi nhận: `BUILD SUCCESSFUL in 7s`. Kiểm tra các file:

```powershell
Get-Item .\gradlew, .\gradlew.bat, `
    .\gradle\wrapper\gradle-wrapper.jar, `
    .\gradle\wrapper\gradle-wrapper.properties |
    Select-Object Name, Length
```

Một lần kiểm tra thực tế:

```text
gradlew                     8762
gradlew.bat                 2966
gradle-wrapper.jar         43504
gradle-wrapper.properties    250
```

Cuối cùng:

```powershell
.\gradlew.bat --version
```

Đã xác minh: **Gradle 8.9**, Launcher JVM **21.0.11**, Daemon JVM **JDK 21**.

> **Lưu ý:** Đường dẫn cache chứa một chuỗi định danh có thể khác trên từng máy. Không sao chép nguyên xi nếu `Get-ChildItem` báo đường dẫn khác. Khi Wrapper được commit đầy đủ, người mới clone repo **không cần làm phần D5**.

## D6. Mở project và Gradle Sync

Trong Android Studio:

1. **File → Open** → chọn root `D:\chi_phi_xe` (hoặc thư mục đã clone).
2. Khi hỏi Trust Project, chỉ đồng ý nếu tin cậy source.
3. Chọn **File → Sync Project with Gradle Files**.
4. Xác nhận có thông báo **`Gradle sync finished`**.

Không thấy nút **Sync Now**? Không sao. Menu **File → Sync Project with Gradle Files** thực hiện tương đương.

Nếu xuất hiện:

- **Project update recommended / AGP Upgrade Assistant:** không nên nâng AGP ngay chỉ để chạy thử. Phiên bản hiện tại phải tương thích với Gradle và Kotlin.
- **Migrate to Gradle Daemon toolchain:** đây là đề xuất tùy chọn; có thể bỏ qua khi đang kiểm tra môi trường.
- **Multiple Gradle daemons might be spawned:** Android Studio Gradle JDK và `JAVA_HOME` đang khác đường dẫn. Đây là **cảnh báo hiệu năng/tài nguyên**, không tự động là lỗi build; cân nhắc đồng bộ JDK khi cần.
- **Cảnh báo màu vàng trong build.gradle.kts:** không phải cứ xuất hiện là Sync thất bại. Xem thông báo Build thực tế để biết có lỗi hay không.

---

# E. Build, cài và chạy app trên điện thoại

## E1. Kết nối điện thoại thật bằng ADB

Trên Android:

1. **Cài đặt → Giới thiệu điện thoại** → chạm **Số bản dựng** (hoặc mục tương ứng của hãng) nhiều lần để bật Developer options.
2. Vào **Tùy chọn nhà phát triển** → bật **USB debugging / Gỡ lỗi USB**.
3. Cắm **cáp truyền dữ liệu**, mở khóa màn hình.
4. Chấp nhận hộp thoại tin cậy máy tính, nếu xuất hiện.

Trên PC:

```powershell
adb devices
```

**Giải thích kết quả:**

| Kết quả | Ý nghĩa |
|---|---|
| Chỉ `List of devices attached` | ADB đang chạy nhưng **chưa thấy thiết bị** |
| `<serial> unauthorized` | Điện thoại chưa xác nhận cấp quyền USB debugging |
| `<serial> device` | ADB nhận thiết bị và đã xác thực |
| `<serial> offline` | Thiết bị/kết nối ADB chưa sẵn sàng |

Nếu chưa nhận, có thể thử:

```powershell
adb kill-server
adb start-server
adb devices
```

Rồi kiểm tra cáp, USB mode (File Transfer), cổng USB và driver nếu cần.

### POCO/Xiaomi: lỗi chặn cài APK qua USB

Trên một số máy POCO/Xiaomi, dù `adb devices` đã hiện `device`, việc **cài** APK vẫn có thể bị chặn.

Tìm trong **Tùy chọn nhà phát triển → Cài đặt qua USB (Install via USB)**, bật khi bạn thực sự cần cài ứng dụng debug qua USB, đọc kỹ thông báo cấp quyền.

**Chỉ bật trên thiết bị của bạn và khi tin cậy máy tính kết nối.** Sau khi kiểm thử xong, có thể tắt lại các quyền debugging/cài qua USB nếu không cần.

Trong phiên thiết lập đã gặp:

```text
INSTALL_FAILED_USER_RESTRICTED
Install canceled by user
```

Nguyên nhân thực tế: trên POCO, tùy chọn **Install via USB** đang tắt. Sau khi bật, `connectedDebugAndroidTest` đã chạy thành công.

## E2. Chạy từ Android Studio

Trên thanh công cụ Android Studio:

1. Chọn thiết bị POCO/Android đã kết nối.
2. Tại **Run configuration**, chọn **`app`**, không chọn `FuelStatsTest`.
3. Nhấn **Run ▶**.

Android Studio sẽ build APK nếu cần → cài qua ADB → khởi chạy Activity của ứng dụng.

Nếu thấy `Not applicable for the "FuelStatsTest" configuration`, thường bạn đang chạy nhầm cấu hình Unit Test. Chuyển sang **`app`** rồi thử lại.

## E3. Build và cài bằng PowerShell

Mở PowerShell tại root project:

```powershell
cd D:\chi_phi_xe
```

Build Debug APK:

```powershell
.\gradlew.bat :app:assembleDebug
```

Nếu `BUILD SUCCESSFUL`, file APK thường ở:

```text
app\build\outputs\apk\debug\app-debug.apk
```

Cài APK:

```powershell
adb install -r .\app\build\outputs\apk\debug\app-debug.apk
```

Sau đó mở ứng dụng bằng biểu tượng trên điện thoại. Có thể yêu cầu Android khởi chạy qua launcher:

```powershell
adb shell monkey -p com.hailn.xecost -c android.intent.category.LAUNCHER 1
```

`-r` yêu cầu cài lại/cập nhật ứng dụng có sẵn; việc giữ dữ liệu phụ thuộc chữ ký, ID ứng dụng và tình trạng cài đặt. **Không gỡ cài đặt hoặc xóa dữ liệu nếu muốn giữ dữ liệu thử nghiệm.**

## E4. Debug và xem log

Khi app crash hoặc chạy khác kỳ vọng, dùng **Logcat** trong Android Studio hoặc:

```powershell
adb logcat
```

Logcat có nhiều log hệ thống. Trong Android Studio có thể lọc theo app/process và mức log để dễ tìm exception.

Khi cần shell trên điện thoại:

```powershell
adb shell
```

Thoát shell:

```text
exit
```

ADB **không biên dịch source code**; nó là kênh giao tiếp/điều khiển thiết bị.

---

# F. Kiểm thử: Unit, Instrumented, UI và Manual

## F1. Khi nào cần test?

**Có thể chạy app trực tiếp mà không cần chạy test trước.** Kiểm thử tự động giúp phát hiện lỗi sớm và giảm công kiểm tra lại sau mỗi lần sửa code.

| Loại kiểm thử | Chạy ở đâu? | Kiểm tra gì? | Cần điện thoại? |
|---|---|---|---|
| **Unit Test (local)** | JVM trên PC | Thuật toán, logic từng hàm/class | Không |
| **Instrumented Test** | Android thật/emulator | Code dùng Android framework, Context, database, UI... | Có |
| **UI Test tự động** | Thường Android thật/emulator | Mở màn hình, bấm, nhập, xác nhận UI | Thường có |
| **Manual Test** | Người dùng trực tiếp trên app | Trải nghiệm và các luồng sử dụng thực tế | Có thiết bị chạy app |

**Ví dụ với XeCost:** Unit Test kiểm tra cách tính nhiên liệu; Instrumented Test kiểm tra Android Context hoặc Room; UI Test điều khiển giao diện; Manual Test là người dùng tự nhập ODO, số lít, lưu và xem kết quả.

Một test **Passed** chỉ chứng minh tình huống được test đó bao phủ đã đạt, không chứng minh toàn bộ ứng dụng không có bug.

## F2. Unit Test

Source set:

```text
app/src/test/
```

File đã thao tác: `FuelStatsTest.kt` thuộc package `com.hailn.xecost.domain`.

**Chạy bằng Android Studio:** mở file `FuelStatsTest.kt` → bấm biểu tượng ▶ bên cạnh class để chạy cả class, hoặc cạnh một hàm `@Test` để chạy đúng hàm đó. Xem kết quả trong cửa sổ **Run**.

**Chạy bằng PowerShell tại root project** (`D:\chi_phi_xe`):

```powershell
# Toàn bộ local unit test
.\gradlew.bat test

# Chỉ biến thể debug
.\gradlew.bat :app:testDebugUnitTest

# Chỉ một class
.\gradlew.bat :app:testDebugUnitTest --tests "com.hailn.xecost.domain.FuelStatsTest"

# Một test có tên chứa khoảng trắng (sử dụng wildcard)
.\gradlew.bat :app:testDebugUnitTest --tests "*odo trung nhau khong lam mat so lit cua doan sau*"

# Ép chạy lại các task, kể cả khi Gradle báo UP-TO-DATE
.\gradlew.bat :app:testDebugUnitTest --rerun-tasks
```

Báo cáo thường ở:

```text
app/build/reports/tests/testDebugUnitTest/index.html
```

**Hiểu `UP-TO-DATE`:** Gradle thấy input/output không đổi nên không chạy lại task. Không phải lỗi. Lệnh `--rerun-tasks` ép task chạy lại.

**Kết quả đã xác nhận trong phiên thiết lập:** 27 Unit Test đã Passed. Con số này chỉ là trạng thái tại thời điểm kiểm tra, có thể thay đổi khi thêm test.

## F3. Instrumented Test trên Android thật

Source set:

```text
app/src/androidTest/java/com/hailn/xecost/
```

**Phân biệt với `app/src/test`:** Instrumented Test được đóng gói thành **test APK** và chạy bằng Android test runner trên thiết bị.

Trong `app/build.gradle.kts`:

```kotlin
android {
    defaultConfig {
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
}

dependencies {
    // Ví dụ các dependency dùng trong quá trình thiết lập:
    androidTestImplementation("androidx.test:runner:1.7.0")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test:core:1.7.0")
}
```

Nếu repository đã có các dòng trên **không thêm trùng**. Ưu tiên thống nhất version bằng `libs.versions.toml` khi bảo trì lâu dài.

### Tạo source set nếu chưa có

Trong Android Studio, chọn **Project view**, vào `app/src`, tạo `androidTest/java`. Tại `java` chọn **New → Package** và nhập:

```text
com.hailn.xecost
```

Tạo file Kotlin `ExampleInstrumentedTest.kt` trong package đó.

Ví dụ test đã dùng:

```kotlin
package com.hailn.xecost

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {

    @Test
    fun checkApplicationPackage() {
        val context =
            InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.hailn.xecost", context.packageName)
    }

    @Test
    fun checkApplicationStorage() {
        val context =
            InstrumentationRegistry.getInstrumentation().targetContext
        assertTrue(context.filesDir.exists())
    }
}
```

Hai test này kiểm tra **package ID** và **khả năng truy cập thư mục riêng của app**. Chúng **không mở giao diện** `MainActivity`, không nhập ODO, không kiểm tra tính toán nghiệp vụ.

**Chạy trên điện thoại đã kết nối:**

```powershell
adb devices
.\gradlew.bat :app:connectedDebugAndroidTest
```

Hoặc mở `ExampleInstrumentedTest.kt`, bấm ▶ cạnh class trong Android Studio.

Gradle sẽ:

```text
Build app APK
    -> build test APK
    -> ADB cài hai APK lên thiết bị
    -> test runner thực thi test trên Android
    -> lấy kết quả về PC
```

Kết quả đã xác minh:

```text
Starting 2 tests on <device>
Finished 2 tests on <device>
BUILD SUCCESSFUL in 38s
```

Báo cáo HTML:

```text
app/build/reports/androidTests/connected/debug/index.html
```

Đã xác nhận **2 tests Passed** trong báo cáo sau khi cho phép cài APK qua USB trên POCO.

**Lưu ý:** Trước đó từng có báo cáo `0 tests`, `INSTALL_FAILED_USER_RESTRICTED`. Đó là lỗi **cài app lên điện thoại**, không phải test logic Failed.

## F4. UI Test và Manual Test

### Manual Test — chạy và sử dụng như người dùng

Chọn cấu hình `app`, Run ▶ trong Android Studio. Tự thử:

- Mở các màn hình, chuyển tab, điều hướng.
- Nhập ODO, số lít nhiên liệu và số tiền; kiểm tra tính toán.
- Thử dữ liệu rỗng/không hợp lệ, ODO trùng, giá trị biên.
- Lưu dữ liệu, tắt và mở lại ứng dụng; xác nhận dữ liệu còn.
- Thử thao tác khi xoay màn hình, app vào background, mất mạng (nếu tính năng cần mạng).

Đây là cách nhanh nhất để kiểm tra **hành vi người dùng thực tế**.

### UI Test tự động với Jetpack Compose

Nếu muốn tự động bấm nút, nhập dữ liệu, kiểm tra text, hãy dùng **Compose UI Test** (ví dụ thư viện `androidx.compose.ui:ui-test-junit4` và các cấu hình liên quan).

Mô hình chung:

```text
Khởi chạy Activity
    -> tìm thành phần bằng text/testTag/contentDescription
    -> performClick / performTextInput
    -> assertIsDisplayed / assertTextEquals
```

Cần đọc UI thực tế của XeCost và định danh thành phần trước khi viết bài test thật; **không nên sao chép một test giả định tên button chưa tồn tại**. Đây là bước nâng cao, khác với hai test `ExampleInstrumentedTest` đơn giản.

---

# G. Xử lý các lỗi đã gặp

| Hiện tượng | Nguyên nhân có thể | Kiểm tra/giải pháp |
|---|---|---|
| `java is not recognized` | JDK chưa có hoặc thiếu PATH | Kiểm tra `Android Studio\jbr\bin\java.exe`, `JAVA_HOME`, PATH |
| `adb is not recognized` | `platform-tools` chưa vào PATH | `Test-Path "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"` |
| `sdkmanager is not recognized` | Chưa có Command-line Tools hoặc thiếu PATH | Cài Command-line Tools, thêm `cmdline-tools\latest\bin` vào PATH |
| `sdkmanager is deprecated` | CLI mới thay thế lệnh cũ | Không phải lỗi nếu vẫn liệt kê được packages |
| `gradle is not recognized` | Không cài Gradle độc lập/không trong PATH | Nếu đã có Wrapper, **dùng `.\gradlew.bat`**, không cần `gradle` |
| `.\gradlew.bat is not recognized` | Thiếu file Wrapper ở root hoặc đứng sai thư mục | `cd D:\chi_phi_xe`; kiểm tra `Get-Item .\gradlew.bat` |
| Gradle 8.9 chạy bằng Java 25 | JDK chạy Gradle chưa phù hợp | Chuyển `JAVA_HOME`/Gradle JDK sang JDK 21 phù hợp |
| `Gradle sync finished` nhưng chưa thấy APK | Sync không đồng nghĩa Build | Chạy `:app:assembleDebug` hoặc Run `app` |
| Source "biến mất" sau Sync | Android view gom thư mục | Chuyển sang **Project** view |
| `adb devices` chỉ có tiêu đề | Điện thoại chưa được nhận | Cáp dữ liệu, USB debugging, driver/cổng USB |
| `unauthorized` | Chưa chấp nhận khóa USB debugging | Mở khóa điện thoại và cho phép kết nối |
| `INSTALL_FAILED_USER_RESTRICTED` | Thiết bị chặn cài APK qua USB | POCO: kiểm tra **Install via USB**, quyền/cảnh báo của máy |
| Báo cáo Instrumented `0 tests` | Có thể test APK chưa cài/chưa chạy | Xem mục **Tool failures** trong HTML report |
| `Not applicable for FuelStatsTest` | Chọn nhầm Run configuration | Chọn **`app`** trước khi Run ▶ |
| `Multiple Gradle daemons...` | Gradle JDK và `JAVA_HOME` khác nhau | Đồng bộ JDK nếu muốn tối ưu, không đồng nghĩa build fail |
| `UP-TO-DATE` | Gradle thấy task không đổi | Bình thường; `--rerun-tasks` nếu thật sự muốn chạy lại |
| Build-Tools 35 không có nhưng SDK 35 đã có | Hai loại package khác nhau | Chỉ cài Build-Tools cụ thể nếu cấu hình/build yêu cầu; kiểm tra kết quả build |

### Cách tự xác định lỗi nhanh

1. **Lệnh không nhận (`not recognized`)** → kiểm tra file executable thật và `PATH`.
2. **Sync thất bại** → xem cửa sổ Build/Gradle, compatibility JDK/AGP/Gradle, dependency download.
3. **Build thất bại** → đọc lỗi compiler/AGP/task đầu tiên.
4. **ADB không thấy thiết bị** → kiểm tra USB/cấp quyền.
5. **ADB thấy `device` nhưng không cài được** → kiểm tra quyền cài qua USB/chính sách thiết bị.
6. **App mở nhưng crash/logic sai** → Logcat, Unit Test, Instrumented/UI Test theo loại lỗi.

Đừng kết luận chỉ từ thông báo màu vàng. **Log/task và kết quả `BUILD SUCCESSFUL` hoặc `BUILD FAILED` mới là tín hiệu chính.**

---

# H. Lệnh thường dùng và quy trình phát triển hằng ngày

## H1. Cheat sheet PowerShell (Windows)

Tại thư mục root project:

```powershell
cd D:\chi_phi_xe

# Kiểm tra môi trường
java -version
adb --version
sdkmanager --list_installed
.\gradlew.bat --version

# Kiểm tra thiết bị
adb devices

# Build app (không cần điện thoại)
.\gradlew.bat :app:assembleDebug

# Cài APK đã build
adb install -r .\app\build\outputs\apk\debug\app-debug.apk

# Unit Test trên PC
.\gradlew.bat :app:testDebugUnitTest

# Instrumented Test trên Android thật / emulator
.\gradlew.bat :app:connectedDebugAndroidTest

# Xem log Android
adb logcat
```

**Linux/macOS:** thay `.\gradlew.bat` bằng `./gradlew`. `adb` và `sdkmanager` có thể có sẵn nếu đường dẫn SDK đã nằm trong PATH.

## H2. Khi chỉ muốn sửa code rồi chạy thử

```text
Sửa source Kotlin
       |
       v
Android Studio: chọn cấu hình app + điện thoại
       |
       v
Run ▶
       |
       v
Ứng dụng chạy trên điện thoại
       |
       v
Test bằng tay và quan sát Logcat
```

Không phải chạy Unit Test và Instrumented Test trước mỗi lần Run. Khi sửa logic phức tạp, nên chạy lại test liên quan; trước phát hành nên chạy đủ test.

## H3. Khi muốn bảo đảm chất lượng

```text
Sửa code
  -> Unit Test (logic)
  -> Instrumented Test (Android framework)
  -> UI Test (nếu có)
  -> Manual Test (trải nghiệm thực)
  -> Build release, kiểm tra ký và phát hành riêng
```

**`assembleDebug` tạo APK debug**. APK release, chữ ký phát hành, ProGuard/R8, Play Store và phân phối ra công chúng là quy trình khác; không mặc định dùng bản debug để phát hành.

## H4. Những kết quả đã xác minh trong buổi thiết lập

Đây là **ghi nhận kết quả ở một máy dev**, không phải lời hứa cho tất cả môi trường:

| Hạng mục | Kết quả đã kiểm chứng |
|---|---|
| Windows | Windows 11 |
| Java trong PowerShell ban đầu | 25.0.4 |
| JDK dùng cho Gradle | JBR 21.0.11 |
| Gradle Wrapper | 8.9 |
| ADB | 37.0.1 |
| SDK Platform | `platforms/android-35` đã cài |
| Build-Tools nhìn thấy | 34.0.0 và 36.0.0 |
| Android Studio | Gradle Sync thành công |
| Điện thoại | POCO, `adb devices` trả `device` |
| Unit Test | 27 passed |
| Instrumented Test | 2 passed |
| `:app:connectedDebugAndroidTest` | `BUILD SUCCESSFUL in 38s` |
| App trên điện thoại | Android Studio Run ▶ đã khởi chạy thành công |

---

# I. Thuật ngữ và tài liệu chính thức

| Từ | Nghĩa ngắn |
|---|---|
| **Build** | Chuyển code/tài nguyên thành artifact có thể cài/chạy |
| **APK** | File gói cài đặt Android |
| **AAB** | Android App Bundle dùng trong quy trình phân phối |
| **Debug / Release** | Biến thể phục vụ phát triển / phát hành |
| **Gradle task** | Đơn vị công việc như `compile`, `assemble`, `test` |
| **Gradle Daemon** | Tiến trình JVM giúp tái sử dụng và tăng tốc build |
| **Wrapper** | Bộ file project gọi đúng phiên bản Gradle |
| **SDK Platform** | API Android dùng biên dịch |
| **Platform-Tools** | Bộ công cụ như ADB |
| **ADB device** | Thiết bị đã xác thực, ADB giao tiếp được |
| **Unit Test** | Test logic trên host JVM |
| **Instrumented Test** | Test chạy trong Android runtime |
| **UI Test** | Tự động thao tác/kiểm tra giao diện |
| **Gradle Sync** | IDE đồng bộ metadata và dependencies, không phải build hoàn chỉnh |

Tài liệu chính thức nên tham khảo:

- [Android Studio](https://developer.android.com/studio)
- [Android Studio — Build overview](https://developer.android.com/build)
- [Android SDK Manager](https://developer.android.com/studio/intro/update#sdk-manager)
- [Android Debug Bridge (adb)](https://developer.android.com/tools/adb)
- [Android test fundamentals](https://developer.android.com/training/testing/fundamentals)
- [Jetpack Compose testing](https://developer.android.com/develop/ui/compose/testing)
- [Gradle Wrapper](https://docs.gradle.org/current/userguide/gradle_wrapper.html)
- [Gradle Java compatibility](https://docs.gradle.org/current/userguide/compatibility.html)

---

## Gợi ý cho người đóng góp

Khi có thay đổi liên quan đến build hoặc môi trường, nên cập nhật README cùng với source:

- Nếu đổi `compileSdk`, `minSdk`, Gradle hoặc AGP: sửa bảng phiên bản và hướng dẫn tương ứng.
- Nếu thêm test: cập nhật vị trí file và lệnh test, không viết cố định số bài test như một cam kết.
- Nếu thay đổi giao diện: bổ sung hướng dẫn thao tác/manual test theo UI thật.
- Nếu có khác biệt giữa Windows, Linux, POCO hay các hãng khác: nêu rõ đây là nhánh xử lý lỗi, không phải điều kiện bắt buộc với mọi máy.
- Nếu phát hành: bổ sung hướng dẫn signing/release độc lập, tránh dùng nhầm Debug APK.
