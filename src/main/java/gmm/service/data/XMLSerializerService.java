package gmm.service.data;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import gmm.domain.Comment;
import gmm.domain.FileTask;
import gmm.domain.ModelTask;
import gmm.domain.Task;
import gmm.domain.TextureTask;
import gmm.domain.User;

import com.thoughtworks.xstream.XStream;


@Service
public class XMLSerializerService {
	
	@Autowired
	DataConfigService dataConfig;
	
	final private XStream xstream;
	
	public XMLSerializerService() {
		xstream = new XStream();
		xstream.autodetectAnnotations(true);
		xstream.setMode(XStream.NO_REFERENCES);
		
		Class<?>[] clazzes = {
							Task.class,
							FileTask.class,
							ModelTask.class,
							ModelTask.class,
							TextureTask.class,
							User.class,
							Comment.class};
		for (Class<?> c : clazzes) {
			xstream.alias(c.getSimpleName(), c);
		}
	}
	
	public void serialize(Object object, String name) {
		String xml= xstream.toXML(object);
		writeToFile(xml, dataConfig.DATA_CURRENT+name+".xml");
	}

	public Object deserialize(String name) {
		return xstream.fromXML(new File(dataConfig.DATA_CURRENT+name+".xml"));
	}
        
    private void writeToFile(String content, String filePath) {
		File file = new File(filePath);
        try(PrintWriter writer = new PrintWriter(file)){
        	writer.println(content);
        }
        catch(IOException e){
            System.err.println("XMLSerialzer Error: Could not create PrintWriter!");
            e.printStackTrace();
        }
    }
}
