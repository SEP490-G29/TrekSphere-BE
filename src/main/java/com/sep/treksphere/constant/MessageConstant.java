package com.sep.treksphere.constant;

public class MessageConstant {
    private MessageConstant() {
    }


    // Auth Messages
    public static final String USER_NOT_FOUND = "Người dùng không tồn tại";
    public static final String USER_NOT_LOGGED_IN = "Người dùng chưa đăng nhập";
    public static final String CURRENT_PASSWORD_INCORRECT = "Mật khẩu hiện tại không chính xác";
    public static final String NEW_PASSWORD_SAME_AS_OLD = "Mật khẩu mới không được trùng với mật khẩu cũ";
    public static final String PASSWORD_CHANGED_SUCCESSFULLY = "Mật khẩu đã được thay đổi thành công.";
    public static final String RESET_LINK_SENT_SUCCESSFULLY = "Nếu email hợp lệ, một đường link đặt lại mật khẩu đã được gửi đến email của bạn.";
    public static final String PASSWORD_RESET_SUCCESSFULLY = "Mật khẩu đã được đặt lại thành công.";
    public static final String REGISTER_SUCCESSFULLY = "Đăng ký thành công. Vui lòng kiểm tra email để xác nhận tài khoản của bạn.";
    public static final String EMAIL_VERIFIED_SUCCESSFULLY = "Xác minh email thành công. Bạn có thể đăng nhập ngay bây giờ.";
    public static final String EMAIL_ALREADY_VERIFIED = "Email đã được xác minh trước đó. Bạn có thể đăng nhập ngay bây giờ.";
    public static final String GOOGLE_LOGIN_SUCCESSFULLY = "Đăng nhập bằng Google thành công.";
    public static final String LOGIN_SUCCESSFULLY = "Đăng nhập thành công.";
    public static final String LOGOUT_SUCCESSFULLY = "Đăng xuất thành công.";
    public static final String INVALID_REFRESH_TOKEN = "Refresh token không hợp lệ hoặc đã hết hạn.";
    public static final String INVALID_GOOGLE_ID_TOKEN = "Google ID token không hợp lệ hoặc đã hết hạn.";

    
    // Validation Messages
    public static final String CURRENT_PASSWORD_REQUIRED = "Vui lòng nhập mật khẩu hiện tại";
    public static final String NEW_PASSWORD_REQUIRED = "Vui lòng nhập mật khẩu mới";
    public static final String PASSWORD_MIN_LENGTH = "Mật khẩu phải có ít nhất 8 ký tự";
    public static final String CONFIRM_PASSWORD_NOT_MATCH = "Mật khẩu xác nhận không khớp";

    // Profile Messages
    public static final String FULL_NAME_REQUIRED = "Họ tên không được để trống";
    public static final String INVALID_PHONE = "Số điện thoại không hợp lệ";
    public static final String INVALID_DOB = "Ngày sinh không hợp lệ (không được lớn hơn ngày hiện tại)";
    public static final String PROFILE_UPDATED_SUCCESSFULLY = "Cập nhật hồ sơ thành công";
    public static final String STATUS_UPDATED_SUCCESSFULLY = "Cập nhật trạng thái người dùng thành công";

    // System Error Messages
    public static final String SYSTEM_ERROR_UNKNOWN = "Lỗi hệ thống không xác định";
    public static final String INVALID_MESSAGE_KEY = "Mã tin nhắn không hợp lệ";
    public static final String VALIDATION_ERROR_MSG = "Dữ liệu đầu vào không hợp lệ";
    public static final String LOCKED_STATUS_NOT_SUPPORTED = "Chức năng khoá vĩnh viễn chưa được hỗ trợ";

