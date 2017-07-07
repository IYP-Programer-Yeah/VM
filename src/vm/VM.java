package vm;

import scanner.Scanner;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Stack;

/**
 * Created by HoseinGhahremanzadeh on 7/6/2017.
 */


public class VM {
    private int memorySize;
    private HashMap<String, Integer> registers = new HashMap<>();
    private byte[] memory;
    private int stackPointer;
    private int pc;
    private Stack<Integer> framePointers = new Stack<>();
    private HashMap<String, Integer> labels = new HashMap<>();
    private HashMap<String, Integer> names = new HashMap<>();
    private ArrayList<Instruction> instructionSet = new ArrayList<>();
    private boolean terminated = false;
    private int terminationResult = -1;
    private Stack<Integer> returnAddresses = new Stack<>();

    private ArrayList<Instruction> instructions = new ArrayList<>();

    private boolean reverseStringBytes = false;

    public VM(int memorySize) {
        this.memorySize = memorySize;
        memory = new byte[memorySize];
        reset();
        Instruction instruction;
        /////////////////////////here we add our supported instructions///////////////////////////////
        /////SET
        //////////////Id, Number
        instruction = new Instruction();
        instruction.name = "SET";
        instruction.operands[0] = new Operand();
        instruction.operands[0].operandType = Operand.OperandType.Id;
        instruction.operands[1] = new Operand();
        instruction.operands[1].operandType = Operand.OperandType.Number;
        instruction.function = new InstructionFunction() {
            @Override
            public void doInstruction(Instruction instruction) {
                names.put((String)instruction.operands[0].value, (Integer) instruction.operands[1].value);
            }
        };
        instructionSet.add(instruction);
        /////LOD
        //////////////Register, Number, Number
        instruction = new Instruction();
        instruction.name = "LOD";
        instruction.operands[0] = new Operand();
        instruction.operands[0].operandType = Operand.OperandType.Register;
        instruction.operands[1] = new Operand();
        instruction.operands[1].operandType = Operand.OperandType.Number;
        instruction.operands[2] = new Operand();
        instruction.operands[2].operandType = Operand.OperandType.Number;

        instruction.function = new InstructionFunction() {
            @Override
            public void doInstruction(Instruction instruction) {
                int secondOperandValue = (Integer)instruction.operands[1].value;
                int thirdOperandValue = (Integer)instruction.operands[2].value;
                int mask = generateByteMask(thirdOperandValue);
                int registerValue = getValueOfRegister((String)instruction.operands[0].value);
                registers.put((String) instruction.operands[0].value, maskBlitNumbers(secondOperandValue, registerValue, mask));
            }
        };
        instructionSet.add(instruction);
        /////LOD
        //////////////Register, Number: Address, Number
        instruction = new Instruction();
        instruction.name = "LOD";
        instruction.operands[0] = new Operand();
        instruction.operands[0].operandType = Operand.OperandType.Register;
        instruction.operands[1] = new Operand();
        instruction.operands[1].operandType = Operand.OperandType.Number;
        instruction.operands[1].isAddress = true;
        instruction.operands[2] = new Operand();
        instruction.operands[2].operandType = Operand.OperandType.Number;
        instruction.function = new InstructionFunction() {
            @Override
            public void doInstruction(Instruction instruction) {
                int secondOperandValue = (Integer)instruction.operands[1].value;
                int thirdOperandValue = (Integer)instruction.operands[2].value;
                if (secondOperandValue < 0 || secondOperandValue >= memorySize) {
                    System.err.println("I don't know wtf you think your doing, but you're accessing out of your memory in LOD instruction.");
                    return;
                }
                loadToRegisterFromMemory(secondOperandValue, thirdOperandValue, (String) instruction.operands[0].value);
            }
        };
        instructionSet.add(instruction);
        /////LOD
        //////////////Register, Number: Address&Relative, Number
        instruction = new Instruction();
        instruction.name = "LOD";
        instruction.operands[0] = new Operand();
        instruction.operands[0].operandType = Operand.OperandType.Register;
        instruction.operands[1] = new Operand();
        instruction.operands[1].operandType = Operand.OperandType.Number;
        instruction.operands[1].isAddress = true;
        instruction.operands[1].isRelative = true;
        instruction.operands[2] = new Operand();
        instruction.operands[2].operandType = Operand.OperandType.Number;
        instruction.function = new InstructionFunction() {
            @Override
            public void doInstruction(Instruction instruction) {
                int secondOperandValue = (Integer)instruction.operands[1].value;
                int thirdOperandValue = (Integer)instruction.operands[2].value;
                int address = secondOperandValue + (instruction.operands[1].isTopStack ? stackPointer : framePointers.peek());
                if (address < 0 || address >= memorySize) {
                    System.err.println("I don't know wtf you think your doing, but you're accessing out of your memory in LOD instruction.");
                    return;
                }
                loadToRegisterFromMemory(address, thirdOperandValue, (String) instruction.operands[0].value);
            }
        };
        instructionSet.add(instruction);
        /////LOD
        //////////////Register, Id, Number
        instruction = new Instruction();
        instruction.name = "LOD";
        instruction.operands[0] = new Operand();
        instruction.operands[0].operandType = Operand.OperandType.Register;
        instruction.operands[1] = new Operand();
        instruction.operands[1].operandType = Operand.OperandType.Id;
        instruction.operands[2] = new Operand();
        instruction.operands[2].operandType = Operand.OperandType.Number;

        instruction.function = new InstructionFunction() {
            @Override
            public void doInstruction(Instruction instruction) {
                int secondOperandValue = getValueOfName((String)instruction.operands[1].value);
                int thirdOperandValue = (Integer)instruction.operands[2].value;
                int registerValue = getValueOfRegister((String)instruction.operands[0].value);
                registers.put((String) instruction.operands[0].value, maskBlitNumbers(secondOperandValue, registerValue, generateByteMask(thirdOperandValue)));
            }
        };
        instructionSet.add(instruction);
        /////LOD
        //////////////Register, Number: Address, Number
        instruction = new Instruction();
        instruction.name = "LOD";
        instruction.operands[0] = new Operand();
        instruction.operands[0].operandType = Operand.OperandType.Register;
        instruction.operands[1] = new Operand();
        instruction.operands[1].operandType = Operand.OperandType.Id;
        instruction.operands[1].isAddress = true;
        instruction.operands[2] = new Operand();
        instruction.operands[2].operandType = Operand.OperandType.Number;
        instruction.function = new InstructionFunction() {
            @Override
            public void doInstruction(Instruction instruction) {
                int secondOperandValue = getValueOfName((String)instruction.operands[1].value);
                int thirdOperandValue = (Integer)instruction.operands[2].value;
                if (secondOperandValue < 0 || secondOperandValue >= memorySize) {
                    System.err.println("I don't know wtf you think your doing, but you're accessing out of your memory in LOD instruction.");
                    return;
                }
                loadToRegisterFromMemory(secondOperandValue, thirdOperandValue, (String) instruction.operands[0].value);
            }
        };
        instructionSet.add(instruction);
        /////LOD
        //////////////Register, Number: Address&Relative, Number
        instruction = new Instruction();
        instruction.name = "LOD";
        instruction.operands[0] = new Operand();
        instruction.operands[0].operandType = Operand.OperandType.Register;
        instruction.operands[1] = new Operand();
        instruction.operands[1].operandType = Operand.OperandType.Id;
        instruction.operands[1].isAddress = true;
        instruction.operands[1].isRelative = true;
        instruction.operands[2] = new Operand();
        instruction.operands[2].operandType = Operand.OperandType.Number;
        instruction.function = new InstructionFunction() {
            @Override
            public void doInstruction(Instruction instruction) {
                int secondOperandValue = getValueOfName((String)instruction.operands[1].value);
                int thirdOperandValue = (Integer)instruction.operands[2].value;
                int address = secondOperandValue + (instruction.operands[1].isTopStack ? stackPointer : framePointers.peek());
                if (address < 0 || address >= memorySize) {
                    System.err.println("I don't know wtf you think your doing, but you're accessing out of your memory in LOD instruction.");
                    return;
                }
                loadToRegisterFromMemory(address, thirdOperandValue, (String) instruction.operands[0].value);
            }
        };
        instructionSet.add(instruction);
        /////LOD
        //////////////Register, Register: Address, Number
        instruction = new Instruction();
        instruction.name = "LOD";
        instruction.operands[0] = new Operand();
        instruction.operands[0].operandType = Operand.OperandType.Register;
        instruction.operands[1] = new Operand();
        instruction.operands[1].operandType = Operand.OperandType.Register;
        instruction.operands[1].isAddress = true;
        instruction.operands[2] = new Operand();
        instruction.operands[2].operandType = Operand.OperandType.Number;
        instruction.function = new InstructionFunction() {
            @Override
            public void doInstruction(Instruction instruction) {
                int secondOperandValue = getValueOfRegister((String)instruction.operands[1].value);
                int thirdOperandValue = (Integer)instruction.operands[2].value;
                if (secondOperandValue < 0 || secondOperandValue >= memorySize) {
                    System.err.println("I don't know wtf you think your doing, but you're accessing out of your memory in LOD instruction.");
                    return;
                }
                loadToKBytesOfRegisterFromMemory(secondOperandValue, thirdOperandValue, (String) instruction.operands[0].value, thirdOperandValue > 2 ? 4 : 2);
            }
        };
        instructionSet.add(instruction);
        /////LOD
        //////////////Register, Register: Address&Relative, Number
        instruction = new Instruction();
        instruction.name = "LOD";
        instruction.operands[0] = new Operand();
        instruction.operands[0].operandType = Operand.OperandType.Register;
        instruction.operands[1] = new Operand();
        instruction.operands[1].operandType = Operand.OperandType.Register;
        instruction.operands[1].isAddress = true;
        instruction.operands[1].isRelative = true;
        instruction.operands[2] = new Operand();
        instruction.operands[2].operandType = Operand.OperandType.Number;
        instruction.function = new InstructionFunction() {
            @Override
            public void doInstruction(Instruction instruction) {
                int secondOperandValue = getValueOfRegister((String)instruction.operands[1].value);
                int thirdOperandValue = (Integer)instruction.operands[2].value;
                int address = secondOperandValue + (instruction.operands[1].isTopStack ? stackPointer : framePointers.peek());
                if (address < 0 || address >= memorySize) {
                    System.err.println("I don't know wtf you think your doing, but you're accessing out of your memory in LOD instruction.");
                    return;
                }
                loadToKBytesOfRegisterFromMemory(address, thirdOperandValue, (String) instruction.operands[0].value, thirdOperandValue > 2 ? 4 : 2);
            }
        };
        instructionSet.add(instruction);
        /////MOV
        //////////////Register, Register, Number
        instruction = new Instruction();
        instruction.name = "LOD";
        instruction.operands[0] = new Operand();
        instruction.operands[0].operandType = Operand.OperandType.Register;
        instruction.operands[1] = new Operand();
        instruction.operands[1].operandType = Operand.OperandType.Register;
        instruction.operands[2] = new Operand();
        instruction.operands[2].operandType = Operand.OperandType.Number;
        instruction.function = new InstructionFunction() {
            @Override
            public void doInstruction(Instruction instruction) {
                registers.put((String)instruction.operands[0].value, maskBlitNumbers(getValueOfName((String)instruction.operands[1].value) ,getValueOfName((String)instruction.operands[0].value), generateByteMask((Integer)instruction.operands[2].value)));
            }
        };
        instructionSet.add(instruction);
        /////EXT
        //////////////Register, Number, Number
        instruction = new Instruction();
        instruction.name = "EXT";
        instruction.operands[0] = new Operand();
        instruction.operands[0].operandType = Operand.OperandType.Register;
        instruction.operands[1] = new Operand();
        instruction.operands[1].operandType = Operand.OperandType.Number;
        instruction.operands[2] = new Operand();
        instruction.operands[2].operandType = Operand.OperandType.Number;
        instruction.function = new InstructionFunction() {
            @Override
            public void doInstruction(Instruction instruction) {
                int extensionResult = maskBlitNumbers(getValueOfRegister((String)instruction.operands[0].value), 0, generateByteMask(getValueOfRegister((String)instruction.operands[1].value)));
                names.put((String)instruction.operands[0].value, maskBlitNumbers(
                        extensionResult,
                        getValueOfRegister((String)instruction.operands[0].value),
                        generateByteMask(getValueOfRegister((String)instruction.operands[1].value))));
            }
        };
        instructionSet.add(instruction);
        /////EXS
        //////////////Register, Number, Number
        instruction = new Instruction();
        instruction.name = "EXS";
        instruction.operands[0] = new Operand();
        instruction.operands[0].operandType = Operand.OperandType.Register;
        instruction.operands[1] = new Operand();
        instruction.operands[1].operandType = Operand.OperandType.Number;
        instruction.operands[2] = new Operand();
        instruction.operands[2].operandType = Operand.OperandType.Number;
        instruction.function = new InstructionFunction() {
            @Override
            public void doInstruction(Instruction instruction) {
                int extensionResult = (getValueOfRegister((String)instruction.operands[0].value) << ((4 - ((Integer)instruction.operands[1].value)) * 8)) >> ((4 - ((Integer)instruction.operands[2].value)) * 8);
                names.put((String)instruction.operands[0].value, maskBlitNumbers(
                        extensionResult,
                        getValueOfRegister((String)instruction.operands[0].value),
                        generateByteMask(getValueOfRegister((String)instruction.operands[1].value))));
            }
        };
        instructionSet.add(instruction);
        /////STR
        //////////////Number: Address, Number, Number
        instruction = new Instruction();
        instruction.name = "STR";
        instruction.operands[0] = new Operand();
        instruction.operands[0].operandType = Operand.OperandType.Number;
        instruction.operands[0].isAddress = true;
        instruction.operands[1] = new Operand();
        instruction.operands[1].operandType = Operand.OperandType.Number;
        instruction.operands[2] = new Operand();
        instruction.operands[2].operandType = Operand.OperandType.Number;
        instruction.function = new InstructionFunction() {
            @Override
            public void doInstruction(Instruction instruction) {
                int firstOperandValue = (Integer)instruction.operands[0].value;
                int secondOperandValue = (Integer)instruction.operands[1].value;
                int thirdOperandValue = (Integer)instruction.operands[2].value;
                int address = firstOperandValue;
                byte[] wantedByte = new byte[thirdOperandValue];
                byte[] integerBytes = intToByteArray(secondOperandValue);
                if (address < 0 || address >= memorySize) {
                    System.err.println("I don't know wtf you think your doing, but you're accessing out of your memory in STR instruction.");
                    return;
                }
                for (int i=0;i<thirdOperandValue; i++)
                    wantedByte[i] = integerBytes[4 - thirdOperandValue + i];
                writeToMemory(wantedByte, address, thirdOperandValue);
            }
        };
        instructionSet.add(instruction);
        /////STR
        //////////////Number: Address, Id, Number
        instruction = new Instruction();
        instruction.name = "STR";
        instruction.operands[0] = new Operand();
        instruction.operands[0].operandType = Operand.OperandType.Number;
        instruction.operands[0].isAddress = true;
        instruction.operands[1] = new Operand();
        instruction.operands[1].operandType = Operand.OperandType.Id;
        instruction.operands[2] = new Operand();
        instruction.operands[2].operandType = Operand.OperandType.Number;
        instruction.function = new InstructionFunction() {
            @Override
            public void doInstruction(Instruction instruction) {
                int firstOperandValue = (Integer)instruction.operands[0].value;
                int secondOperandValue = getValueOfName((String)instruction.operands[1].value);
                int thirdOperandValue = (Integer)instruction.operands[2].value;
                int address = firstOperandValue;
                byte[] wantedByte = new byte[thirdOperandValue];
                byte[] integerBytes = intToByteArray(secondOperandValue);
                if (address < 0 || address >= memorySize) {
                    System.err.println("I don't know wtf you think your doing, but you're accessing out of your memory in STR instruction.");
                    return;
                }
                for (int i=0;i<thirdOperandValue; i++)
                    wantedByte[i] = integerBytes[4 - thirdOperandValue + i];
                writeToMemory(wantedByte, address, thirdOperandValue);
            }
        };
        instructionSet.add(instruction);
        /////STR
        //////////////Number: Address, Register, Number
        instruction = new Instruction();
        instruction.name = "STR";
        instruction.operands[0] = new Operand();
        instruction.operands[0].operandType = Operand.OperandType.Number;
        instruction.operands[0].isAddress = true;
        instruction.operands[1] = new Operand();
        instruction.operands[1].operandType = Operand.OperandType.Register;
        instruction.operands[2] = new Operand();
        instruction.operands[2].operandType = Operand.OperandType.Number;
        instruction.function = new InstructionFunction() {
            @Override
            public void doInstruction(Instruction instruction) {
                int firstOperandValue = (Integer)instruction.operands[0].value;
                int secondOperandValue = getValueOfRegister((String)instruction.operands[1].value);
                int thirdOperandValue = (Integer)instruction.operands[2].value;
                int address = firstOperandValue;
                byte[] wantedByte = new byte[thirdOperandValue];
                byte[] integerBytes = intToByteArray(secondOperandValue);
                if (address < 0 || address >= memorySize) {
                    System.err.println("I don't know wtf you think your doing, but you're accessing out of your memory in STR instruction.");
                    return;
                }
                for (int i=0;i<thirdOperandValue; i++)
                    wantedByte[i] = integerBytes[4 - thirdOperandValue + i];
                writeToMemory(wantedByte, address, thirdOperandValue);
            }
        };
        instructionSet.add(instruction);
        /////STR
        //////////////Number: Address, String
        instruction = new Instruction();
        instruction.name = "STR";
        instruction.operands[0] = new Operand();
        instruction.operands[0].operandType = Operand.OperandType.Number;
        instruction.operands[0].isAddress = true;
        instruction.operands[1] = new Operand();
        instruction.operands[1].operandType = Operand.OperandType.String;
        instruction.function = new InstructionFunction() {
            @Override
            public void doInstruction(Instruction instruction) {
                int firstOperandValue = (Integer)instruction.operands[0].value;
                int address = firstOperandValue;
                String secondOperandValue = (String)instruction.operands[1].value;
                byte[] wantedBytes = new byte[secondOperandValue.length() + 1];
                if (address < 0 || address >= memorySize) {
                    System.err.println("I don't know wtf you think your doing, but you're accessing out of your memory in STR instruction.");
                    return;
                }
                for (int i=0;i<wantedBytes.length - 1; i++)
                    wantedBytes[i] = (byte)secondOperandValue.charAt(i);
                wantedBytes[wantedBytes.length - 1] = 0;
                if (reverseStringBytes)
                    wantedBytes = reverseBytes(wantedBytes);
                writeToMemory(wantedBytes, address, wantedBytes.length);
            }
        };
        instructionSet.add(instruction);
        /////STR
        //////////////Id: Address, Number, Number
        instruction = new Instruction();
        instruction.name = "STR";
        instruction.operands[0] = new Operand();
        instruction.operands[0].operandType = Operand.OperandType.Id;
        instruction.operands[0].isAddress = true;
        instruction.operands[1] = new Operand();
        instruction.operands[1].operandType = Operand.OperandType.Number;
        instruction.operands[2] = new Operand();
        instruction.operands[2].operandType = Operand.OperandType.Number;
        instruction.function = new InstructionFunction() {
            @Override
            public void doInstruction(Instruction instruction) {
                int firstOperandValue = getValueOfName((String)instruction.operands[0].value);
                int secondOperandValue = (Integer)instruction.operands[1].value;
                int thirdOperandValue = (Integer)instruction.operands[2].value;
                int address = firstOperandValue;
                byte[] wantedByte = new byte[thirdOperandValue];
                byte[] integerBytes = intToByteArray(secondOperandValue);
                if (address < 0 || address >= memorySize) {
                    System.err.println("I don't know wtf you think your doing, but you're accessing out of your memory in STR instruction.");
                    return;
                }
                for (int i=0;i<thirdOperandValue; i++)
                    wantedByte[i] = integerBytes[4 - thirdOperandValue + i];
                writeToMemory(wantedByte, address, thirdOperandValue);
            }
        };
        instructionSet.add(instruction);
        /////STR
        //////////////Id: Address, Id, Number
        instruction = new Instruction();
        instruction.name = "STR";
        instruction.operands[0] = new Operand();
        instruction.operands[0].operandType = Operand.OperandType.Id;
        instruction.operands[0].isAddress = true;
        instruction.operands[1] = new Operand();
        instruction.operands[1].operandType = Operand.OperandType.Id;
        instruction.operands[2] = new Operand();
        instruction.operands[2].operandType = Operand.OperandType.Number;
        instruction.function = new InstructionFunction() {
            @Override
            public void doInstruction(Instruction instruction) {
                int firstOperandValue = getValueOfName((String)instruction.operands[0].value);
                int secondOperandValue = getValueOfName((String)instruction.operands[1].value);
                int thirdOperandValue = (Integer)instruction.operands[2].value;
                int address = firstOperandValue;
                byte[] wantedByte = new byte[thirdOperandValue];
                byte[] integerBytes = intToByteArray(secondOperandValue);
                if (address < 0 || address >= memorySize) {
                    System.err.println("I don't know wtf you think your doing, but you're accessing out of your memory in STR instruction.");
                    return;
                }
                for (int i=0;i<thirdOperandValue; i++)
                    wantedByte[i] = integerBytes[4 - thirdOperandValue + i];
                writeToMemory(wantedByte, address, thirdOperandValue);
            }
        };
        instructionSet.add(instruction);
        /////STR
        //////////////Id: Address, Register, Number
        instruction = new Instruction();
        instruction.name = "STR";
        instruction.operands[0] = new Operand();
        instruction.operands[0].operandType = Operand.OperandType.Id;
        instruction.operands[0].isAddress = true;
        instruction.operands[1] = new Operand();
        instruction.operands[1].operandType = Operand.OperandType.Register;
        instruction.operands[2] = new Operand();
        instruction.operands[2].operandType = Operand.OperandType.Number;
        instruction.function = new InstructionFunction() {
            @Override
            public void doInstruction(Instruction instruction) {
                int firstOperandValue = getValueOfName((String)instruction.operands[0].value);
                int secondOperandValue = getValueOfRegister((String)instruction.operands[1].value);
                int thirdOperandValue = (Integer)instruction.operands[2].value;
                int address = firstOperandValue;
                byte[] wantedByte = new byte[thirdOperandValue];
                byte[] integerBytes = intToByteArray(secondOperandValue);
                if (address < 0 || address >= memorySize) {
                    System.err.println("I don't know wtf you think your doing, but you're accessing out of your memory in STR instruction.");
                    return;
                }
                for (int i=0;i<thirdOperandValue; i++)
                    wantedByte[i] = integerBytes[4 - thirdOperandValue + i];
                writeToMemory(wantedByte, address, thirdOperandValue);
            }
        };
        instructionSet.add(instruction);
        /////STR
        //////////////Id: Address, String
        instruction = new Instruction();
        instruction.name = "STR";
        instruction.operands[0] = new Operand();
        instruction.operands[0].operandType = Operand.OperandType.Id;
        instruction.operands[0].isAddress = true;
        instruction.operands[1] = new Operand();
        instruction.operands[1].operandType = Operand.OperandType.String;
        instruction.function = new InstructionFunction() {
            @Override
            public void doInstruction(Instruction instruction) {
                int firstOperandValue = getValueOfName((String)instruction.operands[0].value);
                int address = firstOperandValue;
                String secondOperandValue = (String)instruction.operands[1].value;
                byte[] wantedBytes = new byte[secondOperandValue.length() + 1];
                if (address < 0 || address >= memorySize) {
                    System.err.println("I don't know wtf you think your doing, but you're accessing out of your memory in STR instruction.");
                    return;
                }
                for (int i=0;i<wantedBytes.length - 1; i++)
                    wantedBytes[i] = (byte)secondOperandValue.charAt(i);
                wantedBytes[wantedBytes.length - 1] = 0;
                if (reverseStringBytes)
                    wantedBytes = reverseBytes(wantedBytes);
                writeToMemory(wantedBytes, address, wantedBytes.length);
            }
        };
        instructionSet.add(instruction);
        /////STR
        //////////////Register: Address, Number, Number
        instruction = new Instruction();
        instruction.name = "STR";
        instruction.operands[0] = new Operand();
        instruction.operands[0].operandType = Operand.OperandType.Register;
        instruction.operands[0].isAddress = true;
        instruction.operands[1] = new Operand();
        instruction.operands[1].operandType = Operand.OperandType.Number;
        instruction.operands[2] = new Operand();
        instruction.operands[2].operandType = Operand.OperandType.Number;
        instruction.function = new InstructionFunction() {
            @Override
            public void doInstruction(Instruction instruction) {
                int firstOperandValue = getValueOfRegister((String)instruction.operands[0].value);
                int secondOperandValue = (Integer)instruction.operands[1].value;
                int thirdOperandValue = (Integer)instruction.operands[2].value;
                int address = firstOperandValue;
                byte[] wantedByte = new byte[thirdOperandValue];
                byte[] integerBytes = intToByteArray(secondOperandValue);
                if (address < 0 || address >= memorySize) {
                    System.err.println("I don't know wtf you think your doing, but you're accessing out of your memory in STR instruction.");
                    return;
                }
                for (int i=0;i<thirdOperandValue; i++)
                    wantedByte[i] = integerBytes[4 - thirdOperandValue + i];
                writeToMemory(wantedByte, address, thirdOperandValue);
            }
        };
        instructionSet.add(instruction);
        /////STR
        //////////////Register: Address, Id, Number
        instruction = new Instruction();
        instruction.name = "STR";
        instruction.operands[0] = new Operand();
        instruction.operands[0].operandType = Operand.OperandType.Register;
        instruction.operands[0].isAddress = true;
        instruction.operands[1] = new Operand();
        instruction.operands[1].operandType = Operand.OperandType.Id;
        instruction.operands[2] = new Operand();
        instruction.operands[2].operandType = Operand.OperandType.Number;
        instruction.function = new InstructionFunction() {
            @Override
            public void doInstruction(Instruction instruction) {
                int firstOperandValue = getValueOfRegister((String)instruction.operands[0].value);
                int secondOperandValue = getValueOfName((String)instruction.operands[1].value);
                int thirdOperandValue = (Integer)instruction.operands[2].value;
                int address = firstOperandValue;
                byte[] wantedByte = new byte[thirdOperandValue];
                byte[] integerBytes = intToByteArray(secondOperandValue);
                if (address < 0 || address >= memorySize) {
                    System.err.println("I don't know wtf you think your doing, but you're accessing out of your memory in STR instruction.");
                    return;
                }
                for (int i=0;i<thirdOperandValue; i++)
                    wantedByte[i] = integerBytes[4 - thirdOperandValue + i];
                writeToMemory(wantedByte, address, thirdOperandValue);
            }
        };
        instructionSet.add(instruction);
        /////STR
        //////////////Register: Address, Register, Number
        instruction = new Instruction();
        instruction.name = "STR";
        instruction.operands[0] = new Operand();
        instruction.operands[0].operandType = Operand.OperandType.Register;
        instruction.operands[0].isAddress = true;
        instruction.operands[1] = new Operand();
        instruction.operands[1].operandType = Operand.OperandType.Register;
        instruction.operands[2] = new Operand();
        instruction.operands[2].operandType = Operand.OperandType.Number;
        instruction.function = new InstructionFunction() {
            @Override
            public void doInstruction(Instruction instruction) {
                int firstOperandValue = getValueOfRegister((String)instruction.operands[0].value);
                int secondOperandValue = getValueOfRegister((String)instruction.operands[1].value);
                int thirdOperandValue = (Integer)instruction.operands[2].value;
                int address = firstOperandValue;
                byte[] wantedByte = new byte[thirdOperandValue];
                byte[] integerBytes = intToByteArray(secondOperandValue);
                if (address < 0 || address >= memorySize) {
                    System.err.println("I don't know wtf you think your doing, but you're accessing out of your memory in STR instruction.");
                    return;
                }
                for (int i=0;i<thirdOperandValue; i++)
                    wantedByte[i] = integerBytes[4 - thirdOperandValue + i];
                writeToMemory(wantedByte, address, thirdOperandValue);
            }
        };
        instructionSet.add(instruction);
        /////STR
        //////////////Register: Address, String
        instruction = new Instruction();
        instruction.name = "STR";
        instruction.operands[0] = new Operand();
        instruction.operands[0].operandType = Operand.OperandType.Register;
        instruction.operands[0].isAddress = true;
        instruction.operands[1] = new Operand();
        instruction.operands[1].operandType = Operand.OperandType.String;
        instruction.function = new InstructionFunction() {
            @Override
            public void doInstruction(Instruction instruction) {
                int firstOperandValue = getValueOfRegister((String)instruction.operands[0].value);
                int address = firstOperandValue;
                String secondOperandValue = (String)instruction.operands[1].value;
                byte[] wantedBytes = new byte[secondOperandValue.length() + 1];
                if (address < 0 || address >= memorySize) {
                    System.err.println("I don't know wtf you think your doing, but you're accessing out of your memory in STR instruction.");
                    return;
                }
                for (int i=0;i<wantedBytes.length - 1; i++)
                    wantedBytes[i] = (byte)secondOperandValue.charAt(i);
                wantedBytes[wantedBytes.length - 1] = 0;
                if (reverseStringBytes)
                    wantedBytes = reverseBytes(wantedBytes);
                writeToMemory(wantedBytes, address, wantedBytes.length);
            }
        };
        instructionSet.add(instruction);
        /////STR
        //////////////Number: Address&Relative, Number, Number
        instruction = new Instruction();
        instruction.name = "STR";
        instruction.operands[0] = new Operand();
        instruction.operands[0].operandType = Operand.OperandType.Number;
        instruction.operands[0].isAddress = true;
        instruction.operands[0].isRelative = true;
        instruction.operands[1] = new Operand();
        instruction.operands[1].operandType = Operand.OperandType.Number;
        instruction.operands[2] = new Operand();
        instruction.operands[2].operandType = Operand.OperandType.Number;
        instruction.function = new InstructionFunction() {
            @Override
            public void doInstruction(Instruction instruction) {
                int firstOperandValue = (Integer)instruction.operands[0].value;
                int secondOperandValue = (Integer)instruction.operands[1].value;
                int thirdOperandValue = (Integer)instruction.operands[2].value;
                int address = firstOperandValue + (instruction.operands[0].isTopStack ? stackPointer : framePointers.peek());
                byte[] wantedByte = new byte[thirdOperandValue];
                byte[] integerBytes = intToByteArray(secondOperandValue);
                if (address < 0 || address >= memorySize) {
                    System.err.println("I don't know wtf you think your doing, but you're accessing out of your memory in STR instruction.");
                    return;
                }
                for (int i=0;i<thirdOperandValue; i++)
                    wantedByte[i] = integerBytes[4 - thirdOperandValue + i];
                writeToMemory(wantedByte, address, thirdOperandValue);
            }
        };
        instructionSet.add(instruction);
        /////STR
        //////////////Number: Address&Relative, Id, Number
        instruction = new Instruction();
        instruction.name = "STR";
        instruction.operands[0] = new Operand();
        instruction.operands[0].operandType = Operand.OperandType.Number;
        instruction.operands[0].isAddress = true;
        instruction.operands[0].isRelative = true;
        instruction.operands[1] = new Operand();
        instruction.operands[1].operandType = Operand.OperandType.Id;
        instruction.operands[2] = new Operand();
        instruction.operands[2].operandType = Operand.OperandType.Number;
        instruction.function = new InstructionFunction() {
            @Override
            public void doInstruction(Instruction instruction) {
                int firstOperandValue = (Integer)instruction.operands[0].value;
                int secondOperandValue = getValueOfName((String)instruction.operands[1].value);
                int thirdOperandValue = (Integer)instruction.operands[2].value;
                int address = firstOperandValue + (instruction.operands[0].isTopStack ? stackPointer : framePointers.peek());
                byte[] wantedByte = new byte[thirdOperandValue];
                byte[] integerBytes = intToByteArray(secondOperandValue);
                if (address < 0 || address >= memorySize) {
                    System.err.println("I don't know wtf you think your doing, but you're accessing out of your memory in STR instruction.");
                    return;
                }
                for (int i=0;i<thirdOperandValue; i++)
                    wantedByte[i] = integerBytes[4 - thirdOperandValue + i];
                writeToMemory(wantedByte, address, thirdOperandValue);
            }
        };
        instructionSet.add(instruction);
        /////STR
        //////////////Number: Address&Relative, Register, Number
        instruction = new Instruction();
        instruction.name = "STR";
        instruction.operands[0] = new Operand();
        instruction.operands[0].operandType = Operand.OperandType.Number;
        instruction.operands[0].isAddress = true;
        instruction.operands[0].isRelative = true;
        instruction.operands[1] = new Operand();
        instruction.operands[1].operandType = Operand.OperandType.Register;
        instruction.operands[2] = new Operand();
        instruction.operands[2].operandType = Operand.OperandType.Number;
        instruction.function = new InstructionFunction() {
            @Override
            public void doInstruction(Instruction instruction) {
                int firstOperandValue = (Integer)instruction.operands[0].value;
                int secondOperandValue = getValueOfRegister((String)instruction.operands[1].value);
                int thirdOperandValue = (Integer)instruction.operands[2].value;
                int address = firstOperandValue + (instruction.operands[0].isTopStack ? stackPointer : framePointers.peek());
                byte[] wantedByte = new byte[thirdOperandValue];
                byte[] integerBytes = intToByteArray(secondOperandValue);
                if (address < 0 || address >= memorySize) {
                    System.err.println("I don't know wtf you think your doing, but you're accessing out of your memory in STR instruction.");
                    return;
                }
                for (int i=0;i<thirdOperandValue; i++)
                    wantedByte[i] = integerBytes[4 - thirdOperandValue + i];
                writeToMemory(wantedByte, address, thirdOperandValue);
            }
        };
        instructionSet.add(instruction);
        /////STR
        //////////////Number: Address&Relative, String
        instruction = new Instruction();
        instruction.name = "STR";
        instruction.operands[0] = new Operand();
        instruction.operands[0].operandType = Operand.OperandType.Number;
        instruction.operands[0].isAddress = true;
        instruction.operands[0].isRelative = true;
        instruction.operands[1] = new Operand();
        instruction.operands[1].operandType = Operand.OperandType.String;
        instruction.function = new InstructionFunction() {
            @Override
            public void doInstruction(Instruction instruction) {
                int firstOperandValue = (Integer)instruction.operands[0].value;
                int address = firstOperandValue + (instruction.operands[0].isTopStack ? stackPointer : framePointers.peek());
                String secondOperandValue = (String)instruction.operands[1].value;
                byte[] wantedByte = new byte[secondOperandValue.length() + 1];
                if (address < 0 || address >= memorySize) {
                    System.err.println("I don't know wtf you think your doing, but you're accessing out of your memory in STR instruction.");
                    return;
                }
                for (int i=0;i<wantedByte.length - 1; i++)
                    wantedByte[i] = (byte)secondOperandValue.charAt(i);
                wantedByte[wantedByte.length - 1] = 0;
                writeToMemory(wantedByte, address, wantedByte.length);
            }
        };
        instructionSet.add(instruction);
        /////STR
        //////////////Id: Address&Relative, Number, Number
        instruction = new Instruction();
        instruction.name = "STR";
        instruction.operands[0] = new Operand();
        instruction.operands[0].operandType = Operand.OperandType.Id;
        instruction.operands[0].isAddress = true;
        instruction.operands[0].isRelative = true;
        instruction.operands[1] = new Operand();
        instruction.operands[1].operandType = Operand.OperandType.Number;
        instruction.operands[2] = new Operand();
        instruction.operands[2].operandType = Operand.OperandType.Number;
        instruction.function = new InstructionFunction() {
            @Override
            public void doInstruction(Instruction instruction) {
                int firstOperandValue = getValueOfName((String)instruction.operands[0].value);
                int secondOperandValue = (Integer)instruction.operands[1].value;
                int thirdOperandValue = (Integer)instruction.operands[2].value;
                int address = firstOperandValue + (instruction.operands[0].isTopStack ? stackPointer : framePointers.peek());
                byte[] wantedByte = new byte[thirdOperandValue];
                byte[] integerBytes = intToByteArray(secondOperandValue);
                if (address < 0 || address >= memorySize) {
                    System.err.println("I don't know wtf you think your doing, but you're accessing out of your memory in STR instruction.");
                    return;
                }
                for (int i=0;i<thirdOperandValue; i++)
                    wantedByte[i] = integerBytes[4 - thirdOperandValue + i];
                writeToMemory(wantedByte, address, thirdOperandValue);
            }
        };
        instructionSet.add(instruction);
        /////STR
        //////////////Id: Address&Relative, Id, Number
        instruction = new Instruction();
        instruction.name = "STR";
        instruction.operands[0] = new Operand();
        instruction.operands[0].operandType = Operand.OperandType.Id;
        instruction.operands[0].isAddress = true;
        instruction.operands[0].isRelative = true;
        instruction.operands[1] = new Operand();
        instruction.operands[1].operandType = Operand.OperandType.Id;
        instruction.operands[2] = new Operand();
        instruction.operands[2].operandType = Operand.OperandType.Number;
        instruction.function = new InstructionFunction() {
            @Override
            public void doInstruction(Instruction instruction) {
                int firstOperandValue = getValueOfName((String)instruction.operands[0].value);
                int secondOperandValue = getValueOfName((String)instruction.operands[1].value);
                int thirdOperandValue = (Integer)instruction.operands[2].value;
                int address = firstOperandValue + (instruction.operands[0].isTopStack ? stackPointer : framePointers.peek());
                byte[] wantedByte = new byte[thirdOperandValue];
                byte[] integerBytes = intToByteArray(secondOperandValue);
                if (address < 0 || address >= memorySize) {
                    System.err.println("I don't know wtf you think your doing, but you're accessing out of your memory in STR instruction.");
                    return;
                }
                for (int i=0;i<thirdOperandValue; i++)
                    wantedByte[i] = integerBytes[4 - thirdOperandValue + i];
                writeToMemory(wantedByte, address, thirdOperandValue);
            }
        };
        instructionSet.add(instruction);
        /////STR
        //////////////Id: Address&Relative, Register, Number
        instruction = new Instruction();
        instruction.name = "STR";
        instruction.operands[0] = new Operand();
        instruction.operands[0].operandType = Operand.OperandType.Id;
        instruction.operands[0].isAddress = true;
        instruction.operands[0].isRelative = true;
        instruction.operands[1] = new Operand();
        instruction.operands[1].operandType = Operand.OperandType.Register;
        instruction.operands[2] = new Operand();
        instruction.operands[2].operandType = Operand.OperandType.Number;
        instruction.function = new InstructionFunction() {
            @Override
            public void doInstruction(Instruction instruction) {
                int firstOperandValue = getValueOfName((String)instruction.operands[0].value);
                int secondOperandValue = getValueOfRegister((String)instruction.operands[1].value);
                int thirdOperandValue = (Integer)instruction.operands[2].value;
                int address = firstOperandValue + (instruction.operands[0].isTopStack ? stackPointer : framePointers.peek());
                byte[] wantedByte = new byte[thirdOperandValue];
                byte[] integerBytes = intToByteArray(secondOperandValue);
                if (address < 0 || address >= memorySize) {
                    System.err.println("I don't know wtf you think your doing, but you're accessing out of your memory in STR instruction.");
                    return;
                }
                for (int i=0;i<thirdOperandValue; i++)
                    wantedByte[i] = integerBytes[4 - thirdOperandValue + i];
                writeToMemory(wantedByte, address, thirdOperandValue);
            }
        };
        instructionSet.add(instruction);
        /////STR
        //////////////Id: Address&Relative, String
        instruction = new Instruction();
        instruction.name = "STR";
        instruction.operands[0] = new Operand();
        instruction.operands[0].operandType = Operand.OperandType.Id;
        instruction.operands[0].isAddress = true;
        instruction.operands[0].isRelative = true;
        instruction.operands[1] = new Operand();
        instruction.operands[1].operandType = Operand.OperandType.String;
        instruction.function = new InstructionFunction() {
            @Override
            public void doInstruction(Instruction instruction) {
                int firstOperandValue = getValueOfName((String)instruction.operands[0].value);
                int address = firstOperandValue + (instruction.operands[0].isTopStack ? stackPointer : framePointers.peek());
                String secondOperandValue = (String)instruction.operands[1].value;
                byte[] wantedByte = new byte[secondOperandValue.length() + 1];
                if (address < 0 || address >= memorySize) {
                    System.err.println("I don't know wtf you think your doing, but you're accessing out of your memory in STR instruction.");
                    return;
                }
                for (int i=0;i<wantedByte.length - 1; i++)
                    wantedByte[i] = (byte)secondOperandValue.charAt(i);
                wantedByte[wantedByte.length - 1] = 0;
                writeToMemory(wantedByte, address, wantedByte.length);
            }
        };
        instructionSet.add(instruction);
        /////STR
        //////////////Register: Address&Relative, Number, Number
        instruction = new Instruction();
        instruction.name = "STR";
        instruction.operands[0] = new Operand();
        instruction.operands[0].operandType = Operand.OperandType.Register;
        instruction.operands[0].isAddress = true;
        instruction.operands[0].isRelative = true;
        instruction.operands[1] = new Operand();
        instruction.operands[1].operandType = Operand.OperandType.Number;
        instruction.operands[2] = new Operand();
        instruction.operands[2].operandType = Operand.OperandType.Number;
        instruction.function = new InstructionFunction() {
            @Override
            public void doInstruction(Instruction instruction) {
                int firstOperandValue = getValueOfRegister((String)instruction.operands[0].value);
                int secondOperandValue = (Integer)instruction.operands[1].value;
                int thirdOperandValue = (Integer)instruction.operands[2].value;
                int address = firstOperandValue + (instruction.operands[0].isTopStack ? stackPointer : framePointers.peek());
                byte[] wantedByte = new byte[thirdOperandValue];
                byte[] integerBytes = intToByteArray(secondOperandValue);
                if (address < 0 || address >= memorySize) {
                    System.err.println("I don't know wtf you think your doing, but you're accessing out of your memory in STR instruction.");
                    return;
                }
                for (int i=0;i<thirdOperandValue; i++)
                    wantedByte[i] = integerBytes[4 - thirdOperandValue + i];
                writeToMemory(wantedByte, address, thirdOperandValue);
            }
        };
        instructionSet.add(instruction);
        /////STR
        //////////////Register: Address&Relative, Id, Number
        instruction = new Instruction();
        instruction.name = "STR";
        instruction.operands[0] = new Operand();
        instruction.operands[0].operandType = Operand.OperandType.Register;
        instruction.operands[0].isAddress = true;
        instruction.operands[0].isRelative = true;
        instruction.operands[1] = new Operand();
        instruction.operands[1].operandType = Operand.OperandType.Id;
        instruction.operands[2] = new Operand();
        instruction.operands[2].operandType = Operand.OperandType.Number;
        instruction.function = new InstructionFunction() {
            @Override
            public void doInstruction(Instruction instruction) {
                int firstOperandValue = getValueOfRegister((String)instruction.operands[0].value);
                int secondOperandValue = getValueOfName((String)instruction.operands[1].value);
                int thirdOperandValue = (Integer)instruction.operands[2].value;
                int address = firstOperandValue + (instruction.operands[0].isTopStack ? stackPointer : framePointers.peek());
                byte[] wantedByte = new byte[thirdOperandValue];
                byte[] integerBytes = intToByteArray(secondOperandValue);
                if (address < 0 || address >= memorySize) {
                    System.err.println("I don't know wtf you think your doing, but you're accessing out of your memory in STR instruction.");
                    return;
                }
                for (int i=0;i<thirdOperandValue; i++)
                    wantedByte[i] = integerBytes[4 - thirdOperandValue + i];
                writeToMemory(wantedByte, address, thirdOperandValue);
            }
        };
        instructionSet.add(instruction);
        /////STR
        //////////////Register: Address&Relative, Register, Number
        instruction = new Instruction();
        instruction.name = "STR";
        instruction.operands[0] = new Operand();
        instruction.operands[0].operandType = Operand.OperandType.Register;
        instruction.operands[0].isAddress = true;
        instruction.operands[0].isRelative = true;
        instruction.operands[1] = new Operand();
        instruction.operands[1].operandType = Operand.OperandType.Register;
        instruction.operands[2] = new Operand();
        instruction.operands[2].operandType = Operand.OperandType.Number;
        instruction.function = new InstructionFunction() {
            @Override
            public void doInstruction(Instruction instruction) {
                int firstOperandValue = getValueOfRegister((String)instruction.operands[0].value);
                int secondOperandValue = getValueOfRegister((String)instruction.operands[1].value);
                int thirdOperandValue = (Integer)instruction.operands[2].value;
                int address = firstOperandValue + (instruction.operands[0].isTopStack ? stackPointer : framePointers.peek());
                byte[] wantedByte = new byte[thirdOperandValue];
                byte[] integerBytes = intToByteArray(secondOperandValue);
                if (address < 0 || address >= memorySize) {
                    System.err.println("I don't know wtf you think your doing, but you're accessing out of your memory in STR instruction.");
                    return;
                }
                for (int i=0;i<thirdOperandValue; i++)
                    wantedByte[i] = integerBytes[4 - thirdOperandValue + i];
                writeToMemory(wantedByte, address, thirdOperandValue);
            }
        };
        instructionSet.add(instruction);
        /////STR
        //////////////Register: Address&Relative, String
        instruction = new Instruction();
        instruction.name = "STR";
        instruction.operands[0] = new Operand();
        instruction.operands[0].operandType = Operand.OperandType.Register;
        instruction.operands[0].isAddress = true;
        instruction.operands[0].isRelative = true;
        instruction.operands[1] = new Operand();
        instruction.operands[1].operandType = Operand.OperandType.String;
        instruction.function = new InstructionFunction() {
            @Override
            public void doInstruction(Instruction instruction) {
                int firstOperandValue = getValueOfRegister((String)instruction.operands[0].value);
                int address = firstOperandValue + (instruction.operands[0].isTopStack ? stackPointer : framePointers.peek());
                String secondOperandValue = (String)instruction.operands[1].value;
                byte[] wantedByte = new byte[secondOperandValue.length() + 1];
                if (address < 0 || address >= memorySize) {
                    System.err.println("I don't know wtf you think your doing, but you're accessing out of your memory in STR instruction.");
                    return;
                }
                for (int i=0;i<wantedByte.length - 1; i++)
                    wantedByte[i] = (byte)secondOperandValue.charAt(i);
                wantedByte[wantedByte.length - 1] = 0;
                writeToMemory(wantedByte, address, wantedByte.length);
            }
        };
        instructionSet.add(instruction);
        /////ADD
        //////////////Register, Register, Register
        instruction = new Instruction();
        instruction.name = "ADD";
        instruction.operands[0] = new Operand();
        instruction.operands[0].operandType = Operand.OperandType.Register;
        instruction.operands[1] = new Operand();
        instruction.operands[1].operandType = Operand.OperandType.Register;
        instruction.operands[2] = new Operand();
        instruction.operands[2].operandType = Operand.OperandType.Register;
        instruction.function = new InstructionFunction() {
            @Override
            public void doInstruction(Instruction instruction) {
                int secondOperandValue = getValueOfRegister((String)instruction.operands[1].value);
                int thirdOperandValue = getValueOfRegister((String)instruction.operands[2].value);
                names.put((String)instruction.operands[0].value, secondOperandValue + thirdOperandValue);
            }
        };
        instructionSet.add(instruction);
        /////SUB
        //////////////Register, Register, Register
        instruction = new Instruction();
        instruction.name = "SUB";
        instruction.operands[0] = new Operand();
        instruction.operands[0].operandType = Operand.OperandType.Register;
        instruction.operands[1] = new Operand();
        instruction.operands[1].operandType = Operand.OperandType.Register;
        instruction.operands[2] = new Operand();
        instruction.operands[2].operandType = Operand.OperandType.Register;
        instruction.function = new InstructionFunction() {
            @Override
            public void doInstruction(Instruction instruction) {
                int secondOperandValue = getValueOfRegister((String)instruction.operands[1].value);
                int thirdOperandValue = getValueOfRegister((String)instruction.operands[2].value);
                names.put((String)instruction.operands[0].value, secondOperandValue - thirdOperandValue);
            }
        };
        instructionSet.add(instruction);
        /////MUL
        //////////////Register, Register, Register
        instruction = new Instruction();
        instruction.name = "MUL";
        instruction.operands[0] = new Operand();
        instruction.operands[0].operandType = Operand.OperandType.Register;
        instruction.operands[1] = new Operand();
        instruction.operands[1].operandType = Operand.OperandType.Register;
        instruction.operands[2] = new Operand();
        instruction.operands[2].operandType = Operand.OperandType.Register;
        instruction.function = new InstructionFunction() {
            @Override
            public void doInstruction(Instruction instruction) {
                int secondOperandValue = getValueOfRegister((String)instruction.operands[1].value);
                int thirdOperandValue = getValueOfRegister((String)instruction.operands[2].value);
                names.put((String)instruction.operands[0].value, (int) (((long)secondOperandValue) * ((long)thirdOperandValue) & 0xffffffffL));
            }
        };
        instructionSet.add(instruction);
        /////UPM
        //////////////Register, Register, Register
        instruction = new Instruction();
        instruction.name = "UPM";
        instruction.operands[0] = new Operand();
        instruction.operands[0].operandType = Operand.OperandType.Register;
        instruction.operands[1] = new Operand();
        instruction.operands[1].operandType = Operand.OperandType.Register;
        instruction.operands[2] = new Operand();
        instruction.operands[2].operandType = Operand.OperandType.Register;
        instruction.function = new InstructionFunction() {
            @Override
            public void doInstruction(Instruction instruction) {
                int secondOperandValue = getValueOfRegister((String)instruction.operands[1].value);
                int thirdOperandValue = getValueOfRegister((String)instruction.operands[2].value);
                names.put((String)instruction.operands[0].value, (int) (((long)secondOperandValue) * ((long)thirdOperandValue) >>> 32L));
            }
        };
        instructionSet.add(instruction);
        /////DIV
        //////////////Register, Register, Register
        instruction = new Instruction();
        instruction.name = "DIV";
        instruction.operands[0] = new Operand();
        instruction.operands[0].operandType = Operand.OperandType.Register;
        instruction.operands[1] = new Operand();
        instruction.operands[1].operandType = Operand.OperandType.Register;
        instruction.operands[2] = new Operand();
        instruction.operands[2].operandType = Operand.OperandType.Register;
        instruction.function = new InstructionFunction() {
            @Override
            public void doInstruction(Instruction instruction) {
                int secondOperandValue = getValueOfRegister((String)instruction.operands[1].value);
                int thirdOperandValue = getValueOfRegister((String)instruction.operands[2].value);
                names.put((String)instruction.operands[0].value, secondOperandValue / thirdOperandValue);
            }
        };
        instructionSet.add(instruction);
        /////MOD
        //////////////Register, Register, Register
        instruction = new Instruction();
        instruction.name = "MOD";
        instruction.operands[0] = new Operand();
        instruction.operands[0].operandType = Operand.OperandType.Register;
        instruction.operands[1] = new Operand();
        instruction.operands[1].operandType = Operand.OperandType.Register;
        instruction.operands[2] = new Operand();
        instruction.operands[2].operandType = Operand.OperandType.Register;
        instruction.function = new InstructionFunction() {
            @Override
            public void doInstruction(Instruction instruction) {
                int secondOperandValue = getValueOfRegister((String)instruction.operands[1].value);
                int thirdOperandValue = getValueOfRegister((String)instruction.operands[2].value);
                names.put((String)instruction.operands[0].value, secondOperandValue % thirdOperandValue);
            }
        };
        instructionSet.add(instruction);
        /////AND
        //////////////Register, Register, Register
        instruction = new Instruction();
        instruction.name = "AND";
        instruction.operands[0] = new Operand();
        instruction.operands[0].operandType = Operand.OperandType.Register;
        instruction.operands[1] = new Operand();
        instruction.operands[1].operandType = Operand.OperandType.Register;
        instruction.operands[2] = new Operand();
        instruction.operands[2].operandType = Operand.OperandType.Register;
        instruction.function = new InstructionFunction() {
            @Override
            public void doInstruction(Instruction instruction) {
                int secondOperandValue = getValueOfRegister((String)instruction.operands[1].value);
                int thirdOperandValue = getValueOfRegister((String)instruction.operands[2].value);
                names.put((String)instruction.operands[0].value, secondOperandValue & thirdOperandValue);
            }
        };
        instructionSet.add(instruction);
        /////ORR
        //////////////Register, Register, Register
        instruction = new Instruction();
        instruction.name = "ORR";
        instruction.operands[0] = new Operand();
        instruction.operands[0].operandType = Operand.OperandType.Register;
        instruction.operands[1] = new Operand();
        instruction.operands[1].operandType = Operand.OperandType.Register;
        instruction.operands[2] = new Operand();
        instruction.operands[2].operandType = Operand.OperandType.Register;
        instruction.function = new InstructionFunction() {
            @Override
            public void doInstruction(Instruction instruction) {
                int secondOperandValue = getValueOfRegister((String)instruction.operands[1].value);
                int thirdOperandValue = getValueOfRegister((String)instruction.operands[2].value);
                names.put((String)instruction.operands[0].value, secondOperandValue | thirdOperandValue);
            }
        };
        instructionSet.add(instruction);
        /////XOR
        //////////////Register, Register, Register
        instruction = new Instruction();
        instruction.name = "XOR";
        instruction.operands[0] = new Operand();
        instruction.operands[0].operandType = Operand.OperandType.Register;
        instruction.operands[1] = new Operand();
        instruction.operands[1].operandType = Operand.OperandType.Register;
        instruction.operands[2] = new Operand();
        instruction.operands[2].operandType = Operand.OperandType.Register;
        instruction.function = new InstructionFunction() {
            @Override
            public void doInstruction(Instruction instruction) {
                int secondOperandValue = getValueOfRegister((String)instruction.operands[1].value);
                int thirdOperandValue = getValueOfRegister((String)instruction.operands[2].value);
                names.put((String)instruction.operands[0].value, secondOperandValue ^ thirdOperandValue);
            }
        };
        instructionSet.add(instruction);
        /////GRT
        //////////////Register, Register, Register
        instruction = new Instruction();
        instruction.name = "GRT";
        instruction.operands[0] = new Operand();
        instruction.operands[0].operandType = Operand.OperandType.Register;
        instruction.operands[1] = new Operand();
        instruction.operands[1].operandType = Operand.OperandType.Register;
        instruction.operands[2] = new Operand();
        instruction.operands[2].operandType = Operand.OperandType.Register;
        instruction.function = new InstructionFunction() {
            @Override
            public void doInstruction(Instruction instruction) {
                int secondOperandValue = getValueOfRegister((String)instruction.operands[1].value);
                int thirdOperandValue = getValueOfRegister((String)instruction.operands[2].value);
                names.put((String)instruction.operands[0].value, (secondOperandValue > thirdOperandValue) ? 1 :0);
            }
        };
        instructionSet.add(instruction);
        /////GEQ
        //////////////Register, Register, Register
        instruction = new Instruction();
        instruction.name = "GEQ";
        instruction.operands[0] = new Operand();
        instruction.operands[0].operandType = Operand.OperandType.Register;
        instruction.operands[1] = new Operand();
        instruction.operands[1].operandType = Operand.OperandType.Register;
        instruction.operands[2] = new Operand();
        instruction.operands[2].operandType = Operand.OperandType.Register;
        instruction.function = new InstructionFunction() {
            @Override
            public void doInstruction(Instruction instruction) {
                int secondOperandValue = getValueOfRegister((String)instruction.operands[1].value);
                int thirdOperandValue = getValueOfRegister((String)instruction.operands[2].value);
                names.put((String)instruction.operands[0].value, (secondOperandValue >= thirdOperandValue) ? 1 :0);
            }
        };
        instructionSet.add(instruction);
        /////EQL
        //////////////Register, Register, Register
        instruction = new Instruction();
        instruction.name = "EQL";
        instruction.operands[0] = new Operand();
        instruction.operands[0].operandType = Operand.OperandType.Register;
        instruction.operands[1] = new Operand();
        instruction.operands[1].operandType = Operand.OperandType.Register;
        instruction.operands[2] = new Operand();
        instruction.operands[2].operandType = Operand.OperandType.Register;
        instruction.function = new InstructionFunction() {
            @Override
            public void doInstruction(Instruction instruction) {
                int secondOperandValue = getValueOfRegister((String)instruction.operands[1].value);
                int thirdOperandValue = getValueOfRegister((String)instruction.operands[2].value);
                names.put((String)instruction.operands[0].value, (secondOperandValue == thirdOperandValue) ? 1 :0);
            }
        };
        instructionSet.add(instruction);
        /////LEQ
        //////////////Register, Register, Register
        instruction = new Instruction();
        instruction.name = "LEQ";
        instruction.operands[0] = new Operand();
        instruction.operands[0].operandType = Operand.OperandType.Register;
        instruction.operands[1] = new Operand();
        instruction.operands[1].operandType = Operand.OperandType.Register;
        instruction.operands[2] = new Operand();
        instruction.operands[2].operandType = Operand.OperandType.Register;
        instruction.function = new InstructionFunction() {
            @Override
            public void doInstruction(Instruction instruction) {
                int secondOperandValue = getValueOfRegister((String)instruction.operands[1].value);
                int thirdOperandValue = getValueOfRegister((String)instruction.operands[2].value);
                names.put((String)instruction.operands[0].value, (secondOperandValue <= thirdOperandValue) ? 1 :0);
            }
        };
        instructionSet.add(instruction);
        /////LWR
        //////////////Register, Register, Register
        instruction = new Instruction();
        instruction.name = "LWR";
        instruction.operands[0] = new Operand();
        instruction.operands[0].operandType = Operand.OperandType.Register;
        instruction.operands[1] = new Operand();
        instruction.operands[1].operandType = Operand.OperandType.Register;
        instruction.operands[2] = new Operand();
        instruction.operands[2].operandType = Operand.OperandType.Register;
        instruction.function = new InstructionFunction() {
            @Override
            public void doInstruction(Instruction instruction) {
                int secondOperandValue = getValueOfRegister((String)instruction.operands[1].value);
                int thirdOperandValue = getValueOfRegister((String)instruction.operands[2].value);
                names.put((String)instruction.operands[0].value, (secondOperandValue < thirdOperandValue) ? 1 :0);
            }
        };
        instructionSet.add(instruction);
        /////NEQ
        //////////////Register, Register, Register
        instruction = new Instruction();
        instruction.name = "NEQ";
        instruction.operands[0] = new Operand();
        instruction.operands[0].operandType = Operand.OperandType.Register;
        instruction.operands[1] = new Operand();
        instruction.operands[1].operandType = Operand.OperandType.Register;
        instruction.operands[2] = new Operand();
        instruction.operands[2].operandType = Operand.OperandType.Register;
        instruction.function = new InstructionFunction() {
            @Override
            public void doInstruction(Instruction instruction) {
                int secondOperandValue = getValueOfRegister((String)instruction.operands[1].value);
                int thirdOperandValue = getValueOfRegister((String)instruction.operands[2].value);
                names.put((String)instruction.operands[0].value, (secondOperandValue != thirdOperandValue) ? 1 :0);
            }
        };
        instructionSet.add(instruction);
        /////ADD
        //////////////Register, Id, Register
        instruction = new Instruction();
        instruction.name = "ADD";
        instruction.operands[0] = new Operand();
        instruction.operands[0].operandType = Operand.OperandType.Register;
        instruction.operands[1] = new Operand();
        instruction.operands[1].operandType = Operand.OperandType.Id;
        instruction.operands[2] = new Operand();
        instruction.operands[2].operandType = Operand.OperandType.Register;
        instruction.function = new InstructionFunction() {
            @Override
            public void doInstruction(Instruction instruction) {
                int secondOperandValue = getValueOfName((String)instruction.operands[1].value);
                int thirdOperandValue = getValueOfRegister((String)instruction.operands[2].value);
                names.put((String)instruction.operands[0].value, secondOperandValue + thirdOperandValue);
            }
        };
        instructionSet.add(instruction);
        /////SUB
        //////////////Register, Id, Register
        instruction = new Instruction();
        instruction.name = "SUB";
        instruction.operands[0] = new Operand();
        instruction.operands[0].operandType = Operand.OperandType.Register;
        instruction.operands[1] = new Operand();
        instruction.operands[1].operandType = Operand.OperandType.Id;
        instruction.operands[2] = new Operand();
        instruction.operands[2].operandType = Operand.OperandType.Register;
        instruction.function = new InstructionFunction() {
            @Override
            public void doInstruction(Instruction instruction) {
                int secondOperandValue = getValueOfRegister((String)instruction.operands[1].value);
                int thirdOperandValue = getValueOfRegister((String)instruction.operands[2].value);
                names.put((String)instruction.operands[0].value, secondOperandValue - thirdOperandValue);
            }
        };
        instructionSet.add(instruction);
        /////MUL
        //////////////Register, Id, Register
        instruction = new Instruction();
        instruction.name = "MUL";
        instruction.operands[0] = new Operand();
        instruction.operands[0].operandType = Operand.OperandType.Register;
        instruction.operands[1] = new Operand();
        instruction.operands[1].operandType = Operand.OperandType.Id;
        instruction.operands[2] = new Operand();
        instruction.operands[2].operandType = Operand.OperandType.Register;
        instruction.function = new InstructionFunction() {
            @Override
            public void doInstruction(Instruction instruction) {
                int secondOperandValue = getValueOfName((String)instruction.operands[1].value);
                int thirdOperandValue = getValueOfRegister((String)instruction.operands[2].value);
                names.put((String)instruction.operands[0].value, (int) (((long)secondOperandValue) * ((long)thirdOperandValue) & 0xffffffffL));
            }
        };
        instructionSet.add(instruction);
        /////UPM
        //////////////Register, Id, Register
        instruction = new Instruction();
        instruction.name = "UPM";
        instruction.operands[0] = new Operand();
        instruction.operands[0].operandType = Operand.OperandType.Register;
        instruction.operands[1] = new Operand();
        instruction.operands[1].operandType = Operand.OperandType.Id;
        instruction.operands[2] = new Operand();
        instruction.operands[2].operandType = Operand.OperandType.Register;
        instruction.function = new InstructionFunction() {
            @Override
            public void doInstruction(Instruction instruction) {
                int secondOperandValue = getValueOfName((String)instruction.operands[1].value);
                int thirdOperandValue = getValueOfRegister((String)instruction.operands[2].value);
                names.put((String)instruction.operands[0].value, (int) (((long)secondOperandValue) * ((long)thirdOperandValue) >>> 32L));
            }
        };
        instructionSet.add(instruction);
        /////DIV
        //////////////Register, Id, Register
        instruction = new Instruction();
        instruction.name = "DIV";
        instruction.operands[0] = new Operand();
        instruction.operands[0].operandType = Operand.OperandType.Register;
        instruction.operands[1] = new Operand();
        instruction.operands[1].operandType = Operand.OperandType.Id;
        instruction.operands[2] = new Operand();
        instruction.operands[2].operandType = Operand.OperandType.Register;
        instruction.function = new InstructionFunction() {
            @Override
            public void doInstruction(Instruction instruction) {
                int secondOperandValue = getValueOfName((String)instruction.operands[1].value);
                int thirdOperandValue = getValueOfRegister((String)instruction.operands[2].value);
                names.put((String)instruction.operands[0].value, secondOperandValue / thirdOperandValue);
            }
        };
        instructionSet.add(instruction);
        /////MOD
        //////////////Register, Id, Register
        instruction = new Instruction();
        instruction.name = "MOD";
        instruction.operands[0] = new Operand();
        instruction.operands[0].operandType = Operand.OperandType.Register;
        instruction.operands[1] = new Operand();
        instruction.operands[1].operandType = Operand.OperandType.Id;
        instruction.operands[2] = new Operand();
        instruction.operands[2].operandType = Operand.OperandType.Register;
        instruction.function = new InstructionFunction() {
            @Override
            public void doInstruction(Instruction instruction) {
                int secondOperandValue = getValueOfName((String)instruction.operands[1].value);
                int thirdOperandValue = getValueOfRegister((String)instruction.operands[2].value);
                names.put((String)instruction.operands[0].value, secondOperandValue % thirdOperandValue);
            }
        };
        instructionSet.add(instruction);
        /////AND
        //////////////Register, Id, Register
        instruction = new Instruction();
        instruction.name = "AND";
        instruction.operands[0] = new Operand();
        instruction.operands[0].operandType = Operand.OperandType.Register;
        instruction.operands[1] = new Operand();
        instruction.operands[1].operandType = Operand.OperandType.Id;
        instruction.operands[2] = new Operand();
        instruction.operands[2].operandType = Operand.OperandType.Register;
        instruction.function = new InstructionFunction() {
            @Override
            public void doInstruction(Instruction instruction) {
                int secondOperandValue = getValueOfName((String)instruction.operands[1].value);
                int thirdOperandValue = getValueOfRegister((String)instruction.operands[2].value);
                names.put((String)instruction.operands[0].value, secondOperandValue & thirdOperandValue);
            }
        };
        instructionSet.add(instruction);
        /////ORR
        //////////////Register, Id, Register
        instruction = new Instruction();
        instruction.name = "ORR";
        instruction.operands[0] = new Operand();
        instruction.operands[0].operandType = Operand.OperandType.Register;
        instruction.operands[1] = new Operand();
        instruction.operands[1].operandType = Operand.OperandType.Id;
        instruction.operands[2] = new Operand();
        instruction.operands[2].operandType = Operand.OperandType.Register;
        instruction.function = new InstructionFunction() {
            @Override
            public void doInstruction(Instruction instruction) {
                int secondOperandValue = getValueOfName((String)instruction.operands[1].value);
                int thirdOperandValue = getValueOfRegister((String)instruction.operands[2].value);
                names.put((String)instruction.operands[0].value, secondOperandValue | thirdOperandValue);
            }
        };
        instructionSet.add(instruction);
        /////XOR
        //////////////Register, Id, Register
        instruction = new Instruction();
        instruction.name = "XOR";
        instruction.operands[0] = new Operand();
        instruction.operands[0].operandType = Operand.OperandType.Register;
        instruction.operands[1] = new Operand();
        instruction.operands[1].operandType = Operand.OperandType.Id;
        instruction.operands[2] = new Operand();
        instruction.operands[2].operandType = Operand.OperandType.Register;
        instruction.function = new InstructionFunction() {
            @Override
            public void doInstruction(Instruction instruction) {
                int secondOperandValue = getValueOfName((String)instruction.operands[1].value);
                int thirdOperandValue = getValueOfRegister((String)instruction.operands[2].value);
                names.put((String)instruction.operands[0].value, secondOperandValue ^ thirdOperandValue);
            }
        };
        instructionSet.add(instruction);
        /////GRT
        //////////////Register, Id, Register
        instruction = new Instruction();
        instruction.name = "GRT";
        instruction.operands[0] = new Operand();
        instruction.operands[0].operandType = Operand.OperandType.Register;
        instruction.operands[1] = new Operand();
        instruction.operands[1].operandType = Operand.OperandType.Id;
        instruction.operands[2] = new Operand();
        instruction.operands[2].operandType = Operand.OperandType.Register;
        instruction.function = new InstructionFunction() {
            @Override
            public void doInstruction(Instruction instruction) {
                int secondOperandValue = getValueOfName((String)instruction.operands[1].value);
                int thirdOperandValue = getValueOfRegister((String)instruction.operands[2].value);
                names.put((String)instruction.operands[0].value, (secondOperandValue > thirdOperandValue) ? 1 :0);
            }
        };
        instructionSet.add(instruction);
        /////GEQ
        //////////////Register, Id, Register
        instruction = new Instruction();
        instruction.name = "GEQ";
        instruction.operands[0] = new Operand();
        instruction.operands[0].operandType = Operand.OperandType.Register;
        instruction.operands[1] = new Operand();
        instruction.operands[1].operandType = Operand.OperandType.Id;
        instruction.operands[2] = new Operand();
        instruction.operands[2].operandType = Operand.OperandType.Register;
        instruction.function = new InstructionFunction() {
            @Override
            public void doInstruction(Instruction instruction) {
                int secondOperandValue = getValueOfName((String)instruction.operands[1].value);
                int thirdOperandValue = getValueOfRegister((String)instruction.operands[2].value);
                names.put((String)instruction.operands[0].value, (secondOperandValue >= thirdOperandValue) ? 1 :0);
            }
        };
        instructionSet.add(instruction);
        /////EQL
        //////////////Register, Id, Register
        instruction = new Instruction();
        instruction.name = "EQL";
        instruction.operands[0] = new Operand();
        instruction.operands[0].operandType = Operand.OperandType.Register;
        instruction.operands[1] = new Operand();
        instruction.operands[1].operandType = Operand.OperandType.Id;
        instruction.operands[2] = new Operand();
        instruction.operands[2].operandType = Operand.OperandType.Register;
        instruction.function = new InstructionFunction() {
            @Override
            public void doInstruction(Instruction instruction) {
                int secondOperandValue = getValueOfName((String)instruction.operands[1].value);
                int thirdOperandValue = getValueOfRegister((String)instruction.operands[2].value);
                names.put((String)instruction.operands[0].value, (secondOperandValue == thirdOperandValue) ? 1 :0);
            }
        };
        instructionSet.add(instruction);
        /////LEQ
        //////////////Register, Id, Register
        instruction = new Instruction();
        instruction.name = "LEQ";
        instruction.operands[0] = new Operand();
        instruction.operands[0].operandType = Operand.OperandType.Register;
        instruction.operands[1] = new Operand();
        instruction.operands[1].operandType = Operand.OperandType.Id;
        instruction.operands[2] = new Operand();
        instruction.operands[2].operandType = Operand.OperandType.Register;
        instruction.function = new InstructionFunction() {
            @Override
            public void doInstruction(Instruction instruction) {
                int secondOperandValue = getValueOfName((String)instruction.operands[1].value);
                int thirdOperandValue = getValueOfRegister((String)instruction.operands[2].value);
                names.put((String)instruction.operands[0].value, (secondOperandValue <= thirdOperandValue) ? 1 :0);
            }
        };
        instructionSet.add(instruction);
        /////LWR
        //////////////Register, Id, Register
        instruction = new Instruction();
        instruction.name = "LWR";
        instruction.operands[0] = new Operand();
        instruction.operands[0].operandType = Operand.OperandType.Register;
        instruction.operands[1] = new Operand();
        instruction.operands[1].operandType = Operand.OperandType.Id;
        instruction.operands[2] = new Operand();
        instruction.operands[2].operandType = Operand.OperandType.Register;
        instruction.function = new InstructionFunction() {
            @Override
            public void doInstruction(Instruction instruction) {
                int secondOperandValue = getValueOfName((String)instruction.operands[1].value);
                int thirdOperandValue = getValueOfRegister((String)instruction.operands[2].value);
                names.put((String)instruction.operands[0].value, (secondOperandValue < thirdOperandValue) ? 1 :0);
            }
        };
        instructionSet.add(instruction);
        /////NEQ
        //////////////Register, Id, Register
        instruction = new Instruction();
        instruction.name = "NEQ";
        instruction.operands[0] = new Operand();
        instruction.operands[0].operandType = Operand.OperandType.Register;
        instruction.operands[1] = new Operand();
        instruction.operands[1].operandType = Operand.OperandType.Id;
        instruction.operands[2] = new Operand();
        instruction.operands[2].operandType = Operand.OperandType.Register;
        instruction.function = new InstructionFunction() {
            @Override
            public void doInstruction(Instruction instruction) {
                int secondOperandValue = getValueOfName((String)instruction.operands[1].value);
                int thirdOperandValue = getValueOfRegister((String)instruction.operands[2].value);
                names.put((String)instruction.operands[0].value, (secondOperandValue != thirdOperandValue) ? 1 :0);
            }
        };
        instructionSet.add(instruction);
        /////ADD
        //////////////Register, Register, Id
        instruction = new Instruction();
        instruction.name = "ADD";
        instruction.operands[0] = new Operand();
        instruction.operands[0].operandType = Operand.OperandType.Register;
        instruction.operands[1] = new Operand();
        instruction.operands[1].operandType = Operand.OperandType.Register;
        instruction.operands[2] = new Operand();
        instruction.operands[2].operandType = Operand.OperandType.Id;
        instruction.function = new InstructionFunction() {
            @Override
            public void doInstruction(Instruction instruction) {
                int secondOperandValue = getValueOfRegister((String)instruction.operands[1].value);
                int thirdOperandValue = getValueOfName((String)instruction.operands[2].value);
                names.put((String)instruction.operands[0].value, secondOperandValue + thirdOperandValue);
            }
        };
        instructionSet.add(instruction);
        /////SUB
        //////////////Register, Register, Id
        instruction = new Instruction();
        instruction.name = "SUB";
        instruction.operands[0] = new Operand();
        instruction.operands[0].operandType = Operand.OperandType.Register;
        instruction.operands[1] = new Operand();
        instruction.operands[1].operandType = Operand.OperandType.Register;
        instruction.operands[2] = new Operand();
        instruction.operands[2].operandType = Operand.OperandType.Id;
        instruction.function = new InstructionFunction() {
            @Override
            public void doInstruction(Instruction instruction) {
                int secondOperandValue = getValueOfRegister((String)instruction.operands[1].value);
                int thirdOperandValue = getValueOfName((String)instruction.operands[2].value);
                names.put((String)instruction.operands[0].value, secondOperandValue - thirdOperandValue);
            }
        };
        instructionSet.add(instruction);
        /////MUL
        //////////////Register, Register, Id
        instruction = new Instruction();
        instruction.name = "MUL";
        instruction.operands[0] = new Operand();
        instruction.operands[0].operandType = Operand.OperandType.Register;
        instruction.operands[1] = new Operand();
        instruction.operands[1].operandType = Operand.OperandType.Register;
        instruction.operands[2] = new Operand();
        instruction.operands[2].operandType = Operand.OperandType.Id;
        instruction.function = new InstructionFunction() {
            @Override
            public void doInstruction(Instruction instruction) {
                int secondOperandValue = getValueOfRegister((String)instruction.operands[1].value);
                int thirdOperandValue = getValueOfName((String)instruction.operands[2].value);
                names.put((String)instruction.operands[0].value, (int) (((long)secondOperandValue) * ((long)thirdOperandValue) & 0xffffffffL));
            }
        };
        instructionSet.add(instruction);
        /////UPM
        //////////////Register, Register, Id
        instruction = new Instruction();
        instruction.name = "UPM";
        instruction.operands[0] = new Operand();
        instruction.operands[0].operandType = Operand.OperandType.Register;
        instruction.operands[1] = new Operand();
        instruction.operands[1].operandType = Operand.OperandType.Register;
        instruction.operands[2] = new Operand();
        instruction.operands[2].operandType = Operand.OperandType.Id;
        instruction.function = new InstructionFunction() {
            @Override
            public void doInstruction(Instruction instruction) {
                int secondOperandValue = getValueOfRegister((String)instruction.operands[1].value);
                int thirdOperandValue = getValueOfName((String)instruction.operands[2].value);
                names.put((String)instruction.operands[0].value, (int) (((long)secondOperandValue) * ((long)thirdOperandValue) >>> 32L));
            }
        };
        instructionSet.add(instruction);
        /////DIV
        //////////////Register, Register, Id
        instruction = new Instruction();
        instruction.name = "DIV";
        instruction.operands[0] = new Operand();
        instruction.operands[0].operandType = Operand.OperandType.Register;
        instruction.operands[1] = new Operand();
        instruction.operands[1].operandType = Operand.OperandType.Register;
        instruction.operands[2] = new Operand();
        instruction.operands[2].operandType = Operand.OperandType.Id;
        instruction.function = new InstructionFunction() {
            @Override
            public void doInstruction(Instruction instruction) {
                int secondOperandValue = getValueOfRegister((String)instruction.operands[1].value);
                int thirdOperandValue = getValueOfName((String)instruction.operands[2].value);
                names.put((String)instruction.operands[0].value, secondOperandValue / thirdOperandValue);
            }
        };
        instructionSet.add(instruction);
        /////MOD
        //////////////Register, Register, Id
        instruction = new Instruction();
        instruction.name = "MOD";
        instruction.operands[0] = new Operand();
        instruction.operands[0].operandType = Operand.OperandType.Register;
        instruction.operands[1] = new Operand();
        instruction.operands[1].operandType = Operand.OperandType.Register;
        instruction.operands[2] = new Operand();
        instruction.operands[2].operandType = Operand.OperandType.Id;
        instruction.function = new InstructionFunction() {
            @Override
            public void doInstruction(Instruction instruction) {
                int secondOperandValue = getValueOfRegister((String)instruction.operands[1].value);
                int thirdOperandValue = getValueOfName((String)instruction.operands[2].value);
                names.put((String)instruction.operands[0].value, secondOperandValue % thirdOperandValue);
            }
        };
        instructionSet.add(instruction);
        /////AND
        //////////////Register, Register, Id
        instruction = new Instruction();
        instruction.name = "AND";
        instruction.operands[0] = new Operand();
        instruction.operands[0].operandType = Operand.OperandType.Register;
        instruction.operands[1] = new Operand();
        instruction.operands[1].operandType = Operand.OperandType.Register;
        instruction.operands[2] = new Operand();
        instruction.operands[2].operandType = Operand.OperandType.Id;
        instruction.function = new InstructionFunction() {
            @Override
            public void doInstruction(Instruction instruction) {
                int secondOperandValue = getValueOfRegister((String)instruction.operands[1].value);
                int thirdOperandValue = getValueOfName((String)instruction.operands[2].value);
                names.put((String)instruction.operands[0].value, secondOperandValue & thirdOperandValue);
            }
        };
        instructionSet.add(instruction);
        /////ORR
        //////////////Register, Register, Id
        instruction = new Instruction();
        instruction.name = "ORR";
        instruction.operands[0] = new Operand();
        instruction.operands[0].operandType = Operand.OperandType.Register;
        instruction.operands[1] = new Operand();
        instruction.operands[1].operandType = Operand.OperandType.Register;
        instruction.operands[2] = new Operand();
        instruction.operands[2].operandType = Operand.OperandType.Id;
        instruction.function = new InstructionFunction() {
            @Override
            public void doInstruction(Instruction instruction) {
                int secondOperandValue = getValueOfRegister((String)instruction.operands[1].value);
                int thirdOperandValue = getValueOfName((String)instruction.operands[2].value);
                names.put((String)instruction.operands[0].value, secondOperandValue | thirdOperandValue);
            }
        };
        instructionSet.add(instruction);
        /////XOR
        //////////////Register, Register, Id
        instruction = new Instruction();
        instruction.name = "XOR";
        instruction.operands[0] = new Operand();
        instruction.operands[0].operandType = Operand.OperandType.Register;
        instruction.operands[1] = new Operand();
        instruction.operands[1].operandType = Operand.OperandType.Register;
        instruction.operands[2] = new Operand();
        instruction.operands[2].operandType = Operand.OperandType.Id;
        instruction.function = new InstructionFunction() {
            @Override
            public void doInstruction(Instruction instruction) {
                int secondOperandValue = getValueOfRegister((String)instruction.operands[1].value);
                int thirdOperandValue = getValueOfName((String)instruction.operands[2].value);
                names.put((String)instruction.operands[0].value, secondOperandValue ^ thirdOperandValue);
            }
        };
        instructionSet.add(instruction);
        /////GRT
        //////////////Register, Register, Id
        instruction = new Instruction();
        instruction.name = "GRT";
        instruction.operands[0] = new Operand();
        instruction.operands[0].operandType = Operand.OperandType.Register;
        instruction.operands[1] = new Operand();
        instruction.operands[1].operandType = Operand.OperandType.Register;
        instruction.operands[2] = new Operand();
        instruction.operands[2].operandType = Operand.OperandType.Id;
        instruction.function = new InstructionFunction() {
            @Override
            public void doInstruction(Instruction instruction) {
                int secondOperandValue = getValueOfRegister((String)instruction.operands[1].value);
                int thirdOperandValue = getValueOfName((String)instruction.operands[2].value);
                names.put((String)instruction.operands[0].value, (secondOperandValue > thirdOperandValue) ? 1 :0);
            }
        };
        instructionSet.add(instruction);
        /////GEQ
        //////////////Register, Register, Id
        instruction = new Instruction();
        instruction.name = "GEQ";
        instruction.operands[0] = new Operand();
        instruction.operands[0].operandType = Operand.OperandType.Register;
        instruction.operands[1] = new Operand();
        instruction.operands[1].operandType = Operand.OperandType.Register;
        instruction.operands[2] = new Operand();
        instruction.operands[2].operandType = Operand.OperandType.Id;
        instruction.function = new InstructionFunction() {
            @Override
            public void doInstruction(Instruction instruction) {
                int secondOperandValue = getValueOfRegister((String)instruction.operands[1].value);
                int thirdOperandValue = getValueOfName((String)instruction.operands[2].value);
                names.put((String)instruction.operands[0].value, (secondOperandValue >= thirdOperandValue) ? 1 :0);
            }
        };
        instructionSet.add(instruction);
        /////EQL
        //////////////Register, Register, Id
        instruction = new Instruction();
        instruction.name = "EQL";
        instruction.operands[0] = new Operand();
        instruction.operands[0].operandType = Operand.OperandType.Register;
        instruction.operands[1] = new Operand();
        instruction.operands[1].operandType = Operand.OperandType.Register;
        instruction.operands[2] = new Operand();
        instruction.operands[2].operandType = Operand.OperandType.Id;
        instruction.function = new InstructionFunction() {
            @Override
            public void doInstruction(Instruction instruction) {
                int secondOperandValue = getValueOfRegister((String)instruction.operands[1].value);
                int thirdOperandValue = getValueOfName((String)instruction.operands[2].value);
                names.put((String)instruction.operands[0].value, (secondOperandValue == thirdOperandValue) ? 1 :0);
            }
        };
        instructionSet.add(instruction);
        /////LEQ
        //////////////Register, Register, Id
        instruction = new Instruction();
        instruction.name = "LEQ";
        instruction.operands[0] = new Operand();
        instruction.operands[0].operandType = Operand.OperandType.Register;
        instruction.operands[1] = new Operand();
        instruction.operands[1].operandType = Operand.OperandType.Register;
        instruction.operands[2] = new Operand();
        instruction.operands[2].operandType = Operand.OperandType.Id;
        instruction.function = new InstructionFunction() {
            @Override
            public void doInstruction(Instruction instruction) {
                int secondOperandValue = getValueOfRegister((String)instruction.operands[1].value);
                int thirdOperandValue = getValueOfName((String)instruction.operands[2].value);
                names.put((String)instruction.operands[0].value, (secondOperandValue <= thirdOperandValue) ? 1 :0);
            }
        };
        instructionSet.add(instruction);
        /////LWR
        //////////////Register, Register, Id
        instruction = new Instruction();
        instruction.name = "LWR";
        instruction.operands[0] = new Operand();
        instruction.operands[0].operandType = Operand.OperandType.Register;
        instruction.operands[1] = new Operand();
        instruction.operands[1].operandType = Operand.OperandType.Register;
        instruction.operands[2] = new Operand();
        instruction.operands[2].operandType = Operand.OperandType.Id;
        instruction.function = new InstructionFunction() {
            @Override
            public void doInstruction(Instruction instruction) {
                int secondOperandValue = getValueOfRegister((String)instruction.operands[1].value);
                int thirdOperandValue = getValueOfName((String)instruction.operands[2].value);
                names.put((String)instruction.operands[0].value, (secondOperandValue < thirdOperandValue) ? 1 :0);
            }
        };
        instructionSet.add(instruction);
        /////NEQ
        //////////////Register, Register, Id
        instruction = new Instruction();
        instruction.name = "NEQ";
        instruction.operands[0] = new Operand();
        instruction.operands[0].operandType = Operand.OperandType.Register;
        instruction.operands[1] = new Operand();
        instruction.operands[1].operandType = Operand.OperandType.Register;
        instruction.operands[2] = new Operand();
        instruction.operands[2].operandType = Operand.OperandType.Id;
        instruction.function = new InstructionFunction() {
            @Override
            public void doInstruction(Instruction instruction) {
                int secondOperandValue = getValueOfRegister((String)instruction.operands[1].value);
                int thirdOperandValue = getValueOfName((String)instruction.operands[2].value);
                names.put((String)instruction.operands[0].value, (secondOperandValue != thirdOperandValue) ? 1 :0);
            }
        };
        instructionSet.add(instruction);
        /////ADD
        //////////////Register, Number, Register
        instruction = new Instruction();
        instruction.name = "ADD";
        instruction.operands[0] = new Operand();
        instruction.operands[0].operandType = Operand.OperandType.Register;
        instruction.operands[1] = new Operand();
        instruction.operands[1].operandType = Operand.OperandType.Number;
        instruction.operands[2] = new Operand();
        instruction.operands[2].operandType = Operand.OperandType.Register;
        instruction.function = new InstructionFunction() {
            @Override
            public void doInstruction(Instruction instruction) {
                int secondOperandValue = (Integer) instruction.operands[0].value;
                int thirdOperandValue = getValueOfRegister((String)instruction.operands[2].value);
                names.put((String)instruction.operands[0].value, secondOperandValue + thirdOperandValue);
            }
        };
        instructionSet.add(instruction);
        /////SUB
        //////////////Register, Number, Register
        instruction = new Instruction();
        instruction.name = "SUB";
        instruction.operands[0] = new Operand();
        instruction.operands[0].operandType = Operand.OperandType.Register;
        instruction.operands[1] = new Operand();
        instruction.operands[1].operandType = Operand.OperandType.Number;
        instruction.operands[2] = new Operand();
        instruction.operands[2].operandType = Operand.OperandType.Register;
        instruction.function = new InstructionFunction() {
            @Override
            public void doInstruction(Instruction instruction) {
                int secondOperandValue = (Integer) instruction.operands[0].value;
                int thirdOperandValue = getValueOfRegister((String)instruction.operands[2].value);
                names.put((String)instruction.operands[0].value, secondOperandValue - thirdOperandValue);
            }
        };
        instructionSet.add(instruction);
        /////MUL
        //////////////Register, Number, Register
        instruction = new Instruction();
        instruction.name = "MUL";
        instruction.operands[0] = new Operand();
        instruction.operands[0].operandType = Operand.OperandType.Register;
        instruction.operands[1] = new Operand();
        instruction.operands[1].operandType = Operand.OperandType.Number;
        instruction.operands[2] = new Operand();
        instruction.operands[2].operandType = Operand.OperandType.Register;
        instruction.function = new InstructionFunction() {
            @Override
            public void doInstruction(Instruction instruction) {
                int secondOperandValue = (Integer) instruction.operands[0].value;
                int thirdOperandValue = getValueOfRegister((String)instruction.operands[2].value);
                names.put((String)instruction.operands[0].value, (int) (((long)secondOperandValue) * ((long)thirdOperandValue) & 0xffffffffL));
            }
        };
        instructionSet.add(instruction);
        /////UPM
        //////////////Register, Number, Register
        instruction = new Instruction();
        instruction.name = "UPM";
        instruction.operands[0] = new Operand();
        instruction.operands[0].operandType = Operand.OperandType.Register;
        instruction.operands[1] = new Operand();
        instruction.operands[1].operandType = Operand.OperandType.Number;
        instruction.operands[2] = new Operand();
        instruction.operands[2].operandType = Operand.OperandType.Register;
        instruction.function = new InstructionFunction() {
            @Override
            public void doInstruction(Instruction instruction) {
                int secondOperandValue = (Integer) instruction.operands[0].value;
                int thirdOperandValue = getValueOfRegister((String)instruction.operands[2].value);
                names.put((String)instruction.operands[0].value, (int) (((long)secondOperandValue) * ((long)thirdOperandValue) >>> 32L));
            }
        };
        instructionSet.add(instruction);
        /////DIV
        //////////////Register, Number, Register
        instruction = new Instruction();
        instruction.name = "DIV";
        instruction.operands[0] = new Operand();
        instruction.operands[0].operandType = Operand.OperandType.Register;
        instruction.operands[1] = new Operand();
        instruction.operands[1].operandType = Operand.OperandType.Number;
        instruction.operands[2] = new Operand();
        instruction.operands[2].operandType = Operand.OperandType.Register;
        instruction.function = new InstructionFunction() {
            @Override
            public void doInstruction(Instruction instruction) {
                int secondOperandValue = (Integer) instruction.operands[0].value;
                int thirdOperandValue = getValueOfRegister((String)instruction.operands[2].value);
                names.put((String)instruction.operands[0].value, secondOperandValue / thirdOperandValue);
            }
        };
        instructionSet.add(instruction);
        /////MOD
        //////////////Register, Number, Register
        instruction = new Instruction();
        instruction.name = "MOD";
        instruction.operands[0] = new Operand();
        instruction.operands[0].operandType = Operand.OperandType.Register;
        instruction.operands[1] = new Operand();
        instruction.operands[1].operandType = Operand.OperandType.Number;
        instruction.operands[2] = new Operand();
        instruction.operands[2].operandType = Operand.OperandType.Register;
        instruction.function = new InstructionFunction() {
            @Override
            public void doInstruction(Instruction instruction) {
                int secondOperandValue = (Integer) instruction.operands[0].value;
                int thirdOperandValue = getValueOfRegister((String)instruction.operands[2].value);
                names.put((String)instruction.operands[0].value, secondOperandValue % thirdOperandValue);
            }
        };
        instructionSet.add(instruction);
        /////AND
        //////////////Register, Number, Register
        instruction = new Instruction();
        instruction.name = "AND";
        instruction.operands[0] = new Operand();
        instruction.operands[0].operandType = Operand.OperandType.Register;
        instruction.operands[1] = new Operand();
        instruction.operands[1].operandType = Operand.OperandType.Number;
        instruction.operands[2] = new Operand();
        instruction.operands[2].operandType = Operand.OperandType.Register;
        instruction.function = new InstructionFunction() {
            @Override
            public void doInstruction(Instruction instruction) {
                int secondOperandValue = (Integer) instruction.operands[0].value;
                int thirdOperandValue = getValueOfRegister((String)instruction.operands[2].value);
                names.put((String)instruction.operands[0].value, secondOperandValue & thirdOperandValue);
            }
        };
        instructionSet.add(instruction);
        /////ORR
        //////////////Register, Number, Register
        instruction = new Instruction();
        instruction.name = "ORR";
        instruction.operands[0] = new Operand();
        instruction.operands[0].operandType = Operand.OperandType.Register;
        instruction.operands[1] = new Operand();
        instruction.operands[1].operandType = Operand.OperandType.Number;
        instruction.operands[2] = new Operand();
        instruction.operands[2].operandType = Operand.OperandType.Register;
        instruction.function = new InstructionFunction() {
            @Override
            public void doInstruction(Instruction instruction) {
                int secondOperandValue = (Integer) instruction.operands[0].value;
                int thirdOperandValue = getValueOfRegister((String)instruction.operands[2].value);
                names.put((String)instruction.operands[0].value, secondOperandValue | thirdOperandValue);
            }
        };
        instructionSet.add(instruction);
        /////XOR
        //////////////Register, Number, Register
        instruction = new Instruction();
        instruction.name = "XOR";
        instruction.operands[0] = new Operand();
        instruction.operands[0].operandType = Operand.OperandType.Register;
        instruction.operands[1] = new Operand();
        instruction.operands[1].operandType = Operand.OperandType.Number;
        instruction.operands[2] = new Operand();
        instruction.operands[2].operandType = Operand.OperandType.Register;
        instruction.function = new InstructionFunction() {
            @Override
            public void doInstruction(Instruction instruction) {
                int secondOperandValue = (Integer) instruction.operands[0].value;
                int thirdOperandValue = getValueOfRegister((String)instruction.operands[2].value);
                names.put((String)instruction.operands[0].value, secondOperandValue ^ thirdOperandValue);
            }
        };
        instructionSet.add(instruction);
        /////GRT
        //////////////Register, Number, Register
        instruction = new Instruction();
        instruction.name = "GRT";
        instruction.operands[0] = new Operand();
        instruction.operands[0].operandType = Operand.OperandType.Register;
        instruction.operands[1] = new Operand();
        instruction.operands[1].operandType = Operand.OperandType.Number;
        instruction.operands[2] = new Operand();
        instruction.operands[2].operandType = Operand.OperandType.Register;
        instruction.function = new InstructionFunction() {
            @Override
            public void doInstruction(Instruction instruction) {
                int secondOperandValue = (Integer) instruction.operands[0].value;
                int thirdOperandValue = getValueOfRegister((String)instruction.operands[2].value);
                names.put((String)instruction.operands[0].value, (secondOperandValue > thirdOperandValue) ? 1 :0);
            }
        };
        instructionSet.add(instruction);
        /////GEQ
        //////////////Register, Number, Register
        instruction = new Instruction();
        instruction.name = "GEQ";
        instruction.operands[0] = new Operand();
        instruction.operands[0].operandType = Operand.OperandType.Register;
        instruction.operands[1] = new Operand();
        instruction.operands[1].operandType = Operand.OperandType.Number;
        instruction.operands[2] = new Operand();
        instruction.operands[2].operandType = Operand.OperandType.Register;
        instruction.function = new InstructionFunction() {
            @Override
            public void doInstruction(Instruction instruction) {
                int secondOperandValue = (Integer) instruction.operands[0].value;
                int thirdOperandValue = getValueOfRegister((String)instruction.operands[2].value);
                names.put((String)instruction.operands[0].value, (secondOperandValue >= thirdOperandValue) ? 1 :0);
            }
        };
        instructionSet.add(instruction);
        /////EQL
        //////////////Register, Number, Register
        instruction = new Instruction();
        instruction.name = "EQL";
        instruction.operands[0] = new Operand();
        instruction.operands[0].operandType = Operand.OperandType.Register;
        instruction.operands[1] = new Operand();
        instruction.operands[1].operandType = Operand.OperandType.Number;
        instruction.operands[2] = new Operand();
        instruction.operands[2].operandType = Operand.OperandType.Register;
        instruction.function = new InstructionFunction() {
            @Override
            public void doInstruction(Instruction instruction) {
                int secondOperandValue = (Integer) instruction.operands[0].value;
                int thirdOperandValue = getValueOfRegister((String)instruction.operands[2].value);
                names.put((String)instruction.operands[0].value, (secondOperandValue == thirdOperandValue) ? 1 :0);
            }
        };
        instructionSet.add(instruction);
        /////LEQ
        //////////////Register, Number, Register
        instruction = new Instruction();
        instruction.name = "LEQ";
        instruction.operands[0] = new Operand();
        instruction.operands[0].operandType = Operand.OperandType.Register;
        instruction.operands[1] = new Operand();
        instruction.operands[1].operandType = Operand.OperandType.Number;
        instruction.operands[2] = new Operand();
        instruction.operands[2].operandType = Operand.OperandType.Register;
        instruction.function = new InstructionFunction() {
            @Override
            public void doInstruction(Instruction instruction) {
                int secondOperandValue = (Integer) instruction.operands[0].value;
                int thirdOperandValue = getValueOfRegister((String)instruction.operands[2].value);
                names.put((String)instruction.operands[0].value, (secondOperandValue <= thirdOperandValue) ? 1 :0);
            }
        };
        instructionSet.add(instruction);
        /////LWR
        //////////////Register, Number, Register
        instruction = new Instruction();
        instruction.name = "LWR";
        instruction.operands[0] = new Operand();
        instruction.operands[0].operandType = Operand.OperandType.Register;
        instruction.operands[1] = new Operand();
        instruction.operands[1].operandType = Operand.OperandType.Number;
        instruction.operands[2] = new Operand();
        instruction.operands[2].operandType = Operand.OperandType.Register;
        instruction.function = new InstructionFunction() {
            @Override
            public void doInstruction(Instruction instruction) {
                int secondOperandValue = (Integer) instruction.operands[0].value;
                int thirdOperandValue = getValueOfRegister((String)instruction.operands[2].value);
                names.put((String)instruction.operands[0].value, (secondOperandValue < thirdOperandValue) ? 1 :0);
            }
        };
        instructionSet.add(instruction);
        /////NEQ
        //////////////Register, Number, Register
        instruction = new Instruction();
        instruction.name = "NEQ";
        instruction.operands[0] = new Operand();
        instruction.operands[0].operandType = Operand.OperandType.Register;
        instruction.operands[1] = new Operand();
        instruction.operands[1].operandType = Operand.OperandType.Number;
        instruction.operands[2] = new Operand();
        instruction.operands[2].operandType = Operand.OperandType.Register;
        instruction.function = new InstructionFunction() {
            @Override
            public void doInstruction(Instruction instruction) {
                int secondOperandValue = (Integer) instruction.operands[0].value;
                int thirdOperandValue = getValueOfRegister((String)instruction.operands[2].value);
                names.put((String)instruction.operands[0].value, (secondOperandValue != thirdOperandValue) ? 1 :0);
            }
        };
        instructionSet.add(instruction);
        /////ADD
        //////////////Register, Register, Number
        instruction = new Instruction();
        instruction.name = "ADD";
        instruction.operands[0] = new Operand();
        instruction.operands[0].operandType = Operand.OperandType.Register;
        instruction.operands[1] = new Operand();
        instruction.operands[1].operandType = Operand.OperandType.Register;
        instruction.operands[2] = new Operand();
        instruction.operands[2].operandType = Operand.OperandType.Number;
        instruction.function = new InstructionFunction() {
            @Override
            public void doInstruction(Instruction instruction) {
                int secondOperandValue = getValueOfRegister((String)instruction.operands[1].value);
                int thirdOperandValue = (Integer) instruction.operands[2].value;
                names.put((String)instruction.operands[0].value, secondOperandValue + thirdOperandValue);
            }
        };
        instructionSet.add(instruction);
        /////SUB
        //////////////Register, Register, Number
        instruction = new Instruction();
        instruction.name = "SUB";
        instruction.operands[0] = new Operand();
        instruction.operands[0].operandType = Operand.OperandType.Register;
        instruction.operands[1] = new Operand();
        instruction.operands[1].operandType = Operand.OperandType.Register;
        instruction.operands[2] = new Operand();
        instruction.operands[2].operandType = Operand.OperandType.Number;
        instruction.function = new InstructionFunction() {
            @Override
            public void doInstruction(Instruction instruction) {
                int secondOperandValue = getValueOfRegister((String)instruction.operands[1].value);
                int thirdOperandValue = (Integer) instruction.operands[2].value;
                names.put((String)instruction.operands[0].value, secondOperandValue - thirdOperandValue);
            }
        };
        instructionSet.add(instruction);
        /////MUL
        //////////////Register, Register, Number
        instruction = new Instruction();
        instruction.name = "MUL";
        instruction.operands[0] = new Operand();
        instruction.operands[0].operandType = Operand.OperandType.Register;
        instruction.operands[1] = new Operand();
        instruction.operands[1].operandType = Operand.OperandType.Register;
        instruction.operands[2] = new Operand();
        instruction.operands[2].operandType = Operand.OperandType.Number;
        instruction.function = new InstructionFunction() {
            @Override
            public void doInstruction(Instruction instruction) {
                int secondOperandValue = getValueOfRegister((String)instruction.operands[1].value);
                int thirdOperandValue = (Integer) instruction.operands[2].value;
                names.put((String)instruction.operands[0].value, (int) (((long)secondOperandValue) * ((long)thirdOperandValue) & 0xffffffffL));
            }
        };
        instructionSet.add(instruction);
        /////UPM
        //////////////Register, Register, Number
        instruction = new Instruction();
        instruction.name = "UPM";
        instruction.operands[0] = new Operand();
        instruction.operands[0].operandType = Operand.OperandType.Register;
        instruction.operands[1] = new Operand();
        instruction.operands[1].operandType = Operand.OperandType.Register;
        instruction.operands[2] = new Operand();
        instruction.operands[2].operandType = Operand.OperandType.Number;
        instruction.function = new InstructionFunction() {
            @Override
            public void doInstruction(Instruction instruction) {
                int secondOperandValue = getValueOfRegister((String)instruction.operands[1].value);
                int thirdOperandValue = (Integer) instruction.operands[2].value;
                names.put((String)instruction.operands[0].value, (int) (((long)secondOperandValue) * ((long)thirdOperandValue) >>> 32L));
            }
        };
        instructionSet.add(instruction);
        /////DIV
        //////////////Register, Register, Number
        instruction = new Instruction();
        instruction.name = "DIV";
        instruction.operands[0] = new Operand();
        instruction.operands[0].operandType = Operand.OperandType.Register;
        instruction.operands[1] = new Operand();
        instruction.operands[1].operandType = Operand.OperandType.Register;
        instruction.operands[2] = new Operand();
        instruction.operands[2].operandType = Operand.OperandType.Number;
        instruction.function = new InstructionFunction() {
            @Override
            public void doInstruction(Instruction instruction) {
                int secondOperandValue = getValueOfRegister((String)instruction.operands[1].value);
                int thirdOperandValue = (Integer) instruction.operands[2].value;
                names.put((String)instruction.operands[0].value, secondOperandValue / thirdOperandValue);
            }
        };
        instructionSet.add(instruction);
        /////MOD
        //////////////Register, Register, Number
        instruction = new Instruction();
        instruction.name = "MOD";
        instruction.operands[0] = new Operand();
        instruction.operands[0].operandType = Operand.OperandType.Register;
        instruction.operands[1] = new Operand();
        instruction.operands[1].operandType = Operand.OperandType.Register;
        instruction.operands[2] = new Operand();
        instruction.operands[2].operandType = Operand.OperandType.Number;
        instruction.function = new InstructionFunction() {
            @Override
            public void doInstruction(Instruction instruction) {
                int secondOperandValue = getValueOfRegister((String)instruction.operands[1].value);
                int thirdOperandValue = (Integer) instruction.operands[2].value;
                names.put((String)instruction.operands[0].value, secondOperandValue % thirdOperandValue);
            }
        };
        instructionSet.add(instruction);
        /////AND
        //////////////Register, Register, Number
        instruction = new Instruction();
        instruction.name = "AND";
        instruction.operands[0] = new Operand();
        instruction.operands[0].operandType = Operand.OperandType.Register;
        instruction.operands[1] = new Operand();
        instruction.operands[1].operandType = Operand.OperandType.Register;
        instruction.operands[2] = new Operand();
        instruction.operands[2].operandType = Operand.OperandType.Number;
        instruction.function = new InstructionFunction() {
            @Override
            public void doInstruction(Instruction instruction) {
                int secondOperandValue = getValueOfRegister((String)instruction.operands[1].value);
                int thirdOperandValue = (Integer) instruction.operands[2].value;
                names.put((String)instruction.operands[0].value, secondOperandValue & thirdOperandValue);
            }
        };
        instructionSet.add(instruction);
        /////ORR
        //////////////Register, Register, Number
        instruction = new Instruction();
        instruction.name = "ORR";
        instruction.operands[0] = new Operand();
        instruction.operands[0].operandType = Operand.OperandType.Register;
        instruction.operands[1] = new Operand();
        instruction.operands[1].operandType = Operand.OperandType.Register;
        instruction.operands[2] = new Operand();
        instruction.operands[2].operandType = Operand.OperandType.Number;
        instruction.function = new InstructionFunction() {
            @Override
            public void doInstruction(Instruction instruction) {
                int secondOperandValue = getValueOfRegister((String)instruction.operands[1].value);
                int thirdOperandValue = (Integer) instruction.operands[2].value;
                names.put((String)instruction.operands[0].value, secondOperandValue | thirdOperandValue);
            }
        };
        instructionSet.add(instruction);
        /////XOR
        //////////////Register, Register, Number
        instruction = new Instruction();
        instruction.name = "XOR";
        instruction.operands[0] = new Operand();
        instruction.operands[0].operandType = Operand.OperandType.Register;
        instruction.operands[1] = new Operand();
        instruction.operands[1].operandType = Operand.OperandType.Register;
        instruction.operands[2] = new Operand();
        instruction.operands[2].operandType = Operand.OperandType.Number;
        instruction.function = new InstructionFunction() {
            @Override
            public void doInstruction(Instruction instruction) {
                int secondOperandValue = getValueOfRegister((String)instruction.operands[1].value);
                int thirdOperandValue = (Integer) instruction.operands[2].value;
                names.put((String)instruction.operands[0].value, secondOperandValue ^ thirdOperandValue);
            }
        };
        instructionSet.add(instruction);
        /////GRT
        //////////////Register, Register, Number
        instruction = new Instruction();
        instruction.name = "GRT";
        instruction.operands[0] = new Operand();
        instruction.operands[0].operandType = Operand.OperandType.Register;
        instruction.operands[1] = new Operand();
        instruction.operands[1].operandType = Operand.OperandType.Register;
        instruction.operands[2] = new Operand();
        instruction.operands[2].operandType = Operand.OperandType.Number;
        instruction.function = new InstructionFunction() {
            @Override
            public void doInstruction(Instruction instruction) {
                int secondOperandValue = getValueOfRegister((String)instruction.operands[1].value);
                int thirdOperandValue = (Integer) instruction.operands[2].value;
                names.put((String)instruction.operands[0].value, (secondOperandValue > thirdOperandValue) ? 1 :0);
            }
        };
        instructionSet.add(instruction);
        /////GEQ
        //////////////Register, Register, Number
        instruction = new Instruction();
        instruction.name = "GEQ";
        instruction.operands[0] = new Operand();
        instruction.operands[0].operandType = Operand.OperandType.Register;
        instruction.operands[1] = new Operand();
        instruction.operands[1].operandType = Operand.OperandType.Register;
        instruction.operands[2] = new Operand();
        instruction.operands[2].operandType = Operand.OperandType.Number;
        instruction.function = new InstructionFunction() {
            @Override
            public void doInstruction(Instruction instruction) {
                int secondOperandValue = getValueOfRegister((String)instruction.operands[1].value);
                int thirdOperandValue = (Integer) instruction.operands[2].value;
                names.put((String)instruction.operands[0].value, (secondOperandValue >= thirdOperandValue) ? 1 :0);
            }
        };
        instructionSet.add(instruction);
        /////EQL
        //////////////Register, Register, Number
        instruction = new Instruction();
        instruction.name = "EQL";
        instruction.operands[0] = new Operand();
        instruction.operands[0].operandType = Operand.OperandType.Register;
        instruction.operands[1] = new Operand();
        instruction.operands[1].operandType = Operand.OperandType.Register;
        instruction.operands[2] = new Operand();
        instruction.operands[2].operandType = Operand.OperandType.Number;
        instruction.function = new InstructionFunction() {
            @Override
            public void doInstruction(Instruction instruction) {
                int secondOperandValue = getValueOfRegister((String)instruction.operands[1].value);
                int thirdOperandValue = (Integer) instruction.operands[2].value;
                names.put((String)instruction.operands[0].value, (secondOperandValue == thirdOperandValue) ? 1 :0);
            }
        };
        instructionSet.add(instruction);
        /////LEQ
        //////////////Register, Register, Number
        instruction = new Instruction();
        instruction.name = "LEQ";
        instruction.operands[0] = new Operand();
        instruction.operands[0].operandType = Operand.OperandType.Register;
        instruction.operands[1] = new Operand();
        instruction.operands[1].operandType = Operand.OperandType.Register;
        instruction.operands[2] = new Operand();
        instruction.operands[2].operandType = Operand.OperandType.Number;
        instruction.function = new InstructionFunction() {
            @Override
            public void doInstruction(Instruction instruction) {
                int secondOperandValue = getValueOfRegister((String)instruction.operands[1].value);
                int thirdOperandValue = (Integer) instruction.operands[2].value;
                names.put((String)instruction.operands[0].value, (secondOperandValue <= thirdOperandValue) ? 1 :0);
            }
        };
        instructionSet.add(instruction);
        /////LWR
        //////////////Register, Register, Number
        instruction = new Instruction();
        instruction.name = "LWR";
        instruction.operands[0] = new Operand();
        instruction.operands[0].operandType = Operand.OperandType.Register;
        instruction.operands[1] = new Operand();
        instruction.operands[1].operandType = Operand.OperandType.Register;
        instruction.operands[2] = new Operand();
        instruction.operands[2].operandType = Operand.OperandType.Number;
        instruction.function = new InstructionFunction() {
            @Override
            public void doInstruction(Instruction instruction) {
                int secondOperandValue = getValueOfRegister((String)instruction.operands[1].value);
                int thirdOperandValue = (Integer) instruction.operands[2].value;
                names.put((String)instruction.operands[0].value, (secondOperandValue < thirdOperandValue) ? 1 :0);
            }
        };
        instructionSet.add(instruction);
        /////NEQ
        //////////////Register, Register, Number
        instruction = new Instruction();
        instruction.name = "NEQ";
        instruction.operands[0] = new Operand();
        instruction.operands[0].operandType = Operand.OperandType.Register;
        instruction.operands[1] = new Operand();
        instruction.operands[1].operandType = Operand.OperandType.Register;
        instruction.operands[2] = new Operand();
        instruction.operands[2].operandType = Operand.OperandType.Number;
        instruction.function = new InstructionFunction() {
            @Override
            public void doInstruction(Instruction instruction) {
                int secondOperandValue = getValueOfRegister((String)instruction.operands[1].value);
                int thirdOperandValue = (Integer) instruction.operands[2].value;
                names.put((String)instruction.operands[0].value, (secondOperandValue != thirdOperandValue) ? 1 :0);
            }
        };
        instructionSet.add(instruction);
        /////ADF
        //////////////Register, Register, Register
        instruction = new Instruction();
        instruction.name = "ADF";
        instruction.operands[0] = new Operand();
        instruction.operands[0].operandType = Operand.OperandType.Register;
        instruction.operands[1] = new Operand();
        instruction.operands[1].operandType = Operand.OperandType.Register;
        instruction.operands[2] = new Operand();
        instruction.operands[2].operandType = Operand.OperandType.Register;
        instruction.function = new InstructionFunction() {
            @Override
            public void doInstruction(Instruction instruction) {
                int secondOperandValue = getValueOfRegister((String)instruction.operands[1].value);
                int thirdOperandValue = getValueOfRegister((String)instruction.operands[2].value);
                names.put((String)instruction.operands[0].value,  Float.floatToRawIntBits(Float.intBitsToFloat(secondOperandValue) + Float.intBitsToFloat(thirdOperandValue)));
            }
        };
        instructionSet.add(instruction);
        /////SBF
        //////////////Register, Register, Register
        instruction = new Instruction();
        instruction.name = "SBF";
        instruction.operands[0] = new Operand();
        instruction.operands[0].operandType = Operand.OperandType.Register;
        instruction.operands[1] = new Operand();
        instruction.operands[1].operandType = Operand.OperandType.Register;
        instruction.operands[2] = new Operand();
        instruction.operands[2].operandType = Operand.OperandType.Register;
        instruction.function = new InstructionFunction() {
            @Override
            public void doInstruction(Instruction instruction) {
                int secondOperandValue = getValueOfRegister((String)instruction.operands[1].value);
                int thirdOperandValue = getValueOfRegister((String)instruction.operands[2].value);
                names.put((String)instruction.operands[0].value,  Float.floatToRawIntBits(Float.intBitsToFloat(secondOperandValue) - Float.intBitsToFloat(thirdOperandValue)));
            }
        };
        instructionSet.add(instruction);
        /////MLF
        //////////////Register, Register, Register
        instruction = new Instruction();
        instruction.name = "MLF";
        instruction.operands[0] = new Operand();
        instruction.operands[0].operandType = Operand.OperandType.Register;
        instruction.operands[1] = new Operand();
        instruction.operands[1].operandType = Operand.OperandType.Register;
        instruction.operands[2] = new Operand();
        instruction.operands[2].operandType = Operand.OperandType.Register;
        instruction.function = new InstructionFunction() {
            @Override
            public void doInstruction(Instruction instruction) {
                int secondOperandValue = getValueOfRegister((String)instruction.operands[1].value);
                int thirdOperandValue = getValueOfRegister((String)instruction.operands[2].value);
                names.put((String)instruction.operands[0].value,  Float.floatToRawIntBits(Float.intBitsToFloat(secondOperandValue) * Float.intBitsToFloat(thirdOperandValue)));
            }
        };
        instructionSet.add(instruction);
        /////DVF
        //////////////Register, Register, Register
        instruction = new Instruction();
        instruction.name = "DVF";
        instruction.operands[0] = new Operand();
        instruction.operands[0].operandType = Operand.OperandType.Register;
        instruction.operands[1] = new Operand();
        instruction.operands[1].operandType = Operand.OperandType.Register;
        instruction.operands[2] = new Operand();
        instruction.operands[2].operandType = Operand.OperandType.Register;
        instruction.function = new InstructionFunction() {
            @Override
            public void doInstruction(Instruction instruction) {
                int secondOperandValue = getValueOfRegister((String)instruction.operands[1].value);
                int thirdOperandValue = getValueOfRegister((String)instruction.operands[2].value);
                names.put((String)instruction.operands[0].value,  Float.floatToRawIntBits(Float.intBitsToFloat(secondOperandValue) / Float.intBitsToFloat(thirdOperandValue)));
            }
        };
        instructionSet.add(instruction);
        /////GRF
        //////////////Register, Register, Register
        instruction = new Instruction();
        instruction.name = "GRF";
        instruction.operands[0] = new Operand();
        instruction.operands[0].operandType = Operand.OperandType.Register;
        instruction.operands[1] = new Operand();
        instruction.operands[1].operandType = Operand.OperandType.Register;
        instruction.operands[2] = new Operand();
        instruction.operands[2].operandType = Operand.OperandType.Register;
        instruction.function = new InstructionFunction() {
            @Override
            public void doInstruction(Instruction instruction) {
                int secondOperandValue = getValueOfRegister((String)instruction.operands[1].value);
                int thirdOperandValue = getValueOfRegister((String)instruction.operands[2].value);
                names.put((String)instruction.operands[0].value, (Float.intBitsToFloat(secondOperandValue) > Float.intBitsToFloat(thirdOperandValue)) ? 1 :0);
            }
        };
        instructionSet.add(instruction);
        /////GEF
        //////////////Register, Register, Register
        instruction = new Instruction();
        instruction.name = "GEF";
        instruction.operands[0] = new Operand();
        instruction.operands[0].operandType = Operand.OperandType.Register;
        instruction.operands[1] = new Operand();
        instruction.operands[1].operandType = Operand.OperandType.Register;
        instruction.operands[2] = new Operand();
        instruction.operands[2].operandType = Operand.OperandType.Register;
        instruction.function = new InstructionFunction() {
            @Override
            public void doInstruction(Instruction instruction) {
                int secondOperandValue = getValueOfRegister((String)instruction.operands[1].value);
                int thirdOperandValue = getValueOfRegister((String)instruction.operands[2].value);
                names.put((String)instruction.operands[0].value, (Float.intBitsToFloat(secondOperandValue) >= Float.intBitsToFloat(thirdOperandValue)) ? 1 :0);
            }
        };
        instructionSet.add(instruction);
        /////EQF
        //////////////Register, Register, Register
        instruction = new Instruction();
        instruction.name = "EQF";
        instruction.operands[0] = new Operand();
        instruction.operands[0].operandType = Operand.OperandType.Register;
        instruction.operands[1] = new Operand();
        instruction.operands[1].operandType = Operand.OperandType.Register;
        instruction.operands[2] = new Operand();
        instruction.operands[2].operandType = Operand.OperandType.Register;
        instruction.function = new InstructionFunction() {
            @Override
            public void doInstruction(Instruction instruction) {
                int secondOperandValue = getValueOfRegister((String)instruction.operands[1].value);
                int thirdOperandValue = getValueOfRegister((String)instruction.operands[2].value);
                names.put((String)instruction.operands[0].value, (Float.intBitsToFloat(secondOperandValue) == Float.intBitsToFloat(thirdOperandValue)) ? 1 :0);
            }
        };
        instructionSet.add(instruction);
        /////LEF
        //////////////Register, Register, Register
        instruction = new Instruction();
        instruction.name = "LEF";
        instruction.operands[0] = new Operand();
        instruction.operands[0].operandType = Operand.OperandType.Register;
        instruction.operands[1] = new Operand();
        instruction.operands[1].operandType = Operand.OperandType.Register;
        instruction.operands[2] = new Operand();
        instruction.operands[2].operandType = Operand.OperandType.Register;
        instruction.function = new InstructionFunction() {
            @Override
            public void doInstruction(Instruction instruction) {
                int secondOperandValue = getValueOfRegister((String)instruction.operands[1].value);
                int thirdOperandValue = getValueOfRegister((String)instruction.operands[2].value);
                names.put((String)instruction.operands[0].value, (Float.intBitsToFloat(secondOperandValue) <= Float.intBitsToFloat(thirdOperandValue)) ? 1 :0);
            }
        };
        instructionSet.add(instruction);
        /////LWF
        //////////////Register, Register, Register
        instruction = new Instruction();
        instruction.name = "LWF";
        instruction.operands[0] = new Operand();
        instruction.operands[0].operandType = Operand.OperandType.Register;
        instruction.operands[1] = new Operand();
        instruction.operands[1].operandType = Operand.OperandType.Register;
        instruction.operands[2] = new Operand();
        instruction.operands[2].operandType = Operand.OperandType.Register;
        instruction.function = new InstructionFunction() {
            @Override
            public void doInstruction(Instruction instruction) {
                int secondOperandValue = getValueOfRegister((String)instruction.operands[1].value);
                int thirdOperandValue = getValueOfRegister((String)instruction.operands[2].value);
                names.put((String)instruction.operands[0].value, (Float.intBitsToFloat(secondOperandValue) < Float.intBitsToFloat(thirdOperandValue)) ? 1 :0);
            }
        };
        instructionSet.add(instruction);
        /////NEF
        //////////////Register, Register, Register
        instruction = new Instruction();
        instruction.name = "NEF";
        instruction.operands[0] = new Operand();
        instruction.operands[0].operandType = Operand.OperandType.Register;
        instruction.operands[1] = new Operand();
        instruction.operands[1].operandType = Operand.OperandType.Register;
        instruction.operands[2] = new Operand();
        instruction.operands[2].operandType = Operand.OperandType.Register;
        instruction.function = new InstructionFunction() {
            @Override
            public void doInstruction(Instruction instruction) {
                int secondOperandValue = getValueOfRegister((String)instruction.operands[1].value);
                int thirdOperandValue = getValueOfRegister((String)instruction.operands[2].value);
                names.put((String)instruction.operands[0].value, (Float.intBitsToFloat(secondOperandValue) != Float.intBitsToFloat(thirdOperandValue)) ? 1 :0);
            }
        };
        instructionSet.add(instruction);
        /////ADF
        //////////////Register, Id, Register
        instruction = new Instruction();
        instruction.name = "ADF";
        instruction.operands[0] = new Operand();
        instruction.operands[0].operandType = Operand.OperandType.Register;
        instruction.operands[1] = new Operand();
        instruction.operands[1].operandType = Operand.OperandType.Id;
        instruction.operands[2] = new Operand();
        instruction.operands[2].operandType = Operand.OperandType.Register;
        instruction.function = new InstructionFunction() {
            @Override
            public void doInstruction(Instruction instruction) {
                int secondOperandValue = getValueOfName((String)instruction.operands[1].value);
                int thirdOperandValue = getValueOfRegister((String)instruction.operands[2].value);
                names.put((String)instruction.operands[0].value,  Float.floatToRawIntBits(Float.intBitsToFloat(secondOperandValue) + Float.intBitsToFloat(thirdOperandValue)));
            }
        };
        instructionSet.add(instruction);
        /////SBF
        //////////////Register, Id, Register
        instruction = new Instruction();
        instruction.name = "SBF";
        instruction.operands[0] = new Operand();
        instruction.operands[0].operandType = Operand.OperandType.Register;
        instruction.operands[1] = new Operand();
        instruction.operands[1].operandType = Operand.OperandType.Id;
        instruction.operands[2] = new Operand();
        instruction.operands[2].operandType = Operand.OperandType.Register;
        instruction.function = new InstructionFunction() {
            @Override
            public void doInstruction(Instruction instruction) {
                int secondOperandValue = getValueOfRegister((String)instruction.operands[1].value);
                int thirdOperandValue = getValueOfRegister((String)instruction.operands[2].value);
                names.put((String)instruction.operands[0].value, Float.floatToRawIntBits(Float.intBitsToFloat(secondOperandValue)  - Float.intBitsToFloat(thirdOperandValue)));
            }
        };
        instructionSet.add(instruction);
        /////MLF
        //////////////Register, Id, Register
        instruction = new Instruction();
        instruction.name = "MLF";
        instruction.operands[0] = new Operand();
        instruction.operands[0].operandType = Operand.OperandType.Register;
        instruction.operands[1] = new Operand();
        instruction.operands[1].operandType = Operand.OperandType.Id;
        instruction.operands[2] = new Operand();
        instruction.operands[2].operandType = Operand.OperandType.Register;
        instruction.function = new InstructionFunction() {
            @Override
            public void doInstruction(Instruction instruction) {
                int secondOperandValue = getValueOfName((String)instruction.operands[1].value);
                int thirdOperandValue = getValueOfRegister((String)instruction.operands[2].value);
                names.put((String)instruction.operands[0].value, Float.floatToRawIntBits(Float.intBitsToFloat(secondOperandValue)  * Float.intBitsToFloat(thirdOperandValue)));
            }
        };
        instructionSet.add(instruction);
        /////DVF
        //////////////Register, Id, Register
        instruction = new Instruction();
        instruction.name = "DVF";
        instruction.operands[0] = new Operand();
        instruction.operands[0].operandType = Operand.OperandType.Register;
        instruction.operands[1] = new Operand();
        instruction.operands[1].operandType = Operand.OperandType.Id;
        instruction.operands[2] = new Operand();
        instruction.operands[2].operandType = Operand.OperandType.Register;
        instruction.function = new InstructionFunction() {
            @Override
            public void doInstruction(Instruction instruction) {
                int secondOperandValue = getValueOfName((String)instruction.operands[1].value);
                int thirdOperandValue = getValueOfRegister((String)instruction.operands[2].value);
                names.put((String)instruction.operands[0].value, Float.floatToRawIntBits(Float.intBitsToFloat(secondOperandValue) / Float.intBitsToFloat(thirdOperandValue)));
            }
        };
        instructionSet.add(instruction);
        /////GRF
        //////////////Register, Id, Register
        instruction = new Instruction();
        instruction.name = "GRF";
        instruction.operands[0] = new Operand();
        instruction.operands[0].operandType = Operand.OperandType.Register;
        instruction.operands[1] = new Operand();
        instruction.operands[1].operandType = Operand.OperandType.Id;
        instruction.operands[2] = new Operand();
        instruction.operands[2].operandType = Operand.OperandType.Register;
        instruction.function = new InstructionFunction() {
            @Override
            public void doInstruction(Instruction instruction) {
                int secondOperandValue = getValueOfName((String)instruction.operands[1].value);
                int thirdOperandValue = getValueOfRegister((String)instruction.operands[2].value);
                names.put((String)instruction.operands[0].value, (Float.intBitsToFloat(secondOperandValue) > Float.intBitsToFloat(thirdOperandValue)) ? 1 :0);
            }
        };
        instructionSet.add(instruction);
        /////GEF
        //////////////Register, Id, Register
        instruction = new Instruction();
        instruction.name = "GEF";
        instruction.operands[0] = new Operand();
        instruction.operands[0].operandType = Operand.OperandType.Register;
        instruction.operands[1] = new Operand();
        instruction.operands[1].operandType = Operand.OperandType.Id;
        instruction.operands[2] = new Operand();
        instruction.operands[2].operandType = Operand.OperandType.Register;
        instruction.function = new InstructionFunction() {
            @Override
            public void doInstruction(Instruction instruction) {
                int secondOperandValue = getValueOfName((String)instruction.operands[1].value);
                int thirdOperandValue = getValueOfRegister((String)instruction.operands[2].value);
                names.put((String)instruction.operands[0].value, (Float.intBitsToFloat(secondOperandValue) >= Float.intBitsToFloat(thirdOperandValue)) ? 1 :0);
            }
        };
        instructionSet.add(instruction);
        /////EQF
        //////////////Register, Id, Register
        instruction = new Instruction();
        instruction.name = "EQF";
        instruction.operands[0] = new Operand();
        instruction.operands[0].operandType = Operand.OperandType.Register;
        instruction.operands[1] = new Operand();
        instruction.operands[1].operandType = Operand.OperandType.Id;
        instruction.operands[2] = new Operand();
        instruction.operands[2].operandType = Operand.OperandType.Register;
        instruction.function = new InstructionFunction() {
            @Override
            public void doInstruction(Instruction instruction) {
                int secondOperandValue = getValueOfName((String)instruction.operands[1].value);
                int thirdOperandValue = getValueOfRegister((String)instruction.operands[2].value);
                names.put((String)instruction.operands[0].value, (Float.intBitsToFloat(secondOperandValue) == Float.intBitsToFloat(thirdOperandValue)) ? 1 :0);
            }
        };
        instructionSet.add(instruction);
        /////LEF
        //////////////Register, Id, Register
        instruction = new Instruction();
        instruction.name = "LEF";
        instruction.operands[0] = new Operand();
        instruction.operands[0].operandType = Operand.OperandType.Register;
        instruction.operands[1] = new Operand();
        instruction.operands[1].operandType = Operand.OperandType.Id;
        instruction.operands[2] = new Operand();
        instruction.operands[2].operandType = Operand.OperandType.Register;
        instruction.function = new InstructionFunction() {
            @Override
            public void doInstruction(Instruction instruction) {
                int secondOperandValue = getValueOfName((String)instruction.operands[1].value);
                int thirdOperandValue = getValueOfRegister((String)instruction.operands[2].value);
                names.put((String)instruction.operands[0].value, (Float.intBitsToFloat(secondOperandValue) <= Float.intBitsToFloat(thirdOperandValue)) ? 1 :0);
            }
        };
        instructionSet.add(instruction);
        /////LWF
        //////////////Register, Id, Register
        instruction = new Instruction();
        instruction.name = "LWF";
        instruction.operands[0] = new Operand();
        instruction.operands[0].operandType = Operand.OperandType.Register;
        instruction.operands[1] = new Operand();
        instruction.operands[1].operandType = Operand.OperandType.Id;
        instruction.operands[2] = new Operand();
        instruction.operands[2].operandType = Operand.OperandType.Register;
        instruction.function = new InstructionFunction() {
            @Override
            public void doInstruction(Instruction instruction) {
                int secondOperandValue = getValueOfName((String)instruction.operands[1].value);
                int thirdOperandValue = getValueOfRegister((String)instruction.operands[2].value);
                names.put((String)instruction.operands[0].value, (Float.intBitsToFloat(secondOperandValue) < Float.intBitsToFloat(thirdOperandValue)) ? 1 :0);
            }
        };
        instructionSet.add(instruction);
        /////NEF
        //////////////Register, Id, Register
        instruction = new Instruction();
        instruction.name = "NEF";
        instruction.operands[0] = new Operand();
        instruction.operands[0].operandType = Operand.OperandType.Register;
        instruction.operands[1] = new Operand();
        instruction.operands[1].operandType = Operand.OperandType.Id;
        instruction.operands[2] = new Operand();
        instruction.operands[2].operandType = Operand.OperandType.Register;
        instruction.function = new InstructionFunction() {
            @Override
            public void doInstruction(Instruction instruction) {
                int secondOperandValue = getValueOfName((String)instruction.operands[1].value);
                int thirdOperandValue = getValueOfRegister((String)instruction.operands[2].value);
                names.put((String)instruction.operands[0].value, (Float.intBitsToFloat(secondOperandValue) != Float.intBitsToFloat(thirdOperandValue)) ? 1 :0);
            }
        };
        instructionSet.add(instruction);
        /////ADF
        //////////////Register, Register, Id
        instruction = new Instruction();
        instruction.name = "ADF";
        instruction.operands[0] = new Operand();
        instruction.operands[0].operandType = Operand.OperandType.Register;
        instruction.operands[1] = new Operand();
        instruction.operands[1].operandType = Operand.OperandType.Register;
        instruction.operands[2] = new Operand();
        instruction.operands[2].operandType = Operand.OperandType.Id;
        instruction.function = new InstructionFunction() {
            @Override
            public void doInstruction(Instruction instruction) {
                int secondOperandValue = getValueOfRegister((String)instruction.operands[1].value);
                int thirdOperandValue = getValueOfName((String)instruction.operands[2].value);
                names.put((String)instruction.operands[0].value, Float.floatToRawIntBits(Float.intBitsToFloat(secondOperandValue) + Float.intBitsToFloat(thirdOperandValue)));
            }
        };
        instructionSet.add(instruction);
        /////SBF
        //////////////Register, Register, Id
        instruction = new Instruction();
        instruction.name = "SBF";
        instruction.operands[0] = new Operand();
        instruction.operands[0].operandType = Operand.OperandType.Register;
        instruction.operands[1] = new Operand();
        instruction.operands[1].operandType = Operand.OperandType.Register;
        instruction.operands[2] = new Operand();
        instruction.operands[2].operandType = Operand.OperandType.Id;
        instruction.function = new InstructionFunction() {
            @Override
            public void doInstruction(Instruction instruction) {
                int secondOperandValue = getValueOfRegister((String)instruction.operands[1].value);
                int thirdOperandValue = getValueOfName((String)instruction.operands[2].value);
                names.put((String)instruction.operands[0].value, Float.floatToRawIntBits(Float.intBitsToFloat(secondOperandValue) - Float.intBitsToFloat(thirdOperandValue)));
            }
        };
        instructionSet.add(instruction);
        /////MLF
        //////////////Register, Register, Id
        instruction = new Instruction();
        instruction.name = "MLF";
        instruction.operands[0] = new Operand();
        instruction.operands[0].operandType = Operand.OperandType.Register;
        instruction.operands[1] = new Operand();
        instruction.operands[1].operandType = Operand.OperandType.Register;
        instruction.operands[2] = new Operand();
        instruction.operands[2].operandType = Operand.OperandType.Id;
        instruction.function = new InstructionFunction() {
            @Override
            public void doInstruction(Instruction instruction) {
                int secondOperandValue = getValueOfRegister((String)instruction.operands[1].value);
                int thirdOperandValue = getValueOfName((String)instruction.operands[2].value);
                names.put((String)instruction.operands[0].value, Float.floatToRawIntBits(Float.intBitsToFloat(secondOperandValue) * Float.intBitsToFloat(thirdOperandValue)));
            }
        };
        instructionSet.add(instruction);
        /////DVF
        //////////////Register, Register, Id
        instruction = new Instruction();
        instruction.name = "DVF";
        instruction.operands[0] = new Operand();
        instruction.operands[0].operandType = Operand.OperandType.Register;
        instruction.operands[1] = new Operand();
        instruction.operands[1].operandType = Operand.OperandType.Register;
        instruction.operands[2] = new Operand();
        instruction.operands[2].operandType = Operand.OperandType.Id;
        instruction.function = new InstructionFunction() {
            @Override
            public void doInstruction(Instruction instruction) {
                int secondOperandValue = getValueOfRegister((String)instruction.operands[1].value);
                int thirdOperandValue = getValueOfName((String)instruction.operands[2].value);
                names.put((String)instruction.operands[0].value, Float.floatToRawIntBits(Float.intBitsToFloat(secondOperandValue) / Float.intBitsToFloat(thirdOperandValue)));
            }
        };
        instructionSet.add(instruction);
        /////GRF
        //////////////Register, Register, Id
        instruction = new Instruction();
        instruction.name = "GRF";
        instruction.operands[0] = new Operand();
        instruction.operands[0].operandType = Operand.OperandType.Register;
        instruction.operands[1] = new Operand();
        instruction.operands[1].operandType = Operand.OperandType.Register;
        instruction.operands[2] = new Operand();
        instruction.operands[2].operandType = Operand.OperandType.Id;
        instruction.function = new InstructionFunction() {
            @Override
            public void doInstruction(Instruction instruction) {
                int secondOperandValue = getValueOfRegister((String)instruction.operands[1].value);
                int thirdOperandValue = getValueOfName((String)instruction.operands[2].value);
                names.put((String)instruction.operands[0].value, (Float.intBitsToFloat(secondOperandValue) > Float.intBitsToFloat(thirdOperandValue)) ? 1 :0);
            }
        };
        instructionSet.add(instruction);
        /////GEF
        //////////////Register, Register, Id
        instruction = new Instruction();
        instruction.name = "GEF";
        instruction.operands[0] = new Operand();
        instruction.operands[0].operandType = Operand.OperandType.Register;
        instruction.operands[1] = new Operand();
        instruction.operands[1].operandType = Operand.OperandType.Register;
        instruction.operands[2] = new Operand();
        instruction.operands[2].operandType = Operand.OperandType.Id;
        instruction.function = new InstructionFunction() {
            @Override
            public void doInstruction(Instruction instruction) {
                int secondOperandValue = getValueOfRegister((String)instruction.operands[1].value);
                int thirdOperandValue = getValueOfName((String)instruction.operands[2].value);
                names.put((String)instruction.operands[0].value, (Float.intBitsToFloat(secondOperandValue) >= Float.intBitsToFloat(thirdOperandValue)) ? 1 :0);
            }
        };
        instructionSet.add(instruction);
        /////EQF
        //////////////Register, Register, Id
        instruction = new Instruction();
        instruction.name = "EQF";
        instruction.operands[0] = new Operand();
        instruction.operands[0].operandType = Operand.OperandType.Register;
        instruction.operands[1] = new Operand();
        instruction.operands[1].operandType = Operand.OperandType.Register;
        instruction.operands[2] = new Operand();
        instruction.operands[2].operandType = Operand.OperandType.Id;
        instruction.function = new InstructionFunction() {
            @Override
            public void doInstruction(Instruction instruction) {
                int secondOperandValue = getValueOfRegister((String)instruction.operands[1].value);
                int thirdOperandValue = getValueOfName((String)instruction.operands[2].value);
                names.put((String)instruction.operands[0].value, (Float.intBitsToFloat(secondOperandValue) == Float.intBitsToFloat(thirdOperandValue)) ? 1 :0);
            }
        };
        instructionSet.add(instruction);
        /////LEF
        //////////////Register, Register, Id
        instruction = new Instruction();
        instruction.name = "LEF";
        instruction.operands[0] = new Operand();
        instruction.operands[0].operandType = Operand.OperandType.Register;
        instruction.operands[1] = new Operand();
        instruction.operands[1].operandType = Operand.OperandType.Register;
        instruction.operands[2] = new Operand();
        instruction.operands[2].operandType = Operand.OperandType.Id;
        instruction.function = new InstructionFunction() {
            @Override
            public void doInstruction(Instruction instruction) {
                int secondOperandValue = getValueOfRegister((String)instruction.operands[1].value);
                int thirdOperandValue = getValueOfName((String)instruction.operands[2].value);
                names.put((String)instruction.operands[0].value, (Float.intBitsToFloat(secondOperandValue) <= Float.intBitsToFloat(thirdOperandValue)) ? 1 :0);
            }
        };
        instructionSet.add(instruction);
        /////LWR
        //////////////Register, Register, Id
        instruction = new Instruction();
        instruction.name = "LWR";
        instruction.operands[0] = new Operand();
        instruction.operands[0].operandType = Operand.OperandType.Register;
        instruction.operands[1] = new Operand();
        instruction.operands[1].operandType = Operand.OperandType.Register;
        instruction.operands[2] = new Operand();
        instruction.operands[2].operandType = Operand.OperandType.Id;
        instruction.function = new InstructionFunction() {
            @Override
            public void doInstruction(Instruction instruction) {
                int secondOperandValue = getValueOfRegister((String)instruction.operands[1].value);
                int thirdOperandValue = getValueOfName((String)instruction.operands[2].value);
                names.put((String)instruction.operands[0].value, (Float.intBitsToFloat(secondOperandValue) < Float.intBitsToFloat(thirdOperandValue)) ? 1 :0);
            }
        };
        instructionSet.add(instruction);
        /////NEF
        //////////////Register, Register, Id
        instruction = new Instruction();
        instruction.name = "NEF";
        instruction.operands[0] = new Operand();
        instruction.operands[0].operandType = Operand.OperandType.Register;
        instruction.operands[1] = new Operand();
        instruction.operands[1].operandType = Operand.OperandType.Register;
        instruction.operands[2] = new Operand();
        instruction.operands[2].operandType = Operand.OperandType.Id;
        instruction.function = new InstructionFunction() {
            @Override
            public void doInstruction(Instruction instruction) {
                int secondOperandValue = getValueOfRegister((String)instruction.operands[1].value);
                int thirdOperandValue = getValueOfName((String)instruction.operands[2].value);
                names.put((String)instruction.operands[0].value, (Float.intBitsToFloat(secondOperandValue) != Float.intBitsToFloat(thirdOperandValue)) ? 1 :0);
            }
        };
        instructionSet.add(instruction);
        /////ADF
        //////////////Register, Number, Register
        instruction = new Instruction();
        instruction.name = "ADF";
        instruction.operands[0] = new Operand();
        instruction.operands[0].operandType = Operand.OperandType.Register;
        instruction.operands[1] = new Operand();
        instruction.operands[1].operandType = Operand.OperandType.Number;
        instruction.operands[2] = new Operand();
        instruction.operands[2].operandType = Operand.OperandType.Register;
        instruction.function = new InstructionFunction() {
            @Override
            public void doInstruction(Instruction instruction) {
                int secondOperandValue = (Integer) instruction.operands[0].value;
                int thirdOperandValue = getValueOfRegister((String)instruction.operands[2].value);
                names.put((String)instruction.operands[0].value, Float.floatToRawIntBits(Float.intBitsToFloat(secondOperandValue) + Float.intBitsToFloat(thirdOperandValue)));
            }
        };
        instructionSet.add(instruction);
        /////SBF
        //////////////Register, Number, Register
        instruction = new Instruction();
        instruction.name = "SBF";
        instruction.operands[0] = new Operand();
        instruction.operands[0].operandType = Operand.OperandType.Register;
        instruction.operands[1] = new Operand();
        instruction.operands[1].operandType = Operand.OperandType.Number;
        instruction.operands[2] = new Operand();
        instruction.operands[2].operandType = Operand.OperandType.Register;
        instruction.function = new InstructionFunction() {
            @Override
            public void doInstruction(Instruction instruction) {
                int secondOperandValue = (Integer) instruction.operands[0].value;
                int thirdOperandValue = getValueOfRegister((String)instruction.operands[2].value);
                names.put((String)instruction.operands[0].value, Float.floatToRawIntBits(Float.intBitsToFloat(secondOperandValue) - Float.intBitsToFloat(thirdOperandValue)));
            }
        };
        instructionSet.add(instruction);
        /////MLF
        //////////////Register, Number, Register
        instruction = new Instruction();
        instruction.name = "MLF";
        instruction.operands[0] = new Operand();
        instruction.operands[0].operandType = Operand.OperandType.Register;
        instruction.operands[1] = new Operand();
        instruction.operands[1].operandType = Operand.OperandType.Number;
        instruction.operands[2] = new Operand();
        instruction.operands[2].operandType = Operand.OperandType.Register;
        instruction.function = new InstructionFunction() {
            @Override
            public void doInstruction(Instruction instruction) {
                int secondOperandValue = (Integer) instruction.operands[0].value;
                int thirdOperandValue = getValueOfRegister((String)instruction.operands[2].value);
                names.put((String)instruction.operands[0].value, Float.floatToRawIntBits(Float.intBitsToFloat(secondOperandValue) * Float.intBitsToFloat(thirdOperandValue)));
            }
        };
        instructionSet.add(instruction);
        /////DVF
        //////////////Register, Number, Register
        instruction = new Instruction();
        instruction.name = "DVF";
        instruction.operands[0] = new Operand();
        instruction.operands[0].operandType = Operand.OperandType.Register;
        instruction.operands[1] = new Operand();
        instruction.operands[1].operandType = Operand.OperandType.Number;
        instruction.operands[2] = new Operand();
        instruction.operands[2].operandType = Operand.OperandType.Register;
        instruction.function = new InstructionFunction() {
            @Override
            public void doInstruction(Instruction instruction) {
                int secondOperandValue = (Integer) instruction.operands[0].value;
                int thirdOperandValue = getValueOfRegister((String)instruction.operands[2].value);
                names.put((String)instruction.operands[0].value, Float.floatToRawIntBits(Float.intBitsToFloat(secondOperandValue) / Float.intBitsToFloat(thirdOperandValue)));
            }
        };
        instructionSet.add(instruction);
        /////GRF
        //////////////Register, Number, Register
        instruction = new Instruction();
        instruction.name = "GRF";
        instruction.operands[0] = new Operand();
        instruction.operands[0].operandType = Operand.OperandType.Register;
        instruction.operands[1] = new Operand();
        instruction.operands[1].operandType = Operand.OperandType.Number;
        instruction.operands[2] = new Operand();
        instruction.operands[2].operandType = Operand.OperandType.Register;
        instruction.function = new InstructionFunction() {
            @Override
            public void doInstruction(Instruction instruction) {
                int secondOperandValue = (Integer) instruction.operands[0].value;
                int thirdOperandValue = getValueOfRegister((String)instruction.operands[2].value);
                names.put((String)instruction.operands[0].value, (Float.intBitsToFloat(secondOperandValue) > Float.intBitsToFloat(thirdOperandValue)) ? 1 :0);
            }
        };
        instructionSet.add(instruction);
        /////GEF
        //////////////Register, Number, Register
        instruction = new Instruction();
        instruction.name = "GEF";
        instruction.operands[0] = new Operand();
        instruction.operands[0].operandType = Operand.OperandType.Register;
        instruction.operands[1] = new Operand();
        instruction.operands[1].operandType = Operand.OperandType.Number;
        instruction.operands[2] = new Operand();
        instruction.operands[2].operandType = Operand.OperandType.Register;
        instruction.function = new InstructionFunction() {
            @Override
            public void doInstruction(Instruction instruction) {
                int secondOperandValue = (Integer) instruction.operands[0].value;
                int thirdOperandValue = getValueOfRegister((String)instruction.operands[2].value);
                names.put((String)instruction.operands[0].value, (Float.intBitsToFloat(secondOperandValue) >= Float.intBitsToFloat(thirdOperandValue)) ? 1 :0);
            }
        };
        instructionSet.add(instruction);
        /////EQF
        //////////////Register, Number, Register
        instruction = new Instruction();
        instruction.name = "EQF";
        instruction.operands[0] = new Operand();
        instruction.operands[0].operandType = Operand.OperandType.Register;
        instruction.operands[1] = new Operand();
        instruction.operands[1].operandType = Operand.OperandType.Number;
        instruction.operands[2] = new Operand();
        instruction.operands[2].operandType = Operand.OperandType.Register;
        instruction.function = new InstructionFunction() {
            @Override
            public void doInstruction(Instruction instruction) {
                int secondOperandValue = (Integer) instruction.operands[0].value;
                int thirdOperandValue = getValueOfRegister((String)instruction.operands[2].value);
                names.put((String)instruction.operands[0].value, (Float.intBitsToFloat(secondOperandValue) == Float.intBitsToFloat(thirdOperandValue)) ? 1 :0);
            }
        };
        instructionSet.add(instruction);
        /////LEF
        //////////////Register, Number, Register
        instruction = new Instruction();
        instruction.name = "LEF";
        instruction.operands[0] = new Operand();
        instruction.operands[0].operandType = Operand.OperandType.Register;
        instruction.operands[1] = new Operand();
        instruction.operands[1].operandType = Operand.OperandType.Number;
        instruction.operands[2] = new Operand();
        instruction.operands[2].operandType = Operand.OperandType.Register;
        instruction.function = new InstructionFunction() {
            @Override
            public void doInstruction(Instruction instruction) {
                int secondOperandValue = (Integer) instruction.operands[0].value;
                int thirdOperandValue = getValueOfRegister((String)instruction.operands[2].value);
                names.put((String)instruction.operands[0].value, (Float.intBitsToFloat(secondOperandValue) <= Float.intBitsToFloat(thirdOperandValue)) ? 1 :0);
            }
        };
        instructionSet.add(instruction);
        /////LEF
        //////////////Register, Number, Register
        instruction = new Instruction();
        instruction.name = "LEF";
        instruction.operands[0] = new Operand();
        instruction.operands[0].operandType = Operand.OperandType.Register;
        instruction.operands[1] = new Operand();
        instruction.operands[1].operandType = Operand.OperandType.Number;
        instruction.operands[2] = new Operand();
        instruction.operands[2].operandType = Operand.OperandType.Register;
        instruction.function = new InstructionFunction() {
            @Override
            public void doInstruction(Instruction instruction) {
                int secondOperandValue = (Integer) instruction.operands[0].value;
                int thirdOperandValue = getValueOfRegister((String)instruction.operands[2].value);
                names.put((String)instruction.operands[0].value, (Float.intBitsToFloat(secondOperandValue) < Float.intBitsToFloat(thirdOperandValue)) ? 1 :0);
            }
        };
        instructionSet.add(instruction);
        /////NEF
        //////////////Register, Number, Register
        instruction = new Instruction();
        instruction.name = "NEF";
        instruction.operands[0] = new Operand();
        instruction.operands[0].operandType = Operand.OperandType.Register;
        instruction.operands[1] = new Operand();
        instruction.operands[1].operandType = Operand.OperandType.Number;
        instruction.operands[2] = new Operand();
        instruction.operands[2].operandType = Operand.OperandType.Register;
        instruction.function = new InstructionFunction() {
            @Override
            public void doInstruction(Instruction instruction) {
                int secondOperandValue = (Integer) instruction.operands[0].value;
                int thirdOperandValue = getValueOfRegister((String)instruction.operands[2].value);
                names.put((String)instruction.operands[0].value, (Float.intBitsToFloat(secondOperandValue) != Float.intBitsToFloat(thirdOperandValue)) ? 1 :0);
            }
        };
        instructionSet.add(instruction);
        /////ADF
        //////////////Register, Register, Number
        instruction = new Instruction();
        instruction.name = "ADF";
        instruction.operands[0] = new Operand();
        instruction.operands[0].operandType = Operand.OperandType.Register;
        instruction.operands[1] = new Operand();
        instruction.operands[1].operandType = Operand.OperandType.Register;
        instruction.operands[2] = new Operand();
        instruction.operands[2].operandType = Operand.OperandType.Number;
        instruction.function = new InstructionFunction() {
            @Override
            public void doInstruction(Instruction instruction) {
                int secondOperandValue = getValueOfRegister((String)instruction.operands[1].value);
                int thirdOperandValue = (Integer) instruction.operands[2].value;
                names.put((String)instruction.operands[0].value, Float.floatToRawIntBits(Float.intBitsToFloat(secondOperandValue) + Float.intBitsToFloat(thirdOperandValue)));
            }
        };
        instructionSet.add(instruction);
        /////SBF
        //////////////Register, Register, Number
        instruction = new Instruction();
        instruction.name = "SBF";
        instruction.operands[0] = new Operand();
        instruction.operands[0].operandType = Operand.OperandType.Register;
        instruction.operands[1] = new Operand();
        instruction.operands[1].operandType = Operand.OperandType.Register;
        instruction.operands[2] = new Operand();
        instruction.operands[2].operandType = Operand.OperandType.Number;
        instruction.function = new InstructionFunction() {
            @Override
            public void doInstruction(Instruction instruction) {
                int secondOperandValue = getValueOfRegister((String)instruction.operands[1].value);
                int thirdOperandValue = (Integer) instruction.operands[2].value;
                names.put((String)instruction.operands[0].value, Float.floatToRawIntBits(Float.intBitsToFloat(secondOperandValue) - Float.intBitsToFloat(thirdOperandValue)));
            }
        };
        instructionSet.add(instruction);
        /////MLF
        //////////////Register, Register, Number
        instruction = new Instruction();
        instruction.name = "MLF";
        instruction.operands[0] = new Operand();
        instruction.operands[0].operandType = Operand.OperandType.Register;
        instruction.operands[1] = new Operand();
        instruction.operands[1].operandType = Operand.OperandType.Register;
        instruction.operands[2] = new Operand();
        instruction.operands[2].operandType = Operand.OperandType.Number;
        instruction.function = new InstructionFunction() {
            @Override
            public void doInstruction(Instruction instruction) {
                int secondOperandValue = getValueOfRegister((String)instruction.operands[1].value);
                int thirdOperandValue = (Integer) instruction.operands[2].value;
                names.put((String)instruction.operands[0].value, Float.floatToRawIntBits(Float.intBitsToFloat(secondOperandValue) * Float.intBitsToFloat(thirdOperandValue)));
            }
        };
        instructionSet.add(instruction);
        /////DVF
        //////////////Register, Register, Number
        instruction = new Instruction();
        instruction.name = "DVF";
        instruction.operands[0] = new Operand();
        instruction.operands[0].operandType = Operand.OperandType.Register;
        instruction.operands[1] = new Operand();
        instruction.operands[1].operandType = Operand.OperandType.Register;
        instruction.operands[2] = new Operand();
        instruction.operands[2].operandType = Operand.OperandType.Number;
        instruction.function = new InstructionFunction() {
            @Override
            public void doInstruction(Instruction instruction) {
                int secondOperandValue = getValueOfRegister((String)instruction.operands[1].value);
                int thirdOperandValue = (Integer) instruction.operands[2].value;
                names.put((String)instruction.operands[0].value, Float.floatToRawIntBits(Float.intBitsToFloat(secondOperandValue) / Float.intBitsToFloat(thirdOperandValue)));
            }
        };
        instructionSet.add(instruction);
        /////GRF
        //////////////Register, Register, Number
        instruction = new Instruction();
        instruction.name = "GRF";
        instruction.operands[0] = new Operand();
        instruction.operands[0].operandType = Operand.OperandType.Register;
        instruction.operands[1] = new Operand();
        instruction.operands[1].operandType = Operand.OperandType.Register;
        instruction.operands[2] = new Operand();
        instruction.operands[2].operandType = Operand.OperandType.Number;
        instruction.function = new InstructionFunction() {
            @Override
            public void doInstruction(Instruction instruction) {
                int secondOperandValue = getValueOfRegister((String)instruction.operands[1].value);
                int thirdOperandValue = (Integer) instruction.operands[2].value;
                names.put((String)instruction.operands[0].value, (Float.intBitsToFloat(secondOperandValue) > Float.intBitsToFloat(thirdOperandValue)) ? 1 :0);
            }
        };
        instructionSet.add(instruction);
        /////GEF
        //////////////Register, Register, Number
        instruction = new Instruction();
        instruction.name = "GEF";
        instruction.operands[0] = new Operand();
        instruction.operands[0].operandType = Operand.OperandType.Register;
        instruction.operands[1] = new Operand();
        instruction.operands[1].operandType = Operand.OperandType.Register;
        instruction.operands[2] = new Operand();
        instruction.operands[2].operandType = Operand.OperandType.Number;
        instruction.function = new InstructionFunction() {
            @Override
            public void doInstruction(Instruction instruction) {
                int secondOperandValue = getValueOfRegister((String)instruction.operands[1].value);
                int thirdOperandValue = (Integer) instruction.operands[2].value;
                names.put((String)instruction.operands[0].value, (Float.intBitsToFloat(secondOperandValue) >= Float.intBitsToFloat(thirdOperandValue)) ? 1 :0);
            }
        };
        instructionSet.add(instruction);
        /////EQF
        //////////////Register, Register, Number
        instruction = new Instruction();
        instruction.name = "EQF";
        instruction.operands[0] = new Operand();
        instruction.operands[0].operandType = Operand.OperandType.Register;
        instruction.operands[1] = new Operand();
        instruction.operands[1].operandType = Operand.OperandType.Register;
        instruction.operands[2] = new Operand();
        instruction.operands[2].operandType = Operand.OperandType.Number;
        instruction.function = new InstructionFunction() {
            @Override
            public void doInstruction(Instruction instruction) {
                int secondOperandValue = getValueOfRegister((String)instruction.operands[1].value);
                int thirdOperandValue = (Integer) instruction.operands[2].value;
                names.put((String)instruction.operands[0].value, (Float.intBitsToFloat(secondOperandValue) == Float.intBitsToFloat(thirdOperandValue)) ? 1 :0);
            }
        };
        instructionSet.add(instruction);
        /////LEF
        //////////////Register, Register, Number
        instruction = new Instruction();
        instruction.name = "LEF";
        instruction.operands[0] = new Operand();
        instruction.operands[0].operandType = Operand.OperandType.Register;
        instruction.operands[1] = new Operand();
        instruction.operands[1].operandType = Operand.OperandType.Register;
        instruction.operands[2] = new Operand();
        instruction.operands[2].operandType = Operand.OperandType.Number;
        instruction.function = new InstructionFunction() {
            @Override
            public void doInstruction(Instruction instruction) {
                int secondOperandValue = getValueOfRegister((String)instruction.operands[1].value);
                int thirdOperandValue = (Integer) instruction.operands[2].value;
                names.put((String)instruction.operands[0].value, (Float.intBitsToFloat(secondOperandValue) <= Float.intBitsToFloat(thirdOperandValue)) ? 1 :0);
            }
        };
        instructionSet.add(instruction);
        /////LEF
        //////////////Register, Register, Number
        instruction = new Instruction();
        instruction.name = "LEF";
        instruction.operands[0] = new Operand();
        instruction.operands[0].operandType = Operand.OperandType.Register;
        instruction.operands[1] = new Operand();
        instruction.operands[1].operandType = Operand.OperandType.Register;
        instruction.operands[2] = new Operand();
        instruction.operands[2].operandType = Operand.OperandType.Number;
        instruction.function = new InstructionFunction() {
            @Override
            public void doInstruction(Instruction instruction) {
                int secondOperandValue = getValueOfRegister((String)instruction.operands[1].value);
                int thirdOperandValue = (Integer) instruction.operands[2].value;
                names.put((String)instruction.operands[0].value, (Float.intBitsToFloat(secondOperandValue) < Float.intBitsToFloat(thirdOperandValue)) ? 1 :0);
            }
        };
        instructionSet.add(instruction);
        /////NEF
        //////////////Register, Register, Number
        instruction = new Instruction();
        instruction.name = "NEF";
        instruction.operands[0] = new Operand();
        instruction.operands[0].operandType = Operand.OperandType.Register;
        instruction.operands[1] = new Operand();
        instruction.operands[1].operandType = Operand.OperandType.Register;
        instruction.operands[2] = new Operand();
        instruction.operands[2].operandType = Operand.OperandType.Number;
        instruction.function = new InstructionFunction() {
            @Override
            public void doInstruction(Instruction instruction) {
                int secondOperandValue = getValueOfRegister((String)instruction.operands[1].value);
                int thirdOperandValue = (Integer) instruction.operands[2].value;
                names.put((String)instruction.operands[0].value, (Float.intBitsToFloat(secondOperandValue) != Float.intBitsToFloat(thirdOperandValue)) ? 1 :0);
            }
        };
        instructionSet.add(instruction);
        /////ITF
        //////////////Register, Register, Number
        instruction = new Instruction();
        instruction.name = "ITF";
        instruction.operands[0] = new Operand();
        instruction.operands[0].operandType = Operand.OperandType.Register;
        instruction.operands[1] = new Operand();
        instruction.operands[1].operandType = Operand.OperandType.Register;
        instruction.operands[2] = new Operand();
        instruction.operands[2].operandType = Operand.OperandType.Number;
        instruction.function = new InstructionFunction() {
            @Override
            public void doInstruction(Instruction instruction) {
                int secondOperandValue = getValueOfRegister((String)instruction.operands[1].value);
                int thirdOperandValue = (Integer) instruction.operands[2].value;
                names.put((String)instruction.operands[0].value, Float.floatToRawIntBits(secondOperandValue & generateByteMask(thirdOperandValue)));
            }
        };
        instructionSet.add(instruction);
        /////FTI
        //////////////Register, Register
        instruction = new Instruction();
        instruction.name = "FTI";
        instruction.operands[0] = new Operand();
        instruction.operands[0].operandType = Operand.OperandType.Register;
        instruction.operands[1] = new Operand();
        instruction.operands[1].operandType = Operand.OperandType.Register;
        instruction.function = new InstructionFunction() {
            @Override
            public void doInstruction(Instruction instruction) {
                int secondOperandValue = getValueOfRegister((String)instruction.operands[1].value);
                names.put((String)instruction.operands[0].value, (int)Float.intBitsToFloat(secondOperandValue));
            }
        };
        instructionSet.add(instruction);
        /////LSL
        //////////////Register, Register, Number
        instruction = new Instruction();
        instruction.name = "LSL";
        instruction.operands[0] = new Operand();
        instruction.operands[0].operandType = Operand.OperandType.Register;
        instruction.operands[1] = new Operand();
        instruction.operands[1].operandType = Operand.OperandType.Register;
        instruction.operands[2] = new Operand();
        instruction.operands[2].operandType = Operand.OperandType.Number;
        instruction.function = new InstructionFunction() {
            @Override
            public void doInstruction(Instruction instruction) {
                int firstOperandValue = getValueOfRegister((String)instruction.operands[0].value);
                int secondOperandValue = getValueOfRegister((String)instruction.operands[1].value);
                int thirdOperandValue = (Integer) instruction.operands[2].value;
                int shiftedValue = (firstOperandValue & generateByteMask(thirdOperandValue))<<secondOperandValue;
                names.put((String)instruction.operands[0].value, maskBlitNumbers(shiftedValue, firstOperandValue, generateByteMask(thirdOperandValue)));
            }
        };
        instructionSet.add(instruction);
        /////LSR
        //////////////Register, Register, Number
        instruction = new Instruction();
        instruction.name = "LSR";
        instruction.operands[0] = new Operand();
        instruction.operands[0].operandType = Operand.OperandType.Register;
        instruction.operands[1] = new Operand();
        instruction.operands[1].operandType = Operand.OperandType.Register;
        instruction.operands[2] = new Operand();
        instruction.operands[2].operandType = Operand.OperandType.Number;
        instruction.function = new InstructionFunction() {
            @Override
            public void doInstruction(Instruction instruction) {
                int firstOperandValue = getValueOfRegister((String)instruction.operands[0].value);
                int secondOperandValue = getValueOfRegister((String)instruction.operands[1].value);
                int thirdOperandValue = (Integer) instruction.operands[2].value;
                int shiftedValue = (firstOperandValue & generateByteMask(thirdOperandValue))>>>secondOperandValue;
                names.put((String)instruction.operands[0].value, maskBlitNumbers(shiftedValue, firstOperandValue, generateByteMask(thirdOperandValue)));
            }
        };
        instructionSet.add(instruction);
        /////ASL
        //////////////Register, Register, Number
        instruction = new Instruction();
        instruction.name = "ASL";
        instruction.operands[0] = new Operand();
        instruction.operands[0].operandType = Operand.OperandType.Register;
        instruction.operands[1] = new Operand();
        instruction.operands[1].operandType = Operand.OperandType.Register;
        instruction.operands[2] = new Operand();
        instruction.operands[2].operandType = Operand.OperandType.Number;
        instruction.function = new InstructionFunction() {
            @Override
            public void doInstruction(Instruction instruction) {
                int firstOperandValue = getValueOfRegister((String)instruction.operands[0].value);
                int secondOperandValue = getValueOfRegister((String)instruction.operands[1].value);
                int thirdOperandValue = (Integer) instruction.operands[2].value;
                int shiftedValue = (firstOperandValue & generateByteMask(thirdOperandValue))<<secondOperandValue;
                names.put((String)instruction.operands[0].value, maskBlitNumbers(shiftedValue, firstOperandValue, generateByteMask(thirdOperandValue)));
            }
        };
        instructionSet.add(instruction);
        /////ASR
        //////////////Register, Register, Number
        instruction = new Instruction();
        instruction.name = "ASR";
        instruction.operands[0] = new Operand();
        instruction.operands[0].operandType = Operand.OperandType.Register;
        instruction.operands[1] = new Operand();
        instruction.operands[1].operandType = Operand.OperandType.Register;
        instruction.operands[2] = new Operand();
        instruction.operands[2].operandType = Operand.OperandType.Number;
        instruction.function = new InstructionFunction() {
            @Override
            public void doInstruction(Instruction instruction) {
                int firstOperandValue = getValueOfRegister((String)instruction.operands[0].value);
                int secondOperandValue = getValueOfRegister((String)instruction.operands[1].value);
                int thirdOperandValue = (Integer) instruction.operands[2].value;
                int shiftedValue = (firstOperandValue & generateByteMask(thirdOperandValue))>>secondOperandValue;
                names.put((String)instruction.operands[0].value, maskBlitNumbers(shiftedValue, firstOperandValue, generateByteMask(thirdOperandValue)));
            }
        };
        instructionSet.add(instruction);
        /////LSL
        //////////////Register, Id, Number
        instruction = new Instruction();
        instruction.name = "LSL";
        instruction.operands[0] = new Operand();
        instruction.operands[0].operandType = Operand.OperandType.Register;
        instruction.operands[1] = new Operand();
        instruction.operands[1].operandType = Operand.OperandType.Id;
        instruction.operands[2] = new Operand();
        instruction.operands[2].operandType = Operand.OperandType.Number;
        instruction.function = new InstructionFunction() {
            @Override
            public void doInstruction(Instruction instruction) {
                int firstOperandValue = getValueOfRegister((String)instruction.operands[0].value);
                int secondOperandValue = getValueOfName((String)instruction.operands[1].value);
                int thirdOperandValue = (Integer) instruction.operands[2].value;
                int shiftedValue = (firstOperandValue & generateByteMask(thirdOperandValue))<<secondOperandValue;
                names.put((String)instruction.operands[0].value, maskBlitNumbers(shiftedValue, firstOperandValue, generateByteMask(thirdOperandValue)));
            }
        };
        instructionSet.add(instruction);
        /////LSR
        //////////////Register, Id, Number
        instruction = new Instruction();
        instruction.name = "LSR";
        instruction.operands[0] = new Operand();
        instruction.operands[0].operandType = Operand.OperandType.Register;
        instruction.operands[1] = new Operand();
        instruction.operands[1].operandType = Operand.OperandType.Id;
        instruction.operands[2] = new Operand();
        instruction.operands[2].operandType = Operand.OperandType.Number;
        instruction.function = new InstructionFunction() {
            @Override
            public void doInstruction(Instruction instruction) {
                int firstOperandValue = getValueOfRegister((String)instruction.operands[0].value);
                int secondOperandValue = getValueOfName((String)instruction.operands[1].value);
                int thirdOperandValue = (Integer) instruction.operands[2].value;
                int shiftedValue = (firstOperandValue & generateByteMask(thirdOperandValue))>>>secondOperandValue;
                names.put((String)instruction.operands[0].value, maskBlitNumbers(shiftedValue, firstOperandValue, generateByteMask(thirdOperandValue)));
            }
        };
        instructionSet.add(instruction);
        /////ASL
        //////////////Register, Id, Number
        instruction = new Instruction();
        instruction.name = "ASL";
        instruction.operands[0] = new Operand();
        instruction.operands[0].operandType = Operand.OperandType.Register;
        instruction.operands[1] = new Operand();
        instruction.operands[1].operandType = Operand.OperandType.Id;
        instruction.operands[2] = new Operand();
        instruction.operands[2].operandType = Operand.OperandType.Number;
        instruction.function = new InstructionFunction() {
            @Override
            public void doInstruction(Instruction instruction) {
                int firstOperandValue = getValueOfRegister((String)instruction.operands[0].value);
                int secondOperandValue = getValueOfName((String)instruction.operands[1].value);
                int thirdOperandValue = (Integer) instruction.operands[2].value;
                int shiftedValue = (firstOperandValue & generateByteMask(thirdOperandValue))<<secondOperandValue;
                names.put((String)instruction.operands[0].value, maskBlitNumbers(shiftedValue, firstOperandValue, generateByteMask(thirdOperandValue)));
            }
        };
        instructionSet.add(instruction);
        /////ASR
        //////////////Register, Id, Number
        instruction = new Instruction();
        instruction.name = "ASR";
        instruction.operands[0] = new Operand();
        instruction.operands[0].operandType = Operand.OperandType.Register;
        instruction.operands[1] = new Operand();
        instruction.operands[1].operandType = Operand.OperandType.Id;
        instruction.operands[2] = new Operand();
        instruction.operands[2].operandType = Operand.OperandType.Number;
        instruction.function = new InstructionFunction() {
            @Override
            public void doInstruction(Instruction instruction) {
                int firstOperandValue = getValueOfRegister((String)instruction.operands[0].value);
                int secondOperandValue = getValueOfName((String)instruction.operands[1].value);
                int thirdOperandValue = (Integer) instruction.operands[2].value;
                int shiftedValue = (firstOperandValue & generateByteMask(thirdOperandValue))>>secondOperandValue;
                names.put((String)instruction.operands[0].value, maskBlitNumbers(shiftedValue, firstOperandValue, generateByteMask(thirdOperandValue)));
            }
        };
        instructionSet.add(instruction);
        /////LSL
        //////////////Register, Number, Number
        instruction = new Instruction();
        instruction.name = "LSL";
        instruction.operands[0] = new Operand();
        instruction.operands[0].operandType = Operand.OperandType.Register;
        instruction.operands[1] = new Operand();
        instruction.operands[1].operandType = Operand.OperandType.Number;
        instruction.operands[2] = new Operand();
        instruction.operands[2].operandType = Operand.OperandType.Number;
        instruction.function = new InstructionFunction() {
            @Override
            public void doInstruction(Instruction instruction) {
                int firstOperandValue = getValueOfRegister((String)instruction.operands[0].value);
                int secondOperandValue = (Integer) instruction.operands[1].value;
                int thirdOperandValue = (Integer) instruction.operands[2].value;
                int shiftedValue = (firstOperandValue & generateByteMask(thirdOperandValue))<<secondOperandValue;
                names.put((String)instruction.operands[0].value, maskBlitNumbers(shiftedValue, firstOperandValue, generateByteMask(thirdOperandValue)));
            }
        };
        instructionSet.add(instruction);
        /////LSR
        //////////////Register, Number, Number
        instruction = new Instruction();
        instruction.name = "LSR";
        instruction.operands[0] = new Operand();
        instruction.operands[0].operandType = Operand.OperandType.Register;
        instruction.operands[1] = new Operand();
        instruction.operands[1].operandType = Operand.OperandType.Number;
        instruction.operands[2] = new Operand();
        instruction.operands[2].operandType = Operand.OperandType.Number;
        instruction.function = new InstructionFunction() {
            @Override
            public void doInstruction(Instruction instruction) {
                int firstOperandValue = getValueOfRegister((String)instruction.operands[0].value);
                int secondOperandValue = (Integer) instruction.operands[1].value;
                int thirdOperandValue = (Integer) instruction.operands[2].value;
                int shiftedValue = (firstOperandValue & generateByteMask(thirdOperandValue))>>>secondOperandValue;
                names.put((String)instruction.operands[0].value, maskBlitNumbers(shiftedValue, firstOperandValue, generateByteMask(thirdOperandValue)));
            }
        };
        instructionSet.add(instruction);
        /////ASL
        //////////////Register, Number, Number
        instruction = new Instruction();
        instruction.name = "ASL";
        instruction.operands[0] = new Operand();
        instruction.operands[0].operandType = Operand.OperandType.Register;
        instruction.operands[1] = new Operand();
        instruction.operands[1].operandType = Operand.OperandType.Number;
        instruction.operands[2] = new Operand();
        instruction.operands[2].operandType = Operand.OperandType.Number;
        instruction.function = new InstructionFunction() {
            @Override
            public void doInstruction(Instruction instruction) {
                int firstOperandValue = getValueOfRegister((String)instruction.operands[0].value);
                int secondOperandValue = (Integer) instruction.operands[1].value;
                int thirdOperandValue = (Integer) instruction.operands[2].value;
                int shiftedValue = (firstOperandValue & generateByteMask(thirdOperandValue))<<secondOperandValue;
                names.put((String)instruction.operands[0].value, maskBlitNumbers(shiftedValue, firstOperandValue, generateByteMask(thirdOperandValue)));
            }
        };
        instructionSet.add(instruction);
        /////ASR
        //////////////Register, Number, Number
        instruction = new Instruction();
        instruction.name = "ASR";
        instruction.operands[0] = new Operand();
        instruction.operands[0].operandType = Operand.OperandType.Register;
        instruction.operands[1] = new Operand();
        instruction.operands[1].operandType = Operand.OperandType.Number;
        instruction.operands[2] = new Operand();
        instruction.operands[2].operandType = Operand.OperandType.Number;
        instruction.function = new InstructionFunction() {
            @Override
            public void doInstruction(Instruction instruction) {
                int firstOperandValue = getValueOfRegister((String)instruction.operands[0].value);
                int secondOperandValue = (Integer) instruction.operands[1].value;
                int thirdOperandValue = (Integer) instruction.operands[2].value;
                int shiftedValue = (firstOperandValue & generateByteMask(thirdOperandValue))>>secondOperandValue;
                names.put((String)instruction.operands[0].value, maskBlitNumbers(shiftedValue, firstOperandValue, generateByteMask(thirdOperandValue)));
            }
        };
        instructionSet.add(instruction);
        /////JMP
        //////////////Id
        instruction = new Instruction();
        instruction.name = "JMP";
        instruction.operands[0] = new Operand();
        instruction.operands[0].operandType = Operand.OperandType.Id;
        instruction.function = new InstructionFunction() {
            @Override
            public void doInstruction(Instruction instruction) {
                if (labels.get(instruction.operands[0].value) == null) {
                    System.err.println("I donnu a goddamned thing about this label, i'm not doing the jump, cya");
                    return;
                }
                pc = labels.get((String)instruction.operands[0].value);
            }
        };
        instructionSet.add(instruction);
        /////JMP
        //////////////Number
        instruction = new Instruction();
        instruction.name = "JMP";
        instruction.operands[0] = new Operand();
        instruction.operands[0].operandType = Operand.OperandType.Number;
        instruction.function = new InstructionFunction() {
            @Override
            public void doInstruction(Instruction instruction) {
                pc += (Integer)instruction.operands[0].value - 1;
            }
        };
        instructionSet.add(instruction);
        /////JMP
        //////////////Register
        instruction = new Instruction();
        instruction.name = "JMP";
        instruction.operands[0] = new Operand();
        instruction.operands[0].operandType = Operand.OperandType.Register;
        instruction.function = new InstructionFunction() {
            @Override
            public void doInstruction(Instruction instruction) {
                pc += getValueOfRegister((String)instruction.operands[0].value) - 1;
            }
        };
        instructionSet.add(instruction);
        /////JIZ
        //////////////Id, Register, Number
        instruction = new Instruction();
        instruction.name = "JIZ";
        instruction.operands[0] = new Operand();
        instruction.operands[0].operandType = Operand.OperandType.Id;
        instruction.operands[1] = new Operand();
        instruction.operands[1].operandType = Operand.OperandType.Register;
        instruction.operands[2] = new Operand();
        instruction.operands[2].operandType = Operand.OperandType.Number;
        instruction.function = new InstructionFunction() {
            @Override
            public void doInstruction(Instruction instruction) {
                if (labels.get(instruction.operands[0].value) == null) {
                    System.err.println("I donnu a goddamned thing about this label, i'm not doing the jump, cya");
                    return;
                }
                if ((getValueOfRegister((String)instruction.operands[2].value) & generateByteMask((Integer)instruction.operands[2].value)) == 0)
                    pc = labels.get((String)instruction.operands[0].value);
            }
        };
        instructionSet.add(instruction);
        /////JIZ
        //////////////Number, Register, Number
        instruction = new Instruction();
        instruction.name = "JIZ";
        instruction.operands[0] = new Operand();
        instruction.operands[0].operandType = Operand.OperandType.Number;
        instruction.operands[1] = new Operand();
        instruction.operands[1].operandType = Operand.OperandType.Register;
        instruction.operands[2] = new Operand();
        instruction.operands[2].operandType = Operand.OperandType.Number;
        instruction.function = new InstructionFunction() {
            @Override
            public void doInstruction(Instruction instruction) {
                if ((getValueOfRegister((String)instruction.operands[2].value) & generateByteMask((Integer)instruction.operands[2].value)) == 0)
                    pc += ((Integer)instruction.operands[0].value) - 1;
            }
        };
        instructionSet.add(instruction);
        /////JIZ
        //////////////Register, Register, Number
        instruction = new Instruction();
        instruction.name = "JIZ";
        instruction.operands[0] = new Operand();
        instruction.operands[0].operandType = Operand.OperandType.Register;
        instruction.operands[1] = new Operand();
        instruction.operands[1].operandType = Operand.OperandType.Register;
        instruction.operands[2] = new Operand();
        instruction.operands[2].operandType = Operand.OperandType.Number;
        instruction.function = new InstructionFunction() {
            @Override
            public void doInstruction(Instruction instruction) {
                if ((getValueOfRegister((String)instruction.operands[2].value) & generateByteMask((Integer)instruction.operands[2].value)) == 0)
                    pc += getValueOfRegister((String)instruction.operands[0].value) - 1;
            }
        };
        instructionSet.add(instruction);
        /////JNZ
        //////////////Id, Register, Number
        instruction = new Instruction();
        instruction.name = "JNZ";
        instruction.operands[0] = new Operand();
        instruction.operands[0].operandType = Operand.OperandType.Id;
        instruction.operands[1] = new Operand();
        instruction.operands[1].operandType = Operand.OperandType.Register;
        instruction.operands[2] = new Operand();
        instruction.operands[2].operandType = Operand.OperandType.Number;
        instruction.function = new InstructionFunction() {
            @Override
            public void doInstruction(Instruction instruction) {
                if (labels.get(instruction.operands[0].value) == null) {
                    System.err.println("I donnu a goddamned thing about this label, i'm not doing the jump, cya");
                    return;
                }
                if ((getValueOfRegister((String)instruction.operands[2].value) & generateByteMask((Integer)instruction.operands[2].value)) != 0)
                    pc = labels.get((String)instruction.operands[0].value);
            }
        };
        instructionSet.add(instruction);
        /////JNZ
        //////////////Number, Register, Number
        instruction = new Instruction();
        instruction.name = "JNZ";
        instruction.operands[0] = new Operand();
        instruction.operands[0].operandType = Operand.OperandType.Number;
        instruction.operands[1] = new Operand();
        instruction.operands[1].operandType = Operand.OperandType.Register;
        instruction.operands[2] = new Operand();
        instruction.operands[2].operandType = Operand.OperandType.Number;
        instruction.function = new InstructionFunction() {
            @Override
            public void doInstruction(Instruction instruction) {
                if ((getValueOfRegister((String)instruction.operands[2].value) & generateByteMask((Integer)instruction.operands[2].value)) != 0)
                    pc += ((Integer)instruction.operands[0].value) - 1;
            }
        };
        instructionSet.add(instruction);
        /////JNZ
        //////////////Register, Register, Number
        instruction = new Instruction();
        instruction.name = "JNZ";
        instruction.operands[0] = new Operand();
        instruction.operands[0].operandType = Operand.OperandType.Register;
        instruction.operands[1] = new Operand();
        instruction.operands[1].operandType = Operand.OperandType.Register;
        instruction.operands[2] = new Operand();
        instruction.operands[2].operandType = Operand.OperandType.Number;
        instruction.function = new InstructionFunction() {
            @Override
            public void doInstruction(Instruction instruction) {
                if ((getValueOfRegister((String)instruction.operands[2].value) & generateByteMask((Integer)instruction.operands[2].value)) != 0)
                    pc += getValueOfRegister((String)instruction.operands[0].value) - 1;
            }
        };
        instructionSet.add(instruction);
        /////PSH
        //////////////Number
        instruction = new Instruction();
        instruction.name = "PSH";
        instruction.operands[0] = new Operand();
        instruction.operands[0].operandType = Operand.OperandType.Number;
        instruction.function = new InstructionFunction() {
            @Override
            public void doInstruction(Instruction instruction) {
                stackPointer -= (Integer) instruction.operands[0].value;
            }
        };
        instructionSet.add(instruction);
        /////PSH
        //////////////Register, Number
        instruction = new Instruction();
        instruction.name = "PSH";
        instruction.operands[0] = new Operand();
        instruction.operands[0].operandType = Operand.OperandType.Register;
        instruction.operands[1] = new Operand();
        instruction.operands[1].operandType = Operand.OperandType.Number;
        instruction.function = new InstructionFunction() {
            @Override
            public void doInstruction(Instruction instruction) {
                stackPointer -= (Integer)instruction.operands[1].value;
                byte[] integerBytes = intToByteArray(getValueOfRegister((String)instruction.operands[0].value));
                int address = stackPointer + 1;
                if (address < 0 || address >= memorySize) {
                    System.err.println("I don't know wtf you think your doing, but you're accessing out of your memory in STR instruction.");
                    return;
                }
                byte[] wantedByte = new byte[(Integer)instruction.operands[1].value];
                for (int i=0;i<wantedByte.length; i++)
                    wantedByte[i] = integerBytes[4 - wantedByte.length + i];
                writeToMemory(wantedByte, address, wantedByte.length);
            }
        };
        instructionSet.add(instruction);
        /////PSH
        //////////////Id, Number
        instruction = new Instruction();
        instruction.name = "PSH";
        instruction.operands[0] = new Operand();
        instruction.operands[0].operandType = Operand.OperandType.Id;
        instruction.operands[1] = new Operand();
        instruction.operands[1].operandType = Operand.OperandType.Number;
        instruction.function = new InstructionFunction() {
            @Override
            public void doInstruction(Instruction instruction) {
                stackPointer -= (Integer)instruction.operands[1].value;
                byte[] integerBytes = intToByteArray(getValueOfName((String)instruction.operands[0].value));
                int address = stackPointer + 1;
                if (address < 0 || address >= memorySize) {
                    System.err.println("I don't know wtf you think your doing, but you're accessing out of your memory in STR instruction.");
                    return;
                }
                byte[] wantedByte = new byte[(Integer)instruction.operands[1].value];
                for (int i=0;i<wantedByte.length; i++)
                    wantedByte[i] = integerBytes[4 - wantedByte.length + i];
                writeToMemory(wantedByte, address, wantedByte.length);
            }
        };
        instructionSet.add(instruction);
        /////PSH
        //////////////Id, Number
        instruction = new Instruction();
        instruction.name = "PSH";
        instruction.operands[0] = new Operand();
        instruction.operands[0].operandType = Operand.OperandType.Number;
        instruction.operands[1] = new Operand();
        instruction.operands[1].operandType = Operand.OperandType.Number;
        instruction.function = new InstructionFunction() {
            @Override
            public void doInstruction(Instruction instruction) {
                stackPointer -= (Integer)instruction.operands[1].value;
                byte[] integerBytes = intToByteArray((Integer) instruction.operands[0].value);
                int address = stackPointer + 1;
                if (address < 0 || address >= memorySize) {
                    System.err.println("I don't know wtf you think your doing, but you're accessing out of your memory in PSH instruction.");
                    return;
                }
                byte[] wantedByte = new byte[(Integer)instruction.operands[1].value];
                for (int i=0;i<wantedByte.length; i++)
                    wantedByte[i] = integerBytes[4 - wantedByte.length + i];
                writeToMemory(wantedByte, address, wantedByte.length);
            }
        };
        instructionSet.add(instruction);
        /////POP
        //////////////Number
        instruction = new Instruction();
        instruction.name = "POP";
        instruction.operands[0] = new Operand();
        instruction.operands[0].operandType = Operand.OperandType.Number;
        instruction.function = new InstructionFunction() {
            @Override
            public void doInstruction(Instruction instruction) {
                stackPointer += (Integer) instruction.operands[0].value;
            }
        };
        instructionSet.add(instruction);
        /////POP
        //////////////Register, Number
        instruction = new Instruction();
        instruction.name = "POP";
        instruction.operands[0] = new Operand();
        instruction.operands[0].operandType = Operand.OperandType.Register;
        instruction.operands[1] = new Operand();
        instruction.operands[1].operandType = Operand.OperandType.Number;
        instruction.function = new InstructionFunction() {
            @Override
            public void doInstruction(Instruction instruction) {
                byte[] integerBytes = intToByteArray(getValueOfRegister((String)instruction.operands[0].value));
                int address = stackPointer + 1;
                stackPointer += (Integer)instruction.operands[1].value;
                if (address < 0 || address >= memorySize) {
                    System.err.println("I don't know wtf you think your doing, but you're accessing out of your memory in POP instruction.");
                    return;
                }
                loadToRegisterFromMemory(address, (Integer)instruction.operands[1].value, (String)instruction.operands[0].value);
            }
        };
        instructionSet.add(instruction);
        /////CAL
        //////////////Id
        instruction = new Instruction();
        instruction.name = "CAL";
        instruction.operands[0] = new Operand();
        instruction.operands[0].operandType = Operand.OperandType.Id;
        instruction.function = new InstructionFunction() {
            @Override
            public void doInstruction(Instruction instruction) {
                if (labels.get(instruction.operands[0].value) == null) {
                    System.err.println("I donnu a goddamned thing about this label, i'm not doing the call, cya");
                    return;
                }
                returnAddresses.push(pc);
                pc = labels.get((String)instruction.operands[0].value);
            }
        };
        instructionSet.add(instruction);
        /////LNK
        //////////////Id
        instruction = new Instruction();
        instruction.name = "LNK";
        instruction.operands[0] = new Operand();
        instruction.operands[0].operandType = Operand.OperandType.Id;
        instruction.function = new InstructionFunction() {
            @Override
            public void doInstruction(Instruction instruction) {
                if (labels.get(instruction.operands[0].value) == null) {
                    System.err.println("I donnu a goddamned thing about this label, i'm not doing the linked call, cya");
                    return;
                }
                framePointers.push(stackPointer + 1);
                returnAddresses.push(pc);
                pc = labels.get((String)instruction.operands[0].value);
            }
        };
        instructionSet.add(instruction);
        /////RET
        //////////////Empty
        instruction = new Instruction();
        instruction.name = "RET";
        instruction.operands[0] = new Operand();
        instruction.function = new InstructionFunction() {
            @Override
            public void doInstruction(Instruction instruction) {
                framePointers.pop();
                pc = returnAddresses.pop();
            }
        };
        instructionSet.add(instruction);
        /////RET
        //////////////Number
        instruction = new Instruction();
        instruction.name = "RET";
        instruction.operands[0] = new Operand();
        instruction.operands[0].operandType = Operand.OperandType.Number;
        instruction.function = new InstructionFunction() {
            @Override
            public void doInstruction(Instruction instruction) {
                framePointers.pop();
                pc = returnAddresses.pop();
                stackPointer += (Integer)instruction.operands[0].value;
            }
        };
        instructionSet.add(instruction);
        /////TRM
        //////////////Register
        instruction = new Instruction();
        instruction.name = "TRM";
        instruction.operands[0] = new Operand();
        instruction.operands[0].operandType = Operand.OperandType.Register;
        instruction.function = new InstructionFunction() {
            @Override
            public void doInstruction(Instruction instruction) {
                terminated = true;
                terminationResult = getValueOfRegister((String)instruction.operands[0].value);
            }
        };
        instructionSet.add(instruction);
        /////TRM
        //////////////Id
        instruction = new Instruction();
        instruction.name = "TRM";
        instruction.operands[0] = new Operand();
        instruction.operands[0].operandType = Operand.OperandType.Id;
        instruction.function = new InstructionFunction() {
            @Override
            public void doInstruction(Instruction instruction) {
                terminated = true;
                terminationResult = getValueOfName((String)instruction.operands[0].value);
            }
        };
        instructionSet.add(instruction);
        /////TRM
        //////////////Number
        instruction = new Instruction();
        instruction.name = "TRM";
        instruction.operands[0] = new Operand();
        instruction.operands[0].operandType = Operand.OperandType.Number;
        instruction.function = new InstructionFunction() {
            @Override
            public void doInstruction(Instruction instruction) {
                terminated = true;
                terminationResult = (Integer) instruction.operands[0].value;
            }
        };
        instructionSet.add(instruction);
        /////SDF
        //////////////Empty
        instruction = new Instruction();
        instruction.name = "SDF";
        instruction.operands[0] = new Operand();
        instruction.function = new InstructionFunction() {
            @Override
            public void doInstruction(Instruction instruction) {
                reverseStringBytes = false;
            }
        };
        instructionSet.add(instruction);

        /////SDB
        //////////////Empty
        instruction = new Instruction();
        instruction.name = "SDB";
        instruction.operands[0] = new Operand();
        instruction.function = new InstructionFunction() {
            @Override
            public void doInstruction(Instruction instruction) {
                reverseStringBytes = true;
            }
        };
        instructionSet.add(instruction);
    }

