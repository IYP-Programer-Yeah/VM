import vm.VM;

/**
 * Created by HoseinGhahremanzadeh on 7/6/2017.
 */
public class Main {
    public static void main(String[] args) {
        VM vm = new VM(1024*1024);
        vm.loadInstructions("PSH 1     //pushing space for variable done\n" +
                "PSH 8     //pushing space for variable text\n" +
                "PSH 40     //pushing space for variable p\n" +
                "PSH 8     //pushing space for variable s\n" +
                "PSH 4     //push the space for return value\n" +
                "LNK start312714112     //call the start function\n" +
                "POP R0,4     //popping the return value\n" +
                "POP 61     //popping the global variables at the end of block\n" +
                "TRM R0     //terminate the program by the return value of the start function\n" +
                "LBL fact856419764     //function label\n" +
                "PSH 4     //pushing space for variable a\n" +
                "POP 4     //popping the local variables at the end of block\n" +
                "     //function body finished\n" +
                "LBL start312714112     //function label\n" +
                "PSH 1     //pushing space for variable c\n" +
                "LBL lb     //label code\n" +
                "PSH 8     //pushing space for variable d\n" +
                "POP 8     //popping the local variables at the end of block\n" +
                "LBL BreakLabel621009875     //label code\n" +
                "PSH 4     //pushing space for variable j\n" +
                "PSH 8     //pushing extra stack space for the goto destination block\n" +
                "JMP ContinueLabel1265094477     //jump to goto destination\n" +
                "PSH 8     //pushing space for variable star\n" +
                "LBL ContinueLabel1265094477     //label code\n" +
                "POP 8     //popping the local variables at the end of block\n" +
                "LBL BreakLabel1265094477     //label code\n" +
                "POP 4     //popping the local variables at the end of block\n" +
                "JMP BreakLabel2125039532     //jump to goto destination\n" +
                "LBL ContinueLabel2125039532     //label code\n" +
                "LBL BreakLabel2125039532     //label code\n" +
                "JMP lb     //jump to goto destination\n" +
                "PSH 16     //pushing space for variable me\n" +
                "POP 17     //popping the local variables at the end of block\n" +
                "     //function body finished\n" +
                "LBL abs692404036     //function label\n" +
                "     //function body finished\n" +
                "LBL max1554874502     //function label\n" +
                "     //function body finished");

        while (!vm.isTerminated()) {
            vm.doNextInstruction();
        }
        System.err.println("Process finished with exit code " + vm.getTerminationResult());
    }
}
