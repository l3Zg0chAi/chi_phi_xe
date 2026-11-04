# XeCost

Ứng dụng Android quản lý chi phí xe máy / ô tô. Offline hoàn toàn, không cần tài khoản.

Thiết kế data model và luồng màn hình: [DESIGN.md](DESIGN.md)

---

# Phần 1 — Chuẩn bị máy

## 1.1. Cần những gì

| Phần mềm | Phiên bản | Bắt buộc? | Dùng để làm gì |
|---|---|---|---|
| **JDK** | 17 trở lên | ✅ | Gradle và Kotlin compiler chạy trên JVM. AGP 8.7 yêu cầu tối thiểu JDK 17 |
| **Android SDK** | API 35 (Android 15) | ✅ | Thư viện Android + công cụ đóng gói APK |
| **Android SDK Platform-Tools** | mới nhất | ✅ | Chứa `adb` để cài app lên máy |
| **Android SDK Build-Tools** | 35.0.0 | ✅ | `aapt2`, `d8` — dịch resource và bytecode |
| **Android Studio** | Ladybug (2024.2) trở lên | ⬜ khuyến nghị | IDE, trình quản lý SDK, emulator, Compose Preview |
| **Gradle** | 8.9 | ⬜ | Chỉ cần nếu build bằng dòng lệnh mà chưa có wrapper |
| **Git** | bất kỳ | ⬜ | Quản lý mã nguồn |

> **Chọn một trong hai cách:**
> - **Cách A — cài Android Studio** (khuyến nghị): nó kèm sẵn JDK riêng (JetBrains Runtime)
>   và Gradle riêng, bạn chỉ cần tải thêm SDK qua giao diện. Có emulator và Compose Preview.
> - **Cách B — chỉ dòng lệnh**: nhẹ hơn nhiều (~700 MB thay vì ~4 GB), nhưng phải tự cài JDK
>   và dùng `sdkmanager`. Không có emulator đồ hoạ tiện lợi, không có Preview.

**Dung lượng đĩa:** Android Studio ~4 GB, SDK ~3 GB, một emulator image ~1.5 GB, thư mục
`~/.gradle` sau lần build đầu ~2 GB. Chuẩn bị **tối thiểu 12 GB trống**.

**RAM:** 8 GB chạy được, 16 GB mới mượt nếu dùng emulator.

---

## 1.2. Cài đặt trên Windows

### Cách A — Android Studio (khuyến nghị)

Mở **PowerShell** và dùng winget:

```powershell
winget install --id Google.AndroidStudio -e
winget install --id Git.Git -e
```

Hoặc tải thủ công tại <https://developer.android.com/studio>.

Mở Android Studio lần đầu → trình **Setup Wizard** hiện lên → chọn **Standard** → nó tự tải
Android SDK, Platform-Tools và một emulator image. Chờ khoảng 10–20 phút tuỳ mạng.

Sau đó kiểm tra đã có API 35 chưa:

> **Tools → SDK Manager → SDK Platforms** → tick **Android 15.0 (API 35)**
> → tab **SDK Tools** → tick **Android SDK Build-Tools 35**, **Android SDK Platform-Tools**
> → **Apply**

