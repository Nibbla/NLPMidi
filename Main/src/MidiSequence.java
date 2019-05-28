import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.*;

import javax.sound.midi.*;

/**
 * inspired by https://stackoverflow.com/questions/3850688/reading-midi-files-in-java
 */
public class MidiSequence {
    public static final int NOTE_ON = 0x90;
    public static final int NOTE_OFF = 0x80;
    public static final String[] NOTE_NAMES = {"C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"};
    private final String fileName;
    private Sequence sequence;
    private ArrayList<Note> notes;

    public static class MutableInt {
        int value = 1; // note that we start at 1 since we're counting
        public void increment () { ++value;      }
        public int  get ()       { return value; }
    }
    public static HashMap<LinkedList<Integer>, MutableInt> createProbabilityMatrix(ArrayList<ArrayList<Note>> allMelodies, int numberNotes, boolean rythmIfTrueNormKeyIfFalse, boolean normKeyIfTrueJustKeyIfFalse) {

        HashMap<LinkedList<Integer>,MutableInt> counters = new HashMap<>(); //using linked list, as equals compares the contents of the list and not the identity(memory location)
        for (ArrayList<Note> songMeloduis : allMelodies) {
            LinkedList<Integer> chunk = new LinkedList<Integer>();

            for (Note not:songMeloduis) {



                if (rythmIfTrueNormKeyIfFalse)chunk.add(not.lengthTickNormalizedRezolution);
                else if (normKeyIfTrueJustKeyIfFalse) chunk.add(not.keyNormalized);
                else chunk.add(not.key);

                if (chunk.size()== numberNotes){
                    MutableInt count = counters.get(chunk);
                    if (count == null) {
                        counters.put(chunk, new MutableInt());
                    }
                    else {
                        count.increment();
                    }
                    chunk = (LinkedList<Integer>) chunk.clone();
                    chunk.removeFirst();
                }
            }

        }
        return counters;

    }

