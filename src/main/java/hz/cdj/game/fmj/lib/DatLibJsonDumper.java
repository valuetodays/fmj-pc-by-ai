package hz.cdj.game.fmj.lib;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 从 DAT.LIB 中提取结构化数据并输出为 JSON 格式。
 * 目前仅支持道具（RES_GRS = 6）。
 */
public class DatLibJsonDumper {

    private static final String ASSETS_PATH = "./assets/DAT.LIB";

    private byte[] mBuffer;
    private Map<Integer, Integer> mDataOffset = new LinkedHashMap<>();

    /** (resType, type, index) 条目 */
    private static class IndexEntry {
        int resType, type, index;
        IndexEntry(int r, int t, int i) { resType = r; type = t; index = i; }
    }

    private List<IndexEntry> mIndexEntries = new ArrayList<>();

    private DatLibJsonDumper() throws IOException {
        File f = new File(ASSETS_PATH);
        if (!f.exists()) {
            System.err.println("DAT.LIB not found at " + f.getAbsolutePath());
            System.err.println("Make sure to run from the project root directory.");
            System.exit(1);
        }
        try (FileInputStream in = new FileInputStream(f)) {
            mBuffer = new byte[in.available()];
            in.read(mBuffer);
        }
        parseIndex();
    }

    private void parseIndex() {
        int i = 0x10, j = 0x2000;
        while (i + 2 < mBuffer.length && mBuffer[i] != -1) {
            int resType = mBuffer[i] & 0xFF;
            int type    = mBuffer[i + 1] & 0xFF;
            int index   = mBuffer[i + 2] & 0xFF;
            int key = (resType << 16) | (type << 8) | index;
            int block = mBuffer[j] & 0xFF;
            int low   = mBuffer[j + 1] & 0xFF;
            int high  = mBuffer[j + 2] & 0xFF;
            int value = block * 0x4000 | (high << 8 | low);
            mDataOffset.put(key, value);
            mIndexEntries.add(new IndexEntry(resType, type, index));
            i += 3; j += 3;
        }
    }

    private int getOffset(int resType, int type, int index) {
        int key = (resType << 16) | (type << 8) | index;
        Integer off = mDataOffset.get(key);
        return off == null ? -1 : off;
    }

    // ──── BaseGoods 通用字段 ────
    private static int readByte(byte[] buf, int off) { return buf[off] & 0xFF; }
    private static int read2Bytes(byte[] buf, int off) {
        return (buf[off] & 0xFF) | ((buf[off + 1] & 0xFF) << 8);
    }
    private static int read1Signed(byte[] buf, int off) {
        int v = buf[off] & 0x7F;
        if ((buf[off] & 0x80) != 0) return -v;
        return v;
    }
    private static int read2Signed(byte[] buf, int off) {
        int v = (buf[off] & 0xFF) | ((buf[off + 1] << 8) & 0x7F00);
        if ((buf[off + 1] & 0x80) != 0) return -v;
        return v;
    }
    private static String readGbk(byte[] buf, int off) {
        int end = off;
        while (end < buf.length && buf[end] != 0) end++;
        try { return new String(buf, off, end - off, "GBK"); }
        catch (Exception e) { return ""; }
    }

