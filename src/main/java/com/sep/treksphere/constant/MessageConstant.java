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
    public static final String PASSWORD_MESSAGE = "Mật khẩu phải dài ít nhất 8 ký tự, bao gồm ít nhất một chữ hoa, một chữ thường, một chữ số và một ký tự đặc biệt.";
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
    public static final String ACCOUNT_LOCKED = "Tài khoản đã bị khóa. Vui lòng liên hệ quản trị viên.";
    public static final String ACCOUNT_DEACTIVATED = "Tài khoản đã bị vô hiệu hóa.";
    public static final String VERIFICATION_TOKEN_EXPIRED = "Liên kết xác minh đã hết hạn. Vui lòng yêu cầu gửi lại email xác minh.";
    public static final String VERIFICATION_RESEND_RATE_LIMITED = "Bạn đã yêu cầu gửi lại email quá nhiều lần. Vui lòng thử lại sau.";
    public static final String VERIFICATION_EMAIL_RESENT = "Nếu tài khoản tồn tại và chưa được xác minh, email xác minh mới đã được gửi.";
    public static final String EMAIL_NOT_VERIFIED = "Vui lòng xác thực email trước khi đăng nhập";
    public static final String INVALID_TOKEN = "Token không hợp lệ hoặc đã hết hạn";
    public static final String ROLE_NOT_FOUND = "Không tìm thấy vai trò mặc định trong hệ thống";
    public static final String FORGOT_PASSWORD_RATE_LIMITED = "Bạn đã yêu cầu gửi email khôi phục mật khẩu quá nhiều lần. Vui lòng thử lại sau 5 phút.";

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
    public static final String TOUR_HAS_ACTIVE_BOOKINGS = "Tour đang có đặt chỗ chưa huỷ, không thể xóa hoặc ẩn";
    public static final String TOUR_NOT_IN_REJECTED_STATUS = "Tour phải ở trạng thái REJECTED để chuyển về bản nháp";
    public static final String TOUR_UPDATE_NOT_ALLOWED = "Bạn không có quyền chỉnh sửa Tour ở trạng thái này";
    public static final String TOUR_REVERTED_TO_DRAFT = "Tour đã được chuyển về trạng thái Bản nháp thành công";
    public static final String TOUR_NOT_DELETED = "Tour chưa bị xóa, không thể khôi phục";
    public static final String TOUR_RESTORED_SUCCESSFULLY = "Khôi phục tour thành công. Tour đã được chuyển về trạng thái bản nháp";
    public static final String TOUR_APPROVED_SUCCESSFULLY = "Duyệt tour thành công";
    public static final String TOUR_REJECTED_SUCCESSFULLY = "Từ chối tour thành công";
    public static final String TOUR_NOT_PENDING_APPROVAL = "Chỉ có thể duyệt hoặc từ chối Tour đang ở trạng thái Chờ duyệt (PENDING_APPROVAL)";
    public static final String TOUR_UNHIDDEN_SUCCESSFULLY = "Mở lại (bỏ ẩn) Tour thành công";
    public static final String TOUR_NOT_HIDDEN = "Chỉ có thể mở lại Tour đang ở trạng thái Bị ẩn (HIDDEN)";

    // Vendor Messages
    public static final String VENDOR_NOT_FOUND = "Vendor không tồn tại";
    public static final String EQUIPMENT_NOT_FOUND = "Trang bị không tồn tại";
    public static final String EQUIPMENT_CREATED_SUCCESSFULLY = "Tạo trang bị thành công";
    public static final String EQUIPMENT_UPDATED_SUCCESSFULLY = "Cập nhật trang bị thành công";
    public static final String EQUIPMENT_DELETED_SUCCESSFULLY = "Xóa trang bị thành công";
    public static final String EQUIPMENT_NAME_REQUIRED = "Tên trang bị không được để trống";
    public static final String EQUIPMENT_QUANTITY_MIN = "Số lượng tổng không được nhỏ hơn 0";

    // Blog Messages
    public static final String BLOG_NOT_FOUND = "Bài viết không tồn tại";
    public static final String BLOG_CREATED_SUCCESSFULLY = "Đăng bài viết thành công";
    public static final String BLOG_UPDATED_SUCCESSFULLY = "Cập nhật bài viết thành công";
    public static final String BLOG_DELETED_SUCCESSFULLY = "Xóa bài viết thành công";
    public static final String BLOG_HIDDEN_SUCCESSFULLY = "Ẩn bài viết thành công";
    public static final String BLOG_TITLE_REQUIRED = "Tiêu đề bài viết không được để trống";
    public static final String BLOG_TITLE_MAX_LENGTH = "Tiêu đề bài viết không được vượt quá 500 ký tự";
    public static final String BLOG_CONTENT_REQUIRED = "Nội dung bài viết không được để trống";
    public static final String BLOG_CANNOT_EDIT = "Bạn không có quyền chỉnh sửa bài viết này";

    // Comment Messages
    public static final String COMMENT_NOT_FOUND = "Bình luận không tồn tại";
    public static final String COMMENT_CONTENT_REQUIRED = "Nội dung bình luận không được để trống";
    public static final String COMMENT_CONTENT_MAX_LENGTH = "Nội dung bình luận không được vượt quá 1000 ký tự";
    public static final String COMMENT_ADDED_SUCCESSFULLY = "Gửi bình luận thành công";
    public static final String COMMENT_UPDATED_SUCCESSFULLY = "Cập nhật bình luận thành công";
    public static final String COMMENT_DELETED_SUCCESSFULLY = "Xóa bình luận thành công";
    public static final String COMMENT_CANNOT_EDIT = "Bạn không có quyền chỉnh sửa bình luận này";
    public static final String COMMENT_CANNOT_DELETE = "Bạn không có quyền xóa bình luận này";

    // Checkpoint Messages
    public static final String CHECKPOINT_NOT_FOUND = "Trạm dừng không tồn tại";
    public static final String CHECKPOINT_CREATED_SUCCESSFULLY = "Thêm trạm dừng thành công";
    public static final String CHECKPOINT_UPDATED_SUCCESSFULLY = "Cập nhật trạm dừng thành công";
    public static final String CHECKPOINT_DELETED_SUCCESSFULLY = "Xoá trạm dừng thành công";
    public static final String CHECKPOINT_DUPLICATE_ORDER = "Thứ tự trạm dừng đã tồn tại trong tour này";
    public static final String CHECKPOINT_DUPLICATE_NAME = "Tên trạm dừng đã tồn tại trong tour này";
    public static final String CHECKPOINT_DUPLICATE_COORDINATES = "Tọa độ (vĩ độ, kinh độ) của trạm dừng đã tồn tại trong tour này";
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
    public static final String APPLICANT_ALREADY_HAS_APPLICATION = "Bạn đã có đơn đăng ký đối tác; hãy tiếp tục cập nhật hoặc nộp lại đơn hiện có";
    public static final String APPLICANT_ALREADY_HAS_VENDOR = "Bạn đã quản lý một Vendor và không thể tạo hoặc nộp thêm đơn đăng ký";
    public static final String VENDOR_APPLICATION_DRAFT_CREATED = "Tạo đơn đăng ký bản nháp thành công.";
    public static final String VENDOR_APPLICATION_SUBMITTED = "Nộp đơn đăng ký đối tác thành công. Đang chờ phê duyệt.";
    public static final String VENDOR_APPLICATION_RESUBMITTED = "Nộp lại đơn đăng ký đối tác thành công. Đang chờ phê duyệt lại.";
    public static final String VENDOR_APPLICATION_REVIEWED = "Kiểm duyệt đơn đăng ký đối tác thành công.";
    public static final String REVIEW_STATUS_REQUIRED = "Trạng thái kiểm duyệt không được để trống";
    public static final String INVALID_REVIEW_STATUS = "Trạng thái kiểm duyệt không hợp lệ (phải là APPROVED hoặc REJECTED)";
    public static final String INVALID_APPLICATION_FILTER_STATUS = "Trạng thái lọc đơn đăng ký không hợp lệ (chỉ chấp nhận PENDING, APPROVED hoặc REJECTED)";
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
    public static final String VENDOR_STAFF_ROLE_UPDATED = "Cập nhật vai trò nhân viên thành công";
    public static final String COORDINATOR_HAS_ACTIVE_SCHEDULES = "Không thể đổi vai trò vì điều phối viên đang có lịch tour chưa hoàn thành";
    public static final String STAFF_STATUS_REQUIRED = "Trạng thái hoạt động của nhân viên không được để trống";
    public static final String VENDOR_STAFF_STATUS_UPDATED = "Cập nhật trạng thái nhân viên thành công";
    public static final String VENDOR_STAFF_NOT_FOUND = "Không tìm thấy thông tin nhân viên này";
    public static final String UNAUTHORIZED_STAFF_ACCESS = "Bạn không có quyền quản lý nhân viên này";
    public static final String COORDINATOR_SCHEDULE_FETCHED = "Lấy danh sách lịch dẫn đoàn thành công";
    public static final String COORDINATOR_LOGISTICS_FETCHED = "Lấy thông tin logistics của đoàn thành công";
    public static final String INVALID_DATE_RANGE = "Ngày bắt đầu tìm kiếm không thể lớn hơn ngày kết thúc";
    public static final String SESSION_STARTED_SUCCESSFULLY = "Bắt đầu phiên đi tour thành công";
    public static final String SESSION_ENDED_SUCCESSFULLY = "Kết thúc phiên đi tour thành công";
    public static final String SESSION_NOT_FOUND = "Không tìm thấy phiên đi tour này";
    public static final String SESSION_ALREADY_STARTED = "Phiên đi tour này đã được bắt đầu từ trước";
    public static final String SESSION_ALREADY_COMPLETED = "Phiên đi tour này đã hoàn thành";
    public static final String SESSION_ALREADY_CANCELLED = "Phiên đi tour này đã bị hủy";
    public static final String NOT_LEAD_COORDINATOR = "Chỉ hướng dẫn viên trưởng mới được quyền bắt đầu tour";
    public static final String UNAUTHORIZED_SESSION_ACCESS = "Bạn không có quyền truy cập phiên đi tour này";
    public static final String LATITUDE_REQUIRED = "Tọa độ vĩ độ (latitude) không được để trống";
    public static final String LONGITUDE_REQUIRED = "Tọa độ kinh độ (longitude) không được để trống";
    public static final String CHECKIN_OUT_OF_RANGE = "Bạn đang ở quá xa vị trí trạm dừng để thực hiện check-in";
    public static final String INVALID_GPS_COORDINATES = "Tọa độ GPS không hợp lệ (Vĩ độ phải từ -90 đến 90, Kinh độ phải từ -180 đến 180)";
    public static final String GPS_COORDINATES_REQUIRED = "Tọa độ GPS không được để trống";
    public static final String LATITUDE_OUT_OF_BOUNDS = "Vĩ độ (latitude) phải nằm trong khoảng từ -90 đến 90 độ";
    public static final String LONGITUDE_OUT_OF_BOUNDS = "Kinh độ (longitude) phải nằm trong khoảng từ -180 đến 180 độ";
    public static final String NO_PENDING_CHECKPOINTS = "Tất cả các trạm dừng của chuyến đi này đã hoàn thành check-in";
    public static final String SESSION_NOT_IN_PROGRESS = "Phiên đi tour này hiện tại không hoạt động";
    public static final String CHECKIN_SUCCESSFUL = "Ghi nhận trạm dừng thành công";
    public static final String ATTENDANCE_RECORDED_SUCCESSFULLY = "Ghi nhận điểm danh thành công";
    public static final String INVALID_ATTENDANCE_TYPE = "Loại điểm danh không hợp lệ (phải là START hoặc END)";
    public static final String PARTICIPANT_NOT_FOUND_IN_SESSION = "Thành viên tham gia không thuộc phiên đi tour này";
    public static final String PARTICIPANT_ID_REQUIRED = "ID thành viên tham gia không được để trống";
    public static final String ATTENDANCE_STATUS_REQUIRED = "Trạng thái điểm danh có mặt/vắng mặt không được để trống";
    public static final String ATTENDANCE_TYPE_REQUIRED = "Loại điểm danh (attendanceType) không được để trống";
    public static final String ATTENDANCE_LIST_REQUIRED = "Danh sách điểm danh thành viên (participants) không được rỗng";
    public static final String ATTENDANCE_LIST_TOO_LARGE = "Mỗi lần chỉ được điểm danh tối đa 100 thành viên";
    public static final String DUPLICATE_PARTICIPANT_IN_ATTENDANCE = "Danh sách điểm danh chứa thành viên bị trùng lặp";
    public static final String ATTENDANCE_START_REQUIRED = "Thành viên phải được ghi nhận điểm danh đầu chuyến trước khi điểm danh cuối chuyến";
    public static final String TOUR_CHECKPOINTS_NOT_CONFIGURED = "Tour phải có ít nhất điểm xuất phát và điểm kết thúc trước khi khởi hành";
    public static final String TOUR_CHECKPOINT_COORDINATES_REQUIRED = "Tất cả trạm dừng phải có tọa độ GPS hợp lệ trước khi khởi hành";
    public static final String SESSION_START_DATE_INVALID = "Chỉ có thể bắt đầu chuyến đi trong khoảng ngày khởi hành đến ngày kết thúc";
    public static final String SCHEDULE_NOT_AVAILABLE_FOR_START = "Lịch khởi hành không ở trạng thái cho phép bắt đầu chuyến đi";
    public static final String START_ATTENDANCE_INCOMPLETE = "Phải hoàn tất điểm danh đầu chuyến cho tất cả thành viên trước khi khởi hành";
    public static final String NO_PRESENT_PARTICIPANTS = "Không thể bắt đầu chuyến đi khi không có thành viên nào tham gia";
    public static final String SESSION_EQUIPMENT_NOT_READY = "Tất cả dụng cụ đã phân bổ phải được kiểm tra trước khi khởi hành";
    public static final String SESSION_CHECKPOINT_LOGS_ALREADY_INITIALIZED = "Nhật ký trạm dừng của chuyến đi đã được khởi tạo trước đó";
    public static final String SESSION_EQUIPMENT_CHECKED_SUCCESS = "Đánh dấu kiểm tra dụng cụ đi tour thành công";
    public static final String UNAUTHORIZED_EQUIPMENT_CHECK = "Bạn không có quyền kiểm tra dụng cụ của phiên đi tour này";
    public static final String EQUIPMENT_CHECK_STATUS_REQUIRED = "Trạng thái kiểm tra không được để trống";
    public static final String SOS_ALERT_CREATED_SUCCESS = "Phát tín hiệu SOS khẩn cấp thành công";
    public static final String UNAUTHORIZED_SOS_ALERT = "Bạn không có quyền phát tín hiệu SOS cho phiên đi tour này";
    public static final String SESSION_FOR_SOS_NOT_ACTIVE = "Phiên đi tour này hiện tại không hoạt động để phát tín hiệu SOS";
    public static final String ACTIVE_SOS_ALERTS_FETCHED = "Lấy danh sách cuộc gọi SOS hoạt động thành công";
    public static final String SOS_ALERT_RESOLVED_SUCCESS = "Yêu cầu cứu hộ SOS đã được giải quyết thành công";
    public static final String SOS_ALERT_NOT_FOUND = "Yêu cầu cứu hộ SOS không tồn tại";
    public static final String SOS_ALERT_ALREADY_RESOLVED = "Yêu cầu cứu hộ SOS đã được giải quyết trước đó";
    public static final String UNAUTHORIZED_RESOLVE_SOS = "Bạn không có quyền giải quyết yêu cầu cứu hộ SOS này";
    public static final String MATCHING_GROUPS_FETCHED_SUCCESS = "Lấy danh sách nhóm ghép bạn đồng hành thành công";
    public static final String MATCHING_GROUP_FETCHED_SUCCESS = "Lấy chi tiết nhóm ghép bạn đồng hành thành công";
    public static final String MATCHING_GROUP_NOT_FOUND = "Nhóm ghép bạn đồng hành không tồn tại";
    public static final String MATCHING_GROUP_CREATED_SUCCESS = "Tạo nhóm ghép bạn đồng hành thành công";
    public static final String INVALID_TARGET_DATE = "Ngày đi mong muốn phải ở tương lai";
    public static final String INVALID_DEADLINE = "Hạn chót ghép nhóm phải ở tương lai và trước hoặc trong ngày khởi hành";
    public static final String ALREADY_HAS_ACTIVE_GROUP = "Bạn đã có một nhóm ghép đang hoạt động cho Tour này";
    public static final String MATCHING_GROUP_JOIN_REQUESTED_SUCCESS = "Gửi yêu cầu tham gia nhóm ghép bạn đồng hành thành công";
    public static final String MATCHING_JOIN_REQUESTS_FETCHED_SUCCESS = "Lấy danh sách yêu cầu tham gia nhóm ghép thành công";
    public static final String MY_MATCHING_JOIN_REQUESTS_FETCHED_SUCCESS = "Lấy danh sách yêu cầu tham gia nhóm ghép của bạn thành công";
    public static final String UNAUTHORIZED_VIEW_JOIN_REQUESTS = "Bạn không có quyền xem yêu cầu tham gia của nhóm ghép này";
    public static final String MATCHING_GROUP_NOT_OPEN = "Nhóm ghép không còn mở để xin gia nhập";
    public static final String MATCHING_DEADLINE_PASSED = "Đã quá hạn chót ghép nhóm";
    public static final String MATCHING_GROUP_FULL = "Nhóm ghép bạn đồng hành đã đầy thành viên";
    public static final String ALREADY_MEMBER = "Bạn đã là thành viên của nhóm ghép này";
    public static final String JOIN_REQUEST_PENDING = "Yêu cầu gia nhập nhóm của bạn đang chờ duyệt";
    public static final String MATCHING_MEMBER_NOT_FOUND = "Yêu cầu tham gia của thành viên không tồn tại";
    public static final String UNAUTHORIZED_APPROVE_MEMBER = "Bạn không có quyền duyệt thành viên cho nhóm ghép này";
    public static final String MEMBER_ALREADY_APPROVED = "Thành viên này đã được duyệt vào nhóm trước đó";
    public static final String INVALID_MEMBER_STATUS = "Trạng thái yêu cầu của thành viên không hợp lệ để duyệt";
    public static final String MATCHING_MEMBER_APPROVED_SUCCESS = "Duyệt thành viên vào nhóm ghép thành công";
    public static final String UNAUTHORIZED_REJECT_MEMBER = "Bạn không có quyền từ chối thành viên cho nhóm ghép này";
    public static final String MEMBER_ALREADY_REJECTED = "Thành viên này đã bị từ chối trước đó";
    public static final String MATCHING_MEMBER_REJECTED_SUCCESS = "Từ chối thành viên tham gia nhóm ghép thành công";
    public static final String OWNER_CANNOT_LEAVE = "Trưởng nhóm không thể rời nhóm ghép, hãy sử dụng tính năng giải tán nhóm";
    public static final String NOT_A_MEMBER = "Bạn không phải là thành viên hoạt động của nhóm ghép này";
    public static final String MATCHING_JOIN_REQUEST_CANCELLED_SUCCESS = "Hủy yêu cầu tham gia nhóm ghép thành công";
    public static final String MATCHING_MEMBER_LEFT_SUCCESS = "Rời khỏi nhóm ghép bạn đồng hành thành công";
    public static final String NO_PENDING_JOIN_REQUEST = "Bạn không có yêu cầu tham gia đang chờ duyệt trong nhóm này";
    public static final String NOT_ACCEPTED_MATCHING_MEMBER = "Bạn chưa phải là thành viên đã được duyệt của nhóm ghép này";
    public static final String UNAUTHORIZED_DISBAND_GROUP = "Bạn không có quyền giải tán nhóm ghép này";
    public static final String MATCHING_GROUP_DISBANDED_SUCCESS = "Giải tán nhóm ghép bạn đồng hành thành công";
    public static final String MATCHING_GROUP_CANNOT_BE_DISBANDED = "Chỉ có thể giải tán nhóm ghép đang OPEN hoặc FULL";

    public static final String MATCHING_TOUR_ID_REQUIRED = "Mã tour không được để trống";
    public static final String MATCHING_GROUP_NAME_REQUIRED = "Tên nhóm ghép không được để trống";
    public static final String MATCHING_GROUP_NAME_SIZE = "Tên nhóm ghép phải từ 3 đến 100 ký tự";
    public static final String MATCHING_GROUP_DESCRIPTION_MAX_LENGTH = "Mô tả nhóm ghép không được vượt quá 2000 ký tự";
    public static final String MATCHING_GROUP_MAX_SIZE_REQUIRED = "Số lượng thành viên tối đa không được để trống";
    public static final String MATCHING_GROUP_MAX_SIZE_MIN = "Số lượng thành viên tối đa phải từ 2 trở lên";
    public static final String MATCHING_GROUP_MAX_SIZE_MAX = "Số lượng thành viên tối đa không vượt quá 100 người";
    public static final String MATCHING_TARGET_DATE_REQUIRED = "Ngày đi mong muốn không được để trống";
    public static final String MATCHING_DEADLINE_REQUIRED = "Hạn chót ghép nhóm không được để trống";
    public static final String MATCHING_TOUR_NOT_APPROVED = "Chỉ có thể tạo nhóm ghép cho Tour đã duyệt";
    public static final String MATCHING_GROUP_SIZE_EXCEEDS_TOUR_CAPACITY = "Số thành viên tối đa của nhóm vượt quá sức chứa của Tour";
    public static final String MATCHING_TOUR_NOT_AVAILABLE = "Tour của nhóm ghép không còn công khai";
    public static final String MATCHING_OWNER_CANNOT_JOIN = "Trưởng nhóm không thể gửi yêu cầu tham gia nhóm của chính mình";
    public static final String MATCHING_TARGET_DATE_PASSED = "Ngày dự kiến đi của nhóm đã đến hoặc đã qua";
    public static final String MATCHING_JOIN_REQUEST_PAGE_MIN = "Số trang không được nhỏ hơn 0";
    public static final String MATCHING_JOIN_REQUEST_SIZE_RANGE = "Số phần tử mỗi trang phải từ 1 đến 50";
    public static final String INVALID_JOIN_REQUEST_FILTER_STATUS = "Danh sách yêu cầu tham gia chỉ hỗ trợ trạng thái PENDING hoặc REJECTED";
    public static final String INVALID_JOIN_REQUEST_PAGINATION = "Phân trang yêu cầu tham gia không hợp lệ";

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
    public static final String SCHEDULE_NOT_EDITABLE = "Lịch khởi hành đã hoàn thành hoặc đã huỷ, không thể chỉnh sửa hoặc xóa";
    public static final String TOUR_NOT_APPROVED_FOR_SCHEDULE = "Chỉ có thể tạo hoặc quản lý lịch khởi hành cho Tour đã được duyệt (APPROVED) hoặc tạm ẩn (HIDDEN)";
    public static final String SCHEDULE_CHANGE_REASON_REQUIRED = "Vui lòng nhập lý do điều chỉnh lịch trình đã có khách đặt";
    public static final String SCHEDULE_SLOTS_LESS_THAN_BOOKED = "Số chỗ mở bán không thể nhỏ hơn số chỗ khách đã đặt";
    public static final String SCHEDULE_SLOTS_EXCEED_MAX_CAPACITY = "Số chỗ mở bán không thể vượt quá sức chứa tối đa của Tour";
    // Porter Profile Messages
    public static final String PORTER_NOT_FOUND = "Không tìm thấy thông tin hồ sơ porter";
    public static final String PORTER_NAME_REQUIRED = "Tên porter không được để trống";
    public static final String PORTER_PHONE_REQUIRED = "Số điện thoại không được để trống";
    public static final String PORTER_PHONE_INVALID = "Số điện thoại không hợp lệ";
    public static final String PORTER_CREATED_SUCCESSFULLY = "Tạo hồ sơ porter thành công";
    public static final String PORTER_UPDATED_SUCCESSFULLY = "Cập nhật hồ sơ porter thành công";
    public static final String PORTER_DELETED_SUCCESSFULLY = "Xóa hồ sơ porter thành công";
    public static final String PORTER_LIST_FETCHED_SUCCESSFULLY = "Lấy danh sách hồ sơ porter thành công";

    // Booking Messages
    public static final String BOOKING_NOT_FOUND = "Đơn đặt tour không tồn tại";
    public static final String BOOKING_CREATED_SUCCESSFULLY = "Đặt tour thành công";
    public static final String BOOKING_CANCELLED_SUCCESSFULLY = "Huỷ đơn đặt tour thành công";
    public static final String BOOKING_REJECTED_SUCCESSFULLY = "Từ chối đơn đặt tour thành công";
    public static final String PAYMENT_PROOF_SUBMITTED = "Gửi minh chứng thanh toán thành công";
    public static final String NOT_ENOUGH_SLOTS = "Số lượng chỗ trống không đủ";
    public static final String BOOKING_CANNOT_CANCEL = "Không thể huỷ đơn đặt tour ở trạng thái này";
    public static final String INVALID_BOOKING_STATUS = "Trạng thái đơn đặt tour không hợp lệ";

    // Voucher Messages
    public static final String VOUCHER_NOT_FOUND = "Mã giảm giá không tồn tại";
    public static final String VOUCHER_NOT_ACTIVE = "Mã giảm giá không hoạt động";
    public static final String VOUCHER_EXPIRED = "Mã giảm giá đã hết hạn hoặc chưa có hiệu lực";
    public static final String VOUCHER_MAX_USAGE_REACHED = "Mã giảm giá đã hết lượt sử dụng";
    public static final String VOUCHER_MIN_ORDER_VALUE_NOT_MET = "Đơn đặt tour chưa đạt giá trị tối thiểu để sử dụng mã giảm giá";
    public static final String VOUCHER_VENDOR_MISMATCH = "Mã giảm giá không thuộc nhà cung cấp của tour này";
    public static final String VOUCHER_CODE_ALREADY_EXISTS = "Mã giảm giá đã tồn tại";
    public static final String VOUCHER_VALID_DATE_ERROR = "Ngày bắt đầu phải nhỏ hơn ngày kết thúc";
    public static final String INVALID_DISCOUNT_VALUE = "Giá trị giảm giá phần trăm không hợp lệ (phải <= 100)";
    public static final String VOUCHER_CREATED_SUCCESSFULLY = "Tạo mã giảm giá thành công";
    public static final String VOUCHER_UPDATED_SUCCESSFULLY = "Cập nhật mã giảm giá thành công";
    public static final String VOUCHER_DELETED_SUCCESSFULLY = "Hủy mã giảm giá thành công";
    public static final String VOUCHER_NOT_YET_VALID = "Mã giảm giá chưa đến thời gian sử dụng";
    public static final String VOUCHER_VALIDATION_SUCCESS = "Áp dụng mã giảm giá thành công!";

    public static final String VOUCHER_CODE_REQUIRED = "Mã voucher không được để trống";
    public static final String VOUCHER_DISCOUNT_TYPE_REQUIRED = "Loại giảm giá không được để trống";
    public static final String VOUCHER_DISCOUNT_VALUE_REQUIRED = "Giá trị giảm không được để trống";
    public static final String VOUCHER_DISCOUNT_VALUE_MIN = "Giá trị giảm phải lớn hơn hoặc bằng 0";
    public static final String VOUCHER_MIN_ORDER_VALUE_MIN = "Giá trị đơn hàng tối thiểu phải lớn hơn hoặc bằng 0";
    public static final String VOUCHER_MAX_USAGE_REQUIRED = "Số lượng giới hạn không được để trống";
    public static final String VOUCHER_MAX_USAGE_MIN = "Số lượng giới hạn phải lớn hơn 0";
    public static final String VOUCHER_VALID_FROM_REQUIRED = "Ngày bắt đầu không được để trống";
    public static final String VOUCHER_VALID_FROM_FUTURE = "Ngày bắt đầu phải từ hiện tại trở đi";
    public static final String VOUCHER_VALID_UNTIL_REQUIRED = "Ngày kết thúc không được để trống";
    public static final String VOUCHER_VALID_UNTIL_FUTURE = "Ngày kết thúc phải từ hiện tại trở đi";

    // Booking Validation Messages
    public static final String BOOKING_FULL_NAME_REQUIRED = "Họ tên người tham gia không được để trống";
    public static final String BOOKING_DOB_REQUIRED = "Ngày sinh không được để trống";
    public static final String BOOKING_GENDER_REQUIRED = "Giới tính không được để trống";
    public static final String BOOKING_ID_NUMBER_REQUIRED = "Số CCCD/Hộ chiếu không được để trống";
    public static final String BOOKING_PHONE_REQUIRED = "Số điện thoại không được để trống";
    public static final String BOOKING_EMAIL_INVALID = "Email không đúng định dạng";
    public static final String BOOKING_SCHEDULE_REQUIRED = "Lịch khởi hành không được để trống";
    public static final String BOOKING_PARTICIPANTS_REQUIRED = "Danh sách người tham gia không được để trống";
    public static final String BOOKING_CANCEL_REASON_REQUIRED = "Lý do huỷ tour không được để trống";
    public static final String BOOKING_PROOF_IMAGE_REQUIRED = "Đường dẫn ảnh minh chứng không được để trống";

    // Vendor Booking Messages
    public static final String PAYMENT_CONFIRMED_SUCCESSFULLY = "Xác nhận thanh toán thành công";
    public static final String BOOKING_CONFIRMED_SUCCESSFULLY = "Xác nhận giữ chỗ đơn đặt tour thành công";
    public static final String REFUND_CONFIRMED_SUCCESSFULLY = "Xác nhận hoàn tiền thành công";
    public static final String BOOKING_NOT_CANCELLED = "Đơn đặt tour chưa ở trạng thái huỷ, không thể xác nhận hoàn tiền";

    // Logistics Messages
    public static final String TOUR_SESSION_NOT_FOUND = "Phiên tour không tồn tại";
    public static final String TOUR_SESSION_ALREADY_STARTED = "Không thể phân công khi tour đã bắt đầu hoặc hoàn thành";
    public static final String COORDINATOR_NOT_FOUND = "Không tìm thấy điều phối viên/hướng dẫn viên";
    public static final String COORDINATOR_SCHEDULE_CONFLICT = "Điều phối viên đã có lịch bị trùng trong khoảng thời gian này";
    public static final String COORDINATOR_IN_PROGRESS_TOUR = "Điều phối viên đang có tour chưa hoàn thành";
    public static final String COORDINATOR_ALREADY_ASSIGNED = "Điều phối viên đã được phân công cho phiên tour này";
    public static final String SCHEDULE_NOT_BELONG_TO_VENDOR = "Lịch phân công không thuộc về công ty của bạn";
    public static final String COORDINATOR_ASSIGNED_SUCCESSFULLY = "Phân công điều phối viên thành công";
    public static final String COORDINATOR_REMOVED_SUCCESSFULLY = "Gỡ bỏ phân công điều phối viên thành công";
    public static final String COORDINATOR_ID_REQUIRED = "HDV (Coordinator) không được để trống";
    public static final String IS_LEAD_REQUIRED = "Trạng thái Lead không được để trống";
    public static final String SCHEDULE_CANCELLED_SUCCESSFULLY = "Huỷ phân công khẩn cấp thành công";
    public static final String SCHEDULE_CANCEL_REASON_REQUIRED = "Lý do huỷ phân công không được để trống";
    public static final String CANCEL_TOO_CLOSE_TO_DEPARTURE = "Chỉ có thể hủy lịch trình trước khi tour khởi hành tối thiểu 1 ngày";

    public static final String PORTER_ASSIGNED_SUCCESSFULLY = "Phân công porter thành công";
    public static final String PORTER_REMOVED_SUCCESSFULLY = "Gỡ bỏ phân công porter thành công";
    public static final String PORTER_ALREADY_ASSIGNED = "Porter này đã được phân công cho phiên tour này";
    public static final String PORTER_SCHEDULE_CONFLICT = "Porter đã có lịch trùng trong thời gian này";
    public static final String PORTER_SCHEDULE_NOT_FOUND = "Lịch phân công porter không tồn tại";
    public static final String PORTER_ID_REQUIRED = "Porter không được để trống";

    public static final String EQUIPMENT_ASSIGNED_SUCCESSFULLY = "Phân bổ trang bị thành công";
    public static final String EQUIPMENT_QUANTITY_UPDATED_SUCCESSFULLY = "Cộng dồn số lượng trang bị thành công";
    public static final String EQUIPMENT_REMOVED_SUCCESSFULLY = "Hủy phân bổ trang bị thành công";
    public static final String EQUIPMENT_RETURNED_SUCCESSFULLY = "Xác nhận trả trang bị thành công";
    public static final String EQUIPMENT_ALREADY_ASSIGNED = "Trang bị này đã được phân bổ cho phiên tour này";
    public static final String NOT_ENOUGH_EQUIPMENT_IN_STOCK = "Số lượng tồn kho trang bị không đủ";
    public static final String SESSION_EQUIPMENT_NOT_FOUND = "Không tìm thấy thông tin phân bổ trang bị";
    public static final String SESSION_EQUIPMENT_NOT_BELONG_TO_VENDOR = "Thông tin phân bổ trang bị không thuộc về công ty của bạn";
    public static final String INVALID_RETURN_QUANTITY = "Số lượng trả về không hợp lệ (tổng vượt quá số lượng mượn)";
    public static final String EQUIPMENT_ID_REQUIRED = "Trang bị không được để trống";
    public static final String QUANTITY_REQUIRED_AND_MIN = "Số lượng phải lớn hơn 0";
    public static final String RETURN_QUANTITY_REQUIRED = "Số lượng trả về nguyên vẹn không được để trống";
    public static final String MISSING_QUANTITY_REQUIRED = "Số lượng thất lạc/hư hỏng không được để trống";
    // Review Messages
    public static final String REVIEW_NOT_FOUND = "Đánh giá không tồn tại";
    public static final String REVIEW_CREATED_SUCCESSFULLY = "Gửi đánh giá thành công";
    public static final String REVIEW_STATUS_UPDATED_SUCCESSFULLY = "Cập nhật trạng thái đánh giá thành công";
    public static final String REVIEW_ALREADY_EXISTS = "Bạn đã đánh giá đơn đặt tour này rồi";
    public static final String REVIEW_BOOKING_NOT_COMPLETED = "Chỉ có thể đánh giá khi đơn đặt tour đã hoàn thành";
    public static final String REVIEW_BOOKING_NOT_OWNED = "Bạn không có quyền đánh giá đơn đặt tour này";
    public static final String REVIEW_INVALID_STATUS_MSG = "Trạng thái đánh giá không hợp lệ (phải là APPROVED hoặc HIDDEN)";
    public static final String REVIEW_RATING_REQUIRED = "Điểm đánh giá không được để trống";
    public static final String REVIEW_RATING_RANGE = "Điểm đánh giá phải từ 1 đến 5";
    public static final String REVIEW_BOOKING_REQUIRED = "Mã đơn đặt tour không được để trống";
    public static final String REVIEW_STATUS_REQUIRED_MSG = "Trạng thái đánh giá không được để trống";
    // Chat Messages
    public static final String CONVERSATIONS_FETCHED_SUCCESS = "Lấy danh sách cuộc hội thoại thành công";
    public static final String CONVERSATION_CREATED_SUCCESS = "Tạo cuộc hội thoại thành công";
    public static final String CONVERSATION_NOT_FOUND = "Cuộc hội thoại không tồn tại";
    public static final String CONVERSATION_ALREADY_EXISTS = "Cuộc hội thoại 1-1 với người dùng này đã tồn tại";
    public static final String MESSAGES_FETCHED_SUCCESS = "Lấy lịch sử tin nhắn thành công";
    public static final String MESSAGE_SENT_SUCCESS = "Gửi tin nhắn thành công";
    public static final String MESSAGES_MARKED_READ_SUCCESS = "Đánh dấu đã đọc tin nhắn thành công";
    public static final String RECIPIENT_NOT_FOUND = "Người nhận tin nhắn không tồn tại";
    public static final String CANNOT_CHAT_WITH_SELF = "Bạn không thể tạo cuộc hội thoại với chính mình";
    public static final String CONVERSATION_TYPE_REQUIRED = "Loại cuộc hội thoại không được để trống";
    public static final String CONVERSATION_PARTICIPANTS_REQUIRED = "Danh sách người tham gia không được để trống";
    public static final String MESSAGE_CONVERSATION_ID_REQUIRED = "Mã cuộc hội thoại không được để trống";
    public static final String MESSAGE_CONTENT_REQUIRED = "Nội dung tin nhắn không được để trống";
    public static final String DIRECT_PARTICIPANT_COUNT_INVALID = "Cuộc hội thoại 1-1 phải có đúng một người nhận";
    public static final String GROUP_PARTICIPANT_COUNT_INVALID = "Cuộc hội thoại nhóm phải có ít nhất hai người tham gia";
    public static final String GROUP_TITLE_REQUIRED = "Tên cuộc hội thoại nhóm không được để trống";

    // Report Messages
    public static final String REPORT_NOT_FOUND = "Báo cáo vi phạm không tồn tại";
    public static final String REPORT_TARGET_NOT_FOUND = "Nội dung bị báo cáo không tồn tại";
    public static final String REPORT_CREATED_SUCCESS = "Gửi báo cáo vi phạm thành công";
    public static final String REPORTS_FETCHED_SUCCESS = "Lấy danh sách báo cáo vi phạm thành công";
    public static final String REPORT_RESOLVED_SUCCESS = "Xử lý báo cáo vi phạm thành công";
    public static final String REPORT_ALREADY_RESOLVED = "Báo cáo này đã được xử lý";
    public static final String REPORT_TARGET_ID_REQUIRED = "ID nội dung báo cáo không được để trống";
    public static final String REPORT_TARGET_TYPE_REQUIRED = "Loại nội dung báo cáo không được để trống";
    public static final String REPORT_REASON_REQUIRED = "Lý do báo cáo không được để trống";
    public static final String REPORT_ACTION_REQUIRED = "Hành động xử lý không được để trống";
}