    public String toString(){
        return fileName;
    }
    public MidiSequence(File file, boolean print) {
        String[] splittet = file.getPath().split(File.separator + File.separator);
        this.fileName = splittet[splittet.length-1];
        sequence = null;
        try {
            sequence = MidiSystem.getSequence(file);//new File("Input\\Beatles_Baby_Its_You.mid"));
        } catch (InvalidMidiDataException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Loading: " + fileName);
        int trackNumber = 0;
        for (Track track :  sequence.getTracks()) {
            trackNumber++;
            if (print)System.out.println("Track " + trackNumber + ": size = " + track.size());
            if (print) System.out.println();
            for (int i=0; i < track.size(); i++) {
                MidiEvent event = track.get(i);
                if (print) System.out.print("@" + event.getTick() + " ");
                MidiMessage message = event.getMessage();
                if (message instanceof ShortMessage) {
                    ShortMessage sm = (ShortMessage) message;
                    if (print)  System.out.print("Channel: " + sm.getChannel() + " ");
                    if (sm.getCommand() == NOTE_ON) {
                        int key = sm.getData1();
                        int octave = (key / 12)-1;
                        int note = key % 12;
                        String noteName = NOTE_NAMES[note];
                        int velocity = sm.getData2();
                      if (print)  System.out.println("Note on, " + noteName + octave + " key=" + key + " velocity: " + velocity);
                    } else if (sm.getCommand() == NOTE_OFF) {
                        int key = sm.getData1();
                        int octave = (key / 12)-1;
                        int note = key % 12;
                        String noteName = NOTE_NAMES[note];
                        int velocity = sm.getData2();
                        if (print)  System.out.println("Note off, " + noteName + octave + " key=" + key + " velocity: " + velocity);
                    } else {
                        if (print)  System.out.println("Command:" + sm.getCommand());
                    }
                } else {
                    if (print)  System.out.println("Other message: " + message.getClass());
                }
            }

            if (print)System.out.println();
        }


    }

    public static void createMidiFile(ArrayList<Note> notes, File file, int bbm, int resolution){
        System.out.println("midifile begin ");
        try
        {
//****  Create a new MIDI sequence with 24 ticks per beat  ****
            Sequence s = new Sequence(javax.sound.midi.Sequence.PPQ,resolution);

//****  Obtain a MIDI track from the sequence  ****
            Track t = s.createTrack();

//****  General MIDI sysex -- turn on General MIDI sound set  ****
            byte[] b = {(byte)0xF0, 0x7E, 0x7F, 0x09, 0x01, (byte)0xF7};
            SysexMessage sm = new SysexMessage();
            sm.setMessage(b, 6);
            MidiEvent me = new MidiEvent(sm,(long)0);
            t.add(me);

//****  set tempo (meta event)  ****
            int time = 60000000 / bbm;
            ByteBuffer buf = ByteBuffer.allocate(100);
            buf.putInt(time);
            buf.flip();
            byte[] arr = buf.array();
            byte one = arr[1]; //works dont think about it, why there is a leading 0
            byte two = arr[2];
            byte three = arr[3];
            System.out.print(" { ");
            for (int i = 0 ; i < buf.limit() ; i++)
               // System.out.printf("0x%X, ", arr[i]);
            System.out.println("}");
                    MetaMessage mt = new MetaMessage();
            byte[] bt = {one, two, three};
            mt.setMessage(0x51 ,bt, 3);
            me = new MidiEvent(mt,(long)0);
            t.add(me);

//****  set track name (meta event)  ****
            mt = new MetaMessage();
            String TrackName = new String("midifile track");
            mt.setMessage(0x03 ,TrackName.getBytes(), TrackName.length());
            me = new MidiEvent(mt,(long)0);
            t.add(me);

//****  set omni on  ****
            ShortMessage mm = new ShortMessage();
            mm.setMessage(0xB0, 0x7D,0x00);
            me = new MidiEvent(mm,(long)0);
            t.add(me);

//****  set poly on  ****
            mm = new ShortMessage();
            mm.setMessage(0xB0, 0x7F,0x00);
            me = new MidiEvent(mm,(long)0);
            t.add(me);

//****  set instrument to Piano  ****
            mm = new ShortMessage();
            mm.setMessage(0xC0, 0x00, 0x00);
            me = new MidiEvent(mm,(long)0);
            t.add(me);

            long lastActionTime = addMessagesFromNotesToMidi(t, notes,resolution);


//****  set end of track (meta event) 19 ticks later  ****
            mt = new MetaMessage();
            byte[] bet = {}; // empty array
            mt.setMessage(0x2F,bet,0);
            me = new MidiEvent(mt, (long)lastActionTime+19);
            t.add(me);

//****  write the MIDI sequence to a MIDI file  ****
           // File f = new File("output/midifile.mid");
            MidiSystem.write(s,1,file);
        } //try
        catch(Exception e)
        {
            System.out.println("Exception caught " + e.toString());
        } //catch
        System.out.println("midifile end ");
    } //main

    private static long addMessagesFromNotesToMidi(Track t, ArrayList<Note> notes,int resolution) {

        long lastTime =  0;
//****  note off - middle C - 120 ticks later  ****

        try {
        for (Note n:notes) {
            if (n.key==-1)continue;
            ShortMessage mm = new ShortMessage();

            mm.setMessage(0x90,n.key,n.startVelocity);
            long test1 =n.startTime;
                    long test2 =(long) (n.startTick*resolution);
            long test3 =n.stopTime;
            long test4 =(long) (n.stopTick*resolution);
            MidiEvent me = new MidiEvent(mm, (long) (n.startTick*resolution));
            //lastTime = n.startTime;
            t.add(me);

            mm = new ShortMessage();
            mm.setMessage(0x80,n.key,n.stopVelocity);
            me = new MidiEvent(mm,(long) (n.stopTick*resolution));
            lastTime = n.stopTime;
            t.add(me);
        }
        } catch (InvalidMidiDataException e) {
            e.printStackTrace();
        }
        return lastTime;
    }


    public static void print(ArrayList<Note> notes) {
        for (Note n: notes) {
            System.out.println(n.toString());
        }
    }

    public static void printBackwarts(ArrayList<Note> notes) {
        int s = notes.size();
        for (int i = s-1; i >= 0; i--) {
            Note n = notes.get(i);
            System.out.println(n.toString());
        }

    }

    public static void processMelodie( ArrayList<Note> notes){
        if (notes.size()==0) return;
        Note finalNote = notes.get(notes.size() - 1);
        int key = finalNote.key;
        for (Note n:notes) {
            n.normalizeKey(key);
        }

        addBrakes(notes);
    }

    private static void addBrakes(ArrayList<Note> notes) {
        long lastTime = 0;
        long firstTime = Long.MAX_VALUE;

        for (Note n: notes) {
            if (n.stopTime > lastTime){
                lastTime = n.stopTime;
            }
            if (n.startTime < firstTime){
                firstTime = n.startTime;
            }
            if (lastTime > Integer.MAX_VALUE){
                System.out.println("TOOO LOOONG");
            }
        }
        int lastTimeInt = (int) lastTime;
        BitSet allNoises = new BitSet();
        for (Note n: notes) {
            allNoises.set((int)n.startTime, (int)n.stopTime);
        }
        int resolution = notes.get(0).resolution;
        int counter = 0;
        boolean lookForSet = false;
        ArrayList<Note> breaks = new ArrayList<>(100);
        Note currentBreak = null;
        //if (!allNoises.get(0)){ not needed hopefully
        //    currentBreak = new Note(-1, 0, -1, -1, "break", 0, resolution);
        //    lookForSet = true;
        //}
        int minBreakLength = resolution/8; //only care for things longer then 32th
        while (counter <=lastTimeInt&&counter!=-1){
            System.out.println(counter+"/" + lastTimeInt);
            if (lookForSet) {
                int next = allNoises.nextSetBit(counter);
                counter = next;
                currentBreak.stop(counter-1, 0);
                if (currentBreak.stopTime-currentBreak.startTime > minBreakLength)
                    breaks.add(currentBreak);
                currentBreak=null;
                lookForSet = false;

            }else{
                int next = allNoises.nextClearBit(counter);
                counter = next;
                currentBreak = new Note(-1, counter, -1, -1, "break", 0, resolution);
                lookForSet = true;

            }
        }
        notes.addAll(breaks);
        Comparator<Note> compareByStart = Comparator.comparing(e -> e.startTime);
        Collections.sort(notes,compareByStart);
    }
    public static BufferedImage createImage(ArrayList<Note> notes, int width, int height, boolean colorMode) {
        long startTime = notes.get(0).startTime;
        long endTime = 0;
        for (int i = 0; i < notes.size(); i++) {
            if (endTime < notes.get(i).stopTime)endTime=notes.get(i).stopTime;
        }
        long lengthTime = endTime - startTime;
        int numberOfNotes = notes.size();
        double widthPerTime = (width*1.)/lengthTime;
        double heightPerTime = (height*1.)/lengthTime;
        BufferedImage bi = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        int rgb= Color.BLUE.getRGB();

        for (int i = 0; i < numberOfNotes; i++) {
            for (int j = i; j < numberOfNotes; j++) {
                Note n1 = notes.get(i);
                Note n2 = notes.get(j);
                Color c = null;
                if (n1.key == -1 || n2.key == -1) {
                    c = Color.white;
                } else{
                    //the limit are the 128 notes of Midi, a bit more than 10 octaves
                    float keyDif = (float) (((n1.key - n2.key+6)%12)/12.0);
                    float octaveDif = (float) (1-((n1.octave - n2.octave)%11)/11.0);
                    if (colorMode)
                    c = Color.getHSBColor(keyDif, 1.f, octaveDif);
                    else{
                    int dif = (int)(keyDif*12);
                    if (dif == 6)
                        c = Color.black;
                    else c = Color.white;
                    }
                }
                rgb = c.getRGB();
                int note1Start = Math.max(0, (int) (n1.startTime * widthPerTime));
                int note1End = Math.min(width-1,(int) (n1.stopTime * widthPerTime));
                int note2Start = Math.max(0,(int) (n2.startTime * heightPerTime));
                int note2End = Math.min(height-1,(int) (n2.stopTime * heightPerTime));

                int note1mirStart = Math.max(0,(int) (n1.startTime * heightPerTime));
                int note1mirEnd = Math.min(height-1,(int) (n1.stopTime * heightPerTime));
                int note2mirStart = Math.max(0,(int) (n2.startTime * widthPerTime));
                int note2mirEnd =  Math.min(width-1,(int) (n2.stopTime * widthPerTime));

                for (int x = note1Start; x <= note1End; x++) {
                    for (int y = note2Start; y <= note2End; y++) {
                        bi.setRGB(x, y, rgb);
                    }
                }
                for (int x = note2mirStart; x <= note2mirEnd; x++) {
                    for (int y = note1mirStart; y <= note1mirEnd; y++) {
                        bi.setRGB(x, y, rgb);
                    }
                }
            }
        }



        return bi;
    }
    public ArrayList<Note> getNotes(int channel) {
        int resolution = sequence.getResolution();
        ArrayList<Note> notes = new ArrayList<>();
        ArrayList<Note> unstored = new ArrayList<>();
        int trackNumber = 0;
        outer:
        for (Track track : sequence.getTracks()) {
            trackNumber++;
            inner:
            for (int i = 0; i < track.size(); i++) {
                MidiEvent event = track.get(i);
                MidiMessage message = event.getMessage();
                if (message instanceof ShortMessage) {
                    ShortMessage sm = (ShortMessage) message;
                    if (channel != sm.getChannel()) continue inner;
                    if (sm.getCommand() == NOTE_ON) {
                        int key = sm.getData1();
                        int octave = (key / 12) - 1;
                        int note = key % 12;
                        String noteName = NOTE_NAMES[note];
                        int velocity = sm.getData2();
                        if (velocity != 0){
                            Note n = new Note(key, event.getTick(),octave, note, noteName, velocity,resolution);
                            unstored.add(n);
                        }else{
                            Note unstoredNote = null;
                            for (Note u:unstored) {
                                if (u.key == key) {
                                    unstoredNote = u;
                                    break;
                                }

                            }
                            unstored.remove(unstoredNote);
                            unstoredNote.stop(event.getTick(),velocity);
                            notes.add(unstoredNote);
                        }

                        //System.out.println("Note on, " + noteName + octave + " key=" + key + " velocity: " + velocity);
                    } else if (sm.getCommand() == NOTE_OFF) {
                        int key = sm.getData1();
                        int octave = (key / 12) - 1;
                        int note = key % 12;
                        String noteName = NOTE_NAMES[note];
                        int velocity = sm.getData2();
                        Note unstoredNote = null;
                        for (Note u:unstored) {
                            if (u.key == key) {
                                unstoredNote = u;
                                break;
                            }
                        }
                        unstored.remove(unstoredNote);
                        unstoredNote.stop(event.getTick(),velocity);
                        notes.add(unstoredNote);
                        //System.out.println("Note off, " + noteName + octave + " key=" + key + " velocity: " + velocity);
                    } else {
                        //System.out.println("Command:" + sm.getCommand());
                    }
                } else {
                    //System.out.println("Other message: " + message.getClass());
                }
            }
        }
        return notes;
    }

    public static void main(String[] args) throws Exception {
        Sequence sequence = MidiSystem.getSequence(new File("Input\\Beatles_Baby_Its_You.mid"));

        int trackNumber = 0;
        for (Track track :  sequence.getTracks()) {
            trackNumber++;
            System.out.println("Track " + trackNumber + ": size = " + track.size());
            System.out.println();

            for (int i=0; i < track.size(); i++) {
                MidiEvent event = track.get(i);
                System.out.print("@" + event.getTick() + " ");
                MidiMessage message = event.getMessage();
                if (message instanceof ShortMessage) {
                    ShortMessage sm = (ShortMessage) message;
                    System.out.print("Channel: " + sm.getChannel() + " ");
                    if (sm.getCommand() == NOTE_ON) {
                        int key = sm.getData1();
                        int octave = (key / 12)-1;
                        int note = key % 12;
                        String noteName = NOTE_NAMES[note];
                        int velocity = sm.getData2();
                        System.out.println("Note on, " + noteName + octave + " key=" + key + " velocity: " + velocity);
                    } else if (sm.getCommand() == NOTE_OFF) {
                        int key = sm.getData1();
                        int octave = (key / 12)-1;
                        int note = key % 12;
                        String noteName = NOTE_NAMES[note];
                        int velocity = sm.getData2();
                        System.out.println("Note off, " + noteName + octave + " key=" + key + " velocity: " + velocity);
                    } else {
                        System.out.println("Command:" + sm.getCommand());
                    }
                } else {
                    System.out.println("Other message: " + message.getClass());
                }
            }

            System.out.println();
        }

    }

    public String getFileName() {
        return fileName;
    }

    public Sequence getSequence() {
        return sequence;
    }

    static class Note {
        private  int resolution;
        private  double startTick;
        private  int octave;
        private  int key;
            private  long startTime;
            private  int startVelocity;
            private  String noteName;
            private long stopTime;
            private int stopVelocity;
            private int keyNormalized;
        private double stopTick;
        private int lengthTickNormalizedRezolution;

        private Note() {

        }


        @Override
            public String toString() {
                return noteName + " " + key + " " + keyNormalized + " " + startTime + " " + stopTime;
            }

            public static Note getInstance(int key, double startTickNorm, int lengthNorm, int resolution){
                Note n = new Note();
                n.key = key;
                n.startTick=startTickNorm;
                n.stopTick = startTickNorm+(lengthNorm*1./192);
                n.startVelocity = 100;
                n.stopVelocity = 0;
                return n;
            }

            public Note(int key, long time, int octave, int note, String noteName, int velocity, int resolution) {
                this.key = key;
                this.noteName = noteName;
                this.octave = octave;
                this.startTime = time;
                this.startTick = (time*1.)/resolution;
                this.startVelocity = velocity;
                this.keyNormalized = key;
                this.resolution = resolution;
            }

            public void stop(long time, int velocity) {
                this.stopTime = time;
                this.stopTick = (time*1.)/resolution;
                this.stopVelocity = velocity;
                this.lengthTickNormalizedRezolution = (int) Math.round((stopTick-startTick)*192); //96 because smallest unit is 32ts and there are tripplets and 3/2halfs
            }

            public Note clone(){
                Note n = new Note(key, Math.round(startTime*resolution), octave, 0,noteName,startVelocity,resolution);
                n.stop(Math.round(stopTime*resolution),stopVelocity);
                n.keyNormalized = keyNormalized;
                n.lengthTickNormalizedRezolution = lengthTickNormalizedRezolution;
                return n;
            }

            //store the note realative to the key
            public void normalizeKey(int key){
                this.keyNormalized = this.key-key;
            }

        public double getEndTick() {
            return this.stopTick;
        }
    }
}

