package gmm.service.tasks;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.lang.ProcessBuilder.Redirect;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.Charset;
import java.nio.file.Path;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import gmm.service.data.DataConfigService;

@Service
public class PythonTCPSocket {
	
	@Autowired private DataConfigService config;
	
	private static final int port = 8090;
	
	private static final String conversionStart = "CONVERSION_START";
	private static final String conversionEnd = "CONVERSION_END";
	private static final String conversionFrom = "FROM";
	private static final String conversionTo = "TO";
	
	public boolean createPreview(Path original, Path target) {
		
		final Path scriptPath = config.getBlenderPythonScript();
		final String command = config.blender.resolve("blender") + " --background --python "
				+ scriptPath + " -- ";// append script args here
		try {
			final ProcessBuilder pb = new ProcessBuilder(command);
			pb.redirectOutput(Redirect.INHERIT);
			pb.redirectError(Redirect.INHERIT);
			pb.start();
		} catch (final IOException e1) {
			throw new UncheckedIOException(e1);
		}
		
		try(Socket socket = new Socket("localhost", port)) {
			
			final Charset charset = Charset.forName("UTF-8");
			final InputStreamReader sr = new InputStreamReader(socket.getInputStream(), charset);
			final BufferedReader r = new BufferedReader(sr);
			final OutputStreamWriter sw = new OutputStreamWriter(socket.getOutputStream(), charset);
			final PrintWriter w = new PrintWriter(sw, false) {
				@Override
				public void println(Object x) {
					super.print(x + "\n");
				}
			};

			w.println(conversionStart);
			w.println(conversionFrom);
			w.println(original.toString());
			w.println(conversionTo);
			w.println(target.toString());
			w.println(conversionEnd);
			
			socket.setSoTimeout(60000);
			try {
				r.readLine();
				return true;
			} catch(final SocketTimeoutException e) {
				return false;
			}
		}
		catch (final IOException e) {
			throw new UncheckedIOException(e);
		}
	}
	
	public static void main(String[] args) {
		new PythonTCPSocket();
	}
}
