# XeCost — Thiết kế MVP

Ứng dụng Android quản lý chi phí xe máy / ô tô. Offline-first, không cần server.

- Package: `com.hailn.xecost`
- minSdk 26, targetSdk 35
- Tiền tệ: VND, lưu dạng `Long` (đồng, không có phần lẻ) — tránh sai số của `Double`

---

## 1. Data model

### Sơ đồ quan hệ

```
Vehicle (1) ──< (n) FuelEntry
   │
   ├──< (n) Expense
   │
   └──< (n) Reminder
```

Mọi bảng con đều có `vehicleId` + foreign key `onDelete = CASCADE`, và có index trên
`vehicleId` để query theo xe nhanh.

### Vehicle — xe

| Cột | Kiểu | Ghi chú |
|---|---|---|
| id | Long PK | auto |
| name | String | tên gợi nhớ: "Wave đi làm" |
| type | MOTORBIKE \| CAR | |
| plateNumber | String? | biển số |
| brand / model | String? | |
| year | Int? | |
| fuelType | RON95 \| E5 \| DIESEL \| ELECTRIC | |
| tankCapacity | Double? | lít |
| initialOdometer | Int | odo lúc bắt đầu dùng app |
| currentOdometer | Int | **tính lại** sau mỗi lần thêm/sửa/xoá, xem §2.1 |
| purchaseDate | Long? | epoch millis |
| purchasePrice | Long? | VND, để tính TCO |
| isActive | Boolean | false = đã bán, ẩn khỏi danh sách |
| isDefault | Boolean | xe mặc định khi mở form |
| createdAt | Long | |

### FuelEntry — lần đổ nhiên liệu

| Cột | Kiểu | Ghi chú |
|---|---|---|
| id | Long PK | |
| vehicleId | Long FK | |
| date | Long | epoch millis |
| odometer | Int | **bắt buộc** — không có odo thì không tính được tiêu hao |
| liters | Double | |
| pricePerLiter | Long | VND/lít |
| totalCost | Long | VND |
| isFullTank | Boolean | **quan trọng nhất** cho thuật toán tính L/100km |
| missedPrevious | Boolean | "lần trước quên ghi" → bỏ qua đoạn này khi tính |
| station | String? | |
| note | String? | |
| photoUri | String? | ảnh hoá đơn (V2) |

### Expense — chi phí khác

| Cột | Kiểu | Ghi chú |
|---|---|---|
| id | Long PK | |
| vehicleId | Long FK | |
| date | Long | |
| odometer | Int? | tuỳ chọn |
| category | enum | xem bảng dưới |
| title | String | "Thay nhớt Motul" |
| amount | Long | VND |
| place | String? | |
| note | String? | |
| photoUri | String? | |

`ExpenseCategory`: `MAINTENANCE` (bảo dưỡng), `REPAIR` (sửa chữa), `INSURANCE` (bảo hiểm),
`INSPECTION` (đăng kiểm), `ROAD_FEE` (phí đường bộ), `PARKING` (gửi xe), `WASH` (rửa xe),
`TOLL` (cầu đường), `FINE` (phạt), `ACCESSORY` (phụ kiện), `OTHER`.

### Reminder — nhắc nhở

| Cột | Kiểu | Ghi chú |
|---|---|---|
| id | Long PK | |
| vehicleId | Long FK | |
| type | OIL_CHANGE \| INSPECTION \| INSURANCE \| TIRE \| ROAD_FEE \| CUSTOM | |
| title | String | |
| dueDate | Long? | hạn theo thời gian |
| dueOdometer | Int? | hạn theo km |
| intervalDays | Int? | chu kỳ lặp |
| intervalKm | Int? | chu kỳ lặp |
| lastDoneDate / lastDoneOdometer | Long? / Int? | để tính hạn kế tiếp |
| isEnabled | Boolean | |

**Luật nhắc:** cái nào tới trước thì báo trước — `dueDate` hoặc `dueOdometer`.

---

## 2. Thuật toán tính mức tiêu hao

Đây là phần dễ làm sai nhất, nên tách riêng ra `domain/FuelStats.kt` để test được.

Nguyên tắc: **chỉ tính giữa hai lần đổ ĐẦY BÌNH.**

```
Lần 1: đổ đầy, odo 10.000        ← mốc bắt đầu (lít của lần này KHÔNG tính)
Lần 2: đổ một phần 2.0 L, odo 10.150
Lần 3: đổ đầy 3.5 L, odo 10.300   ← mốc kết thúc

Quãng đường = 10.300 − 10.000 = 300 km
Nhiên liệu đã tiêu thụ = 2.0 + 3.5 = 5.5 L   (tổng các lần SAU mốc đầu)
→ 5.5 / 300 × 100 = 1,83 L/100km
```

