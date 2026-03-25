package petcare.example.community_backend.util;

import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * GZIP 压缩/解压工具类
 * 用于冷数据归档时的 JSON 压缩，节省 HBase 存储空间
 *
 * 设计原则：
 * 1. 压缩率约 70-80%，可有效减少存储体积
 * 2. GZIP 开销 < 5ms，性能可接受
 * 3. 统一处理字符串和字节数组两种格式
 */
@Slf4j
public final class GzipUtils {

    private GzipUtils() {
        // 工具类不允许实例化
    }

    /**
     * 压缩字符串为 GZIP 字节数组
     *
     * @param data 要压缩的字符串
     * @return 压缩后的字节数组
     */
    public static byte[] compress(String data) {
        if (data == null || data.isEmpty()) {
            return new byte[0];
        }

        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             GZIPOutputStream gzip = new GZIPOutputStream(bos)) {

            gzip.write(data.getBytes(StandardCharsets.UTF_8));
            gzip.finish();

            byte[] compressed = bos.toByteArray();
            log.debug("【GZIP】压缩成功: 原始大小={} bytes, 压缩后={} bytes, 压缩率={}%",
                    data.getBytes(StandardCharsets.UTF_8).length,
                    compressed.length,
                    (1 - (double) compressed.length / data.getBytes(StandardCharsets.UTF_8).length) * 100);

            return compressed;

        } catch (IOException e) {
            log.error("【GZIP】压缩失败: {}", e.getMessage(), e);
            throw new RuntimeException("GZIP压缩失败", e);
        }
    }

    /**
     * 解压 GZIP 字节数组为字符串
     *
     * @param compressed 压缩后的字节数组
     * @return 解压后的字符串
     */
    public static String decompress(byte[] compressed) {
        if (compressed == null || compressed.length == 0) {
            return "";
        }

        try (ByteArrayInputStream bis = new ByteArrayInputStream(compressed);
             GZIPInputStream gzip = new GZIPInputStream(bis);
             ByteArrayOutputStream bos = new ByteArrayOutputStream()) {

            byte[] buffer = new byte[1024];
            int len;
            while ((len = gzip.read(buffer)) != -1) {
                bos.write(buffer, 0, len);
            }

            String decompressed = bos.toString(StandardCharsets.UTF_8);
            log.debug("【GZIP】解压成功: 压缩大小={} bytes, 原始大小={} bytes",
                    compressed.length, decompressed.getBytes(StandardCharsets.UTF_8).length);

            return decompressed;

        } catch (IOException e) {
            log.error("【GZIP】解压失败: {}", e.getMessage(), e);
            throw new RuntimeException("GZIP解压失败", e);
        }
    }

    /**
     * 检查字节数组是否已压缩（GZIP 格式头部标识）
     *
     * @param data 字节数组
     * @return true 如果数据已压缩
     */
    public static boolean isCompressed(byte[] data) {
        if (data == null || data.length < 2) {
            return false;
        }
        // GZIP 魔数: 0x1f 0x8b
        return (data[0] == (byte) 0x1f) && (data[1] == (byte) 0x8b);
    }

    /**
     * 安全压缩：如果数据太小（< 100 bytes），不压缩直接返回原字节
     * 小数据压缩后反而可能变大
     *
     * @param data 要处理的字符串
     * @return 字节数组（压缩或原格式）
     */
    public static byte[] compressIfNeeded(String data) {
        if (data == null || data.isEmpty()) {
            return new byte[0];
        }

        byte[] rawBytes = data.getBytes(StandardCharsets.UTF_8);
        // 小于 100 字节不压缩
        if (rawBytes.length < 100) {
            log.debug("【GZIP】数据太小不压缩: {} bytes", rawBytes.length);
            return rawBytes;
        }

        return compress(data);
    }

    /**
     * 安全解压：根据数据是否压缩自动选择解压或直接返回
     *
     * @param data 字节数组
     * @return 解压后的字符串
     */
    public static String decompressIfNeeded(byte[] data) {
        if (data == null || data.length == 0) {
            return "";
        }

        if (isCompressed(data)) {
            return decompress(data);
        } else {
            // 未压缩，直接返回
            return new String(data, StandardCharsets.UTF_8);
        }
    }
}
