import java.io.IOException;

import org.apache.commons.lang3.ArrayUtils;

import com.mpatric.mp3agic.AbstractID3v2Tag;
import com.mpatric.mp3agic.ID3v2;
import com.mpatric.mp3agic.InvalidDataException;
import com.mpatric.mp3agic.Mp3File;
import com.mpatric.mp3agic.NotSupportedException;
import com.mpatric.mp3agic.UnsupportedTagException;

public class ID3ReaderTest {
	public static void main(String[] args) throws UnsupportedTagException, InvalidDataException, IOException, NotSupportedException {
        Mp3File mp3file = new Mp3File("/Users/thcathy/git/squote/src/test/resources/01 - 你最重要.mp3");

        System.out.println("Length of this mp3 is: " + mp3file.getLengthInSeconds() + " seconds");
        System.out.println("Bitrate: " + mp3file.getBitrate() + " kbps " + (mp3file.isVbr() ? "(VBR)" : "(CBR)"));
        System.out.println("Sample rate: " + mp3file.getSampleRate() + " Hz");
        System.out.println("Has ID3v1 tag?: " + (mp3file.hasId3v1Tag() ? "YES" : "NO"));
        System.out.println("Has ID3v2 tag?: " + (mp3file.hasId3v2Tag() ? "YES" : "NO"));
        System.out.println("Has custom tag?: " + (mp3file.hasCustomTag() ? "YES" : "NO"));
              
        ID3v2 id3v2Tag;
        id3v2Tag =  mp3file.getId3v2Tag();
        
        byte[] utfBytes = new String(ArrayUtils.remove(id3v2Tag.getFrameSets().get(AbstractID3v2Tag.ID_ARTIST).getFrames().get(0).getData(), 0),"BIG5").trim().getBytes("UTF-8");
        id3v2Tag.getFrameSets().get(AbstractID3v2Tag.ID_ARTIST).getFrames().get(0).setData(ArrayUtils.add(utfBytes, 0, (byte)3));
        utfBytes = new String(ArrayUtils.remove(id3v2Tag.getFrameSets().get(AbstractID3v2Tag.ID_TITLE).getFrames().get(0).getData(), 0),"BIG5").trim().getBytes("UTF-8");
        id3v2Tag.getFrameSets().get(AbstractID3v2Tag.ID_TITLE).getFrames().get(0).setData(ArrayUtils.add(utfBytes, 0, (byte)3));
        utfBytes = new String(ArrayUtils.remove(id3v2Tag.getFrameSets().get(AbstractID3v2Tag.ID_ALBUM).getFrames().get(0).getData(), 0),"BIG5").trim().getBytes("UTF-8");
        id3v2Tag.getFrameSets().get(AbstractID3v2Tag.ID_ALBUM).getFrames().get(0).setData(ArrayUtils.add(utfBytes, 0, (byte)3));
        //utfBytes = new String(ArrayUtils.remove(id3v2Tag.getFrameSets().get(AbstractID3v2Tag.ID_PUBLISHER).getFrames().get(0).getData(), 0),"BIG5").getBytes("UTF-8");
        //id3v2Tag.getFrameSets().get(AbstractID3v2Tag.ID_PUBLISHER).getFrames().get(0).setData(ArrayUtils.add(utfBytes, 0, (byte)3));
        //utfBytes = new String(ArrayUtils.remove(id3v2Tag.getFrameSets().get(AbstractID3v2Tag.ID_ALBUM_ARTIST).getFrames().get(0).getData(), 0),"BIG5").getBytes("UTF-8");
        //id3v2Tag.getFrameSets().get(AbstractID3v2Tag.ID_ALBUM_ARTIST).getFrames().get(0).setData(ArrayUtils.add(utfBytes, 0, (byte)3));
        
        mp3file.setId3v2Tag(id3v2Tag);
        mp3file.save("/tmp/mp3/01.mp3");
        
        Mp3File mp3fileUTF = new Mp3File("/tmp/mp3/01.mp3");
        if (mp3file.hasId3v2Tag()) {
        	id3v2Tag = mp3fileUTF.getId3v2Tag();
        	System.out.println("Track: " + id3v2Tag.getTrack());
        	System.out.println("Artist: " + id3v2Tag.getArtist());
        	System.out.println("Title: " + id3v2Tag.getTitle());
        	System.out.println("Album: " + id3v2Tag.getAlbum());
        	System.out.println("Year: " + id3v2Tag.getYear());
        	System.out.println("Genre: " + id3v2Tag.getGenre() + " (" + id3v2Tag.getGenreDescription() + ")");
        	System.out.println("Comment: " + id3v2Tag.getComment());
        	System.out.println("Composer: " + id3v2Tag.getComposer());
        	System.out.println("Publisher: " + id3v2Tag.getPublisher());
        	System.out.println("Original artist: " + id3v2Tag.getOriginalArtist());
        	System.out.println("Album artist: " + id3v2Tag.getAlbumArtist());
        	System.out.println("Copyright: " + id3v2Tag.getCopyright());
        	System.out.println("URL: " + id3v2Tag.getUrl());
        	System.out.println("Encoder: " + id3v2Tag.getEncoder());
        }
	}
}
