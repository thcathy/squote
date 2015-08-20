import java.io.File;

import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.mp3.MP3AudioHeader;
import org.jaudiotagger.audio.mp3.MP3File;
import org.jaudiotagger.tag.Tag;

public class ID3ReaderTest {
	public static void main(String[] args) {
		try {
			MP3File f = (MP3File) AudioFileIO
					.read(new File(
							"C:/Users/wongtim/dev/git_repo/squote/src/test/resources/ellie-goulding-love-me-like-you-do-cosmic-radio-edit-3078600441[mp3freex].mp3"));
			MP3AudioHeader audioHeader = (MP3AudioHeader) f.getAudioHeader();
			System.out.println(audioHeader.getTrackLength());
			System.out.println(audioHeader.getSampleRateAsNumber());
			System.out.println(audioHeader.getChannels());
			System.out.println(audioHeader.isVariableBitRate());

			System.out.println(audioHeader.getTrackLengthAsString());
			System.out.println(audioHeader.getMpegVersion());
			System.out.println(audioHeader.getMpegLayer());
			System.out.println(audioHeader.isOriginal());
			System.out.println(audioHeader.isCopyrighted());
			System.out.println(audioHeader.isPrivate());
			System.out.println(audioHeader.isProtected());
			System.out.println(audioHeader.getBitRate());
			System.out.println(audioHeader.getEncodingType());
			System.out.println("--------------------------------------");
			
			Tag tag        = f.getTag();
			
		} catch (Exception ex) {
			System.out
					.println("An error occurred while reading/saving the mp3 file.");
			ex.printStackTrace();
		}
	}
}
