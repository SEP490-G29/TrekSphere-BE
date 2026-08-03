package com.sep.treksphere.utils;

import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class VietQrUtils {

    private static final Map<String, String> BANK_MAP = new HashMap<>();

    static {
        registerBank("VCB", "vietcombank", "vcb", "ngoai thuong", "970436");
        registerBank("MB", "mbbank", "mb bank", "mb", "quan doi", "970422");
        registerBank("TCB", "techcombank", "tcb", "kieu hoi", "970407");
        registerBank("BIDV", "bidv", "dau tu va phat trien", "970418");
        registerBank("ICB", "vietinbank", "vtb", "cong thuong", "970415");
        registerBank("ACB", "acb", "a chau", "970416");
        registerBank("VPB", "vpbank", "vpb", "thinh vuong", "970432");
        registerBank("TPB", "tpbank", "tpb", "tien phong", "970423");
        registerBank("VBA", "agribank", "nong nghiep", "970405");
        registerBank("STB", "sacombank", "stb", "sai gon thuong tin", "970403");
        registerBank("HDB", "hdbank", "hdb", "phat trien tp hcm", "970437");
        registerBank("VIB", "vib", "quoc te", "970441");
        registerBank("SHB", "shb", "sai gon ha noi", "970443");
        registerBank("OCB", "ocb", "phuong dong", "970448");
        registerBank("MSB", "msb", "maritime bank", "hang hai", "970426");
        registerBank("LPB", "lpbank", "lienvietpostbank", "loc phat", "970449");
        registerBank("EIB", "eximbank", "eib", "xuat nhap khau", "970431");
        registerBank("SEAB", "seabank", "dong nam a", "970440");
        registerBank("BAB", "bacabank", "bac a", "970409");
        registerBank("VAB", "vietabank", "viet a", "970427");
        registerBank("NCB", "ncb", "quoc dan", "970419");
        registerBank("ABB", "abbank", "an binh", "970425");
        registerBank("SCB", "scb", "sai gon", "970429");
    }

    private static void registerBank(String standardCode, String... keywords) {
        BANK_MAP.put(standardCode.toLowerCase(), standardCode);
        for (String kw : keywords) {
            BANK_MAP.put(kw.toLowerCase(), standardCode);
        }
    }

    public static String resolveBankId(String bankName) {
        if (!StringUtils.hasText(bankName)) {
            return null;
        }

        String normalized = removeAccent(bankName.toLowerCase().trim())
                .replaceAll("[^a-z0-9]", " ")
                .replaceAll("\\s+", " ")
                .trim();

        if (BANK_MAP.containsKey(normalized)) {
            return BANK_MAP.get(normalized);
        }
        for (Map.Entry<String, String> entry : BANK_MAP.entrySet()) {
            if (normalized.contains(entry.getKey())) {
                return entry.getValue();
            }
        }

        return normalized.replaceAll("\\s+", "").toUpperCase();
    }
    public static String generateVietQrUrl(String bankName, String bankAccount, BigDecimal amount, String addInfo, String accountName) {
        String bankId = resolveBankId(bankName);
        if (!StringUtils.hasText(bankId) || !StringUtils.hasText(bankAccount)) {
            return null;
        }

        String cleanAccount = bankAccount.trim().replaceAll("\\s+", "");
        String amountStr = (amount != null && amount.compareTo(BigDecimal.ZERO) > 0)
                ? amount.toPlainString()
                : "0";

        String encodedAddInfo = encodeParam(addInfo != null ? addInfo.trim() : "");
        String encodedAccountName = encodeParam(accountName != null ? accountName.trim() : "");

        return String.format("https://img.vietqr.io/image/%s-%s-compact.png?amount=%s&addInfo=%s&accountName=%s",
                bankId, cleanAccount, amountStr, encodedAddInfo, encodedAccountName);
    }

    private static String encodeParam(String param) {
        if (!StringUtils.hasText(param)) {
            return "";
        }
        return URLEncoder.encode(param, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private static String removeAccent(String s) {
        if (s == null) return "";
        String temp = Normalizer.normalize(s, Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        return pattern.matcher(temp).replaceAll("").replace('đ', 'd').replace('Đ', 'D');
    }
}