    // Auth Error Messages
    public static final String UNAUTHORIZED_ACTION = "Bạn cần đăng nhập để thực hiện chức năng này";
    public static final String ACCESS_DENIED = "Bạn không có quyền thực hiện hành động này";
    public static final String EMAIL_EXISTED = "Email đã tồn tại";
    public static final String WRONG_PASSWORD = "Mật khẩu không chính xác";
    public static final String USER_NOT_ACTIVE_OR_LOCKED = "Tài khoản chưa được kích hoạt hoặc bị khóa";
    public static final String EMAIL_NOT_VERIFIED = "Vui lòng xác thực email trước khi đăng nhập";
    public static final String INVALID_TOKEN = "Token không hợp lệ hoặc đã hết hạn";
    public static final String ROLE_NOT_FOUND = "Không tìm thấy vai trò mặc định trong hệ thống";

    // Upload Error Messages
    public static final String UPLOAD_FAILED = "Lỗi tải file lên hệ thống";
    public static final String FILE_TOO_LARGE = "Kích thước file vượt quá giới hạn cho phép (10MB)";
    public static final String INVALID_FILE_FORMAT = "Định dạng file không được hỗ trợ (Chỉ chấp nhận: JPEG, PNG, WEBP)";
    
    // File Success Messages
    public static final String FILE_DELETED_SUCCESSFULLY = "Xóa file thành công";

    // Email Messages
    public static final String EMAIL_SEND_FAILED = "Gửi email thất bại";

    // Tour Messages
    public static final String TOUR_NOT_FOUND = "Tour không tồn tại";
    public static final String TOUR_CREATED_SUCCESSFULLY = "Tạo tour thành công";
    public static final String TOUR_UPDATED_SUCCESSFULLY = "Cập nhật tour thành công";
    public static final String TOUR_DELETED_SUCCESSFULLY = "Xóa tour thành công";
    public static final String TOUR_NOT_BELONG_TO_VENDOR = "Tour không thuộc về Vendor của bạn";
    public static final String TOUR_STATUS_NOT_EDITABLE = "Tour ở trạng thái hiện tại không thể chỉnh sửa";
    public static final String TOUR_NAME_REQUIRED = "Tên tour không được để trống";
    public static final String TOUR_DESCRIPTION_REQUIRED = "Mô tả không được để trống";
    public static final String TOUR_DIFFICULTY_REQUIRED = "Độ khó không được để trống";
    public static final String TOUR_LOCATION_REQUIRED = "Địa điểm không được để trống";
    public static final String TOUR_DURATION_REQUIRED = "Số ngày không được để trống";
    public static final String TOUR_DURATION_MIN = "Thời gian tour phải ít nhất 1 ngày";
    public static final String TOUR_BASE_PRICE_REQUIRED = "Giá cơ bản không được để trống";
    public static final String TOUR_BASE_PRICE_MIN = "Giá cơ bản phải lớn hơn 0";
    public static final String TOUR_MIN_CAPACITY_REQUIRED = "Sức chứa tối thiểu không được để trống";
    public static final String TOUR_MIN_CAPACITY_MIN = "Sức chứa tối thiểu phải ít nhất là 1";
    public static final String TOUR_MAX_CAPACITY_REQUIRED = "Sức chứa tối đa không được để trống";
    public static final String TOUR_MAX_CAPACITY_MIN = "Sức chứa tối đa phải ít nhất là 1";
    public static final String TOUR_SUBMITTED_FOR_APPROVAL = "Tour đã được gửi yêu cầu kiểm duyệt thành công";
    public static final String TOUR_NOT_IN_DRAFT_OR_REJECTED = "Chỉ có thể gửi duyệt Tour ở trạng thái Nháp hoặc Bị từ chối";
    public static final String TOUR_HIDDEN_SUCCESSFULLY = "Tour đã được ẩn thành công do vi phạm";
    public static final String TOUR_NOT_APPROVED = "Chỉ có thể ẩn Tour đang ở trạng thái đã duyệt (APPROVED)";
    public static final String HIDE_REASON_REQUIRED = "Lý do ẩn tour không được để trống";

    // Vendor Messages
    public static final String VENDOR_NOT_FOUND = "Vendor không tồn tại";

    // Blog Messages
    public static final String BLOG_NOT_FOUND = "Bài viết không tồn tại";

