**TEST PLAN** 

## **PROJECT: QNU-EVENT** 

## **V0.1.0-SPRINT1** 

## **LỊCH SỬ SỬA ĐỔI TÀI LIỆU** 

|**Phiên**<br>**bản**<br>**(Rev)**|**Ngày cập**<br>**nhật**|**Tác giả**|**Phiên**<br>**bản**<br>**Phần**<br>**mềm**|**Mô tả chi tiết nội dung thay**<br>**đổi**|
|---|---|---|---|---|
|v1.0|05/11/2026|Dương|v0.1.0-<br>sprint1-<br>alpha|Khởi tạo bản thảo đầu tiên: xác<br>định phạm vi 10 hạng mục<br>Sprint 1, kỹ thuật thiết kế test<br>case (EP, BVA, Decision Table),<br>tiêu chí vào/ra, ma trận rủi ro và<br>phân vai A0–A4.|
|v1.1|08/11/2026|Dương|v0.1.0-<br>sprint1-<br>rc1|Bổ sung Tóm tắt điều hành<br>(Executive Summary), chiến<br>lược 4 trụ cột (Functional,<br>Security, Concurrency, UAT),<br>mô phỏng môi trường<br>mạng/đồng hồ, lịch trình 5 pha<br>và yêu cầu tuân thủ<br>(Compliance).|
|v1.2|11/11/2026|Dương<br>Cường|v0.1.0-<br>sprint1|Chuẩn hóa mục Định danh Kế<br>hoạch Kiểm thử (Test Plan<br>Identifier) theo chuẩn IEEE<br>829, bổ sung bảng quản lý cấu<br>hình Software ↔ Testware,<br>thông tin liên hệ và lịch sử<br>phiên bản; chốt số liệu thực<br>thi cuối Sprint.|



# **1.GIỚI THIỆU** 

Đây là tài liệu Level & System Integration Test Plan dành cho giai đoạn Sprint 1 của dự án QNU-Events, Hệ thống Quản lý và Điểm danh Sự kiện Đại học Quy Nhơn. Kế hoạch này chỉ tập trung giải quyết các hạng mục và thành phần liên quan đến lát cắt dọc cốt lõi của Sprint 1, bao gồm cả những yếu tố chịu tác động trực tiếp lẫn gián tiếp trong toàn bộ quy trình nghiệp vụ từ xác thực phiên đăng nhập, tạo và công bố sự kiện, lọc sự kiện theo lớp, đăng ký giữ suất nguyên tử, điểm danh bằng Dynamic QR cho đến tra cứu lịch sử và ghi nhận Audit Log bất biến. Trọng tâm chính của kế hoạch là đảm bảo ứng dụng QNU-Events vận hành chính xác, an toàn và ổn định trước các rủi ro tranh chấp đồng thời hoặc giả mạo mã QR, đồng thời đáp ứng đầy đủ khả năng thu thập dữ liệu kiểm toán chi tiết và trải nghiệm mượt mà trên thiết bị di động. 

Dự án áp dụng ba cấp độ kiểm thử chính gồm Unit & Component Testing, System & Integration Testing và Acceptance Testing. Trong đó, System & Integration Testing bao phủ cả kiểm thử API Integration trên cơ sở dữ liệu PostgreSQL 17 thật, Security Testing và Concurrency Testing, còn Acceptance Testing bao gồm Playwright End-to-End Testing và UAT theo kịch bản trình diễn thực tế. Thông tin chi tiết cho từng cấp độ kiểm thử được trình bày cụ thể trong phần Test Strategy và các tài liệu kiểm thử chuyên biệt của từng phân hệ. 

Quỹ thời gian ước tính cho Sprint 1 rất gấp rút, kéo dài trong ba ngày từ 09/11/2026 đến 11/11/2026, do đó bất kỳ sự chậm trễ nào trong quá trình phát triển mã nguồn Backend và Frontend, thiết lập môi trường PostgreSQL 17 hay cấu hình HTTPS Tunnel phục vụ quét mã QR trên thiết bị thật đều có thể gây ảnh hưởng đáng kể đến tiến độ của Test Plan. Giai đoạn Acceptance Testing dự kiến được thực hiện ngay sau khi hoàn tất System & Integration Testing và chốt Feature Freeze vào lúc 18 giờ ngày 10/11/2026, diễn ra song song với quá trình rà soát lỗi cuối cùng và diễn tập quy trình thực tế trước buổi nghiệm thu Sprint 1 Review vào ngày 11/11/2026. 

# **2.Thành phần kiểm thử** 

Dưới đây là danh sách các hạng mục phần mềm, thư viện lõi và tài liệu đặc tả kèm phiên bản cần được kiểm thử trong phạm vi Sprint 1 của hệ thống QNU-Events: 

A. Ứng dụng QNU Events Backend, artifact vn.edu.qnu:qnu-events, phiên bản mã nguồn 0.0.1-SNAPSHOT, bản phát hành Sprint 1 v0.1.0-sprint1. Nếu có bản cập nhật nâng cấp lên Sprint 2 trước khi kết thúc nghiệm thu thì các tính năng mới đó sẽ không được đưa vào phạm vi kiểm thử lần này mà tách thành chu kỳ kiểm thử riêng. 

B. Nền tảng Runtime và Framework Backend gồm Java 21, Spring Boot 4.1.2, Spring Security OAuth2 Resource Server, Springdoc OpenAPI 3.2.0 và thư viện mã hóa QR ZXing 3.5.4. Mọi phiên bản thư viện được khóa cố định trong tệp pom.xml; nếu có bản phát hành mới sẽ không nâng cấp trong Sprint 1 mà chuyển sang kế hoạch bảo trì sau cài đặt. 

C. Hệ quản trị cơ sở dữ liệu PostgreSQL 17 kết hợp Flyway Migration phiên bản kịch bản V1__sprint1_core_schema.sql. Toàn bộ danh sách bảng dữ liệu, chỉ mục duy nhất, ràng buộc CHECK và trigger bất biến được cố định theo thiết kế; mọi thay đổi cấu trúc sau khi hợp nhất phải tạo tệp migration V2 trở đi. 

D. Ứng dụng QNU Events Frontend, tên gói qnu-events-fe, phiên bản khởi tạo 0.0.0 cho Sprint 1 MVP, xây dựng trên React 19.3.1, React Router 7.18.5, TanStack React Query 5.107.1, TypeScript 7.0.2 và Vite 6.1.2. Danh sách chi tiết các màn hình, thành phần giao diện và luồng điều hướng được quy định trong tài liệu thiết kế giao diện. 

