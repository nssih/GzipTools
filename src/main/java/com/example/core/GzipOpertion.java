package com.example.core;

import com.example.constant.Constants;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.zip.*;

public class GzipOpertion {

    // 对内容进行压缩的处理
    public static String compressSafe(String input, boolean noTime) {
        try {
            // 1. 输入校验
            if (input == null || input.trim().isEmpty()) {
                return Constants.ERR + "输入为空，请先选中内容";
            }

            // 2. 长度限制（防止卡IDE）
            if (input.getBytes(StandardCharsets.UTF_8).length > 10 * 1024 * 1024) {
                return Constants.ERR + "文本过大（超过10MB），不建议压缩";
            }

            // 3. 执行压缩
            if (noTime) {
                return compressNoTime(input);
            } else {
                return compressWithTime(input);
            }

        } catch (OutOfMemoryError e) {
            return Constants.ERR + "内存不足：输入内容太大";

        } catch (Exception e) {
            return Constants.ERR + "压缩失败：\n" + cleanMsg(e.getMessage());
        }
    }

    // 不带时间戳的压缩方法
    public static String compressNoTime(String input) {
        try {
            byte[] data = input.getBytes(StandardCharsets.UTF_8);
            ByteArrayOutputStream out = new ByteArrayOutputStream();

            // 1. 写 GZIP Header（mtime=0）
            out.write(new byte[]{
                    (byte) 0x1f, (byte) 0x8b, // magic
                    (byte) 0x08,              // compression method (deflate)
                    (byte) 0x00,              // flags
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, // mtime = 0
                    (byte) 0x00,              // extra flags
                    (byte) 0x00               // OS
            });

            // 2. 压缩数据（使用 raw deflate）
            Deflater deflater = new Deflater(Deflater.DEFAULT_COMPRESSION, true);
            deflater.setInput(data);
            deflater.finish();

            byte[] buffer = new byte[512];
            while (!deflater.finished()) {
                int len = deflater.deflate(buffer);
                out.write(buffer, 0, len);
            }
            deflater.end();

            // 3. 写 CRC32 和原始长度
            CRC32 crc = new CRC32();
            crc.update(data);

            writeInt(out, (int) crc.getValue());
            writeInt(out, data.length);

            // 4. Base64编码
            return Base64.getEncoder().encodeToString(out.toByteArray());

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // 小端写入（gzip要求）
    private static void writeInt(ByteArrayOutputStream out, int value) {
        out.write(value & 0xff);
        out.write((value >> 8) & 0xff);
        out.write((value >> 16) & 0xff);
        out.write((value >> 24) & 0xff);
    }


    // 带时间戳的压缩方法
    public static String compressWithTime(String input) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            GZIPOutputStream gzip = new GZIPOutputStream(baos);

            gzip.write(input.getBytes(StandardCharsets.UTF_8));
            gzip.close();

            return Base64.getEncoder().encodeToString(baos.toByteArray());

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // 解压内容的方法
    public static String uncompress(String base64) {

        try {
            // 清理空格
            base64 = base64.replaceAll("\\s", "");

            // 补 padding
            int mod = base64.length() % 4;
            if (mod != 0) {
                base64 += "====".substring(mod);
            }

            byte[] compressed = Base64.getDecoder().decode(base64);

            GZIPInputStream gis = new GZIPInputStream(new ByteArrayInputStream(compressed));
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(gis, StandardCharsets.UTF_8)
            );

            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }

            return sb.toString();

        } catch (IllegalArgumentException e) {
            // Base64错误
            return Constants.ERR + "Base64格式错误：\n" + cleanMsg(e.getMessage());

        } catch (ZipException e) {
            // gzip错误（很关键）
            return Constants.ERR + "GZIP格式错误：数据可能不是gzip\n" + cleanMsg(e.getMessage());

        } catch (IOException e) {
            // 其他IO错误
            return Constants.ERR + "解压IO错误：\n" + cleanMsg(e.getMessage());

        } catch (Exception e) {
            return Constants.ERR + "未知错误：\n" + cleanMsg(e.getMessage());
        }

    }

    private static String cleanMsg(String msg) {
        if (msg == null) {
            return "未知原因";
        }

        if (msg.contains("incorrect ending byte")) {
            return "Base64字符串不完整（可能缺少 = 或被截断）";
        }

        if (msg.contains("Not in GZIP format")) {
            return "输入数据不是合法的 GZIP 数据";
        }

        if (msg.contains("Deflater")) {
            return "压缩器错误（可能输入异常）";
        }

        return msg;
    }





}
