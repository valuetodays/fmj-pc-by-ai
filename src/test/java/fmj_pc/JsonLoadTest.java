package fmj_pc;

import hz.cdj.game.fmj.goods.BaseGoods;
import hz.cdj.game.fmj.lib.DatLib;
import hz.cdj.game.fmj.lib.JsonResourceProvider;

import java.io.File;

/**
 * 验证 JSON 加载器能否正确读取道具数据。
 * 不需要 DAT.LIB — 仅从 classpath 的 JSON 文件加载。
 */
public class JsonLoadTest {

    public static void main(String[] args) {
        // DatLib 需要提前初始化，因为 BaseGoods.setData() 内部会调用
        // DatLib.GetRes(RES_GDP, ...) 加载道具图片。
        if (new File("./assets/DAT.LIB").exists()) {
            DatLib.init();
        } else {
            System.out.println("WARN: DAT.LIB not found. Image resources will not load.");
        }

        // JSON 提供者（结构化数据从 JSON 加载）
        JsonResourceProvider provider = JsonResourceProvider.getInstance();

        // 尝试读取几个已知的道具
        testGoods(provider, 1, 1);   // 头巾 (防御+1)
        testGoods(provider, 2, 1);   // 布衣 
        testGoods(provider, 9, 1);   // 药品
        testGoods(provider, 14, 1);  // 剧情物品

        // 测试跨越 DatLib fallback — JSON 优先，DAT.LIB 补缺
        System.out.println("\n--- 测试 DatLib + JSON 混合加载 ---");
        testMixed(1, 1);   // 道具1号，应来自 JSON
        testMixed(6, 1);   // 装饰品
        testMixed(7, 1);   // 武器

        System.out.println("\n=== 测试完成 ===");
    }

    static void testGoods(JsonResourceProvider p, int type, int index) {
        BaseGoods goods = (BaseGoods) p.getRes(DatLib.RES_GRS, type, index);
        if (goods != null) {
            System.out.printf("[JSON] type=%2d index=%2d  name=%-8s  price=%3d  desc=%s%n",
                    goods.getType(), goods.getIndex(),
                    goods.getName(), goods.getBuyPrice(),
                    goods.getDescription().length() > 30
                            ? goods.getDescription().substring(0, 28) + "..."
                            : goods.getDescription());
        } else {
            System.out.printf("[JSON] type=%2d index=%2d  NOT FOUND%n", type, index);
        }
    }

    static void testMixed(int type, int index) {
        BaseGoods goods = (BaseGoods) DatLib.GetRes(DatLib.RES_GRS, type, index);
        if (goods != null) {
            System.out.printf("[MIX] type=%2d index=%2d  name=%-8s (from=%s)%n",
                    goods.getType(), goods.getIndex(),
                    goods.getName(), goods.getClass().getSimpleName());
        } else {
            System.out.printf("[MIX] type=%2d index=%2d  NOT FOUND%n", type, index);
        }
    }
}
