import vm.VM;

import java.io.*;

/**
 * Created by HoseinGhahremanzadeh on 7/6/2017.
 */
public class Main {
    public static void main(String[] args) {
        int memorySize = 1024 * 1024;
        String sourceDir = "out.bin";
        for (int i=0;i<args.length;i++) {
            if (args[i].toLowerCase().equals("-src") && i < args.length - 1)
                sourceDir = args[i + 1];
            if (args[i].toLowerCase().equals("-mem") && i < args.length - 1)
                memorySize = Integer.parseInt(args[i + 1]);
        }

        VM vm = new VM(memorySize);

        File sourceFile = new File(sourceDir);
        FileInputStream source = null;
        try {
            source = new FileInputStream(sourceFile);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        byte fileData[] = new byte[(int)sourceFile.length()];
        try {
            source.read(fileData);
        } catch (IOException e) {
            e.printStackTrace();
        }
        String instructions = new String(fileData);

        vm.loadInstructions(instructions);

        while (!vm.isTerminated()) {
            vm.doNextInstruction();
        }
        System.err.println("Process finished with exit code " + vm.getTerminationResult());
    }
}
