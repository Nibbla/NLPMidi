import javafx.util.Pair;

import javax.imageio.ImageIO;
import javax.sound.midi.*;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by Niebisch Markus on 22.05.2019.
 */
public class mainClass extends JFrame{

    static final Logger logger = Logger.getLogger(mainClass.class.getName());
    private static ArrayList<MidiSequence> midisequences;
    private static long seed;
    private JPanel fileMenuePanel;
    private HashMap<String, int[]> channelChoises;
    private Sequencer sequencer;
    private JLabel imagePanel;
    private static Random random = null;

    public static void main(String[] args){
        seed = new Random().nextLong();
        System.out.println("Initialised RNG Generator with seed: " + seed);
        random = new Random(seed);

        ArrayList<Double> e = new ArrayList<>();
        e.add(3.);e.add(1.);e.add(0.2);
        ArrayList<Double> sol = softmax(e);
        double[] cuma = getCumaliativDistFromPDF(sol);

        boolean autorun = true;
        mainClass mc = new mainClass();
        mc.readChannelChoisesFromFile();
        mc.createWindow(autorun);
        mc.setVisible(true);
        if (autorun){

        }
    }

    private static double[] getCumaliativDistFromPDF(double[] sol) {
        // Transform your probabilities into a cumulative distribution
        int n = sol.length;
        double[] cdf = new double[n];
        cdf[0] = sol[0];
        for(int i = 1; i < sol.length; i++)
            cdf[i] += sol[i] + cdf[i-1];
        return cdf;
    }
    private static double[] getCumaliativDistFromPDF(ArrayList<Double> sol) {
        // Transform your probabilities into a cumulative distribution
        int n = sol.size();
        double[] cdf = new double[n];
        cdf[0] = sol.get(0);
        for(int i = 1; i < sol.size(); i++)
            cdf[i] += sol.get(i) + cdf[i-1];
        return cdf;
    }

    private static int getSampleFromCDF(double[] CDF) {
        // Transform your probabilities into a cumulative distribution

        // Search the bin corresponding to that quantile
        int k = Arrays.binarySearch(CDF, random.nextDouble());
        k = k >= 0 ? k : (-k-1);
        return k;
    }