    public void loadInstructions(String instructions) {
        Scanner scanner = new Scanner(new StringReader(instructions));
        Instruction nextInstr = null;
        try {
            nextInstr = scanner.yylex();
        } catch (IOException e) {
            System.err.println("Something went wrong. This VM has no error handling lol. Tell HGSilverman to write a damn error handling for this.");
        }
        while (nextInstr != null) {
            Instruction currentInstruction = nextInstr;
            try {
                nextInstr = scanner.yylex();
            } catch (IOException e) {
                System.err.println("Something went wrong. This VM has no error handling lol. Tell HGSilverman to write a damn error handling for this.");
            }
            if (currentInstruction.name.toUpperCase().equals("LBL")) {
                if (currentInstruction.operands[1] != null) {
                    System.err.println("Extra damned operands for label, goddammit you just need a single label as operands.");
                    continue;
                }
                if (currentInstruction.operands[0].operandType != Operand.OperandType.Id) {
                    System.err.println("What have you done, the scanner did not recognize the label as an id, wtf?");
                    continue;
                }
                labels.put((String)currentInstruction.operands[0].value, this.instructions.size());
            } else {
                this.instructions.add(currentInstruction);
            }
        }
        return;
    }

    public void doNextInstruction() {
        if (pc >= instructions.size())
            return;
        
        Instruction currentInstruction = instructions.get(pc);
        pc++;
        int instructionIndex = instructionSet.indexOf(currentInstruction);
        if (instructionIndex == -1) {
            System.err.println("Man, I tired just so that you know, but I could not find no instruction like that in my instruction set.");
            return;
        }
        Instruction matchingInstruction = instructionSet.get(instructionIndex);
        matchingInstruction.function.doInstruction(currentInstruction);
    }
    public void reset() {
        pc = 0;
        stackPointer = memorySize - 1;
        framePointers.clear();
        framePointers.add(stackPointer + 1);
        registers.clear();
        names.clear();
        instructions.clear();
        returnAddresses.clear();
    }