E. Bộ thư viện xử lý QR phía giao diện gồm qrcode.react phiên bản 4.2.0 phục vụ trình chiếu mã QR động cho Ban tổ chức và html5-qrcode phiên bản 2.3.8 phục vụ quét mã bằng camera trên trình duyệt di động của Sinh viên. 

F. Tài liệu Đặc tả Yêu cầu Phần mềm và Đề xuất Dự án gồm QNU_Event_SRS.docx và QNU_Event_Participation_Management_Project_Proposal_v2.docx làm cơ sở truy vết các Use Case, Functional Requirements và Non-Functional Requirements. 

# **3.Các vấn đề rủi ro trong phần mềm** 

Có một số thành phần hạ tầng và môi trường nằm ngoài tầm kiểm soát trực tiếp của mã nguồn nghiệp vụ QNU-Events nhưng lại tác động trực tiếp đến sự ổn định của quy trình vận hành và bắt buộc phải được kiểm tra kỹ lưỡng: 

A. Hệ quản trị cơ sở dữ liệu PostgreSQL 17 chạy trên Docker Engine và WSL2. Thành phần này trực tiếp đảm nhiệm việc thực thi kịch bản Flyway Migration V1, xử lý khóa dòng SELECT FOR UPDATE, câu lệnh nguyên tử INSERT ON CONFLICT và kích hoạt trigger trg_audit_logs_immutable. Bất kỳ sai khác nào về phiên bản hoặc cấu hình kết nối cơ sở dữ liệu đều có thể làm gián đoạn cơ chế chống tranh chấp suất cuối và tính toàn vẹn của dữ liệu. 

B. Trình duyệt di động, phần cứng camera trên thiết bị của sinh viên và đường truyền HTTPS Tunnel qua cloudflared. Thư viện html5-qrcode chỉ được cấp quyền truy cập camera khi chạy trong ngữ cảnh bảo mật HTTPS hoặc localhost và phụ thuộc vào trình duyệt Android Chrome hoặc iOS Safari. Do đó, cần kiểm thử song song cả luồng quét 

camera thực tế lẫn luồng nhập chuỗi token dự phòng để đảm bảo quá trình điểm danh không bị tắc nghẽn khi thiết bị gặp sự cố phần cứng hoặc trình duyệt chặn quyền. 

C. Độ trễ mạng và sự sai lệch đồng hồ hệ thống giữa máy chủ Backend, màn hình trình chiếu QR của Ban tổ chức và điện thoại của Sinh viên. Vì thuật toán Dynamic QR sử dụng chu kỳ xoay vòng 10 giây kết hợp ân hạn 3 giây dựa trên java.time.Clock của máy chủ, mọi sự chênh lệch thời gian hoặc mạng chập chờn đều có thể gây từ chối nhầm mã QR hợp lệ. Cần kiểm tra kỹ cơ chế bù lệch thời gian qua trường serverTime trên Frontend và xử lý biên ân hạn trên Backend. 

D. Khả năng phục hồi giao dịch, hoàn tác dữ liệu và chống trùng lặp khi hệ thống hoặc kết nối mạng gặp sự cố giữa chừng. Đây là yếu tố sống còn đối với độ tin cậy của luồng đăng ký giữ suất và điểm danh: khi đăng ký thất bại do hết suất CAPACITY_FULL, giao dịch phải hoàn tác toàn bộ để không để lại bản ghi mồ côi trong bảng registrations; đồng thời khi sinh viên gửi lại yêu cầu điểm danh do rớt mạng với cùng khóa Idempotency-Key, máy chủ phải trả về nguyên trạng kết quả đã lưu trong attendance_attempts mà không trừ vào giới hạn 3 lần thử sai. 

E. Bảo mật cơ sở dữ liệu, quản lý khóa bí mật và kiểm soát truy cập đối với các bảng dùng chung. Các thông số nhạy cảm như JWT_SECRET, QR_SIGNING_SECRET và DEMO_PASSWORD phải được nạp qua biến môi trường, mật khẩu phải băm bằng BCrypt cost factor 12, bộ lọc Spring Security phải giữ quy tắc chặn mặc định denyAll ở cuối chuỗi lọc, và bảng audit_logs phải được bảo vệ ở tầng cơ sở dữ liệu nhằm ngăn chặn mọi thao tác UPDATE hoặc DELETE trái phép. 

# **4.Tính năng cần kiểm thử** 

# **4.1. Xác thực & quản lý phiên đăng nhập** 

Phạm vi kiểm thử: 

- Backend: AuthController, AuthService, JwtService, SessionJwtValidator, DemoDataSeeder, SecurityConfiguration và bảng auth_session và các Endpoint API 

- Frontend: LoginPage, AuthContext và thành phần bảo vệ định tuyến RequireRole. 

Nội dung kiểm thử: 

Unit Testing và White-box Testing: 

- Kiểm tra hàm băm mật khẩu BCryptPasswordEncoder với cost factor 12, đảm bảo mật khẩu lưu trong cơ sở dữ liệu bắt đầu bằng tiền tố mã hóa hợp lệ và không trùng với chuỗi gốc. 

- Kiểm tra JwtService tạo mã JWT HS256 chứa đầy đủ các thông tin sub, sid, roles, classId và thời gian hết hạn exp đúng bằng 8 giờ tính từ MutableClock của máy chủ. 

- Kiểm tra nhánh xử lý khi tài khoản không tồn tại trong AuthService, đảm bảo hệ thống vẫn thực hiện so khớp BCrypt với chuỗi DUMMY_HASH để cân bằng thời gian phản hồi và chống dò quét tên đăng nhập. 

- Kiểm tra cấu hình phân quyền tĩnh trong SecurityConfigurationTest, xác nhận quy tắc chặn mặc định denyAll ở cuối chuỗi lọc và vai trò SYSTEM_ADMIN bị từ chối với mã 403 khi truy cập các endpoint nghiệp vụ chưa mở. 

Integration Testing trên cơ sở dữ liệu PostgreSQL 17: 

- Kiểm tra DemoDataSeeder khởi tạo đầy đủ và lặp lại an toàn các tài khoản mẫu gồm owner01, sv001, sv002, sv003 và admin01 khi chạy ở profile dev hoặc demo. 

- Kiểm tra luồng POST /api/v1/auth/login thành công với owner01 và sv001, xác nhận hệ thống trả về HTTP 200 kèm accessToken và tạo mới một bản ghi tương ứng trong bảng auth_session. 

- Kiểm tra luồng POST /api/v1/auth/logout trả về HTTP 204 và cập nhật trường thời gian thu hồi revoked_at trong bảng auth_session ngay lập tức. 