    /** 把单个道具 dump 为 JSON 字符串 */
    private String dumpGoods(int type, int index) throws IOException {
        int offset = getOffset(DatLib.RES_GRS, type, index);
        if (offset < 0) return null;

        StringBuilder sb = new StringBuilder();

        int mType     = readByte(mBuffer, offset);
        int mIndex    = readByte(mBuffer, offset + 1);
        int mEnable   = readByte(mBuffer, offset + 3);
        int mSumRound = readByte(mBuffer, offset + 4);
        int mImgIdx   = readByte(mBuffer, offset + 5);
        String mName  = readGbk(mBuffer, offset + 6);
        int mBuyPrice  = read2Bytes(mBuffer, offset + 0x12);
        int mSellPrice = read2Bytes(mBuffer, offset + 0x14);
        String mDesc  = readGbk(mBuffer, offset + 0x1e);
        int mEventId   = read2Bytes(mBuffer, offset + 0x84);

        sb.append("  {\n");
        sb.append("    \"type\": ").append(mType).append(",\n");
        sb.append("    \"index\": ").append(mIndex).append(",\n");
        sb.append("    \"enable\": ").append(mEnable).append(",\n");
        sb.append("    \"sumRound\": ").append(mSumRound).append(",\n");
        sb.append("    \"imageIndex\": ").append(mImgIdx).append(",\n");
        sb.append("    \"name\": \"").append(escape(mName)).append("\",\n");
        sb.append("    \"buyPrice\": ").append(mBuyPrice).append(",\n");
        sb.append("    \"sellPrice\": ").append(mSellPrice).append(",\n");
        sb.append("    \"description\": \"").append(escape(mDesc)).append("\",\n");
        sb.append("    \"eventId\": ").append(mEventId).append(",\n");

        // 各子类特有字段
        sb.append("    \"data\": {\n");
        switch (type) {
            case 1: case 2: case 3: case 4: case 5:
                dumpEquipment(sb, offset);
                break;
            case 6:
                dumpDecorations(sb, offset);
                break;
            case 7:
                dumpEquipment(sb, offset);
                break;
            case 8:
                dumpHiddenWeapon(sb, offset);
                break;
            case 9:
                dumpMedicine(sb, offset);
                break;
            case 10:
                dumpMedicineLife(sb, offset);
                break;
            case 11:
                dumpMedicineChg4Ever(sb, offset);
                break;
            case 12:
                dumpStimulant(sb, offset);
                break;
            case 13: case 14:
                sb.append("      \"_note\": \"no extra data\"\n");
                break;
        }
        sb.append("    }\n");
        sb.append("  }");

        return sb.toString();
    }

    private void dumpEquipment(StringBuilder sb, int offset) {
        sb.append("      \"mpMax\": ").append(read1Signed(mBuffer, offset + 0x16)).append(",\n");
        sb.append("      \"hpMax\": ").append(read1Signed(mBuffer, offset + 0x17)).append(",\n");
        sb.append("      \"def\": ").append(read1Signed(mBuffer, offset + 0x18)).append(",\n");
        sb.append("      \"atk\": ").append(readByte(mBuffer, offset + 0x19)).append(",\n");
        sb.append("      \"lingli\": ").append(read1Signed(mBuffer, offset + 0x1a)).append(",\n");
        sb.append("      \"speed\": ").append(read1Signed(mBuffer, offset + 0x1b)).append(",\n");
        sb.append("      \"bitEffect\": ").append(readByte(mBuffer, offset + 0x1c)).append(",\n");
        sb.append("      \"luck\": ").append(read1Signed(mBuffer, offset + 0x1d)).append("\n");
    }

    private void dumpDecorations(StringBuilder sb, int offset) {
        sb.append("      \"mp\": ").append(read1Signed(mBuffer, offset + 0x16)).append(",\n");
        sb.append("      \"hp\": ").append(read1Signed(mBuffer, offset + 0x17)).append(",\n");
        sb.append("      \"def\": ").append(read1Signed(mBuffer, offset + 0x18)).append(",\n");
        sb.append("      \"atk\": ").append(readByte(mBuffer, offset + 0x19)).append(",\n");
        sb.append("      \"lingli\": ").append(read1Signed(mBuffer, offset + 0x1a)).append(",\n");
        sb.append("      \"speed\": ").append(read1Signed(mBuffer, offset + 0x1b)).append(",\n");
        sb.append("      \"magic\": ").append(readByte(mBuffer, offset + 0x1c)).append(",\n");
        sb.append("      \"luck\": ").append(read1Signed(mBuffer, offset + 0x1d)).append("\n");
    }

    private void dumpHiddenWeapon(StringBuilder sb, int offset) {
        sb.append("      \"hp\": ").append(read2Signed(mBuffer, offset + 0x16)).append(",\n");
        sb.append("      \"mp\": ").append(read2Signed(mBuffer, offset + 0x18)).append(",\n");
        sb.append("      \"aniType\": ").append(readByte(mBuffer, offset + 0x1b)).append(",\n");
        sb.append("      \"aniIndex\": ").append(readByte(mBuffer, offset + 0x1a)).append(",\n");
        sb.append("      \"bitMask\": ").append(readByte(mBuffer, offset + 0x1c)).append("\n");
    }