Bỏ qua một đoạn khi:
- có bản ghi `missedPrevious = true` nằm trong đoạn đó
- odo không tăng (nhập sai)

Chi phí mỗi km = tổng (nhiên liệu + chi phí khác) / tổng km đã đi.

### 2.1. Odo hiện tại của xe phải được TÍNH LẠI, không phải "kéo lên"

`Vehicle.currentOdometer` là dữ liệu dẫn xuất. Cách làm trực giác — mỗi lần lưu thì
`UPDATE ... WHERE currentOdometer < :odometer` — sai trong hai tình huống rất dễ gặp:

- nhập nhầm odo 20.000 thay vì 10.200, rồi vào sửa lại cho đúng
- xoá bản ghi đang giữ odo cao nhất

Cả hai đều để odo kẹt ở số cao, kéo theo `distanceTravelled` và ô "chi phí mỗi km" sai
vĩnh viễn, không có cách nào tự khỏi. Vì vậy sau **mọi** lần thêm / sửa / xoá:

```
currentOdometer = max(initialOdometer, MAX(odo) fuel_entries, MAX(odo) expenses)
```

Thực hiện ở `data/repository/OdometerSync.kt`, luôn gọi bên trong cùng một
`db.withTransaction { }` với thao tác vừa làm đổi dữ liệu.

---

## 3. Luồng màn hình (MVP)

```
MainActivity
└── NavHost
    ├── [Bottom bar]
    │   ├── Dashboard      tổng quan tháng, biểu đồ 6 tháng, chỉ số L/100km
    │   ├── Fuel           danh sách lần đổ xăng + FAB thêm
    │   ├── Expense        danh sách chi phí khác + FAB thêm
    │   └── Vehicles       danh sách xe + FAB thêm
    │
    ├── add_fuel?entryId={entryId}        form đổ xăng (thêm / sửa)
    ├── add_expense?expenseId={expenseId} form chi phí
    └── add_vehicle?vehicleId={vehicleId} form xe
```

Tên tham số (`entryId` / `expenseId` / `vehicleId`) phải khớp CHÍNH XÁC ba nơi: `navArgument`
trong `AppRoot.kt`, hàm sinh route trong `Destinations.kt`, và khoá đọc từ `SavedStateHandle`
trong ViewModel. Sai một chữ thì màn hình sửa luôn mở ra form trống.

**Thanh chọn xe** nằm ở đầu Dashboard — đổi xe thì cả app lọc theo xe đó.

### Dashboard hiển thị gì

1. Chip chọn xe
2. 4 ô số liệu: chi phí tháng này · L/100km trung bình · chi phí/km · tổng odo
3. Biểu đồ cột 6 tháng gần nhất (tách nhiên liệu / chi phí khác)
4. Danh sách nhắc nhở sắp tới hạn
5. 5 giao dịch gần nhất

---

## 4. Cấu trúc thư mục

```
app/src/main/java/com/hailn/xecost/
├── XeCostApp.kt              @HiltAndroidApp
├── MainActivity.kt
├── core/                     tiện ích: format tiền, ngày
├── data/
│   ├── local/
│   │   ├── entity/           Vehicle, FuelEntry, Expense, Reminder
│   │   ├── dao/              4 DAO tương ứng
│   │   ├── Converters.kt     enum ↔ String
│   │   └── AppDatabase.kt
│   └── repository/           bọc DAO, nơi đặt logic ghép dữ liệu
├── domain/
│   └── FuelStats.kt          tính L/100km, chi phí/km  (pure Kotlin, test được)
├── di/
│   └── DatabaseModule.kt     Hilt cung cấp Database + DAO + Repository
└── ui/
    ├── theme/
    ├── navigation/
    ├── dashboard/  fuel/  expense/  vehicle/
    │   └── mỗi thư mục: Screen.kt (Compose) + ViewModel.kt
    └── components/           ô số liệu, biểu đồ cột, item danh sách
```

Quy ước: **Screen không bao giờ gọi thẳng Repository.** Screen → ViewModel → Repository → DAO.

---

## 5. Phạm vi MVP vs để sau

| Có trong skeleton này | Để giai đoạn sau |
|---|---|
| CRUD xe / đổ xăng / chi phí | Ảnh hoá đơn, OCR |
| Tính L/100km, chi phí/km | Đồng bộ đa thiết bị |
| Dashboard + biểu đồ cột | Trip / GPS |
| Room + Hilt + Navigation | Widget, backup Drive |
| Bảng Reminder (schema sẵn) | WorkManager thực sự chạy nhắc |
| | Xuất CSV/PDF, ngân sách |