- Kiểm tra SessionJwtValidator từ chối mã JWT đã đăng xuất với HTTP 401 và mã lỗi SESSION_REVOKED, đồng thời từ chối phiên đã quá hạn hoặc người dùng có trạng thái LOCKED. 

- Kiểm tra GET /api/v1/auth/me trả về đúng thông tin định danh từ JWT đang hoạt động và trả về HTTP 401 UNAUTHENTICATED theo chuẩn ApiError khi thiếu token. 

## Black-box Testing và Security Testing: 

- Kiểm tra đầu vào rỗng, thiếu trường loginId hoặc password khi gọi đăng nhập, xác nhận hệ thống trả về HTTP 400 VALIDATION_FAILED kèm chi tiết trường lỗi. 

- Kiểm tra đăng nhập sai mật khẩu và đăng nhập với tài khoản không tồn tại, xác nhận cả hai trường hợp đều trả về cùng mã HTTP 401 INVALID_CREDENTIALS và cùng thông báo tiếng Việt chung nhằm không làm lộ thông tin tài khoản tồn tại. 

- Kiểm tra tài khoản ở trạng thái LOCKED khi đăng nhập sẽ bị chặn với HTTP 403 FORBIDDEN. 

- Kiểm tra ma trận phân quyền RBAC trên các endpoint: tài khoản STUDENT gọi API tạo sự kiện của Ban tổ chức bị chặn 403 FORBIDDEN; ngược lại tài khoản EVENT_OWNER gọi API đăng ký giữ suất hoặc xem lịch sử của Sinh viên cũng bị chặn 403 FORBIDDEN. 

End-to-End Testing trên giao diện Frontend: 

- Kiểm tra người dùng chưa đăng nhập khi truy cập các đường dẫn nội bộ sẽ tự động bị điều hướng về trang /login. 

- Kiểm tra nhập sai mật khẩu trên giao diện LoginPage sẽ hiển thị thông báo lỗi tiếng Việt thân thiện ngay trên biểu mẫu. 

- Kiểm tra đăng nhập thành công với tài khoản sinh viên sv001 và cố tình truy cập đường dẫn /events/new của Ban tổ chức sẽ bị thành phần RequireRole chặn lại. 

# **4.2. Quản lý sự kiện, xem trước và công bố sự kiện** 

Phạm vi kiểm thử: 

- Backend gồm EventController, EventService, EventValidator, ClassController, thực thể Event với khóa lạc quan @Version và các bảng events, event_audience, org_class. 

- Frontend gồm EventFormPage tại đường dẫn /events/new và /events/:id/edit, EventDetailPage, EventsPage và hộp thoại xác nhận ConfirmDialog. 

Nội dung kiểm thử chi tiết theo từng loại hình: 

Unit Testing và White-box Testing: 

- Kiểm tra logic kiểm định dữ liệu đầu vào trong EventValidator đối với các trường hợp sai mốc thời gian như endAt nhỏ hơn hoặc bằng startAt, attendanceCloseAt nhỏ hơn hoặc bằng attendanceOpenAt và registrationDeadline vượt quá attendanceCloseAt. 

- Kiểm tra các nhánh điều kiện chuyển đổi trạng thái trong EventService từ DRAFT sang PUBLISHED, đảm bảo trường previewed_version được gán đúng bằng version hiện tại khi gọi xem trước và kiểm tra khớp phiên bản khi công bố. 

- Kiểm tra cấu trúc phản hồi lỗi chi tiết details.fields trong GlobalExceptionHandler khi dữ liệu EventUpsert vi phạm ràng buộc kiểm định. 

Boundary Value Analysis và Black-box Testing: 

- Kiểm tra giá trị biên độ dài tiêu đề title: từ chối chuỗi 2 ký tự dưới biên và 201 ký tự trên biên với HTTP 400 VALIDATION_FAILED; chấp nhận chuỗi đúng 3 ký tự và đúng 200 ký tự. 

- Kiểm tra giá trị biên chỉ tiêu quotaTotal: từ chối giá trị 0 bằng HTTP 400 VALIDATION_FAILED và chấp nhận giá trị tối thiểu bằng 1. 

- Kiểm tra giá trị biên điểm rèn luyện activityPoints với giá trị âm bị từ chối và giá trị bằng 0 được chấp nhận, đồng thời từ chối danh sách lớp đối tượng audienceClassIds rỗng hoặc chứa mã lớp không tồn tại. 

- Kiểm tra các luồng từ chối công bố sự kiện: chưa xác nhận confirm bằng true trả về HTTP 400; công bố khi chưa xem trước trả về HTTP 409 PREVIEW_REQUIRED; chỉnh sửa nội dung sau khi xem trước rồi công bố bằng previewVersion cũ trả về HTTP 409 PREVIEW_OUTDATED. 

Integration Testing trên cơ sở dữ liệu PostgreSQL 17: 

- Kiểm tra API GET /api/v1/classes trả về danh sách đầy đủ các lớp học từ bảng org_class cho vai trò EVENT_OWNER. 

- Kiểm tra POST /api/v1/events tạo thành công sự kiện ở trạng thái DRAFT với version khởi tạo bằng 0, gắn đúng owner_id từ JWT và lưu đầy đủ bản ghi liên kết trong bảng event_audience. 

- Kiểm tra PUT /api/v1/events/{id} cập nhật bản nháp thành công khi khớp version, tăng version lên 1 và trả về HTTP 409 VERSION_CONFLICT khi hai yêu cầu cập nhật dùng cùng một version cũ. 

- Kiểm tra tính toàn vẹn quyền sở hữu: tài khoản STUDENT gọi API tạo hoặc sửa sự kiện bị chặn HTTP 403 FORBIDDEN; Ban tổ chức B cố tình sửa hoặc công bố sự kiện của Ban tổ chức A cũng bị chặn HTTP 403 FORBIDDEN. 

- Kiểm tra chặn chỉnh sửa bản ghi sự kiện đã ở trạng thái PUBLISHED với HTTP 409, đồng thời xác minh gọi công bố lặp lại lần hai trên sự kiện đã PUBLISHED trả về HTTP 200 an toàn mà không ghi trùng nhật ký kiểm toán. 

- Kiểm tra bảng audit_logs ghi nhận đúng hành động EVENT_PUBLISH khi công bố thành công và EVENT_PUBLISH_DENIED khi bị từ chối quyền. 

End-to-End Testing và UAT trên giao diện Frontend: 

- Kiểm tra Playwright E2E với tài khoản owner01 thấy tiêu đề quản lý sự kiện, nút Tạo sự kiện mới, mở biểu mẫu /events/new và chọn thành công lớp đối tượng K49CNTT1 nạp từ API. 

