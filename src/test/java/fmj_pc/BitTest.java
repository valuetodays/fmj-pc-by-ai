package fmj_pc;

import org.junit.Assert;
import org.junit.Test;

public class BitTest {
    
    
    /** 
     * int整数转换为4字节的byte数组 
     *  
     * @param i 
     *            整数 
     * @return byte数组 
     * @see @{link #byteToInt()}
     */ 
    public static byte[] intToByte(int i) {  
        byte[] targets = new byte[4];  
        targets[3] = (byte) (i & 0xFF);  
        targets[2] = (byte) (i >> 8 & 0xFF);  
        targets[1] = (byte) (i >> 16 & 0xFF);  
        targets[0] = (byte) (i >> 24 & 0xFF);  
        return targets;  
    }  
    /**
     * short
     * @param i
     * @return
     */
    public static byte[] shortToByte(short i) {  
        byte[] targets = new byte[2];  
        targets[1] = (byte) (i & 0xFF);  
        targets[0] = (byte) ((i >> 8) & 0xFF);
        return targets;  
    }  
    
    /** 
     * byte数组转换为int整数 
     *  
     * @param bytes 
     *            byte数组 
     * @param off 
     *            开始位置 
     * @return int整数 
     * @see {@link #byteToInt(byte[], int)}
     */  
    public static int byteToInt(byte[] bytes, int off) {  
        int b0 = bytes[off] & 0xFF;  
        int b1 = bytes[off + 1] & 0xFF;  
        int b2 = bytes[off + 2] & 0xFF;  
        int b3 = bytes[off + 3] & 0xFF;  
        return (b0 << 24) | (b1 << 16) | (b2 << 8) | b3;  
    } 
    /**
     * 无符号
     * @param bytes
     * @param off
     * @return
     */
    public static short byteToShort(byte[] bytes, int off) {  
        int b0 = bytes[off] & 0xFF;  
        int b1 = bytes[off + 1] & 0xFF;  
        return  (short)((b0 << 8) | b1);  
    } 
    
    
   
    /**
     * int,有无符号均可 
     */
    @Test
    public void testIntToByte() {
        int n = -15;
        byte[] intToByte = intToByte(n);
        for (byte b : intToByte) {
            System.out.println(b);
        }
        int byte4ToInt = byteToInt(intToByte, 0);
        System.out.println(byte4ToInt);
    }
    
    /**
     * short, 有无符号均可
     */
    @Test
    public void testShortToByte() {
        short n = 15;
        byte[] intToByte2 = shortToByte(n);
        
        
        short byte2ToInt = byteToShort(intToByte2, 0);
//        System.out.println(byte2ToInt);
        Assert.assertEquals(n, byte2ToInt);
//        String binaryString = Integer.toBinaryString(byte2ToInt);
//        System.out.println(binaryString);
    }

    
    @Test
    public void testShow() {
        int n = -15;
        String binaryString = Integer.toBinaryString(n);
        System.out.println(binaryString);
    }
    

}
