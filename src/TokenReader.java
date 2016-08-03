import java.io.IOException;
import java.io.StringReader;

/**
 * Created by chenleejr on 16/8/2.
 */
public class TokenReader extends StringReader {
    private static final int ERROR = -1;
    public StringBuilder mReaded = new StringBuilder();

    public TokenReader(String s) {
        super(s);
    }

    public char readChar() throws IOException {
        int re = super.read();
        if (re == -1) {
            return '$';
        }
        char result = (char) re;
        mReaded.append(result);
        return result;
    }

    public void reset() throws IOException {
        super.reset();
        mReaded.deleteCharAt(mReaded.length() - 1);
    }

    public StringBuilder getmReaded() {
        return mReaded;
    }

    public int readNextToken() throws IOException {
        switch (peek()) {
            case '$':
                return JsonReader.END_DOCUMENT;
            case '}':
                readChar();
                return JsonReader.END_OBJECT;
            case ']':
                readChar();
                return JsonReader.END_ARRAY;
            case '{':
                readChar();
                return JsonReader.BEGIN_OBJECT;
            case ':':
                readChar();
                return JsonReader.SEP_COLON;
            case ',':
                readChar();
                return JsonReader.SEP_COMMA;
            case '[':
                readChar();
                return JsonReader.BEGIN_ARRAY;
            case 't':
            case 'f':
                return JsonReader.BOOLEAN;
            case 'n':
                return JsonReader.NULL;
            case '"':
                return JsonReader.STRING;
            case '0':
            case '1':
            case '2':
            case '3':
            case '4':
            case '5':
            case '6':
            case '7':
            case '8':
            case '9':
                return JsonReader.NUMBER;
        }
        return ERROR;
    }

    public boolean readBoolean() throws Exception {
        String booleanValue = peekString();
        boolean value = false;
        try {
            value = Boolean.valueOf(booleanValue);
        } catch (Exception e) {
            throw new Exception("Unexpected boolean." + mReaded);
        }
        return value;
    }

    public Object readNull() throws Exception {
        String nullValue = peekString();
        if (nullValue.equals("null")) {
            return null;
        } else {
            throw new Exception("Unexpected null." + mReaded);
        }
    }

    public Number readNumber() throws Exception {
        String numberValue = peekString();
        Number value = 0;
        try {
            if (numberValue.contains(".")) {
                value = Double.valueOf(numberValue);
            } else {
                value = Integer.valueOf(numberValue);
            }
        } catch (Exception e) {
            throw new Exception("Unexpected Number." + mReaded);
        }
        return value;
    }

    public String readString() throws Exception {
        String strValue = peekString();
        String value = "";
        try {
            value = String.valueOf(strValue.substring(1, strValue.length() - 1));
        } catch (Exception e) {
            throw new Exception("Unexpected String." + mReaded);
        }
        return value;
    }

    public char peek() throws IOException {
        mark(1);
        char result = readChar();
        reset();
        return result;
    }
    public String peek(int n) throws IOException {
        mark(n);
        StringBuilder sb = new StringBuilder();
        while (n-- > 0){
            sb.append(readChar());
        }
        reset();
        return sb.toString();
    }

    public String peekString() throws IOException {
        StringBuilder sb = new StringBuilder();
        while (true) {
            mark(1);
            char result = readChar();
            if (result == ',' || result == ':' || result == '}' || result == ']') {
                reset();
                return sb.toString();
            } else {
                sb.append(result);
            }
        }
    }
}
