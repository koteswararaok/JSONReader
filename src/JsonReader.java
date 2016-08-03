import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class JsonReader {
    // 由于冒号后{[代替了冒号,所以对象和数组需要判断自己是作为值还是数组的元素
    // 一般类型可以通过值判断自己的类型,复杂类型通过key的类型也要通过List的泛型判断,区别在于List的泛型可以作用于多个对象
    private int listLevel = 0;
    public static final int BOOLEAN = 0;
    public static final int NULL = 1;
    public static final int NUMBER = 2;
    public static final int STRING = 3;
    public static final int SEP_COLON = 4;
    public static final int SEP_COMMA = 5;
    public static final int BEGIN_ARRAY = 6;
    public static final int BEGIN_OBJECT = 7;
    public static final int END_ARRAY = 8;
    public static final int END_OBJECT = 9;
    public static final int END_DOCUMENT = 10;

    private static final int STATUS_EXPECT_SINGLE_VALUE = 1;
    private static final int STATUS_EXPECT_COLON = 2;
    private static final int STATUS_EXPECT_COMMA = 4;
    private static final int STATUS_EXPECT_BEGIN_OBJECT = 8;
    private static final int STATUS_EXPECT_OBJECT_KEY = 16;
    private static final int STATUS_EXPECT_OBJECT_VALUE = 32;
    private static final int STATUS_EXPECT_END_OBJECT = 64;
    private static final int STATUS_EXPECT_BEGIN_ARRAY = 128;
    private static final int STATUS_EXPECT_ARRAY_VALUE = 256;
    private static final int STATUS_EXPECT_END_ARRAY = 512;
    private static final int STATUS_EXPECT_END_DOCUMENT = 1024;

    private int mStatus;

    private TokenReader mReader;

    private boolean hasStatus(int status) {
        return (mStatus & status) != 0;
    }

    private String perpareJson(String jsonString) {
        StringBuilder sb = new StringBuilder();
        for (char chr : jsonString.toCharArray()) {
            if (!(chr == ' ' || chr == '\n' || chr == '\r' || chr == '\t')) {
                sb.append(chr);
            }
        }
        return sb.toString();
    }

    private Class findClass(String key, Class clazz) {
        for (Field field : clazz.getDeclaredFields()) {
            if (field.getName().equals(key)) {
                return field.getType();
            }
        }
        return null;
    }

    private Class findInClass(String key, Class clazz) throws ClassNotFoundException {
        for (Field field : clazz.getDeclaredFields()) {
            if (field.getName().equals(key)) {
                String name = field.getGenericType().getTypeName();
                return Class.forName(name.substring(name.lastIndexOf("<") + 1, name.indexOf(">")));
            }
        }
        return null;
    }
    private int findLevel(String key, Class clazz) throws ClassNotFoundException {
        for (Field field : clazz.getDeclaredFields()) {
            if (field.getName().equals(key)) {
                String name = field.getGenericType().getTypeName();
                int count = 0;
                while (name.contains("List")){
                    name = name.replaceFirst("List", "Fuck");
                    count++;
                }
                return count;
            }
        }
        return 0;
    }

    public Object parse(String jsonString, Class clazz) throws Exception {
        mReader = new TokenReader(perpareJson(jsonString));
        Stack<StackValue> stack = new Stack();
        Stack<Class> clazzs = new Stack<>();
        clazzs.push(clazz);
        mStatus = STATUS_EXPECT_SINGLE_VALUE | STATUS_EXPECT_BEGIN_OBJECT | STATUS_EXPECT_BEGIN_ARRAY;
        while (true) {
            int currentToken = mReader.readNextToken();
            switch (currentToken) {
                case BOOLEAN:
                    // single boolean:
                    if (hasStatus(STATUS_EXPECT_SINGLE_VALUE)) {
                        boolean bool = mReader.readBoolean();
                        stack.push(new StackValue(bool));
                        mStatus = STATUS_EXPECT_END_DOCUMENT;
                        continue;
                    }
                    if (hasStatus(STATUS_EXPECT_OBJECT_VALUE)) {
                        boolean bool = mReader.readBoolean();
                        String key = stack.pop().getStringValue();
                        stack.peek().setField(key, bool, clazzs.peek());
                        mStatus = STATUS_EXPECT_COMMA | STATUS_EXPECT_END_OBJECT;
                        continue;
                    }
                    if (hasStatus(STATUS_EXPECT_ARRAY_VALUE)) {
                        boolean bool = mReader.readBoolean();
                        stack.peek().add(bool);
                        mStatus = STATUS_EXPECT_COMMA | STATUS_EXPECT_END_ARRAY;
                        continue;
                    }
                    throw new Exception("Unexpected boolean." + mReader.getmReaded());

                case NULL:
                    if (hasStatus(STATUS_EXPECT_SINGLE_VALUE)) {
                        // single null:
                        mReader.readNull();
                        stack.push(new StackValue(null));
                        mStatus = STATUS_EXPECT_END_DOCUMENT;
                        continue;
                    }
                    if (hasStatus(STATUS_EXPECT_OBJECT_VALUE)) {
                        mReader.readNull();
                        String key = stack.pop().getStringValue();
                        stack.peek().setField(key, null, clazzs.peek());
                        mStatus = STATUS_EXPECT_COMMA | STATUS_EXPECT_END_OBJECT;
                        continue;
                    }
                    if (hasStatus(STATUS_EXPECT_ARRAY_VALUE)) {
                        mReader.readNull();
                        stack.peek().add(null);
                        mStatus = STATUS_EXPECT_COMMA | STATUS_EXPECT_END_ARRAY;
                        continue;
                    }
                    throw new Exception("Unexpected null." + mReader.getmReaded());

                case NUMBER:
                    if (hasStatus(STATUS_EXPECT_SINGLE_VALUE)) {
                        // single number:
                        Number number = mReader.readNumber();
                        stack.push(new StackValue(number));
                        mStatus = STATUS_EXPECT_END_DOCUMENT;
                        continue;
                    }
                    if (hasStatus(STATUS_EXPECT_OBJECT_VALUE)) {
                        Number number = mReader.readNumber();
                        String key = stack.pop().getStringValue();
                        stack.peek().setField(key, number, clazzs.peek());
                        mStatus = STATUS_EXPECT_COMMA | STATUS_EXPECT_END_OBJECT;
                        continue;
                    }
                    if (hasStatus(STATUS_EXPECT_ARRAY_VALUE)) {
                        Number number = mReader.readNumber();
                        stack.peek().add(number);
                        mStatus = STATUS_EXPECT_COMMA | STATUS_EXPECT_END_ARRAY;
                        continue;
                    }
                    throw new Exception("Unexpected number." + mReader.getmReaded());

                case STRING:
                    if (hasStatus(STATUS_EXPECT_SINGLE_VALUE)) {
                        // single string:
                        String str = mReader.readString();
                        stack.push(new StackValue(str));
                        mStatus = STATUS_EXPECT_END_DOCUMENT;
                        continue;
                    }
                    if (hasStatus(STATUS_EXPECT_OBJECT_KEY)) {
                        String str = mReader.readString();
                        stack.push(new StackValue(str));
                        stack.peek().setTypeKey();
                        Class strClass = findClass(str, clazzs.peek());
                        if (strClass != int.class && strClass != double.class && strClass != String.class
                                && strClass != boolean.class && strClass != Object.class) {
                            if (strClass != List.class) {
                                clazzs.push(strClass);
                            } else {
                                Class strInClass = findInClass(str, clazzs.peek());
                                if (strInClass != int.class && strInClass != double.class && strInClass != String.class
                                        && strInClass != boolean.class && strInClass != Object.class && strInClass != List.class) {
                                    listLevel = findLevel(str, clazzs.peek());
                                    clazzs.push(strInClass);
                                }
                            }
                        }

                        mStatus = STATUS_EXPECT_COLON;
                        continue;
                    }
                    if (hasStatus(STATUS_EXPECT_OBJECT_VALUE)) {
                        String str = mReader.readString();
                        String key = stack.pop().getStringValue();
                        stack.peek().setField(key, str, clazzs.peek());
                        mStatus = STATUS_EXPECT_COMMA | STATUS_EXPECT_END_OBJECT;
                        continue;
                    }
                    if (hasStatus(STATUS_EXPECT_ARRAY_VALUE)) {
                        String str = mReader.readString();
                        stack.peek().add(str);
                        mStatus = STATUS_EXPECT_COMMA | STATUS_EXPECT_END_ARRAY;
                        continue;
                    }
                    throw new Exception("Unexpected char \'\"\'." + mReader.getmReaded());

                case SEP_COLON: // :
                    if (mStatus == STATUS_EXPECT_COLON) {
                        mStatus = STATUS_EXPECT_OBJECT_VALUE | STATUS_EXPECT_BEGIN_OBJECT | STATUS_EXPECT_BEGIN_ARRAY;
                        continue;
                    }
                    throw new Exception("Unexpected char \':\'." + mReader.getmReaded());

                case SEP_COMMA: // ,
                    if (hasStatus(STATUS_EXPECT_COMMA)) {
                        if (hasStatus(STATUS_EXPECT_END_OBJECT)) {
                            mStatus = STATUS_EXPECT_OBJECT_KEY;
                            continue;
                        }
                        if (hasStatus(STATUS_EXPECT_END_ARRAY)) {
                            mStatus = STATUS_EXPECT_ARRAY_VALUE | STATUS_EXPECT_BEGIN_ARRAY | STATUS_EXPECT_BEGIN_OBJECT;
                            continue;
                        }
                    }
                    throw new Exception("Unexpected char \',\'." + mReader.getmReaded());

                case BEGIN_ARRAY:
                    if (hasStatus(STATUS_EXPECT_BEGIN_ARRAY)) {
                        stack.push(new StackValue(new ArrayList()));
                        mStatus = STATUS_EXPECT_ARRAY_VALUE | STATUS_EXPECT_BEGIN_OBJECT | STATUS_EXPECT_BEGIN_ARRAY | STATUS_EXPECT_END_ARRAY;
                        continue;
                    }
                    throw new Exception("Unexpected char: \'[\'." + mReader.getmReaded());

                case BEGIN_OBJECT:
                    if (hasStatus(STATUS_EXPECT_BEGIN_OBJECT)) {
                        stack.push(new StackValue(clazzs.peek().newInstance()));
                        mStatus = STATUS_EXPECT_OBJECT_KEY | STATUS_EXPECT_BEGIN_OBJECT | STATUS_EXPECT_END_OBJECT;
                        continue;
                    }
                    throw new Exception("Unexpected char: \'{\'." + mReader.getmReaded());
                case END_ARRAY:
                    if (listLevel != 0 && mReader.peek(listLevel - 1).replaceAll("]", "").equals("")){
                        clazzs.pop();
                        listLevel = 0;
                    }
                    if (hasStatus(STATUS_EXPECT_END_ARRAY)) {
                        StackValue array = stack.pop();
                        if (stack.isEmpty()) {
                            stack.push(array);
                            mStatus = STATUS_EXPECT_END_DOCUMENT;
                            continue;
                        }
                        if (stack.peek().isKey()) {
                            // key: [ CURRENT ] ,}
                            String key = stack.pop().getStringValue();
                            stack.peek().setField(key, array.getValue(), clazzs.peek());
                            mStatus = STATUS_EXPECT_COMMA | STATUS_EXPECT_END_OBJECT;
                            continue;
                        } else {
                            // xx, xx, [CURRENT] ,]
                            stack.peek().add(array.getValue());
                            mStatus = STATUS_EXPECT_COMMA | STATUS_EXPECT_END_ARRAY;
                            continue;
                        }
                    }
                    throw new Exception("Unexpected char: \']\'." + mReader.getmReaded());

                case END_OBJECT:
                    if (listLevel == 0) {
                        clazzs.pop();
                    }
                    if (hasStatus(STATUS_EXPECT_END_OBJECT)) {
                        StackValue object = stack.pop();
                        if (stack.isEmpty()) {
                            // root object:
                            stack.push(object);
                            mStatus = STATUS_EXPECT_END_DOCUMENT;
                            continue;
                        }
                        if (stack.peek().isKey()) {
                            String key = stack.pop().getStringValue();
                            stack.peek().setField(key, object.getValue(), clazzs.peek());
                            mStatus = STATUS_EXPECT_COMMA | STATUS_EXPECT_END_OBJECT;
                            continue;
                        } else {
                            stack.peek().add(object.getValue());
                            mStatus = STATUS_EXPECT_COMMA | STATUS_EXPECT_END_ARRAY;
                            continue;
                        }
                    }
                    throw new Exception("Unexpected char: \'}\'." + mReader.getmReaded());

                case END_DOCUMENT:
                    if (hasStatus(STATUS_EXPECT_END_DOCUMENT)) {
                        StackValue v = stack.pop();
                        if (stack.isEmpty()) {
                            return v.getValue();
                        }
                    }
                    throw new Exception("Unexpected EOF." + mReader.getmReaded());
            }
        }
    }

    public static void main(String[] args) throws Exception {
        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream("a.txt")));
        StringBuilder jsonString = new StringBuilder();
        String str = "";
        while ((str = br.readLine()) != null) {
            jsonString.append(str);
        }
        Test test = (Test) new JsonReader().parse(jsonString.toString(), Test.class);
        System.out.println(test.bool);
        System.out.println(test.doubleNumber);
        System.out.println(test.intNumber);
        System.out.println(test.str);
        System.out.println(test.nullObj);
        System.out.println(test.innerTest);
        for (List<String> o : test.list) {
            for (String ss:o){
                System.out.print(ss + " ");
            }
            System.out.println();
        }
        for (InnerTest t:test.objList){
            System.out.print(t + " ");
        }
        System.out.println();
        for (List<InnerTest> o:test.crazyTest){
            for (InnerTest t:o){
                System.out.print(t + " ");
            }
            System.out.println();
        }
        System.out.println(test.crazyInner);
    }
}