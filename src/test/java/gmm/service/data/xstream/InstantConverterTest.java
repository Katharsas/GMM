package gmm.service.data.xstream;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;

import org.junit.Test;

public class InstantConverterTest {

	@Test
	public void testUtilDaterParser() {
		final String[] toParse = new String[] {
				"2017-10-11 22:19:51.358 UTC",
				"2017-10-11 22:19:51.35 UTC",
				"2017-10-11 22:19:51.3 UTC"};
		
		final DateTimeFormatter formatter = new DateTimeFormatterBuilder()
				.appendPattern("yyyy-MM-dd HH:mm:ss.[SSS][SS][S] z")
				.toFormatter();
		
		for (final String time : toParse) {
			final Instant i = Instant.from(formatter.parse(time));
			System.out.println(i);
		}
	}
}
