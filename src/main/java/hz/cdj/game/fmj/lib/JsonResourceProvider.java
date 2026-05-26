package hz.cdj.game.fmj.lib;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

/**
 * 从 classpath 下的 JSON 资源文件加载数据。
 * 
 * JSON 文件位置: src/main/resources/fmj-assets/RES_{typeName}/{fileName}.json
 * 
 * 目前支持:
 * - RES_GRS (6): goods.json
 * 
 * 本类将 JSON 数据转换回与 DAT.LIB 一致的二进制格式，然后复用
 * 现有的 ResBase.setData(byte[], int) 方法解析，无需修改现有模型类。
 */
public class JsonResourceProvider {

    private static final Logger LOG = Logger.getLogger(JsonResourceProvider.class);

    private static final String RESOURCE_BASE = "/fmj-assets/";

    /** resType → JSON 数组名 → List<JsonObject> */
    private Map<Integer, java.util.List<JsonObject>> mCache = new HashMap<>();

    private static JsonResourceProvider sInstance;

    public static synchronized JsonResourceProvider getInstance() {
        if (sInstance == null) {
            sInstance = new JsonResourceProvider();
        }
        return sInstance;
    }

    private JsonResourceProvider() {
    }

    /**
     * 从 JSON 加载资源对象。
     * @return ResBase 对象，如果 JSON 中不存在则返回 null
     */
    public ResBase getRes(int resType, int type, int index) {
        try {
            switch (resType) {
                case DatLib.RES_GRS: return getGoods(type, index);
                // 后续可扩展其他资源类型
                default: return null;
            }
        } catch (Exception e) {
            LOG.warn("JsonResourceProvider.getRes(" + resType + "," + type + "," + index + ") failed", e);
            return null;
        }
    }

    // ──── 道具 (RES_GRS) ────

