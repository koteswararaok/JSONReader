import java.lang.reflect.Field;
import java.util.ArrayList;

/**
 * Created by chenleejr on 16/8/2.
 */
public class StackValue {
    private static final int TYPE_KEY = 1;
    private int mType;
    private Object mValue;

    public StackValue(Object value) {
        mValue = value;
    }

    public Object getValue() {
        return mValue;
    }

    public String getStringValue() throws Exception {
        if (mValue instanceof String) {
            return (String) mValue;
        } else {
            throw new Exception("String format error");
        }
    }

    public Boolean getBooleanValue() throws Exception {
        if (mValue instanceof Boolean) {
            return (Boolean) mValue;
        } else {
            throw new Exception("boolean format error");
        }
    }

    public Number getNumberValue() throws Exception {
        if (mValue instanceof Number) {
            return (Number) mValue;
        } else {
            throw new Exception("Number format error");
        }
    }
    public void setField(String name, Object value, Class clazz) throws IllegalAccessException {
        Field[] fields = clazz.getDeclaredFields();
        for (Field field:fields){
            if (field.getName().equals(name)){
                field.set(mValue, value);
                break;
            }
        }
    }
    public void add(Object obj){
        ((ArrayList)mValue).add(obj);
    }
    public void setTypeKey(){
        mType = TYPE_KEY;
    }
    public boolean isKey(){
        return mType-- == TYPE_KEY;
    }
}