- Kiểm tra nhập sai mốc thời gian trên giao diện EventFormPage sẽ hiển thị thông báo lỗi tiếng Việt tương ứng ngay bên dưới từng trường nhập liệu và chuyển đổi đúng múi giờ Asia/Ho_Chi_Minh sang chuẩn UTC. 

- Kiểm tra quy trình Preview-then-Publish trên giao diện EventDetailPage: bấm Xem trước và Công bố để mở hộp thoại ConfirmDialog, chỉ gửi confirm bằng true sau khi người dùng tích chọn xác nhận và chuyển trạng thái hiển thị sang PUBLISHED. 

# **4.3. Danh sách sự kiện** 

Phạm vi kiểm thử: 

- Backend gồm EventController, EventService, EventRepository và các bảng events, event_audience. 

- Frontend gồm EventsPage tại đường dẫn /events, EventDetailPage tại đường dẫn /events/:id và thành phần giao diện StatusBadge cùng EmptyState. 

Nội dung kiểm thử chi tiết theo từng loại hình: 

## White-box Testing và Unit Testing: 

- Kiểm tra logic tính toán động trên thực thể EventSummary và EventDetail tại EventService: remainingQuota luôn bằng giá trị lớn nhất giữa 0 và hiệu số quotaTotal trừ registeredCount; registrationOpen chỉ đúng khi thời gian hiện tại từ MutableClock nhỏ hơn hoặc bằng registrationDeadline và remainingQuota lớn hơn 0; attendanceOpen chỉ đúng khi thời gian hiện tại nằm trong khoảng từ attendanceOpenAt đến attendanceCloseAt. 

- Kiểm tra nhánh phân luồng truy vấn theo vai trò trong EventService: với vai trò STUDENT thì bắt buộc lọc theo điều kiện trạng thái PUBLISHED kết hợp classId thuộc event_audience; với vai trò EVENT_OWNER thì lọc theo ownerId của chính người gọi. 

## Component Testing trên Frontend với Vitest: 

- Kiểm tra StatusBadge.test.tsx xác nhận thành phần StatusBadge hiển thị đồng thời cả nhãn chữ tiếng Việt và biểu tượng icon tương ứng cho từng trạng thái DRAFT, PUBLISHED, ACCEPTED, NOT_CHECKED_IN và REGISTERED, đảm bảo không chỉ dựa vào màu sắc. 

- Kiểm tra EventsPage hiển thị trạng thái rỗng EmptyState với thông báo phù hợp khi danh sách sự kiện trả về rỗng. 

Integration Testing trên cơ sở dữ liệu PostgreSQL 17: 

- Kiểm tra GET /api/v1/events với tài khoản sinh viên sv001 thuộc lớp K49CNTT1 chỉ trả về các sự kiện đang PUBLISHED có gắn lớp K49CNTT1 trong event_audience. 

- Kiểm tra sinh viên sv003 thuộc lớp khác không nhìn thấy sự kiện của lớp K49CNTT1 trong danh sách trả về. 

- Kiểm tra tìm kiếm với tham số q không phân biệt chữ hoa và chữ thường trên tiêu đề sự kiện, trả về đúng tập kết quả phân trang theo tham số page và size. 

- Kiểm tra tài khoản Ban tổ chức owner01 gọi GET /api/v1/events thấy được cả sự kiện DRAFT lẫn PUBLISHED do chính mình tạo, nhưng không thấy sự kiện do Ban tổ chức khác tạo. 

- Kiểm tra trường myRegistrationStatus trên EventDetail phản ánh chính xác trạng thái REGISTERED hoặc null theo tài khoản sinh viên đang đăng nhập. 

Black-box Testing và Security Testing: 

- Kiểm tra truy cập trực tiếp GET /api/v1/events/{id} đối với sự kiện đang ở trạng thái DRAFT bằng tài khoản Sinh viên, xác nhận hệ thống trả về HTTP 404 NOT_FOUND thay vì HTTP 403 để tránh làm lộ thông tin sự kiện có tồn tại hay không. 

- Kiểm tra sinh viên khác lớp gọi trực tiếp GET /api/v1/events/{id} của sự kiện PUBLISHED ngoài phạm vi lớp mình cũng nhận đồng nhất mã HTTP 404 NOT_FOUND nhằm chống dò quét dữ liệu theo UUID. 

- Kiểm tra gọi GET /api/v1/events với UUID không tồn tại trong hệ thống trả về HTTP 404 NOT_FOUND theo chuẩn ApiError. 

End-to-End Testing trên giao diện Frontend: 

- Kiểm tra sau khi đăng nhập bằng tài khoản sinh viên, trang /events hiển thị danh sách thẻ sự kiện đúng lớp và điều hướng sang trang chi tiết /events/:id khi bấm xem chi tiết. 

- Kiểm tra trang chi tiết /events/:id kết xuất đầy đủ thông tin thời gian theo múi giờ Asia/Ho_Chi_Minh, chỉ tiêu còn lại và nhúng thành công hai khu vực chức năng RegistrationSlot cùng AttendanceSlot. 

# **4.4. Đăng ký sự kiện** 

Phạm vi kiểm thử: 

- Backend gồm RegistrationController, RegistrationService, RegistrationRepository, thực thể Registration và hai bảng registrations, events. 

- Frontend gồm thành phần RegistrationSlot trong features/registration cùng bảng ánh xạ thông báo lỗi tiếng Việt. 

Nội dung kiểm thử chi tiết theo từng loại hình: 

White-box Testing: 

- Kiểm tra cấu trúc giao dịch @Transactional với thuộc tính rollbackFor trong RegistrationService, đảm bảo khi câu lệnh UPDATE events SET registered_count = registered_count + 1 WHERE id = ? AND registered_count < quota_total trả về 0 dòng cập nhật thì ngoại lệ ApiException với mã CAPACITY_FULL được ném ra và hoàn tác toàn bộ câu lệnh INSERT ở bước trước đó. 

- Kiểm tra câu lệnh native SQL INSERT INTO registrations ON CONFLICT DO NOTHING RETURNING id chỉ kích hoạt xung đột trên chỉ mục duy nhất 

uq_registrations_active khi trạng thái là REGISTERED hoặc WAITING_LIST, cho phép sinh viên đã có bản ghi CANCELLED được đăng ký lại. 

- Kiểm tra thứ tự ưu tiên kiểm tra điều kiện nghiệp vụ trong RegistrationService trước khi thực hiện ghi dữ liệu: xác thực vai trò STUDENT, trạng thái PUBLISHED của sự kiện, thời hạn registrationDeadline qua MutableClock và lớp của sinh viên thuộc event_audience. 

Decision Table Testing và Black-box Testing: 