    // Checkpoint Messages
    public static final String CHECKPOINT_NOT_FOUND = "Trạm dừng không tồn tại";
    public static final String CHECKPOINT_CREATED_SUCCESSFULLY = "Thêm trạm dừng thành công";
    public static final String CHECKPOINT_UPDATED_SUCCESSFULLY = "Cập nhật trạm dừng thành công";
    public static final String CHECKPOINT_DELETED_SUCCESSFULLY = "Xoá trạm dừng thành công";
    public static final String CHECKPOINT_DUPLICATE_ORDER = "Thứ tự trạm dừng đã tồn tại trong tour này";
    public static final String CHECKPOINT_NAME_REQUIRED = "Tên trạm dừng không được để trống";
    public static final String CHECKPOINT_ORDER_REQUIRED = "Thứ tự trạm dừng không được để trống";
    public static final String CHECKPOINT_ORDER_POSITIVE = "Thứ tự trạm dừng phải lớn hơn 0";


    // Additional Validation Messages
    public static final String EMAIL_REQUIRED = "Vui lòng nhập email";
    public static final String EMAIL_INVALID = "Định dạng email không hợp lệ";
    public static final String PASSWORD_REQUIRED = "Vui lòng nhập mật khẩu";
    public static final String TOKEN_REQUIRED = "Vui lòng cung cấp token";
    public static final String CONFIRM_PASSWORD_REQUIRED = "Vui lòng nhập lại mật khẩu xác nhận";

    // Vendor Application Messages
    public static final String COMPANY_NAME_REQUIRED = "Tên công ty không được để trống";
    public static final String CONTACT_EMAIL_REQUIRED = "Email liên hệ không được để trống";
    public static final String CONTACT_PHONE_REQUIRED = "Số điện thoại liên hệ không được để trống";
    public static final String TAX_CODE_REQUIRED = "Mã số thuế không được để trống";
    public static final String TAX_CODE_INVALID = "Mã số thuế không hợp lệ (phải từ 10 đến 13 chữ số)";
    public static final String BUSINESS_LICENSE_REQUIRED = "Ảnh/file giấy phép kinh doanh không được để trống";
    public static final String VENDOR_APPLICATION_NOT_FOUND = "Đơn đăng ký đối tác không tồn tại";
    public static final String TAX_CODE_ALREADY_EXISTS = "Mã số thuế đã tồn tại trên hệ thống";
    public static final String APPLICATION_PENDING_EXISTS = "Bạn đã có đơn đăng ký đang chờ xử lý";
    public static final String VENDOR_APPLICATION_DRAFT_CREATED = "Tạo đơn đăng ký bản nháp thành công.";
    public static final String VENDOR_APPLICATION_SUBMITTED = "Nộp đơn đăng ký đối tác thành công. Đang chờ phê duyệt.";
    public static final String VENDOR_APPLICATION_RESUBMITTED = "Nộp lại đơn đăng ký đối tác thành công. Đang chờ phê duyệt lại.";
    public static final String VENDOR_APPLICATION_REVIEWED = "Kiểm duyệt đơn đăng ký đối tác thành công.";
    public static final String REVIEW_STATUS_REQUIRED = "Trạng thái kiểm duyệt không được để trống";
    public static final String INVALID_REVIEW_STATUS = "Trạng thái kiểm duyệt không hợp lệ (phải là APPROVED hoặc REJECTED)";
    public static final String CANNOT_SUBMIT_APPLICATION = "Chỉ có thể nộp đơn đăng ký ở trạng thái DRAFT";
    public static final String CANNOT_RESUBMIT_APPLICATION = "Chỉ có thể nộp lại đơn đăng ký ở trạng thái REJECTED";
    public static final String CANNOT_UPDATE_APPLICATION_STATUS = "Chỉ có thể cập nhật đơn đăng ký ở trạng thái DRAFT hoặc REJECTED";
    public static final String CONTACT_EMAIL_ALREADY_EXISTS = "Email liên hệ đã tồn tại trên hệ thống";
    public static final String CONTACT_PHONE_ALREADY_EXISTS = "Số điện thoại liên hệ đã tồn tại trên hệ thống";
    public static final String UNAUTHORIZED_APPLICATION_ACCESS = "Bạn không có quyền truy cập đơn đăng ký này";
    public static final String APPLICATION_ALREADY_PROCESSED = "Đơn đăng ký đối tác này đã được xử lý";
    public static final String VENDOR_APPLICATION_APPROVED = "Phê duyệt đơn đăng ký và kích hoạt tài khoản Vendor thành công.";
    public static final String REJECTION_REASON_REQUIRED = "Lý do từ chối không được để trống";
    public static final String VENDOR_APPLICATION_REJECTED = "Từ chối đơn đăng ký đối tác thành công.";
    public static final String CANNOT_UPDATE_APPLICATION = "Chỉ có thể cập nhật đơn đăng ký ở trạng thái DRAFT hoặc REJECTED";
    public static final String VENDOR_APPLICATION_UPDATED = "Cập nhật đơn đăng ký đối tác thành công.";
    public static final String UNAUTHORIZED_VENDOR_ACCESS = "Bạn không có quyền truy cập thông tin Vendor";
    public static final String VENDOR_PROFILE_FETCHED = "Lấy thông tin hồ sơ đối tác thành công";
    public static final String VENDOR_PROFILE_UPDATED = "Cập nhật thông tin hồ sơ đối tác thành công";
    public static final String VENDOR_STATUS_REQUIRED = "Trạng thái của đối tác không được để trống";
    public static final String VENDOR_STATUS_UPDATED = "Cập nhật trạng thái đối tác thành công";
    public static final String VENDOR_REVOKED_STATUS = "Không thể thay đổi trạng thái của đối tác đã bị thu hồi quyền (REVOKED)";
    public static final String STAFF_ALREADY_EXISTS = "Nhân viên này đã thuộc công ty của bạn";
    public static final String STAFF_BELONGS_TO_OTHER_VENDOR = "Nhân viên này đang thuộc một công ty khác";
    public static final String VENDOR_STAFF_ADDED = "Thêm nhân viên mới thành công";
    public static final String STAFF_STATUS_REQUIRED = "Trạng thái hoạt động của nhân viên không được để trống";
    public static final String VENDOR_STAFF_STATUS_UPDATED = "Cập nhật trạng thái nhân viên thành công";
    public static final String VENDOR_STAFF_NOT_FOUND = "Không tìm thấy thông tin nhân viên này";
    public static final String UNAUTHORIZED_STAFF_ACCESS = "Bạn không có quyền quản lý nhân viên này";