    public byte[] getMemory() {
        return memory;
    }

    public int getPc() {
        return pc;
    }

    public HashMap<String, Integer> getRegisters() {
        return registers;
    }

    public int getMemorySize() {
        return memorySize;
    }

    public int getStackPointer() {
        return stackPointer;
    }

    public Stack<Integer> getFramePointers() {
        return framePointers;
    }

    public Instruction[] getInstructions() {
        Instruction[] result = new Instruction[instructions.size()];
        return instructions.toArray(result);
    }

    public boolean isTerminated() {
        return terminated;
    }

    public int getTerminationResult() {
        return terminationResult;
    }

    private void loadToRegisterFromMemory(int address, int bytes, String register) {
        int valueToLoad = 0;
        for (int i=0; i < bytes; i++) {
            if ((address + i) > memorySize || (address + i) < 0) {
                System.err.println("I don't know wtf you think your doing, but you're accessing out of your memory.");
                continue;
            }
            valueToLoad *= 256;
            valueToLoad += memory[address + i];
        }
        registers.put(register, valueToLoad);
    }

    private void loadToKBytesOfRegisterFromMemory(int address, int bytes, String register, int k) {
        int valueToLoad = 0;
        for (int i=0; i < bytes; i++) {
            if ((address + i) > memorySize || (address + i) < 0) {
                System.err.println("I don't know wtf you think your doing, but you're accessing out of your memory.");
                continue;
            }
            valueToLoad *= 256;
            valueToLoad += memory[address + i];
        }
        registers.put(register, maskBlitNumbers(valueToLoad, getValueOfRegister(register), generateByteMask(k)));
    }