- Kiểm tra tổ hợp điều kiện sự kiện ở trạng thái DRAFT, kết quả trả về HTTP 409 EVENT_NOT_PUBLISHED và số lượng registered_count không thay đổi. 

- Kiểm tra tổ hợp điều kiện thời gian hiện tại vượt quá registrationDeadline bằng MutableClock, kết quả trả về HTTP 409 REGISTRATION_CLOSED. 

- Kiểm tra tổ hợp điều kiện sinh viên thuộc lớp ngoài danh sách event_audience, kết quả trả về HTTP 403 NOT_IN_AUDIENCE. 

- Kiểm tra tổ hợp điều kiện sinh viên đã đăng ký thành công trước đó và gửi lại yêu cầu đăng ký, kết quả trả về HTTP 200 OK với alreadyRegistered bằng true và không trừ thêm chỉ tiêu. 

- Kiểm tra tổ hợp điều kiện chỉ tiêu đã đầy, kết quả trả về HTTP 409 CAPACITY_FULL. 

- Kiểm tra gọi API đăng ký với vai trò EVENT_OWNER hoặc chưa đăng nhập, kết quả lần lượt bị chặn với HTTP 403 FORBIDDEN và HTTP 401 UNAUTHENTICATED. 

Component Testing trên Frontend với Vitest: 

- Kiểm tra RegistrationSlot.test.tsx xác nhận hiển thị đúng tiến trình chỉ tiêu đã giữ trên tổng chỉ tiêu, hiển thị nút Đăng ký giữ chỗ cho sinh viên và chuyển sang trạng thái hết chỗ khi remainingQuota bằng 0. 

- Kiểm tra khóa nút bấm ở trạng thái disabled trong lúc request đang chờ phản hồi từ máy chủ nhằm ngăn người dùng bấm đúp liên tiếp. 

- Kiểm tra chỉ hiển thị huy hiệu Đã đăng ký sau khi API trả kết quả thành công và ánh xạ đúng thông báo lỗi tiếng Việt theo từng mã code trả về như CAPACITY_FULL, NOT_IN_AUDIENCE hoặc REGISTRATION_CLOSED. 

Integration Testing trên cơ sở dữ liệu PostgreSQL 17: 

- Kiểm tra POST /api/v1/events/{id}/registrations tạo thành công bản ghi mới trong bảng registrations với HTTP 201 Created và tăng registered_count trong bảng events lên 1. 

- Kiểm tra GET /api/v1/events/{id}/registrations/me trả về HTTP 200 kèm trạng thái REGISTERED và thời gian đăng ký nếu sinh viên đã giữ chỗ, hoặc trả về HTTP 404 NOT_FOUND nếu chưa đăng ký. 

- Kiểm tra khẳng định không có bản ghi mồ côi nào tồn tại trong bảng registrations sau khi giao dịch đăng ký thất bại do CAPACITY_FULL bằng cách đếm trực tiếp số dòng trong cơ sở dữ liệu. 

- Kiểm tra bảng audit_logs ghi nhận đầy đủ hành động REGISTRATION_CREATE khi giữ suất thành công và REGISTRATION_REJECTED khi bị từ chối. 

Concurrency Testing và Stress Testing trên PostgreSQL 17: 

Kiểm tra kịch bản tranh suất cuối cùng sử dụng ExecutorService và CountDownLatch với 2 luồng sinh viên hợp lệ cùng lớp bấm đăng ký đồng thời vào sự kiện có quotaTotal bằng 1: kết quả phải có đúng 1 luồng nhận HTTP 201 Created, luồng còn lại nhận HTTP 409 CAPACITY_FULL và registered_count trong cơ sở dữ liệu bằng chính xác 1. 

Kiểm tra kịch bản tranh chấp tải cao với 30 luồng sinh viên đồng thời tranh 5 suất: kết quả phải có đúng 5 bản ghi REGISTERED, 25 luồng nhận lỗi CAPACITY_FULL và registered_count không bao giờ vượt quá 5. 

Kiểm tra kịch bản gửi lặp đồng thời với 10 luồng song song của cùng 1 tài khoản sinh viên: hệ thống chỉ tạo duy nhất 1 bản ghi trong bảng registrations, chỉ tăng registered_count lên 1 và 9 luồng còn lại nhận phản hồi an toàn mà không trừ thừa chỉ tiêu. 

# **4.5. Điểm danh Dynamic QR** 

Phạm vi kiểm thử: 

- Backend gồm AttendanceController, AttendanceService, AttendanceAttemptService, QrTokenService, các thực thể Attendance, AttendanceAttempt và hai bảng attendances, attendance_attempts. 

- Frontend gồm màn hình trình chiếu EventQrPage tại đường dẫn /events/:id/qr dùng thư viện qrcode.react và thành phần AttendanceSlot trong features/attendance dùng thư viện html5-qrcode. 

Nội dung kiểm thử chi tiết theo từng loại hình: 

Unit Testing và White-box Testing trên QrTokenServiceTest: 

- Kiểm tra phương thức issue của QrTokenService sinh chuỗi token đúng cấu trúc bốn phần qr1, eventId, epoch và chữ ký base64url HMAC-SHA256 độc lập cho từng sự kiện ngay cả khi tạo trong cùng một epoch. 

- Kiểm tra phép so sánh chữ ký trong QrTokenService sử dụng hàm thời gian hằng MessageDigest.isEqual nhằm chống tấn công đo thời gian Timing Attack. 

- Kiểm tra nhánh trả kết quả CheckInResult trong AttendanceService không ném ngoại lệ khi từ chối mã QR để bản ghi đếm lượt sai trong bảng attendance_attempts luôn được commit an toàn. 

- Kiểm tra thứ tự ưu tiên xử lý trong AttendanceService: kiểm tra trùng Idempotency-Key trước, tiếp theo kiểm tra cửa sổ mở điểm danh, khóa dòng đăng ký bằng SELECT FOR UPDATE, kiểm tra đã điểm danh trước đó, kiểm tra giới hạn 3 lần thử sai và cuối cùng mới xác thực chữ ký QR. 

Equivalence Partitioning và Boundary Value Analysis: 

- Kiểm tra phân vùng token hợp lệ thuộc epoch hiện tại và phân vùng token thuộc epoch ngay trước đó khi thời gian lệch vẫn nằm trong biên ân hạn 3 giây. 

- Kiểm tra giá trị biên thời gian ân hạn bằng MutableClock: ở mốc lệch 2.9 giây nhỏ hơn grace period thì chấp nhận hợp lệ; đúng tại mốc lệch 3.0 giây bằng hoặc lớn hơn grace period thì từ chối với mã HTTP 422 QR_EXPIRED. 

