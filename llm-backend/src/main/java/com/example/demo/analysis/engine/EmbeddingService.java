package com.example.demo.analysis.engine;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

@Slf4j
@Service
public class EmbeddingService {

    private static final int DIM = 768;

    @PostConstruct
    public void init() {
        log.info("嵌入模型就绪（模拟模式）");
    }

    public float[] embed(String text) {
        return generateMockVector(text);
    }

    public byte[] floatToBytes(float[] v) {
        ByteBuffer buf = ByteBuffer.allocate(v.length * 4);
        buf.order(ByteOrder.LITTLE_ENDIAN);
        for (float f : v) buf.putFloat(f);
        return buf.array();
    }

    public float[] bytesToFloat(byte[] b) {
        if (b == null) return new float[DIM];
        ByteBuffer buf = ByteBuffer.wrap(b);
        buf.order(ByteOrder.LITTLE_ENDIAN);
        float[] v = new float[b.length / 4];
        for (int i = 0; i < v.length; i++) v[i] = buf.getFloat();
        return v;
    }

    private float[] generateMockVector(String text) {
        float[] v = new float[DIM];
        int hash = text.hashCode();
        for (int i = 0; i < DIM; i++) v[i] = (float) Math.sin(hash * (i + 1) * 0.01);
        return normalize(v);
    }

    public float[] normalize(float[] v) {
        double n = 0;
        for (float f : v) n += f * f;
        n = Math.sqrt(n);
        if (n == 0) return v;
        for (int i = 0; i < v.length; i++) v[i] /= n;
        return v;
    }
}
