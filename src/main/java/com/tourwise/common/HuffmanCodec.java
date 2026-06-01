package com.tourwise.common;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.PriorityQueue;

public final class HuffmanCodec {
    public static final String ENCODING = "huffman-v1";
    private static final int MAGIC = 0x48554631;

    private HuffmanCodec() {
    }

    public static CompressedText compress(String text) {
        byte[] input = text == null ? new byte[0] : text.getBytes(StandardCharsets.UTF_8);
        int[] frequencies = frequencies(input);
        Node root = buildTree(frequencies);
        String[] codes = new String[256];
        buildCodes(root, "", codes);

        ByteArrayOutputStream bitBytes = new ByteArrayOutputStream();
        int currentByte = 0;
        int bitCount = 0;
        int totalBits = 0;
        for (byte value : input) {
            String code = codes[value & 0xFF];
            for (int i = 0; i < code.length(); i++) {
                currentByte = (currentByte << 1) | (code.charAt(i) == '1' ? 1 : 0);
                bitCount++;
                totalBits++;
                if (bitCount == 8) {
                    bitBytes.write(currentByte);
                    currentByte = 0;
                    bitCount = 0;
                }
            }
        }
        if (bitCount > 0) {
            bitBytes.write(currentByte << (8 - bitCount));
        }

        byte[] payload = writePayload(input.length, totalBits, frequencies, bitBytes.toByteArray());
        return new CompressedText(payload, input.length, payload.length);
    }

    public static String decompress(byte[] payload) {
        if (payload == null || payload.length == 0) {
            return "";
        }
        try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(payload))) {
            int magic = input.readInt();
            if (magic != MAGIC) {
                throw new IllegalArgumentException("不支持的 Huffman 数据格式");
            }
            int originalSize = input.readInt();
            int bitLength = input.readInt();
            int[] frequencies = new int[256];
            int frequencyCount = input.readInt();
            for (int i = 0; i < frequencyCount; i++) {
                int value = input.readUnsignedByte();
                frequencies[value] = input.readInt();
            }
            int dataLength = input.readInt();
            byte[] compressedBytes = input.readNBytes(dataLength);
            byte[] output = decodeBytes(frequencies, compressedBytes, originalSize, bitLength);
            return new String(output, StandardCharsets.UTF_8);
        } catch (IOException | IllegalArgumentException ex) {
            throw new IllegalStateException("Huffman 解压失败", ex);
        }
    }

    private static int[] frequencies(byte[] input) {
        int[] frequencies = new int[256];
        for (byte value : input) {
            frequencies[value & 0xFF]++;
        }
        return frequencies;
    }

    private static Node buildTree(int[] frequencies) {
        PriorityQueue<Node> queue = new PriorityQueue<>(Comparator.comparingInt(Node::frequency));
        for (int i = 0; i < frequencies.length; i++) {
            if (frequencies[i] > 0) {
                queue.add(new Node(i, frequencies[i], null, null));
            }
        }
        if (queue.isEmpty()) {
            return new Node(0, 1, null, null);
        }
        while (queue.size() > 1) {
            Node left = queue.poll();
            Node right = queue.poll();
            queue.add(new Node(-1, left.frequency + right.frequency, left, right));
        }
        return queue.poll();
    }

    private static void buildCodes(Node node, String prefix, String[] codes) {
        if (node.isLeaf()) {
            codes[node.value] = prefix.isEmpty() ? "0" : prefix;
            return;
        }
        buildCodes(node.left, prefix + "0", codes);
        buildCodes(node.right, prefix + "1", codes);
    }

    private static byte[] writePayload(int originalSize, int bitLength, int[] frequencies, byte[] data) {
        try {
            ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
            DataOutputStream output = new DataOutputStream(byteStream);
            output.writeInt(MAGIC);
            output.writeInt(originalSize);
            output.writeInt(bitLength);
            int frequencyCount = 0;
            for (int frequency : frequencies) {
                if (frequency > 0) {
                    frequencyCount++;
                }
            }
            output.writeInt(frequencyCount);
            for (int i = 0; i < frequencies.length; i++) {
                if (frequencies[i] > 0) {
                    output.writeByte(i);
                    output.writeInt(frequencies[i]);
                }
            }
            output.writeInt(data.length);
            output.write(data);
            output.flush();
            return byteStream.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("Huffman 压缩失败", ex);
        }
    }

    private static byte[] decodeBytes(int[] frequencies, byte[] compressedBytes, int originalSize, int bitLength) {
        Node root = buildTree(frequencies);
        ByteArrayOutputStream output = new ByteArrayOutputStream(originalSize);
        if (root.isLeaf()) {
            for (int i = 0; i < originalSize; i++) {
                output.write(root.value);
            }
            return output.toByteArray();
        }
        Node current = root;
        int consumedBits = 0;
        for (byte compressedByte : compressedBytes) {
            for (int bitIndex = 7; bitIndex >= 0 && consumedBits < bitLength; bitIndex--) {
                int bit = (compressedByte >> bitIndex) & 1;
                current = bit == 0 ? current.left : current.right;
                if (current.isLeaf()) {
                    output.write(current.value);
                    current = root;
                    if (output.size() == originalSize) {
                        return output.toByteArray();
                    }
                }
                consumedBits++;
            }
        }
        return output.toByteArray();
    }

    private static final class Node {
        private final int value;
        private final int frequency;
        private final Node left;
        private final Node right;

        private Node(int value, int frequency, Node left, Node right) {
            this.value = value;
            this.frequency = frequency;
            this.left = left;
            this.right = right;
        }

        private int frequency() {
            return frequency;
        }

        boolean isLeaf() {
            return left == null && right == null;
        }
    }

    @Data
    @AllArgsConstructor
    public static class CompressedText {
        private byte[] payload;
        private int originalSize;
        private int compressedSize;
    }
}