- Kiểm tra các phân vùng token không hợp lệ gồm chuỗi sai định dạng 4 phần hoặc epoch không phải số trả về QR_MALFORMED; token bị sửa chữ ký trả về QR_BAD_SIGNATURE; token của sự kiện B đem quét tại sự kiện A trả về QR_WRONG_EVENT; token mang epoch tương lai trả về QR_FROM_FUTURE. 

- Kiểm tra giá trị biên số lần thử sai: các lần gửi mã QR sai thứ 1, 2 và 3 đều trả về HTTP 422 kèm counted bằng true; đến lần thử thứ 4, kể cả khi gửi mã QR hợp lệ, hệ thống lập tức chặn bằng HTTP 409 ATTEMPT_LIMIT_REACHED. 

Integration Testing và Security Testing trên cơ sở dữ liệu PostgreSQL 17: 

- Kiểm tra GET /api/v1/events/{id}/qr chỉ cho phép EVENT_OWNER là chủ sự kiện gọi khi sự kiện đang PUBLISHED và nằm trong cửa sổ attendanceOpenAt đến attendanceCloseAt; tài khoản STUDENT gọi bị chặn 403 FORBIDDEN; gọi ngoài cửa sổ điểm danh bị chặn 409 ATTENDANCE_NOT_OPEN. 

- Kiểm tra POST /api/v1/events/{id}/attendance/qr bắt buộc phải có header Idempotency-Key; khi gửi lại đúng Idempotency-Key cũ thì trả về nguyên trạng response_body đã lưu mà không tăng số lần attempt. 

- Kiểm tra các điều kiện tiên quyết không tính vào giới hạn 3 lần thử sai: gọi điểm danh khi chưa đăng ký trả về 403 NOT_REGISTERED; gọi ngoài cửa sổ thời gian trả về 409 ATTENDANCE_NOT_OPEN; gọi điểm danh lại bằng Idempotency-Key mới sau khi đã ACCEPTED trả về 409 ALREADY_CHECKED_IN. 

   - Kiểm tra bảo mật dữ liệu: trường response_body trong bảng attendance_attempts và trường details trong bảng audit_logs tuyệt đối không lưu chuỗi token QR đầy đủ mà chỉ lưu mã lỗi hoặc tiền tố hash 8 ký tự. 

   - Kiểm tra bảng audit_logs ghi nhận đầy đủ hai hành động ATTENDANCE_QR_ACCEPT và ATTENDANCE_QR_REJECT. 

- Concurrency Testing trên PostgreSQL 17: 

   - Kiểm tra kịch bản 10 luồng song song của cùng một sinh viên gửi yêu cầu điểm danh hợp lệ đồng thời với các khóa khác nhau hoặc giống nhau: kết quả chỉ tạo duy nhất 1 bản ghi ACCEPTED trong bảng attendances nhờ chỉ mục duy nhất uq_attendances_valid kết hợp khóa tuần tự SELECT FOR UPDATE. 

End-to-End Testing và UAT trên thiết bị di động: 

- Kiểm tra Playwright E2E trên màn hình EventQrPage xác nhận hiển thị mã QR khổ lớn qua QRCodeSVG, đồng hồ đếm ngược chu kỳ 10 giây và tự động làm mới trước mốc validUntil có bù trừ độ lệch đồng hồ qua serverTime. 

- Kiểm tra trên giao diện AttendanceSlot khóa nút bấm khi đang gửi yêu cầu, tự động tái sử dụng cùng một Idempotency-Key khi thử lại do lỗi mạng và chỉ hiển thị huy hiệu Đã điểm danh sau khi API trả về ACCEPTED. 

- Kiểm tra UAT trên điện thoại thật qua kết nối HTTPS Tunnel cloudflared cho luồng quét Camera bằng html5-qrcode, đồng thời kiểm chứng luồng dự phòng qua ô dán chuỗi token khi trình duyệt từ chối quyền Camera. 

# **4.6. Tra cứu lịch sử tham gia sự kiện** 

Phạm vi kiểm thử: 

- Backend gồm HistoryController, HistoryService, DTO HistoryItem và dữ liệu kết nối từ các bảng registrations, attendances và events. 

- Frontend gồm trang HistoryPage tại đường dẫn /history cùng các thành phần StatusBadge và EmptyState. 

Nội dung kiểm thử chi tiết theo từng loại hình: 

White-box Testing và Unit Testing: 

- Kiểm tra phương thức getHistory trong HistoryController chỉ trích xuất định danh sinh viên từ CurrentUserProvider qua JWT của phiên đăng nhập hiện tại và tuyệt đối không tiếp nhận tham số studentId từ client. 

- Kiểm tra câu truy vấn trong HistoryService thực hiện kết nối dữ liệu từ registrations với các bản ghi attendances có trạng thái ACCEPTED và bảng events, đồng thời sắp xếp kết quả theo thứ tự thời gian bắt đầu startAt giảm dần. 

- Kiểm tra ánh xạ trạng thái điểm danh: khi sinh viên đã đăng ký nhưng chưa có bản ghi điểm danh ACCEPTED thì trả về attendanceStatus là NOT_CHECKED_IN và attendanceAt là null; khi đã điểm danh hợp lệ thì trả về ACCEPTED kèm thời điểm điểm danh do máy chủ ghi nhận và giá trị activityPoints. 

Component Testing trên Frontend với Vitest: 

- Kiểm tra HistoryPage.test.tsx xác nhận hiển thị chính xác các thẻ tổng kết KPI gồm tổng số sự kiện đã đăng ký, số sự kiện đã điểm danh và tổng điểm rèn luyện tích lũy. 

- Kiểm tra giao diện hiển thị trạng thái rỗng EmptyState với thông báo Bạn chưa đăng ký sự kiện nào khi danh sách trả về rỗng, đồng thời hiển thị nút thử lại khi API gặp lỗi kết nối. 

Integration Testing và Security Testing trên cơ sở dữ liệu PostgreSQL 17: 

- Kiểm tra GET /api/v1/me/history trả về danh sách rỗng HTTP 200 OK với mảng [] đối với sinh viên chưa từng đăng ký sự kiện nào. 

- Kiểm tra sau khi sinh viên đăng ký giữ suất thành công nhưng chưa quét mã QR, API trả về đúng sự kiện đó với registrationStatus là REGISTERED và attendanceStatus là NOT_CHECKED_IN. 

- Kiểm tra ngay sau khi sinh viên hoàn tất điểm danh QR thành công, API cập nhật sang trạng thái ACCEPTED kèm dấu thời gian chính xác từ máy chủ và dữ liệu vẫn được bảo toàn khi tải lại trang. 

