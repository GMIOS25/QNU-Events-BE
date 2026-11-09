## 📌 Thông tin công việc (Work Item)

- **Jira Key:** [QE-...]
- **Loại:** `Feature` / `Bugfix` / `Chore` / `Docs`
- **Mã SRS liên quan:** UC..., REQ-...

---

## 📝 Tóm tắt thay đổi

- Mô tả ngắn gọn những gì đã được thực hiện trong PR này.
- Lý do thay đổi và cách tiếp cận kỹ thuật.

---

## 🧪 Hướng dẫn kiểm thử (Test Instructions)

1. **Điều kiện chuẩn bị:**
   - 
2. **Các bước thực hiện:**
   - Bước 1: 
   - Bước 2: 
3. **Kết quả mong đợi:**
   - 

---

## 📷 Bằng chứng kiểm thử (Evidence)

*(Dán ảnh chụp màn hình terminal `mvnw verify` xanh, kết quả test Swagger hoặc log thực thi)*

---

## ✅ Checklist nghiệm thu (Definition of Done)

- [ ] Lệnh `.\mvnw.cmd -B -ntp verify -Pintegration` chạy thành công không có lỗi.
- [ ] Không sử dụng `@Disabled` hay `-DskipTests`.
- [ ] Không có secret / password / token hardcoded trong mã nguồn.
- [ ] Commit message tuân thủ đúng Conventional Commits kèm `[QE-xx]`.
- [ ] Đã gán đúng Reviewer theo quy định của `.github/CODEOWNERS`.