    private void dumpMedicine(StringBuilder sb, int offset) {
        sb.append("      \"hp\": ").append(read2Bytes(mBuffer, offset + 0x16)).append(",\n");
        sb.append("      \"mp\": ").append(read2Bytes(mBuffer, offset + 0x18)).append(",\n");
        sb.append("      \"aniIndex\": ").append(readByte(mBuffer, offset + 0x1a)).append(",\n");
        sb.append("      \"bitMask\": ").append(readByte(mBuffer, offset + 0x1c)).append("\n");
    }

    private void dumpMedicineLife(StringBuilder sb, int offset) {
        sb.append("      \"percent\": ").append(readByte(mBuffer, offset + 0x17)).append("\n");
    }

    private void dumpMedicineChg4Ever(StringBuilder sb, int offset) {
        sb.append("      \"mpMax\": ").append(read1Signed(mBuffer, offset + 0x16)).append(",\n");
        sb.append("      \"hpMax\": ").append(read1Signed(mBuffer, offset + 0x17)).append(",\n");
        sb.append("      \"def\": ").append(read1Signed(mBuffer, offset + 0x18)).append(",\n");
        sb.append("      \"atk\": ").append(read1Signed(mBuffer, offset + 0x19)).append(",\n");
        sb.append("      \"lingli\": ").append(read1Signed(mBuffer, offset + 0x1a)).append(",\n");
        sb.append("      \"speed\": ").append(read1Signed(mBuffer, offset + 0x1b)).append(",\n");
        sb.append("      \"luck\": ").append(read1Signed(mBuffer, offset + 0x1d)).append("\n");
    }

    private void dumpStimulant(StringBuilder sb, int offset) {
        sb.append("      \"defPercent\": ").append(readByte(mBuffer, offset + 0x18)).append(",\n");
        sb.append("      \"atkPercent\": ").append(readByte(mBuffer, offset + 0x19)).append(",\n");
        sb.append("      \"speedPercent\": ").append(readByte(mBuffer, offset + 0x1b)).append(",\n");
        sb.append("      \"forAll\": ").append((readByte(mBuffer, offset + 0x1c) & 0x10) != 0).append("\n");
    }

    private static String escape(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    // ──── 入口 ────

    public static void main(String[] args) throws IOException {
        // 指定输出的 JSON 目录（默认 RES_GRS）
        File outDir = new File(args.length > 0 ? args[0]
                : "src/main/resources/fmj-assets/RES_GRS");
        outDir.mkdirs();

        DatLibJsonDumper dumper = new DatLibJsonDumper();

        // 按 type 分组
        java.util.Map<Integer, java.util.List<String>> goodsByType = new java.util.LinkedHashMap<Integer, java.util.List<String>>();
        for (IndexEntry entry : dumper.mIndexEntries) {
            if (entry.resType != DatLib.RES_GRS) continue;
            String json = dumper.dumpGoods(entry.type, entry.index);
            if (json != null) {
                java.util.List<String> list = goodsByType.get(entry.type);
                if (list == null) {
                    list = new java.util.ArrayList<String>();
                    goodsByType.put(entry.type, list);
                }
                list.add(json);
            }
        }

        // 每个 type 写一个文件: 1.json, 2.json, ...
        int total = 0;
        for (java.util.Map.Entry<Integer, java.util.List<String>> e : goodsByType.entrySet()) {
            int type = e.getKey();
            java.util.List<String> goodsList = e.getValue();
            total += goodsList.size();

            StringBuilder sb = new StringBuilder(4096);
            sb.append("{\n");
            sb.append("  \"type\": ").append(type).append(",\n");
            sb.append("  \"goods\": [\n");

            for (int i = 0; i < goodsList.size(); i++) {
                if (i > 0) sb.append(",\n");
                sb.append(goodsList.get(i));
            }

            sb.append("\n  ]\n");
            sb.append("}\n");

            File outFile = new File(outDir, type + ".json");
            try (java.io.FileOutputStream fos = new java.io.FileOutputStream(outFile)) {
                fos.write(sb.toString().getBytes(java.nio.charset.Charset.forName("UTF-8")));
            }
            System.out.println("Wrote " + outFile.getName() + " (" + goodsList.size() + " entries)");
        }

        System.out.println("Total goods entries: " + total);
    }
}
