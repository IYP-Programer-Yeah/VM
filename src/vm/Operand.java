package vm;

/**
 * Created by HoseinGhahremanzadeh on 7/6/2017.
 */
public class Operand {
    public enum OperandType {
        Empty, Register, String, Id, Number
    }
    public OperandType operandType = OperandType.Empty;
    public boolean isRelative;
    public boolean isAddress;
    public Object value;

    public boolean isTopStack;

    @Override
    public boolean equals(Object obj) {
        Operand input = (Operand)obj;
        if (isRelative == input.isRelative && isAddress == input.isAddress && input.operandType == operandType)
            return true;
        return false;
    }
}