Đến đây là chạy được. Nhảy xuống [Phần 2](#phần-2--mở-và-chạy-project).

### Cách B — chỉ dòng lệnh

**Bước 1. Cài JDK 17**

```powershell
winget install --id Microsoft.OpenJDK.17 -e
```

**Bước 2. Cài Android command line tools**

Tải *Command line tools only* tại <https://developer.android.com/studio#command-tools>,
rồi giải nén theo đúng cấu trúc thư mục dưới đây (bắt buộc phải có cấp `latest`,
nếu không `sdkmanager` sẽ báo lỗi):

```
C:\Android\Sdk\
└── cmdline-tools\
    └── latest\
        ├── bin\
        └── lib\
```

**Bước 3. Đặt biến môi trường**

```powershell
setx JAVA_HOME "C:\Program Files\Microsoft\jdk-17.0.13.11-hotspot"
setx ANDROID_HOME "C:\Android\Sdk"
setx PATH "$env:PATH;C:\Android\Sdk\cmdline-tools\latest\bin;C:\Android\Sdk\platform-tools"
```

Sửa lại đường dẫn `JAVA_HOME` cho khớp máy bạn — kiểm tra bằng:

```powershell
Get-ChildItem "C:\Program Files\Microsoft" -Directory
```

**Đóng PowerShell và mở lại** thì biến môi trường mới có hiệu lực.

**Bước 4. Tải SDK**

```powershell
sdkmanager "platform-tools" "platforms;android-35" "build-tools;35.0.0"
sdkmanager --licenses
```

Lệnh `--licenses` hỏi `y/N` nhiều lần — gõ `y` cho tất cả, không chấp nhận license thì
Gradle sẽ từ chối build.

**Bước 5. Cài Gradle** (chỉ cần nếu project chưa có wrapper)

```powershell
winget install --id Gradle.Gradle -e
```

---

## 1.3. Cài đặt trên Linux

### Bước 1 — Cài JDK 17

**Ubuntu / Debian / Linux Mint**

```bash
sudo apt update && sudo apt install -y openjdk-17-jdk
```

**Fedora / RHEL**

```bash
sudo dnf install -y java-17-openjdk-devel
```

**Arch / Manjaro**

```bash
sudo pacman -S --needed jdk17-openjdk
```

Nếu máy có nhiều bản Java, chọn bản 17 làm mặc định:

```bash
sudo update-alternatives --config java
```

### Bước 2 — Cài Android Studio (Cách A)

**Qua snap** (Ubuntu và các bản hỗ trợ snap):

```bash
sudo snap install android-studio --classic
```

**Hoặc tải thủ công** — cách này chạy được trên mọi distro:

```bash
cd /opt && sudo tar -xzf ~/Downloads/android-studio-*-linux.tar.gz
/opt/android-studio/bin/studio.sh
```

Thư viện 32-bit mà Android Studio cần trên Ubuntu/Debian:

```bash
sudo apt install -y libc6-i386 libncurses6 libstdc++6 lib32z1 libbz2-1.0
```

Mở lần đầu → **Setup Wizard** → chọn **Standard** → nó tự tải SDK về `~/Android/Sdk`.

### Bước 2' — Chỉ dòng lệnh (Cách B)

```bash
mkdir -p ~/Android/Sdk/cmdline-tools
cd ~/Android/Sdk/cmdline-tools
curl -o tools.zip https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip
unzip tools.zip && mv cmdline-tools latest && rm tools.zip
```

> Số hiệu bản `11076708` thay đổi theo thời gian. Lấy link mới nhất tại
> <https://developer.android.com/studio#command-tools> nếu lệnh trên báo 404.

### Bước 3 — Đặt biến môi trường

Thêm vào `~/.bashrc` (hoặc `~/.zshrc` nếu dùng zsh):

```bash
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
export ANDROID_HOME=$HOME/Android/Sdk
export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools
```

Nạp lại:

```bash
source ~/.bashrc
```

Đường dẫn `JAVA_HOME` khác nhau theo distro — tìm đúng đường dẫn máy bạn bằng:

```bash
dirname $(dirname $(readlink -f $(which javac)))
```

### Bước 4 — Tải SDK

```bash
sdkmanager "platform-tools" "platforms;android-35" "build-tools;35.0.0"
```

```bash
yes | sdkmanager --licenses
```

### Bước 5 — Bật KVM cho emulator

Không có KVM thì emulator chạy chậm đến mức không dùng được.

```bash
sudo apt install -y qemu-kvm cpu-checker && kvm-ok
```

Nếu báo *KVM acceleration can be used* là ổn. Thêm tài khoản của bạn vào nhóm `kvm`:

```bash
sudo usermod -aG kvm $USER
```

**Đăng xuất và đăng nhập lại** để nhóm mới có hiệu lực.

### Bước 6 — Cho phép nhận máy Android qua USB

```bash
sudo apt install -y android-sdk-platform-tools-common
```

Gói này cài sẵn file `/lib/udev/rules.d/51-android.rules`, nhờ đó `adb` thấy được máy thật
mà không phải chạy bằng `sudo`. Cắm máy vào rồi kiểm tra bằng `adb devices`.

---

## 1.4. Kiểm tra máy đã sẵn sàng chưa

Chạy lần lượt, cả Windows lẫn Linux đều giống nhau:

```bash
java -version
```

```bash
adb --version
```

```bash
sdkmanager --list_installed
```

Kết quả mong đợi: Java báo `17.x` trở lên, `adb` in ra số phiên bản, `sdkmanager` liệt kê
có `platforms;android-35` và `build-tools;35.0.0`.

---

# Phần 2 — Mở và chạy project

> ### ⚠️ Windows: đặt project ở đường dẫn NGẮN
>
> Windows giới hạn đường dẫn 260 ký tự. Gói Java lồng sâu (`app/src/main/java/com/hailn/
> xecost/data/repository/…`) đã dài sẵn, mà Gradle/KSP còn sinh thêm
> `app/build/generated/ksp/debug/java/com/hailn/xecost/…` sâu hơn nữa — build sẽ fail với
> lỗi khó hiểu kiểu `FileNotFoundException` hoặc `path too long`.
>
> **Để project ở `C:\dev\xecost` hoặc `C:\src\xecost`**, đừng để sâu trong `Documents`,
> `OneDrive` hay thư mục tạm. Kiểm tra nhanh đường dẫn dài nhất hiện có:
>
> ```powershell
> (Get-ChildItem . -Recurse -File | Measure-Object { $_.FullName.Length } -Maximum).Maximum
> ```
>
> Nếu buộc phải để ở đường dẫn dài, bật hỗ trợ long path (cần quyền Administrator, khởi
> động lại máy sau đó):
>
> ```powershell
> New-ItemProperty -Path "HKLM:\SYSTEM\CurrentControlSet\Control\FileSystem" -Name LongPathsEnabled -Value 1 -PropertyType DWORD -Force
> ```
>
> ```powershell
> git config --global core.longpaths true
> ```
>
> Linux và macOS không có giới hạn này.

## 2.1. Việc cần làm một lần: tạo Gradle wrapper

Project có sẵn `gradle/wrapper/gradle-wrapper.properties` (ghim Gradle 8.9) nhưng **không có
`gradle-wrapper.jar`, `gradlew` và `gradlew.bat`** — đó là file nhị phân, không nên commit
kèm trong một skeleton. Thiếu chúng thì cả `./gradlew` lẫn Android Studio đều không sync được.

Cần một bản Gradle cài sẵn trên máy để sinh ra chúng. Chạy **một lần** ở thư mục gốc project:

```bash
gradle wrapper --gradle-version 8.9
```

Chưa có `gradle` trên máy thì:

```powershell
winget install --id Gradle.Gradle -e
```

```bash
sudo apt install -y gradle     # Ubuntu/Debian — hoặc dnf/pacman tương ứng
```

Sau lệnh đó thư mục gốc có thêm `gradlew`, `gradlew.bat` và `gradle/wrapper/gradle-wrapper.jar`.
Từ đây trở đi mọi lệnh đều dùng wrapper, **không** dùng `gradle` toàn cục nữa — wrapper bảo đảm
ai cũng build bằng đúng Gradle 8.9.

> Không muốn cài Gradle? Mở project trong Android Studio, nó sẽ báo *Gradle wrapper not found*
> và cho chọn **Use Gradle from: 'gradle-wrapper.properties' file** hoặc trỏ tới bản Gradle
> riêng của nó. Chọn xong rồi vào **File → Settings → Build Tools → Gradle** để kiểm tra lại.

Trên Linux, nếu `./gradlew` báo *Permission denied*:

```bash
chmod +x gradlew
```

## 2.2. Bằng Android Studio

1. **File → Open** → chọn thư mục `xecost` (thư mục chứa `settings.gradle.kts`), **không phải**
   thư mục `app`
2. Android Studio tự sync Gradle và tải thư viện — lần đầu mất 5–15 phút, thanh tiến trình
   nằm ở đáy cửa sổ
3. Chọn thiết bị ở thanh công cụ → bấm **Run** (▶) hoặc `Shift+F10`

Android Studio tự tạo `local.properties` trỏ tới SDK, bạn không cần làm gì thêm.

Lần sync đầu tiên cũng là lần đầu KSP sinh code cho Room và Hilt. Nếu IDE báo đỏ
`DaggerXeCostApp_HiltComponents` hay `AppDatabase_Impl` **trước khi** sync xong thì đó là
bình thường — code đó chưa tồn tại. Sync xong mà vẫn đỏ thì mới là lỗi thật.

## 2.3. Bằng dòng lệnh

Nếu không dùng Android Studio, tự tạo file `local.properties` ở thư mục gốc:

**Windows** — chú ý dấu `\:` và `\\`, đây là định dạng của file `.properties`
```properties
sdk.dir=C\:\\Android\\Sdk
```

**Linux**
```properties
sdk.dir=/home/<tên-user>/Android/Sdk
```

File này **không commit** (đã nằm trong `.gitignore`) vì đường dẫn SDK mỗi máy một khác.

| Việc cần làm | Lệnh |
|---|---|
| Build APK debug | `./gradlew assembleDebug` |
| Build + cài lên máy đang cắm USB | `./gradlew installDebug` |
| Cài rồi mở app luôn | `./gradlew installDebug && adb shell am start -n com.hailn.xecost/.MainActivity` |
| Gỡ app khỏi máy | `./gradlew uninstallDebug` |
| Xoá kết quả build cũ | `./gradlew clean` |
| Xem log của app đang chạy | `adb logcat --pid=$(adb shell pidof -s com.hailn.xecost)` |

Trên Windows dùng `gradlew.bat` thay cho `./gradlew`. File APK nằm ở
`app/build/outputs/apk/debug/app-debug.apk`.

Lần build đầu tải khoảng 300–500 MB thư viện về `~/.gradle`, mất 5–15 phút. Những lần sau
chỉ còn vài chục giây.

## 2.4. Chuẩn bị thiết bị để chạy thử

### Máy Android thật (nhanh nhất, nhẹ nhất)

Trên điện thoại: **Cài đặt → Giới thiệu điện thoại** → bấm **Số bản dựng** 7 lần để mở
*Tuỳ chọn nhà phát triển* → bật **Gỡ lỗi USB**. Cắm cáp, chọn *Cho phép* trên màn hình
điện thoại, rồi kiểm tra:

```bash
adb devices
```

Thấy dòng `<serial>  device` là được. Nếu thấy `unauthorized` thì rút ra cắm lại và bấm
*Cho phép* trên điện thoại.

### Emulator

Trong Android Studio: **Tools → Device Manager → Create Device**.

Bằng dòng lệnh:

```bash
sdkmanager "system-images;android-35;google_apis;x86_64"
```

```bash
avdmanager create avd -n Pixel7 -k "system-images;android-35;google_apis;x86_64" -d pixel_7
```

```bash
emulator -avd Pixel7
```

Trên Windows, emulator cần bật ảo hoá trong BIOS và bật tính năng
*Windows Hypervisor Platform*. Trên Linux cần KVM (xem mục 1.3 bước 5).

---

## 2.5. Lỗi thường gặp

| Thông báo lỗi | Nguyên nhân và cách xử lý |
|---|---|
| `gradlew: no such file or directory` / *Gradle wrapper not found* | Chưa tạo wrapper. Xem mục 2.1 |
| `SDK location not found` | Thiếu `local.properties` hoặc biến `ANDROID_HOME`. Xem mục 2.3 |
| `Failed to install the following SDK components ... licenses` | Chưa chấp nhận license. Chạy `sdkmanager --licenses` |
| `Unsupported class file major version 6x` | Đang dùng JDK quá mới hoặc quá cũ. Project cần JDK 17. Trong Android Studio: **Settings → Build Tools → Gradle → Gradle JDK** |
| `Could not resolve ...` khi sync | Mất mạng hoặc bị chặn. Thử lại, hoặc đặt proxy trong `gradle.properties` |
| `adb: no devices/emulators found` | Chưa bật Gỡ lỗi USB, hoặc trên Linux thiếu udev rules (mục 1.3 bước 6) |
| Emulator chậm như rùa | Thiếu tăng tốc phần cứng. Linux: bật KVM. Windows: bật ảo hoá trong BIOS |
| `Daemon will be stopped ... JVM heap space` | Tăng bộ nhớ trong `gradle.properties`: `org.gradle.jvmargs=-Xmx4096m` |
| `FileNotFoundException` / `path too long` / `Cannot find path` (chỉ trên Windows) | Đường dẫn vượt 260 ký tự. Chuyển project về `C:\dev\xecost` — xem cảnh báo đầu Phần 2 |
| `Unresolved reference: kotlinx` | Hiếm, chỉ xảy ra nếu thư viện nào đó đổi scope. Đã khai báo tường minh `kotlinx-coroutines-android` trong `app/build.gradle.kts` nên không nên gặp |
| `Unresolved reference: Insights` | Tên icon ở `ui/navigation/Destinations.kt`. Đây là chỗ duy nhất trong project không xác minh được bằng tài liệu chính thức. Thay bằng `Icons.Default.BarChart` (sửa cả dòng `import`) |
| `No parameter named 'suffix'` | Ở `ui/components/FormFields.kt`. Thay `suffix = suffixSlot` bằng `trailingIcon = suffixSlot` |
| `This class can only be used with the compiler argument '-opt-in=...'` | Thiếu `@OptIn`. Thêm annotation đúng tên mà thông báo lỗi nêu, đặt ngay trên hàm `@Composable` chứa lời gọi |

---

# Phần 3 — Test

## 3.1. Chạy test

Toàn bộ unit test (chạy trên JVM của máy bạn, **không cần điện thoại hay emulator**):

```bash
./gradlew test
```

Chỉ chạy biến thể debug cho nhanh:

```bash
./gradlew testDebugUnitTest
```

Chạy đúng một class:

```bash
./gradlew testDebugUnitTest --tests "com.hailn.xecost.domain.FuelStatsTest"
```

Chạy đúng một test — tên hàm có dấu cách nên phải bọc trong nháy:

```bash
./gradlew testDebugUnitTest --tests "*odo trung nhau khong lam mat so lit cua doan sau*"
```

Gradle **bỏ qua** task `test` nếu không có gì thay đổi từ lần chạy trước (`UP-TO-DATE`).
Muốn ép chạy lại:

```bash
./gradlew testDebugUnitTest --rerun-tasks
```

Trong Android Studio: mở `FuelStatsTest.kt`, bấm mũi tên ▶ xanh ở lề trái — cạnh tên class
để chạy cả file, cạnh tên hàm để chạy một test.

## 3.2. Đọc kết quả

Dòng `BUILD SUCCESSFUL` là qua hết. Khi có test fail, Gradle in tên test fail và đường dẫn
tới báo cáo HTML:

```
app/build/reports/tests/testDebugUnitTest/index.html
```

Mở file đó bằng trình duyệt — nó hiện từng test, thông báo `assertEquals` mong đợi gì nhận
được gì, và stack trace. Bản XML cho CI nằm ở `app/build/test-results/testDebugUnitTest/`.

Muốn thấy luôn `println` trong lúc chạy:

```bash
./gradlew testDebugUnitTest --info
```

## 3.3. Test đang phủ cái gì

**27 test, tất cả nằm ở `app/src/test/java/com/hailn/xecost/domain/FuelStatsTest.kt`.**

Chúng chỉ kiểm tra `domain/FuelStats.kt` — phần tính mức tiêu hao và chi phí mỗi km. Đây là
chủ ý: `FuelStats` là Kotlin thuần, không đụng tới Android, nên test chạy trong vài giây mà
vẫn phủ đúng chỗ dễ sai nhất.

| Nhóm | Test kiểm tra điều gì |
|---|---|
| Trường hợp rỗng | danh sách rỗng, chưa đủ hai lần đổ đầy, không có lần nào đổ đầy |
| Cộng dồn | lần đổ một phần ở giữa phải được cộng vào, lần đổ lẻ ở cuối không đóng đoạn |
| `missedPrevious` | đoạn chứa bản ghi "quên ghi" bị loại, và **không** lan sang đoạn kế tiếp |
| Odo bất thường | odo không tăng, odo trùng nhau, nhập lẫn thứ tự ngày |
| Phép trung bình | trung bình tính trên tổng quãng đường, không phải trung bình cộng các đoạn |
| Tiền | chi phí của đoạn không tính tiền của lần đổ làm mốc đầu |
| Liên kết UI | mỗi đoạn ghi lại `endEntryId` để danh sách gắn đúng con số vào đúng dòng |

Vài test trong số này được viết *sau khi* tìm ra lỗi thật — ví dụ "odo trùng nhau không làm
mất số lít của đoạn sau" chính là lỗi nuốt mất 3 lít đã mô tả ở [DESIGN.md](DESIGN.md) §6.
Đừng xoá chúng khi refactor.

## 3.4. Những phần CHƯA có test

Nói thẳng để bạn không tin nhầm vào con số 27:

- **Không có test cho Repository, DAO, ViewModel.** Logic odo tính lại
  (`OdometerSync`), ba ô lít/đơn giá/thành tiền tự suy nhau, chống lưu trùng khi bấm hai
  lần — tất cả mới chỉ được review bằng mắt.
- **Không có UI test.** `app/build.gradle.kts` có khai báo
  `testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"` nhưng thư mục
  `app/src/androidTest/` **không tồn tại** và cũng chưa khai báo dependency nào cho nó. Chạy
  `./gradlew connectedAndroidTest` lúc này sẽ không chạy test nào cả — không phải lỗi.

Muốn thêm instrumentation test thì cần tạo `app/src/androidTest/java/...` và bổ sung vào
`app/build.gradle.kts`:

```kotlin
androidTestImplementation(platform(libs.androidx.compose.bom))
androidTestImplementation("androidx.test.ext:junit:1.2.1")
androidTestImplementation("androidx.compose.ui:ui-test-junit4")
debugImplementation("androidx.compose.ui:ui-test-manifest")
```

Đáng làm nhất theo thứ tự: test Room cho `OdometerSync` (dùng
`Room.inMemoryDatabaseBuilder`), rồi test ViewModel cho `AddFuelViewModel`.

## 3.5. Test tay — checklist chạy thử lần đầu

Vì UI chưa có test tự động, đây là đường đi ngắn nhất để biết app có thật sự chạy đúng:

1. **Thêm xe** — tên, loại xe, odo ban đầu `10000`. Lưu. Xe hiện trong tab *Xe*.
2. **Đổ xăng lần 1** — odo `10000`, `3` lít, đơn giá `23500` → ô *Thành tiền* tự nhảy `70500`.
   Bật *Đổ đầy bình*. Lưu.
3. **Đổ xăng lần 2** — odo `10300`, `5` lít, bật *Đổ đầy bình*. Lưu.
   → Dòng thứ hai trong danh sách phải hiện **1,67 L/100km** (5 ÷ 300 × 100).
4. **Sửa lại odo lần 2** thành `10200` → con số đổi thành **2,5 L/100km**, và ô *Tổng odo*
   trên Dashboard tụt xuống `10200`. Đây là chỗ kiểm tra odo được *tính lại* chứ không phải
   chỉ kéo lên.
5. **Xoá bản ghi lần 2** → *Tổng odo* phải quay về `10000`, không kẹt ở `10200`.
6. **Thêm chi phí** — danh mục *Bảo dưỡng*, số tiền `250000`. Dashboard cộng vào ô chi phí
   tháng này và vẽ thêm vào biểu đồ cột.
7. **Gõ Thành tiền trước rồi sửa đơn giá** ở form đổ xăng → con số *Thành tiền* bạn vừa gõ
   phải được **giữ nguyên**, ô *Lít* mới là ô được tính lại.
8. **Bấm Lưu hai lần thật nhanh** → chỉ được tạo **một** bản ghi.
9. **Đổi sang dark mode** (Cài đặt hệ thống) rồi mở lại app → không loé khung trắng, chữ
   vẫn đọc được.
10. **Xoay ngang màn hình** ở mọi màn hình form → dữ liệu đang nhập không mất.

Bước 4, 5, 7, 8 chính là bốn lỗi đã sửa trong đợt review — nếu bước nào sai, báo lại kèm
số bước.

---

# Phần 4 — Về project

## Đã có trong skeleton này

- **Xe**: thêm / sửa / đặt mặc định / đánh dấu đã bán. Nhiều xe, xe máy và ô tô chung một app.
- **Đổ xăng**: ngày, odo, số lít, đơn giá, thành tiền — nhập 2 trong 3 ô, ô còn lại tự tính.
  Có cờ *đổ đầy bình* và *lần trước quên ghi*.
- **Mức tiêu hao**: tự tính L/100km giữa hai lần đổ đầy, hiện trên từng dòng và trên Dashboard.
- **Chi phí khác**: 11 danh mục (bảo dưỡng, sửa chữa, bảo hiểm, đăng kiểm, gửi xe…), lọc theo danh mục.
- **Dashboard**: chi phí tháng này so với tháng trước, L/100km, chi phí/km, biểu đồ cột 6 tháng,
  danh sách giao dịch gần nhất.
- **Room + Hilt + Navigation Compose**, Material 3, hỗ trợ dark mode và dynamic color.
- **27 unit test** cho thuật toán tính tiêu hao — xem [Phần 3](#phần-3--test).

## Chưa có (giai đoạn sau)

Nhắc nhở bảo dưỡng/đăng kiểm (bảng `reminders` đã tạo sẵn, chỉ thiếu phần WorkManager),
ảnh hoá đơn, ngân sách, xuất CSV/PDF, backup, widget, đồng bộ đa thiết bị.

## Những chỗ nên đọc trước khi sửa code

| File | Vì sao quan trọng |
|---|---|
| `domain/FuelStats.kt` | Thuật toán tính L/100km — dễ làm sai nhất, đã có test đi kèm |
| `data/local/entity/` | Schema Room. Đổi ở đây là phải viết migration |
| `data/SelectedVehicleStore.kt` | Xe đang chọn, dùng chung giữa các tab |
| `ui/navigation/AppRoot.kt` | Khung app: thanh tab + NavHost |

## Quy ước

- Tiền lưu bằng `Long` (đơn vị đồng), **không dùng `Double`** để tránh sai số cộng dồn.
- Enum lưu xuống DB theo **tên**, không theo thứ tự — đổi thứ tự enum sau này không hỏng dữ liệu.
- Màn hình (Compose) không gọi thẳng Repository. Luồng luôn là:
  `Screen → ViewModel → Repository → DAO`.
- Không dùng `fallbackToDestructiveMigration()` — nó xoá sạch dữ liệu người dùng khi đổi schema.