    // Schedule Messages
    public static final String SCHEDULE_NOT_FOUND = "Lịch khởi hành không tồn tại";
    public static final String SCHEDULE_CREATED_SUCCESSFULLY = "Tạo lịch khởi hành thành công";
    public static final String SCHEDULE_UPDATED_SUCCESSFULLY = "Cập nhật lịch khởi hành thành công";
    public static final String SCHEDULE_DELETED_SUCCESSFULLY = "Huỷ lịch khởi hành thành công";
    public static final String SCHEDULE_HAS_BOOKINGS = "Không thể huỷ lịch khởi hành đã có khách đặt";
    public static final String SCHEDULE_DEPARTURE_REQUIRED = "Ngày khởi hành không được để trống";
    public static final String SCHEDULE_RETURN_REQUIRED = "Ngày kết thúc không được để trống";
    public static final String SCHEDULE_PRICE_REQUIRED = "Giá lịch trình không được để trống";
    public static final String SCHEDULE_PRICE_MIN = "Giá lịch trình phải lớn hơn 0";
    public static final String SCHEDULE_SLOTS_REQUIRED = "Số slot không được để trống";
    public static final String SCHEDULE_SLOTS_MIN = "Số slot phải ít nhất là 1";
    public static final String SCHEDULE_RETURN_BEFORE_DEPARTURE = "Ngày kết thúc phải sau ngày khởi hành";
    public static final String SCHEDULE_DEPARTURE_IN_PAST = "Ngày khởi hành phải từ hôm nay trở đi";
}

