import java.lang.reflect.Field;
import java.util.List;

/**
 * Created by chenleejr on 16/8/2.
 */
public class Test {
    public int intNumber;
    public double doubleNumber;
    public String str;
    public boolean bool;
    public Object nullObj;
    public List<List<String>> list;
    public List<InnerTest> objList;
    public InnerTest innerTest;
    public List<List<InnerTest>> crazyTest;
    public CrazyInner crazyInner;

    // 将所有的内部类定义在这里

    public static void main(String[] args) {

        for (Field field : Test.class.getDeclaredFields()) {
            System.out.println(field.getGenericType());
        }
    }


}
class InnerTest {
    public int a;

    public String toString() {
        return "{a:" + a + "}";
    }
}
class CrazyInner{
    public InnerTest inner;
    public int b;public String toString() {
        return "{inner:" + inner.toString() + ",b:" + b + "}";
    }
}

