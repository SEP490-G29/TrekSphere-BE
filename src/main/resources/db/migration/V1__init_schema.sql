
    create table blog (
        is_deleted boolean not null,
        view_count integer not null,
        created_at timestamp(6) not null,
        deleted_at timestamp(6),
        updated_at timestamp(6),
        blogid uuid not null,
        user_id uuid not null,
        status varchar(20) not null check (status in ('DRAFT','PUBLISHED','HIDDEN','DELETED')),
        cover_image_url varchar(500),
        title varchar(500) not null,
        content TEXT not null,
        created_by varchar(255),
        deleted_by varchar(255),
        updated_by varchar(255),
        primary key (blogid)
    );

    create table blog_comment (
        is_deleted boolean not null,
        created_at timestamp(6) not null,
        deleted_at timestamp(6),
        updated_at timestamp(6),
        status varchar(10) not null check (status in ('ACTIVE','HIDDEN','DELETED')),
        blog_id uuid not null,
        commentid uuid not null,
        parent_comment_id uuid,
        user_id uuid not null,
        content TEXT not null,
        created_by varchar(255),
        deleted_by varchar(255),
        updated_by varchar(255),
        primary key (commentid)
    );

    create table booking (
        discount_amount numeric(12,2) not null,
        is_deleted boolean not null,
        number_of_participants integer not null,
        refund_amount numeric(12,2),
        total_price numeric(12,2) not null,
        cancelled_at timestamp(6),
        created_at timestamp(6) not null,
        deleted_at timestamp(6),
        updated_at timestamp(6),
        booking_status varchar(10) not null check (booking_status in ('PENDING','CONFIRMED','CANCELLED','COMPLETED')),
        bookingid uuid not null,
        cancelled_by_user_id uuid,
        schedule_id uuid not null,
        user_id uuid not null,
        voucher_id uuid,
        payment_status varchar(20) not null check (payment_status in ('PENDING','PAID','REFUNDED','PARTIALLY_REFUNDED')),
        cancellation_reason TEXT,
        created_by varchar(255),
        deleted_by varchar(255),
        updated_by varchar(255),
        primary key (bookingid)
    );

    create table booking_participant (
        date_of_birth date not null,
        info_confirmed boolean not null,
        gender varchar(10) not null check (gender in ('MALE','FEMALE','OTHER')),
        booking_id uuid not null,
        participantid uuid not null,
        id_number varchar(20) not null,
        phone varchar(20) not null,
        email varchar(255),
        emergency_contact varchar(255) not null,
        full_name varchar(255) not null,
        special_requirements TEXT,
        primary key (participantid)
    );

    create table cancellation_policy (
        days_before_departure integer not null,
        is_active boolean not null,
        is_deleted boolean not null,
        refund_percentage integer not null,
        created_at timestamp(6) not null,
        deleted_at timestamp(6),
        updated_at timestamp(6),
        policyid uuid not null,
        vendor_id uuid not null,
        created_by varchar(255),
        deleted_by varchar(255),
        description varchar(255),
        updated_by varchar(255),
        primary key (policyid)
    );

    create table conversation (
        is_deleted boolean not null,
        created_at timestamp(6) not null,
        deleted_at timestamp(6),
        last_message_at timestamp(6),
        updated_at timestamp(6),
        status varchar(10) not null check (status in ('ACTIVE','ARCHIVED')),
        conversationid uuid not null,
        conversation_type varchar(20) not null check (conversation_type in ('DIRECT','GROUP','MATCHING_GROUP')),
        created_by varchar(255),
        deleted_by varchar(255),
        title varchar(255),
        updated_by varchar(255),
        primary key (conversationid)
    );

    create table conversation_participant (
        conversation_id uuid not null,
        user_id uuid not null,
        primary key (conversation_id, user_id)
    );

    create table matching_group (
        current_size integer not null,
        is_deleted boolean not null,
        max_size integer not null,
        target_date date,
        created_at timestamp(6) not null,
        deleted_at timestamp(6),
        updated_at timestamp(6),
        status varchar(10) not null check (status in ('OPEN','FULL','CLOSED','HIDDEN')),
        groupid uuid not null,
        owner_user_id uuid not null,
        tour_id uuid not null,
        created_by varchar(255),
        deleted_by varchar(255),
        description TEXT,
        group_name varchar(255) not null,
        updated_by varchar(255),
        primary key (groupid)
    );

    create table matching_member (
        joined_at timestamp(6) not null,
        left_at timestamp(6),
        join_status varchar(10) not null check (join_status in ('PENDING','ACCEPTED','REJECTED','LEFT')),
        role varchar(10) not null check (role in ('OWNER','MEMBER')),
        group_id uuid not null,
        memberid uuid not null,
        user_id uuid not null,
        primary key (memberid),
        unique (group_id, user_id)
    );

    create table message (
        is_deleted boolean not null,
        is_read boolean not null,
        created_at timestamp(6) not null,
        deleted_at timestamp(6),
        updated_at timestamp(6),
        message_type varchar(10) not null check (message_type in ('TEXT','IMAGE','SYSTEM')),
        conversation_id uuid not null,
        messageid uuid not null,
        sender_user_id uuid not null,
        content TEXT not null,
        created_by varchar(255),
        deleted_by varchar(255),
        updated_by varchar(255),
        primary key (messageid)
    );

    create table notification (
        is_deleted boolean not null,
        is_read boolean not null,
        created_at timestamp(6) not null,
        deleted_at timestamp(6),
        updated_at timestamp(6),
        notificationid uuid not null,
        referenceid uuid,
        user_id uuid not null,
        event_type varchar(50) not null check (event_type in ('BOOKING_CONFIRMED','BOOKING_CANCELLED','PAYMENT_SUCCESS','NEW_MESSAGE','TOUR_APPROVED','GROUP_JOIN_REQUEST')),
        reference_type varchar(50) check (reference_type in ('BOOKING','TOUR','BLOG','MATCHING_GROUP','CONVERSATION')),
        created_by varchar(255),
        deleted_by varchar(255),
        message TEXT not null,
        title varchar(255) not null,
        updated_by varchar(255),
        primary key (notificationid)
    );

    create table permission (
        permissionid uuid not null,
        action varchar(50) not null,
        resource varchar(100) not null,
        description varchar(255),
        primary key (permissionid),
        unique (resource, action)
    );

    create table refresh_token (
        created_at timestamp(6) not null,
        expires_at timestamp(6) not null,
        revoked_at timestamp(6),
        status varchar(10) not null check (status in ('ACTIVE','REVOKED','EXPIRED')),
        tokenid uuid not null,
        user_id uuid not null,
        token varchar(500) not null,
        primary key (tokenid)
    );

    create table report_content (
        reported_at timestamp(6) not null,
        resolved_at timestamp(6),
        status varchar(10) not null check (status in ('PENDING','REVIEWED','RESOLVED','DISMISSED')),
        target_type varchar(10) not null check (target_type in ('BLOG','COMMENT','REVIEW')),
        moderated_by_user_id uuid,
        reporter_user_id uuid not null,
        reportid uuid not null,
        targetid uuid not null,
        detail TEXT,
        moderation_decision TEXT,
        reason varchar(255) not null,
        primary key (reportid)
    );

    create table review (
        is_deleted boolean not null,
        rating integer not null,
        created_at timestamp(6) not null,
        deleted_at timestamp(6),
        updated_at timestamp(6),
        status varchar(10) not null check (status in ('PENDING','APPROVED','HIDDEN')),
        booking_id uuid not null unique,
        reviewid uuid not null,
        tour_id uuid not null,
        user_id uuid not null,
        content TEXT,
        created_by varchar(255),
        deleted_by varchar(255),
        updated_by varchar(255),
        primary key (reviewid)
    );

    create table role (
        roleid uuid not null,
        role_name varchar(50) not null unique,
        description varchar(255),
        primary key (roleid)
    );

    create table role_permission (
        permission_id uuid not null,
        role_id uuid not null,
        primary key (permission_id, role_id)
    );

    create table system_policy (
        is_deleted boolean not null,
        created_at timestamp(6) not null,
        deleted_at timestamp(6),
        updated_at timestamp(6),
        policyid uuid not null,
        updated_by_user_id uuid,
        data_type varchar(20) not null check (data_type in ('STRING','INTEGER','FLOAT','BOOLEAN','JSON')),
        config_key varchar(100) not null unique,
        config_value TEXT not null,
        created_by varchar(255),
        deleted_by varchar(255),
        description varchar(255),
        updated_by varchar(255),
        primary key (policyid)
    );

    create table tour (
        base_price numeric(12,2) not null,
        duration_days integer not null,
        is_deleted boolean not null,
        max_capacity integer not null,
        created_at timestamp(6) not null,
        deleted_at timestamp(6),
        updated_at timestamp(6),
        difficulty varchar(10) not null check (difficulty in ('EASY','MODERATE','HARD','EXPERT')),
        created_by_user_id uuid not null,
        tourid uuid not null,
        vendor_id uuid not null,
        status varchar(20) not null check (status in ('DRAFT','PENDING_APPROVAL','APPROVED','REJECTED','HIDDEN')),
        cover_image_url varchar(500),
        created_by varchar(255),
        deleted_by varchar(255),
        description TEXT not null,
        excludes TEXT,
        highlights TEXT,
        includes TEXT,
        location varchar(255) not null,
        tour_name varchar(255) not null,
        updated_by varchar(255),
        primary key (tourid)
    );

    create table tour_image (
        sort_order integer not null,
        created_at timestamp(6) not null,
        imageid uuid not null,
        tour_id uuid not null,
        image_url varchar(500) not null,
        caption varchar(255),
        primary key (imageid)
    );

    create table tour_schedule (
        available_slots integer not null,
        booked_slots integer not null,
        departure_date date not null,
        is_deleted boolean not null,
        price numeric(12,2) not null,
        return_date date not null,
        created_at timestamp(6) not null,
        deleted_at timestamp(6),
        updated_at timestamp(6),
        status varchar(10) not null check (status in ('OPEN','CLOSED','CANCELLED','COMPLETED')),
        scheduleid uuid not null,
        tour_id uuid not null,
        created_by varchar(255),
        deleted_by varchar(255),
        updated_by varchar(255),
        primary key (scheduleid)
    );

    create table transaction (
        amount numeric(12,2) not null,
        currency varchar(3) not null,
        is_deleted boolean not null,
        created_at timestamp(6) not null,
        deleted_at timestamp(6),
        paid_at timestamp(6),
        updated_at timestamp(6),
        status varchar(10) not null check (status in ('PENDING','SUCCESS','FAILED','REFUNDED')),
        booking_id uuid not null,
        transactionid uuid not null,
        payment_gateway varchar(50) not null,
        created_by varchar(255),
        deleted_by varchar(255),
        gateway_ref varchar(255),
        updated_by varchar(255),
        primary key (transactionid)
    );

    create table user_role (
        role_id uuid not null,
        user_id uuid not null,
        primary key (role_id, user_id)
    );

    create table users (
        date_of_birth date,
        email_verified boolean not null,
        is_deleted boolean not null,
        created_at timestamp(6) not null,
        deleted_at timestamp(6),
        updated_at timestamp(6),
        gender varchar(10) check (gender in ('MALE','FEMALE','OTHER')),
        userid uuid not null,
        phone varchar(20),
        status varchar(20) not null check (status in ('ACTIVE','LOCKED','DEACTIVATED')),
        avatar_url varchar(500),
        created_by varchar(255),
        deleted_by varchar(255),
        email varchar(255) not null unique,
        full_name varchar(255) not null,
        password_hash varchar(255),
        updated_by varchar(255),
        primary key (userid)
    );

    create table vendor (
        is_deleted boolean not null,
        created_at timestamp(6) not null,
        deleted_at timestamp(6),
        updated_at timestamp(6),
        status varchar(10) not null check (status in ('ACTIVE','SUSPENDED','REVOKED')),
        manager_user_id uuid not null unique,
        vendorid uuid not null,
        contact_phone varchar(20) not null,
        bank_account varchar(50),
        bank_name varchar(100),
        logo_url varchar(500),
        company_name varchar(255) not null,
        contact_email varchar(255) not null,
        created_by varchar(255),
        deleted_by varchar(255),
        description TEXT,
        updated_by varchar(255),
        primary key (vendorid)
    );

    create table vendor_application (
        is_deleted boolean not null,
        created_at timestamp(6) not null,
        deleted_at timestamp(6),
        reviewed_at timestamp(6),
        updated_at timestamp(6),
        application_status varchar(10) not null check (application_status in ('PENDING','APPROVED','REJECTED')),
        applicant_user_id uuid not null,
        applicationid uuid not null,
        reviewed_by uuid,
        contact_phone varchar(20) not null,
        business_description TEXT,
        company_name varchar(255) not null,
        contact_email varchar(255) not null,
        created_by varchar(255),
        deleted_by varchar(255),
        rejection_reason TEXT,
        updated_by varchar(255),
        primary key (applicationid)
    );

    create table vendor_staff (
        is_active boolean not null,
        is_deleted boolean not null,
        created_at timestamp(6) not null,
        deactivated_at timestamp(6),
        deleted_at timestamp(6),
        updated_at timestamp(6),
        user_id uuid not null unique,
        vendor_id uuid not null,
        vendor_staffid uuid not null,
        created_by varchar(255),
        deleted_by varchar(255),
        updated_by varchar(255),
        primary key (vendor_staffid)
    );

    create table voucher (
        discount_value numeric(12,2) not null,
        is_deleted boolean not null,
        max_usage integer not null,
        min_order_value numeric(12,2) not null,
        used_count integer not null,
        created_at timestamp(6) not null,
        deleted_at timestamp(6),
        updated_at timestamp(6),
        valid_from timestamp(6) not null,
        valid_until timestamp(6) not null,
        approval_status varchar(10) not null check (approval_status in ('PENDING','APPROVED','REJECTED')),
        status varchar(10) not null check (status in ('ACTIVE','INACTIVE','EXPIRED')),
        vendor_id uuid not null,
        voucherid uuid not null,
        discount_type varchar(20) not null check (discount_type in ('PERCENTAGE','FIXED_AMOUNT')),
        code varchar(50) not null unique,
        created_by varchar(255),
        deleted_by varchar(255),
        updated_by varchar(255),
        primary key (voucherid)
    );

    alter table if exists blog 
       add constraint FKkr2fy24puc3x3sdnla4r1iok1 
       foreign key (user_id) 
       references users;

    alter table if exists blog_comment 
       add constraint FKb9cpog8ie2cyapsyyt7gikpbl 
       foreign key (blog_id) 
       references blog;

    alter table if exists blog_comment 
       add constraint FKnm1ssnjmp89hrvt255u1xlv90 
       foreign key (parent_comment_id) 
       references blog_comment;

    alter table if exists blog_comment 
       add constraint FKrd59qyd7xdutfpjl05212w526 
       foreign key (user_id) 
       references users;

    alter table if exists booking 
       add constraint FKfnw7tsc5xwbpyh5ehpvqp22vm 
       foreign key (cancelled_by_user_id) 
       references users;

    alter table if exists booking 
       add constraint FKi6bht551tm4hq20vj4dn1rhpw 
       foreign key (schedule_id) 
       references tour_schedule;

    alter table if exists booking 
       add constraint FK7udbel7q86k041591kj6lfmvw 
       foreign key (user_id) 
       references users;

    alter table if exists booking 
       add constraint FKbs6twtq6v6sobgvl1gt6v1lan 
       foreign key (voucher_id) 
       references voucher;

    alter table if exists booking_participant 
       add constraint FKfr36euk0d5896yrs0lspakpxy 
       foreign key (booking_id) 
       references booking;

    alter table if exists cancellation_policy 
       add constraint FKdl8kymki2kl4nfji3pmjp9w0d 
       foreign key (vendor_id) 
       references vendor;

    alter table if exists conversation_participant 
       add constraint FKfhu6wpoqip0lkdrpwj2xbfgan 
       foreign key (user_id) 
       references users;

    alter table if exists conversation_participant 
       add constraint FK93dv599s56uqs8xslhdp3arya 
       foreign key (conversation_id) 
       references conversation;

    alter table if exists matching_group 
       add constraint FKgfwl06dnsq6sm102x51boxg9m 
       foreign key (owner_user_id) 
       references users;

    alter table if exists matching_group 
       add constraint FKlcvwaw2iqxutp2jfdlxujsqs6 
       foreign key (tour_id) 
       references tour;

    alter table if exists matching_member 
       add constraint FKtrov6yet3h88b6xkksioqnnig 
       foreign key (group_id) 
       references matching_group;

    alter table if exists matching_member 
       add constraint FK7bwiqsbwwobv2wbisa5e93grj 
       foreign key (user_id) 
       references users;

    alter table if exists message 
       add constraint FK6yskk3hxw5sklwgi25y6d5u1l 
       foreign key (conversation_id) 
       references conversation;

    alter table if exists message 
       add constraint FKcjpqfyeygciwel5ca0uet94 
       foreign key (sender_user_id) 
       references users;

    alter table if exists notification 
       add constraint FKnk4ftb5am9ubmkv1661h15ds9 
       foreign key (user_id) 
       references users;

    alter table if exists refresh_token 
       add constraint FKjtx87i0jvq2svedphegvdwcuy 
       foreign key (user_id) 
       references users;

    alter table if exists report_content 
       add constraint FKo2mdtr4aejvtjv25ius4vkkwh 
       foreign key (moderated_by_user_id) 
       references users;

    alter table if exists report_content 
       add constraint FKs4jn7wcgbgf1q0b32we1sevnr 
       foreign key (reporter_user_id) 
       references users;

    alter table if exists review 
       add constraint FKk4xawqohtguy5yx5nnpba6yf3 
       foreign key (booking_id) 
       references booking;

    alter table if exists review 
       add constraint FK2yxuruefnrj0xan64vi2gg7ag 
       foreign key (tour_id) 
       references tour;

    alter table if exists review 
       add constraint FK6cpw2nlklblpvc7hyt7ko6v3e 
       foreign key (user_id) 
       references users;

    alter table if exists role_permission 
       add constraint FKf8yllw1ecvwqy3ehyxawqa1qp 
       foreign key (permission_id) 
       references permission;

    alter table if exists role_permission 
       add constraint FKa6jx8n8xkesmjmv6jqug6bg68 
       foreign key (role_id) 
       references role;

    alter table if exists system_policy 
       add constraint FK8uaquf90r4k3ucvuwjxlh1iwk 
       foreign key (updated_by_user_id) 
       references users;

    alter table if exists tour 
       add constraint FK7jbjsuwk8aikjm6dfh96vw0gt 
       foreign key (created_by_user_id) 
       references users;

    alter table if exists tour 
       add constraint FKl75a0o7midp5dybsjtqk37056 
       foreign key (vendor_id) 
       references vendor;

    alter table if exists tour_image 
       add constraint FKe829px6ivuaq4s9chmdn2wree 
       foreign key (tour_id) 
       references tour;

    alter table if exists tour_schedule 
       add constraint FK4pywqoa7g8xmv2yv4816wonrs 
       foreign key (tour_id) 
       references tour;

    alter table if exists transaction 
       add constraint FKhaefbm92emogmcjh4kuk3esti 
       foreign key (booking_id) 
       references booking;

    alter table if exists user_role 
       add constraint FKa68196081fvovjhkek5m97n3y 
       foreign key (role_id) 
       references role;

    alter table if exists user_role 
       add constraint FKj345gk1bovqvfame88rcx7yyx 
       foreign key (user_id) 
       references users;

    alter table if exists vendor 
       add constraint FKprqofv07qv2huyg6xf81e7rfx 
       foreign key (manager_user_id) 
       references users;

    alter table if exists vendor_application 
       add constraint FK16am92cv7sbpcj0j03q3yctdg 
       foreign key (applicant_user_id) 
       references users;

    alter table if exists vendor_application 
       add constraint FK6uh45h40ujma11b94543rt6ye 
       foreign key (reviewed_by) 
       references users;

    alter table if exists vendor_staff 
       add constraint FKeu5wqjjucp2vngqofi83g8l51 
       foreign key (user_id) 
       references users;

    alter table if exists vendor_staff 
       add constraint FKik6y3fbxd3e4sabfhs2imya16 
       foreign key (vendor_id) 
       references vendor;

    alter table if exists voucher 
       add constraint FK1511dagraplcqidgbm3g2fdu0 
       foreign key (vendor_id) 
       references vendor;