    private void readChannelChoisesFromFile() {
        HashMap<String,int[]> channelChoises = new HashMap<>();
        this.channelChoises = channelChoises;
        try (BufferedReader br = new BufferedReader(new FileReader("input/channel.csv"))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] values = line.split(";");
                String fileName = values[0];
                String channelS = values[1];
                String[] channelAsString = channelS.split(",");
                int[] channels = new int[channelAsString.length];
                for (int i = 0; i < channels.length; i++) {
                     channels[i] = Integer.parseInt(channelAsString[i]);
                }
                channelChoises.put(fileName,channels);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void createWindow(boolean autorun) {
        //loadButton
        //Analise
        init();

    }
    private void init() {
        this.setPreferredSize(new Dimension(800,400));
        this.setLayout(new BorderLayout());

        imagePanel = new JLabel();
        this.add(imagePanel,BorderLayout.CENTER);
        JTabbedPane tabbedPane = new JTabbedPane();
        this.add(tabbedPane,BorderLayout.SOUTH);
        tabbedPane.setPreferredSize(new Dimension(600,150));

        fileMenuePanel = createFileMenuePanel();
        tabbedPane.addTab("File", fileMenuePanel);

        pack();
    }

    private JPanel createFileMenuePanel() {

        JPanel fileMenuePanel = new JPanel();
        fileMenuePanel.setLayout(new BoxLayout(fileMenuePanel, BoxLayout.X_AXIS));
        fileMenuePanel.add(Box.createHorizontalGlue());
        addButton(fileMenuePanel, "Load", null, e -> this.load());
        addButton(fileMenuePanel, "<html>Load All<br />InputFolder</html>", null, e -> this.loadAll());
        //addButton(fileMenuePanel, "Extract Melodie", null, e -> this.melodie(true));
        addButton(fileMenuePanel, "<html>Create Image<br />Matrix</html> ", null, e -> this.imageMatrix());
        addButton(fileMenuePanel, "<html>Create Image<br />Matrix for all</html>", null, e -> this.imageMatrixForAll());
        addButton(fileMenuePanel, "<html>Save Melodie for all<br />loaded using default channels</html>", null, e -> this.saveMelodie());
        addButton(fileMenuePanel, "<html>Create Probability<br />Matrix</html>", null, e -> this.probabilityMatrix());
        //addButton(fileMenuePanel, "Create N-Gram", null, e -> this.ngram());
        addButton(fileMenuePanel, "Play Melodie", null, e -> this.playSequence());
        addButton(fileMenuePanel, "Stop Melodie", null, e -> this.stopSequence());
        return fileMenuePanel;
    }

    private void loadAll() {
        midisequences =  load(getFilesRec(new File("Input")));
    }

    private void imageMatrixForAll() {
        boolean colorMode = false;
        int dialogResult2 = JOptionPane.showConfirmDialog (null, "Would You Like to use color?","ColorChoise",JOptionPane.YES_NO_OPTION);
        if(dialogResult2 == JOptionPane.YES_OPTION){
            colorMode = true;
        }

        int[] channels = new int[]{0};
        boolean channelChoiseFile = false;
        int dialogResult = JOptionPane.showConfirmDialog (null, "Would You Like to use the default channel?","ChannelChoise",JOptionPane.YES_NO_OPTION);
        if(dialogResult == JOptionPane.YES_OPTION){
            channelChoiseFile = true;
            readChannelChoisesFromFile();
        }
        for (MidiSequence ms:midisequences) {
            MidiSequence chosen = ms;
            String fileName = chosen.getFileName();
            Sequence sequence = chosen.getSequence();
            channels = channelChoises.get(fileName);
            for (int ch:channels) {
                int selectedChannel = ch;
                ArrayList<MidiSequence.Note> notes = chosen.getNotes(selectedChannel);
                MidiSequence.processMelodie(notes); //normalizing and adding breaks

                BufferedImage bi = MidiSequence.createImage(notes,800,800, colorMode);
                //this.remove(imagePanel);
                //imagePanel = new JLabel(new ImageIcon(bi));
                //add(imagePanel,BorderLayout.CENTER);
                //int newWidth = bi.getWidth() + 40;
                //int newHeight = bi.getHeight() +40 + fileMenuePanel.getHeight();
                //this.setPreferredSize(new Dimension(newWidth,newHeight));
                //this.pack();
                //this.repaint();
                saveImage(bi,chosen,selectedChannel,colorMode);

            }
        }






    }

    private void imageMatrix(){

        int[] channels = new int[]{0};
        boolean channelChoiseFile = false;
        int dialogResult = JOptionPane.showConfirmDialog (null, "Would You Like to use the default channel?","ChannelChoise",JOptionPane.YES_NO_OPTION);
        if(dialogResult == JOptionPane.YES_OPTION){
            channelChoiseFile = true;
            readChannelChoisesFromFile();
        }
        boolean colorMode = false;
        int dialogResult2 = JOptionPane.showConfirmDialog (null, "Would You Like to use color?","ColorChoise",JOptionPane.YES_NO_OPTION);
        if(dialogResult2 == JOptionPane.YES_OPTION){
            colorMode = true;
        }

        MidiSequence chosen = null;
        if (midisequences.size() == 1){
            chosen = midisequences.get(0);
        }else {
            MidiSequence[] choises = new MidiSequence[midisequences.size()];
            midisequences.toArray(choises);
            chosen = (MidiSequence) JOptionPane.showInputDialog(null, "Choose a track. Melody should be pressed first", "Menu", JOptionPane.PLAIN_MESSAGE, null, choises, choises[0]);
        }


        String fileName = chosen.getFileName();
        Sequence sequence = chosen.getSequence();

        if (channelChoiseFile){
            channels = channelChoises.get(fileName);
        }else{
            String answer = JOptionPane.showInputDialog(null, "Define the channel for file " + fileName, "Channel", JOptionPane.INFORMATION_MESSAGE);
            channels = new int[]{Integer.parseInt(answer)};
        }
        int selectedChannel = 0;
        if (channels.length>1){
            Integer[] what = Arrays.stream(channels).boxed().toArray( Integer[]::new );
            selectedChannel = (Integer) JOptionPane.showInputDialog(null, "Choose a channel.", "Menu", JOptionPane.PLAIN_MESSAGE, null, what, what[0]);
        }else selectedChannel = channels[0];


        ArrayList<MidiSequence.Note> notes = chosen.getNotes(selectedChannel);
        MidiSequence.processMelodie(notes); //normalizing and adding breaks

        BufferedImage bi = MidiSequence.createImage(notes,800,800,colorMode);
        this.remove(imagePanel);
        imagePanel = new JLabel(new ImageIcon(bi));
        add(imagePanel,BorderLayout.CENTER);
        int newWidth = bi.getWidth() + 40;
        int newHeight = bi.getHeight() +40 + fileMenuePanel.getHeight();
        this.setPreferredSize(new Dimension(newWidth,newHeight));
        this.pack();
        this.repaint();
        saveImage(bi,chosen,selectedChannel, colorMode);



    }

    private void saveImage(BufferedImage bi, MidiSequence chosen, int selectedChannel, boolean colorMode) {
        String[] splittet = chosen.getFileName().split("\\.");
        String name = splittet[splittet.length - 2];
        String colorModeString = "blackWhite";
        if (colorMode) colorModeString = "colorMode";
        long timestamp = System.currentTimeMillis() & 0xfff;
        File outputfile = new File("Output\\images\\" + name +"_" +selectedChannel + "_" +colorModeString+ ".png");
        try {
            ImageIO.write(bi, "png", outputfile);
            System.out.println(outputfile.getAbsolutePath() + " saved");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }



    private void probabilityMatrix() {
        long start = System.currentTimeMillis();
        ArrayList<ArrayList<MidiSequence.Note>> allMelodies = melodie(false);
        String answer2 = JOptionPane.showInputDialog(null, "Select the patternsize", "15");
        int patternsize = Integer.parseInt(answer2);

        int startN = patternsize+1;
        HashMap<Integer,HashMap<LinkedList<Integer>, MidiSequence.MutableInt>> rythms = new HashMap<>();
        HashMap<Integer,HashMap<LinkedList<Integer>, MidiSequence.MutableInt>> keyNorms= new HashMap<>();
        HashMap<Integer,HashMap<LinkedList<Integer>, MidiSequence.MutableInt>> keyCounts= new HashMap<>();
        for (int n = 1; n <= startN; n++) {
            rythms.put(n,MidiSequence.createProbabilityMatrix(allMelodies, n, true, true));
            keyNorms.put(n,MidiSequence.createProbabilityMatrix(allMelodies, n, false, true));
            keyCounts.put(n,MidiSequence.createProbabilityMatrix(allMelodies, n, false, false));
        }
        long stop = System.currentTimeMillis();
        long dur = stop-start;
        System.out.println("It took: " + dur + " to process " + allMelodies.size() + " melodies");


        //create sampler
        HashMap<Integer, HashMap<LinkedList<Integer>, Pair<double[], int[]>>> rythmsCdfs = createCDFS(rythms,startN);
        HashMap<Integer, HashMap<LinkedList<Integer>, Pair<double[], int[]>>> keysCdfs = createCDFS(keyCounts,startN);
        LinkedList<Integer> justASample = new LinkedList<>(); justASample.add(48);justASample.add(48);justASample.add(48);justASample.add(48);

        int noOfNotes = 110;
        int sizeOfPattern = patternsize;
        ArrayList<Integer> noteLengths = createPiece(rythmsCdfs,noOfNotes,sizeOfPattern);
        ArrayList<Integer> keys = createPiece(keysCdfs,noOfNotes,sizeOfPattern);

        int resolution = 240;
        ArrayList<MidiSequence.Note> notes = createNotes(keys,noteLengths,resolution);
        String answer = JOptionPane.showInputDialog(null, "Select the bbm", "160");
        int bpm = Integer.parseInt(answer);
        long timestamp = System.currentTimeMillis() & 0xfff;
        storeNotes("Output\\" + "generatet_seed" + seed + "_bpm_" + bpm+ "_" + "pat_" + sizeOfPattern +"t_" +timestamp+".mid",bpm, notes,resolution);




    }

    private void storeNotes(String path, int bpm, ArrayList<MidiSequence.Note> notes,int res) {

        MidiSequence.createMidiFile(notes, new File(path),bpm,res);//384);
    }

    private ArrayList<MidiSequence.Note> createNotes(ArrayList<Integer> keys, ArrayList<Integer> rythms, int resolution) {
        int n = Math.min(keys.size(),rythms.size()); //should be the same anyhow
        ArrayList<MidiSequence.Note> notes = new ArrayList<>(n);
        double currentTick = 0;
        for (int i = 0; i < n; i++) {
            Integer currentKey = keys.get(i);
            Integer currentNoteLenght = rythms.get(i);
            MidiSequence.Note note = MidiSequence.Note.getInstance(currentKey, currentTick, currentNoteLenght,resolution);
            notes.add(note);
            currentTick = note.getEndTick();
        }


            return notes;
    }

    private ArrayList<Integer> createPiece(HashMap<Integer, HashMap<LinkedList<Integer>, Pair<double[], int[]>>> rythmsCdfs, int noOfNotes, int sizeOfPattern) {
        ArrayList<Integer> returnVal = new ArrayList<>(noOfNotes);
        LinkedList<Integer> justASample = new LinkedList<>();
        //int start = 44;//getSampleFromCDF(cdf);
       // justASample.add(start);
        //returnVal.add(start);
        for (int i = justASample.size(); i < noOfNotes; i++) {

            int sample = getNextSample(rythmsCdfs,justASample);
            justASample.add(sample);
            returnVal.add(sample);
            if (justASample.size() > sizeOfPattern) justASample.removeFirst();
        }

        return returnVal;
    }

    private int getNextSample(HashMap<Integer, HashMap<LinkedList<Integer>, Pair<double[], int[]>>> cdfs, LinkedList<Integer> justASample) {
        int size = justASample.size(); //the sampler has this lenght.

        HashMap<LinkedList<Integer>, Pair<double[], int[]>> map=null;
        Pair<double[], int[]> cdf = null;
        while (cdf == null){
            map=null;

        while (map == null){
            map = cdfs.get(justASample.size()+1);

            size--;
        }
            size++;
            cdf = map.get(justASample);
            if (cdf == null) {
                justASample = (LinkedList<Integer>) justASample.clone();

                if (justASample.size()>0)justASample.removeFirst();
                else cdf = cdfs.get(1).get(justASample);
            }
        }

        int key;
        int sample;
        if (cdf == null){
            //shouldnt happen
            key = getSampleFromCDF(cdf.getKey());
            sample = cdf.getValue()[key];
        }else{
            key = getSampleFromCDF(cdf.getKey());
            sample = cdf.getValue()[key];
        }

        return sample;
    }

    private HashMap<Integer, HashMap<LinkedList<Integer>, Pair<double[],int[]>>> createCDFS(HashMap<Integer, HashMap<LinkedList<Integer>, MidiSequence.MutableInt>> counters,int maxPattern) {
        HashMap<Integer, HashMap<LinkedList<Integer>, Pair<double[], int[]>>> returnVal = new HashMap<>();

        for (int i = 1; i <= maxPattern; i++) {
            HashMap<LinkedList<Integer>, MidiSequence.MutableInt> map = counters.get(i); //the keys have i-1 length
            Iterator<Map.Entry<LinkedList<Integer>, MidiSequence.MutableInt>> it = map.entrySet().iterator();
            HashMap<LinkedList<Integer>,ArrayList<Pair<Integer, MidiSequence.MutableInt>>> allPatternLinkedToTheirFollowUpNote = new HashMap<>();
            while (it.hasNext()){
                Map.Entry<LinkedList<Integer>, MidiSequence.MutableInt> entry = it.next();
                LinkedList<Integer> pattern = (LinkedList<Integer>) entry.getKey().clone();
                Integer output = pattern.removeLast();
                LinkedList<Integer> shortPatternInput = pattern;
                Pair<Integer, MidiSequence.MutableInt> jup = new Pair<>(output, entry.getValue());

                ArrayList<Pair<Integer, MidiSequence.MutableInt>> lookup = allPatternLinkedToTheirFollowUpNote.get(shortPatternInput);
                if (lookup == null) {
                    lookup = new ArrayList<>();
                    allPatternLinkedToTheirFollowUpNote.put(shortPatternInput,lookup);
                }
                lookup.add(jup);
            }

            HashMap<LinkedList<Integer>, Pair<double[], int[]>> toFill = new HashMap<>();
            Iterator<Map.Entry<LinkedList<Integer>, ArrayList<Pair<Integer, MidiSequence.MutableInt>>>> iter2 = allPatternLinkedToTheirFollowUpNote.entrySet().iterator();
            int sizeOfPossibleValue = allPatternLinkedToTheirFollowUpNote.entrySet().size();
            //go trough all Patterns
            while (iter2.hasNext()){
                //now the counting starts
                Map.Entry<LinkedList<Integer>, ArrayList<Pair<Integer, MidiSequence.MutableInt>>> nextor = iter2.next();
                ArrayList<Pair<Integer, MidiSequence.MutableInt>> countme = nextor.getValue();
                LinkedList<Integer> shortpattern = nextor.getKey();
                int[] valueToPlay = new int[countme.size()];
                double[] pecentagos = new double[countme.size()];
                int sum = 0;
                for (int j = 0; j < countme.size(); j++) {
                    Pair<Integer, MidiSequence.MutableInt> al = countme.get(j);
                    MidiSequence.MutableInt currentCount = al.getValue();
                    valueToPlay[j] = al.getKey();
                    sum+= currentCount.value;
                }
                if (true){
                    for (int j = 0; j < countme.size(); j++) {
                        Pair<Integer, MidiSequence.MutableInt> al = countme.get(j);
                        int currentCount = al.getValue().value;
                        pecentagos[j] = (currentCount*1.)/sum;
                    }
                }else{
                   // ArrayList<Double> sol = softmax(pecentagos);
                    //

                }

                double[] cuma = getCumaliativDistFromPDF(pecentagos);
                toFill.put(shortpattern,new Pair<>(cuma,valueToPlay));

            }
            returnVal.put(i, toFill);

        }


        return returnVal;
    }

    //https://stackoverflow.com/questions/29480842/sample-without-replacement-in-java-with-probabilities
    private static ArrayList<Double> softmax(ArrayList<Double> counts){ //works probably only one dim
        //"""Compute softmax values for each sets of scores in x."""
        //def softmax(x):
        //e_x = np.exp(x - np.max(x))
        //return e_x / e_x.sum(axis=0) # only difference
        double maximum = Collections.max(counts);
        int sum = 0;
        for (double i:counts) {
            sum+=i;
        }
        double e_xsum = 0;
        ArrayList<Double> e_xses = new ArrayList<>();
        for (double i:counts) {
            double e_x = Math.exp(i - maximum);
            e_xses.add(e_x);
            e_xsum += e_x;
        }
        for (int i = 0; i < e_xses.size(); i++) {
            e_xses.set(i, e_xses.get(i)/e_xsum);
        }
        return e_xses;
    }


    private void stopSequence() {
        try{
            sequencer.stop();
        }catch (Exception e){
            e.printStackTrace();
        }

    }

    private void playSequence() {
        MidiSequence[] choises = new MidiSequence[midisequences.size()];
        midisequences.toArray(choises);

        //...and passing `frame` instead of `null` as first parameter
        MidiSequence chosen = (MidiSequence) JOptionPane.showInputDialog(null, "Choose", "Menu", JOptionPane.PLAIN_MESSAGE, null, choises, choises[0]);
        playSequence(chosen.getSequence());

    }

    //https://riptutorial.com/java/example/621/play-a-midi-file
    private void playSequence(Sequence sequence) {
        try {
            sequencer = MidiSystem.getSequencer(); // Get the default Sequencer
            if (sequencer==null) {
                System.err.println("Sequencer device not supported");
                return;
            }
            sequencer.open(); // Open device

            sequencer.setSequence(sequence); // load it into sequencer
            sequencer.start();  // start the playback
        } catch (MidiUnavailableException | InvalidMidiDataException ex) {
            ex.printStackTrace();
        }
    }

    private void saveMelodie() {
        readChannelChoisesFromFile(); //read everytime the default
        String answer = JOptionPane.showInputDialog(null, "Select the bbm", "120");
        int bpm = Integer.parseInt(answer);

        for (MidiSequence ms: midisequences) {
            String fileName = ms.getFileName();
            int[] channels = channelChoises.get(fileName);
            for (int i = 0; i < channels.length; i++) {
                int channel = channels[i];
                ArrayList<MidiSequence.Note> notes = ms.getNotes(channel);
                String[] splittet = fileName.split("\\.");
                String name = splittet[splittet.length - 2];
                MidiSequence.createMidiFile(notes, new File("Output\\all\\" + name + channel + "bpm_" + bpm+ ".mid"),bpm,240);//384);
            }


        }

    }

    private ArrayList<ArrayList<MidiSequence.Note>> melodie(boolean print) {
        ArrayList<ArrayList<MidiSequence.Note>> returnval = new ArrayList<ArrayList<MidiSequence.Note>>();
        int[] channels = new int[]{0};
        boolean channelChoiseFile = false;
        int dialogResult = JOptionPane.showConfirmDialog (null, "Would You Like to use the default channel?","ChannelChoise",JOptionPane.YES_NO_OPTION);
        if(dialogResult == JOptionPane.YES_OPTION){
            channelChoiseFile = true;
        }

        for (MidiSequence ms:midisequences) {
            String fileName = ms.getFileName();
            Sequence sequence = ms.getSequence();

            if (channelChoiseFile){
                channels = channelChoises.get(fileName);
            }else{
                String answer = JOptionPane.showInputDialog(null, "Define the channel for file " + fileName, "Channel", JOptionPane.INFORMATION_MESSAGE);
                channels = new int[]{Integer.parseInt(answer)};
            }
            for (int c:channels) {
                ArrayList<MidiSequence.Note> notes = ms.getNotes(c);
               MidiSequence.processMelodie(notes);
                if (print) System.out.println(notes.size() + " notes played");
                if (print) MidiSequence.printBackwarts(notes);
                returnval.add(notes);
            }

            System.out.println("Microsecondlength: " + sequence.getMicrosecondLength());
            System.out.println("DevisionType: " + sequence.getDivisionType());
            System.out.println("Resolution: " + sequence.getResolution());
            System.out.println("TickLength : " + sequence.getTickLength());
            System.out.println("Name : " + ms.getFileName());
        }

        return returnval;
    }

    private void ngram() {
        int n_gramSize = 1;
        boolean levelingOfKey = false;
        String answer = JOptionPane.showInputDialog(null, "Define the N", "N-Gram", JOptionPane.INFORMATION_MESSAGE);
       n_gramSize = Integer.parseInt(answer);
        int dialogResult = JOptionPane.showConfirmDialog (null, "Would You Like to Save your Previous Note First?","Keyequaltiy",JOptionPane.YES_NO_OPTION);
        if(dialogResult == JOptionPane.YES_OPTION){
            levelingOfKey = true;
        }
    }

    private interface ButtonMethod{
        public void function(String s);
    }
    public static void addButton(JPanel panel, String buttonName, String toolTipText, ButtonMethod o) {
        JButton jb = new JButton(buttonName);
        jb.addActionListener(e->o.function(""));
        jb.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        if (toolTipText != null) jb.setToolTipText(toolTipText);
        panel.add(jb);
    }
    public static BitSet readSmallBinaryFile(String fileName)  {
        java.nio.file.Path path = Paths.get(fileName);


        try {
            return BitSet.valueOf(Files.readAllBytes(path));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void load(){
        String path = null;
        JFileChooser fileChooser = new JFileChooser();
        String currentDir = "Input";
        fileChooser.setDialogTitle("Select Folder with midis or choose .mid files");
        fileChooser.setCurrentDirectory(new File(currentDir));
        fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        fileChooser.setMultiSelectionEnabled(true);
        //
        // disable the "All files" option.
        //
        //fileChooser.setAcceptAllFileFilterUsed(false);
        File[] files = null;
        if (fileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
            //path = fileChooser.getSelectedFile().getAbsolutePath();
             files = fileChooser.getSelectedFiles();
            //s = file.getName();
        }
       if (files == null) return;
        if (files[0].isDirectory()){
            files = getFilesRec(files[0]);
        }
        midisequences = load(files);
    }

    private static File[] getFilesRec(File file) {
        ArrayList<File> files = listFilesForFolder(file, null);
        File[] arr = new File[files.size()];
        files.toArray(arr);
        return arr;
    }

    public static ArrayList<File> listFilesForFolder(final File folder,ArrayList<File> files) {
        if (files == null) files = new ArrayList<>();
        for (final File fileEntry : folder.listFiles()) {
            if (fileEntry.isDirectory()) {
                listFilesForFolder(fileEntry,files);
            } else {
                files.add(fileEntry);
            }
        }
        return files;
    }

    public static ArrayList<MidiSequence> load(File[] files) {
       // File file = new File(path);
        //boolean exists =      file.exists();      // Check if the file exists
        //boolean isDirectory = file.isDirectory();
        /*if (!isDirectory){
            try {
                FileInputStream fileIn = new FileInputStream(path);
                ObjectInputStream in = new ObjectInputStream(fileIn);
                MidiSequence h = (MidiSequence) in.readObject();
                in.close();
                fileIn.close();
                return h;
            } catch (IOException i) {
                logger.log(Level.SEVERE,"IO exception:" + i.getMessage());
                i.printStackTrace();
                return null;
            } catch (ClassNotFoundException c) {
                logger.log(Level.SEVERE,"Histogramm class not found:" + c.getMessage());

                c.printStackTrace();
                return null;
            }
        }else{*/

            //split if file is dlr or hyperion
        long start = System.currentTimeMillis();
            ArrayList<MidiSequence> ms = new ArrayList<>();
            for (int i = 0; i < files.length; i++) {
                //File fileEntry = folder.listFiles()[i];
                String[] split = files[i].getName().split("\\.");
                String suffix = split[split.length-1];
                if (suffix.equalsIgnoreCase("mid")) ms.add(new MidiSequence(files[i],false));
                else continue;
                Sequence lastSequence = ms.get(ms.size() - 1).getSequence();
                System.out.println("Microsecondlength: " + lastSequence.getMicrosecondLength());
                System.out.println("DevisionType: " + lastSequence.getDivisionType());
                System.out.println("Resolution: " + lastSequence.getResolution());
                System.out.println("TickLength : " + lastSequence.getTickLength());
                System.out.println("Name : " + ms.get(ms.size() - 1).getFileName());
            }
            long stop = System.currentTimeMillis();
            long dur = stop-start;
            System.out.println("It took: " + dur + " to load " + ms.size() + " midifiles");
            return ms;//new RawData3d(path,isDirectory,true);

        }


    public String save(boolean useFileChooser) {
        String path = getPath(useFileChooser);

        save(path);

        return path;
    }

    public void save(String path) {
        try {

            FileOutputStream fileOut =
                    new FileOutputStream(path);
            ObjectOutputStream out = new ObjectOutputStream(fileOut);
            out.writeObject(this);
            out.close();
            fileOut.close();
            logger.log(Level.INFO,path);
        } catch (IOException i) {
            i.printStackTrace();
        }
    }

    public static String getPath(boolean useFileChooser) {
        String path = null;
        if (useFileChooser){

            JFileChooser fileChooser = new JFileChooser();
            String currentDir = "Input";
            fileChooser.setCurrentDirectory(new File(currentDir));
            if (fileChooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
                path = fileChooser.getSelectedFile().getAbsolutePath();
                //s = file.getName();
            }

        }
        if (path == null) path = "Input" +   File.separator + createName() + ".mid";
        return path;
    }

    private static String createName() {
        return null;
    }
}
