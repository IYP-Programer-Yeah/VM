package vm;

/**
 * Created by HoseinGhahremanzadeh on 7/6/2017.
 */
public class Instruction {
    public String name;
    public Operand[] operands = new Operand[3];
    InstructionFunction function;
    @Override
    public boolean equals(Object obj) {
        Instruction input = (Instruction) obj;
        if (name.toUpperCase().equals(input.name) &&
                (operands[0] != null && input.operands[0] != null && operands[0].equals(input.operands[0]) || (operands[0] == null && input.operands[0] == null)) &&
                (operands[1] != null && input.operands[1] != null && operands[1].equals(input.operands[1]) || (operands[1] == null && input.operands[1] == null)) &&
                (operands[2] != null && input.operands[2] != null && operands[2].equals(input.operands[2]) || (operands[2] == null && input.operands[2] == null)))
            return true;
        return false;
    }
}
