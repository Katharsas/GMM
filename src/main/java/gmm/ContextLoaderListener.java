package gmm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * May help find the startup points when scrolling through logs.
 * 
 * @author Jan Mothes
 */
public class ContextLoaderListener extends org.springframework.web.context.ContextLoaderListener {

	private final Logger logger = LoggerFactory.getLogger(getClass());
	
	public ContextLoaderListener() {
		super();
		logger.info("\n\n"
				+	"   .g8\"\"\"bgd      `7MMM.     ,MMF'    `7MMM.     ,MMF'" + "\n"
				+	" .dP'     `M        MMMb    dPMM        MMMb    dPMM  " + "\n"
				+	" dM'       `        M YM   ,M MM        M YM   ,M MM  " + "\n"
				+	" MM                 M  Mb  M' MM        M  Mb  M' MM  " + "\n"
				+	" MM.    `7MMF'      M  YM.P'  MM        M  YM.P'  MM  " + "\n"
				+	" `Mb.     MM        M  `YM'   MM        M  `YM'   MM  " + "\n"
				+	"   `\"bmmmdPY      .JML. `'  .JMML.    .JML. `'  .JMML." + "\n\n"
				+	"          Gothic Mod Manager is starting up...\n\n");
	}
}
