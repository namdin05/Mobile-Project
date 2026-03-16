# MELODIX - TAI LIEU CHUC NANG VA LO TRINH PHAT TRIEN

## 1. Tong quan du an
- Ten du an: Melodix
- Nen tang: Android Studio (Java)
- Loai ung dung: Nghe nhac truc tuyen, podcast, ca nhan hoa trai nghiem nguoi dung
- Muc tieu: Xay dung ung dung co trai nghiem tuong tu Spotify, toi uu cho nguoi dung di dong va de mo rong trong cac giai doan tiep theo

## 2. Muc tieu san pham
- Cung cap trai nghiem nghe nhac on dinh, muot, it gian doan.
- Ho tro ca nhan hoa noi dung dua tren hanh vi nghe.
- Cung cap bo cong cu cho nghe si dang tai va quan ly bai hat.
- Cung cap he thong quan tri noi dung va nguoi dung cho admin.
- Dam bao kien truc de bao tri, de test, de mo rong cho Android Java.

## 3. Doi tuong su dung
- User: Nguoi dung nghe nhac/podcast, tao playlist, theo doi nghe si.
- Artist: Nghe si quan ly ho so va noi dung am nhac.
- Admin: Quan tri he thong, duyet noi dung, quan ly nguoi dung.

## 4. Pham vi va nguyen tac phat trien

### 4.1 Pham vi chinh
- Mobile app Android cho User va Artist.
- Trang quan tri web hoac module dashboard rieng cho Admin (co the tach repo).

### 4.2 Nguyen tac
- Uu tien MVP truoc: Nghe nhac, tim kiem, playlist, thu vien.
- Tung buoc bo sung tinh nang nang cao: xa hoi, AI, gamification.
- Moi tinh nang deu co tieu chi nghiem thu ro rang.

## 5. Dac ta chuc nang chi tiet

### 5.1 Xac thuc va bao mat
- Dang ky, dang nhap bang email/so dien thoai.
- Quen mat khau, doi mat khau.
- Dang nhap bang Google (social login).
- Quan ly phien dang nhap, dang xuat tat ca thiet bi.
- Ma hoa token, bao ve API bang JWT/OAuth2.

Tieu chi nghiem thu:
- Dang nhap thanh cong trong duoi 2 giay voi mang on dinh.
- Token het han duoc xu ly tu dong (refresh token).

### 5.2 Trang chu va kham pha
- Banner chien dich nhac moi/noi bat.
- Danh sach top trending, new releases, de xuat theo the loai.
- Khu vuc "Danh cho ban" dua tren lich su nghe.

Tieu chi nghiem thu:
- Du lieu trang chu tai theo tung khoi, khong block UI toan man hinh.

### 5.3 Tim kiem nang cao
- Tim theo bai hat, nghe si, album, playlist, podcast.
- Goi y tu khoa khi nhap.
- Luu lich su tim kiem gan day.
- Loc ket qua theo loai noi dung.

Tieu chi nghiem thu:
- Ket qua dau tien tra ve duoi 1.5 giay trong dieu kien API binh thuong.

### 5.4 Trinh phat nhac
- Phat/tam dung/chuyen bai.
- Che do lap lai, phat ngau nhien.
- Thanh tien trinh, seek den vi tri bat ky.
- Chay nen, hien thong bao media control.
- Tu dong phat tiep bai ke tiep trong hang doi.

Tieu chi nghiem thu:
- Khong bi crash khi chuyen app, tat man hinh, xoay man hinh.

### 5.5 Playlist ca nhan
- Tao, sua, xoa playlist.
- Them/xoa bai hat trong playlist.
- Sap xep thu tu bai hat (drag and drop).
- Dat anh bia, mo ta playlist.

Tieu chi nghiem thu:
- Thao tac them/xoa cap nhat UI ngay lap tuc, dong bo server sau.

### 5.6 Thu vien va tuong tac
- Like/Unlike bai hat.
- Recently played.
- Follow/Unfollow nghe si.
- Quan ly danh sach bai hat da tai ve (neu co offline).

Tieu chi nghiem thu:
- Du lieu thu vien dong bo dung khi dang nhap lai tai khoan.

### 5.7 Chi tiet nghe si va album
- Trang nghe si: tieu su, bai hat pho bien, danh sach album.
- Trang album: thong tin album, danh sach bai hat, nam phat hanh.
- Related artists.

### 5.8 Lyrics
- Hien thi loi bai hat van ban.
- Dong bo loi theo thoi gian (karaoke) neu co timestamp.
- Toan man hinh cho che do lyrics.

### 5.9 Podcast
- Kham pha podcast theo chu de.
- Phat podcast voi toc do 0.5x, 1x, 1.25x, 1.5x, 2x.
- Luu vi tri dang nghe (resume playback).
- Theo doi creator podcast.

### 5.10 Offline mode
- Tai bai hat/podcast ve bo nho may.
- Nghe khong can mang.
- Quan ly dung luong va xoa cache.

Rang buoc:
- Kiem soat ban quyen noi dung va token truy cap file offline.

### 5.11 Cai dat va ca nhan hoa
- Light/Dark mode.
- Sleep timer.
- Chat luong stream: Auto/Low/High.
- Chinh sua profile: avatar, ten hien thi.
- Ngon ngu ung dung (neu co).

