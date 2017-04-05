package net.jards.remote.ddp;

import net.jards.core.IdGenerator;

import java.util.UUID;

/**
 * Created by jDzama on 24.1.2017.
 */
public class DDPIdGenerator implements IdGenerator{

    private final String UNMISTAKABLE_CHARS = "23456789ABCDEFGHJKLMNPQRSTWXYZabcdefghijkmnopqrstuvwxyz";
    private final String BASE64_CHARS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ" +
            "0123456789-_";

    private final String[] seed;
    private double s0;
    private double s1;
    private double s2;
    private double c;

    public DDPIdGenerator(String... seed){
        this.seed = seed;
        //alea
        if (seed!=null)
            alea();
    }

    @Override
    public String getId() {
        int charCount = 17;
        if (seed == null){
            return UUID.randomUUID().toString();
        }
        return generateRandomString(charCount);
    }

    private String generateRandomString(int charCount) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < charCount; i++) {
            result.append(choice());
        }
        return result.toString();
    }

    private char choice(){
        double d = fraction();
        int index = (int)Math.floor(d*UNMISTAKABLE_CHARS.length());
        return UNMISTAKABLE_CHARS.charAt(index);
    }

    private double fraction(){
        return random();
    }

    private double n = 4022871197d;

    private void alea(){
        //mashes with n and makes it "random"

        //Mash - reset "n" in js code
        n = 4022871197d;

        //"returnedFunctionInJs" - this calls "mash",
        returnedFunctionInJs(seed);
    }

    private double mash(String data){
        for (int i = 0; i < data.length(); i++) {
            n = n + data.charAt(i);
            double h = 0.02519603282416938d * n;
            n = (int)h;
            h -= n;
            h *= n;
            n = (int)h;
            h -= n;
            n += h * 4294967296d; // 2^32
        }
        double intV = ((long)n);
        return  intV* 2.3283064365386963e-10; // 2^-32
    }

    private void returnedFunctionInJs(String[] args){
        s0 = 0;
        s1 = 0;
        s2 = 0;
        c = 1;

        s0 = mash(" ");
        s1 = mash(" ");
        s2 = mash(" ");
        for (int i = 0; i < args.length; i++) {
            s0 -= mash(args[i]);
            if (s0 < 0) {
                s0 += 1;
            }
            s1 -= mash(args[i]);
            if (s1 < 0) {
                s1 += 1;
            }
            s2 -= mash(args[i]);
            if (s2 < 0) {
                s2 += 1;
            }
        }
    }

    private double random(){
        //random function
        double t = 2091639 * s0 + c * 2.3283064365386963e-10; // 2^-32
        s0 = s1;
        s1 = s2;
        c = (int)t;
        s2 = t - c;

        return s2;
    }

    public static void main(String[] args) {
        DDPIdGenerator idGen = new DDPIdGenerator("sekfbsdkfbfbsdkjfbsdkjb");
        System.out.println(idGen.getId());
        System.out.println(idGen.getId());

        //System.out.println(""+(3707969490d*2.3283064365386963e-10));

        /*double h = 101360395.53329061d;
        double n = 4022871229d;
        n = (int) h;
        System.out.println(n);
        h -= n;
        System.out.println("h2 : "+h);
        h *= n;
        System.out.println("h3 : "+h);
        n = (int) h;
        System.out.println("n2 : "+n);*/
    }

}