    private int getValueOfName(String name) {
        Integer value = names.get(name);
        if (value == null) {
            System.err.println("I donnu what " + name + " is, but it damn sure isn't a name I've ever heard of. I'm gonna consider it zero.");
            return 0;
        }
        return value;
    }

    private int getValueOfRegister (String register) {
        Integer value = names.get(register);
        if (value == null) {
            return 0;
        }
        return value;
    }

    private int maskBlitNumbers (int v1, int v2, int mask) {
        return (v1&mask) | (v2&(~mask));
    }

    private int generateByteMask (int bytes) {
        return (1 << (bytes*8)) - 1;
    }

    private byte[] intToByteArray (int inp) {
        byte[] result = new byte[4];
        for (int i=3;i>-1;i--) {
            result[i] = (byte)(inp & 0xff);
            inp = inp>>>8;
        }
        return result;
    }

    private void writeToMemory (byte[] bytes, int address, int size) {
        for (int i=0;i<size; i++) {
            if ((address + i) > memorySize || (address + i) < 0) {
                System.err.println("I don't know wtf you think your doing, but you're accessing out of your memory.");
                continue;
            }
            memory[address + i] = bytes[i];
        }
    }
    private byte[] reverseBytes(byte[] bytes) {
        byte[] reversedBytes = new byte[bytes.length];
        for (int i=0; i<bytes.length; i++)
            reversedBytes[i] = bytes[bytes.length - 1 - i];
        return reversedBytes;
    }
}