### 5.12 Tinh nang xa hoi
- Chia se bai hat/playlist qua link.
- Story/clip ngan tu bai hat.
- Activity feed ban be.
- Listen together (dong bo phat nhac va chat).

### 5.13 Music insights va gamification
- Thong ke thoi gian nghe theo ngay/tuan/thang.
- The loai nghe nhieu nhat.
- Huy hieu (fan cung, night owl, explorer...).

### 5.14 AI recommendation
- Goi y bai hat dua tren lich su nghe, like, skip.
- Goi y playlist theo tam trang/chu de.
- Cung cap khu vuc "Vi ban se thich".

### 5.15 Chuc nang cho Artist
- Quan ly profile nghe si.
- Dang tai/chinh sua/xoa bai hat.
- Theo doi luot nghe, luot like, luot follow.
- Gui yeu cau duyet bai hat moi.

### 5.16 Chuc nang cho Admin
- Dashboard tong quan he thong.
- Duyet/noi dung bai hat tu artist.
- Quan ly user, artist, phan quyen tai khoan.
- Quan ly vi pham noi dung, khoa/mo tai khoan.
- Gui thong bao he thong den nguoi dung.

## 6. Yeu cau phi chuc nang

### 6.1 Hieu nang
- Startup time duoi 3 giay tren thiet bi tam trung.
- Tieu thu pin va data hop ly khi stream nhac.

### 6.2 Do on dinh
- Ty le crash free >= 99.5%.
- Co co che retry API va xu ly mat ket noi.

### 6.3 Bao mat
- Ma hoa du lieu nhay cam.
- Chan replay token, han che request bat thuong.

### 6.4 Kha nang mo rong
- Tach lop presentation/domain/data.
- De mo rong sang tablet/wearables neu can.

## 7. Kien truc de xuat cho Android Java
- Pattern: MVVM + Repository.
- Network: Retrofit + OkHttp.
- Local storage: Room + DataStore/SharedPreferences.
- Media: ExoPlayer.
- Background task: WorkManager.
- Dependency injection: Hilt/Dagger (hoac manual DI neu MVP nho).
- Test: JUnit, Mockito, Espresso.

## 8. Lo trinh phat trien (Roadmap)

## Giai doan 0 - Khoi dong du an (1-2 tuan)
- Chot scope MVP, use case, wireframe.
- Thiet lap repo, quy tac code, CI co ban.
- Setup base project Android Java, architecture skeleton.

Deliverables:
- Tai lieu yeu cau, backlog, mockup man hinh chinh.
- Source base chay duoc, tich hop API mau.

## Giai doan 1 - MVP cot loi (4-6 tuan)
- Xac thuc nguoi dung.
- Trang chu, tim kiem co ban.
- Trinh phat nhac, thu vien, playlist.
- Chi tiet nghe si/album.

Deliverables:
- Ban MVP co the demo end-to-end.
- Log su kien co ban (play/pause/search).

## Giai doan 2 - Mo rong trai nghiem (3-4 tuan)
- Lyrics, podcast, cai dat ca nhan.
- Offline mode phien ban dau.
- Nang cao UI/UX va toi uu toc do tai du lieu.

Deliverables:
- Ban beta cho nhom nguoi dung thu nghiem noi bo.

## Giai doan 3 - Xa hoi va tuong tac (3-4 tuan)
- Chia se link, activity feed.
- Listen together (ban dau).
- Push notification cho su kien quan trong.

Deliverables:
- Ban beta 2 voi tinh nang cong dong.

## Giai doan 4 - AI va he thong quan tri (3-5 tuan)
- Recommendation engine phien ban 1.
- Dashboard artist/admin.
- Quy trinh duyet noi dung bai hat.

Deliverables:
- Ban release candidate voi he thong van hanh co ban.

## Giai doan 5 - On dinh va phat hanh (2-3 tuan)
- Kiem thu toan dien: unit, integration, UI test.
- Performance tuning, crash fixing, security review.
- Chuan bi tai lieu phat hanh va metadata store.

Deliverables:
- Ban v1.0 san sang phat hanh.

## 9. Ke hoach phan chia sprint goi y
- Sprint 1: Auth + base navigation + home skeleton.
- Sprint 2: Search + song detail + playback co ban.
- Sprint 3: Playlist + library + profile.
- Sprint 4: Podcast + lyrics + settings.
- Sprint 5: Offline + optimization + analytics.
- Sprint 6: Social + AI recommendation + hardening.

## 10. KPI danh gia thanh cong
- Day-7 retention.
- Tong thoi gian nghe trung binh moi user/ngay.
- Ty le chuyen doi tu tim kiem sang phat nhac.
- Ty le su dung playlist.
- Crash free sessions.

## 11. Rui ro va giai phap
- Rui ro ban quyen noi dung: can quy trinh duyet va watermark/DRM phu hop.
- Rui ro hieu nang stream: cache hop ly, adaptive bitrate.
- Rui ro tai nguyen nho: uu tien MVP, tach backlog thanh nhom bat buoc va nang cao.

## 12. Dinh huong mo rong sau v1.0
- Wear OS companion.
- Android Auto.
- Dong bo da thiet bi nang cao.
- Goi y theo ngu canh (thoi gian, vi tri, hoat dong).

---

Tai lieu nay dung lam baseline de ca nhom thong nhat pham vi, thu tu uu tien, va ke hoach trien khai cho ung dung Melodix tren Android Studio (Java).