    private ResBase getGoods(int type, int index) throws IOException {
        java.util.List<JsonObject> list = loadJsonArray(DatLib.RES_GRS, type, type + ".json", "goods");
        if (list == null) return null;

        for (JsonObject obj : list) {
            if (obj.getInt("index") == index) {
                return buildGoods(obj);
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private ResBase buildGoods(JsonObject obj) throws IOException {
        int type = obj.getInt("type");

        // 确定具体子类
        ResBase goods;
        switch (type) {
            case 1: case 2: case 3: case 4: case 5:
                goods = new hz.cdj.game.fmj.goods.GoodsEquipment();
                break;
            case 6:
                goods = new hz.cdj.game.fmj.goods.GoodsDecorations();
                break;
            case 7:
                goods = new hz.cdj.game.fmj.goods.GoodsWeapon();
                break;
            case 8:
                goods = new hz.cdj.game.fmj.goods.GoodsHiddenWeapon();
                break;
            case 9:
                goods = new hz.cdj.game.fmj.goods.GoodsMedicine();
                break;
            case 10:
                goods = new hz.cdj.game.fmj.goods.GoodsMedicineLife();
                break;
            case 11:
                goods = new hz.cdj.game.fmj.goods.GoodsMedicineChg4Ever();
                break;
            case 12:
                goods = new hz.cdj.game.fmj.goods.GoodsStimulant();
                break;
            case 13:
                goods = new hz.cdj.game.fmj.goods.GoodsTudun();
                break;
            case 14:
                goods = new hz.cdj.game.fmj.goods.GoodsDrama();
                break;
            default:
                return null;
        }

        // 将 JSON 对象编码为二进制字节数组，复用 setData 解析
        byte[] buf = encodeGoodsToBytes(obj);
        goods.setData(buf, 0);
        return goods;
    }

    /**
     * 将 JSON 道具对象编码为与 DAT.LIB 一致的二进制格式。
     * 布局规则来自 BaseGoods.setData() 及各子类 setOtherData()。
     */
    private byte[] encodeGoodsToBytes(JsonObject obj) throws IOException {
        int type = obj.getInt("type");
        ByteArrayOutputStream bos = new ByteArrayOutputStream(256);

        // ── BaseGoods 公共头部 (参考 BaseGoods.setData) ──
        writeByte(bos, type);                          // +0: mType
        writeByte(bos, obj.getInt("index"));           // +1: mIndex
        writeByte(bos, 0);                             // +2: 未使用
        writeByte(bos, obj.getInt("enable"));          // +3: mEnable
        writeByte(bos, obj.getInt("sumRound"));        // +4: mSumRound
        writeByte(bos, obj.getInt("imageIndex"));      // +5: 图片索引
        // +6: mName (GBK 字符串，0结尾)
        writeGbkString(bos, obj.getString("name"), 0x12 - 6);

        // 填充到 +0x12
        fill(bos, 0x12 - bos.size());

        // +0x12: mBuyPrice (2 bytes, little-endian)
        write2Bytes(bos, obj.getInt("buyPrice"));
        // +0x14: mSellPrice (2 bytes, little-endian)
        write2Bytes(bos, obj.getInt("sellPrice"));

        // 填充到 +0x1e
        fill(bos, 0x1e - bos.size());

        // +0x1e: mDescription (GBK 字符串)
        writeGbkString(bos, obj.getString("description"), 0x84 - 0x1e);

        // 填充到 +0x84
        fill(bos, 0x84 - bos.size());

        // +0x84: mEventId (2 bytes, little-endian)
        write2Bytes(bos, obj.getInt("eventId"));

        // 填充到子类数据区起始 +0x16 (relative to offset 0)
        fill(bos, 0x86 - bos.size());

        // ── 子类特有数据 ──
        JsonObject data = obj.getJsonObject("data");
        if (data != null) {
            switch (type) {
                case 1: case 2: case 3: case 4: case 5:
                case 7:
                    write1Signed(bos, data.getInt("mpMax"));      // +0x16
                    write1Signed(bos, data.getInt("hpMax"));      // +0x17
                    write1Signed(bos, data.getInt("def"));        // +0x18
                    writeByte(bos, data.getInt("atk"));           // +0x19
                    write1Signed(bos, data.getInt("lingli"));     // +0x1a
                    write1Signed(bos, data.getInt("speed"));      // +0x1b
                    writeByte(bos, data.getInt("bitEffect"));     // +0x1c
                    write1Signed(bos, data.getInt("luck"));       // +0x1d
                    break;
                case 6:
                    write1Signed(bos, data.getInt("mp"));         // +0x16
                    write1Signed(bos, data.getInt("hp"));         // +0x17
                    write1Signed(bos, data.getInt("def"));        // +0x18
                    writeByte(bos, data.getInt("atk"));           // +0x19
                    write1Signed(bos, data.getInt("lingli"));     // +0x1a
                    write1Signed(bos, data.getInt("speed"));      // +0x1b
                    writeByte(bos, data.getInt("magic"));         // +0x1c
                    write1Signed(bos, data.getInt("luck"));       // +0x1d
                    break;
                case 8:
                    write2Signed(bos, data.getInt("hp"));         // +0x16
                    write2Signed(bos, data.getInt("mp"));         // +0x18
                    writeByte(bos, data.getInt("aniIndex"));      // +0x1a
                    writeByte(bos, data.getInt("aniType"));       // +0x1b
                    writeByte(bos, data.getInt("bitMask"));       // +0x1c
                    break;
                case 9:
                    write2Bytes(bos, data.getInt("hp"));          // +0x16
                    write2Bytes(bos, data.getInt("mp"));          // +0x18
                    writeByte(bos, data.getInt("aniIndex"));      // +0x1a
                    writeByte(bos, 0);                            // +0x1b
                    writeByte(bos, data.getInt("bitMask"));       // +0x1c
                    break;
                case 10:
                    writeByte(bos, 0);                            // +0x16
                    writeByte(bos, data.getInt("percent"));       // +0x17
                    break;
                case 11:
                    write1Signed(bos, data.getInt("mpMax"));      // +0x16
                    write1Signed(bos, data.getInt("hpMax"));      // +0x17
                    write1Signed(bos, data.getInt("def"));        // +0x18
                    write1Signed(bos, data.getInt("atk"));        // +0x19
                    write1Signed(bos, data.getInt("lingli"));     // +0x1a
                    write1Signed(bos, data.getInt("speed"));      // +0x1b
                    writeByte(bos, 0);                            // +0x1c
                    write1Signed(bos, data.getInt("luck"));       // +0x1d
                    break;
                case 12:
                    writeByte(bos, 0);                            // +0x16
                    writeByte(bos, 0);                            // +0x17
                    writeByte(bos, data.getInt("defPercent"));    // +0x18
                    writeByte(bos, data.getInt("atkPercent"));    // +0x19
                    writeByte(bos, 0);                            // +0x1a
                    writeByte(bos, data.getInt("speedPercent"));  // +0x1b
                    writeByte(bos, data.getBoolean("forAll") ? 0x10 : 0); // +0x1c
                    break;
                // case 13, 14: 无额外数据
            }
        }

        return bos.toByteArray();
    }

    // ──── JSON 加载 ────

    private java.util.List<JsonObject> loadJsonArray(int resType, int subType, String fileName, String arrayKey) {
        int cacheKey = (resType << 16) | (subType << 8);
        if (mCache.containsKey(cacheKey)) {
            return mCache.get(cacheKey);
        }

        String path = RESOURCE_BASE + resTypeDir(resType) + "/" + fileName;
        InputStream is = getClass().getResourceAsStream(path);
        if (is == null) {
            is = getClass().getClassLoader().getResourceAsStream("fmj-assets/" + resTypeDir(resType) + "/" + fileName);
        }
        if (is == null) {
            LOG.debug("JSON resource not found: " + path);
            mCache.put(cacheKey, null);
            return null;
        }

        try {
            String content = new String(readAllBytes(is), "UTF-8");
            JsonObject root = JsonObject.parse(content);
            java.util.List<JsonObject> list = root.getJsonArray(arrayKey);
            mCache.put(cacheKey, list);
            return list;
        } catch (Exception e) {
            LOG.warn("Failed to parse JSON: " + path, e);
            mCache.put(cacheKey, null);
            return null;
        }
    }

    private static String resTypeDir(int resType) {
        switch (resType) {
            case 6:  return "RES_GRS";
            default: return "RES_" + resType;
        }
    }

    // ──── 二进制编码工具 ────

    private static void writeByte(ByteArrayOutputStream bos, int v) {
        bos.write(v & 0xFF);
    }

    private static void write2Bytes(ByteArrayOutputStream bos, int v) {
        bos.write(v & 0xFF);
        bos.write((v >> 8) & 0xFF);
    }

    private static void write1Signed(ByteArrayOutputStream bos, int v) {
        if (v < 0) {
            bos.write((-v) | 0x80);
        } else {
            bos.write(v & 0x7F);
        }
    }

    private static void write2Signed(ByteArrayOutputStream bos, int v) {
        if (v < 0) {
            bos.write((-v) & 0xFF);
            bos.write(((-v) >> 8) | 0x80);
        } else {
            bos.write(v & 0xFF);
            bos.write((v >> 8) & 0x7F);
        }
    }

    private static void writeGbkString(ByteArrayOutputStream bos, String s, int maxLen) throws IOException {
        if (s == null) s = "";
        byte[] gbk = s.getBytes(Charset.forName("GBK"));
        int len = Math.min(gbk.length, maxLen - 1);
        bos.write(gbk, 0, len);
        bos.write(0); // null terminator
    }

    private static void fill(ByteArrayOutputStream bos, int count) {
        while (count > 0) {
            bos.write(0);
            count--;
        }
    }

    private static byte[] readAllBytes(InputStream is) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        int n;
        while ((n = is.read(buf)) != -1) {
            bos.write(buf, 0, n);
        }
        return bos.toByteArray();
    }

    // ──── 极简 JSON 解析器 (无第三方依赖) ────

    /**
     * 极简 JSON 对象。仅支持 JSON 解析本程序生成的 goods.json 所需的功能。
     */
    public static class JsonObject {
        private Map<String, Object> map = new HashMap<>();

        public int getInt(String key) {
            Object v = map.get(key);
            if (v instanceof Number) return ((Number) v).intValue();
            if (v instanceof String) return Integer.parseInt((String) v);
            return 0;
        }

        public String getString(String key) {
            Object v = map.get(key);
            if (v instanceof String) return (String) v;
            return v == null ? "" : v.toString();
        }

        public boolean getBoolean(String key) {
            Object v = map.get(key);
            return "true".equals(v) || Boolean.TRUE.equals(v);
        }

        public JsonObject getJsonObject(String key) {
            return (JsonObject) map.get(key);
        }

        @SuppressWarnings("unchecked")
        public java.util.List<JsonObject> getJsonArray(String key) {
            return (java.util.List<JsonObject>) map.get(key);
        }

        void put(String key, Object value) {
            map.put(key, value);
        }

        /** 解析 JSON 字符串为 JsonObject */
        public static JsonObject parse(String json) {
            return new JsonParser(json).parseObject();
        }

        private static class JsonParser {
            private final String s;
            private int pos;

            JsonParser(String raw) {
                // 去除 C 风格注释 (/* ... */ 和 // ...)
                this.s = stripComments(raw);
                this.pos = 0;
            }

            /** 去除 JSON 中的 // 和 / * 注释（识别字符串边界） */
            private static String stripComments(String s) {
                StringBuilder sb = new StringBuilder(s.length());
                int i = 0;
                while (i < s.length()) {
                    // 字符串字面量 — 原样保留
                    if (s.charAt(i) == '"') {
                        sb.append('"');
                        i++;
                        while (i < s.length()) {
                            char c = s.charAt(i);
                            sb.append(c);
                            i++;
                            if (c == '\\' && i < s.length()) {
                                sb.append(s.charAt(i));
                                i++;
                            } else if (c == '"') {
                                break;
                            }
                        }
                        continue;
                    }
                    // 单行注释 //
                    if (i + 1 < s.length() && s.charAt(i) == '/' && s.charAt(i + 1) == '/') {
                        i += 2;
                        while (i < s.length() && s.charAt(i) != '\n') i++;
                        continue;
                    }
                    // 多行注释 /* */
                    if (i + 1 < s.length() && s.charAt(i) == '/' && s.charAt(i + 1) == '*') {
                        i += 2;
                        while (i + 1 < s.length() && !(s.charAt(i) == '*' && s.charAt(i + 1) == '/')) i++;
                        i += 2;
                        continue;
                    }
                    sb.append(s.charAt(i));
                    i++;
                }
                return sb.toString();
            }

            void skipWs() {
                while (pos < s.length() && Character.isWhitespace(s.charAt(pos))) pos++;
            }

            char peek() { skipWs(); return pos < s.length() ? s.charAt(pos) : 0; }
            char next() { skipWs(); return s.charAt(pos++); }

            void expect(char c) {
                char ch = next();
                if (ch != c) throw new RuntimeException("Expected '" + c + "' but got '" + ch + "' at " + pos);
            }

            String readString() {
                expect('"');
                StringBuilder sb = new StringBuilder();
                while (pos < s.length()) {
                    char c = s.charAt(pos++);
                    if (c == '"') break;
                    if (c == '\\') {
                        char esc = s.charAt(pos++);
                        switch (esc) {
                            case '"': sb.append('"'); break;
                            case '\\': sb.append('\\'); break;
                            case '/': sb.append('/'); break;
                            case 'n': sb.append('\n'); break;
                            case 'r': sb.append('\r'); break;
                            case 't': sb.append('\t'); break;
                            case 'u': sb.append((char) Integer.parseInt(s.substring(pos, pos + 4), 16)); pos += 4; break;
                            default: sb.append(esc);
                        }
                    } else {
                        sb.append(c);
                    }
                }
                return sb.toString();
            }

            Number readNumber() {
                int start = pos;
                if (s.charAt(pos) == '-') pos++;
                while (pos < s.length() && Character.isDigit(s.charAt(pos))) pos++;
                boolean isFloat = false;
                if (pos < s.length() && s.charAt(pos) == '.') {
                    isFloat = true;
                    pos++;
                    while (pos < s.length() && Character.isDigit(s.charAt(pos))) pos++;
                }
                String num = s.substring(start, pos);
                if (isFloat) return Double.parseDouble(num);
                // 检查是否超出 int 范围
                long v = Long.parseLong(num);
                if (v >= Integer.MIN_VALUE && v <= Integer.MAX_VALUE) return (int) v;
                return v;
            }

            Object readValue() {
                char c = peek();
                if (c == '"') return readString();
                if (c == '-' || Character.isDigit(c)) return readNumber();
                if (c == '{') return parseObject();
                if (c == '[') return parseArray();
                if (c == 't' || c == 'f') {
                    String raw = s.substring(pos, pos + (c == 't' ? 4 : 5));
                    pos += raw.length();
                    return "true".equals(raw);
                }
                if (c == 'n') {
                    pos += 4; // null
                    return null;
                }
                throw new RuntimeException("Unexpected char '" + c + "' at " + pos);
            }

            JsonObject parseObject() {
                expect('{');
                JsonObject obj = new JsonObject();
                if (peek() == '}') { next(); return obj; }
                while (true) {
                    String key = readString();
                    expect(':');
                    Object value = readValue();
                    obj.put(key, value);
                    if (peek() == '}') { next(); return obj; }
                    expect(',');
                }
            }

            java.util.List<JsonObject> parseArray() {
                expect('[');
                java.util.List<JsonObject> list = new java.util.ArrayList<>();
                if (peek() == ']') { next(); return list; }
                while (true) {
                    list.add(parseObject());
                    if (peek() == ']') { next(); return list; }
                    expect(',');
                }
            }
        }
    }
}