Bảng `Reminder` được tạo sẵn ngay từ đầu dù chưa dùng — để sau này thêm tính năng nhắc
không phải viết migration.

---

## 6. Những cái bẫy đã biết

Ghi lại để lần sau không ai đạp phải.

**Enum lưu theo tên là con dao hai lưỡi.** `Converters` dùng `enum.valueOf(name)`. Lưu theo
tên nên đổi *thứ tự* enum thoải mái — nhưng **đổi tên hoặc xoá** một hằng enum sẽ làm
`valueOf` ném `IllegalArgumentException` và **sập cả câu query** với dữ liệu cũ, mà không
migration nào báo cho bạn biết. Muốn bỏ một danh mục thì phải viết migration `UPDATE` dữ
liệu cũ sang hằng mới trước.

**Property tính toán trong `@Entity`.** `Vehicle.displayName` và `distanceTravelled` chỉ có
getter nên không sinh backing field → Room bỏ qua, không thành cột. Nhưng thêm một property
**có giá trị khởi tạo** (`val x = f()`) vào thân class thì nó *sẽ* có backing field và KSP
báo lỗi. Lúc đó phải dùng `@Ignore` — và lưu ý `@Ignore` **không** đặt được lên property
không có backing field.

**`Modifier.padding(innerPadding)` không tiêu thụ window insets.** Scaffold lồng Scaffold
(khung app + form) sẽ trừ status bar và nav bar hai lần. Phải thêm
`.consumeWindowInsets(padding)` cho `NavHost`.

**Ba ô lít / đơn giá / thành tiền — phải nhớ HAI ô, không phải một.** Trực giác đầu tiên là
nhớ ô người dùng vừa gõ (`lastEdited`) rồi tính lại ô còn lại. Sai, vì **mỗi ký tự gõ vào là
một lần gọi hàm set**: đến ký tự thứ hai thì `lastEdited` đã là chính ô đang gõ, nhánh `if`
rẽ nhầm và ghi đè lên con số ở ô kia. Phải có thêm `prevEdited` — ô *khác* gõ gần nhất — và
chỉ đẩy `lastEdited` xuống `prevEdited` khi người dùng thật sự chuyển sang ô khác.

**Sửa bản ghi = dựng entity MỚI rồi ghi đè, nên trường nào quên chép là mất.** `AddXViewModel`
không giữ nguyên object cũ mà dựng `Vehicle(...)` / `FuelEntry(...)` / `Expense(...)` mới từ
state của form. Trường nào không có ô nhập trên màn hình (`year`, `photoUri`, `purchaseDate`,
`createdAt`) phải được chép tay từ bản ghi gốc, nếu không nó âm thầm về `null` mỗi lần sửa.
Lỗi này không báo gì cả, và chỉ lộ ra khi ai đó thêm ô nhập cho trường đó vài tháng sau.

**`enableEdgeToEdge()` làm `adjustResize` mất tác dụng.** Nó đặt
`decorFitsSystemWindows = false`, nên window không tự co lại khi bàn phím hiện nữa — dù
manifest vẫn khai `android:windowSoftInputMode="adjustResize"`. Mọi màn hình có ô nhập phải
tự thêm `Modifier.imePadding()`, nếu không nửa dưới form bị bàn phím che và không cuộn tới
được.

**Cờ boolean mặc định `false` vừa nghĩa "không có" vừa nghĩa "chưa biết".** `stateIn` phát
`initialValue` ngay lập tức, trước khi Room trả dữ liệu. Nếu UiState chỉ có `hasVehicle:
Boolean = false` thì frame đầu tiên luôn vẽ "Chưa có xe nào" dù người dùng có đủ xe. Mỗi
UiState đọc từ DB phải có cờ `loading` riêng, và màn hình `return` sớm khi còn loading.

**Chốt chặn quy tắc nghiệp vụ phải nằm ở repository, không chỉ ở UI.** `save()` bỏ cờ mặc
định cho xe đã bán, nhưng `setDefault()` gọi từ menu lại là một đường khác — nếu chỉ chặn ở
menu thì đường kia vẫn lọt. Mọi đường ghi cùng một bất biến phải tự kiểm lại bất biến đó.

**Đừng bật `ksp.useKSP2=true`.** Room 2.6.1 chưa hỗ trợ KSP2. Nếu cần KSP2 thì phải nâng
Room lên 2.7+ trước.
