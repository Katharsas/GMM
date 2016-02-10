package gmm.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.net.Socket;
import java.nio.charset.Charset;

public class PythonTCPSocket {
	
	private Socket socket;
	
	public PythonTCPSocket() {
		
		
		
		
		
		try {
			this.socket = new Socket("localhost", 8090);
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

			String message = "κόσμε";

			w.println(message);
			w.flush();

			String result = r.readLine();
			System.out.println(result);

			message = "⬙⬙⬙";

			w.println(message);
			w.flush();

			result = r.readLine();
			System.out.println(result);

			socket.close();
		}
		catch (final IOException e) {
			throw new UncheckedIOException(e);
		}
	}
	
	public static void main(String[] args) {
		new PythonTCPSocket();
	}
}
