package gmm.service.tasks;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.lang.ProcessBuilder.Redirect;
import java.net.ConnectException;
import java.net.Socket;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import gmm.service.data.DataConfigService;

@Service
public class PythonTCPSocket {
	
	private final Logger logger = LoggerFactory.getLogger(getClass());
	
	@Autowired private DataConfigService config;
	
	private static final int port = 8090;
	private static final int tryCount = 10;
	private static final int trySleep = 100;
	
	private static final String conversionStart = "CONVERSION_START";
	private static final String conversionEnd = "CONVERSION_END";
	private static final String conversionFrom = "FROM";
	private static final String conversionTo = "TO";
	
	public void createPreview(Path original, Path target) {
		
		final Path scriptPath = config.getBlenderPythonScript();
		final Path scriptRelative = config.blender.relativize(scriptPath);
		try {
			final ProcessBuilder pb = new ProcessBuilder(
					"\"" + config.blender.resolve("blender") + "\"",
					"--background",
					"--python",
					"\"" + scriptRelative + "\"",
					"--");// append script args here
			pb.directory(config.blender.toFile());
			pb.redirectOutput(Redirect.INHERIT);
			pb.redirectError(Redirect.INHERIT);
			pb.start();
		} catch (final IOException e1) {
			throw new UncheckedIOException(e1);
		}
		
		int currentTry = 1;
		boolean success = false;
		while(currentTry <= tryCount && !success) {
			try(Socket socket = new Socket("localhost", port)) {
				
				final Charset charset = StandardCharsets.UTF_8;
				final InputStreamReader sr = new InputStreamReader(socket.getInputStream(), charset);
				final BufferedReader r = new BufferedReader(sr);
				final OutputStreamWriter sw = new OutputStreamWriter(socket.getOutputStream(), charset);
				final PrintWriter w = new PrintWriter(sw, true) {
					@Override
					public void println(String x) {
						super.print(x + "\n");
						flush();
					}
				};

				w.println(conversionStart);
				w.println(conversionFrom);
				w.println(original.toString());
				w.println(conversionTo);
				w.println(target.toString());
				w.println(conversionEnd);
				
				socket.setSoTimeout(60000);
				// python server will send something to notify success
				r.readLine(); 
				success = true;
			}
			catch (final ConnectException e) {
				final String message = "Connect failed, trying to reconnect.";
				logger.info(message);
				logger.debug(message, e);
				try {
					Thread.sleep(trySleep);
				} catch (InterruptedException e1) {
					logger.error(e1.getMessage(), e1);
				}
				currentTry++;
			}
			catch (final IOException e) {
				throw new UncheckedIOException(e);
			}
		}
		if(!success) {
			throw new UncheckedIOException(new IOException("Could not connect to python server "
					+ "(tried " + tryCount + " times and failed each time)!"));
		}
	}
	
	public static void main(String[] args) {
		new PythonTCPSocket();
	}
}
