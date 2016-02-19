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
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import gmm.service.data.DataConfigService;

/**
 * This service is responsible for converting 3DS files to JSON 3D files using blender.
 * Its does this by establishing a TCP connection to a python socket server and sending the
 * paths over TCP.<br>
 * The python script responsible for starting the socket server and communicating with blender
 * is also started by this service.<br>
 * <br>
 * Internally the service uses a thread that manages the connection and exits if there are
 * no more paths to convert for a specified time. On exiting the thread will close the TCP
 * connection (python / blender will exit too). Thread will be restarted when needed.
 * 
 * @author Jan Mothes
 */
@Service
public class PythonTCPSocket {
	
	private final Logger logger = LoggerFactory.getLogger(getClass());
	
	/**
	 * Simple wrapper for two paths (source file and target path for converted file).
	 * @author Jan Mothes
	 */
	private static class AssetConversionPaths {
		public final Path original;
		public final Path target;
		public AssetConversionPaths(Path original, Path target) {
			this.original = original;
			this.target = target;
		}
	}
	
	/**
	 * Simple wrapper for returning exceptions from thread.
	 * @author Jan Mothes
	 */
	private static class ConversionResult {
		public final boolean success;
		public final MeshData meshData;
		public final RuntimeException exception;
		public ConversionResult(boolean success, MeshData meshData, RuntimeException exception) {
			this.success = success;
			this.meshData = meshData;
			this.exception = exception;
		}
	}
	
	protected static class MeshData {
		private int polygonCount;
		public int getPolygonCount() {
			return polygonCount;
		}
	}
	
	@Autowired private DataConfigService config;
	
	private volatile AssetConversionPaths next = null;
	private volatile ConversionResult result = null;
	private AtomicBoolean threadIsAlive = new AtomicBoolean(false);
	
	private final Runnable convertion = new PythonRunnable();
	
	/**
	 * Convert original (3DS file) to JSON and save converted file at target.
	 * This is not an async method, it blocks until the file has been converted, event though
	 * the convertion is executed by another thread internally.
	 * 
	 * @param original - original 3DS file
	 * @param target - target path for converted file containing JSON
	 */
	public synchronized MeshData createPreview(Path original, Path target) {
		final long startTime = System.currentTimeMillis();
		result = null; // delete last result
		next = new AssetConversionPaths(original, target);
		while (result == null) {
			initThreadIfNotRunning(); // start thread if needed
			notify(); // wake up thread if sleeping
			try {
				wait(10000); // block: worker will create result and wake us up
			} catch (InterruptedException e) {}
		}
		if(logger.isInfoEnabled()) {
			final long duration = System.currentTimeMillis() - startTime;
			logger.info("==> Conversion duration: "+duration+" ms");
		}
		if (!result.success) throw result.exception;
		else return result.meshData;
	}
	
	private void initThreadIfNotRunning() {
		synchronized(threadIsAlive) {
			if(!threadIsAlive.get()) {
				logger.info("Thread not alive: Starting new thread for python socket connection.");
				Thread thread = new Thread(convertion);
				threadIsAlive.set(true); // dont accidentally restart twice
				thread.start();
			}
		}
	}
	
	/**
	 * Runnable that will communicate with the calling thread using the roducer-consumer pattern
	 * to achieve synchronization.<br>
	 * <br>
	 * It will execute a python script that starts up a TCP server. It will connect to that
	 * TCP server and send the paths for each convertion (which it receives from the caller)
	 * to the server.<br>
	 * When there are no calls from a calling thread for a certain period of tmme as specified in
	 * threadTimeout attribute, the runnable will return. On returning it will inform the TCP server
	 * about connection end and close the connection.
	 * 
	 * An instance of this class can be reused since it holds no internal state.
	 * 
	 * @author Jan Mothes
	 */
	private class PythonRunnable implements Runnable {
		
		private ObjectMapper jackson = new ObjectMapper();
		
		private static final int port = 8090;
		private static final int tryReconnectCount = 10;
		private static final int tryReconnectSleep = 100;
		private static final int threadTimeout = 10000;
		
		private static final String conversionStart = "CONVERSION_START";
		private static final String conversionEnd = "CONVERSION_END";
		private static final String conversionFrom = "FROM";
		private static final String conversionTo = "TO";
		private static final String conversionSuccess = "SUCCESS";
		
		@Override
		public void run() {
			synchronized(PythonTCPSocket.this) {
				logger.info("Internal thread: starting execution...");
				try {
					startPythonScript();
					connectAndSend();
				}
				catch(RuntimeException e) {
					result = new ConversionResult(false, null, e); 
					PythonTCPSocket.this.notify();
				}
				logger.info("Internal thread: exiting...");
			}
		}
		
		private void startPythonScript() {
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
		}
		
		private void connectAndSend() {
			int currentTry = 1;
			boolean success = false;
			while(currentTry <= tryReconnectCount && !success) {
				try(Socket socket = new Socket("localhost", port)) {
					connectStreamsAndSend(socket);
					success = true;
				}
				catch (final ConnectException e) {
					final String message = "Connect failed, trying to reconnect.";
					logger.info(message);
					logger.debug(message, e);
					try {
						Thread.sleep(tryReconnectSleep);
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
						+ "(tried " + tryReconnectCount + " times and failed each time)!"));
			}
		}
		
		private void connectStreamsAndSend(Socket socket) throws IOException {
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
			socket.setSoTimeout(60000); // never wait more than 1 minute
			w.println(conversionStart);
			retrievePathsAndSend(w, r);
			w.println(conversionEnd);
		}
		
		private void retrievePathsAndSend(PrintWriter toPython, BufferedReader fromPython) {
			while (true) {
				while (next == null) { // wait until caller gives us next element
					final long startTime = System.currentTimeMillis();
					try {
						PythonTCPSocket.this.wait(threadTimeout);
						final long duration = System.currentTimeMillis() - startTime;
						if(duration > (threadTimeout - 300)) {
							// since caller didnt call us for quite some time we shutdown
							logger.info("Shutdown timeout reached: Shutting down python socket connection and thread.");
							threadIsAlive.set(false);
							return;
						}
					} catch (InterruptedException e1) {}
				}
				RuntimeException goneWrong = null;
				MeshData meshData = null;
				try {
					sendPaths(toPython, next);
					try {
						String success = fromPython.readLine();
						if (!success.equals(conversionSuccess)) {
							throw new IOException("Conversion protocol violation!");
						}
						String jsonString = fromPython.readLine();
						meshData = jackson.readValue(jsonString, MeshData.class);
					} catch (IOException e) {
						throw new UncheckedIOException(e);
					}
				}
				catch(RuntimeException e) {
					goneWrong = e; // record any exception
					// will be rethrown by thread that caused convertion
				}
				next = null; // reset next
				result = new ConversionResult(goneWrong == null, meshData, goneWrong); 
				PythonTCPSocket.this.notify(); // wake up waiting caller
			}
		}
		
		private void sendPaths(PrintWriter toPython, AssetConversionPaths paths) {
			toPython.println(conversionFrom);
			toPython.println(paths.original.toString());
			toPython.println(conversionTo);
			toPython.println(paths.target.toString());
		}
	};
}