- Kiểm tra phân lập dữ liệu và bảo mật IDOR: sinh viên A gọi GET /api/v1/me/history chỉ nhận được dữ liệu của chính mình và tuyệt đối không nhìn thấy lịch sử đăng ký hay điểm danh của sinh viên B; tài khoản EVENT_OWNER hoặc SYSTEM_ADMIN gọi vào endpoint này bị chặn bằng HTTP 403 FORBIDDEN. 

## End-to-End Testing trên giao diện Frontend: 

- Kiểm tra Playwright E2E với tài khoản sinh viên truy cập đường dẫn /history hiển thị đầy đủ danh sách sự kiện đã tham gia, huy hiệu trạng thái chữ kết hợp biểu tượng và điểm rèn luyện tích lũy chính xác. 

- Kiểm tra thao tác tải lại trang F5 trên trình duyệt vẫn giữ nguyên trạng thái lịch sử đã điểm danh từ dữ liệu máy chủ. 

# **5.Tính năng không kiểm thử** 

A. Các phương thức điểm danh mở rộng và điểm danh ngoại tuyến gồm GPS, Evidence và Offline Queue. Trong Sprint 1, hệ thống chỉ triển khai và kiểm thử luồng điểm danh trực tuyến qua Dynamic QR. Các phân hệ evidence, kiểm tra tọa độ địa lý GPS và đồng bộ dữ liệu ngoại tuyến chưa được cài đặt ở tầng nghiệp vụ nên chỉ được kiểm chứng gián tiếp về mặt ràng buộc lược đồ bảng trong Flyway Migration V1 và không chịu tác động từ các thay đổi của Sprint 1. 

B. Quy trình đăng ký nâng cao gồm Waiting List, hủy đăng ký sự kiện và chế tài khóa tài khoản khi vắng mặt No-show. Theo quyết định cắt phạm vi Sprint 1, khi sự kiện hết chỉ tiêu thì hệ thống từ chối trực tiếp với mã CAPACITY_FULL thay vì đưa vào danh sách chờ QE-39. Các thao tác tự hủy giữ suất hay xử phạt vắng mặt không nằm trong phạm vi phát hành lần này và sẽ được kiểm thử độc lập ở Sprint sau. 

C. Luồng khiếu nại điểm danh Appeal cùng quy trình phê duyệt của vai trò CLASS_REPRESENTATIVE và Cố vấn học tập. Gói mã nguồn appeal hiện tại chỉ mới dừng ở mức cấu trúc gói rỗng, chưa có API hay màn hình giao diện xử lý và chưa mở quyền truy cập cho hai vai trò trung gian này, do đó không thực hiện kiểm thử chức năng trong Sprint 1. 

D. Tích hợp xác thực bên thứ ba qua Google OAuth2 và tính năng nhập danh sách tài khoản hàng loạt từ tệp Excel hoặc CSV. Mặc dù thư viện spring-boot-starter-securityoauth2-client và Apache POI đã được khai báo trong tệp cấu hình dự án, Sprint 1 chỉ kiểm thử luồng đăng nhập nội bộ qua app_user và dữ liệu mẫu DemoDataSeeder. Các luồng SSO và import hàng loạt sẽ được kiểm thử riêng khi tính năng hoàn thiện. 

E. Giao diện tra cứu Audit Log dành cho SYSTEM_ADMIN và kiểm thử tải quy mô lớn QE-46. Tính đúng đắn của bảng audit_logs trong Sprint 1 chỉ được kiểm chứng gián tiếp thông qua các luồng Publish, Registration, QR Check-in và trigger bất biến dưới cơ sở dữ liệu; màn hình tra cứu của quản trị viên cùng bộ kịch bản kiểm thử chịu tải diện rộng bằng k6 hoặc JMeter sẽ được triển khai ở giai đoạn sau. 

# **6.Phương pháp kiểm thử** 

# **6.1. Các cấp độ kiểm thử** 

Unit & Component Testing được thực hiện trực tiếp bởi từng Developer phụ trách phân hệ và phải được Tech Lead A0 rà soát, phê duyệt thông qua Pull Request trước khi hợp nhất vào nhánh develop 

System & Integration Testing được thực hiện bởi Tester và sự hỗ trợ của các Developer từng phân hệ khi cần thiết. Hệ thống sử dụng bộ công cụ kiểm thử tự động trên cơ sở dữ 

liệu PostgreSQL 17 thật gồm Spring Boot MockMvc, TestUsers, TestAuth, ExecutorService và CountDownLatch để kiểm tra đồng thời API Integration, Security Testing và Concurrency Testing. Các phân hệ chỉ được phép bước vào giai đoạn System & Integration Testing sau khi toàn bộ lỗi mức Critical đã được khắc phục hoàn toàn 

Acceptance Testing được thực hiện bởi Tester với sự giám sát của Scrum Master thông qua bộ kiểm thử tự động Playwright End-to-End Testing kết hợp buổi kiểm thử chấp nhận. Một tính năng chỉ được đưa vào Acceptance Testing sau khi toàn bộ lỗi Critical và phần lớn lỗi Major ở giai đoạn System & Integration Testing đã được sửa chữa 

# **6.2. Quản lý Cấu hình và Kiểm soát Thay đổi** 

Việc luân chuyển mã nguồn từ môi trường phát triển sang môi trường kiểm thử tích hợp và môi trường trình diễn chính thức trong dự án QNU-Events được kiểm soát chặt chẽ thông qua quy trình Git Branching Workflow, GitHub Actions CI và hệ thống Pull Request. Mọi tính năng mới đều phải phát triển trên nhánh riêng theo cú pháp feature/QE-xx hoặc chore/QE-xx rẽ từ nhánh develop, sử dụng chuẩn Conventional Commits có gắn mã Jira key. Chỉ những Pull Request vượt qua kiểm tra tự động của CI và nhận được phê duyệt của ít nhất một Reviewer theo phân quyền CODEOWNERS mới được phép hợp nhất vào nhánh develop 

Toàn bộ Unit Testing và System & Integration Testing ban đầu được thực thi trên môi trường máy trạm cục bộ và GitHub Actions sử dụng cơ sở dữ liệu PostgreSQL 17 độc lập với dữ liệu khởi tạo riêng cho từng bài kiểm thử qua TestUsers và TestAuth. 

# **6.3. Công cụ test** 

A. Git, GitHub Pull Request, CODEOWNERS và GitHub Actions CI được sử dụng làm công cụ quản lý cấu hình phiên bản mã nguồn, kết hợp cơ chế kiểm soát phê duyệt trước khi hợp nhất nhánh nhằm bảo đảm tính nhất quán giữa môi trường phát triển và môi trường kiểm thử. 

B. Figma với tệp thiết kế jVs22zFVUQjDbYQ5d0pwmU được sử dụng để xây dựng nguyên mẫu giao diện ban đầu, giúp đối chiếu bố cục và nội dung các màn hình với bộ phận nghiệp vụ trước khi tiến hành lập trình và kiểm thử giao diện trên React 19. 

C. Bộ công cụ phát triển, biên dịch và kiểm thử tự động lõi gồm Maven Wrapper, Java 21, JUnit 5, Spring Boot Test MockMvc và AssertJ cho phía Backend; kết hợp cùng Vite, TypeScript, oxlint, Vitest, React Testing Library và Playwright cho việc kiểm tra tĩnh, gỡ lỗi và kiểm thử tự động phía Frontend. 

D. Bộ công cụ kiểm thử chuyên dụng bổ sung gồm Postman, Selenium và k2. Trong đó, Postman được sử dụng để kiểm thử nhanh các REST API endpoint theo hợp đồng contract-v1, kiểm tra mã lỗi ApiError và xác thực JWT; Selenium được sử dụng để hỗ trợ kiểm thử tự động hóa thao tác trình duyệt trên các luồng giao diện trọng tâm bên cạnh Playwright; và k2 được sử dụng để hỗ trợ kiểm thử hiệu năng, giả lập tải truy cập đồng thời vào các điểm nóng như đăng ký giữ suất và điểm danh QR. 

E. Công cụ quản lý cơ sở dữ liệu và tạo dữ liệu kiểm thử gồm PostgreSQL 17 trên Docker WSL2, Flyway Migration V1, psql, DBeaver, DemoDataSeeder, TestUsers, TestAuth và MutableClock. Dữ liệu kiểm thử được khởi tạo cô lập hoặc nhân bản từ bộ dữ liệu mẫu để mô phỏng nhiều sinh viên đăng ký tranh suất đồng thời qua ExecutorService và CountDownLatch, bảo đảm tuyệt đối không gây ảnh hưởng hay làm sai lệch dữ liệu trên môi trường trình diễn chính thức. 

# **7.Chiến lược kiểm thử** 

# **7.1. Nguyên tắc định hướng kiểm thử** 

Các bài automated test được tập trung sâu nhất vào hai luồng có rủi ro cao nhất. Đó là luồng đăng ký giữ suất ở QE-71, vốn cần đảm bảo tính nguyên tử để tránh tranh chấp suất cuối, và luồng xác thực mã Dynamic QR HMAC-SHA256 ở QE-56 với giới hạn 3 lần thử sai cùng cơ chế Idempotency. 

Ở phía Backend, toàn bộ Integration Testing và Concurrency Testing đều chạy trực tiếp trên PostgreSQL 17 thật thay vì database giả lập trên RAM. Cách chạy này giúp kiểm chứng đúng hành vi của câu lệnh native INSERT ON CONFLICT, khóa dòng SELECT FOR UPDATE, các ràng buộc CHECK và trigger bất biến trg_audit_logs_immutable. 

Để kiểm soát mốc thời gian chủ động, hệ thống không dùng Thread.sleep gây chậm trễ mà nạp MutableClock vào Spring Context. Nhờ vậy, bài test có thể dịch chuyển ngay mốc thời gian trên server để kiểm tra các điểm biên như lúc hết hạn đăng ký, khung giờ mở điểm danh và thời gian ân hạn 3 giây của mã QR. 

# **7.2. Chiến lược thực thi** 

Functional Testing áp dụng các kỹ thuật Black-box Testing gồm Equivalence Partitioning, Boundary Value Analysis và Decision Table Testing để kiểm chứng toàn bộ 15 REST API endpoint và các màn hình giao diện Frontend. Ít nhất 40% tổng số kịch bản kiểm thử tập trung vào các trường hợp biên, dữ liệu sai định dạng và vi phạm trạng thái nghiệp vụ, giúp đảm bảo thông báo lỗi tiếng Việt trả về đúng theo chuẩn ApiError. 

Về Security Testing, các bài kiểm thử rà soát việc băm mật khẩu bằng BCrypt cost factor 12, chống dò quét tài khoản bằng dummy hash và thu hồi ngay AuthSession khi đăng xuất. Hệ thống cũng được kiểm tra kiểm soát truy cập theo vai trò RBAC và chống truy cập ngang IDOR ở các API xem sự kiện hoặc lịch sử cá nhân. Song song đó, nhóm kiểm chứng việc so sánh chữ ký HMAC-SHA256 bằng MessageDigest.isEqual để chống Timing Attack, đảm bảo không lưu mật khẩu, JWT hay chuỗi QR đầy đủ vào bảng audit_logs, và xác minh trigger cơ sở dữ liệu chặn mọi thao tác UPDATE hoặc DELETE trên nhật ký kiểm toán. 

Với Concurrency và Performance Testing, hệ thống dùng ExecutorService kết hợp CountDownLatch để giả lập các đợt truy cập đồng thời cao trực tiếp vào PostgreSQL 17. Các kịch bản gồm 2 luồng tranh 1 suất cuối, 30 luồng tranh 5 suất và 10 luồng gửi lặp của cùng 1 sinh viên, đảm bảo không xảy ra Overbooking hay phát sinh bản ghi mồ côi. Ngoài ra, Postman và k2 cũng được dùng để kiểm tra độ ổn định phản hồi của API dưới tải thử nghiệm. 

Ở giai đoạn End-to-End và UAT Testing, bộ kiểm thử tự động Playwright trên trình duyệt Chromium cho các luồng giao diện chính được kết hợp với buổi kiểm thử chấp nhận UAT theo kịch bản 8 bước. Quá trình UAT chạy trên cả máy tính trình chiếu của Ban tổ chức lẫn điện thoại thật của Sinh viên qua kết nối HTTPS Tunnel cloudflared, nhằm xác minh luồng quét camera bằng html5-qrcode và phương án dán chuỗi token dự phòng. 

# **8.3. Quy trình quản lý lỗi và kiểm thử hồi quy** 

Mọi lỗi phát hiện trong quá trình kiểm thử đều phải được QA kiểm tra log, trích xuất traceId và khởi tạo thành một Jira Bug trong project QE liên kết trực tiếp về User Story tương ứng và hạng mục QE-28. Khi Developer khắc phục lỗi trên nhánh riêng kèm bài kiểm thử tự động tái hiện lỗi đó và hợp nhất Pull Request thành công, QA sẽ thực hiện kiểm thử lại Retest và chạy toàn bộ bộ Regression Testing ở cả Backend lẫn Frontend trước khi đóng Bug. 

